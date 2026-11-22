package com.manganew.reader.feature.search

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.manganew.reader.R
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.feature.common.FeaturePlaceholder

@Composable
internal fun SearchScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholder(
        title = stringResource(R.string.search_title),
        description = stringResource(R.string.search_description),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchScreenPreview() {
    MangaNewTheme {
        SearchScreen()
    }
}
