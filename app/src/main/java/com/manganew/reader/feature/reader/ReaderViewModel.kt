package com.manganew.reader.feature.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface ReaderUiState {
    data object Loading : ReaderUiState
    data class Content(
        val chapter: PrototypeReaderChapter,
        val pageIndex: Int = 0,
        val mode: ReaderMode = ReaderMode.Paged,
        val toolbarVisible: Boolean = true,
    ) : ReaderUiState

    data class Error(val reason: ReaderError) : ReaderUiState
}

internal enum class ReaderError {
    ChapterNotFound,
}

internal sealed interface ReaderAction {
    data object NextPage : ReaderAction
    data object PreviousPage : ReaderAction
    data class PageChanged(val pageIndex: Int) : ReaderAction
    data object ToggleToolbar : ReaderAction
    data class SetMode(val mode: ReaderMode) : ReaderAction
    data object Retry : ReaderAction
}

internal class ReaderViewModel(
    private val chapterId: String,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ReaderUiState>(ReaderUiState.Loading)
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadChapter()
    }

    fun onAction(action: ReaderAction) {
        when (action) {
            ReaderAction.NextPage -> changePage(1)
            ReaderAction.PreviousPage -> changePage(-1)
            is ReaderAction.PageChanged -> updateCurrentPage(action.pageIndex)
            ReaderAction.ToggleToolbar -> updateContent { it.copy(toolbarVisible = !it.toolbarVisible) }
            is ReaderAction.SetMode -> updateContent { it.copy(mode = action.mode) }
            ReaderAction.Retry -> loadChapter()
        }
    }

    private fun loadChapter() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = ReaderUiState.Loading
            try {
                val chapter = FakeReaderRepository.loadChapter(chapterId)
                _uiState.value = if (chapter == null) {
                    ReaderUiState.Error(ReaderError.ChapterNotFound)
                } else {
                    ReaderUiState.Content(chapter)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalStateException) {
                _uiState.value = ReaderUiState.Error(ReaderError.ChapterNotFound)
            }
        }
    }

    private fun changePage(delta: Int) {
        updateContent { content ->
            content.copy(
                pageIndex = (content.pageIndex + delta).coerceIn(
                    minimumValue = 0,
                    maximumValue = content.chapter.pages.lastIndex.coerceAtLeast(0),
                ),
            )
        }
    }

    private fun updateCurrentPage(pageIndex: Int) {
        updateContent { content ->
            content.copy(
                pageIndex = pageIndex.coerceIn(
                    minimumValue = 0,
                    maximumValue = content.chapter.pages.lastIndex.coerceAtLeast(0),
                ),
            )
        }
    }

    private fun updateContent(transform: (ReaderUiState.Content) -> ReaderUiState.Content) {
        val content = _uiState.value as? ReaderUiState.Content ?: return
        _uiState.value = transform(content)
    }

    companion object {
        fun factory(chapterId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ReaderViewModel(chapterId)
            }
        }
    }
}
