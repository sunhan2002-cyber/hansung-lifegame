package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class V4FullLifeEndingPathTest {

    @Test
    fun `study path completes every period and selects researcher ending`() {
        val session = playFullLife(
            focusDelta = StatDelta(intelligence = 10),
            acquiredFlag = "study_focus",
            seed = 20261014L,
        )

        assertEquals("ending_genius_researcher", session.ending?.endingId)
    }

    @Test
    fun `training path completes every period and selects athlete ending`() {
        val session = playFullLife(
            focusDelta = StatDelta(health = 10, fitness = 10),
            acquiredFlag = "sports_training",
            seed = 20261015L,
        )

        assertEquals("ending_star_athlete", session.ending?.endingId)
    }

    @Test
    fun `ordinary path completes every period and selects default ending`() {
        val session = playFullLife(
            focusDelta = StatDelta(),
            acquiredFlag = "ordinary_life",
            seed = 20261016L,
        )

        assertEquals("ending_ordinary_path", session.ending?.endingId)
    }

    private fun playFullLife(
        focusDelta: StatDelta,
        acquiredFlag: String,
        seed: Long,
    ): PeriodGameSession {
        val focuses = LifePeriod.entries.map { period ->
            Focus(
                focusId = "${period.name.lowercase()}_focus",
                period = period,
                label = "검증 초점",
                statEffects = focusDelta,
                description = "전체 플레이 결말 경로 검증용 초점",
            )
        }
        val decisions = LifePeriod.entries.map { period ->
            DecisionEvent(
                eventId = "${period.name.lowercase()}_milestone",
                periods = setOf(period),
                kind = DecisionKind.MILESTONE,
                title = "${period.name} 검증 사건",
                text = "결말 경로를 위한 선택을 한다.",
                yearInPeriod = 1,
                choices = listOf(
                    DecisionOption(
                        choiceId = "continue",
                        label = "계속한다",
                        outcomes = listOf(
                            Outcome(
                                outcomeId = "success",
                                resultText = "선택을 이어 갔다.",
                                addFlags = setOf(acquiredFlag),
                                chance = 100,
                                primary = true,
                            ),
                        ),
                    ),
                ),
            )
        }
        val session = PeriodGameSession(
            periods = PERIODS,
            focuses = focuses,
            decisions = decisions,
            endings = ENDINGS,
            runIdFactory = { "ending-path-$seed" },
            runSeedFactory = { seed },
        )

        var step = session.start()
        var completedPeriods = 0
        var guard = 0
        while (step.phase != TurnPhase.ENDING && guard++ < 30) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> {
                    completedPeriods += 1
                    session.selectFocus("${step.state.period.name.lowercase()}_focus")
                }
                TurnPhase.DECISION -> session.choose("continue").step
                TurnPhase.AUTO_PROGRESS -> session.advance()
                TurnPhase.ENDING -> step
            }
        }

        assertEquals(TurnPhase.ENDING, step.phase)
        assertEquals(LifePeriod.entries.size, completedPeriods)
        // 시기마다 초점·마일스톤 기록이 남고 노년기 자연사 기록이 마지막에 추가된다.
        assertEquals(LifePeriod.entries.size * 2 + 1, step.state.moments.size)
        assertNotNull(session.ending)
        return session
    }

    private companion object {
        val PERIODS = LifePeriod.entries.mapIndexed { index, period ->
            PeriodDefinition(period, order = index, years = 1)
        }

        val ENDINGS = listOf(
            Ending(
                endingId = "ending_genius_researcher",
                title = "연구자",
                summary = "공부를 이어 갔다.",
                priority = 88,
                conditions = EndingConditions(
                    stats = StatConditions(minIntelligence = 85),
                    requiredFlags = setOf("study_focus"),
                ),
            ),
            Ending(
                endingId = "ending_star_athlete",
                title = "운동선수",
                summary = "훈련을 이어 갔다.",
                priority = 87,
                conditions = EndingConditions(
                    stats = StatConditions(minHealth = 70, minFitness = 85),
                    requiredFlags = setOf("sports_training"),
                ),
            ),
            Ending(
                endingId = "ending_ordinary_path",
                title = "평범한 인생",
                summary = "자기 방식대로 한 회차를 마쳤다.",
                priority = 1,
                isDefault = true,
            ),
        )
    }
}
