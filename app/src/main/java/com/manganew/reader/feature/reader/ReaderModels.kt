package com.manganew.reader.feature.reader

import androidx.compose.runtime.Immutable

/** Presentation data for the Reader prototype. */
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
