package com.manganew.reader.network.api

import com.manganew.reader.core.model.ExperimentalMangaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMangaApi::class)
class SourceHttpResponseTest {
    @Test
    fun responseExposesBoundedTextMetadata() {
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://example.test/search?q=demo",
            headers = mapOf("Content-Type" to "Text/HTML; charset=utf-8"),
            body = "<html></html>",
        )

        assertEquals("text/html", response.contentType)
        assertEquals(13, response.bodySizeBytes)
        assertTrue(response.isSuccessful)
        assertEquals(mapOf("content-type" to "Text/HTML; charset=utf-8"), response.headers)
    }

    @Test
    fun httpErrorStatusRemainsAvailableToCaller() {
        val response = SourceHttpResponse(
            statusCode = 503,
            finalUrl = "https://example.test/search",
            body = "Unavailable",
        )

        assertFalse(response.isSuccessful)
    }

    @Test
    fun responseCopiesHeadersAndRejectsSensitiveValues() {
        val mutableHeaders = mutableMapOf("Content-Type" to "application/json")
        val response = SourceHttpResponse(
            statusCode = 200,
            finalUrl = "https://example.test/data",
            headers = mutableHeaders,
            body = "{}",
        )
        mutableHeaders["Content-Type"] = "text/plain"

        assertEquals(mapOf("content-type" to "application/json"), response.headers)
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpResponse(
                statusCode = 200,
                finalUrl = "https://example.test/data",
                headers = mapOf("Set-Cookie" to "session=secret"),
                body = "{}",
            )
        }
    }

    @Test
    fun responseRejectsInvalidStatusAndUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpResponse(statusCode = 99, finalUrl = "https://example.test", body = "")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpResponse(statusCode = 200, finalUrl = " ", body = "")
        }
    }

    @Test
    fun responseRejectsOversizedBody() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceHttpResponse(
                statusCode = 200,
                finalUrl = "https://example.test/large",
                body = "x".repeat(SourceHttpLimits.MAX_RESPONSE_BODY_BYTES + 1),
            )
        }
    }
}
