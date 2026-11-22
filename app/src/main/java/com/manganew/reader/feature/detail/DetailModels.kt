package com.manganew.reader.feature.detail

import androidx.compose.runtime.Immutable

/** Presentation model for comic detail and its chapter list. */
@Immutable
internal data class PrototypeComicDetail(
    val sourceId: String,
    val id: String,
    val title: String,
    val author: String,
    val description: String,
    val tags: List<String>,
    val chapters: List<PrototypeChapter>,
)

@Immutable
internal data class PrototypeChapter(
    val id: String,
    val title: String,
    val updatedAt: String,
)
