package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EndingTier
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifeFactRule
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.StatKey
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Week8LifePathEngineTest {

    @Test fun healthFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.HEALTH)
    @Test fun fitnessFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.FITNESS)
    @Test fun intelligenceFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.INTELLIGENCE)
    @Test fun socialFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.SOCIAL)
    @Test fun wealthFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.WEALTH)
    @Test fun happinessFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.HAPPINESS)
    @Test fun luckFocusedLifeHasOwnEventsFactsAndEnding() = assertPath(StatKey.LUCK)

    @Test
    fun teenHealthZeroRecordsActualDeathPeriodAndCause() {
        val event = DecisionEvent(
            eventId = "teen_health_crisis",
            periods = setOf(LifePeriod.TEEN),
            kind = DecisionKind.MILESTONE,
            title = "청소년기 건강 위기",
            text = "건강이 급격히 악화됐다.",
            choices = listOf(
                DecisionOption(
                    choiceId = "collapse",
                    label = "쓰러진다",
                    outcomes = listOf(
                        Outcome(
                            outcomeId = "fatal",
                            resultText = "청소년기에 생을 마쳤다.",
                            statDelta = StatDelta(health = -100),
                            chance = 100,
                            death = true,
                            deathCause = "급성 질환",
                        ),
                    ),
                ),
            ),
        )
        val state = PeriodRunState(
            runId = "teen-death",
            runSeed = SEED,
            period = LifePeriod.TEEN,
            selectedFocusId = "teen-health",
            pendingDecisionId = event.eventId,
            stats = Stats(health = 30),
        )

        val resolved = GameEngine().resolvePeriodDecision(state, event, "collapse", PERIODS)

        assertEquals(0, resolved.step.state.stats.health)
        assertEquals(LifePeriod.TEEN, resolved.step.state.lifeFacts.deathPeriod)
        assertEquals("급성 질환", resolved.step.state.lifeFacts.deathCause)
        val summary = GameEngine().buildLifeSummary(resolved.step.state)
        assertTrue(summary.contains("청소년기"))
        assertTrue(summary.contains("급성 질환"))
    }

    @Test
    fun sameSeedAndChoicesReproducePathFactsAndEnding() {
        val first = play(StatKey.LUCK, SEED)
        val second = play(StatKey.LUCK, SEED)

        assertEquals(first.state, second.state)
        assertEquals(first.ending, second.ending)
    }

    @Test
    fun legacyFlagOnlySaveIsPromotedToLifeFacts() {
        val migrated = GameEngine().synchronizeLifeFacts(
            PeriodRunState(
                runId = "legacy",
                runSeed = SEED,
                period = LifePeriod.YOUNG_ADULT,
                flags = setOf("university", "career_researcher", "married", "child_two"),
                stats = Stats(wealth = 82),
            ),
            listOf(
                LifeFactRule("university", "educationLevel", "UNIVERSITY"),
                LifeFactRule("career_researcher", "careerTrack", "RESEARCHER"),
                LifeFactRule("married", "maritalStatus", "MARRIED"),
                LifeFactRule("child_two", "childrenAtLeast", "2"),
            ),
        )

        assertEquals("UNIVERSITY", migrated.lifeFacts.educationLevel)
        assertEquals("RESEARCHER", migrated.lifeFacts.careerTrack)
        assertEquals("MARRIED", migrated.lifeFacts.maritalStatus)
        assertEquals(2, migrated.lifeFacts.childrenCount)
        assertEquals("HIGH", migrated.lifeFacts.wealthBand)
    }

    @Test
    fun allSevenPathsProduceDifferentFactsAndEndings() {
        val results = StatKey.entries.map { play(it, SEED) }

        assertEquals(7, results.map { it.ending?.endingId }.toSet().size)
        assertEquals(7, results.map { it.state?.lifeFacts?.careerTrack }.toSet().size)
        assertEquals(7, results.map { it.pathEventIds }.toSet().size)
        assertTrue(results.none { it.ending?.summary == "노년기에서 인생을 마쳤다." })
    }

    private fun assertPath(stat: StatKey) {
        val result = play(stat, SEED)
        assertEquals("ending_${stat.code}", result.ending?.endingId)
        assertEquals(stat.career, result.state?.lifeFacts?.careerTrack)
        assertTrue(result.pathEventIds.all { it.startsWith("path_${stat.code}_") })
        assertTrue(result.ending?.summary.orEmpty().contains("직업 ${stat.career}"))
    }

    private fun play(stat: StatKey, seed: Long): PlayedLife {
        val focuses = LifePeriod.entries.map { period ->
            Focus(
                focusId = "${period.name.lowercase()}_${stat.code}",
                period = period,
                label = "${stat.name} 집중",
                statEffects = stat.delta(10),
                description = "8주차 ${stat.name} 경로",
            )
        }
        val decisions = LifePeriod.entries.flatMap { period ->
            StatKey.entries.map { eventStat ->
                DecisionEvent(
                    eventId = "path_${eventStat.code}_${period.ordinal}",
                    periods = setOf(period),
                    kind = DecisionKind.MILESTONE,
                    title = "${eventStat.name} 경로 사건",
                    text = "집중한 능력이 다음 인생 사실로 이어진다.",
                    choices = listOf(
                        DecisionOption(
                            choiceId = "continue",
                            label = "계속한다",
                            outcomes = listOf(
                                Outcome(
                                    outcomeId = "success",
                                    resultText = "경로를 이어 갔다.",
                                    addFlags = setOf("career_${eventStat.code}"),
                                    chance = 100,
                                ),
                            ),
                        ),
                    ),
                    yearInPeriod = 1,
                    statConditions = eventStat.conditions(55),
                    pathStat = eventStat,
                )
            }
        }
        val endings = StatKey.entries.map { endingStat ->
            Ending(
                endingId = "ending_${endingStat.code}",
                title = "${endingStat.name}의 삶",
                summary = "집중한 선택이 고유한 인생 경로를 만들었다.",
                priority = 80,
                tier = EndingTier.EDUCATION_CAREER_FAMILY,
                conditions = EndingConditions(
                    lifeFacts = LifeFactConditions(careerTrack = endingStat.career),
                ),
            )
        } + Ending("ending_default", "기록 부족", "기록이 부족하다.", 0, isDefault = true)
        val rules = StatKey.entries.map {
            LifeFactRule("career_${it.code}", "careerTrack", it.career)
        }
        val session = PeriodGameSession(
            periods = PERIODS,
            focuses = focuses,
            decisions = decisions,
            endings = endings,
            lifeFactRules = rules,
            runIdFactory = { "week8-${stat.code}" },
            runSeedFactory = { seed },
        )
        var step = session.start()
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 100) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> session.selectFocus("${step.state.period.name.lowercase()}_${stat.code}")
                TurnPhase.DECISION -> session.choose("continue").step
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.ENDING -> step
            }
        }
        assertEquals(TurnPhase.ENDING, step.phase)
        return PlayedLife(
            state = session.state,
            ending = session.ending,
            pathEventIds = step.state.flags
                .filter { it.startsWith("v4:event:path_") }
                .map { it.removePrefix("v4:event:") }
                .toSet(),
        )
    }

    private data class PlayedLife(
        val state: PeriodRunState?,
        val ending: Ending?,
        val pathEventIds: Set<String>,
    )

    private companion object {
        const val SEED = 20261017L
        val PERIODS = LifePeriod.entries.mapIndexed { index, period ->
            PeriodDefinition(period, index, years = 1)
        }
    }
}

private val StatKey.code: String
    get() = when (this) {
        StatKey.HEALTH -> "hea"
        StatKey.FITNESS -> "fit"
        StatKey.INTELLIGENCE -> "int"
        StatKey.SOCIAL -> "soc"
        StatKey.WEALTH -> "wea"
        StatKey.HAPPINESS -> "hap"
        StatKey.LUCK -> "luk"
    }

private val StatKey.career: String
    get() = when (this) {
        StatKey.HEALTH -> "HEALTH_MANAGER"
        StatKey.FITNESS -> "ATHLETE"
        StatKey.INTELLIGENCE -> "RESEARCHER"
        StatKey.SOCIAL -> "COUNSELOR"
        StatKey.WEALTH -> "FOUNDER"
        StatKey.HAPPINESS -> "CREATOR"
        StatKey.LUCK -> "OPPORTUNIST"
    }

private fun StatKey.delta(value: Int): StatDelta = when (this) {
    StatKey.HEALTH -> StatDelta(health = value)
    StatKey.FITNESS -> StatDelta(fitness = value)
    StatKey.INTELLIGENCE -> StatDelta(intelligence = value)
    StatKey.SOCIAL -> StatDelta(social = value)
    StatKey.WEALTH -> StatDelta(wealth = value)
    StatKey.HAPPINESS -> StatDelta(happiness = value)
    StatKey.LUCK -> StatDelta(luck = value)
}

private fun StatKey.conditions(value: Int): StatConditions = when (this) {
    StatKey.HEALTH -> StatConditions(minHealth = value)
    StatKey.FITNESS -> StatConditions(minFitness = value)
    StatKey.INTELLIGENCE -> StatConditions(minIntelligence = value)
    StatKey.SOCIAL -> StatConditions(minSocial = value)
    StatKey.WEALTH -> StatConditions(minWealth = value)
    StatKey.HAPPINESS -> StatConditions(minHappiness = value)
    StatKey.LUCK -> StatConditions(minLuck = value)
}
