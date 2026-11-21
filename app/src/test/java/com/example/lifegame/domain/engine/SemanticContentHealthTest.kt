package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class SemanticContentHealthTest {
    private val assets = File("src/main/assets")
    private val events = V4ContentParser.parseDecisionEvents(File(assets, "events.sample.json").readText())
    private val facts = V4ContentParser.parseLifeFactRules(File(assets, "life_facts.sample.json").readText())
    private val engine = GameEngine()

    @Test fun bothWorkingParentChoicesApplyAnnualHealthLossOnceWithoutAnUnstatedRecoveryBonus() {
        val event = events.single { it.eventId == "adult_039" }
        for (period in listOf(LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE)) {
            for (choiceId in listOf("A", "B")) {
                val state = PeriodRunState("parent-$period-$choiceId", 42L, period,
                    stats = Stats(health = 50), flags = setOf("married", "has_child", "job_employed"),
                    pendingDecisionId = event.eventId)
                val after = engine.resolvePeriodDecision(state, event, choiceId,
                    listOf(PeriodDefinition(period, 0, 5)), facts).step.state
                assertEquals("annual loss must remain and be applied once", 45, after.stats.health)
                assertTrue(after.flags.contains("job_employed"))
                assertFalse(after.isDead)
            }
        }
    }

    @Test fun familyReconciliationDoesNotReduceIntelligenceInEitherOutcome() {
        val event = events.single { it.eventId == "path_soc_05" }
        val seen = mutableSetOf<String>()
        for (seed in 0L until 64L) {
            val state = PeriodRunState("family-$seed", seed, LifePeriod.YOUNG_ADULT,
                stats = Stats(intelligence = 70), flags = setOf("engaged", "soc_path"),
                pendingDecisionId = event.eventId)
            val resolved = engine.resolvePeriodDecision(state, event, "A",
                listOf(PeriodDefinition(LifePeriod.YOUNG_ADULT, 0, 5)), facts)
            seen += resolved.outcome.outcomeId
            assertEquals(70, resolved.step.state.stats.intelligence)
            assertTrue(resolved.step.state.flags.contains("families_met"))
        }
        assertEquals(setOf("a_video", "a_worse"), seen)
    }
}
