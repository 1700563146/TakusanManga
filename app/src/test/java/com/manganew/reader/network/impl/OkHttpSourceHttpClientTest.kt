package com.manganew.reader.network.impl

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.network.api.SourceHttpLimits
import com.manganew.reader.network.api.SourceHttpMethod
import com.manganew.reader.network.api.SourceHttpRequest
import com.manganew.reader.network.api.SourceHttpResult
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMangaApi::class)
class OkHttpSourceHttpClientTest {
    @get:Rule
    val serverRule = MockWebServerRule()

    private val server
        get() = serverRule.server

    @Test
    fun returns200AndErrorStatusesWithoutThrowing() = runBlocking {
        OkHttpSourceHttpClient(SourceA).use { client ->
            listOf(200, 404, 503).forEach { status ->
                server.enqueue(
                    MockResponse.Builder()
                        .code(status)
                        .setHeader("Content-Type", "text/html; charset=utf-8")
                        .body("status-$status")
                        .build(),
                )

                val result = client.execute(request())

                assertTrue(result is SourceHttpResult.Success)
                val response = (result as SourceHttpResult.Success).response
                assertEquals(status, response.statusCode)
                assertEquals("status-$status", response.body)
            }
        }
    }

    @Test
    fun sendsHeadersAndPostBodyAndFiltersSensitiveResponseHeaders() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .setHeader("Content-Type", "application/json")
                .setHeader("Set-Cookie", "session=secret")
                .body("""{"ok":true}""")
                .build(),
        )
        OkHttpSourceHttpClient(SourceA).use { client ->
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = SourceA,
                    url = server.url("/search").toString(),
                    method = SourceHttpMethod.POST,
                    headers = mapOf(
                        "Content-Type" to "application/x-www-form-urlencoded",
                        "X-Test" to "present",
                    ),
                    body = "q=demo",
                ),
            )

            assertTrue(result is SourceHttpResult.Success)
            val response = (result as SourceHttpResult.Success).response
            assertFalse(response.headers.containsKey("set-cookie"))

            val recorded = server.takeRequest(1, TimeUnit.SECONDS) ?: error("request not received")
            assertEquals("POST", recorded.method)
            assertEquals("present", recorded.headers["X-Test"])
            assertEquals("q=demo", recorded.body?.utf8())
        }
    }

    @Test
    fun isolatesCookiesBetweenSourceClients() = runBlocking {
        OkHttpSourceHttpClient(SourceA).use { clientA ->
            OkHttpSourceHttpClient(SourceB).use { clientB ->
                server.enqueue(
                    MockResponse.Builder()
                        .code(200)
                        .setHeader("Set-Cookie", "session=source-a; Path=/")
                        .body("set")
                        .build(),
                )
                server.enqueue(MockResponse.Builder().code(200).body("a").build())
                server.enqueue(MockResponse.Builder().code(200).body("b").build())

                val firstResult = clientA.execute(request())
                val secondResult = clientA.execute(request(path = "/cookie-check"))
                val isolatedResult = clientB.execute(request(sourceId = SourceB, path = "/cookie-check"))
                assertTrue("first=$firstResult", firstResult is SourceHttpResult.Success)
                assertTrue("second=$secondResult", secondResult is SourceHttpResult.Success)
                assertTrue("isolated=$isolatedResult", isolatedResult is SourceHttpResult.Success)

                server.takeRequest(1, TimeUnit.SECONDS)
                val requestA = server.takeRequest(1, TimeUnit.SECONDS) ?: error("A request missing")
                val requestB = server.takeRequest(1, TimeUnit.SECONDS) ?: error("B request missing")
                assertTrue(requestA.headers["Cookie"].orEmpty().contains("session=source-a"))
                assertNull(requestB.headers["Cookie"])
            }
        }
    }

    @Test
    fun timesOutSlowResponse() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("slow")
                .bodyDelay(2, TimeUnit.SECONDS)
                .build(),
        )
        OkHttpSourceHttpClient(SourceA).use { client ->
            val result = client.execute(request(timeoutMillis = 100))

            assertTrue(result is SourceHttpResult.Failure)
            assertEquals(SourceHttpError.Timeout, (result as SourceHttpResult.Failure).error)
        }
    }

    @Test
    fun cancellationCancelsUnderlyingCall() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("slow")
                .bodyDelay(5, TimeUnit.SECONDS)
                .build(),
        )
        OkHttpSourceHttpClient(SourceA).use { client ->
            val job = launch {
                try {
                    client.execute(request(timeoutMillis = 10_000))
                    fail("canceled request returned normally")
                } catch (_: CancellationException) {
                    // Expected cancellation propagates through the caller coroutine.
                }
            }
            delay(100)
            job.cancelAndJoin()

            assertTrue(job.isCancelled)
        }
    }

    @Test
    fun rejectsOversizedStreamingResponse() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .body("x".repeat(SourceHttpLimits.MAX_RESPONSE_BODY_BYTES + 1))
                .build(),
        )
        server.enqueue(MockResponse.Builder().code(200).body("next").build())
        OkHttpSourceHttpClient(SourceA).use { client ->
            val result = client.execute(request())

            assertTrue(result is SourceHttpResult.Failure)
            assertEquals(
                SourceHttpError.ResponseTooLarge(SourceHttpLimits.MAX_RESPONSE_BODY_BYTES),
                (result as SourceHttpResult.Failure).error,
            )
            assertTrue(client.execute(request(path = "/next")) is SourceHttpResult.Success)
        }
    }

    @Test
    fun rejectsWrongSourceAndKeepsNoDiskCache() = runBlocking {
        OkHttpSourceHttpClient(SourceA).use { client ->
            assertNull(client.okHttpClient.cache)
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = SourceB,
                    url = server.url("/unused").toString(),
                ),
            )

            assertTrue(result is SourceHttpResult.Failure)
            assertEquals(
                SourceHttpError.InvalidRequest(
                    field = "sourceId",
                    reason = "request source does not match client source",
                ),
                (result as SourceHttpResult.Failure).error,
            )
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun closeIsIdempotentAndRejectsFurtherRequests() = runBlocking {
        val client = OkHttpSourceHttpClient(SourceA)
        client.close()
        client.close()

        val result = client.execute(request())

        assertEquals(
            SourceHttpError.InvalidRequest(
                field = "client",
                reason = "client is closed",
            ),
            (result as SourceHttpResult.Failure).error,
        )
        assertEquals(0, server.requestCount)
    }

    private fun request(
        sourceId: SourceId = SourceA,
        path: String = "/",
        timeoutMillis: Int = 5_000,
    ): SourceHttpRequest =
        SourceHttpRequest(
            sourceId = sourceId,
            url = server.url(path).toString(),
            timeoutMillis = timeoutMillis,
        )

    private companion object {
        val SourceA = SourceId("source-a")
        val SourceB = SourceId("source-b")
    }
}
