package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi

/** Result of one host-mediated source HTTP request. */
@ExperimentalMangaApi
public sealed interface SourceHttpResult {
    /** A complete HTTP response, including non-success status codes. */
    public data class Success(public val response: SourceHttpResponse) : SourceHttpResult

    /** The request could not produce a complete HTTP response. */
    public data class Failure(public val error: SourceHttpError) : SourceHttpResult
}

/**
 * Coroutine-friendly HTTP boundary for one installed source.
 *
 * Implementations are source-scoped so cookies and connection state cannot
 * leak across [com.manganew.reader.core.model.SourceId] values. They must
 * propagate coroutine cancellation and close every response body.
 */
@ExperimentalMangaApi
public interface SourceHttpClient : AutoCloseable {
    /** Executes [request] without blocking the caller thread. */
    public suspend fun execute(request: SourceHttpRequest): SourceHttpResult

    /** Releases source-scoped requests, cookies, and pooled connections. */
    override fun close()
}
