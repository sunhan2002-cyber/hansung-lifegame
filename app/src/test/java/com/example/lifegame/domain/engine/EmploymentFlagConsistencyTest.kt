package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class EmploymentFlagConsistencyTest {
    private val rules = listOf(
        LifeFactRule("job_employed", "employmentStatus", "EMPLOYED"),
        LifeFactRule("job_employed__research", "employmentStatus", "EMPLOYED"),
        LifeFactRule("job_retired", "employmentStatus", "RETIRED"),
        LifeFactRule("job_retired__old", "employmentStatus", "RETIRED"),
    )
    private val periods = listOf(PeriodDefinition(LifePeriod.MIDDLE, 0, 5))
    private val engine = GameEngine(skipInfantFocus = true)

    @Test fun newEmploymentKeepsCanonicalAndProvenanceButRemovesThePreviousStatus() {
        val event = DecisionEvent("research", setOf(LifePeriod.MIDDLE), DecisionKind.INCIDENT, "연구", "연구",
            listOf(DecisionOption("A", "선택", listOf(Outcome("hired", "채용", chance = 100,
                addFlags = linkedSetOf("job_employed", "job_employed__research"))))))
        val state = PeriodRunState("run", 2L, LifePeriod.MIDDLE, pendingDecisionId = event.eventId,
            flags = linkedSetOf("job_retired", "job_retired__old"), lifeFacts = LifeFacts(employmentStatus = "RETIRED"))

        val after = engine.resolvePeriodDecision(state, event, "A", periods, rules).step.state

        assertTrue(after.flags.containsAll(setOf("job_employed", "job_employed__research")))
        assertFalse(after.flags.any { it.startsWith("job_retired") })
        assertEquals("EMPLOYED", after.lifeFacts.employmentStatus)
        assertEquals(after, engine.synchronizeLifeFacts(after, rules))
    }

    @Test fun automaticEventsUseTheSameEmploymentValueRule() {
        val event = AutomaticEvent("hired", setOf(LifePeriod.MIDDLE), "채용",
            addFlags = linkedSetOf("job_employed", "job_employed__research"))
        val state = PeriodRunState("run", 2L, LifePeriod.MIDDLE,
            flags = setOf("job_retired", "job_retired__old"), lifeFacts = LifeFacts(employmentStatus = "RETIRED"))
        val after = requireNotNull(engine.applyAutomaticEvent(state, listOf(event), "once", rules)).state
        assertTrue(after.flags.containsAll(setOf("job_employed", "job_employed__research")))
        assertFalse(after.flags.any { it.startsWith("job_retired") })
        assertEquals("EMPLOYED", after.lifeFacts.employmentStatus)
    }

    @Test fun aliasOnlyExistingSaveRegainsItsCanonicalStatusIdempotently() {
        val old = PeriodRunState("saved", 2L, LifePeriod.MIDDLE,
            flags = linkedSetOf("job_retired", "job_employed__research"), lifeFacts = LifeFacts(employmentStatus = "EMPLOYED"))
        val repaired = engine.synchronizeLifeFacts(old, rules)
        assertEquals(setOf("job_employed__research", "job_employed"), repaired.flags)
        assertEquals("EMPLOYED", repaired.lifeFacts.employmentStatus)
        assertEquals(repaired, engine.synchronizeLifeFacts(repaired, rules))
    }

    @Test fun authoredGraduateChoiceRetainsTheRequiredFlagForTheResearchPath() {
        val assets = File("src/main/assets")
        val events = V4ContentParser.parseDecisionEvents(File(assets, "events.sample.json").readText())
        val facts = V4ContentParser.parseLifeFactRules(File(assets, "life_facts.sample.json").readText())
        val event = events.single { it.eventId == "path_int_07" }
        val state = PeriodRunState("research", 42L, LifePeriod.YOUNG_ADULT, stats = Stats(intelligence = 90),
            flags = setOf("int_path", "university"), pendingDecisionId = event.eventId)
        val after = engine.resolvePeriodDecision(state, event, "A",
            listOf(PeriodDefinition(LifePeriod.YOUNG_ADULT, 0, 5)), facts).step.state
        assertTrue(after.flags.containsAll(setOf("job_employed", "job_employed__path_int_07", "edu_graduate", "career_researcher")))
        assertTrue(after.flags.containsAll(events.single { it.eventId == "path_int_09" }.requiredFlags))
        assertEquals("EMPLOYED", after.lifeFacts.employmentStatus)
    }

    @Test fun canonicalStatusRepairDoesNotInventEmploymentHistoryFlags() {
        val facts = V4ContentParser.parseLifeFactRules(File("src/main/assets/life_facts.sample.json").readText())
        for ((flag, status) in mapOf("job_employed" to "EMPLOYED", "job_unemployed" to "UNEMPLOYED", "job_retired" to "RETIRED")) {
            val state = PeriodRunState("status-$flag", 1L, LifePeriod.MIDDLE, flags = setOf(flag))
            val repaired = engine.synchronizeLifeFacts(state, facts)
            assertEquals(status, repaired.lifeFacts.employmentStatus)
            assertFalse(repaired.flags.any { it in setOf("comeback", "laid_off", "early_retire") })
        }
    }
}
