package com.manganew.reader.network.impl

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.network.api.SourceHttpClient
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.network.api.SourceHttpHeaderValidation
import com.manganew.reader.network.api.SourceHttpLimits
import com.manganew.reader.network.api.SourceHttpRequest
import com.manganew.reader.network.api.SourceHttpResponse
import com.manganew.reader.network.api.SourceHttpResult
import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * OkHttp implementation scoped to one source.
 *
 * The client has no HTTP disk cache and does not follow redirects until
 * per-hop URL validation is available. Each instance owns an in-memory
 * CookieJar and a bounded dispatcher.
 */
@OptIn(ExperimentalMangaApi::class)
internal class OkHttpSourceHttpClient(
    private val sourceId: SourceId,
    private val cookieJar: InMemoryCookieJar = InMemoryCookieJar(),
    internal val okHttpClient: OkHttpClient = defaultClient(cookieJar),
) : SourceHttpClient {
    private val closed = AtomicBoolean(false)

    override suspend fun execute(request: SourceHttpRequest): SourceHttpResult {
        if (closed.get()) {
            return failure("client", "client is closed")
        }
        if (request.sourceId != sourceId) {
            return failure("sourceId", "request source does not match client source")
        }

        val url = request.url.toHttpUrlOrNull()
            ?: return failure("url", "url must be an absolute HTTP(S) URL")
        val mediaType = request.headers["content-type"]?.let { value ->
            value.toMediaTypeOrNull() ?: return failure("content-type", "invalid media type")
        }
        val requestBody = request.body?.toRequestBody(mediaType)
        val okHttpRequest = try {
            Request.Builder()
                .url(url)
                .apply {
                    request.headers.forEach { (name, value) -> header(name, value) }
                    method(request.method.name, requestBody)
                }
                .build()
        } catch (_: IllegalArgumentException) {
            return failure("request", "request could not be created")
        }

        val timeoutMillis = request.timeoutMillis.toLong()
        val callClient = okHttpClient.newBuilder()
            .connectTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .readTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .writeTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .callTimeout(timeoutMillis, TimeUnit.MILLISECONDS)
            .build()
        val call = callClient.newCall(okHttpRequest)

        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            continuation.resume(SourceHttpResult.Failure(e.toSourceHttpError()))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val result = try {
                            readResponse(response)
                        } catch (_: IllegalArgumentException) {
                            SourceHttpResult.Failure(SourceHttpError.ProtocolFailure)
                        } catch (exception: IOException) {
                            SourceHttpResult.Failure(exception.toSourceHttpError())
                        }
                        if (continuation.isActive) {
                            continuation.resume(result)
                        }
                    }
                },
            )
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        cookieJar.clear()
        okHttpClient.dispatcher.cancelAll()
        okHttpClient.connectionPool.evictAll()
        okHttpClient.dispatcher.executorService.shutdown()
    }

    private fun readResponse(response: Response): SourceHttpResult = response.use {
        val body = response.body
        val contentLength = body.contentLength()
        if (contentLength > SourceHttpLimits.MAX_RESPONSE_BODY_BYTES) {
            return SourceHttpResult.Failure(
                SourceHttpError.ResponseTooLarge(SourceHttpLimits.MAX_RESPONSE_BODY_BYTES),
            )
        }

        val source = body.source()
        val buffer = Buffer()
        var totalBytes = 0L
        while (true) {
            val read = source.read(buffer, READ_BUFFER_SIZE)
            if (read == -1L) {
                break
            }
            totalBytes += read
            if (totalBytes > SourceHttpLimits.MAX_RESPONSE_BODY_BYTES) {
                return SourceHttpResult.Failure(
                    SourceHttpError.ResponseTooLarge(SourceHttpLimits.MAX_RESPONSE_BODY_BYTES),
                )
            }
        }

        val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
        val response = SourceHttpResponse(
            statusCode = response.code,
            finalUrl = response.request.url.toString(),
            headers = safeResponseHeaders(response),
            body = buffer.readByteArray().toString(charset),
        )
        SourceHttpResult.Success(response)
    }

    private fun safeResponseHeaders(response: Response): Map<String, String> =
        response.headers.names()
            .asSequence()
            .filterNot(SourceHttpHeaderValidation::isSensitiveResponseHeader)
            .take(SourceHttpLimits.MAX_HEADER_COUNT)
            .associateWith { name -> response.headers.values(name).joinToString(", ") }

    private fun failure(field: String, reason: String): SourceHttpResult.Failure =
        SourceHttpResult.Failure(SourceHttpError.InvalidRequest(field, reason))

    private companion object {
        const val READ_BUFFER_SIZE = 8 * 1024L
        const val MAX_CONCURRENT_REQUESTS = 4
        const val MAX_CONCURRENT_REQUESTS_PER_HOST = 2

        fun defaultClient(cookieJar: InMemoryCookieJar): OkHttpClient =
            OkHttpClient.Builder()
                .cache(null)
                .cookieJar(cookieJar)
                .dispatcher(
                    Dispatcher().apply {
                        maxRequests = MAX_CONCURRENT_REQUESTS
                        maxRequestsPerHost = MAX_CONCURRENT_REQUESTS_PER_HOST
                    },
                )
                .followRedirects(false)
                .followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .build()
    }
}

private fun Throwable.toSourceHttpError(): SourceHttpError =
    when (this) {
        is SocketTimeoutException -> SourceHttpError.Timeout
        is UnknownHostException -> SourceHttpError.DnsFailure
        is SSLException -> SourceHttpError.TlsFailure
        is ProtocolException -> SourceHttpError.ProtocolFailure
        is ConnectException -> SourceHttpError.ConnectionFailure
        is InterruptedIOException -> SourceHttpError.Timeout
        is IOException -> SourceHttpError.ConnectionFailure
        else -> SourceHttpError.ProtocolFailure
    }
