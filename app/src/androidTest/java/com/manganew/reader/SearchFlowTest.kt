package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.feature.search.SearchInputTestTag
import com.manganew.reader.data.AppRepositoryContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchFlowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun searchResultOpensDetailAndChapterReader() {
        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")
        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithTag(SearchInputTestTag).performTextInput("星海")

        waitForText("星海旅人")
        composeRule.onNodeWithText("查看详情").performClick()

        waitForText("章节 · 4")
        composeRule.onNodeWithText("第 1 话 · 失联信号").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("阅读 第 1 话 · 失联信号").performClick()
        waitForText("1 / 8")
    }

    @Test
    fun searchCoversErrorEmptyClearAndRetryStates() {
        composeRule.onNodeWithContentDescription("搜索").performClick()

        composeRule.onNodeWithText("预览错误状态").performClick()
        composeRule.onNodeWithText("搜索暂时失败").assertIsDisplayed()

        composeRule.onNodeWithText("重试").performClick()
        composeRule.onNodeWithText("输入关键词开始搜索").assertIsDisplayed()

        composeRule.onNodeWithTag(SearchInputTestTag).performTextInput("不存在的漫画")
        waitForText("没有找到漫画")

        composeRule.onNodeWithText("清除").performClick()
        composeRule.onNodeWithText("输入关键词开始搜索").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
