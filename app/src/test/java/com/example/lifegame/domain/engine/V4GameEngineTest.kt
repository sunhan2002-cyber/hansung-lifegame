package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TransitionCue
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class V4GameEngineTest {
    private val engine = GameEngine()
    private val periods = engine.defaultPeriodDefinitions()

    @Test
    fun newPeriodWithoutFocus_returnsChapterStep() {
        val state = engine.createPeriodRun("run", 7L, periods)

        val step = engine.advancePeriodYear(state, periods, focuses(), emptyList())

        assertEquals(TurnPhase.CHAPTER, step.phase)
        assertEquals(TransitionCue.CHAPTER, step.transitionCue)
        assertTrue(step.needsFocusSelection)
        assertFalse(step.canAutoAdvance)
    }

    @Test
    fun focusCannotChangeInsidePeriod() {
        val state = engine.createPeriodRun("run", 7L, periods)
        val selected = engine.selectFocus(state, focus(LifePeriod.INFANT, "focus_a"))

        assertThrows(IllegalArgumentException::class.java) {
            engine.selectFocus(selected, focus(LifePeriod.INFANT, "focus_b"))
        }
    }

    @Test
    fun advancingYear_appliesFocusEffectAndProvidesSceneIds() {
        val focus = focus(
            period = LifePeriod.INFANT,
            id = "focus_health",
            delta = StatDelta(health = 3),
        )
        val state = engine.selectFocus(engine.createPeriodRun("run", 7L, periods), focus)

        val step = engine.advancePeriodYear(state, periods, listOf(focus), emptyList())

        assertEquals(53, step.state.stats.health)
        assertEquals(2, step.state.yearInPeriod)
        assertEquals("scene_focus_health", step.sceneImageId)
        assertEquals("moment_focus_health", step.momentImageId)
        assertTrue(step.canAutoAdvance)
    }

    @Test
    fun milestoneWinsEvenWhenIncidentAlwaysTriggers() {
        val focus = focus(LifePeriod.INFANT, "focus")
        val state = engine.selectFocus(engine.createPeriodRun("run", 7L, periods), focus)
        val milestone = decision("milestone", DecisionKind.MILESTONE, year = 1)
        val incident = decision("incident", DecisionKind.INCIDENT)
        val bigIncident = decision("big", DecisionKind.BIG_INCIDENT)

        val step = engine.advancePeriodYear(
            state,
            periods,
            listOf(focus),
            listOf(incident, bigIncident, milestone),
            bigIncidentChancePercent = 100,
            incidentChancePercent = 100,
        )

        assertEquals("milestone", step.pendingDecision!!.eventId)
        assertEquals(DecisionKind.MILESTONE, step.pendingDecision!!.kind)
    }

    @Test
    fun pendingDecisionStopsAutomaticProgress() {
        val focus = focus(LifePeriod.INFANT, "focus")
        val state = engine.selectFocus(engine.createPeriodRun("run", 7L, periods), focus)

        val step = engine.advancePeriodYear(
            state,
            periods,
            listOf(focus),
            listOf(decision("milestone", DecisionKind.MILESTONE, year = 1)),
        )

        assertEquals(TurnPhase.DECISION, step.phase)
        assertFalse(step.canAutoAdvance)
        assertEquals(1, step.state.yearInPeriod)
        assertEquals("milestone", step.state.pendingDecisionId)
    }

    @Test
    fun sameSeedAndChoiceAlwaysProduceSameOutcome() {
        val event = decisionWithOutcomes(
            listOf(
                Outcome("success", "성공", chance = 50),
                Outcome("failure", "실패", chance = 50),
            ),
        )
        val state = pendingState(seed = 9901L, event = event)

        val first = engine.resolvePeriodDecision(state, event, "A", periods)
        val second = engine.resolvePeriodDecision(state, event, "A", periods)

        assertEquals(first.outcome.outcomeId, second.outcome.outcomeId)
        assertEquals(first.step.state, second.step.state)
    }

    @Test
    fun statsChangeOutcomeWeightsWithoutLockingChoice() {
        val event = decisionWithOutcomes(
            listOf(
                Outcome(
                    outcomeId = "healthy",
                    resultText = "버텼다",
                    chance = 50,
                    chanceWeights = ChanceWeights(health = 10),
                ),
                Outcome(
                    outcomeId = "weak",
                    resultText = "쓰러졌다",
                    chance = 50,
                    chanceWeights = ChanceWeights(health = -10),
                ),
            ),
        )

        val high = engine.resolvePeriodDecision(
            pendingState(1L, event, Stats(health = 100)),
            event,
            "A",
            periods,
        )
        val low = engine.resolvePeriodDecision(
            pendingState(1L, event, Stats(health = 0)),
            event,
            "A",
            periods,
        )

        assertEquals("healthy", high.outcome.outcomeId)
        assertEquals("weak", low.outcome.outcomeId)
        assertEquals(listOf("A"), event.choices.map { it.choiceId })
    }

    @Test
    fun hiddenDeathOutcomeIsNotExposedInPendingPreview() {
        val focus = focus(LifePeriod.TEEN, "focus")
        val event = decisionWithOutcomes(
            outcomes = listOf(
                Outcome("normal", "회복했다", chance = 99),
                Outcome("secret_death", "숨은 사망", chance = 1, hidden = true, death = true),
            ),
            period = LifePeriod.TEEN,
        )
        val state = engine.selectFocus(
            PeriodRunState("run", 7L, LifePeriod.TEEN),
            focus,
        )

        val step = engine.advancePeriodYear(state, periods, listOf(focus), listOf(event))

        assertEquals(listOf("A"), step.pendingDecision!!.choices.map { it.choiceId })
        assertFalse(step.pendingDecision.toString().contains("숨은 사망"))
        assertFalse(step.pendingDecision.toString().contains("secret_death"))
    }

    @Test
    fun deathOutcomeMovesImmediatelyToEnding() {
        val event = decisionWithOutcomes(
            listOf(Outcome("death", "생을 마쳤다", chance = 100, hidden = true, death = true)),
            period = LifePeriod.TEEN,
        )
        val state = pendingState(7L, event, period = LifePeriod.TEEN)

        val result = engine.resolvePeriodDecision(state, event, "A", periods)

        assertTrue(result.step.state.isEnding)
        assertEquals(TurnPhase.ENDING, result.step.phase)
        assertEquals(TransitionCue.DISTORT, result.step.transitionCue)
    }

    @Test
    fun fixedSeedDevelopmentLoopCompletesAll24Turns() {
        var state = engine.createPeriodRun("demo", 20261012L, periods)
        val focuses = focuses()
        var completedTurns = 0

        while (!state.isEnding) {
            if (state.selectedFocusId == null) {
                state = engine.selectFocus(state, focuses.first { it.period == state.period })
            }
            val step = engine.advancePeriodYear(state, periods, focuses, emptyList())
            if (step.recordText != null) completedTurns += 1
            state = step.state
        }

        assertEquals(24, completedTurns)
        assertEquals(24, state.moments.size)
        assertEquals(LifePeriod.MIDDLE, state.period)
    }

    @Test
    fun bigIncidentProvidesPovImageAndDistortCue() {
        val focus = focus(LifePeriod.INFANT, "focus")
        val state = engine.selectFocus(engine.createPeriodRun("run", 77L, periods), focus)
        val event = decision("big", DecisionKind.BIG_INCIDENT)

        val step = engine.advancePeriodYear(
            state,
            periods,
            listOf(focus),
            listOf(event),
            bigIncidentChancePercent = 100,
            incidentChancePercent = 0,
        )

        assertEquals("pov_big", step.pendingDecision!!.povImageId)
        assertEquals(TransitionCue.DISTORT, step.transitionCue)
    }

    private fun focuses(): List<Focus> = LifePeriod.entries.map { period ->
        focus(period, "focus_${period.name.lowercase()}")
    }

    private fun focus(
        period: LifePeriod,
        id: String,
        delta: StatDelta = StatDelta(),
    ) = Focus(
        focusId = id,
        period = period,
        label = "집중 $id",
        statEffects = delta,
        description = "테스트 초점",
        sceneImageId = "scene_$id",
        momentImageId = "moment_$id",
    )

    private fun decision(
        id: String,
        kind: DecisionKind,
        year: Int? = null,
    ) = decisionWithOutcomes(
        outcomes = listOf(Outcome("${id}_outcome", "결과", chance = 100)),
        eventId = id,
        kind = kind,
        year = year,
    )

    private fun decisionWithOutcomes(
        outcomes: List<Outcome>,
        eventId: String = "decision",
        kind: DecisionKind = DecisionKind.MILESTONE,
        year: Int? = 1,
        period: LifePeriod = LifePeriod.INFANT,
    ) = DecisionEvent(
        eventId = eventId,
        periods = setOf(period),
        kind = kind,
        title = "결정",
        text = "어떻게 할까?",
        choices = listOf(DecisionOption("A", "선택한다", outcomes)),
        yearInPeriod = year,
        povImageId = "pov_$eventId",
        momentImageId = "moment_$eventId",
    )

    private fun pendingState(
        seed: Long,
        event: DecisionEvent,
        stats: Stats = Stats(),
        period: LifePeriod = event.periods.first(),
    ) = PeriodRunState(
        runId = "run",
        runSeed = seed,
        period = period,
        stats = stats,
        selectedFocusId = "focus",
        pendingDecisionId = event.eventId,
    )
}
