package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.manganew.reader.designsystem.theme.MangaNewTheme
import com.manganew.reader.feature.source.FakeSourceStore
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReducedMotionNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Before
    fun resetFakeSources() {
        FakeSourceStore.resetForTests()
    }

    @Test
    fun navigationRemainsUsableWithReduceMotionEnabled() {
        composeRule.setContent {
            MangaNewTheme(reduceMotion = true) {
                val navController = rememberNavController()
                MangaAppShell(navController = navController)
            }
        }

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithText("输入关键词开始搜索").assertIsDisplayed()
    }
}
