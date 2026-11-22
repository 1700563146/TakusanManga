package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.data.AppRepositoryContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SourceManagementTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetFakeSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun homeCoversEmptyErrorAndRetryStates() {
        composeRule.onNodeWithText("还没有可用来源").assertIsDisplayed()

        composeRule.onNodeWithText("预览错误状态").performClick()
        composeRule.onNodeWithText("首页暂时无法加载").assertIsDisplayed()

        composeRule.onNodeWithText("重试").performClick()
        composeRule.onNodeWithText("还没有可用来源").assertIsDisplayed()
    }

    @Test
    fun importToggleTestAndDeleteSourcePrototype() {
        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")

        composeRule.onNodeWithContentDescription("来源").performClick()
        composeRule.onNodeWithText("示例漫画站").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("停用 示例漫画站").performClick()
        composeRule.onNodeWithContentDescription("启用 示例漫画站").assertIsDisplayed()

        composeRule.onNodeWithText("查看详情").performClick()
        waitForText("来源详情")
        composeRule.onNodeWithText("example.test").assertIsDisplayed()

        composeRule.onNodeWithText("重新导入").performClick()
        waitForText("1.0.1")

        composeRule.onNodeWithText("测试来源").performClick()
        waitForText("连接测试成功")

        composeRule.onNodeWithText("删除来源").performClick()
        waitForText("来源列表为空")
    }

    @Test
    fun sourceListCoversEmptyErrorAndRetryStates() {
        composeRule.onNodeWithContentDescription("来源").performClick()
        composeRule.onNodeWithText("来源列表为空").assertIsDisplayed()

        composeRule.onNodeWithText("预览错误状态").performClick()
        composeRule.onNodeWithText("来源列表加载失败").assertIsDisplayed()

        composeRule.onNodeWithText("重试").performClick()
        composeRule.onNodeWithText("来源列表为空").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
