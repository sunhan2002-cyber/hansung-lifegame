package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.OutcomeLikelihood
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
    fun lifeDamageEndsRunAtZeroAndRecoveryIsClamped() {
        val dangerous = decisionWithOutcomes(
            listOf(Outcome("hit", "큰 사고를 당했다", chance = 100, lifeDelta = -100)),
            period = LifePeriod.TEEN,
        )
        val damaged = engine.resolvePeriodDecision(
            pendingState(7L, dangerous, period = LifePeriod.TEEN, stats = Stats(health = 40)),
            dangerous,
            "A",
            periods,
        )
        assertEquals(0, damaged.step.state.stats.health)
        assertTrue(damaged.step.state.isEnding)
        assertTrue(damaged.step.state.isDead)

        val recovery = decisionWithOutcomes(
            listOf(Outcome("rest", "회복했다", chance = 100, lifeDelta = 20)),
            period = LifePeriod.TEEN,
        )
        val recovered = engine.resolvePeriodDecision(
            pendingState(8L, recovery, period = LifePeriod.TEEN, stats = Stats(health = 90)),
            recovery,
            "A",
            periods,
        )
        assertEquals(100, recovered.step.state.stats.health)
        assertFalse(recovered.step.state.isDead)
    }

    @Test
    fun lowHealthCanCauseSuddenDeathButNormalHealthCannot() {
        val middleFocus = focus(LifePeriod.MIDDLE, "middle", StatDelta())
        val lowHealthStates = (1L..500L).map { seed ->
            engine.advancePeriodYear(
                engine.selectFocus(
                    PeriodRunState("low-$seed", seed, LifePeriod.MIDDLE, stats = Stats(health = 10)),
                    middleFocus,
                ),
                periods,
                listOf(middleFocus),
                emptyList(),
            )
        }
        assertTrue(lowHealthStates.any { it.state.isEnding && it.state.isDead })

        val normalHealthStates = (1L..500L).map { seed ->
            engine.advancePeriodYear(
                engine.selectFocus(
                    PeriodRunState("normal-$seed", seed, LifePeriod.MIDDLE, stats = Stats(health = 50)),
                    middleFocus,
                ),
                periods,
                listOf(middleFocus),
                emptyList(),
            )
        }
        assertTrue(normalHealthStates.none { it.state.isEnding && it.state.isDead })
    }

    @Test
    fun fixedSeedDevelopmentLoopStopsWhenLifeReachesZero() {
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

        assertEquals(20, completedTurns)
        assertEquals(21, state.moments.size)
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

    @Test
    fun pendingPreviewProvidesEngineCalculatedLikelihoodAndKeyStats() {
        val focus = focus(LifePeriod.INFANT, "focus")
        val event = decisionWithOutcomes(
            outcomes = listOf(
                Outcome(
                    "recover",
                    "회복했다",
                    chance = 60,
                    primary = true,
                    chanceWeights = ChanceWeights(health = 5),
                ),
                Outcome("hidden", "숨은 결과", chance = 3, hidden = true),
                Outcome("worse", "악화됐다", StatDelta(health = -10), chance = 37),
            ),
        )
        val state = engine.selectFocus(engine.createPeriodRun("run", 88L, periods), focus)

        val step = engine.advancePeriodYear(state, periods, listOf(focus), listOf(event))
        val preview = step.pendingDecision!!.choices.single()

        assertEquals(OutcomeLikelihood.HIGH, preview.likelihood)
        assertEquals(listOf("건강"), preview.keyStats)
        assertFalse(preview.toString().contains("숨은 결과"))
    }

    @Test
    fun militaryMilestoneRequiresFrontendGenderFlag() {
        val focus = focus(LifePeriod.YOUNG_ADULT, "career")
        val event = decisionWithOutcomes(
            outcomes = listOf(Outcome("served", "복무했다", chance = 100, primary = true)),
            eventId = "adult_071",
            period = LifePeriod.YOUNG_ADULT,
        ).copy(requiredFlags = setOf("military_service_required"))
        val base = PeriodRunState(
            runId = "run",
            runSeed = 71L,
            period = LifePeriod.YOUNG_ADULT,
            selectedFocusId = focus.focusId,
        )

        val withoutFlag = engine.advancePeriodYear(base, periods, listOf(focus), listOf(event))
        val withFlag = engine.advancePeriodYear(
            base.copy(flags = setOf("gender_male", "military_service_required")),
            periods,
            listOf(focus),
            listOf(event),
        )

        assertNull(withoutFlag.pendingDecision)
        assertEquals("adult_071", withFlag.pendingDecision?.eventId)
        assertFalse(withFlag.canAutoAdvance)
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
