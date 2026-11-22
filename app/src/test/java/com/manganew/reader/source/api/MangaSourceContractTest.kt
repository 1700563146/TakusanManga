package com.manganew.reader.source.api

import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.model.ImageResource
import com.manganew.reader.core.model.PageResult
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaSourceContractTest {
    @Test
    fun sourceContractCanReturnAllCoreModelsWithoutPlatformTypes() = runBlocking {
        val source = StubMangaSource()
        val ref = ComicRef(source.descriptor.id, RemoteId("comic-1"))
        val chapter = Chapter(
            sourceId = source.descriptor.id,
            comicId = ref.remoteId,
            chapterId = RemoteId("chapter-1"),
            title = "第 1 话",
        )

        val search = source.search("demo")
        val detail = source.getDetail(ref)
        val chapters = source.getChapters(ref)
        val pages = source.getPages(chapter)
        val testResult = source.testConnection()

        assertEquals(source.descriptor.id, search.items.single().ref.sourceId)
        assertEquals(ref, detail.ref)
        assertEquals(source.descriptor.id, chapters.single().sourceId)
        assertEquals(1, pages.size)
        assertTrue(testResult.isSuccess)
    }

    @Test
    fun closeIsIdempotentInContractUsage() = runBlocking {
        val source = StubMangaSource()

        source.close()
        source.close()

        assertEquals(1, source.closeCalls)
    }

    private class StubMangaSource : MangaSource {
        override val descriptor = SourceDescriptor(
            id = SourceId("stub-source"),
            name = "Stub",
            version = "1.0.0",
            format = SourceFormat.NATIVE_JSON,
            adapterApiVersion = 1,
            baseUrl = "https://stub.example.test",
            allowedHosts = setOf("stub.example.test"),
            capabilities = SourceCapability.entries.toSet(),
        )

        var closeCalls = 0
            private set
        private var closed = false

        override suspend fun search(
            keyword: String,
            pageToken: String?,
        ): PageResult<ComicSummary> {
            val ref = ComicRef(descriptor.id, RemoteId("comic-1"))
            return PageResult(listOf(ComicSummary(ref = ref, title = keyword)))
        }

        override suspend fun getDetail(ref: ComicRef): ComicDetail =
            ComicDetail(ref = ref, title = "Demo")

        override suspend fun getChapters(ref: ComicRef): List<Chapter> = listOf(
            Chapter(
                sourceId = descriptor.id,
                comicId = ref.remoteId,
                chapterId = RemoteId("chapter-1"),
                title = "第 1 话",
            ),
        )

        override suspend fun getPages(chapter: Chapter): List<ImageResource> = listOf(
            ImageResource("https://stub.example.test/page-1.jpg"),
        )

        override suspend fun testConnection(): SourceTestResult =
            SourceTestResult.Success(latencyMillis = 10)

        override suspend fun close() {
            if (!closed) {
                closed = true
                closeCalls += 1
            }
        }
    }
}
