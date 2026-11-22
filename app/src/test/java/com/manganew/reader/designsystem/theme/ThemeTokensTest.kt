package com.manganew.reader.designsystem.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {
    @Test
    fun lightAndDarkThemesExposeDistinctSemanticColors() {
        assertNotEquals(LightMangaColors.background, DarkMangaColors.background)
        assertNotEquals(LightMangaColors.readerBackground, LightMangaColors.background)
        assertNotEquals(DarkMangaColors.readerBackground, DarkMangaColors.background)
    }

    @Test
    fun materialSchemeUsesSemanticTokens() {
        val lightScheme = LightMangaColors.toMaterialColorScheme()
        val darkScheme = DarkMangaColors.toMaterialColorScheme()

        assertEquals(LightMangaColors.primary, lightScheme.primary)
        assertEquals(DarkMangaColors.surface, darkScheme.surface)
        assertEquals(LightMangaColors.textPrimary, lightScheme.onBackground)
    }

    @Test
    fun reduceMotionOptionUsesReducedDurations() {
        val normal = resolveMangaMotion(reduceMotion = false)
        val reduced = resolveMangaMotion(reduceMotion = true)

        assertFalse(normal.reduceMotion)
        assertTrue(reduced.reduceMotion)
        assertTrue(reduced.instantMillis < normal.instantMillis)
        assertTrue(reduced.readerMillis < normal.readerMillis)
    }

    @Test
    fun zeroPlatformAnimationScaleEnablesReduceMotion() {
        assertTrue(isSystemReduceMotionEnabled(animatorDurationScale = 0f, transitionAnimationScale = 1f))
        assertTrue(isSystemReduceMotionEnabled(animatorDurationScale = 1f, transitionAnimationScale = 0f))
    }

    @Test
    fun normalPlatformAnimationsDoNotEnableReduceMotion() {
        assertFalse(isSystemReduceMotionEnabled(animatorDurationScale = 1f, transitionAnimationScale = 1f))
    }
}
