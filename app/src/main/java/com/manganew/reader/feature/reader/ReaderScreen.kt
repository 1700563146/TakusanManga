package com.manganew.reader.feature.reader

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.common.FeaturePlaceholder

@Composable
internal fun ReaderScreen(
    chapterId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = stringResource(R.string.reader_title),
        description = stringResource(
            R.string.reader_description,
            chapterId.ifBlank { stringResource(R.string.reader_unknown_chapter) },
        ),
        modifier = modifier,
    ) {
        TextButton(onClick = onBack) {
            Text(
                text = stringResource(R.string.reader_back),
                style = MangaTheme.typography.label,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReaderScreenPreview() {
    MangaNewTheme {
        ReaderScreen(
            chapterId = "demo-chapter",
            onBack = {},
        )
    }
}
