package com.manganew.reader.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.common.ComicCoverPlaceholder

@Composable
internal fun ComicSummaryCard(
    comic: PrototypeComicSummary,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(MangaTheme.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
            verticalAlignment = Alignment.Top,
        ) {
            ComicCoverPlaceholder(
                title = comic.title,
                modifier = Modifier
                    .size(width = 76.dp, height = 104.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.extraSmall),
            ) {
                Text(
                    text = comic.title,
                    color = MangaTheme.colors.textPrimary,
                    style = MangaTheme.typography.title,
                )
                Text(
                    text = comic.author,
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.label,
                )
                Text(
                    text = comic.summary,
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = comic.tags.joinToString(separator = " · "),
                    color = MangaTheme.colors.info,
                    style = MangaTheme.typography.caption,
                )
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.view_comic_details))
                }
            }
        }
    }
}

@Composable
internal fun ComicLoadingSkeleton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(MangaTheme.spacing.large),
            horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.large),
        ) {
            Surface(
                modifier = Modifier.size(width = 76.dp, height = 104.dp),
                shape = MangaTheme.shapes.medium,
                color = MangaTheme.colors.surfaceVariant,
            ) {}
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
            ) {
                SkeletonLine(widthFraction = 0.72f)
                SkeletonLine(widthFraction = 0.42f)
                SkeletonLine()
                SkeletonLine(widthFraction = 0.82f)
                SkeletonLine(widthFraction = 0.36f)
            }
        }
    }
}

@Composable
private fun SkeletonLine(widthFraction: Float = 1f) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(14.dp),
        shape = MangaTheme.shapes.small,
        color = MangaTheme.colors.surfaceVariant,
    ) {}
}
