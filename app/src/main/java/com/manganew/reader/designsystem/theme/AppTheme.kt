package com.manganew.reader.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

internal enum class ThemeMode {
    FollowSystem,
    Light,
    Dark,
}

internal fun ThemeMode.prefersDarkTheme(systemInDarkTheme: Boolean): Boolean = when (this) {
    ThemeMode.FollowSystem -> systemInDarkTheme
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/**
 * Applies TakusanManga's design tokens and Material 3 theme.
 *
 * A user-selected reduce-motion preference is combined with the platform
 * preference; enabling the system setting cannot be overridden by this flag.
 */
@Composable
internal fun MangaNewTheme(
    themeMode: ThemeMode = ThemeMode.FollowSystem,
    reduceMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = themeMode.prefersDarkTheme(isSystemInDarkTheme())
    val colors = if (darkTheme) DarkMangaColors else LightMangaColors
    val typography = DefaultMangaTypography
    val shapes = DefaultMangaShapes
    val systemReduceMotion = rememberSystemReduceMotion()
    val effectiveReduceMotion = reduceMotion || systemReduceMotion
    val motion = resolveMangaMotion(effectiveReduceMotion)

    CompositionLocalProvider(
        LocalMangaColors provides colors,
        LocalMangaTypography provides typography,
        LocalMangaSpacing provides DefaultMangaSpacing,
        LocalMangaShapes provides shapes,
        LocalMangaMotion provides motion,
        LocalReduceMotion provides effectiveReduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = shapes.toMaterialShapes(),
            content = content,
        )
    }
}

internal object MangaTheme {
    val colors: MangaColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMangaColors.current

    val typography: MangaTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalMangaTypography.current

    val spacing: MangaSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMangaSpacing.current

    val shapes: MangaShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalMangaShapes.current

    val motion: MangaMotion
        @Composable
        @ReadOnlyComposable
        get() = LocalMangaMotion.current

    val reduceMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReduceMotion.current
}
