package com.manganew.reader.data.fake

import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.model.ImageResource
import com.manganew.reader.core.model.PageResult
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.core.repository.MangaRepository
import com.manganew.reader.core.repository.ReaderChapterSnapshot
import com.manganew.reader.source.api.InstalledSourceSummary
import com.manganew.reader.source.api.MangaSource
import com.manganew.reader.source.api.SourceFailure
import com.manganew.reader.source.api.SourceTestResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** In-memory repository wiring one fake MangaSource to every feature ViewModel. */
internal class FakeMangaRepository(
    private val source: FakeMangaSource = FakeMangaSource(),
) : MangaRepository {
    private val _installedSources =
        MutableStateFlow<List<InstalledSourceSummary>>(emptyList())
    override val installedSources: StateFlow<List<InstalledSourceSummary>> =
        _installedSources.asStateFlow()

    override fun observeInstalledSources(): StateFlow<List<InstalledSourceSummary>> =
        installedSources

    override suspend fun listInstalledSources(): List<InstalledSourceSummary> =
        installedSources.value

    override suspend fun getInstalledSource(sourceId: SourceId): InstalledSourceSummary? =
        installedSources.value.firstOrNull { it.descriptor.id == sourceId }

    override suspend fun installSampleSource() {
        delay(450)
        source.prepareForInstall()
        val descriptor = source.descriptor
        _installedSources.update { current ->
            if (current.any { it.descriptor.id == descriptor.id }) {
                current
            } else {
                current + InstalledSourceSummary(descriptor = descriptor, enabled = true)
            }
        }
    }

    override suspend fun setEnabled(sourceId: SourceId, enabled: Boolean) {
        _installedSources.update { current ->
            current.map { summary ->
                if (summary.descriptor.id == sourceId) summary.copy(enabled = enabled) else summary
            }
        }
    }

    override suspend fun remove(sourceId: SourceId) {
        delay(260)
        _installedSources.update { current ->
            current.filterNot { it.descriptor.id == sourceId }
        }
        if (source.descriptor.id == sourceId) {
            source.close()
        }
    }

    override suspend fun testConnection(sourceId: SourceId): SourceTestResult {
        if (getInstalledSource(sourceId) == null) {
            return SourceTestResult.Failure(
                failure = SourceFailure.PermissionDenied("source.installed"),
            )
        }
        return source.testConnection()
    }

    override suspend fun getSource(sourceId: SourceId): MangaSource {
        requireEnabled(sourceId)
        return source
    }

    override suspend fun reimportSource(sourceId: SourceId) {
        delay(420)
        source.reimport()
        _installedSources.update { current ->
            current.map { summary ->
                if (summary.descriptor.id == sourceId) {
                    summary.copy(descriptor = source.descriptor, enabled = true)
                } else {
                    summary
                }
            }
        }
    }

    override suspend fun search(
        keyword: String,
        pageToken: String?,
    ): PageResult<ComicSummary> {
        if (enabledSource() == null) return PageResult(emptyList())
        return source.search(keyword, pageToken)
    }

    override suspend fun getDetail(ref: ComicRef): ComicDetail {
        requireEnabled(ref.sourceId)
        return source.getDetail(ref)
    }

    override suspend fun getChapters(ref: ComicRef): List<Chapter> {
        requireEnabled(ref.sourceId)
        return source.getChapters(ref)
    }

    override suspend fun getPages(chapter: Chapter): List<ImageResource> {
        requireEnabled(chapter.sourceId)
        return source.getPages(chapter)
    }

    override suspend fun getReaderChapter(
        sourceId: SourceId,
        comicId: RemoteId,
        chapterId: RemoteId,
    ): ReaderChapterSnapshot? {
        requireEnabled(sourceId)
        val chapter = source.findChapter(comicId, chapterId) ?: return null
        return ReaderChapterSnapshot(
            chapter = chapter,
            pages = source.getPages(chapter),
        )
    }

    fun resetForTests() {
        source.reset()
        _installedSources.value = emptyList()
    }

    private fun enabledSource(): MangaSource? =
        installedSources.value.firstOrNull { it.enabled }?.let { source }

    private fun requireEnabled(sourceId: SourceId) {
        val summary = installedSources.value.firstOrNull { it.descriptor.id == sourceId }
        require(summary != null && summary.enabled) {
            "Source is not installed or enabled: $sourceId"
        }
    }
}
