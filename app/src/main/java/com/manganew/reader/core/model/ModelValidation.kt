package com.manganew.reader.core.model

import java.net.URI

internal const val MaxSourceIdLength = 128
internal const val MaxRemoteIdLength = 2_048
internal const val MaxTitleLength = 512
internal const val MaxOptionalTextLength = 2_048
internal const val MaxDescriptionLength = 32_768
internal const val MaxTagCount = 64
internal const val MaxTagLength = 128
internal const val MaxMetadataCount = 64
internal const val MaxHeaderCount = 64
internal const val MaxHeaderValueLength = 8_192
internal const val MaxPageTokenLength = 4_096

internal fun requireOpaqueId(
    field: String,
    value: String,
    maxLength: Int,
) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) { "$field must not contain surrounding whitespace" }
    require(value.length <= maxLength) { "$field exceeds $maxLength characters" }
    require(value.none(Char::isISOControl)) { "$field must not contain control characters" }
}

internal fun requireSingleLineText(
    field: String,
    value: String,
    maxLength: Int,
) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) { "$field must not contain surrounding whitespace" }
    require(value.length <= maxLength) { "$field exceeds $maxLength characters" }
    require(value.none(Char::isISOControl)) { "$field must be a single line" }
}

internal fun requireOptionalSingleLineText(
    field: String,
    value: String?,
    maxLength: Int = MaxOptionalTextLength,
) {
    if (value != null) {
        requireSingleLineText(field, value, maxLength)
    }
}

internal fun requireDescription(value: String?) {
    if (value == null) return
    require(value.isNotBlank()) { "description must not be blank when present" }
    require(value == value.trim()) { "description must not contain surrounding whitespace" }
    require(value.length <= MaxDescriptionLength) {
        "description exceeds $MaxDescriptionLength characters"
    }
    require(value.none { it == '\u0000' }) { "description must not contain NUL" }
}

internal fun requireHttpUrl(
    field: String,
    value: String,
) {
    require(value == value.trim()) { "$field must not contain surrounding whitespace" }
    val uri = try {
        URI(value)
    } catch (invalid: IllegalArgumentException) {
        throw IllegalArgumentException("$field must be a valid absolute HTTP URL", invalid)
    }
    require(uri.isAbsolute) { "$field must be an absolute URL" }
    require(uri.scheme.lowercase() == "http" || uri.scheme.lowercase() == "https") {
        "$field must use http or https"
    }
    require(!uri.host.isNullOrBlank()) { "$field must contain a host" }
    require(uri.rawUserInfo == null) { "$field must not contain embedded credentials" }
}

internal fun requireHeaders(headers: Map<String, String>) {
    require(headers.size <= MaxHeaderCount) { "headers exceed $MaxHeaderCount entries" }
    headers.forEach { (name, value) ->
        require(name.isNotBlank()) { "header name must not be blank" }
        require(name == name.trim()) { "header name must not contain surrounding whitespace" }
        require(name.none(Char::isISOControl)) { "header name must not contain control characters" }
        require(value.length <= MaxHeaderValueLength) {
            "header $name exceeds $MaxHeaderValueLength characters"
        }
        require('\r' !in value && '\n' !in value && '\u0000' !in value) {
            "header $name contains prohibited control characters"
        }
    }
}

internal fun requireMetadata(metadata: Map<String, String>) {
    require(metadata.size <= MaxMetadataCount) {
        "metadata exceeds $MaxMetadataCount entries"
    }
    metadata.forEach { (key, value) ->
        requireSingleLineText("metadata key", key, MaxOptionalTextLength)
        requireSingleLineText("metadata value", value, MaxOptionalTextLength)
    }
}
