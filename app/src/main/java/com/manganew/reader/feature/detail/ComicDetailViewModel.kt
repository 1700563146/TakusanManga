package com.manganew.reader.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manganew.reader.feature.search.FakeComicRepository
import com.manganew.reader.feature.search.PrototypeComicDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface ComicDetailUiState {
    data object Loading : ComicDetailUiState
    data class Content(val comic: PrototypeComicDetail) : ComicDetailUiState
    data class Error(val reason: ComicDetailError) : ComicDetailUiState
}

internal enum class ComicDetailError {
    NotFound,
    RequestFailed,
}

internal sealed interface ComicDetailAction {
    data object Retry : ComicDetailAction
}

internal class ComicDetailViewModel(
    private val comicId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ComicDetailUiState>(ComicDetailUiState.Loading)
    val uiState: StateFlow<ComicDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadComic()
    }

    fun onAction(action: ComicDetailAction) {
        when (action) {
            ComicDetailAction.Retry -> loadComic()
        }
    }

    private fun loadComic() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ComicDetailUiState.Loading
            try {
                val comic = FakeComicRepository.detail(comicId)
                _uiState.value = if (comic == null) {
                    ComicDetailUiState.Error(ComicDetailError.NotFound)
                } else {
                    ComicDetailUiState.Content(comic)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalStateException) {
                _uiState.value = ComicDetailUiState.Error(ComicDetailError.RequestFailed)
            }
        }
    }

    companion object {
        fun factory(comicId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ComicDetailViewModel(comicId)
            }
        }
    }
}
