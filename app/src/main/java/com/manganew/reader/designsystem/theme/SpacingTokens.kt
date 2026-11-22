package com.manganew.reader.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
internal data class MangaSpacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 12.dp,
    val large: Dp = 16.dp,
    val extraLarge: Dp = 24.dp,
    val huge: Dp = 32.dp,
    val pageHorizontal: Dp = 24.dp,
    val sectionGap: Dp = 24.dp,
    val contentGap: Dp = 8.dp,
)

internal val DefaultMangaSpacing = MangaSpacing()

internal val LocalMangaSpacing = staticCompositionLocalOf { DefaultMangaSpacing }
