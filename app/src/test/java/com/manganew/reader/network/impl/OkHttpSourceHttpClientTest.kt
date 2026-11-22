package com.manganew.reader.network.impl

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.network.api.SourceHttpError
import com.manganew.reader.network.api.SourceHttpLimits
import com.manganew.reader.network.api.SourceHttpMethod
import com.manganew.reader.network.api.SourceHttpRequest
import com.manganew.reader.network.api.SourceHttpResult
import com.manganew.reader.source.api.SourceCapability
import com.manganew.reader.source.api.SourceDescriptor
import com.manganew.reader.source.api.SourceFormat
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.junit4.MockWebServerRule
import okhttp3.Dns
import okhttp3.HttpUrl
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

    private val sourceA = descriptor("source-a")
    private val sourceB = descriptor("source-b")

    @Test
    fun returns200AndErrorStatusesWithoutThrowing() = runBlocking {
        client().use { client ->
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
    fun followsAuthorizedRedirectAndValidatesEachHop() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .setHeader("Location", "/final")
                .build(),
        )
        server.enqueue(MockResponse.Builder().code(200).body("final").build())

        client().use { client ->
            val result = client.execute(request())

            assertTrue(result is SourceHttpResult.Success)
            val response = (result as SourceHttpResult.Success).response
            assertEquals("final", response.body)
            assertEquals(testUrl("/final").toString(), response.finalUrl)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun rejectsRedirectToUnauthorizedHostAndNonHttpScheme() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .setHeader("Location", "https://evil.example.test/steal")
                .build(),
        )
        client().use { client ->
            val result = client.execute(request())

            assertTrue(result is SourceHttpResult.Failure)
            assertTrue((result as SourceHttpResult.Failure).error is SourceHttpError.UrlRejected)
            assertEquals(1, server.requestCount)
        }

        server.enqueue(
            MockResponse.Builder()
                .code(302)
                .setHeader("Location", "file:///etc/passwd")
                .build(),
        )
        client().use { client ->
            val result = client.execute(request())

            assertTrue(result is SourceHttpResult.Failure)
            assertTrue((result as SourceHttpResult.Failure).error is SourceHttpError.UrlRejected)
            assertEquals(2, server.requestCount)
        }
    }

    @Test
    fun rejectsPrivateResolvedAddressBeforeConnection() = runBlocking {
        client(allowLoopbackForTests = false).use { client ->
            val result = client.execute(request())

            assertEquals(
                SourceHttpError.UrlRejected("resolved address is not public"),
                (result as SourceHttpResult.Failure).error,
            )
            assertEquals(0, server.requestCount)
        }
    }

    @Test
    fun stopsAfterFiveRedirects() = runBlocking {
        repeat(SourceHttpLimits.MAX_REDIRECTS + 1) {
            server.enqueue(
                MockResponse.Builder()
                    .code(302)
                    .setHeader("Location", "/loop")
                    .build(),
            )
        }

        client().use { client ->
            val result = client.execute(request())

            assertEquals(
                SourceHttpError.TooManyRedirects(SourceHttpLimits.MAX_REDIRECTS),
                (result as SourceHttpResult.Failure).error,
            )
            assertEquals(SourceHttpLimits.MAX_REDIRECTS + 1, server.requestCount)
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
        client().use { client ->
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = sourceA.id,
                    url = testUrl("/search").toString(),
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
        client(sourceA).use { clientA ->
            client(sourceB).use { clientB ->
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
                val isolatedResult = clientB.execute(
                    request(sourceId = sourceB.id, path = "/cookie-check"),
                )
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
        client().use { client ->
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
        client().use { client ->
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
        client().use { client ->
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
        client().use { client ->
            assertNull(client.okHttpClient.cache)
            val result = client.execute(
                SourceHttpRequest(
                    sourceId = sourceB.id,
                    url = testUrl("/unused").toString(),
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
        val client = client()
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
        sourceId: SourceId = sourceA.id,
        path: String = "/",
        timeoutMillis: Int = 5_000,
    ): SourceHttpRequest =
        SourceHttpRequest(
            sourceId = sourceId,
            url = testUrl(path).toString(),
            timeoutMillis = timeoutMillis,
        )

    private fun client(
        source: SourceDescriptor = sourceA,
        allowLoopbackForTests: Boolean = true,
    ): OkHttpSourceHttpClient =
        OkHttpSourceHttpClient(
            networkPolicy = SourceNetworkPolicy(
                descriptor = source,
                allowLoopbackForTests = allowLoopbackForTests,
                allowedPortsForTests = setOf(server.port),
            ),
            dns = object : Dns {
                override fun lookup(hostname: String): List<InetAddress> =
                    listOf(server.socketAddress.address)
            },
        )

    private fun testUrl(path: String = "/"): HttpUrl =
        HttpUrl.Builder()
            .scheme("http")
            .host(SourceHost)
            .port(server.port)
            .addPathSegments(path.trimStart('/'))
            .build()

    private fun descriptor(id: String): SourceDescriptor =
        SourceDescriptor(
            id = SourceId(id),
            name = "Test source",
            version = "1.0.0",
            format = SourceFormat.NATIVE_JSON,
            adapterApiVersion = 1,
            baseUrl = "https://$SourceHost",
            allowedHosts = setOf(SourceHost, "images.example.test"),
            capabilities = setOf(SourceCapability.SEARCH),
            allowHttp = true,
        )

    private companion object {
        const val SourceHost = "source.example.test"
    }
}
