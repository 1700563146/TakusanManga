package com.manganew.reader.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manganew.reader.feature.source.FakeSourceStore
import com.manganew.reader.feature.source.PrototypeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

internal sealed interface HomeUiState {
    data object Loading : HomeUiState
    data object Empty : HomeUiState
    data class Content(val sources: List<PrototypeSource>) : HomeUiState
    data class Error(val reason: HomeError) : HomeUiState
}

internal enum class HomeError {
    Preview,
    ImportFailed,
}

internal sealed interface HomeAction {
    data object ImportSampleSource : HomeAction
    data object PreviewError : HomeAction
    data object Retry : HomeAction
}

internal class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var operationJob: Job? = null

    init {
        viewModelScope.launch {
            FakeSourceStore.sources.collectLatest { sources ->
                _uiState.value = if (sources.isEmpty()) {
                    HomeUiState.Empty
                } else {
                    HomeUiState.Content(sources.take(2))
                }
            }
        }
    }

    fun onAction(action: HomeAction) {
        when (action) {
            HomeAction.ImportSampleSource -> importSampleSource()
            HomeAction.PreviewError -> _uiState.value = HomeUiState.Error(HomeError.Preview)
            HomeAction.Retry -> restoreFromStore()
        }
    }

    private fun importSampleSource() {
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                FakeSourceStore.importSampleSource()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalStateException) {
                _uiState.value = HomeUiState.Error(HomeError.ImportFailed)
            }
        }
    }

    private fun restoreFromStore() {
        val sources = FakeSourceStore.sources.value
        _uiState.value = if (sources.isEmpty()) {
            HomeUiState.Empty
        } else {
            HomeUiState.Content(sources.take(2))
        }
    }
}
