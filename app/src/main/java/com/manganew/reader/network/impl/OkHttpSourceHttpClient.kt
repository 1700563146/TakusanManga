package com.manganew.reader.network.impl

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.network.api.SourceHttpClient
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.network.api.SourceHttpHeaderValidation
import com.manganew.reader.network.api.SourceHttpLimits
import com.manganew.reader.network.api.SourceHttpMethod
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
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.Buffer

/**
 * OkHttp implementation scoped to one source.
 *
 * The client has no HTTP disk cache and validates every redirect before the
 * next request. Each instance owns an in-memory CookieJar and a bounded
 * dispatcher.
 */
@OptIn(ExperimentalMangaApi::class)
internal class OkHttpSourceHttpClient(
    private val networkPolicy: SourceNetworkPolicy,
    dns: Dns = Dns.SYSTEM,
    private val cookieJar: InMemoryCookieJar = InMemoryCookieJar(),
    internal val okHttpClient: OkHttpClient = defaultClient(cookieJar, networkPolicy, dns),
) : SourceHttpClient {
    private val sourceId = networkPolicy.sourceId
    private val closed = AtomicBoolean(false)

    override suspend fun execute(request: SourceHttpRequest): SourceHttpResult {
        if (closed.get()) {
            return failure("client", "client is closed")
        }
        if (request.sourceId != sourceId) {
            return failure("sourceId", "request source does not match client source")
        }

        val initialUrl = when (val decision = networkPolicy.validate(request.url)) {
            is SourceUrlDecision.Rejected -> return SourceHttpResult.Failure(decision.error)
            is SourceUrlDecision.Allowed -> decision.url
        }
        val mediaType = request.headers["content-type"]?.let { value ->
            value.toMediaTypeOrNull() ?: return failure("content-type", "invalid media type")
        }
        val deadlineNanos = System.nanoTime() +
            request.timeoutMillis.toLong() * NANOS_PER_MILLI

        return executeWithRedirects(
            request = request,
            initialUrl = initialUrl,
            mediaType = mediaType,
            deadlineNanos = deadlineNanos,
        )
    }

    private suspend fun executeWithRedirects(
        request: SourceHttpRequest,
        initialUrl: HttpUrl,
        mediaType: MediaType?,
        deadlineNanos: Long,
    ): SourceHttpResult {
        var currentUrl = initialUrl
        var currentMethod = request.method
        var currentBody = request.body
        var redirectCount = 0

        while (true) {
            val remainingMillis = remainingMillis(deadlineNanos)
                ?: return SourceHttpResult.Failure(SourceHttpError.Timeout)
            val result = executeSingle(
                request = request,
                url = currentUrl,
                method = currentMethod,
                body = currentBody,
                mediaType = mediaType,
                timeoutMillis = remainingMillis,
            )
            if (result !is SourceHttpResult.Success) {
                return result
            }

            val response = result.response
            if (response.statusCode !in REDIRECT_STATUS_CODES) {
                return result
            }
            val location = response.headers["location"] ?: return result
            if (redirectCount >= SourceHttpLimits.MAX_REDIRECTS) {
                return SourceHttpResult.Failure(
                    SourceHttpError.TooManyRedirects(SourceHttpLimits.MAX_REDIRECTS),
                )
            }

            val nextUrl = currentUrl.resolve(location)
                ?: return SourceHttpResult.Failure(
                    SourceHttpError.UrlRejected("redirect URL is invalid"),
                )
            currentUrl = when (val decision = networkPolicy.validate(nextUrl)) {
                is SourceUrlDecision.Rejected -> return SourceHttpResult.Failure(decision.error)
                is SourceUrlDecision.Allowed -> decision.url
            }
            redirectCount += 1

            if (
                response.statusCode == 303 ||
                (response.statusCode in REDIRECT_TO_GET_STATUS_CODES && currentMethod == SourceHttpMethod.POST)
            ) {
                currentMethod = SourceHttpMethod.GET
                currentBody = null
            }
        }
    }

    private suspend fun executeSingle(
        request: SourceHttpRequest,
        url: HttpUrl,
        method: SourceHttpMethod,
        body: String?,
        mediaType: MediaType?,
        timeoutMillis: Int,
    ): SourceHttpResult {
        val requestBody = body?.toRequestBody(mediaType)
        val okHttpRequest = try {
            Request.Builder()
                .url(url)
                .apply {
                    request.headers.forEach { (name, value) -> header(name, value) }
                    method(method.name, requestBody)
                }
                .build()
        } catch (_: IllegalArgumentException) {
            return failure("request", "request could not be created")
        }

        val timeout = timeoutMillis.toLong()
        val callClient = okHttpClient.newBuilder()
            .connectTimeout(timeout, TimeUnit.MILLISECONDS)
            .readTimeout(timeout, TimeUnit.MILLISECONDS)
            .writeTimeout(timeout, TimeUnit.MILLISECONDS)
            .callTimeout(timeout, TimeUnit.MILLISECONDS)
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

    private fun remainingMillis(deadlineNanos: Long): Int? {
        val remainingNanos = deadlineNanos - System.nanoTime()
        if (remainingNanos <= 0) {
            return null
        }
        return (remainingNanos / NANOS_PER_MILLI)
            .coerceAtLeast(1L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
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
        const val NANOS_PER_MILLI = 1_000_000L
        const val MAX_CONCURRENT_REQUESTS = 4
        const val MAX_CONCURRENT_REQUESTS_PER_HOST = 2
        val REDIRECT_STATUS_CODES = setOf(301, 302, 303, 307, 308)
        val REDIRECT_TO_GET_STATUS_CODES = setOf(301, 302)

        fun defaultClient(
            cookieJar: InMemoryCookieJar,
            networkPolicy: SourceNetworkPolicy,
            dns: Dns,
        ): OkHttpClient =
            OkHttpClient.Builder()
                .cache(null)
                .cookieJar(cookieJar)
                .dns(SourceValidatingDns(networkPolicy, dns))
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
    firstPolicyViolation()?.let { violation ->
        SourceHttpError.UrlRejected(violation.message.orEmpty())
    } ?: when (this) {
        is SocketTimeoutException -> SourceHttpError.Timeout
        is UnknownHostException -> SourceHttpError.DnsFailure
        is SSLException -> SourceHttpError.TlsFailure
        is ProtocolException -> SourceHttpError.ProtocolFailure
        is ConnectException -> SourceHttpError.ConnectionFailure
        is InterruptedIOException -> SourceHttpError.Timeout
        is IOException -> SourceHttpError.ConnectionFailure
        else -> SourceHttpError.ProtocolFailure
    }

private fun Throwable.firstPolicyViolation(): SourceNetworkPolicyException? {
    var current: Throwable? = this
    while (current != null) {
        if (current is SourceNetworkPolicyException) {
            return current
        }
        current = current.cause
    }
    return null
}
