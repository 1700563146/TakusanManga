package com.manganew.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.feature.reader.ReaderViewportTestTag
import com.manganew.reader.feature.settings.SettingsListTestTag
import com.manganew.reader.data.AppRepositoryContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun themeSelectionChangesImmediately() {
        openSettings()

        composeRule.onNodeWithText("深色").performScrollTo().performClick()
        composeRule.onNodeWithText("深色").assertIsSelected()

        composeRule.onNodeWithText("浅色").performScrollTo().performClick()
        composeRule.onNodeWithText("浅色").assertIsSelected()
    }

    @Test
    fun settingsPropagateToReaderModeDirectionAndConcurrency() {
        openSettings()

        composeRule.onNodeWithText("纵向连续阅读").performScrollTo().performClick()
        composeRule.onNodeWithText("从右到左").performScrollTo().performClick()
        composeRule.onNodeWithText("减少动态效果").performScrollTo().performClick()
        composeRule.onNodeWithTag(SettingsListTestTag).performScrollToIndex(6)
        composeRule.onNodeWithText("4 张").performClick()

        composeRule.onNodeWithContentDescription("首页").performClick()
        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")
        composeRule.onNodeWithText("打开阅读器骨架").performClick()
        waitForText("1 / 8")

        composeRule.onNodeWithText("切换为分页").assertIsDisplayed()
        composeRule.onNodeWithText("右 → 左").assertIsDisplayed()
        composeRule.onNodeWithText("并发 4").assertIsDisplayed()

        composeRule.onNodeWithText("切换为分页").performClick()
        composeRule.onNodeWithTag(ReaderViewportTestTag).performTouchInput {
            click(Offset(width * 0.1f, height * 0.5f))
        }
        waitForText("2 / 8")
    }

    private fun openSettings() {
        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("主题").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
