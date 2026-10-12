package com.example.lifegame.ui

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionChoicePreview
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PendingDecision
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodPlayUiTest {

    private val periods = listOf(
        PeriodDefinition(LifePeriod.INFANT, order = 0, years = 4, displayName = "영아기", chapterTitle = "제1장 영아기"),
        PeriodDefinition(
            LifePeriod.TEEN,
            order = 1,
            years = 5,
            displayName = "청소년기",
            chapterTitle = "제3장 청소년기",
            startLabel = "중학교 입학부터 고등학교 졸업까지",
        ),
    )
    private val study = Focus(
        focusId = "teen_study",
        period = LifePeriod.TEEN,
        label = "학업",
        statEffects = StatDelta(intelligence = 4, happiness = -1),
        description = "학원 · 독서실",
        sceneImageId = "img_scene_teen_study_001",
    )
    private val sick = DecisionOption(
        choiceId = "C",
        label = "이불 속에서 버틴다",
        outcomes = listOf(
            Outcome("recover", "이틀 앓고 나았다.", StatDelta(health = 1), chance = 60, chanceWeights = ChanceWeights(health = 5)),
            Outcome("fatal", "끝까지 버티다 생을 마쳤다.", chance = 3, hidden = true, death = true),
            Outcome("worse", "버틸수록 열이 올랐다.", StatDelta(health = -10), chance = 37),
        ),
    )
    private val hospital = DecisionOption(
        choiceId = "A",
        label = "병원에 간다",
        outcomes = listOf(Outcome("a_primary", "주사 한 대에 나았다.", StatDelta(health = 4, happiness = -1), chance = 100)),
    )
    private val fever = DecisionEvent(
        eventId = "teen_fever",
        periods = setOf(LifePeriod.TEEN),
        kind = DecisionKind.INCIDENT,
        title = "{nameCall}, 열이 끓는다",
        text = "{name}의 하루가 빙글 돈다.",
        choices = listOf(hospital, sick),
        povImageId = "img_pov_fever_001",
    )

    @Test
    fun `likelihood words follow the v4 thresholds`() {
        assertEquals(Likelihood.LOW, likelihoodOf(0))
        assertEquals(Likelihood.LOW, likelihoodOf(34))
        assertEquals(Likelihood.HALF, likelihoodOf(35))
        assertEquals(Likelihood.HALF, likelihoodOf(59))
        assertEquals(Likelihood.HIGH, likelihoodOf(60))
        assertEquals(Likelihood.HIGH, likelihoodOf(79))
        assertEquals(Likelihood.VERY_HIGH, likelihoodOf(80))
        assertEquals(Likelihood.VERY_HIGH, likelihoodOf(100))
    }

    @Test
    fun `primary chance uses stat weights like the engine and counts hidden outcomes`() {
        // 건강 50: 60 / (60 + 3 + 37) = 60%
        assertEquals(60, sick.primaryChancePercent(Stats()))
        // 건강 90: 60 + (40 * 5) / 10 = 80 → 80 / 120 = 67%
        assertEquals(67, sick.primaryChancePercent(Stats(health = 90)))
        // 건강 10: 60 - 20 = 40 → 40 / 80 = 50%
        assertEquals(50, sick.primaryChancePercent(Stats(health = 10)))
    }

    @Test
    fun `probability hint shows words and key stat but never the hidden outcome`() {
        val hint = sick.hint(Stats(health = 10))
        assertEquals("가능성 반반쯤 · 건강", hint)
        assertFalse(hint.contains("%"))
        assertFalse(hint.contains("생을 마쳤다"))
    }

    @Test
    fun `single outcome hint lists only the affected stats`() {
        assertEquals("건강 · 행복", hospital.hint(Stats()))
    }

    @Test
    fun `decision ui keeps preview order and does not carry outcome text`() {
        val pending = PendingDecision(
            eventId = fever.eventId,
            kind = fever.kind,
            title = fever.title,
            text = fever.text,
            choices = fever.choices.map { DecisionChoicePreview(it.choiceId, it.label) },
            povImageId = fever.povImageId,
        )
        val ui = buildDecisionUi(pending, fever, Stats(), mapOf("img_pov_fever_001" to "pov/fever.png"))

        assertEquals(listOf("A", "B"), ui.choices.map { it.key })
        assertEquals(listOf("A", "C"), ui.choices.map { it.choiceId })
        assertEquals("pov/fever.png", ui.imageFile)
        assertTrue(ui.isIncident)
        val everything = ui.toString()
        fever.choices.flatMap { it.outcomes }.forEach { outcome ->
            assertFalse("결과 문장이 미리 보이면 안 된다: ${outcome.resultText}", everything.contains(outcome.resultText))
        }
    }

    @Test
    fun `chapter step becomes the focus picking screen of that period`() {
        val step = TurnStep(
            state = PeriodRunState(runId = "run", runSeed = 1L, period = LifePeriod.TEEN),
            phase = TurnPhase.CHAPTER,
            needsFocusSelection = true,
        )
        val ui = buildPeriodPlayState(step, content(), playerName = "재겸")

        assertEquals(PlayPhase.CHAPTER, ui.phase)
        assertEquals("청소년기", ui.periodName)
        assertEquals("제3장", ui.chapterLabel)
        assertEquals(5, ui.periodYears)
        assertEquals("영아기", ui.previousPeriodName)
        assertEquals(listOf("teen_study"), ui.focusOptions.map { it.focusId })
        assertEquals(listOf(1f, 0f), ui.periodMarks.map { it.fill })
        assertNull(ui.decision)
    }

    @Test
    fun `auto step shows the year that just passed and its log line`() {
        val state = PeriodRunState(
            runId = "run",
            runSeed = 1L,
            period = LifePeriod.TEEN,
            yearInPeriod = 3,
            selectedFocusId = "teen_study",
            moments = listOf(
                LifeMoment(LifePeriod.TEEN, 1, "교복이 컸다."),
                LifeMoment(LifePeriod.TEEN, 2, "학원이 늘었다."),
            ),
        )
        val ui = buildPeriodPlayState(
            TurnStep(state = state, phase = TurnPhase.AUTO_PROGRESS, sceneImageId = "img_scene_teen_study_001", canAutoAdvance = true),
            content(),
            playerName = "재겸",
        )

        assertEquals(PlayPhase.AUTO, ui.phase)
        assertEquals(2, ui.yearInPeriod)
        assertEquals("청소년기 · 2번째 해", ui.place)
        assertEquals("학원이 늘었다.", ui.log.last().text)
        assertEquals("청소년기 · 2번째 해", ui.log.last().label)
        assertEquals("학업", ui.focus?.label)
        assertEquals("scene/teen_study.png", ui.sceneFile)
        assertEquals(0.4f, ui.periodMarks[1].fill, 0.001f)
    }

    @Test
    fun `focus effects list ups before downs`() {
        val effects = study.toOptionUi().effects
        assertEquals(listOf(StatChangeUi("지력", 4), StatChangeUi("행복", -1)), effects)
    }

    @Test
    fun `korean particle ro follows the final consonant`() {
        assertEquals("학업으로", "학업".withRo())
        assertEquals("놀기로", "놀기".withRo())
        assertEquals("일로", "일".withRo())
        assertEquals("애착으로", "애착".withRo())
    }

    @Test
    fun `player name is replaced inside decision outcomes and focus logs`() {
        val named = fever.withPlayerName("재겸", nameCallOf("재겸"))
        assertEquals("재겸아, 열이 끓는다", named.title)
        assertEquals("재겸의 하루가 빙글 돈다.", named.text)

        val focus = study.copy(yearlyLogTemplates = listOf("{nameCall} 오늘도 책상 앞")).withPlayerName("민지", "민지야")
        assertEquals("민지야 오늘도 책상 앞", focus.yearlyLogTemplates.single())
    }

    @Test
    fun `starters are parsed from the characters file`() {
        val starters = parseStarters(
            """
            [{"starterId":"starter_loud","name":"우렁찬 아기","description":"목청이 크다.",
              "startStatDelta":{"운동능력":5,"사회성":3,"지력":-3},"startFlags":["starter_loud"]}]
            """.trimIndent(),
        )
        val starter = starters.single()
        assertEquals("우렁찬 아기", starter.name)
        assertEquals(StatDelta(fitness = 5, social = 3, intelligence = -3), starter.statDelta)
        assertEquals(setOf("starter_loud"), starter.startFlags)
        assertTrue(parseStarters("not json").isEmpty())
    }

    @Test
    fun `starter delta is clamped to the stat range`() {
        val stats = Stats(health = 98, luck = 1).plusClamped(StatDelta(health = 5, luck = -3))
        assertEquals(100, stats.health)
        assertEquals(0, stats.luck)
    }

    private fun content() = PlayContent(
        periods = periods,
        focuses = listOf(study),
        decisions = mapOf(fever.eventId to fever),
        imageFiles = mapOf("img_scene_teen_study_001" to "scene/teen_study.png"),
    )
}
