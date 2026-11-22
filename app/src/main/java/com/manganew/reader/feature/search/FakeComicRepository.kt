package com.manganew.reader.feature.search

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.delay

/**
 * Local-only catalog for the search/detail UI prototype.
 *
 * No network, source rule, or persisted content is used.
 */
internal object FakeComicRepository {
    private val catalog = listOf(
        PrototypeComic(
            id = "star-traveler",
            title = "星海旅人",
            author = "林舟",
            summary = "一艘失联多年的探索舰，在星海边缘重新发出信号。",
            tags = listOf("科幻", "冒险"),
            description = "维修师岚在一次例行巡检中发现来自禁区的微弱信号。为了寻找失踪的姐姐，她加入了一支临时探索队，驶向地图之外的星海。",
            chapters = listOf(
                PrototypeChapter("star-1", "第 1 话 · 失联信号", "2026-11-02"),
                PrototypeChapter("star-2", "第 2 话 · 边缘航线", "2026-11-09"),
                PrototypeChapter("star-3", "第 3 话 · 静默舰队", "2026-11-16"),
                PrototypeChapter("star-4", "第 4 话 · 星光尽头", "2026-11-21"),
            ),
        ),
        PrototypeComic(
            id = "rain-city-detective",
            title = "雨城侦探",
            author = "周寻",
            summary = "终年落雨的城市里，一位侦探专门调查被遗忘的小事。",
            tags = listOf("悬疑", "都市"),
            description = "雨城的档案室丢失了一页旧案记录。侦探顾遥与见习助手沿着一段被涂改的报纸剪影，找到一桩没有人愿意再提起的往事。",
            chapters = listOf(
                PrototypeChapter("rain-1", "第 1 话 · 潮湿的委托", "2026-10-24"),
                PrototypeChapter("rain-2", "第 2 话 · 缺失的一页", "2026-11-01"),
                PrototypeChapter("rain-3", "第 3 话 · 伞下证人", "2026-11-12"),
            ),
        ),
        PrototypeComic(
            id = "cat-bookshop",
            title = "猫与旧书店",
            author = "白川",
            summary = "一家只在黄昏营业的旧书店，和一只会挑选读者的猫。",
            tags = listOf("治愈", "日常"),
            description = "新搬来的夏织误入一间藏在巷子尽头的旧书店。店主是一只橘猫，而每一本被它选中的书，都记录着一段尚未说完的故事。",
            chapters = listOf(
                PrototypeChapter("cat-1", "第 1 话 · 黄昏营业", "2026-11-05"),
                PrototypeChapter("cat-2", "第 2 话 · 橘猫选书", "2026-11-13"),
                PrototypeChapter("cat-3", "第 3 话 · 未寄出的信", "2026-11-20"),
            ),
        ),
    )

    suspend fun search(query: String): List<PrototypeComicSummary> {
        delay(480)
        return filterComics(query)
    }

    suspend fun detail(comicId: String): PrototypeComicDetail? {
        delay(260)
        return catalog.firstOrNull { it.id == comicId }?.toDetail()
    }

    internal fun filterComics(query: String): List<PrototypeComicSummary> {
        val normalized = query.trim()
        if (normalized.isEmpty()) return emptyList()
        return catalog.filter { comic ->
            comic.title.contains(normalized, ignoreCase = true) ||
                comic.author.contains(normalized, ignoreCase = true) ||
                comic.tags.any { it.contains(normalized, ignoreCase = true) }
        }.map(PrototypeComic::toSummary)
    }
}

@Immutable
internal data class PrototypeComicSummary(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val tags: List<String>,
)

@Immutable
internal data class PrototypeChapter(
    val id: String,
    val title: String,
    val updatedAt: String,
)

@Immutable
internal data class PrototypeComicDetail(
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val tags: List<String>,
    val chapters: List<PrototypeChapter>,
)

private data class PrototypeComic(
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val tags: List<String>,
    val description: String,
    val chapters: List<PrototypeChapter>,
) {
    fun toSummary() = PrototypeComicSummary(
        id = id,
        title = title,
        author = author,
        summary = summary,
        tags = tags,
    )

    fun toDetail() = PrototypeComicDetail(
        id = id,
        title = title,
        author = author,
        description = description,
        tags = tags,
        chapters = chapters,
    )
}
