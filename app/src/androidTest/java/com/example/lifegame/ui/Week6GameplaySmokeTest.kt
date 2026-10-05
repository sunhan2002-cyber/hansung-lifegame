package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** 실제 Activity와 assets 데이터를 사용해 화면 연결을 확인한다. */
@RunWith(AndroidJUnit4::class)
class Week6GameplaySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun newGameCanChooseAndContinueThroughTwoRealEvents() {
        composeRule.onNodeWithText("새 게임").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("아기 1").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("게임 시작").assertIsDisplayed().performClick()
        waitForScreen("game_content")
        saveScreenshot("game.png")

        // 사건 순서나 문구를 고정하지 않고 실제 사건의 첫 번째 선택지를 찾는다.
        val firstChoice = SemanticsMatcher("실제 사건의 A 선택지") { node ->
            node.config.getOrNull(SemanticsProperties.Text)
                ?.any { it.text == "A" } == true
        } and hasClickAction()

        repeat(2) { index ->
            composeRule.onNodeWithTag("game_content").performScrollToNode(firstChoice)
            val choiceNode = composeRule.onNode(firstChoice).assertIsDisplayed()
            val selectedLabel = choiceNode.fetchSemanticsNode()
                .config[SemanticsProperties.Text]
                .first { it.text != "A" }.text
            if (index == 0) saveScreenshot("choices.png")
            choiceNode.performClick()

            waitForScreen("game_content")
            composeRule.onNodeWithTag("game_content").performScrollToNode(hasText("선택 결과"))
            composeRule.onNodeWithText("선택 결과").assertIsDisplayed()
            composeRule.onNodeWithTag("game_content")
                .performScrollToNode(hasText("→ $selectedLabel"))
            composeRule.onNodeWithText("→ $selectedLabel").assertIsDisplayed()
            if (index == 0) saveScreenshot("result.png")

            composeRule.onNodeWithTag("game_content")
                .performScrollToNode(hasText("다음 해로", substring = true))
            composeRule.onNodeWithText("다음 해로", substring = true).assertIsDisplayed().performClick()
            waitForScreen("game_content")
        }

        // 두 번 모두 같은 게임 화면 안에서 결과를 확인하고 다음 사건으로 전환해야 한다.
        composeRule.onNodeWithTag("game_content").assertIsDisplayed()
    }

    private fun waitForScreen(tag: String) {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size == 1
        }
        composeRule.waitForIdle()
        if (composeRule.onAllNodesWithTag("special_popup").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithText("사건 보기").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getExternalFilesDir("week6-uiux"),
        ) { "화면 검증 이미지를 저장할 앱 외부 파일 경로가 없습니다." }
        assertTrue("화면 검증 디렉터리를 만들 수 없습니다.", directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue("화면 검증 이미지를 저장하지 못했습니다.", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
