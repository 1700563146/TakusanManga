package com.manganew.reader.data

import com.manganew.reader.core.repository.MangaRepository
import com.manganew.reader.data.fake.FakeMangaRepository

/** Single composition-root repository for the in-memory UI prototype. */
internal object AppRepositoryContainer {
    val repository: MangaRepository = FakeMangaRepository()

    fun resetForTests() {
        (repository as FakeMangaRepository).resetForTests()
    }
}
