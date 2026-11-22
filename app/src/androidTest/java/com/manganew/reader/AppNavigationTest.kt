package com.manganew.reader

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.feature.source.FakeSourceStore
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
        FakeSourceStore.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNavigationMovesBetweenTopLevelScreens() {
        composeRule.onNodeWithText("还没有可用来源").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("来源").performClick()
        composeRule.onNodeWithText("来源管理").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("搜索").performClick()
        composeRule.onNodeWithText("搜索输入和结果状态将在后续阶段接入。").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("设置").performClick()
        composeRule.onNodeWithText("主题、阅读模式和动效设置将在后续阶段接入。").assertIsDisplayed()
    }

    @Test
    fun readerRouteHidesBottomNavigationAndCanReturnHome() {
        composeRule.onNodeWithText("打开阅读器骨架").performClick()

        composeRule.onNodeWithText("阅读器").assertIsDisplayed()
        composeRule.onNodeWithText("阅读器路由测试：demo-chapter").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("首页").assertDoesNotExist()

        composeRule.onNodeWithText("返回").performClick()
        composeRule.onNodeWithContentDescription("首页").assertIsDisplayed()
    }
}
