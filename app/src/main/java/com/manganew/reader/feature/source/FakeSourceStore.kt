package com.manganew.reader.feature.source

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory data for the T012 prototype.
 *
 * This store must be replaced by the real SourceRepository before source
 * management becomes a production feature.
 */
internal object FakeSourceStore {
    private const val SampleSourceId = "demo-source"

    private val _sources = MutableStateFlow<List<PrototypeSource>>(emptyList())
    val sources: StateFlow<List<PrototypeSource>> = _sources.asStateFlow()

    suspend fun importSampleSource() {
        delay(450)
        _sources.update { current ->
            if (current.any { it.id == SampleSourceId }) {
                current
            } else {
                current + sampleSource()
            }
        }
    }

    suspend fun deleteSource(sourceId: String) {
        delay(260)
        _sources.update { sources -> sources.filterNot { it.id == sourceId } }
    }

    suspend fun testSource(sourceId: String): SourceTestFeedback {
        delay(520)
        return if (_sources.value.any { it.id == sourceId }) {
            SourceTestFeedback.Success
        } else {
            SourceTestFeedback.NotFound
        }
    }

    suspend fun reimportSource(sourceId: String) {
        delay(420)
        _sources.update { sources ->
            sources.map { source ->
                if (source.id == sourceId) {
                    source.copy(
                        version = "1.0.1",
                        enabled = true,
                    )
                } else {
                    source
                }
            }
        }
    }

    fun findSource(sourceId: String): PrototypeSource? =
        _sources.value.firstOrNull { it.id == sourceId }

    fun setEnabled(sourceId: String, enabled: Boolean) {
        _sources.update { sources ->
            sources.map { source ->
                if (source.id == sourceId) source.copy(enabled = enabled) else source
            }
        }
    }

    fun resetForTests() {
        _sources.value = emptyList()
    }

    private fun sampleSource() = PrototypeSource(
        id = SampleSourceId,
        name = "示例漫画站",
        host = "example.manga.local",
        format = "Native JSON",
        version = "1.0.0",
        enabled = true,
        capabilities = listOf("搜索", "详情", "章节", "页面"),
    )
}

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

internal enum class SourceTestFeedback {
    Success,
    NotFound,
}
