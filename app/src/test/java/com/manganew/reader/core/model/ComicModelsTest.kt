package com.manganew.reader.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ComicModelsTest {
    private val ref = ComicRef(
        sourceId = SourceId("demo-source"),
        remoteId = RemoteId("comic-1"),
    )

    @Test
    fun summaryRetainsNullableFieldsAndOptionalCover() {
        val cover = ImageResource("https://images.example.test/cover.jpg")
        val summary = ComicSummary(
            ref = ref,
            title = "示例漫画",
            cover = cover,
            subtitle = "副标题",
            author = "作者",
            status = "连载中",
            latestChapter = "第 12 话",
        )

        assertEquals(ref, summary.ref)
        assertEquals(cover, summary.cover)
        assertEquals("第 12 话", summary.latestChapter)
    }

    @Test
    fun detailPreservesTagOrderAndDisplayMetadata() {
        val detail = ComicDetail(
            ref = ref,
            title = "示例漫画",
            tags = listOf("科幻", "冒险"),
            metadata = mapOf("出版社" to "示例出版社"),
        )

        assertEquals(listOf("科幻", "冒险"), detail.tags)
        assertEquals("示例出版社", detail.metadata["出版社"])
    }

    @Test
    fun invalidDisplayTextAndChapterOrderAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            ComicSummary(ref = ref, title = " ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComicDetail(
                ref = ref,
                title = "示例漫画",
                tags = listOf(" "),
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            Chapter(
                sourceId = ref.sourceId,
                comicId = ref.remoteId,
                chapterId = RemoteId("chapter-1"),
                title = "第 1 话",
                order = -1,
            )
        }
    }

    @Test
    fun chapterKeepsPublishedDateAsDisplayText() {
        val chapter = Chapter(
            sourceId = ref.sourceId,
            comicId = ref.remoteId,
            chapterId = RemoteId("chapter-1"),
            title = "第 1 话",
            order = 1,
            publishedAt = "2026-11-22 08:00",
        )

        assertEquals("2026-11-22 08:00", chapter.publishedAt)
    }
}
