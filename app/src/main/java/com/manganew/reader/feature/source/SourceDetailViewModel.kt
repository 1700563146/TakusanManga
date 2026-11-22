package com.manganew.reader.feature.source

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.core.repository.MangaRepository
import com.manganew.reader.data.AppRepositoryContainer
import com.manganew.reader.source.api.SourceTestResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface SourceDetailUiState {
    data object Loading : SourceDetailUiState
    data class Content(
        val source: PrototypeSource,
        val isTesting: Boolean = false,
        val isReimporting: Boolean = false,
        val isDeleting: Boolean = false,
        val testFeedback: SourceTestFeedback? = null,
    ) : SourceDetailUiState

    data class Error(val reason: SourceDetailError) : SourceDetailUiState
}

internal enum class SourceDetailError {
    NotFound,
}

internal sealed interface SourceDetailAction {
    data object TestSource : SourceDetailAction
    data object Reimport : SourceDetailAction
    data object Delete : SourceDetailAction
    data object Retry : SourceDetailAction
}

internal sealed interface SourceDetailEffect {
    data object SourceDeleted : SourceDetailEffect
}

internal class SourceDetailViewModel(
    private val sourceId: String,
    private val repository: MangaRepository = AppRepositoryContainer.repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<SourceDetailUiState>(SourceDetailUiState.Loading)
    val uiState: StateFlow<SourceDetailUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SourceDetailEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SourceDetailEffect> = _effects.asSharedFlow()

    private var operationJob: Job? = null

    init {
        loadSource()
    }

    fun onAction(action: SourceDetailAction) {
        when (action) {
            SourceDetailAction.TestSource -> testSource()
            SourceDetailAction.Reimport -> reimportSource()
            SourceDetailAction.Delete -> deleteSource()
            SourceDetailAction.Retry -> loadSource()
        }
    }

    private fun loadSource() {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = SourceDetailUiState.Loading
            delay(180)
            val source = repository.getInstalledSource(SourceId(sourceId))?.toUiModel()
            _uiState.value = if (source == null) {
                SourceDetailUiState.Error(SourceDetailError.NotFound)
            } else {
                SourceDetailUiState.Content(source)
            }
        }
    }

    private fun testSource() {
        val current = _uiState.value as? SourceDetailUiState.Content ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = current.copy(
                isTesting = true,
                testFeedback = null,
            )
            try {
                val feedback = when (repository.testConnection(SourceId(sourceId))) {
                    is SourceTestResult.Success -> SourceTestFeedback.Success
                    is SourceTestResult.Failure -> SourceTestFeedback.NotFound
                }
                _uiState.value = current.copy(
                    isTesting = false,
                    testFeedback = feedback,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun reimportSource() {
        val current = _uiState.value as? SourceDetailUiState.Content ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = current.copy(
                isReimporting = true,
                testFeedback = null,
            )
            try {
                repository.reimportSource(SourceId(sourceId))
                val source = repository.getInstalledSource(SourceId(sourceId))?.toUiModel()
                _uiState.value = if (source == null) {
                    SourceDetailUiState.Error(SourceDetailError.NotFound)
                } else {
                    SourceDetailUiState.Content(source)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    private fun deleteSource() {
        val current = _uiState.value as? SourceDetailUiState.Content ?: return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = current.copy(
                isDeleting = true,
                testFeedback = null,
            )
            try {
                repository.remove(SourceId(sourceId))
                _effects.emit(SourceDetailEffect.SourceDeleted)
            } catch (cancellation: CancellationException) {
                throw cancellation
            }
        }
    }

    companion object {
        fun factory(sourceId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SourceDetailViewModel(sourceId)
            }
        }
    }
}
