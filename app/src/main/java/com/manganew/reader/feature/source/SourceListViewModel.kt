package com.manganew.reader.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal sealed interface SourceListUiState {
    data object Loading : SourceListUiState
    data object Empty : SourceListUiState
    data class Content(val sources: List<PrototypeSource>) : SourceListUiState
    data class Error(val reason: SourceListError) : SourceListUiState
}

internal enum class SourceListError {
    Preview,
    ImportFailed,
}

internal sealed interface SourceListAction {
    data object ImportSampleSource : SourceListAction
    data object PreviewError : SourceListAction
    data object Retry : SourceListAction
    data class ToggleEnabled(val sourceId: String, val enabled: Boolean) : SourceListAction
    data class Delete(val sourceId: String) : SourceListAction
}

internal sealed interface SourceListEvent {
    data class SourceDeleted(val sourceName: String) : SourceListEvent
}

internal class SourceListViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<SourceListUiState>(SourceListUiState.Loading)
    val uiState: StateFlow<SourceListUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SourceListEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SourceListEvent> = _events.asSharedFlow()

    private var operationJob: Job? = null

    init {
        viewModelScope.launch {
            FakeSourceStore.sources.collectLatest { sources ->
                _uiState.value = if (sources.isEmpty()) {
                    SourceListUiState.Empty
                } else {
                    SourceListUiState.Content(sources)
                }
            }
        }
    }

    fun onAction(action: SourceListAction) {
        when (action) {
            SourceListAction.ImportSampleSource -> importSampleSource()
            SourceListAction.PreviewError -> _uiState.value =
                SourceListUiState.Error(SourceListError.Preview)
            SourceListAction.Retry -> restoreFromStore()
            is SourceListAction.ToggleEnabled ->
                FakeSourceStore.setEnabled(action.sourceId, action.enabled)
            is SourceListAction.Delete -> deleteSource(action.sourceId)
        }
    }

    private fun importSampleSource() {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = SourceListUiState.Loading
            try {
                FakeSourceStore.importSampleSource()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalStateException) {
                _uiState.value = SourceListUiState.Error(SourceListError.ImportFailed)
            }
        }
    }

    private fun deleteSource(sourceId: String) {
        val sourceName = FakeSourceStore.findSource(sourceId)?.name ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = SourceListUiState.Loading
            try {
                FakeSourceStore.deleteSource(sourceId)
                _events.emit(SourceListEvent.SourceDeleted(sourceName))
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun restoreFromStore() {
        val sources = FakeSourceStore.sources.value
        _uiState.value = if (sources.isEmpty()) {
            SourceListUiState.Empty
        } else {
            SourceListUiState.Content(sources)
        }
    }
}
