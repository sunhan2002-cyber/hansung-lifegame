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
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.ui.screens.AutoProgressScreen
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.RunEndScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 8주차: 인생 경로 · 잠금 이유 · 결과 사실 · 결말 요약이 작은 화면 + 큰 글씨에서도 잘리지 않는지 확인한다.
 * 일곱 스탯 결말은 실제 assets와 엔진으로 같은 시드 · 같은 시작 조건의 판을 끝까지 돌려 만든다.
 * 화면 캡처는 앱 외부 파일 폴더의 week8-frontend에 남긴다 (저장소에는 올리지 않는다).
 */
@RunWith(AndroidJUnit4::class)
class Week8LifePathScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun asset(name: String) = context.assets.open(name).bufferedReader().use { it.readText() }

    private val periods = V4ContentParser.parsePeriods(asset("periods.sample.json"))
    private val focuses = V4ContentParser.parseFocuses(asset("focuses.sample.json"))
    private val decisions = V4ContentParser.parseDecisionEvents(asset("events.sample.json"))
    private val catalog = LifeFactCatalog(parseLifeFacts(asset("life_facts.sample.json")))
    private val content = PlayContent(
        periods = periods,
        focuses = focuses,
        decisions = decisions.associateBy { it.eventId },
        imageFiles = emptyMap(),
        factCatalog = catalog,
        lookup = FlagLookup.from(decisions, catalog),
    )

    @Test
    fun sevenStatEndingsAreDistinctAndFitOnASmallScreen() {
        val states = LifePathKind.entries.associateWith { simulate(it) }
        var current by mutableStateOf(states.getValue(LifePathKind.HEALTH))
        setSmallScreen { RunEndScreen(state = current, onDone = {}) }

        val lifeLines = mutableSetOf<String>()
        states.forEach { (path, state) ->
            current = state
            composeRule.waitForIdle()
            val ending = state.ending!!
            lifeLines += ending.lifeLine
            assertTextFits(ending.headline)
            assertTextFits(ending.lifeLine, multiline = true)
            saveScreenshot("end_${path.name.lowercase()}_1_top.png")

            scrollTo("교육 · 직업")
            ending.educationCareer.forEach { assertTextFits(it.value) }
            scrollTo("가족 · 관계 · 주거 · 경제력")
            ending.familyLife.forEach { assertTextFits(it.value) }
            saveScreenshot("end_${path.name.lowercase()}_2_facts.png")

            scrollTo("주요 업적")
            ending.achievements.forEach { assertTextFits("· $it") }
            scrollTo("주요 선택")
            saveScreenshot("end_${path.name.lowercase()}_3_choices.png")
            composeRule.onNodeWithTag("run_end_content").performScrollToNode(hasText("인생 요약"))
        }
        assertEquals("일곱 경로의 한 줄 요약이 모두 달라야 한다", 7, lifeLines.size)
    }

    @Test
    fun earlyHealthDeathShowsTheRealPeriod() {
        val base = simulate(LifePathKind.INTELLIGENCE)
        val teenDeath = run {
            val session = newSession(LifePathKind.INTELLIGENCE)
            var step = session.start()
            while (step.state.period.ordinal < com.example.lifegame.domain.model.LifePeriod.TEEN.ordinal ||
                step.phase == TurnPhase.CHAPTER
            ) {
                step = advance(session, step, LifePathKind.INTELLIGENCE)
            }
            // 청소년기에 건강이 0이 된 상태를 그대로 결말로 만든다.
            val dead = step.state.copy(
                stats = step.state.stats.copy(health = 0),
                isEnding = true,
                isDead = true,
                endingPeriod = step.state.period,
                moments = step.state.moments + com.example.lifegame.domain.model.LifeMoment(
                    step.state.period, step.state.yearInPeriod, "시험 기간 밤샘 끝에 쓰러졌다.",
                ),
            )
            buildPeriodPlayState(step.copy(state = dead, phase = TurnPhase.ENDING, pendingDecision = null), content, "재겸")
        }
        setSmallScreen { RunEndScreen(state = teenDeath, onDone = {}) }
        composeRule.onNodeWithText(teenDeath.ending!!.headline).assertIsDisplayed()
        assertTrue(teenDeath.ending!!.headline.startsWith("청소년기"))
        composeRule.onNodeWithTag("run_end_content").performScrollToNode(hasText("건강이 바닥남"))
        composeRule.onNodeWithText("건강이 바닥남").assertIsDisplayed()
        saveScreenshot("end_teen_health_death.png")
        assertFalse(base.ending!!.headline == teenDeath.ending!!.headline)
    }

    @Test
    fun lockedChoiceShowsRealReasonsAndOutcomeShowsNewFacts() {
        val decision = DecisionUi(
            eventId = "path_int_07",
            kind = DecisionKind.MILESTONE,
            title = "졸업 후의 길",
            text = "연구실과 회사에서 동시에 연락이 왔다.",
            imageId = "",
            imageFile = null,
            choices = listOf(
                DecisionChoiceUi("A", "A", "회사에 들어간다", hint = "경제력 · 사회성"),
                DecisionChoiceUi(
                    "B", "B", "대학원에 진학해 연구를 계속한다", hint = "지력", available = false,
                    lockReasons = listOf("지력 82 필요 (지금 64)", "대학 진학 필요", "「고등학교 원서」 이후 가능"),
                ),
            ),
        )
        val facts = LifeFactsUi(education = "대학 진학", career = "연구원", careerLevel = "책임자", marital = "결혼",
            children = 2, housing = "내 집(아파트)", wealthBand = "여유", achievements = listOf("연구 돌파구", "마지막 강연"))
        var state by mutableStateOf(playState(decision = decision, facts = facts))
        setSmallScreen {
            DecisionScreen(state = state, onChoose = {}, onContinue = {})
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("decision_content").performScrollToNode(hasText("지력 82 필요 (지금 64)"))
        listOf("지력 82 필요 (지금 64)", "대학 진학 필요", "「고등학교 원서」 이후 가능").forEach { assertTextFits(it) }
        saveScreenshot("decision_locked_reasons.png")

        state = playState(
            decision = decision,
            facts = facts,
            outcome = OutcomeUi(
                choiceLabel = "회사에 들어간다",
                text = "첫 출근 날, 사원증 사진이 생각보다 잘 나왔다.",
                changes = listOf(StatChangeUi("경제력", 6), StatChangeUi("행복", -2)),
                endsLife = false,
                factChanges = listOf(FactChangeUi("직업", "회사원"), FactChangeUi("경제력", "보통 → 여유")),
                openedEvents = listOf("첫 승진 심사"),
            ),
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("decision_content").performScrollToNode(hasText("새로 생긴 사실"))
        assertTextFits("회사원")
        assertTextFits("보통 → 여유")
        composeRule.onNodeWithTag("decision_content").performScrollToNode(hasText("새로 열린 길: 「첫 승진 심사」"))
        assertTextFits("새로 열린 길: 「첫 승진 심사」")
        saveScreenshot("decision_outcome_facts.png")
    }

    @Test
    fun playingSummaryShowsFocusTopStatAndFacts() {
        val facts = LifeFactsUi(education = "대학원 진학", career = "연구원", careerLevel = "책임자", marital = "결혼",
            children = 1, housing = "전세", wealthBand = "여유", achievements = listOf("연구 돌파구"))
        val state = playState(decision = null, facts = facts).copy(phase = PlayPhase.AUTO, autoPlaying = false)
        setSmallScreen {
            AutoProgressScreen(state = state, onAdvanceYear = {}, onTogglePlaying = {}, onSpeedChange = {}, onSkipToDecision = {})
        }
        assertTextFits(facts.shortLine, multiline = true)
        assertTextFits("이번 시기 초점 커리어  ·  대표 지력 88")
        saveScreenshot("auto_life_summary.png")
        composeRule.onNodeWithTag("life_path_summary").performClick()
        composeRule.onNodeWithText("지금까지의 나").assertIsDisplayed()
    }

    // ---- helpers ----

    private fun playState(decision: DecisionUi?, facts: LifeFactsUi, outcome: OutcomeUi? = null) = PeriodPlayUiState(
        runKey = "test",
        playerName = "재겸",
        phase = PlayPhase.DECISION,
        periodName = "청년기",
        chapterLabel = "제4장",
        periodYears = 5,
        periodIntro = "",
        yearInPeriod = 5,
        periodMarks = periods.map { PeriodMarkUi(it.shortName(), 0f, it.displayName == "청년기") },
        previousPeriodName = "청소년기",
        stats = Stats(intelligence = 88, wealth = 60),
        focus = FocusOptionUi("f", "커리어", "일에 집중한다", emptyList()),
        focusOptions = emptyList(),
        sceneImageId = "",
        sceneFile = null,
        log = listOf(LogLineUi("0", "청년기 · 4번째 해", "연구실 불이 늦게까지 켜져 있었다.")),
        yearKey = "k",
        decision = decision,
        outcome = outcome,
        isDead = false,
        autoPlaying = false,
        speed = 1,
        facts = facts,
        topStat = StatValueUi("지력", 88),
    )

    private fun newSession(target: LifePathKind) = PeriodGameSession(
        periods = periods,
        focuses = focuses,
        decisions = decisions,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "path-${target.name}" },
        runSeedFactory = { 20261020L },
    )

    private fun advance(session: PeriodGameSession, step: com.example.lifegame.domain.model.TurnStep, target: LifePathKind) =
        when (step.phase) {
            TurnPhase.CHAPTER -> {
                val options = focuses.filter { it.period == step.state.period }
                val pick = options.maxByOrNull { it.statEffects.toDisplayMap()[target.stat] ?: 0 } ?: options.first()
                session.selectFocus(pick.focusId)
            }
            TurnPhase.AUTO_PROGRESS -> session.advance()
            TurnPhase.DECISION -> session.choose(pickFor(step, target).choiceId).step
            TurnPhase.ENDING -> step
        }

    /** 그 스탯을 가장 많이 올리는(사망 결과는 피하는) 고를 수 있는 선택지. */
    private fun pickFor(step: com.example.lifegame.domain.model.TurnStep, target: LifePathKind) =
        requireNotNull(step.pendingDecision).let { pending ->
            val event = content.decisions.getValue(pending.eventId)
            val available = pending.choices.filter { it.available }
            available.maxByOrNull { preview ->
                val option = event.choices.first { it.choiceId == preview.choiceId }
                option.outcomes.firstOrNull { !it.hidden }?.let { o ->
                    (o.statDelta.toDisplayMap()[target.stat] ?: 0) * 10 + (if (o.death) -1000 else 0)
                } ?: 0
            } ?: available.first()
        }

    private fun simulate(target: LifePathKind): PeriodPlayUiState {
        val session = newSession(target)
        var step = session.start()
        val records = mutableListOf<ChoiceRecordUi>()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 600) {
            if (step.phase == TurnPhase.DECISION) {
                val pending = requireNotNull(step.pendingDecision)
                val period = periods.first { it.period == step.state.period }.displayName
                records += ChoiceRecordUi(period, step.state.yearInPeriod, pending.title, pickFor(step, target).label)
                step = advance(session, step, target)
            } else {
                step = advance(session, step, target)
            }
        }
        return buildPeriodPlayState(step, content, playerName = "재겸", choiceLog = records)
    }

    private fun setSmallScreen(content: @Composable () -> Unit) {
        composeRule.setContent {
            val deviceDensity = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(deviceDensity, fontScale = 1.5f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(320.dp, 640.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNodeWithTag("run_end_content").performScrollToNode(hasText(text))
        composeRule.onNodeWithText(text).assertIsDisplayed()
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
        val directory = requireNotNull(context.getExternalFilesDir("week8-frontend"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, fileName).outputStream().use { stream ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
    }
}
