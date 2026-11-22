package com.manganew.reader.designsystem.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeModeTest {
    @Test
    fun followSystemUsesPlatformPreference() {
        assertTrue(ThemeMode.FollowSystem.prefersDarkTheme(systemInDarkTheme = true))
        assertFalse(ThemeMode.FollowSystem.prefersDarkTheme(systemInDarkTheme = false))
    }

    @Test
    fun explicitModesIgnorePlatformPreference() {
        assertFalse(ThemeMode.Light.prefersDarkTheme(systemInDarkTheme = true))
        assertTrue(ThemeMode.Dark.prefersDarkTheme(systemInDarkTheme = false))
    }
}
