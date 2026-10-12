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
        composeRule.onNodeWithText("남").performClick()
        composeRule.onNodeWithText("우렁찬 아기").performScrollTo().performClick()
        composeRule.onNodeWithText("이 아기로 시작하기").performClick()

        waitForAny("chapter_content")
        chooseFirstFocus(screenshot = "02_chapter.png")

        // 자동 진행 화면은 시계를 멈춘 채로 확인한다. 첫해에 결정이 걸리면 이 단계는 건너뛴다.
        composeRule.mainClock.autoAdvance = false
        composeRule.mainClock.advanceTimeBy(200)
        if (composeRule.onAllNodesWithTag("auto_progress_content").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("play_pause").performClick()
            composeRule.mainClock.advanceTimeBy(600)
            composeRule.onNodeWithTag("year_log").assertIsDisplayed()
            composeRule.onNodeWithTag("speed_toggle").assertIsDisplayed()
            saveScreenshot("03_auto_progress.png")
            composeRule.onNodeWithTag("play_pause").performClick()
        }
        composeRule.mainClock.autoAdvance = true

        // 자동 진행은 결정 · 돌발 또는 다음 장 표지에서 멈춘다. 결정이 나올 때까지 진행한다.
        var reachedDecision = false
        repeat(12) {
            if (reachedDecision) return@repeat
            when (waitForAny("decision_content", "chapter_content", "auto_progress_content", "run_end_content")) {
                "chapter_content" -> chooseFirstFocus(screenshot = null)
                "auto_progress_content" -> composeRule.onNodeWithText("다음 결정까지 ›").performClick()
                "run_end_content" -> fail("결정이 한 번도 나오지 않고 인생이 끝났다")
                "decision_content" -> reachedDecision = true
            }
        }
        assertTrue("결정 화면에 닿지 못했다", reachedDecision)

        if (composeRule.onAllNodesWithTag("incident_popup").fetchSemanticsNodes().isNotEmpty()) {
            saveScreenshot("04_incident_popup.png")
            composeRule.onNodeWithText("사건 보기").performScrollTo().performClick()
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("decision_content").assertIsDisplayed()
        composeRule.onNodeWithTag("decision_choice_A").performScrollTo().assertIsDisplayed()
        saveScreenshot("05_decision.png")

        // 숫자 확률은 어디에도 보이면 안 된다 (R-43).
        composeRule.onAllNodes(hasText("%", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("{name", substring = true)).assertCountEquals(0)

        composeRule.onNodeWithTag("decision_choice_A").performClick()
        composeRule.onNodeWithTag("decision_outcome").performScrollTo().assertIsDisplayed()
        saveScreenshot("06_outcome.png")
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
