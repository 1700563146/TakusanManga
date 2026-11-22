package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/**
 * Structured transport failures that may cross the source network boundary.
 *
 * Errors deliberately contain no raw exception, response body, URL query, or
 * request header. Cancellation is not an error and must continue to propagate
 * as `CancellationException`.
 */
@ExperimentalMangaApi
public sealed interface SourceHttpError {
    /** The request DTO or transport policy rejected an input before sending. */
    public data class InvalidRequest(
        public val field: String,
        public val reason: String,
    ) : SourceHttpError

    /** The URL or one redirect target failed scheme, host, or IP policy. */
    public data class UrlRejected(public val reason: String) : SourceHttpError

    /** DNS resolution failed. */
    public data object DnsFailure : SourceHttpError

    /** A TCP or TLS connection could not be established. */
    public data object ConnectionFailure : SourceHttpError

    /** The server certificate or TLS negotiation failed. */
    public data object TlsFailure : SourceHttpError

    /** A request exceeded its configured timeout. */
    public data object Timeout : SourceHttpError

    /** The host stopped following redirects at its configured limit. */
    public data class TooManyRedirects(public val limit: Int) : SourceHttpError

    /** The response exceeded the host byte limit while streaming. */
    public data class ResponseTooLarge(public val limitBytes: Int) : SourceHttpError

    /** The peer returned an invalid HTTP response. */
    public data object ProtocolFailure : SourceHttpError
}
