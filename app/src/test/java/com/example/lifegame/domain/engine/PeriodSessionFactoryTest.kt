package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class PeriodSessionFactoryTest {
    private val periods = listOf(PeriodDefinition(LifePeriod.INFANT, 0, 2))
    private val profile = SessionStartProfile(
        genderFlag = "gender_male", starterId = "curious",
        starterDelta = StatDelta(intelligence = 20), starterFlags = setOf("starter_curious"),
        rolledDelta = StatDelta(intelligence = 6), startingRollRaw = "3,4,5,5,3,4,3,4,3,4,3,4", runSeed = 42L,
    )

    @Test fun completeProfileIsAppliedBeforeTheFirstDecisionIsSelected() {
        val male = decision("male_intro").copy(requiredFlags = setOf("gender_male", "starter_curious"),
            statConditions = StatConditions(minIntelligence = 56))
        val female = decision("female_intro").copy(requiredFlags = setOf("gender_female"))
        val factory = factory(listOf(male, female))

        val started = factory.start(profile, "test")
        assertEquals("male_intro", started.step.pendingDecision?.eventId)
        assertEquals(56, started.step.state.stats.intelligence)
        assertEquals(42L, started.step.state.runSeed)
        assertTrue(started.step.state.flags.containsAll(setOf("starter_curious", "gender_male", "military_service_required")))

        val other = factory.start(profile.copy(genderFlag = "gender_female"), "female")
        assertEquals("female_intro", other.step.pendingDecision?.eventId)
        assertFalse("military_service_required" in other.step.state.flags)
    }

    @Test fun legacyStarterBonusIsUsedOnlyWhenThereIsNoDiceRoll() {
        assertEquals(70, profile.copy(rolledDelta = null, startingRollRaw = null).initialStats.intelligence)
        assertEquals(56, profile.initialStats.intelligence)
    }

    @Test fun automaticEventsUseTheSameProfileAndAreNotReappliedAfterRestoration() {
        val automatic = AutomaticEvent("male_memory", setOf(LifePeriod.INFANT), "기억", statDelta = StatDelta(happiness = 3))
        val factory = PeriodSessionFactory(periods, emptyList(), listOf(decision("intro")), emptyList(), emptyList(),
            listOf(automatic), listOf(AutomaticEventRule(automatic, stage = AutomaticEventStage.PERIOD_START,
                requiredFlags = setOf("gender_male"))))
        val started = factory.start(profile, "same-run")
        assertEquals("male_memory", started.step.automaticEvent?.eventId)
        assertEquals(53, started.step.state.stats.happiness)
        assertEquals(1, started.step.state.automaticEventRecords.size)
        assertEquals(started.step, factory.start(profile, "same-run").step)

        val restored = factory.create()
        assertEquals(started.step.state, restored.restoreSaved(started.step.state).state)
        val next = restored.advance()
        assertEquals("intro", next.pendingDecision?.eventId)
        assertEquals(53, next.state.stats.happiness)
        assertEquals(1, next.state.automaticEventRecords.size)
    }

    @Test fun healthZeroEndsLifeAndRecordsTheActualPeriodAtEveryAge() {
        for (period in LifePeriod.entries) {
            val event = decision("health_zero").copy(periods = setOf(period),
                choices = listOf(DecisionOption("A", "선택", listOf(Outcome("zero", "결과", statDelta = StatDelta(health = -1), chance = 100)))))
            val before = PeriodRunState("test-$period", 1L, period, stats = Stats(health = 1), pendingDecisionId = event.eventId)
            val after = GameEngine(skipInfantFocus = true).resolvePeriodDecision(before, event, "A",
                listOf(PeriodDefinition(period, 0, 3))).step
            assertEquals(period.name, TurnPhase.ENDING, after.phase)
            assertEquals(0, after.state.stats.health)
            assertTrue(after.state.isDead)
            assertEquals(period, after.state.lifeFacts.deathPeriod)
            assertEquals("건강 악화", after.state.lifeFacts.deathCause)
        }
    }

    private fun factory(decisions: List<DecisionEvent>) = PeriodSessionFactory(
        periods, emptyList(), decisions, emptyList(), emptyList(), emptyList(), emptyList(),
    )

    private fun decision(id: String) = DecisionEvent(id, setOf(LifePeriod.INFANT), DecisionKind.MILESTONE,
        id, id, listOf(DecisionOption("A", "선택", listOf(Outcome("done", "결과", chance = 100)))), yearInPeriod = 1)
}
