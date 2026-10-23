package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.ui.LifePathKind
import com.example.lifegame.ui.toDisplayMap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 9주차 밸런스 검수용 실제 콘텐츠 시뮬레이션.
 *
 * 기존 8주차 검사는 집중 능력치를 가장 많이 올리는 선택을 고르므로 성공 경로에 치우친다.
 * 이 검사는 seed와 사건 ID로 선택지를 분산해 실패 결과, 조기 사망, 일반 결말까지 함께 통과시킨다.
 */
class Week9BalanceSimulationTest {
    private val assets = File("src/main/assets")
    private fun json(name: String) = File(assets, "$name.sample.json").readText()
    private val periods = V4ContentParser.parsePeriods(json("periods"))
    private val focuses = V4ContentParser.parseFocuses(json("focuses"))
    private val events = V4ContentParser.parseDecisionEvents(json("events"))
    private val rules = V4ContentParser.parseLifeFactRules(json("life_facts"))
    private val endings = V4ContentParser.parseEndings(json("endings"))

    private fun session(seed: Long) = PeriodGameSession(
        periods = periods,
        focuses = focuses,
        decisions = events,
        endings = endings,
        lifeFactRules = rules,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "week9-$seed" },
        runSeedFactory = { seed },
    )

    private fun play(target: LifePathKind, seed: Long): SimulationResult {
        val session = session(seed)
        var step = session.start()
        repeat(MAX_STEPS) {
            if (step.phase == TurnPhase.ENDING) {
                val ending = assertNotNull(session.ending).let { session.ending!! }
                assertTrue(step.state.isDead)
                assertEquals(step.state.endingPeriod, step.state.lifeFacts.deathPeriod)
                assertNotNull(step.state.lifeFacts.deathCause)
                return SimulationResult(target, seed, ending.endingId, step.state)
            }
            step = when (step.phase) {
                TurnPhase.CHAPTER -> {
                    val focus = focuses
                        .filter { it.period == step.state.period }
                        .maxBy { it.statEffects.toDisplayMap()[target.stat] ?: 0 }
                    session.selectFocus(focus.focusId)
                }
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.DECISION -> {
                    val pending = requireNotNull(step.pendingDecision)
                    val available = pending.choices.filter { it.available }
                    assertTrue("All options locked: ${pending.eventId} seed=$seed", available.isNotEmpty())
                    val choiceIndex = Math.floorMod(
                        "$seed:${pending.eventId}:${step.state.period}:${step.state.yearInPeriod}".hashCode(),
                        available.size,
                    )
                    session.choose(available[choiceIndex].choiceId).step
                }
                TurnPhase.ENDING -> error("handled above")
            }
        }
        error("Life did not end within $MAX_STEPS steps: target=$target seed=$seed state=${step.state}")
    }

    @Test
    fun randomizedChoicesAcrossManySeedsPreserveCoreLifeInvariants() {
        val results = LifePathKind.entries.flatMap { target ->
            SEEDS.map { seed -> play(target, seed) }
        }

        assertEquals(LifePathKind.entries.size * SEEDS.count(), results.size)
        assertTrue(results.none { it.state.lifeFacts.deathPeriod in FORBIDDEN_DEATH_PERIODS })
        assertTrue(results.all { it.state.lifeFacts.deathCause?.isNotBlank() == true })
        assertTrue(
            "Actual content must retain at least one pre-old-age ending across randomized runs",
            results.any { it.state.lifeFacts.deathPeriod != LifePeriod.OLD_AGE },
        )

        LifePathKind.entries.forEach { target ->
            val pathResults = results.filter { it.target == target }
            val endingsByCount = pathResults.groupingBy { it.endingId }.eachCount().toSortedMap()
            val deathPeriods = pathResults.groupingBy { it.state.lifeFacts.deathPeriod }.eachCount()
            val wealthBands = pathResults.groupingBy { it.state.lifeFacts.wealthBand }.eachCount()
            assertTrue(
                "${target.name} should reach at least three different endings",
                endingsByCount.size >= MIN_ENDING_DIVERSITY,
            )
            assertTrue(
                "${target.name} should reach at least one matching path ending",
                pathResults.any { it.endingId.startsWith(target.eventPrefix.replace("path_", "ending_path_")) },
            )
            println("WEEK9_BALANCE ${target.name} endings=$endingsByCount deaths=$deathPeriods wealth=$wealthBands")
        }
    }

    private data class SimulationResult(
        val target: LifePathKind,
        val seed: Long,
        val endingId: String,
        val state: PeriodRunState,
    )

    private companion object {
        const val MAX_STEPS = 150
        const val MIN_ENDING_DIVERSITY = 3
        val SEEDS = 20261000L..20261049L
        val FORBIDDEN_DEATH_PERIODS = setOf(LifePeriod.INFANT, LifePeriod.CHILD)
    }
}
