package com.manganew.reader.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.manganew.reader.core.model.Chapter
import com.manganew.reader.core.model.ComicDetail
import com.manganew.reader.core.model.ComicRef
import com.manganew.reader.core.model.RemoteId
import com.manganew.reader.core.model.SourceId
import com.manganew.reader.core.repository.MangaRepository
import com.manganew.reader.data.AppRepositoryContainer
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
    private val sourceId: String,
    private val comicId: String,
    private val repository: MangaRepository = AppRepositoryContainer.repository,
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
                val ref = ComicRef(SourceId(sourceId), RemoteId(comicId))
                val detail = repository.getDetail(ref)
                val chapters = repository.getChapters(ref)
                _uiState.value = ComicDetailUiState.Content(
                    detail.toUiModel(chapters),
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: IllegalArgumentException) {
                _uiState.value = ComicDetailUiState.Error(ComicDetailError.NotFound)
            } catch (_: IllegalStateException) {
                _uiState.value = ComicDetailUiState.Error(ComicDetailError.RequestFailed)
            }
        }
    }

    companion object {
        fun factory(
            sourceId: String,
            comicId: String,
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ComicDetailViewModel(
                    sourceId = sourceId,
                    comicId = comicId,
                )
            }
        }
    }
}

private fun ComicDetail.toUiModel(chapters: List<Chapter>): PrototypeComicDetail =
    PrototypeComicDetail(
        sourceId = ref.sourceId.value,
        id = ref.remoteId.value,
        title = title,
        author = author.orEmpty(),
        description = description.orEmpty(),
        tags = tags,
        chapters = chapters.map { chapter ->
            PrototypeChapter(
                id = chapter.chapterId.value,
                title = chapter.title,
                updatedAt = chapter.publishedAt.orEmpty(),
            )
        },
    )
