package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class Week11WooyoungFollowupTest {
    private val engine = GameEngine(skipInfantFocus = true)
    private val oldAge = listOf(PeriodDefinition(LifePeriod.OLD_AGE, 0, 3))

    @Test fun oldAgeHealthDecreasesExactlyOnceWhenDecisionResolves() {
        val event = decision("old_decision", LifePeriod.OLD_AGE)
        val state = PeriodRunState("run", 1L, LifePeriod.OLD_AGE, stats = Stats(health = 50), pendingDecisionId = event.eventId)

        val resolved = engine.resolvePeriodDecision(state, event, "A", oldAge).step.state

        assertEquals(45, resolved.stats.health)
    }

    @Test fun oldAgeHealthDecreasesExactlyOnceWithoutDecision() {
        val state = PeriodRunState("run", 1L, LifePeriod.OLD_AGE, stats = Stats(health = 50))

        val step = engine.advancePeriodYear(state, oldAge, emptyList(), emptyList(), 0, 0)

        assertEquals(45, step.state.stats.health)
    }

    @Test fun healthZeroEndsLifeEvenDuringChildhoodFocus() {
        val periods = listOf(PeriodDefinition(LifePeriod.CHILD, 0, 3))
        val focus = Focus("danger", LifePeriod.CHILD, "위험", StatDelta(health = -50), "위험한 집중")
        val selected = engine.selectFocus(engine.createPeriodRun("child", 2L, periods), focus)

        val step = engine.advancePeriodYear(selected, periods, listOf(focus), emptyList(), 0, 0)

        assertTrue(step.state.isDead)
        assertEquals(LifePeriod.CHILD, step.state.lifeFacts.deathPeriod)
        assertEquals(0, step.state.stats.health)
    }

    @Test fun automaticEventIsAppliedOnlyOnceForSameTrigger() {
        val event = AutomaticEvent("minor_1", setOf(LifePeriod.YOUNG_ADULT), "산책했다.", statDelta = StatDelta(happiness = 2), repeatable = true)
        val state = PeriodRunState("run", 3L, LifePeriod.YOUNG_ADULT)

        val first = requireNotNull(engine.applyAutomaticEvent(state, listOf(event), "progress-1"))
        val duplicate = engine.applyAutomaticEvent(first.state, listOf(event), "progress-1")

        assertEquals(52, first.state.stats.happiness)
        assertEquals(1, first.state.automaticEventRecords.size)
        assertNull(duplicate)
    }

    @Test fun pendingDecisionAlwaysHasPriorityOverAutomaticEvent() {
        val event = AutomaticEvent("minor_1", setOf(LifePeriod.YOUNG_ADULT), "짧은 사건")
        val state = PeriodRunState("run", 4L, LifePeriod.YOUNG_ADULT, pendingDecisionId = "major")

        assertNull(engine.applyAutomaticEvent(state, listOf(event), "progress-1"))
    }

    @Test fun employmentFlagsRemainMutuallyExclusive() {
        val rules = listOf(
            LifeFactRule("employed", "employmentStatus", "EMPLOYED"),
            LifeFactRule("unemployed", "employmentStatus", "UNEMPLOYED"),
            LifeFactRule("retired", "employmentStatus", "RETIRED"),
        )
        val event = decision("retire", LifePeriod.YOUNG_ADULT, setOf("retired"))
        val state = PeriodRunState(
            "run", 5L, LifePeriod.YOUNG_ADULT,
            flags = linkedSetOf("employed", "unemployed"),
            lifeFacts = LifeFacts(employmentStatus = "UNEMPLOYED"),
            pendingDecisionId = event.eventId,
        )

        val after = engine.resolvePeriodDecision(state, event, "A", listOf(PeriodDefinition(LifePeriod.YOUNG_ADULT, 0, 3)), rules).step.state

        assertEquals(setOf("retired"), after.flags.intersect(setOf("employed", "unemployed", "retired")))
        assertEquals("RETIRED", after.lifeFacts.employmentStatus)
    }

    private fun decision(id: String, period: LifePeriod, addFlags: Set<String> = emptySet()) = DecisionEvent(
        eventId = id,
        periods = setOf(period),
        kind = DecisionKind.INCIDENT,
        title = id,
        text = id,
        choices = listOf(
            DecisionOption(
                choiceId = "A",
                label = "선택",
                outcomes = listOf(Outcome("result", "결과", addFlags = addFlags, chance = 100)),
            ),
        ),
    )
}
