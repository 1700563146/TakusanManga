package com.manganew.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class IdentityModelsTest {
    @Test
    fun comicRefsRemainDistinctAcrossSourcesForSameRemoteId() {
        val left = ComicRef(
            sourceId = SourceId("source-a"),
            remoteId = RemoteId("comic-1"),
        )
        val right = ComicRef(
            sourceId = SourceId("source-b"),
            remoteId = RemoteId("comic-1"),
        )

        assertNotEquals(left, right)
        assertEquals("comic-1", left.remoteId.value)
    }

    @Test
    fun opaqueIdsRejectBlankWhitespaceAndControlCharacters() {
        assertThrows(IllegalArgumentException::class.java) { SourceId("") }
        assertThrows(IllegalArgumentException::class.java) { SourceId(" source ") }
        assertThrows(IllegalArgumentException::class.java) { RemoteId("comic\n1") }
    }

    @Test
    fun opaqueIdsAllowNonNumericValues() {
        val sourceId = SourceId("github.example.source")
        val remoteId = RemoteId("/comic/海边的书店?edition=2")

        assertEquals("github.example.source", sourceId.value)
        assertEquals("/comic/海边的书店?edition=2", remoteId.value)
    }
}
