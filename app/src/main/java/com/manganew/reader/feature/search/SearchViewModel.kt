package com.manganew.reader.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manganew.reader.core.model.ComicSummary
import com.manganew.reader.core.repository.MangaRepository
import com.manganew.reader.data.AppRepositoryContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class SearchUiState(
    val query: String = "",
    val result: SearchResultState = SearchResultState.Idle,
)

internal sealed interface SearchResultState {
    data object Idle : SearchResultState
    data object Debouncing : SearchResultState
    data object Loading : SearchResultState
    data object Empty : SearchResultState
    data class Content(val comics: List<PrototypeComicSummary>) : SearchResultState
    data class Error(val reason: SearchError) : SearchResultState
}

internal enum class SearchError {
    Preview,
    RequestFailed,
}

internal sealed interface SearchAction {
    data class QueryChanged(val query: String) : SearchAction
    data object Submit : SearchAction
    data object Clear : SearchAction
    data object PreviewError : SearchAction
    data object Retry : SearchAction
}

internal class SearchViewModel(
    private val repository: MangaRepository = AppRepositoryContainer.repository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.QueryChanged -> updateQuery(action.query)
            SearchAction.Submit -> searchNow()
            SearchAction.Clear -> clearQuery()
            SearchAction.PreviewError -> _uiState.value = _uiState.value.copy(
                result = SearchResultState.Error(SearchError.Preview),
            )
            SearchAction.Retry -> searchNow()
        }
    }

    private fun updateQuery(rawQuery: String) {
        val query = rawQuery.take(MaxQueryLength)
        searchJob?.cancel()
        _uiState.value = SearchUiState(query = query)

        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(result = SearchResultState.Debouncing)
        searchJob = viewModelScope.launch {
            delay(DebounceMillis)
            search(query)
        }
    }

    private fun searchNow() {
        val query = _uiState.value.query.trim()
        if (query.isEmpty()) {
            clearQuery()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            search(query)
        }
    }

    private suspend fun search(query: String) {
        _uiState.value = _uiState.value.copy(result = SearchResultState.Loading)
        try {
            val comics = repository.search(query).items.map(ComicSummary::toUiModel)
            if (_uiState.value.query.trim() != query) return
            _uiState.value = _uiState.value.copy(
                result = if (comics.isEmpty()) {
                    SearchResultState.Empty
                } else {
                    SearchResultState.Content(comics)
                },
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IllegalStateException) {
            _uiState.value = _uiState.value.copy(
                result = SearchResultState.Error(SearchError.RequestFailed),
            )
        }
    }

    private fun clearQuery() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }

    private companion object {
        const val DebounceMillis = 350L
        const val MaxQueryLength = 80
    }
}

private fun ComicSummary.toUiModel(): PrototypeComicSummary = PrototypeComicSummary(
    sourceId = ref.sourceId.value,
    id = ref.remoteId.value,
    title = title,
    author = author.orEmpty(),
    summary = subtitle.orEmpty(),
    tags = listOfNotNull(status, latestChapter),
)
