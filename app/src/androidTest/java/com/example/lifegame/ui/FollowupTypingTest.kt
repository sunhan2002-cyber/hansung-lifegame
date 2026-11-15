package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
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
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.CHANGE_HOLD_MILLIS
import com.example.lifegame.ui.screens.PREVIEW_HOLD_MILLIS
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
 * 사건 화면의 표시 순서와 터치 보호.
 *
 * 2026-11-13 통합지시서 · 김재겸 과제 2 · 3: 사건 그림 선노출 → 제목 · 본문 타이핑 → 선택지.
 * 선노출이 터치 · 시간 초과 · 뒤로가기로 끝날 때와 본문이 끝나 선택지가 나타날 때 그 순간의 터치가 선택으로 넘어가지 않는다.
 * 그림을 읽지 못하면 선노출을 건너뛰고 "본 그림"으로 저장하지 않는다.
 * 2026-10-31 후속 과제 2번(타이핑 중 터치는 본문 완성만), 2026-11-06 요구 7 · 9(선택지 보조 태그 접기, 결과 변화 문구)는 그대로 지킨다.
 *
 * 그림은 다른 스레드에서 읽으므로, 그림이 걸린 경우는 실제 시간을 조금씩 흘리며 프레임을 넘겨 화면이 나타날 때까지 기다린다([awaitTag]).
 * 그림이 없는 사건(파일 없음)은 곧바로 "읽기 실패"가 되어 선노출 없이 본문부터 나온다.
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
    private var imageShownCount = 0

    /** 승인된 실제 사건 그림 하나(크립토 사건). 선노출이 필요한 테스트에 붙인다. */
    private val sceneImage = ImageCandidate("img_adult_crypto_001", "followup_20261031/img_adult_crypto_001.png")

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

    private fun DecisionUi.withScene() = copy(imageId = sceneImage.imageId, imageFile = sceneImage.path, images = listOf(sceneImage))

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

    private var backDispatcher: OnBackPressedDispatcher? = null

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { content() }
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun screen(decision: DecisionUi, outcome: OutcomeUi? = null): @Composable () -> Unit = {
        DecisionScreen(
            playState(decision, outcome),
            onChoose = { chooseCount++ },
            onContinue = { continueCount++ },
            onEventImageShown = { imageShownCount++ },
        )
    }

    private fun shownBody(): String =
        composeRule.onNodeWithTag("decision_body", useUnmergedTree = true).fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    private fun advance(millis: Long) {
        composeRule.mainClock.advanceTimeBy(millis)
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun count(tag: String) = composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().size

    /** 그림 읽기처럼 다른 스레드 일을 기다린다. 실제 시간을 조금씩 흘리며 한 프레임씩 넘긴다. */
    private fun awaitTag(tag: String, timeoutMs: Long = 5_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (count(tag) == 0) {
            check(System.currentTimeMillis() < end) { "$tag 화면이 나타나지 않았다" }
            Thread.sleep(10)
            composeRule.mainClock.advanceTimeByFrame()
        }
    }

    private fun tapCenterOf(tag: String) {
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(center) }
        composeRule.mainClock.advanceTimeByFrame()
    }

    /** 그림 선노출을 한 번 눌러 넘기고, 연타 보호 시간까지 기다린다. */
    private fun passPreview() {
        awaitTag("scene_preview")
        tapCenterOf("scene_preview")
        advance(TAP_GUARD_MILLIS + 50)
    }

    /** 본문을 한 번 눌러 끝까지 보이고, 선택지가 나타난 뒤의 보호 시간까지 기다린다. */
    private fun completeBodyAndWait() {
        awaitTag("decision_body")
        tapCenterOf("decision_title")
        advance(TAP_GUARD_MILLIS + 50)
    }

    @Test
    fun sceneImageComesFirstThenBodyThenChoices() {
        val decision = longMilestone.withScene()
        setScreen(screen(decision))
        // 1) 그림이 먼저: 블러 없이 그림만, 제목 · 본문 · 선택지는 아직 없다.
        awaitTag("scene_preview")
        composeRule.onNodeWithTag("scene_clear").assertExists()
        assertEquals("선노출 동안 본문이 없다", 0, count("decision_body"))
        assertEquals("선노출 동안 선택지가 없다", 0, count("decision_choice_A"))
        assertEquals("선노출이 끝나기 전에는 본 그림으로 저장하지 않는다", 0, imageShownCount)
        composeRule.onNodeWithText("누르면 본문으로").assertExists()
        saveScreenshot("w13_order_1_scene.png")

        // 2) 그림을 넘기면 본문이 한 글자씩 나온다. 선택지는 아직 없다.
        tapCenterOf("scene_preview")
        assertEquals(0, chooseCount)
        assertEquals("선노출을 본 뒤 한 번 저장", 1, imageShownCount)
        advance(TAP_GUARD_MILLIS + 50)
        advance(TYPE_MILLIS * 10)
        val partial = shownBody()
        assertTrue("본문이 타이핑 중이다: $partial", partial.isNotEmpty() && partial.length < decision.text.length)
        assertEquals(0, count("decision_choice_A"))
        composeRule.onNodeWithTag("scene_blurred").assertExists()
        saveScreenshot("w13_order_2_body.png")

        // 3) 본문이 끝나면 선택지가 나온다. 완성 터치는 선택이 아니다.
        tapCenterOf("decision_title")
        assertEquals(decision.text, shownBody())
        assertEquals(0, chooseCount)
        advance(TAP_GUARD_MILLIS + 50)
        composeRule.onNodeWithTag("decision_choice_A").assertExists()
        saveScreenshot("w13_order_3_choices.png")
        tapCenterOf("decision_choice_A")
        tapCenterOf("decision_choice_A")
        assertEquals("선택은 한 번만 반영", 1, chooseCount)
        assertEquals(1, imageShownCount)
    }

    @Test
    fun tapWhileTypingOnlyCompletesTheBody() {
        val decision = longMilestone
        setScreen(screen(decision))
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 12)
        val partial = shownBody()
        assertTrue("일부만 나와 있어야 한다: $partial", partial.isNotEmpty() && partial.length < decision.text.length)
        // 본문이 나오는 동안에는 선택지 · 장면 보기 버튼이 아직 없다.
        composeRule.onAllNodesWithTag("decision_choice_A").assertCountEquals(0)
        composeRule.onAllNodesWithTag("scene_view_button").assertCountEquals(0)

        tapCenterOf("decision_title")
        assertEquals(decision.text, shownBody())
        assertEquals("완성 터치가 선택을 확정하면 안 된다", 0, chooseCount)
        assertEquals(0, continueCount)

        // 남아 있던 타이핑 작업이 다시 글자 수를 줄이지 않는다.
        advance(TYPE_MILLIS * 20)
        assertEquals(decision.text, shownBody())
        advance(TAP_GUARD_MILLIS)
        tapCenterOf("decision_choice_A")
        tapCenterOf("decision_choice_A")
        assertEquals(1, chooseCount)
    }

    @Test
    fun aFailedImageSkipsThePreviewAndIsNotMarkedShown() {
        val missing = ImageCandidate("img_missing", "followup_20261031/img_does_not_exist_001.png")
        val decision = longMilestone.copy(imageId = missing.imageId, imageFile = missing.path, images = listOf(missing))
        setScreen(screen(decision))
        awaitTag("decision_body")
        assertEquals("그림을 읽지 못하면 선노출을 건너뛴다", 0, count("scene_preview"))
        completeBodyAndWait()
        tapCenterOf("decision_choice_B")
        assertEquals(1, chooseCount)
        assertEquals("읽지 못한 그림은 본 것으로 저장하지 않는다", 0, imageShownCount)
    }

    @Test
    fun aTapJustAfterThePreviewTimesOutOnlyStartsTheBody() {
        val decision = longMilestone.withScene()
        setScreen(screen(decision))
        awaitTag("scene_preview")
        // 누르지 않고 기다려 저절로 끝나게 한 뒤, 곧바로 화면을 누른다.
        advance(PREVIEW_HOLD_MILLIS + 16)
        composeRule.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        assertEquals(1, imageShownCount)
        advance(40)
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals("선노출이 끝나는 순간의 터치는 선택이 아니다", 0, chooseCount)
        // 그 터치는 (보호 시간 뒤라면) 본문 완성으로만 쓰이고, 선택지는 본문 다음에 나온다.
        assertEquals(0, continueCount)
        composeRule.onNodeWithTag("decision_body").assertExists()
    }

    @Test
    fun aTapJustAsTheBodyFinishesOnItsOwnDoesNotChoose() {
        val decision = longMilestone
        setScreen(screen(decision))
        awaitTag("decision_body")
        // 누르지 않고 본문이 저절로 끝나게 둔다. 그 순간 선택지가 나타난다.
        advance(TYPE_MILLIS * (decision.text.length + 4))
        assertEquals(decision.text, shownBody())
        composeRule.onNodeWithTag("decision_choice_B").assertExists()
        advance(40)
        tapCenterOf("decision_choice_B")
        assertEquals("본문이 끝나 선택지가 막 나타난 순간의 터치는 선택이 아니다", 0, chooseCount)
        advance(TAP_GUARD_MILLIS + 50)
        tapCenterOf("decision_choice_B")
        assertEquals(1, chooseCount)
    }

    @Test
    fun backDuringThePreviewMovesOnToTheBodyWithoutChoosing() {
        val decision = longMilestone.withScene()
        setScreen(screen(decision))
        awaitTag("scene_preview")
        composeRule.runOnUiThread { checkNotNull(backDispatcher).onBackPressed() }
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        composeRule.onNodeWithTag("decision_body").assertExists()
        advance(40)
        composeRule.onRoot().performTouchInput { click(center) }
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(0, chooseCount)
    }

    @Test
    fun incidentPopupOpensTheSceneBeforeTheBody() {
        val incident = decisionOf(events.getValue("path_luk_17"), DecisionKind.INCIDENT).withScene()
        setScreen(screen(incident))
        advance(2_000)
        composeRule.onNodeWithTag("incident_popup").assertIsDisplayed()
        composeRule.onNodeWithText("사건 보기").performClick()
        // "사건 보기" 다음은 그림, 그다음 본문.
        awaitTag("scene_preview")
        composeRule.onAllNodesWithTag("incident_popup").assertCountEquals(0)
        assertEquals(0, count("decision_body"))
        tapCenterOf("scene_preview")
        advance(TAP_GUARD_MILLIS + 50)
        advance(TYPE_MILLIS * 6)
        val partial = shownBody()
        assertTrue("그림 다음에 본문이 나오기 시작한다: $partial", partial.isNotEmpty() && partial.length < incident.text.length)
        assertEquals(0, chooseCount)
    }

    @Test
    fun sceneButtonWorksAfterTheChoicesAppear() {
        val decision = longMilestone.withScene()
        setScreen(screen(decision))
        passPreview()
        completeBodyAndWait()
        composeRule.onNodeWithTag("decision_choice_A").assertExists()
        // 장면 보기는 그대로: 블러를 풀고 UI를 숨기며, 한 번 터치로 돌아와도 다시 타이핑하거나 그림을 다시 띄우지 않는다.
        tapCenterOf("scene_view_button")
        composeRule.onNodeWithTag("scene_only").assertIsDisplayed()
        composeRule.onAllNodesWithTag("play_top_bar").assertCountEquals(0)
        tapCenterOf("scene_only")
        advance(PREVIEW_HOLD_MILLIS)
        composeRule.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        assertEquals(decision.text, shownBody())
        assertEquals(0, chooseCount)
        assertEquals(1, imageShownCount)
    }

    @Test
    fun rapidTapsAndBackgroundTapsNeverSelect() {
        val decision = longMilestone
        setScreen(screen(decision))
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 5)
        // 배경(제목) 터치로 완성하고, 곧바로 연타해도 아무것도 확정되지 않는다.
        tapCenterOf("decision_title")
        assertEquals(decision.text, shownBody())
        repeat(3) {
            advance(40)
            tapCenterOf("decision_title")
        }
        tapCenterOf("decision_choice_B")
        assertEquals(0, chooseCount)
        assertEquals(0, continueCount)
        advance(TAP_GUARD_MILLIS + 50)
        tapCenterOf("decision_choice_B")
        assertEquals(1, chooseCount)
    }

    @Test
    fun nextEventStartsTypingAgainWithoutLeakingTheCompletedState() {
        var state by mutableStateOf(playState(longMilestone, yearKey = "y1"))
        setScreen { DecisionScreen(state, onChoose = { chooseCount++ }, onContinue = {}) }
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 4)
        tapCenterOf("decision_body")
        assertEquals(longMilestone.text, shownBody())

        val next = decisionOf(events.getValue("path_wea_03"))
        state = playState(next, yearKey = "y2")
        composeRule.mainClock.advanceTimeByFrame()
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 6)
        val partial = shownBody()
        assertTrue("다음 사건은 처음부터 다시 나온다: $partial", partial.length < next.text.length)
        assertNotEquals(next.text, partial)
        assertEquals(0, chooseCount)
    }

    @Test
    fun incidentPopupClosesFirstAndItsTapDoesNotChoose() {
        val incident = decisionOf(events.getValue("path_luk_17"), DecisionKind.INCIDENT)
        setScreen(screen(incident))
        advance(2_000)
        composeRule.onNodeWithTag("incident_popup").assertIsDisplayed()
        composeRule.onNodeWithText("사건 보기").performClick()
        advance(400)
        composeRule.onAllNodesWithTag("incident_popup").assertCountEquals(0)
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 6)
        val partial = shownBody()
        assertTrue("팝업을 닫은 뒤 본문이 나오기 시작한다: $partial", partial.length < incident.text.length)
        tapCenterOf("decision_body")
        assertEquals(incident.text, shownBody())
        assertEquals(0, chooseCount)
    }

    @Test
    fun completedBodySurvivesStateRestoration() {
        val restoration = ManualRestoreHost(composeRule)
        val decision = longMilestone
        composeRule.mainClock.autoAdvance = false
        restoration.setContent {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                    DecisionScreen(playState(decision), onChoose = { chooseCount++ }, onContinue = {})
                }
            }
        }
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 5)
        tapCenterOf("decision_body")
        assertEquals(decision.text, shownBody())
        advance(TAP_GUARD_MILLIS + 50)
        restoration.recreate()
        assertEquals(1, restoration.recreations)
        assertEquals("복원 뒤에도 다시 타이핑하지 않는다", decision.text, shownBody())
        advance(PREVIEW_HOLD_MILLIS)
        composeRule.onAllNodesWithTag("scene_preview").assertCountEquals(0)
        composeRule.onNodeWithTag("decision_choice_A").assertExists()
        assertEquals(0, chooseCount)
    }

    @Test
    fun longTextWithLineBreakAndEmojiCompletesExactly() {
        // 실제 콘텐츠에는 아직 줄바꿈 · 이모지 본문이 없어 경계 예시로 확인한다.
        val sample = longMilestone.copy(
            eventId = "typing_edge",
            text = "엄마: \"재겸아, 밥은 먹었니? 🍚\"\n휴대폰 화면에 메시지가 줄줄이 쌓였다. 답장을 쓰다 지우고, 또 쓰다 지웠다. 👀",
        )
        setScreen(screen(sample))
        awaitTag("decision_body")
        advance(TYPE_MILLIS * 14)
        val partial = shownBody()
        assertTrue(partial.isNotEmpty() && partial.length < sample.text.length)
        assertTrue("반쪽 이모지가 보이면 안 된다", partial.isEmpty() || !Character.isHighSurrogate(partial.last()))
        tapCenterOf("decision_body")
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
            val decision = longMilestone.withScene()
            var state by mutableStateOf(playState(decision))
            setScreen { DecisionScreen(state, onChoose = { chooseCount++ }, onContinue = {}) }
            // 그림 선노출은 움직임 없이 그대로 보여 주고, 넘기면 본문은 처음부터 전문이다.
            passPreview()
            advance(TYPE_MILLIS)
            assertEquals("동작 줄이기면 처음부터 전문", decision.text, shownBody())
            advance(TAP_GUARD_MILLIS + 50)
            tapCenterOf("decision_choice_A")
            assertEquals(1, chooseCount)
            // 결과 변화는 움직이지 않는 고정 문구로 대신한다.
            state = playState(decision, outcome = OutcomeUi("고른 길", "결과 문장", listOf(StatChangeUi("건강", 3)), endsLife = false), yearKey = "k2")
            advance(300)
            composeRule.onNodeWithTag("outcome_stat_changes", useUnmergedTree = true).assertExists()
            composeRule.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
        } finally {
            shell("settings put global animator_duration_scale ${original.ifBlank { "1" }.let { if (it == "null") "1" else it }}")
        }
    }

    @Test
    fun outcomeContinueIsCalledOnceWithoutTyping() {
        val outcome = OutcomeUi(choiceLabel = "고른 길", text = "결과 문장", changes = emptyList(), endsLife = false)
        setScreen(screen(longMilestone, outcome))
        // 결과 화면은 처음부터 전문이 보이고, 계속하기는 한 번 누르면 한 번 불린다.
        composeRule.onNodeWithText("계속하기").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, continueCount)
        assertEquals(0, chooseCount)
    }

    @Test
    fun resultChangesFloatOverTheCardOnceThenFade() {
        val outcome = OutcomeUi(
            choiceLabel = "고른 길",
            text = "결과 문장",
            changes = listOf(StatChangeUi("건강", 3), StatChangeUi("지력", -2)),
            endsLife = false,
        )
        var state by mutableStateOf(playState(longMilestone, outcome = outcome, yearKey = "r1"))
        setScreen { DecisionScreen(state, onChoose = { chooseCount++ }, onContinue = { continueCount++ }) }
        advance(300)
        composeRule.onNodeWithTag("outcome_floating_changes", useUnmergedTree = true).assertExists()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentDescription, listOf("능력치 변화: 건강 +3, 지력 −2")))
        saveScreenshot("iter_outcome_changes_float.png")
        advance(CHANGE_HOLD_MILLIS + 600)
        composeRule.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithTag("outcome_text").assertIsDisplayed()
        // 같은 결과로 다시 그려도(재구성) 다시 떠오르지 않는다.
        state = state.copy(isDead = false)
        advance(300)
        composeRule.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
        composeRule.onNodeWithText("계속하기").performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertEquals(1, continueCount)
        assertEquals(0, chooseCount)
    }

    @Test
    fun choiceCardsLeadWithTheActionAndFoldStatTags() {
        val base = longMilestone
        val decision = base.copy(
            choices = listOf(
                DecisionChoiceUi("c1", "A", "주말마다 봉사를 나간다", hint = "가능성 높음 · 사회성 · 행복", healthEffectText = "건강 +10 회복"),
                DecisionChoiceUi("c2", "B", "집에서 쉰다", hint = "건강 · 행복"),
            ),
        )
        setScreen(screen(decision))
        completeBodyAndWait()
        // 기본: 행동 제목 + 가능성 · 건강 한 줄. 사회성 · 행복 태그는 접혀 있다(색 없는 보조 글자).
        composeRule.onNodeWithTag("choice_summary_A", useUnmergedTree = true).assertTextEquals("가능성 높음 · 건강 +10")
        composeRule.onAllNodesWithTag("choice_tags_A", useUnmergedTree = true).assertCountEquals(0)
        // 스크린리더에는 접힌 태그까지 함께 읽힌다.
        composeRule.onNodeWithTag("decision_choice_A").assert(
            SemanticsMatcher("접힌 태그도 설명에 포함") { node ->
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString().contains("능력치 영향 사회성, 행복")
            },
        )
        // 2026-11-09 보완 3번: 행동 제목이 건강 회복 문구보다 먼저(위 · 크게 · 읽기 순서 앞) 온다.
        val description = composeRule.onNodeWithTag("decision_choice_A").fetchSemanticsNode()
            .config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        assertTrue("설명은 행동 제목으로 시작: $description", description.startsWith("선택지 A. 주말마다 봉사를 나간다"))
        assertTrue("건강 문구는 제목 뒤", description.indexOf("건강 +10 회복") > description.indexOf("주말마다 봉사를 나간다"))
        val label = textStyleOf("choice_label_A")
        val summary = textStyleOf("choice_summary_A")
        assertTrue("제목 글자가 보조 줄보다 크다", label.fontSize.value > summary.fontSize.value)
        assertTrue("보조 줄은 제목보다 흐리다", summary.color.alpha < label.color.alpha)
        assertTrue("제목이 보조 줄보다 위", topOf("choice_label_A") < topOf("choice_summary_A"))
        saveScreenshot("iter_choice_cards_folded.png")
        tapCenterOf("choice_details_toggle")
        composeRule.onNodeWithTag("choice_tags_A", useUnmergedTree = true).assertTextEquals("영향: 사회성 · 행복")
        composeRule.onNodeWithTag("choice_tags_B", useUnmergedTree = true).assertTextEquals("영향: 건강 · 행복")
        // 보완 4번: 펼친 사회성 · 행복 태그도 제목보다 작고 흐리며 제목 아래에 있다(색 · 굵기 강조 없음).
        val tags = textStyleOf("choice_tags_A")
        assertTrue("태그 글자는 보조 줄 이하", tags.fontSize.value <= summary.fontSize.value)
        assertTrue("태그는 제목보다 흐리다", tags.color.alpha < label.color.alpha)
        assertTrue("태그는 굵게 강조하지 않는다", (tags.fontWeight ?: FontWeight.Normal) < FontWeight.SemiBold)
        assertTrue("태그는 제목 아래", topOf("choice_label_A") < topOf("choice_tags_A"))
        saveScreenshot("iter_choice_cards_expanded.png")
        assertEquals(0, chooseCount)
    }

    @Test
    fun recallAndReturningPersonShowOnlyWhenTheRecordHasThem() {
        var state by mutableStateOf(playState(longMilestone, yearKey = "plain"))
        setScreen { DecisionScreen(state, onChoose = { chooseCount++ }, onContinue = {}) }
        completeBodyAndWait()
        assertEquals("기록이 없으면 회상을 만들지 않는다", 0, count("decision_recall"))
        assertEquals(0, count("decision_npc"))

        // 실제 기록에서 조회된 값이 있으면 본문이 끝난 뒤 본문 아래에 한 줄씩 보인다.
        val withRecord = longMilestone.copy(
            recall = RecallLine("recall_dol_music", "돌잡이 때 피아노 장난감을 꼭 쥐던 그 손이었다."),
            returningNpc = NpcReturnUi("npc_transfer_child", "급식실 전학생", originTitle = "혼자 밥 먹는 전학생", originPeriodName = "유년기"),
        )
        state = playState(withRecord, yearKey = "recorded")
        composeRule.mainClock.advanceTimeByFrame()
        awaitTag("decision_body")
        assertEquals("본문이 끝나기 전에는 회상이 없다", 0, count("decision_recall"))
        tapCenterOf("decision_title")
        composeRule.onNodeWithTag("decision_recall").assertTextEquals("그때 그 선택 · 돌잡이 때 피아노 장난감을 꼭 쥐던 그 손이었다.")
        composeRule.onNodeWithTag("decision_npc").assertTextEquals("다시 만난 사람 · 급식실 전학생 (유년기 「혼자 밥 먹는 전학생」)")
        saveScreenshot("w13_recall_npc.png")
        assertEquals(0, chooseCount)
    }

    private fun textStyleOf(tag: String): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.first().layoutInput.style
    }

    private fun topOf(tag: String): Float =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().positionInRoot.y

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
