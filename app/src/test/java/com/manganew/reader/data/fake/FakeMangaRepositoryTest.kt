package com.manganew.reader.data.fake

import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.source.api.SourceRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMangaRepositoryTest {
    private val sourceId = SourceId("demo-source")

    @Test
    fun sourceMustBeInstalledAndEnabledBeforeContentOperations() = runBlocking {
        val repository = FakeMangaRepository()
        assertTrue(repository.search("星海").items.isEmpty())

        repository.installSampleSource()
        val result = repository.search("星海")

        assertEquals(1, result.items.size)
        assertEquals(sourceId, result.items.single().ref.sourceId)

        repository.setEnabled(sourceId, false)
        assertTrue(repository.search("星海").items.isEmpty())
    }

    @Test
    fun installedSourceCanLoadDetailChaptersAndPages() = runBlocking {
        val repository = FakeMangaRepository()
        repository.installSampleSource()
        val ref = ComicRef(sourceId, RemoteId("star-traveler"))

        val detail = repository.getDetail(ref)
        val chapters = repository.getChapters(ref)
        val pages = repository.getPages(chapters.first())

        assertEquals("星海旅人", detail.title)
        assertEquals(4, chapters.size)
        assertEquals(8, pages.size)
        assertTrue(pages.all { it.url.startsWith("https://") })
    }

    @Test
    fun removingSourceStopsContentAccess() = runBlocking {
        val repository = FakeMangaRepository()
        repository.installSampleSource()

        repository.remove(sourceId)

        assertNull(repository.getInstalledSource(sourceId))
        assertTrue(repository.search("星海").items.isEmpty())

        repository.installSampleSource()
        assertEquals(1, repository.search("星海").items.size)
    }

    @Test
    fun readerSnapshotUsesStableSourceComicAndChapterIds() = runBlocking {
        val repository = FakeMangaRepository()
        repository.installSampleSource()

        val snapshot = repository.getReaderChapter(
            sourceId = sourceId,
            comicId = RemoteId("star-traveler"),
            chapterId = RemoteId("star-1"),
        )

        assertEquals(sourceId, snapshot?.chapter?.sourceId)
        assertEquals(RemoteId("star-traveler"), snapshot?.chapter?.comicId)
        assertEquals(RemoteId("star-1"), snapshot?.chapter?.chapterId)
        assertEquals(8, snapshot?.pages?.size)
    }

    @Test
    fun publicSourceRepositoryExposesSummariesAndManagedRuntime() = runBlocking {
        val repository: SourceRepository = FakeMangaRepository()
        (repository as FakeMangaRepository).installSampleSource()

        val summaries = repository.listInstalledSources()
        val source = repository.getSource(sourceId)
        val testResult = repository.testConnection(sourceId)

        assertEquals(1, summaries.size)
        assertEquals(sourceId, source.descriptor.id)
        assertTrue(testResult.isSuccess)
    }
}
