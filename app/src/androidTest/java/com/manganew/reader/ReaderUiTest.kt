package com.manganew.reader

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.manganew.reader.feature.reader.ReaderPageTestTagPrefix
import com.manganew.reader.feature.reader.ReaderViewportTestTag
import com.manganew.reader.data.AppRepositoryContainer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReaderUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSources() {
        AppRepositoryContainer.resetForTests()
        composeRule.waitForIdle()
    }

    @Test
    fun tapZonesFlipPagesAndSwipeStillWorks() {
        openReader()
        val viewport = composeRule.onNodeWithTag(ReaderViewportTestTag)

        viewport.performTouchInput {
            click(Offset(width * 0.9f, height * 0.5f))
        }
        waitForText("2 / 8")

        viewport.performTouchInput {
            swipeLeft()
        }
        waitForText("3 / 8")
    }

    @Test
    fun centerTapTogglesToolbarAndModeSwitchWorks() {
        openReader()
        val viewport = composeRule.onNodeWithTag(ReaderViewportTestTag)

        viewport.performTouchInput {
            click(center)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1 / 8").fetchSemanticsNodes().isEmpty()
        }

        viewport.performTouchInput {
            click(center)
        }
        waitForText("1 / 8")

        composeRule.onNodeWithText("切换为纵向").performClick()
        waitForText("切换为分页")
    }

    @Test
    fun doubleTapZoomBlocksTapFlipUntilReset() {
        openReader()
        val viewport = composeRule.onNodeWithTag(ReaderViewportTestTag)

        viewport.performTouchInput {
            doubleClick(center)
        }
        composeRule.onNodeWithTag("${ReaderPageTestTagPrefix}1").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "缩放 2.0×",
            ),
        )

        viewport.performTouchInput {
            click(Offset(width * 0.9f, height * 0.5f))
        }
        composeRule.onNodeWithText("1 / 8").assertIsDisplayed()

        viewport.performTouchInput {
            doubleClick(center)
        }
        composeRule.onNodeWithTag("${ReaderPageTestTagPrefix}1").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                "原始大小",
            ),
        )

        viewport.performTouchInput {
            click(Offset(width * 0.9f, height * 0.5f))
        }
        waitForText("2 / 8")
    }

    private fun openReader() {
        composeRule.onNodeWithText("导入示例来源").performClick()
        waitForText("示例漫画站")
        composeRule.onNodeWithText("打开阅读器骨架").performClick()
        waitForText("1 / 8")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
    }
}
