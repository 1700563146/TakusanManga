package com.manganew.reader.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color roles shared by every feature.
 *
 * Features must use these roles instead of referencing palette values directly.
 */
@Immutable
internal data class MangaColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val info: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val readerBackground: Color,
    val readerControlsOverlay: Color,
    val onReaderControls: Color,
    val scrim: Color,
)

internal val LightMangaColors = MangaColors(
    primary = Color(0xFF4059AD),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = Color(0xFF0B2050),
    secondary = Color(0xFF5B6478),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDFE3EE),
    onSecondaryContainer = Color(0xFF181C24),
    tertiary = Color(0xFF6E5672),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF6DDF4),
    onTertiaryContainer = Color(0xFF27152B),
    background = Color(0xFFF7F7FB),
    onBackground = Color(0xFF171A21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171A21),
    surfaceVariant = Color(0xFFE9EBF2),
    onSurfaceVariant = Color(0xFF454B58),
    outline = Color(0xFF8E95A5),
    outlineVariant = Color(0xFFC5C9D5),
    success = Color(0xFF256B4B),
    warning = Color(0xFF7A5200),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    info = Color(0xFF0B5FA5),
    textPrimary = Color(0xFF171A21),
    textSecondary = Color(0xFF5E6472),
    readerBackground = Color(0xFF0B0D12),
    readerControlsOverlay = Color(0xE61A1D24),
    onReaderControls = Color(0xFFFFFFFF),
    scrim = Color(0x99000000),
)

internal val DarkMangaColors = MangaColors(
    primary = Color(0xFFA9C4FF),
    onPrimary = Color(0xFF102E65),
    primaryContainer = Color(0xFF29477E),
    onPrimaryContainer = Color(0xFFDDE5FF),
    secondary = Color(0xFFBFC6DA),
    onSecondary = Color(0xFF293042),
    secondaryContainer = Color(0xFF3F475A),
    onSecondaryContainer = Color(0xFFDFE3EE),
    tertiary = Color(0xFFD9BEE0),
    onTertiary = Color(0xFF3E2943),
    tertiaryContainer = Color(0xFF56405C),
    onTertiaryContainer = Color(0xFFF6DDF4),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFEFF1F6),
    surface = Color(0xFF161922),
    onSurface = Color(0xFFEFF1F6),
    surfaceVariant = Color(0xFF232733),
    onSurfaceVariant = Color(0xFFC4CAD8),
    outline = Color(0xFF8E95A5),
    outlineVariant = Color(0xFF444B5A),
    success = Color(0xFF72D6A6),
    warning = Color(0xFFF0C36A),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    info = Color(0xFF9CCBFF),
    textPrimary = Color(0xFFEFF1F6),
    textSecondary = Color(0xFFB1B7C6),
    readerBackground = Color(0xFF090A0D),
    readerControlsOverlay = Color(0xE61A1D24),
    onReaderControls = Color(0xFFFFFFFF),
    scrim = Color(0xCC000000),
)

internal val LocalMangaColors = staticCompositionLocalOf { LightMangaColors }

internal fun MangaColors.toMaterialColorScheme(): ColorScheme {
    val base = if (this == DarkMangaColors) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        scrim = scrim,
    )
}
