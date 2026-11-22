package com.manganew.reader.network.api

import java.util.Locale

internal object SourceHttpHeaderValidation {
    private val tokenPunctuation = "!#$%&'*+-.^_`|~"

    private val forbiddenRequestHeaders = setOf(
        "connection",
        "content-length",
        "cookie",
        "expect",
        "host",
        "proxy-connection",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade",
    )

    private val sensitiveResponseHeaders = setOf(
        "authorization",
        "cookie",
        "proxy-authorization",
        "set-cookie",
    )

    fun normalizeRequestHeaders(headers: Map<String, String>): Map<String, String> =
        normalize(headers, forbiddenNames = forbiddenRequestHeaders)

    fun normalizeResponseHeaders(headers: Map<String, String>): Map<String, String> =
        normalize(headers, forbiddenNames = sensitiveResponseHeaders)

    private fun normalize(
        headers: Map<String, String>,
        forbiddenNames: Set<String>,
    ): Map<String, String> {
        require(headers.size <= SourceHttpLimits.MAX_HEADER_COUNT) {
            "header count exceeds ${SourceHttpLimits.MAX_HEADER_COUNT}"
        }

        val normalized = LinkedHashMap<String, String>(headers.size)
        headers.forEach { (name, value) ->
            require(name.length in 1..SourceHttpLimits.MAX_HEADER_NAME_LENGTH) {
                "header name length is invalid"
            }
            require(name.all(::isTokenCharacter)) { "header name contains invalid characters" }

            val normalizedName = name.lowercase(Locale.ROOT)
            require(normalizedName !in forbiddenNames) { "header $normalizedName is host-controlled" }
            require(normalizedName !in normalized) { "duplicate header name: $normalizedName" }
            require(value.length <= SourceHttpLimits.MAX_HEADER_VALUE_LENGTH) {
                "header value is too long for $normalizedName"
            }
            require(value.none(Char::isISOControl)) { "header value contains control characters" }

            normalized[normalizedName] = value
        }
        return normalized.toMap()
    }

    private fun isTokenCharacter(character: Char): Boolean =
        character in 'a'..'z' ||
            character in 'A'..'Z' ||
            character in '0'..'9' ||
            character in tokenPunctuation
}
