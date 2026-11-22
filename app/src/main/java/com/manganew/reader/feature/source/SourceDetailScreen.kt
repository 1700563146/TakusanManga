package com.manganew.reader.feature.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme

@Composable
internal fun SourceDetailRoute(
    sourceId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SourceDetailViewModel = viewModel(
        key = "source-detail-$sourceId",
        factory = SourceDetailViewModel.factory(sourceId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SourceDetailEffect.SourceDeleted -> onBack()
            }
        }
    }

    SourceDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun SourceDetailScreen(
    state: SourceDetailUiState,
    onAction: (SourceDetailAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.back))
        }
        Text(
            text = stringResource(R.string.source_detail_title),
            color = MangaTheme.colors.textPrimary,
            style = MangaTheme.typography.headline,
        )

        when (state) {
            SourceDetailUiState.Loading -> {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.loading_source_detail),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                )
            }

            is SourceDetailUiState.Error -> {
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
                            text = stringResource(R.string.source_not_found_title),
                            color = MangaTheme.colors.onErrorContainer,
                            style = MangaTheme.typography.title,
                        )
                        Text(
                            text = stringResource(R.string.source_not_found_description),
                            color = MangaTheme.colors.onErrorContainer,
                            style = MangaTheme.typography.body,
                        )
                        Button(onClick = { onAction(SourceDetailAction.Retry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is SourceDetailUiState.Content -> {
                SourceDetailContent(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun SourceDetailContent(
    state: SourceDetailUiState.Content,
    onAction: (SourceDetailAction) -> Unit,
) {
    val source = state.source
    val busy = state.isTesting || state.isReimporting || state.isDeleting

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
                text = source.name,
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.title,
            )
            DetailRow(
                label = stringResource(R.string.source_host_label),
                value = source.host,
            )
            DetailRow(
                label = stringResource(R.string.source_format_label),
                value = source.format,
            )
            DetailRow(
                label = stringResource(R.string.source_version_label),
                value = source.version,
            )
            DetailRow(
                label = stringResource(R.string.source_capabilities_label),
                value = source.capabilities.joinToString(separator = " · "),
            )
            DetailRow(
                label = stringResource(R.string.source_status_label),
                value = stringResource(
                    if (source.enabled) R.string.source_enabled else R.string.source_disabled,
                ),
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
        Button(
            onClick = { onAction(SourceDetailAction.TestSource) },
            enabled = !busy,
            shape = MangaTheme.shapes.button,
        ) {
            Text(
                text = stringResource(
                    if (state.isTesting) R.string.testing_source else R.string.test_source,
                ),
            )
        }
        OutlinedButton(
            onClick = { onAction(SourceDetailAction.Reimport) },
            enabled = !busy,
            shape = MangaTheme.shapes.button,
        ) {
            Text(stringResource(R.string.reimport_source))
        }
    }

    if (state.isTesting) {
        CircularProgressIndicator()
    }
    when (state.testFeedback) {
        SourceTestFeedback.Success -> Text(
            text = stringResource(R.string.source_test_success),
            color = MangaTheme.colors.success,
            style = MangaTheme.typography.body,
        )

        SourceTestFeedback.NotFound -> Text(
            text = stringResource(R.string.source_test_failed),
            color = MangaTheme.colors.error,
            style = MangaTheme.typography.body,
        )

        null -> Unit
    }

    Button(
        onClick = { onAction(SourceDetailAction.Delete) },
        enabled = !busy,
        shape = MangaTheme.shapes.button,
        colors = ButtonDefaults.buttonColors(
            containerColor = MangaTheme.colors.error,
            contentColor = MangaTheme.colors.onError,
        ),
    ) {
        Text(
            text = stringResource(
                if (state.isDeleting) R.string.deleting_source else R.string.delete_source_action,
            ),
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.extraSmall)) {
        Text(
            text = label,
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.caption,
        )
        Text(
            text = value,
            color = MangaTheme.colors.textPrimary,
            style = MangaTheme.typography.body,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SourceDetailPreview() {
    MangaNewTheme {
        SourceDetailScreen(
            state = SourceDetailUiState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}
