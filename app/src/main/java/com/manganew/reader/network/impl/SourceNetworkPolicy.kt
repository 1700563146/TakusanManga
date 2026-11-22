package com.manganew.reader.network.impl

import com.manganew.reader.core.model.SourceId
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.source.api.SourceDescriptor
import java.io.IOException
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.URI
import java.net.UnknownHostException
import java.util.Locale
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal sealed interface SourceUrlDecision {
    data class Allowed(val url: HttpUrl) : SourceUrlDecision

    data class Rejected(val error: SourceHttpError.UrlRejected) : SourceUrlDecision
}

/**
 * Source-scoped URL and DNS policy.
 *
 * Test-only loopback and port allowances are internal and default to disabled,
 * so production construction cannot bypass the public-address requirement.
 */
internal class SourceNetworkPolicy(
    descriptor: SourceDescriptor,
    private val allowLoopbackForTests: Boolean = false,
    allowedPortsForTests: Set<Int> = emptySet(),
) {
    val sourceId: SourceId = descriptor.id

    private val allowedHosts = descriptor.allowedHosts
    private val allowHttp = descriptor.allowHttp
    private val allowedPorts: Set<Int> = buildSet {
        add(443)
        if (allowHttp) {
            add(80)
        }
        descriptor.baseUrl.toHttpUrlOrNull()?.port?.let(::add)
        addAll(allowedPortsForTests)
    }

    init {
        require(allowedPortsForTests.all { it in 1..65535 }) {
            "test ports must be valid TCP ports"
        }
    }

    fun validate(rawUrl: String): SourceUrlDecision {
        val scheme = try {
            URI(rawUrl).scheme?.lowercase(Locale.ROOT)
        } catch (_: IllegalArgumentException) {
            null
        }
        if (scheme != null && scheme != "http" && scheme != "https") {
            return rejected("only HTTP(S) URLs are allowed")
        }

        val url = rawUrl.toHttpUrlOrNull()
            ?: return rejected("URL must be absolute and valid")
        return validate(url)
    }

    fun validate(url: HttpUrl): SourceUrlDecision {
        if (!url.isHttps && !(allowHttp && url.scheme == "http")) {
            return rejected("HTTPS is required for this source")
        }
        if (url.username.isNotEmpty() || url.password.isNotEmpty()) {
            return rejected("URL credentials are not allowed")
        }

        val host = url.host.lowercase(Locale.ROOT).trimEnd('.')
        if (host !in allowedHosts) {
            return rejected("host is not authorized for this source")
        }
        if (url.port !in allowedPorts) {
            return rejected("port is not authorized for this source")
        }
        return SourceUrlDecision.Allowed(url)
    }

    fun requireAllowedDnsHost(hostname: String) {
        val normalized = hostname.lowercase(Locale.ROOT).trimEnd('.')
        if (normalized !in allowedHosts) {
            throw SourceNetworkPolicyException("host is not authorized for this source")
        }
    }

    fun requirePublicAddress(address: InetAddress) {
        if (!isPublicNetworkAddress(address, allowLoopbackForTests)) {
            throw SourceNetworkPolicyException("resolved address is not public")
        }
    }

    private fun rejected(reason: String): SourceUrlDecision.Rejected =
        SourceUrlDecision.Rejected(SourceHttpError.UrlRejected(reason))
}

internal class SourceNetworkPolicyException(message: String) : IOException(message)

internal class SourceValidatingDns(
    private val policy: SourceNetworkPolicy,
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        policy.requireAllowedDnsHost(hostname)
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty()) {
            throw UnknownHostException("host did not resolve")
        }
        addresses.forEach(policy::requirePublicAddress)
        return addresses
    }
}

internal fun isPublicNetworkAddress(
    address: InetAddress,
    allowLoopbackForTests: Boolean = false,
): Boolean {
    if (address.isLoopbackAddress) {
        return allowLoopbackForTests
    }
    if (
        address.isAnyLocalAddress ||
        address.isLinkLocalAddress ||
        address.isSiteLocalAddress ||
        address.isMulticastAddress
    ) {
        return false
    }
    return when (address) {
        is Inet4Address -> isPublicIpv4(address.address)
        is Inet6Address -> isPublicIpv6(address.address)
        else -> false
    }
}

private fun isPublicIpv4(bytes: ByteArray): Boolean {
    if (bytes.size != 4) {
        return false
    }
    val first = bytes[0].toInt() and 0xff
    val second = bytes[1].toInt() and 0xff
    val third = bytes[2].toInt() and 0xff

    return when {
        first == 0 || first == 10 || first == 127 -> false
        first == 100 && second in 64..127 -> false
        first == 169 && second == 254 -> false
        first == 172 && second in 16..31 -> false
        first == 192 && second == 0 && (third == 0 || third == 2) -> false
        first == 192 && second == 88 && third == 99 -> false
        first == 192 && second == 168 -> false
        first == 198 && second in 18..19 -> false
        first == 198 && second == 51 && third == 100 -> false
        first == 203 && second == 0 && third == 113 -> false
        first >= 224 -> false
        else -> true
    }
}

private fun isPublicIpv6(bytes: ByteArray): Boolean {
    if (bytes.size != 16) {
        return false
    }
    val first = bytes[0].toInt() and 0xff
    val second = bytes[1].toInt() and 0xff
    val third = bytes[2].toInt() and 0xff
    val fourth = bytes[3].toInt() and 0xff

    return when {
        (first and 0xfe) == 0xfc -> false
        first == 0xfe && (second and 0xc0) == 0x80 -> false
        first == 0xfe && (second and 0xc0) == 0xc0 -> false
        first == 0xff -> false
        first == 0x01 && second == 0x00 && bytes.bytesAreZero(2, 8) -> false
        first == 0x20 && second == 0x01 && third == 0x0d && fourth == 0xb8 -> false
        first == 0x20 && second == 0x01 && third == 0x00 && fourth == 0x00 -> false
        first == 0x20 && second == 0x01 && third == 0x00 && fourth == 0x02 -> false
        first == 0x20 && second == 0x01 && third == 0x00 && fourth in 0x10..0x2f -> false
        first == 0x20 && second == 0x02 -> false
        first == 0x00 && second == 0x64 && third == 0xff && fourth == 0x9b -> false
        first == 0x3f && (second and 0xf0) == 0xf0 -> false
        else -> true
    }
}

private fun ByteArray.bytesAreZero(start: Int, endExclusive: Int): Boolean {
    for (index in start until endExclusive) {
        if (this[index] != 0.toByte()) {
            return false
        }
    }
    return true
}
