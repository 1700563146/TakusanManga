package com.manganew.reader.feature.settings

import com.manganew.reader.designsystem.theme.ThemeMode
import com.manganew.reader.feature.reader.ReaderMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    @Test
    fun actionsUpdatePrototypeSettingsImmediately() {
        val viewModel = SettingsViewModel()

        viewModel.onAction(SettingsAction.SetThemeMode(ThemeMode.Dark))
        viewModel.onAction(SettingsAction.SetReaderMode(ReaderMode.Vertical))
        viewModel.onAction(
            SettingsAction.SetReadingDirection(ReadingDirection.RightToLeft),
        )
        viewModel.onAction(SettingsAction.SetReduceMotion(true))
        viewModel.onAction(SettingsAction.SetImageConcurrency(4))

        val settings = viewModel.settings.value
        assertEquals(ThemeMode.Dark, settings.themeMode)
        assertEquals(ReaderMode.Vertical, settings.readerMode)
        assertEquals(ReadingDirection.RightToLeft, settings.readingDirection)
        assertTrue(settings.reduceMotion)
        assertEquals(4, settings.imageConcurrency)
    }

    @Test
    fun imageConcurrencyIsBoundedToPrototypeRange() {
        val viewModel = SettingsViewModel()

        viewModel.onAction(SettingsAction.SetImageConcurrency(10))
        assertEquals(4, viewModel.settings.value.imageConcurrency)

        viewModel.onAction(SettingsAction.SetImageConcurrency(1))
        assertEquals(2, viewModel.settings.value.imageConcurrency)
    }
}
