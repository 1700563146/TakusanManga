package com.manganew.reader.feature.source

import androidx.compose.runtime.Immutable

/** Presentation model for an installed source summary. */
@Immutable
internal data class PrototypeSource(
    val id: String,
    val name: String,
    val host: String,
    val format: String,
    val version: String,
    val enabled: Boolean,
    val capabilities: List<String>,
)

/** Presentation-level source test result. */
internal enum class SourceTestFeedback {
    Success,
    NotFound,
}
