package com.manganew.reader.feature.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme

@Composable
internal fun SourceListRoute(
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourceListViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is SourceListEvent.SourceDeleted -> snackbarHostState.showSnackbar(
                    context.getString(R.string.source_deleted_message, event.sourceName),
                )
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SourceListScreen(
            state = state,
            onAction = viewModel::onAction,
            onOpenSource = onOpenSource,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
internal fun SourceListScreen(
    state: SourceListUiState,
    onAction: (SourceListAction) -> Unit,
    onOpenSource: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        item {
            SourceListHeader()
        }

        when (state) {
            SourceListUiState.Loading -> item {
                SourceListLoading()
            }

            SourceListUiState.Empty -> item {
                SourceListEmpty(
                    onImport = { onAction(SourceListAction.ImportSampleSource) },
                    onPreviewError = { onAction(SourceListAction.PreviewError) },
                )
            }

            is SourceListUiState.Error -> item {
                SourceListErrorState(
                    onRetry = { onAction(SourceListAction.Retry) },
                )
            }

            is SourceListUiState.Content -> {
                item {
                    Button(
                        onClick = { onAction(SourceListAction.ImportSampleSource) },
                        shape = MangaTheme.shapes.button,
                    ) {
                        Text(stringResource(R.string.import_sample_source))
                    }
                }
                items(
                    items = state.sources,
                    key = { it.id },
                ) { source ->
                    SourceSummaryCard(
                        source = source,
                        onOpen = { onOpenSource(source.id) },
                        onToggleEnabled = { enabled ->
                            onAction(
                                SourceListAction.ToggleEnabled(
                                    sourceId = source.id,
                                    enabled = enabled,
                                ),
                            )
                        },
                        onDelete = {
                            onAction(SourceListAction.Delete(source.id))
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceListHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
        Text(
            text = stringResource(R.string.sources_title),
            color = MangaTheme.colors.textPrimary,
            style = MangaTheme.typography.headline,
        )
        Text(
            text = stringResource(R.string.sources_description),
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.body,
        )
    }
}

@Composable
private fun SourceListLoading() {
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
private fun SourceListEmpty(
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
                text = stringResource(R.string.sources_empty_title),
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.sources_empty_description),
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
private fun SourceListErrorState(onRetry: () -> Unit) {
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
                text = stringResource(R.string.sources_error_title),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(R.string.sources_error_description),
                color = MangaTheme.colors.onErrorContainer,
                style = MangaTheme.typography.body,
            )
            Button(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SourceListScreenPreview() {
    MangaNewTheme {
        SourceListScreen(
            state = SourceListUiState.Empty,
            onAction = {},
            onOpenSource = {},
        )
    }
}
