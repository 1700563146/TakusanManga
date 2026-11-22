package com.manganew.reader.source.api

import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ExperimentalMangaApi
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalMangaApi::class)
class SourceApiSampleTest {
    @Test
    fun sourceApiSampleCompilesAndKeepsStableIdentity() {
        val descriptor = SourceDescriptor(
            id = SourceId("sample-source"),
            name = "Sample",
            version = "1.0.0",
            format = SourceFormat.NATIVE_JSON,
            adapterApiVersion = 1,
            baseUrl = "https://sample.example.test",
            allowedHosts = setOf("sample.example.test", "img.example.test"),
            capabilities = setOf(SourceCapability.SEARCH, SourceCapability.DETAIL),
        )
        val ref = ComicRef(descriptor.id, RemoteId("comic-1"))

        assertEquals(descriptor.id, ref.sourceId)
    }
}
