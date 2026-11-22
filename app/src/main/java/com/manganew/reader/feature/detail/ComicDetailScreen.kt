package com.manganew.reader.feature.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.common.ComicCoverPlaceholder
import com.manganew.reader.feature.search.PrototypeChapter
import com.manganew.reader.feature.search.PrototypeComicDetail

@Composable
internal fun ComicDetailRoute(
    comicId: String,
    onBack: () -> Unit,
    onReadChapter: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ComicDetailViewModel = viewModel(
        key = "comic-detail-$comicId",
        factory = ComicDetailViewModel.factory(comicId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ComicDetailScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onReadChapter = onReadChapter,
        modifier = modifier,
    )
}

@Composable
internal fun ComicDetailScreen(
    state: ComicDetailUiState,
    onAction: (ComicDetailAction) -> Unit,
    onBack: () -> Unit,
    onReadChapter: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            MangaTheme.spacing.pageHorizontal,
        ),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
        }

        when (state) {
            ComicDetailUiState.Loading -> item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.loading_comic_detail),
                        color = MangaTheme.colors.textSecondary,
                        style = MangaTheme.typography.body,
                    )
                }
            }

            is ComicDetailUiState.Error -> item {
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
                            text = stringResource(R.string.comic_not_found_title),
                            color = MangaTheme.colors.onErrorContainer,
                            style = MangaTheme.typography.title,
                        )
                        Text(
                            text = stringResource(R.string.comic_not_found_description),
                            color = MangaTheme.colors.onErrorContainer,
                            style = MangaTheme.typography.body,
                        )
                        Button(onClick = { onAction(ComicDetailAction.Retry) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }

            is ComicDetailUiState.Content -> {
                val comic = state.comic
                item {
                    ComicHeader(comic)
                }
                item {
                    comic.chapters.firstOrNull()?.let { chapter ->
                        Button(
                            onClick = { onReadChapter(chapter.id) },
                            shape = MangaTheme.shapes.button,
                        ) {
                            Text(stringResource(R.string.start_reading))
                        }
                    }
                }
                item {
                    Text(
                        text = stringResource(
                            R.string.chapter_count,
                            comic.chapters.size,
                        ),
                        color = MangaTheme.colors.textPrimary,
                        style = MangaTheme.typography.title,
                    )
                }
                if (comic.chapters.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.no_chapters),
                            color = MangaTheme.colors.textSecondary,
                            style = MangaTheme.typography.body,
                        )
                    }
                } else {
                    items(
                        items = comic.chapters,
                        key = { it.id },
                    ) { chapter ->
                        ChapterRow(
                            chapter = chapter,
                            onRead = { onReadChapter(chapter.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComicHeader(comic: PrototypeComicDetail) {
    Column(verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
        ) {
            ComicCoverPlaceholder(
                title = comic.title,
                modifier = Modifier.size(width = 112.dp, height = 150.dp),
                cornerRadius = 16.dp,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small),
            ) {
                Text(
                    text = comic.title,
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.headline,
                )
                Text(
                    text = comic.author,
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.label,
                )
                Text(
                    text = comic.tags.joinToString(separator = " · "),
                    color = MangaTheme.colors.info,
                    style = MangaTheme.typography.caption,
                )
            }
        }
        Text(
            text = comic.description,
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.body,
        )
    }
}

@Composable
private fun ChapterRow(
    chapter: PrototypeChapter,
    onRead: () -> Unit,
) {
    val readDescription = stringResource(R.string.read_chapter, chapter.title)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(MangaTheme.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.extraSmall),
            ) {
                Text(
                    text = chapter.title,
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.body,
                )
                Text(
                    text = stringResource(R.string.chapter_updated_at, chapter.updatedAt),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.caption,
                )
            }
            TextButton(
                onClick = onRead,
                modifier = Modifier.semantics {
                    contentDescription = readDescription
                },
            ) {
                Text(stringResource(R.string.read_chapter_action))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ComicDetailPreview() {
    MangaNewTheme {
        ComicDetailScreen(
            state = ComicDetailUiState.Loading,
            onAction = {},
            onBack = {},
            onReadChapter = {},
        )
    }
}
