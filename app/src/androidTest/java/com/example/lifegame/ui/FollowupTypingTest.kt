package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.screens.TYPE_MILLIS
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-10-31 후속 과제 2번: 본문이 한 글자씩 나오는 중에 터치하면 그 사건 본문 전체를 바로 보여 준다.
 * 시계를 멈춘 상태에서 일부 출력 → 한 번 터치 → 전문 표시를 보고, 그 터치가 선택 · 장면 보기 · 진행을 부르지 않는지 센다.
 * 본문은 실제 콘텐츠의 긴 사건(대사 포함)을 쓰고, 줄바꿈 · 이모지 경계 예시를 따로 둔다.
 */
@RunWith(AndroidJUnit4::class)
class FollowupTypingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val periods = V4ContentParser.parsePeriods(context.assets.open("periods.sample.json").bufferedReader().use { it.readText() })
    private val events = V4ContentParser.parseDecisionEvents(
        context.assets.open("events.sample.json").bufferedReader().use { it.readText() },
    ).associateBy { it.eventId }

    private var chooseCount = 0
    private var continueCount = 0

    private fun decisionOf(event: DecisionEvent, kind: DecisionKind = event.kind) = DecisionUi(
        eventId = event.eventId,
        kind = kind,
        title = event.title,
        text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
        imageId = "",
        imageFile = null,
        choices = event.choices.take(3).mapIndexed { index, option ->
            DecisionChoiceUi(option.choiceId, ('A' + index).toString(), option.label, hint = "")
        },
    )

    /** 실제 콘텐츠에서 가장 긴 전환점 본문(대사 포함) */
    private val longMilestone get() = decisionOf(events.getValue("adult_072"))

    private fun playState(decision: DecisionUi, outcome: OutcomeUi? = null, yearKey: String = "k1") = PeriodPlayUiState(
        runKey = "typing",
        playerName = "재겸",
        phase = PlayPhase.DECISION,
        periodName = "청년기",
        chapterLabel = "제4장",
        periodYears = 5,
        periodIntro = "",
        yearInPeriod = 2,
        periodMarks = periods.sortedBy { it.order }.map { PeriodMarkUi(it.shortName(), 0f, it.displayName == "청년기") },
        previousPeriodName = "청소년기",
        stats = Stats(health = 45),
        focus = null,
        focusOptions = emptyList(),
        sceneImageId = "",
        sceneFile = null,
        log = emptyList(),
        yearKey = yearKey,
        decision = decision,
        outcome = outcome,
        isDead = false,
        autoPlaying = false,
        speed = 1,
    )

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { content() }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun shownBody(): String =
        composeRule.onNodeWithTag("decision_body", useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun tapCenterOf(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(center) }
        composeRule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun tapOnAChoiceWhileTypingOnlyCompletesTheBody() {
        val decision = longMilestone
        setScreen { DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = { continueCount++ }) }
        advance(TYPE_MILLIS * 12)
        val partial = shownBody()
        assertTrue("일부만 나와 있어야 한다: $partial", partial.isNotEmpty() && partial.length < decision.text.length)
        saveScreenshot("followup_typing_partial.png")

        tapCenterOf("decision_choice_A")
        assertEquals(decision.text, shownBody())
        assertEquals("완성 터치가 선택을 확정하면 안 된다", 0, chooseCount)
        assertEquals(0, continueCount)
        saveScreenshot("followup_typing_completed.png")

        // 남아 있던 타이핑 작업이 다시 글자 수를 줄이지 않는다.
        advance(TYPE_MILLIS * 200)
        assertEquals(decision.text, shownBody())

        // 완성 뒤 선택지를 누르면 한 번만 반영된다.
        advance(TAP_GUARD_MILLIS + 50)
        tapCenterOf("decision_choice_A")
        assertEquals(1, chooseCount)
    }

    @Test
    fun tapOnTheSceneButtonWhileTypingDoesNotOpenTheScene() {
        val decision = longMilestone
        setScreen { DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = {}) }
        advance(TYPE_MILLIS * 8)
        tapCenterOf("scene_view_button")
        assertEquals(decision.text, shownBody())
        composeRule.onAllNodesWithTag("scene_only").assertCountEquals(0)

        // 완성 뒤에는 장면 보기가 열리고, 한 번 터치로 돌아와도 본문을 다시 타이핑하지 않는다.
        advance(TAP_GUARD_MILLIS + 50)
        tapCenterOf("scene_view_button")
        composeRule.onNodeWithTag("scene_only").assertIsDisplayed()
        composeRule.onNodeWithTag("scene_clear").assertExists()
        composeRule.onAllNodesWithTag("play_top_bar").assertCountEquals(0)
        tapCenterOf("scene_only")
        advance(TYPE_MILLIS * 3)
        assertEquals(decision.text, shownBody())
        assertEquals(0, chooseCount)
    }

    @Test
    fun rapidTapsAndBackgroundTapsNeverSelect() {
        val decision = longMilestone
        setScreen { DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = { continueCount++ }) }
        advance(TYPE_MILLIS * 5)
        // 배경(제목) 터치로 완성하고, 곧바로 선택지를 연타해도 확정되지 않는다.
        tapCenterOf("decision_title")
        assertEquals(decision.text, shownBody())
        repeat(3) {
            advance(40)
            tapCenterOf("decision_choice_B")
        }
        assertEquals(0, chooseCount)
        assertEquals(0, continueCount)
    }

    @Test
    fun nextEventStartsTypingAgainWithoutLeakingTheCompletedState() {
        var state by mutableStateOf(playState(longMilestone, yearKey = "y1"))
        setScreen { DecisionScreen(state, onChoose = { chooseCount++ }, onContinue = {}) }
        advance(TYPE_MILLIS * 4)
        tapCenterOf("decision_body")
        assertEquals(longMilestone.text, shownBody())

        val next = decisionOf(events.getValue("path_wea_03"))
        state = playState(next, yearKey = "y2")
        advance(TYPE_MILLIS * 6)
        val partial = shownBody()
        assertTrue("다음 사건은 처음부터 다시 나온다: $partial", partial.length < next.text.length)
        assertNotEquals(next.text, partial)
        assertEquals(0, chooseCount)
    }

    @Test
    fun incidentPopupClosesFirstAndItsTapDoesNotChoose() {
        val incident = decisionOf(events.getValue("path_luk_17"), DecisionKind.INCIDENT)
        setScreen { DecisionScreen(playState(incident), onChoose = { chooseCount++ }, onContinue = {}) }
        advance(2_000)
        composeRule.onNodeWithTag("incident_popup").assertIsDisplayed()
        composeRule.onNodeWithText("사건 보기").performClick()
        advance(400)
        composeRule.onAllNodesWithTag("incident_popup").assertCountEquals(0)
        advance(TYPE_MILLIS * 6)
        val partial = shownBody()
        assertTrue("팝업을 닫은 뒤 본문이 나오기 시작한다: $partial", partial.length < incident.text.length)
        tapCenterOf("decision_choice_A")
        assertEquals(incident.text, shownBody())
        assertEquals(0, chooseCount)
    }

    @Test
    fun completedBodySurvivesStateRestoration() {
        val restoration = StateRestorationTester(composeRule)
        val decision = longMilestone
        composeRule.mainClock.autoAdvance = false
        restoration.setContent {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                    DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = {})
                }
            }
        }
        advance(TYPE_MILLIS * 5)
        tapCenterOf("decision_body")
        assertEquals(decision.text, shownBody())
        restoration.emulateSavedInstanceStateRestore()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("복원 뒤에도 다시 타이핑하지 않는다", decision.text, shownBody())
        assertEquals(0, chooseCount)
    }

    @Test
    fun longTextWithLineBreakAndEmojiCompletesExactly() {
        // 실제 콘텐츠에는 아직 줄바꿈 · 이모지 본문이 없어 경계 예시로 확인한다.
        val sample = longMilestone.copy(
            eventId = "typing_edge",
            text = "엄마: \"재겸아, 밥은 먹었니? 🍚\"\n휴대폰 화면에 메시지가 줄줄이 쌓였다. 답장을 쓰다 지우고, 또 쓰다 지웠다. 👀",
        )
        setScreen { DecisionScreen(playState(sample), onChoose = { chooseCount++ }, onContinue = {}) }
        advance(TYPE_MILLIS * 14)
        val partial = shownBody()
        assertTrue(partial.isNotEmpty() && partial.length < sample.text.length)
        assertTrue("반쪽 이모지가 보이면 안 된다", partial.isEmpty() || !Character.isHighSurrogate(partial.last()))
        tapCenterOf("decision_choice_C")
        assertEquals(sample.text, shownBody())
        assertEquals(0, chooseCount)
    }

    @Test
    fun reducedMotionShowsTheWholeBodyAtOnce() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String): String =
            android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }
        val original = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 0")
            val decision = longMilestone
            setScreen { DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = {}) }
            advance(TYPE_MILLIS)
            assertEquals("동작 줄이기면 처음부터 전문", decision.text, shownBody())
            // 이미 완성된 상태라 첫 터치가 그대로 선택이 된다(삼키는 단계 없음).
            tapCenterOf("decision_choice_A")
            assertEquals(1, chooseCount)
        } finally {
            shell("settings put global animator_duration_scale ${original.ifBlank { "1" }.let { if (it == "null") "1" else it }}")
        }
    }

    @Test
    fun outcomeContinueIsCalledOnceWithoutTyping() {
        val outcome = OutcomeUi(choiceLabel = "고른 길", text = "결과 문장", changes = emptyList(), endsLife = false)
        setScreen { DecisionScreen(playState(longMilestone, outcome = outcome), onChoose = { chooseCount++ }, onContinue = { continueCount++ }) }
        // 결과 화면은 처음부터 전문이 보이고, 계속하기는 한 번 누르면 한 번 불린다.
        composeRule.onNodeWithText("계속하기").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, continueCount)
        assertEquals(0, chooseCount)
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.mainClock.advanceTimeByFrame()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(context.getExternalFilesDir("followup-20261031"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
