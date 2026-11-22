package com.manganew.reader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.designsystem.theme.MangaTheme
import com.manganew.reader.designsystem.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MangaNewApp()
        }
    }
}

@Composable
internal fun MangaNewApp() {
    MangaNewTheme(themeMode = ThemeMode.FollowSystem) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MangaTheme.colors.background,
            contentColor = MangaTheme.colors.textPrimary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .padding(MangaTheme.spacing.pageHorizontal),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MangaTheme.typography.headline,
                )
                Text(
                    text = stringResource(R.string.app_skeleton_ready),
                    modifier = Modifier.padding(top = MangaTheme.spacing.contentGap),
                    color = MangaTheme.colors.textSecondary,
                    style = MangaTheme.typography.body,
                )
            }
        }
    }
}
