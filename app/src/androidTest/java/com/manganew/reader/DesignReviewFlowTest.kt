package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.feature.reader.ReaderViewportTestTag
import com.manganew.reader.feature.search.SearchInputTestTag
import com.manganew.reader.feature.settings.SettingsListTestTag
import com.manganew.reader.data.AppRepositoryContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DesignReviewFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun completePrototypeJourneyCoversCoreStatesAndInteractions() {
        composeRule.onNodeWithText("还没有可用来源").assertIsDisplayed()

        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithTag(SearchInputTestTag).performTextInput("星海")
        waitForText("星海旅人")

        composeRule.onNodeWithText("查看详情").performClick()
        waitForText("章节 · 4")

        composeRule.onAllNodesWithText("开始阅读").onFirst().performClick()
        waitForText("1 / 8")

        composeRule.onNodeWithTag(ReaderViewportTestTag).performTouchInput {
            click(center)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1 / 8").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithTag(ReaderViewportTestTag).performTouchInput {
            click(center)
        }
        waitForText("1 / 8")

        composeRule.onNodeWithText("返回").performClick()
        waitForText("章节 · 4")
        composeRule.onNodeWithText("返回").performClick()
        waitForText("星海旅人")

        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("深色").performScrollTo().performClick()
        composeRule.onNodeWithText("深色").assertIsSelected()

        composeRule.onNodeWithTag(SettingsListTestTag).performScrollToIndex(5)
        composeRule.onNodeWithText("减少动态效果").performClick()
        composeRule.onNodeWithText("减少动态效果").assertIsOn()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
