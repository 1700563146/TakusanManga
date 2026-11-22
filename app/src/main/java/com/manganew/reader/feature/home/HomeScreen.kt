package com.manganew.reader.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.source.SourceSummaryCard

@Composable
internal fun HomeRoute(
    onOpenReader: () -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenReader = onOpenReader,
        onOpenSource = onOpenSource,
        modifier = modifier,
    )
}

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit,
    onOpenReader: () -> Unit,
    onOpenSource: (String) -> Unit,
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
                    text = stringResource(R.string.home_title),
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.headline,
                )
                Text(
                    text = stringResource(R.string.home_description),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                )
            }
        }

        when (state) {
            HomeUiState.Loading -> item {
                LoadingState()
            }

            HomeUiState.Empty -> item {
                EmptyHomeState(
                    onImport = { onAction(HomeAction.ImportSampleSource) },
                    onPreviewError = { onAction(HomeAction.PreviewError) },
                )
            }

            is HomeUiState.Error -> item {
                HomeErrorState(
                    onRetry = { onAction(HomeAction.Retry) },
                )
            }

            is HomeUiState.Content -> {
                item {
                    Text(
                        text = stringResource(R.string.home_sources_title),
                        color = MangaTheme.colors.textPrimary,
                        style = MangaTheme.typography.title,
                    )
                }
                items(
                    items = state.sources,
                    key = { it.id },
                ) { source ->
                    SourceSummaryCard(
                        source = source,
                        onOpen = { onOpenSource(source.id) },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { onAction(HomeAction.ImportSampleSource) },
                        shape = MangaTheme.shapes.button,
                    ) {
                        Text(stringResource(R.string.import_sample_source))
                    }
                }
            }
        }

        item {
            TextButton(onClick = onOpenReader) {
                Text(
                    text = stringResource(R.string.open_reader_skeleton),
                    style = MangaTheme.typography.label,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.loading_sources),
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.body,
        )
    }
}

@Composable
private fun EmptyHomeState(
    onImport: () -> Unit,
    onPreviewError: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        ) {
            Text(
                text = stringResource(R.string.home_empty_title),
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.home_empty_description),
                color = MangaTheme.colors.textSecondary,
                style = MangaTheme.typography.body,
            )
            Button(
                onClick = onImport,
                shape = MangaTheme.shapes.button,
            ) {
                Text(stringResource(R.string.import_sample_source))
            }
            TextButton(onClick = onPreviewError) {
                Text(stringResource(R.string.preview_error_state))
            }
        }
    }
}

@Composable
private fun HomeErrorState(onRetry: () -> Unit) {
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
                text = stringResource(R.string.home_error_title),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.home_error_description),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.body,
            )
            Button(
                onClick = onRetry,
                shape = MangaTheme.shapes.button,
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MangaNewTheme {
        HomeScreen(
            state = HomeUiState.Empty,
            onAction = {},
            onOpenReader = {},
            onOpenSource = {},
        )
    }
}
