package com.manganew.reader.feature.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ReaderRoute(
    chapterId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = viewModel(
        key = "reader-$chapterId",
        factory = ReaderViewModel.factory(chapterId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ReaderScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
internal fun ReaderScreen(
    state: ReaderUiState,
    onAction: (ReaderAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MangaTheme.colors.readerBackground),
    ) {
        when (state) {
            ReaderUiState.Loading -> ReaderStatusContent(
                title = stringResource(R.string.reader_loading),
                onBack = onBack,
            )

            is ReaderUiState.Error -> ReaderStatusContent(
                title = stringResource(R.string.reader_error),
                onBack = onBack,
                showProgress = false,
                action = {
                    Button(onClick = { onAction(ReaderAction.Retry) }) {
                        Text(stringResource(R.string.retry))
                    }
                },
            )

            is ReaderUiState.Content -> ReaderContent(
                state = state,
                onAction = onAction,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState.Content,
    onAction: (ReaderAction) -> Unit,
    onBack: () -> Unit,
) {
    val motion = MangaTheme.motion
    Box(modifier = Modifier.fillMaxSize()) {
        when (state.mode) {
            ReaderMode.Paged -> PagedReader(
                state = state,
                onAction = onAction,
            )

            ReaderMode.Vertical -> VerticalReader(
                state = state,
                onAction = onAction,
            )
        }

        AnimatedVisibility(
            visible = state.toolbarVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(durationMillis = motion.normalMillis)),
            exit = fadeOut(tween(durationMillis = motion.normalMillis)),
        ) {
            ReaderTopBar(
                chapter = state.chapter,
                pageIndex = state.pageIndex,
                onBack = onBack,
            )
        }

        AnimatedVisibility(
            visible = state.toolbarVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(durationMillis = motion.normalMillis)),
            exit = fadeOut(tween(durationMillis = motion.normalMillis)),
        ) {
            ReaderBottomBar(
                state = state,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun PagedReader(
    state: ReaderUiState.Content,
    onAction: (ReaderAction) -> Unit,
) {
    val pageCount = state.chapter.pages.size
    val pagerState = rememberPagerState(initialPage = state.pageIndex) { pageCount }
    val reduceMotion = MangaTheme.reduceMotion

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { pageIndex ->
                onAction(ReaderAction.PageChanged(pageIndex))
            }
    }
    LaunchedEffect(state.pageIndex) {
        if (pagerState.currentPage != state.pageIndex) {
            if (reduceMotion) {
                pagerState.scrollToPage(state.pageIndex)
            } else {
                pagerState.animateScrollToPage(state.pageIndex)
            }
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(ReaderViewportTestTag),
    ) { pageIndex ->
        ReaderPageView(
            page = state.chapter.pages[pageIndex],
            modifier = Modifier.fillMaxSize(),
            onSingleTap = { zone ->
                when (zone) {
                    ReaderTapZone.Previous -> onAction(ReaderAction.PreviousPage)
                    ReaderTapZone.Center -> onAction(ReaderAction.ToggleToolbar)
                    ReaderTapZone.Next -> onAction(ReaderAction.NextPage)
                }
            },
        )
    }
}

@Composable
private fun VerticalReader(
    state: ReaderUiState.Content,
    onAction: (ReaderAction) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.pageIndex,
    )
    val reduceMotion = MangaTheme.reduceMotion

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { pageIndex ->
                onAction(ReaderAction.PageChanged(pageIndex))
            }
    }
    LaunchedEffect(state.pageIndex) {
        if (listState.firstVisibleItemIndex != state.pageIndex) {
            if (reduceMotion) {
                listState.scrollToItem(state.pageIndex)
            } else {
                listState.animateScrollToItem(state.pageIndex)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(ReaderViewportTestTag),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small),
    ) {
        items(
            count = state.chapter.pages.size,
            key = { state.chapter.pages[it].id },
        ) { pageIndex ->
            ReaderPageView(
                page = state.chapter.pages[pageIndex],
                modifier = Modifier
                    .fillMaxWidth()
                    .fillParentMaxHeight(0.92f),
                onSingleTap = {
                    onAction(ReaderAction.ToggleToolbar)
                },
            )
        }
    }
}

@Composable
private fun ReaderTopBar(
    chapter: PrototypeReaderChapter,
    pageIndex: Int,
    onBack: () -> Unit,
) {
    Surface(color = MangaTheme.colors.readerControlsOverlay) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MangaTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text(
                    text = stringResource(R.string.reader_back),
                    color = MangaTheme.colors.onReaderControls,
                )
            }
            Text(
                text = chapter.title,
                modifier = Modifier.weight(1f),
                color = MangaTheme.colors.onReaderControls,
                style = MangaTheme.typography.title,
            )
            Text(
                text = stringResource(
                    R.string.reader_page_indicator,
                    pageIndex + 1,
                    chapter.pages.size,
                ),
                color = MangaTheme.colors.onReaderControls,
                style = MangaTheme.typography.label,
            )
        }
    }
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState.Content,
    onAction: (ReaderAction) -> Unit,
) {
    val lastPage = state.chapter.pages.lastIndex
    Surface(color = MangaTheme.colors.readerControlsOverlay) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MangaTheme.spacing.medium, vertical = MangaTheme.spacing.small),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row {
                TextButton(
                    onClick = { onAction(ReaderAction.PreviousPage) },
                    enabled = state.pageIndex > 0,
                ) {
                    Text(
                        text = stringResource(R.string.reader_previous_page),
                        color = if (state.pageIndex > 0) {
                            MangaTheme.colors.onReaderControls
                        } else {
                            MangaTheme.colors.onReaderControls.copy(alpha = 0.38f)
                        },
                    )
                }
                TextButton(
                    onClick = { onAction(ReaderAction.NextPage) },
                    enabled = state.pageIndex < lastPage,
                ) {
                    Text(
                        text = stringResource(R.string.reader_next_page),
                        color = if (state.pageIndex < lastPage) {
                            MangaTheme.colors.onReaderControls
                        } else {
                            MangaTheme.colors.onReaderControls.copy(alpha = 0.38f)
                        },
                    )
                }
            }
            TextButton(
                onClick = {
                    onAction(
                        ReaderAction.SetMode(
                            if (state.mode == ReaderMode.Paged) {
                                ReaderMode.Vertical
                            } else {
                                ReaderMode.Paged
                            },
                        ),
                    )
                },
            ) {
                Text(
                    text = stringResource(
                        if (state.mode == ReaderMode.Paged) {
                            R.string.reader_switch_to_vertical
                        } else {
                            R.string.reader_switch_to_paged
                        },
                    ),
                    color = MangaTheme.colors.onReaderControls,
                )
            }
        }
    }
}

@Composable
private fun ReaderStatusContent(
    title: String,
    onBack: () -> Unit,
    showProgress: Boolean = true,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.reader_back),
                color = MangaTheme.colors.onReaderControls,
            )
        }
        if (showProgress) {
            CircularProgressIndicator()
        }
        Text(
            text = title,
            color = MangaTheme.colors.onReaderControls,
            style = MangaTheme.typography.body,
        )
        action()
    }
}

private enum class ReaderTapZone {
    Previous,
    Center,
    Next,
}

@Composable
private fun ReaderPageView(
    page: PrototypeReaderPage,
    onSingleTap: (ReaderTapZone) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by rememberSaveable(page.id) { mutableFloatStateOf(1f) }
    var offset by remember(page.id) { mutableStateOf(Offset.Zero) }
    val zoomDescription = if (scale > 1.01f) {
        stringResource(R.string.reader_zoom_scaled, scale)
    } else {
        stringResource(R.string.reader_zoom_original)
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .testTag("$ReaderPageTestTagPrefix${page.number}")
            .semantics {
                stateDescription = zoomDescription
            }
            .pointerInput(page.id) {
                detectTapGestures(
                    onTap = { tapOffset ->
                        if (scale <= 1.01f) {
                            val fraction = tapOffset.x / size.width.coerceAtLeast(1)
                            onSingleTap(
                                when {
                                    fraction < 0.33f -> ReaderTapZone.Previous
                                    fraction > 0.67f -> ReaderTapZone.Next
                                    else -> ReaderTapZone.Center
                                },
                            )
                        }
                    },
                    onDoubleTap = {
                        scale = if (scale > 1.01f) 1f else 2f
                        offset = Offset.Zero
                    },
                )
            }
            .pointerInput(page.id) {
                detectReaderTransformGestures(
                    canPan = { scale > 1.01f },
                ) { zoom, pan, viewport ->
                    val newScale = (scale * zoom).coerceIn(1f, 3f)
                    scale = newScale
                    offset = if (newScale <= 1.01f) {
                        Offset.Zero
                    } else {
                        val maxX = viewport.width * (newScale - 1f) / 2f
                        val maxY = viewport.height * (newScale - 1f) / 2f
                        Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY),
                        )
                    }
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
                transformOrigin = TransformOrigin.Center
            },
    ) {
        MockComicPage(page)
    }
}

@Composable
private fun MockComicPage(page: PrototypeReaderPage) {
    val pageBackground = when (page.number % 3) {
        0 -> MangaTheme.colors.primaryContainer.copy(alpha = 0.42f)
        1 -> MangaTheme.colors.secondaryContainer.copy(alpha = 0.5f)
        else -> MangaTheme.colors.surfaceVariant
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBackground)
            .padding(MangaTheme.spacing.extraLarge),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
    ) {
        Text(
            text = stringResource(R.string.reader_mock_page, page.number, page.totalPages),
            color = MangaTheme.colors.onSurfaceVariant,
            style = MangaTheme.typography.caption,
        )
        MockPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            label = stringResource(R.string.reader_mock_scene_one),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        ) {
            MockPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = stringResource(R.string.reader_mock_scene_two),
            )
            MockPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = stringResource(R.string.reader_mock_scene_three),
            )
        }
        MockPanel(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.8f),
            label = stringResource(R.string.reader_mock_scene_four),
        )
    }
}

@Composable
private fun MockPanel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface.copy(alpha = 0.82f),
    ) {
        Box(
            modifier = Modifier.padding(MangaTheme.spacing.large),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                color = MangaTheme.colors.textSecondary,
                style = MangaTheme.typography.label,
            )
        }
    }
}

private suspend fun PointerInputScope.detectReaderTransformGestures(
    canPan: () -> Boolean,
    onTransform: (zoom: Float, pan: Offset, viewport: IntSize) -> Unit,
) {
    // At normal scale, single-finger movement remains available to the Pager or
    // LazyColumn. Only multi-touch zoom and pan while already zoomed are consumed.
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()
            val pressedPointers = event.changes.count { it.pressed }
            val zoom = event.calculateZoom()
            val pan = event.calculatePan()
            val shouldHandle = (pressedPointers >= 2 && zoom != 1f) ||
                (canPan() && pan != Offset.Zero)

            if (shouldHandle) {
                onTransform(zoom, pan, size)
                event.changes.forEach { change ->
                    if (change.positionChanged()) change.consume()
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

internal const val ReaderViewportTestTag = "reader-viewport"
internal const val ReaderPageTestTagPrefix = "reader-page-"

@Preview(showBackground = true)
@Composable
private fun ReaderPreview() {
    MangaNewTheme(themeMode = com.manganew.reader.designsystem.theme.ThemeMode.Dark) {
        ReaderScreen(
            state = ReaderUiState.Loading,
            onAction = {},
            onBack = {},
        )
    }
}
