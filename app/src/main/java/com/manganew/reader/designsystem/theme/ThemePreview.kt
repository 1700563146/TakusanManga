package com.manganew.reader.designsystem.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(name = "Theme Light", showBackground = true, widthDp = 360)
@Composable
private fun ThemeLightPreview() {
    MangaNewTheme(themeMode = ThemeMode.Light) {
        ThemeTokenContent()
    }
}

@Preview(name = "Theme Dark", showBackground = true, widthDp = 360)
@Composable
private fun ThemeDarkPreview() {
    MangaNewTheme(themeMode = ThemeMode.Dark) {
        ThemeTokenContent()
    }
}

@Composable
private fun ThemeTokenContent() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MangaTheme.colors.background,
        contentColor = MangaTheme.colors.textPrimary,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.pageHorizontal),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
        ) {
            Text(
                text = "TakusanManga",
                style = MangaTheme.typography.display,
            )
            Text(
                text = "Design tokens",
                color = MangaTheme.colors.textSecondary,
                style = MangaTheme.typography.body,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                ColorSwatch(MangaTheme.colors.primary)
                ColorSwatch(MangaTheme.colors.secondary)
                ColorSwatch(MangaTheme.colors.success)
                ColorSwatch(MangaTheme.colors.warning)
                ColorSwatch(MangaTheme.colors.error)
                ColorSwatch(MangaTheme.colors.info)
            }
            Surface(
                shape = MangaTheme.shapes.card,
                color = MangaTheme.colors.surfaceVariant,
            ) {
                Text(
                    text = "Card · Caption",
                    modifier = Modifier.padding(MangaTheme.spacing.large),
                    color = MangaTheme.colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    Column(modifier = Modifier.background(color, CircleShape).size(size = 32.dp)) {}
}
