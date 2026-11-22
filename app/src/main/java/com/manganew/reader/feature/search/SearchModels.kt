package com.manganew.reader.feature.search

import androidx.compose.runtime.Immutable

/** Presentation model for one search result. */
@Immutable
internal data class PrototypeComicSummary(
    val sourceId: String,
    val id: String,
    val title: String,
    val author: String,
    val summary: String,
    val tags: List<String>,
)
