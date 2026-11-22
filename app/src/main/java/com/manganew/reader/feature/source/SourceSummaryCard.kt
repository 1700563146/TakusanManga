package com.manganew.reader.feature.source

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaTheme

@Composable
internal fun SourceSummaryCard(
    source: PrototypeSource,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleEnabled: ((Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val toggleDescription = stringResource(
        if (source.enabled) R.string.disable_source else R.string.enable_source,
        source.name,
    )
    val deleteDescription = stringResource(R.string.delete_source, source.name)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MangaTheme.shapes.card,
        color = MangaTheme.colors.surface,
    ) {
        Column(
            modifier = Modifier.padding(MangaTheme.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.medium),
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(MangaTheme.spacing.extraSmall),
                ) {
                    Text(
                        text = source.name,
                        color = MangaTheme.colors.textPrimary,
                        style = MangaTheme.typography.title,
                    )
                    Text(
                        text = source.host,
                        color = MangaTheme.colors.textSecondary,
                        style = MangaTheme.typography.caption,
                    )
                }
                if (onToggleEnabled != null) {
                    Switch(
                        checked = source.enabled,
                        onCheckedChange = onToggleEnabled,
                        modifier = Modifier.semantics {
                            contentDescription = toggleDescription
                        },
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (source.enabled) R.string.source_enabled else R.string.source_disabled,
                        ),
                        color = if (source.enabled) {
                            MangaTheme.colors.success
                        } else {
                            MangaTheme.colors.textSecondary
                        },
                        style = MangaTheme.typography.label,
                    )
                }
            }

            Text(
                text = stringResource(
                    R.string.source_metadata,
                    source.format,
                    source.version,
                ),
                color = MangaTheme.colors.textSecondary,
                style = MangaTheme.typography.body,
            )
            Text(
                text = source.capabilities.joinToString(separator = " · "),
                color = MangaTheme.colors.onSurfaceVariant,
                style = MangaTheme.typography.caption,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(MangaTheme.spacing.small)) {
                TextButton(onClick = onOpen) {
                    Text(stringResource(R.string.view_source_details))
                }
                if (onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.semantics {
                            contentDescription = deleteDescription
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.delete_source_action),
                            color = MangaTheme.colors.error,
                        )
                    }
                }
            }
        }
    }
}
