package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/**
 * Host-owned limits for source HTTP requests and text document responses.
 *
 * Rules and plugins may request a shorter timeout, but cannot increase these
 * limits. Response bytes are counted while reading the stream because
 * `Content-Length` may be missing or untrusted.
 */
@ExperimentalMangaApi
public object SourceHttpLimits {
    /** Default timeout for one source request. */
    public const val DEFAULT_TIMEOUT_MILLIS: Int = 12_000

    /** Hard upper bound for a caller-selected request timeout. */
    public const val MAX_TIMEOUT_MILLIS: Int = 30_000

    /** Maximum UTF-8 request body size. */
    public const val MAX_REQUEST_BODY_BYTES: Int = 64 * 1024

    /**
     * Maximum text response size.
     *
     * Implementations must enforce this against bytes read after transport
     * content decoding, such as gzip, and before character decoding.
     */
    public const val MAX_RESPONSE_BODY_BYTES: Int = 4 * 1024 * 1024

    /** Maximum URL length accepted by the request model. */
    public const val MAX_URL_LENGTH: Int = 8 * 1024

    /** Maximum number of request or response headers exposed through network API. */
    public const val MAX_HEADER_COUNT: Int = 64

    /** Maximum length of one HTTP header name. */
    public const val MAX_HEADER_NAME_LENGTH: Int = 128

    /** Maximum length of one HTTP header value. */
    public const val MAX_HEADER_VALUE_LENGTH: Int = 4 * 1024
}
