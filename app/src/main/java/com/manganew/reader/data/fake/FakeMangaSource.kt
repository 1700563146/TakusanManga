package com.manganew.reader.data.fake

import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.model.ImageResource
import com.manganew.reader.core.model.PageResult
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.source.api.MangaSource
import com.manganew.reader.source.api.SourceCapability
import com.manganew.reader.source.api.SourceDescriptor
import com.manganew.reader.source.api.SourceFormat
import com.manganew.reader.source.api.SourceTestResult
import kotlinx.coroutines.delay

/**
 * Local source used only to exercise the host contracts and UI prototype.
 *
 * It performs no network or file I/O. Image resources contain remote-looking
 * URLs but the prototype Reader never fetches them.
 */
internal class FakeMangaSource : MangaSource {
    private var version = "1.0.0"
    private var closed = false

    override val descriptor: SourceDescriptor
        get() = SourceDescriptor(
            id = SourceId(SourceIdValue),
            name = "示例漫画站",
            version = version,
            format = SourceFormat.NATIVE_JSON,
            adapterApiVersion = 1,
            baseUrl = "https://example.test",
            allowedHosts = setOf("example.test", "images.example.test"),
            capabilities = SourceCapability.entries.toSet(),
        )

    override suspend fun search(
        keyword: String,
        pageToken: String?,
    ): PageResult<ComicSummary> {
        checkOpen()
        delay(480)
        if (keyword.isBlank()) return PageResult(emptyList())
        val items = catalog
            .filter { item ->
                item.title.contains(keyword, ignoreCase = true) ||
                    item.author.contains(keyword, ignoreCase = true) ||
                    item.tags.any { it.contains(keyword, ignoreCase = true) }
            }
            .map(ComicEntry::toSummary)
        return PageResult(items = items)
    }

    override suspend fun getDetail(ref: ComicRef): ComicDetail {
        checkOpen()
        requireSource(ref.sourceId)
        delay(260)
        return findComic(ref.remoteId)?.toDetail()
            ?: throw IllegalArgumentException("Unknown fake comic: ${ref.remoteId}")
    }

    override suspend fun getChapters(ref: ComicRef): List<Chapter> {
        checkOpen()
        requireSource(ref.sourceId)
        delay(120)
        return findComic(ref.remoteId)?.chapters
            ?.map { chapter -> chapter.toModel(ref.remoteId) }
            ?: throw IllegalArgumentException("Unknown fake comic: ${ref.remoteId}")
    }

    override suspend fun getPages(chapter: Chapter): List<ImageResource> {
        checkOpen()
        requireSource(chapter.sourceId)
        delay(180)
        require(chapter.comicId.value in catalog.map { it.id }) {
            "Unknown fake comic: ${chapter.comicId}"
        }
        require(findChapter(chapter.comicId, chapter.chapterId) != null) {
            "Unknown fake chapter: ${chapter.chapterId}"
        }
        return (1..8).map { page ->
            ImageResource(
                url = "https://images.example.test/${chapter.chapterId.value}/page-$page.jpg",
                referer = "https://example.test/reader/${chapter.comicId.value}",
                headers = mapOf("Accept" to "image/*"),
                mimeTypeHint = "image/jpeg",
            )
        }
    }

    override suspend fun testConnection(): SourceTestResult {
        checkOpen()
        delay(520)
        return SourceTestResult.Success(latencyMillis = 18)
    }

    override suspend fun close() {
        closed = true
    }

    fun findChapter(
        comicId: RemoteId,
        chapterId: RemoteId,
    ): Chapter? = findComic(comicId)
        ?.chapters
        ?.firstOrNull { it.id == chapterId.value }
        ?.toModel(comicId)

    fun reimport() {
        version = "1.0.1"
        closed = false
    }

    fun prepareForInstall() {
        closed = false
    }

    fun reset() {
        version = "1.0.0"
        closed = false
    }

    private fun checkOpen() {
        check(!closed) { "Fake source is closed" }
    }

    private fun requireSource(sourceId: SourceId) {
        require(sourceId.value == SourceIdValue) {
            "ComicRef source does not match this MangaSource"
        }
    }

    private fun findComic(remoteId: RemoteId): ComicEntry? =
        catalog.firstOrNull { it.id == remoteId.value }

    private data class ComicEntry(
        val id: String,
        val title: String,
        val author: String,
        val summary: String,
        val tags: List<String>,
        val description: String,
        val chapters: List<ChapterEntry>,
    ) {
        fun toSummary(): ComicSummary = ComicSummary(
            ref = ComicRef(SourceId(SourceIdValue), RemoteId(id)),
            title = title,
            cover = cover(id),
            subtitle = summary,
            author = author,
            status = "连载中",
            latestChapter = chapters.lastOrNull()?.title,
        )

        fun toDetail(): ComicDetail = ComicDetail(
            ref = ComicRef(SourceId(SourceIdValue), RemoteId(id)),
            title = title,
            cover = cover(id),
            author = author,
            description = description,
            tags = tags,
            status = "连载中",
        )
    }

    private data class ChapterEntry(
        val id: String,
        val title: String,
        val order: Int,
        val updatedAt: String,
    ) {
        fun toModel(comicId: RemoteId): Chapter = Chapter(
            sourceId = SourceId(SourceIdValue),
            comicId = comicId,
            chapterId = RemoteId(id),
            title = title,
            order = order,
            publishedAt = updatedAt,
        )
    }

    private companion object {
        const val SourceIdValue = "demo-source"

        fun cover(comicId: String): ImageResource = ImageResource(
            url = "https://images.example.test/$comicId/cover.jpg",
            referer = "https://example.test/comic/$comicId",
            mimeTypeHint = "image/jpeg",
        )

        val catalog = listOf(
            ComicEntry(
                id = "star-traveler",
                title = "星海旅人",
                author = "林舟",
                summary = "一艘失联多年的探索舰，在星海边缘重新发出信号。",
                tags = listOf("科幻", "冒险"),
                description = "维修师岚在一次例行巡检中发现来自禁区的微弱信号。为了寻找失踪的姐姐，她加入了一支临时探索队，驶向地图之外的星海。",
                chapters = listOf(
                    ChapterEntry("star-1", "第 1 话 · 失联信号", 1, "2026-11-02"),
                    ChapterEntry("star-2", "第 2 话 · 边缘航线", 2, "2026-11-09"),
                    ChapterEntry("star-3", "第 3 话 · 静默舰队", 3, "2026-11-16"),
                    ChapterEntry("star-4", "第 4 话 · 星光尽头", 4, "2026-11-21"),
                ),
            ),
            ComicEntry(
                id = "rain-city-detective",
                title = "雨城侦探",
                author = "周寻",
                summary = "终年落雨的城市里，一位侦探专门调查被遗忘的小事。",
                tags = listOf("悬疑", "都市"),
                description = "雨城档案室丢失了一页旧案记录，侦探顾遥沿着被涂改的报纸剪影追查一段无人愿意提起的往事。",
                chapters = listOf(
                    ChapterEntry("rain-1", "第 1 话 · 潮湿的委托", 1, "2026-10-24"),
                    ChapterEntry("rain-2", "第 2 话 · 缺失的一页", 2, "2026-11-01"),
                    ChapterEntry("rain-3", "第 3 话 · 伞下证人", 3, "2026-11-12"),
                ),
            ),
            ComicEntry(
                id = "cat-bookshop",
                title = "猫与旧书店",
                author = "白川",
                summary = "一家只在黄昏营业的旧书店，和一只会挑选读者的猫。",
                tags = listOf("治愈", "日常"),
                description = "夏织误入巷子尽头的旧书店。店主是一只橘猫，而每一本被它选中的书都记录着尚未说完的故事。",
                chapters = listOf(
                    ChapterEntry("cat-1", "第 1 话 · 黄昏营业", 1, "2026-11-05"),
                    ChapterEntry("cat-2", "第 2 话 · 橘猫选书", 2, "2026-11-13"),
                    ChapterEntry("cat-3", "第 3 话 · 未寄出的信", 3, "2026-11-20"),
                ),
            ),
        )
    }
}
