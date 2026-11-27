package com.example.lifegame.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.EventImageExposureStatus
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.components.BigResultStyle
import com.example.lifegame.ui.components.ChoicePickStyle
import com.example.lifegame.ui.components.ChoiceRevealStyle
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.HealthLevel
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.LocalTouchExploration
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.components.StatChangeStyle
import com.example.lifegame.ui.screens.CHANGE_HOLD_MILLIS
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.screens.PICK_EXIT_MS
import com.example.lifegame.ui.screens.PICK_RESULT_DELAY_MS
import com.example.lifegame.ui.screens.ResultImpactTier
import com.example.lifegame.ui.screens.ResultImpactTierKey
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * play 영역 연출(선택지 등장 · 고른 카드 · 능력치 변화 · 큰 결과)을 실제 DecisionScreen으로 확인한다.
 *
 * 가짜 진행(호스트)은 엔진처럼 움직인다: 카드를 누르면 같은 프레임에 결과를 내고, 기록 한 줄이 늘어 yearKey가 바뀐다.
 * - 움직임은 그리기 단계뿐이다: 등장 중에도 카드의 의미 경계 · 터치는 쉬는 자리이고, 입력 보호 · 콜백은 그대로다.
 * - 고른 카드가 물러나는 그림에는 의미 노드가 없다(선택지 노드 · 글자가 결과 화면에 남지 않는다).
 * - 결과 연출은 실시간 선택 뒤에만 PICK_RESULT_DELAY_MS 늦고, 복원 · 움직임 줄이기 · 터치 탐색에서는 늦추거나 다시 하지 않는다.
 * 시계를 멈추고(autoAdvance = false) 프레임 단위로 진행한다. 시스템 애니메이션 배율과 움직임 설정은 고정한다.
 */
@RunWith(AndroidJUnit4::class)
class PlayMotionTest {
    @get:Rule val compose = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before @After fun clearStoredStyle() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
    }

    private val decision = DecisionUi(
        eventId = "play_motion_test",
        kind = DecisionKind.MILESTONE,
        title = "졸업 후의 길이 갈라졌다",
        text = "졸업장을 받아 든 날, 단체 대화방이 조용해졌다.",
        imageId = "",
        imageFile = null,
        choices = listOf(
            DecisionChoiceUi("a", "A", "회사에 들어가 바로 일을 배운다", hint = "가능성 높음 · 경제력 · 사회성"),
            DecisionChoiceUi("b", "B", "대학원에 진학해 연구를 계속한다", hint = "지력"),
            DecisionChoiceUi("c", "C", "일단 반년 동안 여행을 떠난다", hint = "행복 · 운"),
        ),
    )

    private var current by mutableStateOf(choices(seen = true))
    private var style by mutableStateOf(MotionStyle.Default)
    private var round by mutableIntStateOf(0)
    private var picks = 0
    private var continues = 0
    private var resultChanges = listOf(StatChangeUi("건강", -25), StatChangeUi("지력", 5))
    private var resultFacts = emptyList<FactChangeUi>()

    private fun state(yearKey: String, health: Int, outcome: OutcomeUi?, seen: Boolean, presentation: PresentationState = PresentationState()) =
        PeriodPlayUiState(
            runKey = "play-motion",
            playerName = "재겸",
            phase = PlayPhase.DECISION,
            periodName = "청년기",
            chapterLabel = "제4장",
            periodYears = 5,
            periodIntro = "",
            yearInPeriod = 3,
            periodMarks = listOf("영아", "유년", "청소년", "청년", "중년", "노년").mapIndexed { i, label -> PeriodMarkUi(label, 0f, i == 3) },
            previousPeriodName = "청소년기",
            stats = Stats(health = health),
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
            presentation = if (seen) {
                presentation.copy(exposedEventId = decision.eventId, eventImageExposure = EventImageExposureStatus.SHOWN)
            } else {
                presentation
            },
        )

    /** The choices page: already seen (choices from the first frame) or fresh (the body types first). */
    private fun choices(seen: Boolean, health: Int = 72) = state("before-$round", health, outcome = null, seen = seen)

    /** What the engine does on a pick: the result in the same frame, new stats and a new yearKey. */
    private fun resultFor(choiceId: String): PeriodPlayUiState {
        val label = decision.choices.first { it.choiceId == choiceId }.label
        val health = (current.stats.health + (resultChanges.firstOrNull { it.name == "건강" }?.delta ?: 0)).coerceIn(0, 100)
        return state(
            yearKey = "after-$round",
            health = health,
            outcome = OutcomeUi(label, "결과 문장이 여기에 나온다.", resultChanges, endsLife = false, factChanges = resultFacts),
            seen = true,
            presentation = PresentationState().requestResultAnimation("result-$round"),
        )
    }

    private fun show(
        motion: Boolean = true,
        touchExploration: Boolean = false,
        restoration: ManualRestoreHost? = null,
    ) {
        compose.mainClock.autoAdvance = false
        val content: @Composable () -> Unit = {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides if (motion) 1f else 0f,
                    LocalTouchExploration provides touchExploration,
                    LocalMotionStyle provides style,
                ) {
                    Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                        key(round) {
                            DecisionScreen(
                                state = current,
                                onChoose = { id ->
                                    picks++
                                    current = resultFor(id)
                                },
                                onContinue = { continues++ },
                                skipImagePreview = true,
                            )
                        }
                    }
                }
            }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
        compose.mainClock.advanceTimeByFrame()
    }

    /** Starts over on a fresh choices page (a new DecisionScreen) with [with] applied. */
    private fun restart(with: MotionStyle, seen: Boolean = true) {
        compose.runOnIdle {
            style = with
            round++
            current = choices(seen)
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun count(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun bounds(tag: String): Rect =
        compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().let { Rect(it.positionInRoot, it.size.toSize()) }

    private fun assertSameRect(message: String, expected: Rect, actual: Rect) {
        listOf(expected.left to actual.left, expected.top to actual.top, expected.width to actual.width, expected.height to actual.height)
            .forEach { (e, a) -> assertEquals(message, e, a, 0.5f) }
    }

    /** Body typing → one tap completes it → the choices reveal live. */
    private fun revealChoices() {
        tap("decision_title")
        compose.onNodeWithTag("decision_choice_A").assertExists()
    }

    @Test
    fun aLivePickCallsBackOnceAndLeavesNoChoiceNodesDuringTheExit() {
        show()
        compose.onNodeWithTag("decision_choice_A").performTouchInput { repeat(3) { click(center) } }
        compose.mainClock.advanceTimeByFrame()
        assertEquals("onChoose fires once, synchronously, as before", 1, picks)
        // 결과 화면은 첫 프레임부터 있다(들어오는 연출은 투명도와 그리기 이동뿐). 물러나는 카드는 노드가 아니다.
        compose.onNodeWithTag("decision_outcome").assertExists()
        compose.onNodeWithTag("outcome_text").assertExists()
        compose.onNodeWithText("계속하기").assertExists()
        listOf("A", "B", "C").forEach { assertEquals("no choice node $it after the pick", 0, count("decision_choice_$it")) }
        listOf(decision.choices[1].label, decision.choices[2].label).forEach {
            compose.onAllNodesWithText(it, useUnmergedTree = true).assertCountEquals(0)
        }
        // 결과 칩은 고른 카드가 물러나는 동안 기다렸다가 PICK_RESULT_DELAY_MS에 뜬다.
        advance(PICK_RESULT_DELAY_MS / 2L)
        assertEquals(0, count("outcome_floating_changes"))
        advance(300)
        assertEquals(1, count("outcome_floating_changes"))
        // 오버레이에는 터치가 없다: 결과 글자를 눌러도 아무 일 없고, 계속하기는 그대로 한 번 눌린다.
        repeat(3) { tap("outcome_text") }
        advance(CHANGE_HOLD_MILLIS + 800)
        assertEquals(0, count("outcome_floating_changes"))
        compose.onNodeWithText("계속하기").performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, continues)
        assertEquals(1, picks)
    }

    @Test
    fun theContinueButtonIsClickableWhileTheResultPageEnters() {
        show()
        tap("decision_choice_B")
        advance(40)
        compose.onNodeWithText("계속하기").assertIsDisplayed().performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, continues)
    }

    @Test
    fun recreatingTheScreenDuringTheExitReplaysNothing() {
        val restoration = ManualRestoreHost(compose)
        show(restoration = restoration)
        tap("decision_choice_A")
        advance(PICK_RESULT_DELAY_MS + 60L)
        assertEquals(1, count("outcome_floating_changes"))
        restoration.recreate()
        assertEquals(1, restoration.recreations)
        advance(300)
        assertEquals("the chips do not replay after recreation", 0, count("outcome_floating_changes"))
        compose.onNodeWithTag("outcome_text").assertIsDisplayed()
        assertEquals(1, picks)
    }

    @Test
    fun everyRevealVariantMovesOnlyPixelsAndKeepsTheTapGuards() {
        show()
        for (reveal in ChoiceRevealStyle.entries) {
            restart(MotionStyle.Default.copy(choiceReveal = reveal), seen = false)
            advance(100)
            revealChoices()
            advance(40)
            val early = listOf("A", "B", "C").associateWith { bounds("decision_choice_$it") }
            val badge = bounds("choice_badge_A")
            // 본문이 막 끝난 순간의 터치는 여전히 선택이 아니다(300ms 보호).
            val before = picks
            tap("decision_choice_B")
            assertEquals("$reveal: a tap just as the choices appear does not choose", before, picks)
            advance(2_000)
            early.forEach { (key, rect) -> assertSameRect("$reveal: card $key never moved its bounds", rect, bounds("decision_choice_$key")) }
            assertSameRect("$reveal: the badge stays pinned to the card corner", badge, bounds("choice_badge_A"))
            assertEquals(early.getValue("A").left, badge.left, 0.6f)
            assertEquals(early.getValue("A").top, badge.top, 0.6f)
            tap("decision_choice_B")
            assertEquals("$reveal: after the guard the tap chooses once", before + 1, picks)
        }
    }

    @Test
    fun everyPickVariantChoosesOnceAndSettlesOnTheResult() {
        show()
        for (pick in ChoicePickStyle.entries) {
            restart(MotionStyle.Default.copy(choicePick = pick))
            val before = picks
            compose.onNodeWithTag("decision_choice_C").performTouchInput { repeat(2) { click(center) } }
            compose.mainClock.advanceTimeByFrame()
            assertEquals("$pick: one pick", before + 1, picks)
            listOf("A", "B", "C").forEach { assertEquals("$pick: no choice node $it", 0, count("decision_choice_$it")) }
            advance(PICK_EXIT_MS + 200L)
            compose.onNodeWithTag("decision_outcome").assertIsDisplayed()
            compose.onNodeWithText("계속하기").assertIsDisplayed()
        }
    }

    @Test
    fun afterALivePickTheResultEffectsWaitForTheExit() {
        show()
        tap("decision_choice_A")
        advance(PICK_RESULT_DELAY_MS / 2L)
        assertEquals("the chips wait while the picked card leaves", 0, count("outcome_floating_changes"))
        advance(PICK_RESULT_DELAY_MS.toLong())
        assertEquals(1, count("outcome_floating_changes"))
    }

    @Test
    fun underTouchExplorationThePickRestsAndTheResultIsNotDelayed() {
        // 터치 탐색에서는 고른 카드 연출(결과 화면이 투명하게 시작하는 들어오기)이 쉬므로 결과 칩도 늦추지 않는다.
        show(touchExploration = true)
        tap("decision_choice_A")
        advance(16)
        assertEquals(1, count("outcome_floating_changes"))
        compose.onNodeWithTag("outcome_text").assertIsDisplayed()
    }

    @Test
    fun choosingStyleNoneKeepsTodaysInstantSwap() {
        style = MotionStyle.Default.copy(choicePick = ChoicePickStyle.NONE)
        show()
        tap("decision_choice_A")
        advance(16)
        assertEquals("NONE starts the result effects at once", 1, count("outcome_floating_changes"))
    }

    @Test
    fun aResultShownWithoutAPickStartsAtOnce() {
        show()
        // 앱을 다시 열어 PENDING 결과로 돌아온 경우와 같다: 고른 순간이 없으니 늦추지 않는다.
        compose.runOnIdle { round++; current = resultFor("a") }
        advance(16)
        assertEquals(1, count("outcome_floating_changes"))
        advance(CHANGE_HOLD_MILLIS + 800)
        assertEquals(0, count("outcome_floating_changes"))
    }

    @Test
    fun theResultTierIsExposedOnceOnTheOutcomeCard() {
        resultChanges = listOf(StatChangeUi("행복", 5))
        resultFacts = listOf(FactChangeUi("업적", "첫 개인전"))
        show()
        tap("decision_choice_C")
        compose.onNodeWithTag("decision_outcome").assert(SemanticsMatcher.expectValue(ResultImpactTierKey, ResultImpactTier.BIG))
        resultChanges = listOf(StatChangeUi("행복", 3))
        resultFacts = emptyList()
        restart(MotionStyle.Default)
        tap("decision_choice_A")
        compose.onNodeWithTag("decision_outcome").assert(SemanticsMatcher.expectValue(ResultImpactTierKey, ResultImpactTier.CALM))
    }

    @Test
    fun reducedMotionRestsEveryVariantAndShowsStaticChanges() {
        style = MotionStyle(
            choiceReveal = ChoiceRevealStyle.DEAL,
            choicePick = ChoicePickStyle.STAMP,
            statChange = StatChangeStyle.FLOAT,
            bigResult = BigResultStyle.GLOW,
        )
        current = choices(seen = false)
        show(motion = false)
        advance(100)
        // 움직임 줄이기: 본문은 처음부터 전문이고 선택지는 바로 나온다.
        compose.onNodeWithTag("decision_choice_A").assertExists()
        advance(TAP_GUARD_MILLIS + 50L)
        tap("decision_choice_A")
        assertEquals(1, picks)
        compose.onNodeWithTag("outcome_stat_changes", useUnmergedTree = true).assertExists()
        assertEquals(0, count("outcome_floating_changes"))
        compose.onNodeWithText("계속하기").assertIsDisplayed()
    }

    @Test
    fun theGaugeHoldsStillOnItsFirstFrameAndSettlesOnTheNewLevelColour() {
        style = MotionStyle.Default.copy(statChange = StatChangeStyle.FLOAT)
        show()
        // 처음 그려질 때는 움직이지 않는다: 첫 프레임부터 채움 끝까지 지금 구간의 색이다.
        assertFillEndsIn(HealthLevel.GOOD.color)
        tap("decision_choice_A")
        advance(1_500)
        assertFillEndsIn(HealthLevel.CAUTION.color)
        // 떠오르는 숫자는 그림이라 글자 노드가 없다(숫자 없는 게이지 규칙).
        listOf("−25", "+5", "47", "72").forEach { compose.onAllNodesWithText(it, useUnmergedTree = true).assertCountEquals(0) }
    }

    private fun assertFillEndsIn(expected: Color) {
        val fill = compose.onNodeWithTag("health_gauge_fill", useUnmergedTree = true).captureToImage().asAndroidBitmap()
        val y = fill.height / 2
        listOf(fill.width / 2, fill.width - fill.height - 1).forEach { x ->
            val actual = fill.getPixel(x, y)
            val e = expected.toArgb()
            val diff = listOf(16, 8, 0).maxOf { shift -> abs(((e shr shift) and 0xFF) - ((actual shr shift) and 0xFF)) }
            assertTrue("fill pixel at $x: ${Integer.toHexString(actual)} vs ${Integer.toHexString(e)}", diff <= 24)
        }
    }

    @Test
    fun comparisonSectionsPreviewOneScreenAndApplyEveryPlayEffect() {
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 0f,
                ) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) {
                        ProvideMotionStyle(allowOverride = true) { MotionCompareScreen(onBack = {}) }
                    }
                }
            }
        }
        compose.onNodeWithTag("motion_group_play").performScrollTo().performClick()
        // 처음에는 선택지 등장 미리보기만 실제 화면을 띄운다.
        compose.onAllNodesWithTag("choice_reveal_preview").assertCountEquals(1)
        compose.onAllNodesWithTag("choice_pick_preview").assertCountEquals(0)
        compose.onNodeWithTag("choice_pick_STAMP").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onAllNodesWithTag("choice_pick_preview").assertCountEquals(1)
        compose.onAllNodesWithTag("choice_reveal_preview").assertCountEquals(0)
        compose.onAllNodes(hasTestTag("decision_choice_A") and hasAnyAncestor(hasTestTag("choice_pick_preview")))
            .assertCountEquals(1)
        compose.onNodeWithTag("big_result_tier_FINAL").performScrollTo().assertExists()
        compose.onNodeWithTag("stat_change_gain").performScrollTo().assertExists()

        val picked = mapOf(
            "choice_reveal" to ChoiceRevealStyle.INK.name,
            "choice_pick" to ChoicePickStyle.STAMP.name,
            "stat_change" to StatChangeStyle.FLOAT.name,
            "big_result" to BigResultStyle.GLOW.name,
        )
        picked.forEach { (key, variant) ->
            compose.onNodeWithTag("${key}_$variant").performScrollTo().performClick()
            compose.onNodeWithTag("${key}_apply").performScrollTo().performClick()
            compose.waitForIdle()
        }
        assertEquals(
            MotionStyle.Default.copy(
                choiceReveal = ChoiceRevealStyle.INK,
                choicePick = ChoicePickStyle.STAMP,
                statChange = StatChangeStyle.FLOAT,
                bigResult = BigResultStyle.GLOW,
            ),
            MotionStyleStore(context).read(),
        )
    }
}
