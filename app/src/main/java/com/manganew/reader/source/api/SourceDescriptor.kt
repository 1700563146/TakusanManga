package com.manganew.reader.source.api

import com.manganew.reader.core.model.SourceId
import java.net.URI

/** Optional operations declared by an installed source. */
public enum class SourceCapability {
    SEARCH,
    DETAIL,
    CHAPTERS,
    PAGES,
    EXPLORE,
    ADVANCED_IMAGE,
}

/** Declarative source format handled by a host-side adapter. */
public enum class SourceFormat {
    NATIVE_JSON,
    LEGADO_JSON_SUBSET,
    NATIVE_JS,
    BREEZE_JS_COMPAT,
}

/**
 * Host-visible metadata and network permission baseline for one source.
 *
 * [allowedHosts] contains exact lowercase DNS host names. Wildcards, ports,
 * paths, and IP literals are rejected here; the network gateway separately
 * resolves and validates every connection and redirect.
 *
 * [allowHttp] defaults to false. HTTP sources require an explicit declaration
 * and must still receive user consent in the import flow.
 *
 * ```
 * val descriptor = SourceDescriptor(
 *     id = SourceId("demo-source"),
 *     name = "Demo",
 *     version = "1.0.0",
 *     format = SourceFormat.NATIVE_JSON,
 *     adapterApiVersion = 1,
 *     baseUrl = "https://example.test",
 *     allowedHosts = setOf("example.test", "images.example.test"),
 *     capabilities = setOf(SourceCapability.SEARCH, SourceCapability.DETAIL),
 * )
 * ```
 */
public data class SourceDescriptor(
    public val id: SourceId,
    public val name: String,
    public val version: String,
    public val format: SourceFormat,
    public val adapterApiVersion: Int,
    public val baseUrl: String,
    public val allowedHosts: Set<String>,
    public val capabilities: Set<SourceCapability>,
    public val allowHttp: Boolean = false,
) {
    init {
        requireSourceText("name", name, maxLength = 256)
        requireSourceText("version", version, maxLength = 128)
        require(adapterApiVersion > 0) { "adapterApiVersion must be positive" }
        require(capabilities.isNotEmpty()) { "capabilities must not be empty" }

        val baseUri = requireSourceBaseUrl(baseUrl, allowHttp)
        val baseHost = requireNotNull(baseUri.host).lowercase().trimEnd('.')

        require(allowedHosts.isNotEmpty()) { "allowedHosts must not be empty" }
        require(allowedHosts.size <= MaxAllowedHosts) {
            "allowedHosts exceeds $MaxAllowedHosts entries"
        }
        allowedHosts.forEach { host ->
            requireAllowedHost(host)
        }
        require(baseHost in allowedHosts) {
            "allowedHosts must contain the base URL host"
        }
    }
}

private const val MaxAllowedHosts = 64

private fun requireSourceText(
    field: String,
    value: String,
    maxLength: Int,
) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) { "$field must not contain surrounding whitespace" }
    require(value.length <= maxLength) { "$field exceeds $maxLength characters" }
    require(value.none(Char::isISOControl)) { "$field must not contain control characters" }
}

private fun requireSourceBaseUrl(
    value: String,
    allowHttp: Boolean,
): URI {
    require(value == value.trim()) { "baseUrl must not contain surrounding whitespace" }
    val uri = try {
        URI(value)
    } catch (invalid: IllegalArgumentException) {
        throw IllegalArgumentException("baseUrl must be a valid absolute URL", invalid)
    }
    require(uri.isAbsolute) { "baseUrl must be absolute" }
    val scheme = uri.scheme.lowercase()
    require(scheme == "https" || (allowHttp && scheme == "http")) {
        "baseUrl must use HTTPS unless allowHttp is explicitly enabled"
    }
    require(!uri.host.isNullOrBlank()) { "baseUrl must contain a host" }
    require(uri.rawUserInfo == null) { "baseUrl must not contain embedded credentials" }
    return uri
}

private fun requireAllowedHost(value: String) {
    require(value.isNotBlank()) { "allowedHosts must not contain blank values" }
    require(value == value.trim()) {
        "allowedHosts values must not contain surrounding whitespace"
    }
    require(value == value.lowercase()) { "allowedHosts values must be lowercase" }
    require(value.length <= 253) { "allowed host exceeds 253 characters" }
    require('*' !in value) { "allowedHosts must not contain wildcards" }
    require(':' !in value && '/' !in value) {
        "allowedHosts must contain host names without ports or paths"
    }
    val labels = value.split('.')
    require(labels.size >= 2) { "allowedHosts must contain DNS host names" }
    require(labels.all { label ->
        label.isNotEmpty() &&
            label.length <= 63 &&
            label.first().isLetterOrDigit() &&
            label.last().isLetterOrDigit() &&
            label.all { it.isLetterOrDigit() || it == '-' }
    }) {
        "allowedHosts contains an invalid host name"
    }
    require(!labels.all { label -> label.all(Char::isDigit) }) {
        "allowedHosts must not contain IP address literals"
    }
}
