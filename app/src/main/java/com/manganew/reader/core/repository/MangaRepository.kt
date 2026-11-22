package com.manganew.reader.core.repository

import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.model.ImageResource
import com.manganew.reader.core.model.PageResult
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.source.api.InstalledSourceSummary
import com.manganew.reader.source.api.SourceRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository contract consumed by feature ViewModels.
 *
 * This interface deliberately exposes no parser, HTTP client, plugin runtime,
 * or concrete fake source. Implementations may change without changing page
 * components.
 */
internal interface MangaRepository : SourceRepository {
    val installedSources: StateFlow<List<InstalledSourceSummary>>

    override fun observeInstalledSources(): StateFlow<List<InstalledSourceSummary>>

    /** Install one local sample source for the UI prototype. */
    suspend fun installSampleSource()

    /** Replace sample source metadata to simulate a re-import. */
    suspend fun reimportSource(sourceId: SourceId)

    suspend fun search(
        keyword: String,
        pageToken: String? = null,
    ): PageResult<ComicSummary>

    suspend fun getDetail(ref: ComicRef): ComicDetail
    suspend fun getChapters(ref: ComicRef): List<Chapter>
    suspend fun getPages(chapter: Chapter): List<ImageResource>

    suspend fun getReaderChapter(
        sourceId: SourceId,
        comicId: RemoteId,
        chapterId: RemoteId,
    ): ReaderChapterSnapshot?
}

internal data class ReaderChapterSnapshot(
    val chapter: Chapter,
    val pages: List<ImageResource>,
)
