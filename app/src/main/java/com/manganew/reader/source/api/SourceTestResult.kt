package com.manganew.reader.source.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/**
 * Result of a bounded source connection test.
 *
 * A success contains no source document data. A failure contains only a
 * structured [SourceFailure] and an optional user-safe message.
 */
@ExperimentalMangaApi
public sealed interface SourceTestResult {
    public val isSuccess: Boolean

    /**
     * Connection test completed.
     *
     * @param latencyMillis optional non-negative elapsed time.
     * @param message optional user-safe message.
     */
    public data class Success(
        public val latencyMillis: Long? = null,
        public val message: String? = null,
    ) : SourceTestResult {
        init {
            require(latencyMillis == null || latencyMillis >= 0) {
                "latencyMillis must be non-negative"
            }
            requireOptionalTestMessage(message)
        }

        override val isSuccess: Boolean
            get() = true
    }

    /**
     * Connection test failed.
     *
     * @param failure structured failure suitable for host diagnostics.
     * @param message optional user-safe message.
     */
    public data class Failure(
        public val failure: SourceFailure,
        public val message: String? = null,
    ) : SourceTestResult {
        init {
            requireOptionalTestMessage(message)
        }

        override val isSuccess: Boolean
            get() = false
    }
}

private fun requireOptionalTestMessage(message: String?) {
    if (message == null) return
    require(message.isNotBlank()) { "test message must not be blank" }
    require(message == message.trim()) { "test message must not contain surrounding whitespace" }
    require(message.length <= 512) { "test message exceeds 512 characters" }
    require('\r' !in message && '\n' !in message && '\u0000' !in message) {
        "test message must not contain control characters"
    }
}
