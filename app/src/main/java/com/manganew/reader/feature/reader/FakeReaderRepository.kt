package com.manganew.reader.feature.reader

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.delay

/**
 * In-memory pages for the Reader UI prototype.
 *
 * Pages contain numbers only; their visual panels are drawn by Compose.
 */
internal object FakeReaderRepository {
    suspend fun loadChapter(chapterId: String): PrototypeReaderChapter? {
        if (chapterId.isBlank()) return null
        delay(220)
        return PrototypeReaderChapter(
            id = chapterId,
            title = chapterTitle(chapterId),
            pages = (1..8).map { number ->
                PrototypeReaderPage(
                    id = "$chapterId-page-$number",
                    number = number,
                    totalPages = 8,
                )
            },
        )
    }

    private fun chapterTitle(chapterId: String): String = when {
        chapterId.startsWith("star-1") -> "第 1 话 · 失联信号"
        chapterId.startsWith("rain-1") -> "第 1 话 · 潮湿的委托"
        chapterId.startsWith("cat-1") -> "第 1 话 · 黄昏营业"
        else -> "示例章节"
    }
}

@Immutable
internal data class PrototypeReaderChapter(
    val id: String,
    val title: String,
    val pages: List<PrototypeReaderPage>,
)

@Immutable
internal data class PrototypeReaderPage(
    val id: String,
    val number: Int,
    val totalPages: Int,
)

internal enum class ReaderMode {
    Paged,
    Vertical,
}
