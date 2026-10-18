package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.MainActivity
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 7주차 v4 흐름: 홈 → 프로필 만들기 → 장 표지(초점) → 자동 진행 → 결정 · 돌발 → 결과.
 * 실제 Activity와 assets 데이터를 쓴다. 화면 캡처는 앱 외부 파일 폴더의 week7-frontend에 남긴다.
 */
@RunWith(AndroidJUnit4::class)
class Week7FlowSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeProfileChapterAutoProgressAndDecisionAreConnected() {
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        saveScreenshot("01_home.png")

        composeRule.onNodeWithText("새 인생 살기").performClick()
        waitForAny("profile_content")
        composeRule.onNodeWithText("남").performClick()
        composeRule.onNodeWithText("우렁찬 아기").performScrollTo().performClick()
        composeRule.onNodeWithText("이 아기로 시작하기").performClick()

        // 영아기는 초점 없이 바로 진행되고(매해 사건 보장), 첫 장 표지는 유년기에 나온다.
        // 결정 화면과 장 표지를 한 번씩 볼 때까지 진행한다.
        var sawChapter = false
        var sawDecision = false
        var sawAuto = false
        repeat(60) {
            if (sawChapter && sawDecision) return@repeat
            when (waitForAny("decision_content", "chapter_content", "auto_progress_content", "run_end_content")) {
                "chapter_content" -> {
                    chooseFirstFocus(screenshot = if (sawChapter) null else "02_chapter.png")
                    sawChapter = true
                }
                "auto_progress_content" -> {
                    if (!sawAuto) {
                        composeRule.onNodeWithTag("life_path_summary").assertIsDisplayed()
                        saveScreenshot("03_auto_progress.png")
                        sawAuto = true
                    }
                    composeRule.onNodeWithText("다음 결정까지 ›").performClick()
                }
                "decision_content" -> {
                    resolveDecision(screenshot = !sawDecision)
                    sawDecision = true
                }
                "run_end_content" -> fail("장 표지와 결정을 다 보기 전에 인생이 끝났다")
            }
        }
        assertTrue("결정 화면에 닿지 못했다", sawDecision)
        assertTrue("장 표지에 닿지 못했다", sawChapter)
    }

    /** 돌발이면 팝업을 닫고, 고를 수 있는 첫 선택지(A)를 고른 뒤 결과에서 계속한다. */
    private fun resolveDecision(screenshot: Boolean) {
        if (composeRule.onAllNodesWithTag("incident_popup").fetchSemanticsNodes().isNotEmpty()) {
            if (screenshot) saveScreenshot("04_incident_popup.png")
            composeRule.onNodeWithText("사건 보기").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("decision_choice_A").performScrollTo().assertIsDisplayed()
        if (screenshot) {
            saveScreenshot("05_decision.png")
            // 숫자 확률과 이름 자리 표시는 어디에도 보이면 안 된다 (R-43).
            composeRule.onAllNodes(hasText("%", substring = true)).assertCountEquals(0)
            composeRule.onAllNodes(hasText("{name", substring = true)).assertCountEquals(0)
        }
        // 글자가 다 나오기 전에는 선택지가 눌리지 않으므로 결과가 뜰 때까지 다시 누른다.
        composeRule.waitUntil(timeoutMillis = 20_000) {
            if (composeRule.onAllNodesWithTag("decision_outcome").fetchSemanticsNodes().isEmpty()) {
                composeRule.onNodeWithTag("decision_choice_A").performScrollTo().performClick()
            }
            composeRule.onAllNodesWithTag("decision_outcome").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("decision_outcome").performScrollTo().assertIsDisplayed()
        if (screenshot) saveScreenshot("06_outcome.png")
        val next = if (composeRule.onAllNodes(hasText("계속하기")).fetchSemanticsNodes().isNotEmpty()) "계속하기" else "인생 정리하기"
        composeRule.onNodeWithText(next).performScrollTo().performClick()
    }

    private fun chooseFirstFocus(screenshot: String?) {
        composeRule.onAllNodesWithTag("focus_option")[0].performScrollTo().performClick()
        if (screenshot != null) saveScreenshot(screenshot)
        composeRule.onNodeWithText("시작하기", substring = true).performClick()
    }

    /** 태그 중 하나가 화면에 나타날 때까지 기다리고, 나타난 태그를 돌려준다. */
    private fun waitForAny(vararg tags: String): String {
        var found: String? = null
        composeRule.waitUntil(timeoutMillis = 20_000) {
            found = tags.firstOrNull { tag ->
                composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
            found != null
        }
        return requireNotNull(found)
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(
            InstrumentationRegistry.getInstrumentation().targetContext
                .getExternalFilesDir("week7-frontend"),
        ) { "화면 검증 이미지를 저장할 앱 외부 파일 경로가 없습니다." }
        assertTrue("화면 검증 디렉터리를 만들 수 없습니다.", directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue("화면 검증 이미지를 저장하지 못했습니다.", bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
