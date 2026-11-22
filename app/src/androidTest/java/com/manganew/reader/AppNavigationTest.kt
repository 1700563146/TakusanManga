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
class AppNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetFakeSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNavigationMovesBetweenTopLevelScreens() {
        composeRule.onNodeWithText("还没有可用来源").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("来源").performClick()
        composeRule.onNodeWithText("来源管理").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithText("输入关键词开始搜索").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("调整主题、阅读和动效偏好，修改会立即影响当前原型。").assertIsDisplayed()
    }

    @Test
    fun readerRouteHidesBottomNavigationAndCanReturnHome() {
        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")
        composeRule.onNodeWithText("打开阅读器骨架").performClick()

        waitForText("1 / 8")
        composeRule.onNodeWithText("第 1 话 · 失联信号").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("首页").assertDoesNotExist()

        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithContentDescription("首页").assertIsDisplayed()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
