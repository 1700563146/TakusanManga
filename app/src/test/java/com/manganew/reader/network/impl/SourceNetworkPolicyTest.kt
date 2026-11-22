package com.manganew.reader.network.impl

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.source.api.SourceCapability
import com.manganew.reader.source.api.SourceDescriptor
import com.manganew.reader.source.api.SourceFormat
import java.net.InetAddress
import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMangaApi::class)
class SourceNetworkPolicyTest {
    @Test
    fun allowsOnlyDeclaredHostsHttpsAndAuthorizedPorts() {
        val policy = policy()

        assertTrue(policy.validate("https://source.example.test/path") is SourceUrlDecision.Allowed)
        assertEquals(
            SourceUrlDecision.Rejected(SourceHttpError.UrlRejected("host is not authorized for this source")),
            policy.validate("https://evil.example.test/path"),
        )
        assertTrue(
            policy.validate("https://source.example.test:8443/path") is SourceUrlDecision.Rejected,
        )
    }

    @Test
    fun rejectsHttpUnlessExplicitlyAllowed() {
        val securePolicy = policy(allowHttp = false)
        val httpPolicy = policy(allowHttp = true, allowedTestPorts = setOf(8080))

        assertTrue(httpPolicy.validate("http://source.example.test:8080/") is SourceUrlDecision.Allowed)
        assertTrue(securePolicy.validate("http://source.example.test:8080/") is SourceUrlDecision.Rejected)
    }

    @Test
    fun rejectsUnsupportedSchemesCredentialsAndUserInfo() {
        val policy = policy()

        assertEquals(
            SourceUrlDecision.Rejected(
                SourceHttpError.UrlRejected(
                    "only HTTP(S) URLs are allowed",
                ),
            ),
            policy.validate("javascript:alert(1)"),
        )
        assertEquals(
            SourceUrlDecision.Rejected(
                SourceHttpError.UrlRejected(
                    "only HTTP(S) URLs are allowed",
                ),
            ),
            policy.validate("file:///etc/passwd"),
        )
        assertEquals(
            SourceUrlDecision.Rejected(
                SourceHttpError.UrlRejected(
                    "URL credentials are not allowed",
                ),
            ),
            policy.validate("https://user:secret@source.example.test/"),
        )
        assertTrue(
            policy(allowHttp = true).validate("http://localhost/") is SourceUrlDecision.Rejected,
        )
    }

    @Test
    fun rejectsPrivateLoopbackLinkLocalAndReservedIpv4() {
        listOf(
            "127.0.0.1",
            "10.1.2.3",
            "172.16.0.1",
            "192.168.1.1",
            "169.254.1.1",
            "100.64.0.1",
            "192.0.2.1",
            "198.51.100.1",
            "203.0.113.1",
            "224.0.0.1",
        ).forEach { literal ->
            assertFalse(literal, isPublicNetworkAddress(InetAddress.getByName(literal)))
        }
        assertTrue(isPublicNetworkAddress(InetAddress.getByName("93.184.216.34")))
    }

    @Test
    fun rejectsPrivateAndReservedIpv6() {
        listOf(
            "::1",
            "fc00::1",
            "fe80::1",
            "2001:db8::1",
            "2001::1",
            "2001:2::1",
            "2001:10::1",
            "100::1",
            "2002:7f00:1::",
            "64:ff9b::7f00:1",
            "::ffff:127.0.0.1",
        ).forEach { literal ->
            assertFalse(literal, isPublicNetworkAddress(InetAddress.getByName(literal)))
        }
        assertTrue(isPublicNetworkAddress(InetAddress.getByName("2606:4700:4700::1111")))
    }

    @Test
    fun dnsPolicyRejectsRebindingAndMixedPrivateResults() {
        val policy = policy()
        val publicAddress = InetAddress.getByName("93.184.216.34")
        val privateAddress = InetAddress.getByName("127.0.0.1")
        var lookupCount = 0
        val changingDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                lookupCount += 1
                return if (lookupCount == 1) {
                    listOf(publicAddress)
                } else {
                    listOf(publicAddress, privateAddress)
                }
            }
        }
        val validatingDns = SourceValidatingDns(policy, changingDns)

        assertEquals(listOf(publicAddress), validatingDns.lookup("source.example.test"))
        assertThrows(SourceNetworkPolicyException::class.java) {
            validatingDns.lookup("source.example.test")
        }
    }

    private fun policy(
        allowHttp: Boolean = false,
        allowedTestPorts: Set<Int> = emptySet(),
    ): SourceNetworkPolicy =
        SourceNetworkPolicy(
            descriptor = SourceDescriptor(
                id = SourceId("source-policy"),
                name = "Policy source",
                version = "1.0.0",
                format = SourceFormat.NATIVE_JSON,
                adapterApiVersion = 1,
                baseUrl = "https://source.example.test",
                allowedHosts = setOf("source.example.test", "images.example.test"),
                capabilities = setOf(SourceCapability.SEARCH),
                allowHttp = allowHttp,
            ),
            allowedPortsForTests = allowedTestPorts,
        )
}
