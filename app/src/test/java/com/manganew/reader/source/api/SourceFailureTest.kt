package com.manganew.reader.source.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SourceFailureTest {
    @Test
    fun httpFailureAcceptsOnlyFailureStatusCodes() {
        assertEquals(429, SourceFailure.Http(429).statusCode)
        assertThrows(IllegalArgumentException::class.java) {
            SourceFailure.Http(200)
        }
    }

    @Test
    fun diagnosticFieldsRejectBlankAndMultilineValues() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceFailure.Parse(stage = "detail.title", detail = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceFailure.InvalidInput(
                field = "comicRef",
                reason = "source mismatch\nretry",
            )
        }
    }

    @Test
    fun testResultSuccessAndFailureAreExplicit() {
        val success = SourceTestResult.Success(latencyMillis = 42, message = "OK")
        val failure = SourceTestResult.Failure(
            failure = SourceFailure.Timeout,
            message = "Connection timed out",
        )

        assertTrue(success.isSuccess)
        assertFalse(failure.isSuccess)
    }

    @Test
    fun testResultRejectsNegativeLatencyAndBlankMessages() {
        assertThrows(IllegalArgumentException::class.java) {
            SourceTestResult.Success(latencyMillis = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SourceTestResult.Failure(
                failure = SourceFailure.EmptyResult,
                message = " ",
            )
        }
    }
}
