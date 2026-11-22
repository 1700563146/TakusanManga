package com.manganew.reader.feature.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.manganew.reader.designsystem.theme.MangaTheme

@Composable
internal fun FeaturePlaceholder(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(MangaTheme.spacing.pageHorizontal),
        verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
    ) {
        Text(
            text = title,
            color = MangaTheme.colors.textPrimary,
            style = MangaTheme.typography.headline,
        )
        Text(
            text = description,
            color = MangaTheme.colors.textSecondary,
            style = MangaTheme.typography.body,
        )
        action()
    }
}
