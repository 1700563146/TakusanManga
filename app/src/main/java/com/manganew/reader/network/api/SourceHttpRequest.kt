package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId

/** HTTP methods supported by the source network API. */
@ExperimentalMangaApi
public enum class SourceHttpMethod {
    GET,
    POST,
}

/**
 * One host-mediated HTTP request for a source.
 *
 * [headers] are normalized to lower-case names and copied defensively. The
 * request cannot set transport-controlled headers such as `Host`,
 * `Content-Length`, `Connection`, or `Cookie`; the host owns those values.
 *
 * URL scheme, host, DNS, and redirect checks happen in host HTTP policy
 * before any connection is opened.
 */
@ExperimentalMangaApi
public class SourceHttpRequest(
    public val sourceId: SourceId,
    public val url: String,
    public val method: SourceHttpMethod = SourceHttpMethod.GET,
    headers: Map<String, String> = emptyMap(),
    public val body: String? = null,
    public val timeoutMillis: Int = SourceHttpLimits.DEFAULT_TIMEOUT_MILLIS,
) {
    /** Defensive, lower-case copy of source-provided headers. */
    public val headers: Map<String, String> =
        SourceHttpHeaderValidation.normalizeRequestHeaders(headers)

    init {
        require(url.isNotBlank()) { "url must not be blank" }
        require(url.length <= SourceHttpLimits.MAX_URL_LENGTH) {
            "url exceeds ${SourceHttpLimits.MAX_URL_LENGTH} characters"
        }
        require(url.none(Char::isISOControl)) { "url must not contain control characters" }
        require(timeoutMillis in 1..SourceHttpLimits.MAX_TIMEOUT_MILLIS) {
            "timeoutMillis must be between 1 and ${SourceHttpLimits.MAX_TIMEOUT_MILLIS}"
        }
        require(method == SourceHttpMethod.POST || body == null) {
            "only POST requests may contain a body"
        }
        require(body == null || body.encodeToByteArray().size <= SourceHttpLimits.MAX_REQUEST_BODY_BYTES) {
            "request body exceeds ${SourceHttpLimits.MAX_REQUEST_BODY_BYTES} UTF-8 bytes"
        }
    }
}
