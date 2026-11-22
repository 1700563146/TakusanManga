package com.manganew.reader.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Shared durations and curves. Features must not define local animation timing.
 */
@Immutable
internal data class MangaMotion(
    val instantMillis: Int,
    val fastMillis: Int,
    val normalMillis: Int,
    val emphasisMillis: Int,
    val readerMillis: Int,
    val standardEasing: Easing,
    val decelerateEasing: Easing,
    val accelerateEasing: Easing,
    val emphasizedEasing: Easing,
    val reduceMotion: Boolean,
)

internal val DefaultMangaMotion = MangaMotion(
    instantMillis = 70,
    fastMillis = 140,
    normalMillis = 220,
    emphasisMillis = 360,
    readerMillis = 120,
    standardEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    decelerateEasing = LinearOutSlowInEasing,
    accelerateEasing = FastOutLinearInEasing,
    emphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f),
    reduceMotion = false,
)

internal val ReducedMangaMotion = DefaultMangaMotion.copy(
    instantMillis = 0,
    fastMillis = 80,
    normalMillis = 120,
    emphasisMillis = 120,
    readerMillis = 0,
    reduceMotion = true,
)

internal fun resolveMangaMotion(reduceMotion: Boolean): MangaMotion =
    if (reduceMotion) ReducedMangaMotion else DefaultMangaMotion

internal val LocalMangaMotion = staticCompositionLocalOf { DefaultMangaMotion }

internal val LocalReduceMotion = staticCompositionLocalOf { false }
