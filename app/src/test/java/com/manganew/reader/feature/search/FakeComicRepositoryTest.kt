package com.manganew.reader.feature.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeComicRepositoryTest {
    @Test
    fun findsComicsByTitle() {
        val results = FakeComicRepository.filterComics("星海")

        assertEquals(listOf("星海旅人"), results.map { it.title })
    }

    @Test
    fun findsComicsByAuthor() {
        val results = FakeComicRepository.filterComics("周寻")

        assertEquals(listOf("雨城侦探"), results.map { it.title })
    }

    @Test
    fun findsComicsByTag() {
        val results = FakeComicRepository.filterComics("治愈")

        assertEquals(listOf("猫与旧书店"), results.map { it.title })
    }

    @Test
    fun returnsEmptyForBlankOrUnknownQueries() {
        assertTrue(FakeComicRepository.filterComics("").isEmpty())
        assertTrue(FakeComicRepository.filterComics("不存在的漫画").isEmpty())
    }
}
