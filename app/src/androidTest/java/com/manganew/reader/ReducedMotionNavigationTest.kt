package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.manganew.reader.designsystem.theme.MangaNewTheme
import org.junit.Rule
import org.junit.Test

class ReducedMotionNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun navigationRemainsUsableWithReduceMotionEnabled() {
        composeRule.setContent {
            MangaNewTheme(reduceMotion = true) {
                val navController = rememberNavController()
                MangaAppShell(navController = navController)
            }
        }

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithText("搜索输入和结果状态将在后续阶段接入。").assertIsDisplayed()
    }
}
