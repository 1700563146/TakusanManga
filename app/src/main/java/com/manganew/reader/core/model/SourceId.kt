package com.manganew.reader.core.model

/**
 * Stable host-owned identifier for one installed source.
 *
 * The value is opaque to parsers and readers. It must be non-blank, trimmed,
 * at most [MaxSourceIdLength] characters, and contain no control characters.
 *
 * ```
 * val sourceId = SourceId("github.example.reader")
 * ```
 */
@JvmInline
public value class SourceId(public val value: String) {
    init {
        requireOpaqueId("sourceId", value, MaxSourceIdLength)
    }

    override fun toString(): String = value
}

/**
 * Source-defined opaque identifier for a comic or chapter.
 *
 * Remote IDs may be URLs or non-numeric strings. Callers must not infer their
 * format; commands should keep them unchanged when passing them back to the
 * source.
 *
 * ```
 * val remoteId = RemoteId("/comic/42")
 * ```
 */
@JvmInline
public value class RemoteId(public val value: String) {
    init {
        requireOpaqueId("remoteId", value, MaxRemoteIdLength)
    }

    override fun toString(): String = value
}
