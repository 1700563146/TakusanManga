package com.manganew.reader.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.designsystem.theme.ThemeMode
import com.manganew.reader.feature.reader.ReaderMode

@Composable
internal fun SettingsRoute(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(
        settings = settings,
        onAction = onAction,
        modifier = modifier,
    )
}

@Composable
internal fun SettingsScreen(
    settings: AppSettings,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag(SettingsListTestTag),
        contentPadding = PaddingValues(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                Text(
                    text = stringResource(R.string.settings_title),
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.headline,
                )
                Text(
                    text = stringResource(R.string.settings_description),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MangaTheme.shapes.card,
                color = MangaTheme.colors.surfaceVariant,
            ) {
                Text(
                    text = stringResource(R.string.settings_memory_notice),
                    modifier = Modifier.padding(MangaTheme.spacing.large),
                    color = MangaTheme.colors.onSurfaceVariant,
                    style = MangaTheme.typography.caption,
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_theme_title)) {
                ChoiceRow(
                    title = stringResource(R.string.settings_theme_system),
                    selected = settings.themeMode == ThemeMode.FollowSystem,
                    onClick = {
                        onAction(SettingsAction.SetThemeMode(ThemeMode.FollowSystem))
                    },
                )
                ChoiceRow(
                    title = stringResource(R.string.settings_theme_light),
                    selected = settings.themeMode == ThemeMode.Light,
                    onClick = {
                        onAction(SettingsAction.SetThemeMode(ThemeMode.Light))
                    },
                )
                ChoiceRow(
                    title = stringResource(R.string.settings_theme_dark),
                    selected = settings.themeMode == ThemeMode.Dark,
                    onClick = {
                        onAction(SettingsAction.SetThemeMode(ThemeMode.Dark))
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_reader_mode_title)) {
                ChoiceRow(
                    title = stringResource(R.string.settings_reader_mode_paged),
                    selected = settings.readerMode == ReaderMode.Paged,
                    onClick = {
                        onAction(SettingsAction.SetReaderMode(ReaderMode.Paged))
                    },
                )
                ChoiceRow(
                    title = stringResource(R.string.settings_reader_mode_vertical),
                    selected = settings.readerMode == ReaderMode.Vertical,
                    onClick = {
                        onAction(SettingsAction.SetReaderMode(ReaderMode.Vertical))
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_direction_title)) {
                ChoiceRow(
                    title = stringResource(R.string.settings_direction_ltr),
                    description = stringResource(R.string.settings_direction_ltr_description),
                    selected = settings.readingDirection == ReadingDirection.LeftToRight,
                    onClick = {
                        onAction(
                            SettingsAction.SetReadingDirection(ReadingDirection.LeftToRight),
                        )
                    },
                )
                ChoiceRow(
                    title = stringResource(R.string.settings_direction_rtl),
                    description = stringResource(R.string.settings_direction_rtl_description),
                    selected = settings.readingDirection == ReadingDirection.RightToLeft,
                    onClick = {
                        onAction(
                            SettingsAction.SetReadingDirection(ReadingDirection.RightToLeft),
                        )
                    },
                )
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_motion_title)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = settings.reduceMotion,
                            role = Role.Switch,
                            onValueChange = {
                                onAction(SettingsAction.SetReduceMotion(it))
                            },
                        )
                        .padding(vertical = MangaTheme.spacing.small),
                    horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_reduce_motion),
                            color = MangaTheme.colors.textPrimary,
                            style = MangaTheme.typography.body,
                        )
                        Text(
                            text = stringResource(R.string.settings_reduce_motion_description),
                            color = MangaTheme.colors.textSecondary,
                            style = MangaTheme.typography.caption,
                        )
                    }
                    Switch(
                        checked = settings.reduceMotion,
                        onCheckedChange = null,
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.settings_concurrency_title)) {
                Text(
                    text = stringResource(R.string.settings_concurrency_description),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.caption,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                    ImageConcurrencyOptions.forEach { count ->
                        FilterChip(
                            selected = settings.imageConcurrency == count,
                            onClick = {
                                onAction(SettingsAction.SetImageConcurrency(count))
                            },
                            label = {
                                Text(stringResource(R.string.settings_concurrency_value, count))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small),
        ) {
            Text(
                text = title,
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.title,
            )
            content()
        }
    }
}

@Composable
private fun ChoiceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    description: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(vertical = MangaTheme.spacing.small),
        horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MangaTheme.colors.textPrimary,
                style = MangaTheme.typography.body,
            )
            if (description != null) {
                Text(
                    text = description,
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.caption,
                )
            }
        }
        RadioButton(
            selected = selected,
            onClick = null,
        )
    }
}

internal const val SettingsListTestTag = "settings-list"

@Preview(showBackground = true)
@Composable
private fun SettingsPreview() {
    MangaNewTheme {
        SettingsScreen(
            settings = AppSettings(),
            onAction = {},
        )
    }
}
