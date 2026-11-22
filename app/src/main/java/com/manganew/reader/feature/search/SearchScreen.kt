package com.manganew.reader.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme

@Composable
internal fun SearchRoute(
    onOpenComic: (sourceId: String, comicId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SearchScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenComic = onOpenComic,
        modifier = modifier,
    )
}

@Composable
internal fun SearchScreen(
    state: SearchUiState,
    onAction: (SearchAction) -> Unit,
    onOpenComic: (sourceId: String, comicId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                Text(
                    text = stringResource(R.string.search_title),
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.headline,
                )
                Text(
                    text = stringResource(R.string.search_description),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = { onAction(SearchAction.QueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SearchInputTestTag),
                label = { Text(stringResource(R.string.search_field_label)) },
                placeholder = { Text(stringResource(R.string.search_field_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onAction(SearchAction.Submit) },
                ),
                shape = MangaTheme.shapes.medium,
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                if (state.query.isNotEmpty()) {
                    TextButton(onClick = { onAction(SearchAction.Clear) }) {
                        Text(stringResource(R.string.clear_search))
                    }
                }
                TextButton(onClick = { onAction(SearchAction.PreviewError) }) {
                    Text(stringResource(R.string.preview_error_state))
                }
            }
        }

        when (val result = state.result) {
            SearchResultState.Idle -> item {
                SearchIdleState()
            }

            SearchResultState.Debouncing -> item {
                SearchDebouncingState()
            }

            SearchResultState.Loading -> {
                item {
                    Text(
                        text = stringResource(R.string.search_loading),
                        color = MangaTheme.colors.textSecondary,
                        style = MangaTheme.typography.body,
                    )
                }
                items(count = 3, key = { "search-skeleton-$it" }) {
                    ComicLoadingSkeleton()
                }
            }

            SearchResultState.Empty -> item {
                SearchEmptyState(query = state.query)
            }

            is SearchResultState.Error -> item {
                SearchErrorState(onRetry = { onAction(SearchAction.Retry) })
            }

            is SearchResultState.Content -> {
                item {
                    Text(
                        text = stringResource(
                            R.string.search_result_count,
                            result.comics.size,
                        ),
                        color = MangaTheme.colors.textSecondary,
                        style = MangaTheme.typography.label,
                    )
                }
                items(
                    items = result.comics,
                    key = { it.id },
                ) { comic ->
                    ComicSummaryCard(
                        comic = comic,
                        onOpen = { onOpenComic(comic.sourceId, comic.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchIdleState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small),
        ) {
            Text(
                text = stringResource(R.string.search_idle_title),
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.search_idle_description),
                color = MangaTheme.colors.textSecondary,
                style = MangaTheme.typography.body,
            )
        }
    }
}

@Composable
private fun SearchDebouncingState() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.search_debouncing),
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.body,
        )
    }
}

@Composable
private fun SearchEmptyState(query: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small),
        ) {
            Text(
                text = stringResource(R.string.search_empty_title),
                color = MangaTheme.colors.onSurfaceVariant,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.search_empty_description, query),
                color = MangaTheme.colors.onSurfaceVariant,
                style = MangaTheme.typography.body,
            )
        }
    }
}

@Composable
private fun SearchErrorState(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.search_error_title),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.search_error_description),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.body,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

internal const val SearchInputTestTag = "search-input"

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    MangaNewTheme {
        SearchScreen(
            state = SearchUiState(),
            onAction = {},
            onOpenComic = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Search Content")
@Composable
private fun SearchContentPreview() {
    MangaNewTheme {
        SearchScreen(
            state = SearchUiState(
                query = "星海",
                result = SearchResultState.Content(listOf(PreviewComic)),
            ),
            onAction = {},
            onOpenComic = { _, _ -> },
        )
    }
}

@Preview(showBackground = true, name = "Search Error")
@Composable
private fun SearchErrorPreview() {
    MangaNewTheme {
        SearchScreen(
            state = SearchUiState(
                query = "星海",
                result = SearchResultState.Error(SearchError.Preview),
            ),
            onAction = {},
            onOpenComic = { _, _ -> },
        )
    }
}

private val PreviewComic = PrototypeComicSummary(
    sourceId = "preview-source",
    id = "preview-comic",
    title = "预览漫画",
    author = "预览作者",
    summary = "用于检查搜索结果卡片布局的本地示例。",
    tags = listOf("示例", "原型"),
)
