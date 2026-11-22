package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi
import java.util.Locale

/**
 * A completed host-mediated HTTP response containing HTML or JSON text.
 *
 * HTTP 4xx and 5xx responses are represented here so callers can map status,
 * headers, and a bounded body into a source-specific failure. Transport
 * failures use [SourceHttpError].
 *
 * Sensitive response headers such as `Set-Cookie` and `Authorization` are not
 * exposed. The response body must never be written to disk or used to carry
 * image bytes through this API.
 */
@ExperimentalMangaApi
public class SourceHttpResponse(
    public val statusCode: Int,
    public val finalUrl: String,
    headers: Map<String, String> = emptyMap(),
    public val body: String,
) {
    /** Defensive, lower-case copy of response headers safe for source callers. */
    public val headers: Map<String, String> =
        SourceHttpHeaderValidation.normalizeResponseHeaders(headers)

    /** UTF-8 size of the decoded text body. */
    public val bodySizeBytes: Int = body.encodeToByteArray().size

    /** Lower-case media type without parameters, when supplied by the server. */
    public val contentType: String? =
        this.headers["content-type"]?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)

    /** Whether the HTTP status is in the successful 2xx range. */
    public val isSuccessful: Boolean
        get() = statusCode in 200..299

    init {
        require(statusCode in 100..599) { "statusCode must be between 100 and 599" }
        require(finalUrl.isNotBlank()) { "finalUrl must not be blank" }
        require(finalUrl.length <= SourceHttpLimits.MAX_URL_LENGTH) {
            "finalUrl exceeds ${SourceHttpLimits.MAX_URL_LENGTH} characters"
        }
        require(finalUrl.none(Char::isISOControl)) {
            "finalUrl must not contain control characters"
        }
        require(bodySizeBytes <= SourceHttpLimits.MAX_RESPONSE_BODY_BYTES) {
            "response body exceeds ${SourceHttpLimits.MAX_RESPONSE_BODY_BYTES} UTF-8 bytes"
        }
    }
}
