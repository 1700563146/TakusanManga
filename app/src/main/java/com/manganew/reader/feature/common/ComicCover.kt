package com.manganew.reader.feature.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.manganew.reader.designsystem.theme.MangaTheme

/**
 * Placeholder artwork for the local UI prototype. It never represents or stores
 * remote comic image bytes.
 */
@Composable
internal fun ComicCoverPlaceholder(
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MangaTheme.colors.primaryContainer,
                        MangaTheme.colors.secondaryContainer,
                    ),
                ),
            )
            .padding(MangaTheme.spacing.small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.take(1),
            color = MangaTheme.colors.onPrimaryContainer,
            style = MangaTheme.typography.display,
            textAlign = TextAlign.Center,
        )
    }
}
