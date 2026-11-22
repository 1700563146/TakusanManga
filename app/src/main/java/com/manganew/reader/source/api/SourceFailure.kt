package com.manganew.reader.source.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/**
 * Structured failures that may cross the public source boundary.
 *
 * Implementations must not include full response bodies, cookies,
 * authorization values, signed query strings, or stack traces in diagnostic
 * fields displayed to users. Cancellation is not represented here and must
 * continue to propagate as `CancellationException`.
 */
@ExperimentalMangaApi
public sealed interface SourceFailure {
    /** Network failure. The optional cause is for internal diagnostics only. */
    public data class Network(
        public val cause: Throwable? = null,
    ) : SourceFailure

    /** A request or source test exceeded its configured deadline. */
    public data object Timeout : SourceFailure

    /** A non-success HTTP response in the supported failure range. */
    public data class Http(
        public val statusCode: Int,
    ) : SourceFailure {
        init {
            require(statusCode in 400..599) { "HTTP failure status must be in 400..599" }
        }
    }

    /** A known document could not be interpreted at [stage]. */
    public data class Parse(
        public val stage: String,
        public val detail: String,
    ) : SourceFailure {
        init {
            requireSafeDiagnostic("parse.stage", stage)
            requireSafeDiagnostic("parse.detail", detail)
        }
    }

    /** The source completed successfully but returned no usable result. */
    public data object EmptyResult : SourceFailure

    /** The source lacks a declared or user-approved capability. */
    public data class PermissionDenied(
        public val permission: String,
    ) : SourceFailure {
        init {
            requireSafeDiagnostic("permission", permission)
        }
    }

    /** No installed adapter supports the detected format. */
    public data class UnsupportedFormat(
        public val format: String,
    ) : SourceFailure {
        init {
            requireSafeDiagnostic("format", format)
        }
    }

    /** A constrained plugin operation failed. */
    public data class PluginExecution(
        public val pluginId: String,
        public val detail: String,
    ) : SourceFailure {
        init {
            requireSafeDiagnostic("pluginId", pluginId)
            requireSafeDiagnostic("pluginExecution.detail", detail)
        }
    }

    /** The remote service asked the host to slow down. */
    public data object RateLimited : SourceFailure

    /** Input failed a documented contract precondition. */
    public data class InvalidInput(
        public val field: String,
        public val reason: String,
    ) : SourceFailure {
        init {
            requireSafeDiagnostic("invalidInput.field", field)
            requireSafeDiagnostic("invalidInput.reason", reason)
        }
    }
}

private fun requireSafeDiagnostic(
    field: String,
    value: String,
) {
    require(value.isNotBlank()) { "$field must not be blank" }
    require(value == value.trim()) { "$field must not contain surrounding whitespace" }
    require(value.length <= 512) { "$field exceeds 512 characters" }
    require('\r' !in value && '\n' !in value && '\u0000' !in value) {
        "$field must not contain control characters"
    }
}
