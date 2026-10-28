package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.components.HealthLevel
import com.example.lifegame.ui.components.HealthLevelKey
import com.example.lifegame.ui.screens.AutoProgressScreen
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 10주차 최종 플레이 화면: 상단 투명 캐릭터 · 숫자 없는 두꺼운 색상 건강바, 초점 박스 없음,
 * 하단 -O-O-O- 시기선, 버튼이 아닌 사건 분류, 모서리에 붙은 ABC, 장면 보기 복귀.
 * 약 360dp 폭 · 글꼴 1.3배에서 본문 · 선택지 3개 · 시기 바가 잘리지 않는지 함께 본다.
 * 화면 캡처는 앱 외부 파일 폴더의 week10-frontend에 남긴다(저장소에는 올리지 않는다).
 */
@RunWith(AndroidJUnit4::class)
class Week10PlayScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val periods = V4ContentParser.parsePeriods(
        context.assets.open("periods.sample.json").bufferedReader().use { it.readText() },
    )

    private val decision = DecisionUi(
        eventId = "path_int_07",
        kind = DecisionKind.MILESTONE,
        title = "졸업 후의 길이 갈라졌다",
        text = "연구실과 회사에서 같은 날 연락이 왔다. 둘 다 오늘 안에 답을 달라고 한다. 휴대폰 화면을 끄고 한참을 창밖만 봤다.",
        imageId = "",
        imageFile = null,
        choices = listOf(
            DecisionChoiceUi("a", "A", "회사에 들어가 바로 일을 배운다", hint = "가능성 높음 · 경제력 · 사회성", healthEffectText = "건강 -3 감소"),
            DecisionChoiceUi("b", "B", "대학원에 진학해 연구를 계속한다", hint = "지력", available = false,
                lockReasons = listOf("지력 82 필요 (지금 64)", "대학 진학 필요")),
            DecisionChoiceUi("c", "C", "일단 반년 동안 여행을 떠난다", hint = "행복 · 운"),
        ),
    )

    private fun playState(
        health: Int = 67,
        outcome: OutcomeUi? = null,
        phase: PlayPhase = PlayPhase.DECISION,
        decision: DecisionUi? = this.decision,
    ) = PeriodPlayUiState(
        runKey = "w10",
        playerName = "재겸",
        phase = phase,
        periodName = "청년기",
        chapterLabel = "제4장",
        periodYears = 5,
        periodIntro = "",
        yearInPeriod = 3,
        periodMarks = periods.sortedBy { it.order }.mapIndexed { index, item ->
            PeriodMarkUi(item.shortName(), if (index < 3) 1f else 0f, item.displayName == "청년기")
        },
        previousPeriodName = "청소년기",
        stats = Stats(health = health, intelligence = 64, wealth = 55),
        focus = FocusOptionUi("f", "커리어", "일에 집중한다", listOf(StatChangeUi("경제력", 2))),
        focusOptions = emptyList(),
        sceneImageId = "",
        sceneFile = null,
        log = listOf(LogLineUi("0", "청년기 · 3번째 해", "출근길 버스에서 졸았다.")),
        yearKey = "k",
        decision = decision,
        outcome = outcome,
        isDead = false,
        autoPlaying = false,
        speed = 1,
        facts = LifeFactsUi(education = "대학 진학", career = "회사원"),
        avatar = AvatarLook("skin_tan", "hair_bob_brown", "eyes_big", LifePeriod.YOUNG_ADULT),
    )

    @Test
    fun newBrightBackgroundKeepsChoiceTextReadable() {
        val art = com.example.lifegame.ui.images.ImageCandidate(
            "img_week10_bg_child", "week10/scenes/img_week10_bg_child.png", "scene")
        setScreen { DecisionScreen(state = playState(decision = decision.copy(images = listOf(art))), onChoose = {}, onContinue = {}) }
        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.waitForIdle()
        saveScreenshot("w10_bright_background.png")
        assertTextFits(decision.title)
        assertTextFits(decision.text, multiline = true)
    }

    @Test
    fun decisionScreenFollowsTheFinalLayoutAt360dpAndLargeText() {
        setScreen { DecisionScreen(state = playState(), onChoose = {}, onContinue = {}) }

        // 상단: 캐릭터와 건강바만. 숫자 · 초점 요약 박스 · 장식 문구는 없다.
        composeRule.onNodeWithTag("play_top_bar").assertIsDisplayed()
        composeRule.onNodeWithTag("player_avatar_button").assert(hasClickAction())
        composeRule.onAllNodesWithText("/100", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("/ 100", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithTag("life_path_summary").assertCountEquals(0)
        composeRule.onAllNodesWithText("이번 시기 초점", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("어떻게 할까").assertCountEquals(0)
        composeRule.onAllNodesWithText("결과 가능성").assertCountEquals(0)

        // 사건 분류는 누를 수 없는 글자 효과다.
        composeRule.onNodeWithTag("decision_kind_mark").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))

        // 하단 시기선은 화면 아래에 붙어 있고 지금 시기를 읽어 준다.
        val screen = composeRule.onNodeWithTag("test_screen").fetchSemanticsNode().boundsInRoot
        val track = composeRule.onNodeWithTag("period_track").fetchSemanticsNode()
        val content = composeRule.onNodeWithTag("decision_content").fetchSemanticsNode().boundsInRoot
        val navInset = with(composeRule.density) { 64.dp.toPx() }
        assertTrue("시기선은 사건 내용 아래", track.boundsInRoot.top >= content.bottom - 1f)
        // 시기선 아래에는 시스템 내비게이션 여백만 남는다.
        assertTrue("시기선은 화면 맨 아래", screen.bottom - track.boundsInRoot.bottom in -1f..navInset)
        val description = track.config.getOrNull(SemanticsProperties.ContentDescription)?.joinToString().orEmpty()
        assertTrue(description, description.contains("지금 시기 청년") && description.contains("3번째 해"))

        // 본문과 세 선택지가 잘리지 않는다(길면 스크롤). 글자가 다 나올 때까지 시계를 앞당긴다.
        composeRule.mainClock.advanceTimeBy(10_000)
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText(decision.text, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        assertTextFits(decision.title)
        assertTextFits(decision.text, multiline = true)
        decision.choices.forEach { choice ->
            composeRule.onNodeWithTag("decision_content").performScrollToNode(hasText(choice.label))
            assertTextFits(choice.label)
            val card = composeRule.onNodeWithTag("decision_choice_${choice.key}").fetchSemanticsNode().boundsInRoot
            val badge = composeRule.onNodeWithTag("choice_badge_${choice.key}", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            // ABC 표식은 카드 왼쪽 위 모서리에 딱 붙는다.
            assertEquals(card.left, badge.left, 0.6f)
            assertEquals(card.top, badge.top, 0.6f)
            assertTrue("터치 영역 48dp 이상", card.height >= with(composeRule.density) { 48.dp.toPx() })
        }
        listOf("지력 82 필요 (지금 64)", "대학 진학 필요").forEach { assertTextFits(it) }
        composeRule.onNodeWithTag("decision_choice_B").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "잠김"),
        )
        saveScreenshot("w10_decision_360dp.png")
    }

    @Test
    fun healthGaugeColorFollowsTheBoundariesWithoutNumbers() {
        var health by mutableStateOf(100)
        setScreen { DecisionScreen(state = playState(health = health), onChoose = {}, onContinue = {}) }
        val expected = listOf(
            100 to HealthLevel.GOOD,
            80 to HealthLevel.GOOD,
            79 to HealthLevel.CAUTION,
            60 to HealthLevel.CAUTION,
            59 to HealthLevel.DANGER,
            5 to HealthLevel.DANGER,
        )
        expected.forEach { (value, level) ->
            health = value
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("health_gauge").assert(SemanticsMatcher.expectValue(HealthLevelKey, level))
            composeRule.onNodeWithTag("health_gauge").assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "건강 ${level.label}"),
            )
            // 실제로 칠해진 색도 구간 색이다.
            val fill = composeRule.onNodeWithTag("health_gauge_fill", useUnmergedTree = true).captureToImage().asAndroidBitmap()
            val pixel = fill.getPixel(fill.width / 2, fill.height / 2)
            assertColorNear(level.color, pixel, "health=$value")
            composeRule.onAllNodesWithText("$value", substring = false).assertCountEquals(0)
            if (value == 79 || value == 59) saveScreenshot("w10_health_$value.png")
        }
    }

    @Test
    fun sceneViewHidesEveryUiAndOneTapRestoresTheSameOutcome() {
        val outcome = OutcomeUi(
            choiceLabel = "회사에 들어가 바로 일을 배운다",
            text = "첫 출근 날, 사원증 사진이 생각보다 잘 나왔다.",
            changes = listOf(StatChangeUi("경제력", 6), StatChangeUi("건강", -3)),
            endsLife = false,
        )
        val art = com.example.lifegame.ui.images.ImageCandidate(
            "img_week10_bg_young_adult", "week10/scenes/img_week10_bg_young_adult.png", "scene")
        setScreen { DecisionScreen(state = playState(outcome = outcome, decision = decision.copy(images = listOf(art))), onChoose = {}, onContinue = {}) }
        composeRule.onNodeWithText(outcome.text).assertIsDisplayed()
        assertTextFits(outcome.text, multiline = true)
        saveScreenshot("w10_outcome_360dp.png")

        composeRule.onNodeWithTag("scene_view_button").performClick()
        composeRule.onNodeWithTag("scene_only").assertIsDisplayed()
        composeRule.onNodeWithTag("scene_clear").assertExists()
        saveScreenshot("w10_scene_only.png")
        composeRule.onAllNodesWithTag("play_top_bar").assertCountEquals(0)
        composeRule.onAllNodesWithTag("decision_outcome").assertCountEquals(0)
        composeRule.onAllNodesWithTag("period_track").assertCountEquals(0)

        composeRule.onNodeWithTag("scene_only").performClick()
        composeRule.onNodeWithTag("scene_blurred").assertExists()
        composeRule.onNodeWithText(outcome.text).assertIsDisplayed()
        composeRule.onNodeWithTag("play_top_bar").assertIsDisplayed()
    }

    @Test
    fun focusAndFactsAreInTheDetailsWindowNotOnThePlayScreen() {
        setScreen {
            AutoProgressScreen(
                state = playState(phase = PlayPhase.AUTO, decision = null),
                onAdvanceYear = {},
                onTogglePlaying = {},
                onSpeedChange = {},
                onSkipToDecision = {},
            )
        }
        composeRule.onAllNodesWithTag("life_path_summary").assertCountEquals(0)
        composeRule.onNodeWithTag("character_stage").assertIsDisplayed()
        composeRule.onNodeWithTag("period_track").assertIsDisplayed()
        saveScreenshot("w10_auto_360dp.png")
        // 큰 글꼴에서도 "다음 결정까지"는 한 줄로 남는다(자리가 없으면 아랫줄로 내려간다).
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag("skip_to_decision", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        val skip = layouts.single()
        assertEquals(1, skip.lineCount)
        assertTrue(
            "skip text width ${skip.getLineRight(0)} > box ${skip.size.width}",
            skip.getLineRight(0) <= skip.size.width + 1f,
        )
        composeRule.onNodeWithTag("player_avatar_button").performClick()
        composeRule.onNodeWithText("지금까지의 나").assertIsDisplayed()
        composeRule.onNodeWithTag("details_focus").assertExists()
        composeRule.onNodeWithText("커리어").assertExists()
    }

    // ---- helpers ----

    private fun setScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 1.3f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds().testTag("test_screen")) { content() }
                }
            }
        }
    }

    private fun assertColorNear(expected: Color, actual: Int, message: String) {
        val e = expected.toArgb()
        val channels = listOf(16, 8, 0).map { shift -> abs(((e shr shift) and 0xFF) - ((actual shr shift) and 0xFF)) }
        assertTrue("$message: expected ${Integer.toHexString(e)} but was ${Integer.toHexString(actual)}", channels.all { it <= 12 })
    }

    private fun assertTextFits(text: String, multiline: Boolean = false) {
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action -> action(layouts) }
        assertTrue("Expected a text layout for: $text", layouts.isNotEmpty())
        layouts.forEach { layout ->
            assertFalse("Text height must fit: $text", layout.didOverflowHeight)
            repeat(layout.lineCount) { line ->
                assertFalse("Text must not be ellipsized: $text", layout.isLineEllipsized(line))
                val lineWidth = layout.getLineRight(line) - layout.getLineLeft(line)
                assertTrue("Text line must fit: $text", lineWidth <= layout.size.width + 1f)
            }
            assertEquals("All characters must be laid out: $text", text.length,
                layout.getLineEnd(layout.lineCount - 1, visibleEnd = false))
            if (multiline) assertTrue("Long text should wrap: $text", layout.lineCount > 1)
        }
    }

    private fun saveScreenshot(fileName: String) {
        composeRule.waitForIdle()
        val bitmap = composeRule.onRoot().captureToImage().asAndroidBitmap()
        val directory = requireNotNull(context.getExternalFilesDir("week10-frontend"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
