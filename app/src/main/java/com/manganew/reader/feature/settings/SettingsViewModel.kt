package com.manganew.reader.feature.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import com.manganew.reader.designsystem.theme.ThemeMode
import com.manganew.reader.feature.reader.ReaderMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@Immutable
internal data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.FollowSystem,
    val readerMode: ReaderMode = ReaderMode.Paged,
    val readingDirection: ReadingDirection = ReadingDirection.LeftToRight,
    val reduceMotion: Boolean = false,
    val imageConcurrency: Int = 3,
)

internal enum class ReadingDirection {
    LeftToRight,
    RightToLeft,
}

internal sealed interface SettingsAction {
    data class SetThemeMode(val mode: ThemeMode) : SettingsAction
    data class SetReaderMode(val mode: ReaderMode) : SettingsAction
    data class SetReadingDirection(val direction: ReadingDirection) : SettingsAction
    data class SetReduceMotion(val enabled: Boolean) : SettingsAction
    data class SetImageConcurrency(val count: Int) : SettingsAction
}

/**
 * In-memory settings for the UI prototype.
 *
 * Persistence belongs to T061 and must not be inferred from this ViewModel.
 */
internal class SettingsViewModel : ViewModel() {
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun onAction(action: SettingsAction) {
        _settings.update { current ->
            when (action) {
                is SettingsAction.SetThemeMode -> current.copy(themeMode = action.mode)
                is SettingsAction.SetReaderMode -> current.copy(readerMode = action.mode)
                is SettingsAction.SetReadingDirection ->
                    current.copy(readingDirection = action.direction)
                is SettingsAction.SetReduceMotion ->
                    current.copy(reduceMotion = action.enabled)
                is SettingsAction.SetImageConcurrency ->
                    current.copy(imageConcurrency = action.count.coerceIn(2, 4))
            }
        }
    }
}

internal val ImageConcurrencyOptions = listOf(2, 3, 4)
