package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.SourceId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalMangaApi::class)
class SourceHttpRequestTest {
    @Test
    fun getRequestNormalizesAndCopiesHeaders() {
        val mutableHeaders = mutableMapOf("Accept-Language" to "zh-CN")

        val request = SourceHttpRequest(
            sourceId = SourceId("source-a"),
            url = "https://example.test/search",
            headers = mutableHeaders,
        )
        mutableHeaders["Accept-Language"] = "en-US"

        assertEquals(mapOf("accept-language" to "zh-CN"), request.headers)
        assertEquals(SourceHttpLimits.DEFAULT_TIMEOUT_MILLIS, request.timeoutMillis)
    }

    @Test
    fun postRequestAllowsBoundedBody() {
        val request = SourceHttpRequest(
            sourceId = SourceId("source-a"),
            url = "https://example.test/search",
            method = SourceHttpMethod.POST,
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            body = "q=demo",
        )

        assertEquals(SourceHttpMethod.POST, request.method)
        assertEquals("q=demo", request.body)
    }

    @Test
    fun getRequestRejectsBody() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                body = "q=demo",
            )
        }
    }

    @Test
    fun requestRejectsOversizedBody() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                method = SourceHttpMethod.POST,
                body = "x".repeat(SourceHttpLimits.MAX_REQUEST_BODY_BYTES + 1),
            )
        }
    }

    @Test
    fun requestRejectsHostControlledHeaders() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                headers = mapOf("Host" to "internal.example.test"),
            )
        }
    }

    @Test
    fun requestRejectsHeaderInjectionAndDuplicateNames() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                headers = mapOf("Accept" to "text/html\r\nX-Injected: true"),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                headers = linkedMapOf("Accept" to "text/html", "accept" to "application/json"),
            )
        }
    }

    @Test
    fun requestRejectsTimeoutOutsideHostLimits() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpRequest(
                sourceId = SourceId("source-a"),
                url = "https://example.test/search",
                timeoutMillis = SourceHttpLimits.MAX_TIMEOUT_MILLIS + 1,
            )
        }
    }
}
