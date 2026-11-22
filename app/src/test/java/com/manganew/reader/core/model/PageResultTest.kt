package com.manganew.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PageResultTest {
    @Test
    fun morePagesAreDerivedFromOpaqueToken() {
        val page = PageResult(
            items = listOf("second", "first"),
            nextPageToken = "cursor:/comic?page=2",
        )

        assertTrue(page.hasMore)
        assertEquals("cursor:/comic?page=2", page.nextPageToken)
        assertEquals(listOf("second", "first"), page.items)
    }

    @Test
    fun lastPageHasNoMoreAndNoToken() {
        val page = PageResult(items = listOf("only"))

        assertFalse(page.hasMore)
        assertNull(page.nextPageToken)
    }

    @Test
    fun blankOrPaddedTokenIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            PageResult(items = emptyList<String>(), nextPageToken = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            PageResult(items = emptyList<String>(), nextPageToken = " cursor ")
        }
    }
}
