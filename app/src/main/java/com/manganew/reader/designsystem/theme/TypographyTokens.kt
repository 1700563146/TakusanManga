package com.manganew.reader.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Base text styles for content hierarchy.
 *
 * Sizes remain in scalable `sp` units so Android font scaling is preserved.
 */
@Immutable
internal data class MangaTypography(
    val display: TextStyle,
    val headline: TextStyle,
    val title: TextStyle,
    val body: TextStyle,
    val label: TextStyle,
    val caption: TextStyle,
)

internal val DefaultMangaTypography = MangaTypography(
    display = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    ),
    headline = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    title = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
    ),
    body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    label = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    caption = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
)

internal val LocalMangaTypography = staticCompositionLocalOf { DefaultMangaTypography }

internal fun MangaTypography.toMaterialTypography(): Typography {
    val defaults = Typography()
    return defaults.copy(
        displayLarge = display.copy(fontSize = 40.sp, lineHeight = 48.sp),
        displayMedium = display,
        displaySmall = display.copy(fontSize = 30.sp, lineHeight = 38.sp),
        headlineLarge = headline.copy(fontSize = 28.sp, lineHeight = 36.sp),
        headlineMedium = headline,
        headlineSmall = headline.copy(fontSize = 22.sp, lineHeight = 30.sp),
        titleLarge = title.copy(fontSize = 22.sp, lineHeight = 30.sp),
        titleMedium = title,
        titleSmall = title.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyLarge = body.copy(fontSize = 18.sp, lineHeight = 28.sp),
        bodyMedium = body,
        bodySmall = body.copy(fontSize = 14.sp, lineHeight = 22.sp),
        labelLarge = label.copy(fontSize = 16.sp, lineHeight = 22.sp),
        labelMedium = label,
        labelSmall = caption.copy(fontSize = 11.sp, lineHeight = 16.sp),
    )
}
