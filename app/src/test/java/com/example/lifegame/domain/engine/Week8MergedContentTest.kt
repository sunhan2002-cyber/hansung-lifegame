package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.*
import com.example.lifegame.ui.*
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** Exercises the same assets, rules, options and engine configuration as the app. */
class Week8MergedContentTest {
    private val assets = File("src/main/assets")
    private fun json(name: String) = File(assets, "$name.sample.json").readText()
    private val periods = V4ContentParser.parsePeriods(json("periods"))
    private val focuses = V4ContentParser.parseFocuses(json("focuses"))
    private val events = V4ContentParser.parseDecisionEvents(json("events"))
    private val rules = V4ContentParser.parseLifeFactRules(json("life_facts"))
    private val endings = V4ContentParser.parseEndings(json("endings"))

    private fun session(seed: Long) = PeriodGameSession(
        periods, focuses, events, endings, rules,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "qa-$seed" }, runSeedFactory = { seed },
    )

    private fun play(target: LifePathKind, seed: Long): PeriodRunState {
        val session = session(seed)
        var step = session.start()
        repeat(150) {
            if (step.phase == TurnPhase.ENDING) {
                assertNotNull(session.ending)
                assertEquals(step.state.endingPeriod, step.state.lifeFacts.deathPeriod)
                assertNotNull(step.state.lifeFacts.deathCause)
                println("$seed ${target.name}: ${session.ending!!.endingId}, ${step.state.endingPeriod}, ${step.state.lifeFacts}")
                return step.state
            }
            step = when (step.phase) {
                TurnPhase.CHAPTER -> session.selectFocus(focuses.filter { it.period == step.state.period }
                    .maxBy { it.statEffects.toDisplayMap()[target.stat] ?: 0 }.focusId)
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val event = events.first { e -> e.eventId == pending.eventId }
                    val available = pending.choices.filter { c -> c.available }
                    assertTrue("All options locked: ${event.eventId} seed=$seed", available.isNotEmpty())
                    val choice = available.maxBy { preview ->
                        val outcome = event.choices.first { c -> c.choiceId == preview.choiceId }
                            .outcomes.firstOrNull { o -> o.primary }!!
                        (outcome.statDelta.toDisplayMap()[target.stat] ?: 0) * 10 + outcome.statDelta.health
                    }
                    // Restoring a pending decision must preserve facts, stats and outcome RNG.
                    val restored = session(seed)
                    restored.restoreSaved(step.state)
                    val result = session.choose(choice.choiceId)
                    val restoredResult = restored.choose(choice.choiceId)
                    assertEquals(result.outcome, restoredResult.outcome)
                    assertEquals(result.step.state, restoredResult.step.state)
                    result.step
                }
                TurnPhase.ENDING -> error("handled above")
            }
        }
        error("Life did not end: seed=$seed target=$target state=${step.state}")
    }

    @Test fun sevenPathsFinishAcrossTenSeedsAndReplayExactly() {
        LifePathKind.entries.forEach { target ->
            (20261020L..20261029L).forEach { seed -> play(target, seed) }
            assertEquals(play(target, 20261020L), play(target, 20261020L))
        }
    }

    @Test fun authoredOpenOptionsDoNotAcquireRewardBasedStatLocks() {
        val raw = JSONArray(json("events"))
        repeat(raw.length()) { i ->
            val event = raw.getJSONObject(i)
            val parsed = events.first { it.eventId == event.getString("eventId") }
            val choices = event.getJSONArray("choices")
            repeat(choices.length()) { j ->
                val choice = choices.getJSONObject(j)
                if (!choice.has("requiredStats") && !choice.has("stats")) {
                    assertEquals("${parsed.eventId}/${choice.getString("choiceId")}", StatConditions(), parsed.choices[j].statConditions)
                }
            }
        }
    }

    @Test fun restoringFactsRespectsChronologyInsteadOfCatalogOrder() {
        val state = PeriodRunState("facts", 1L, LifePeriod.MIDDLE,
            flags = linkedSetOf("later_in_catalog", "earlier_in_catalog"))
        val rules = listOf(
            LifeFactRule("earlier_in_catalog", "careerTrack", "teacher"),
            LifeFactRule("later_in_catalog", "careerTrack", "researcher"),
        )
        val restored = GameEngine().synchronizeLifeFacts(state, rules)
        assertEquals("teacher", restored.lifeFacts.careerTrack)
    }

    @Test fun uiUsesPersistedFactsAndRecordedDeathRatherThanInferringThem() {
        val catalog = LifeFactCatalog(listOf(LifeFactEntry("teacher", "careerTrack", "teacher", "교사")))
        val state = PeriodRunState("death", 1L, LifePeriod.TEEN, stats = Stats(health = 0),
            isDead = true, isEnding = true, endingPeriod = LifePeriod.TEEN,
            lifeFacts = LifeFacts(careerTrack = "teacher", childrenCount = 2,
                deathPeriod = LifePeriod.TEEN, deathCause = "사고"))
        val ui = buildLifeFacts(state, catalog)
        assertEquals("교사", ui.career)
        assertEquals(2, ui.children)
        assertEquals("사고", buildDeathInfo(state, periods)!!.cause)
        assertEquals("청소년기", buildDeathInfo(state, periods)!!.periodName)
    }
}
