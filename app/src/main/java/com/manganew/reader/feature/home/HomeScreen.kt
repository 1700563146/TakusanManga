package com.manganew.reader.feature.home

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.feature.common.FeaturePlaceholder

@Composable
internal fun HomeScreen(
    onOpenReader: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholder(
        title = stringResource(R.string.home_title),
        description = stringResource(R.string.home_description),
        modifier = modifier,
    ) {
        Button(
            onClick = onOpenReader,
            shape = MangaTheme.shapes.button,
        ) {
            Text(
                text = stringResource(R.string.open_reader_skeleton),
                style = MangaTheme.typography.label,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    MangaNewTheme {
        HomeScreen(onOpenReader = {})
    }
}
