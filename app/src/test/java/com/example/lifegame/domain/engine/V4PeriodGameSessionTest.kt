package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class V4PeriodGameSessionTest {
    private val periods = listOf(PeriodDefinition(LifePeriod.INFANT, order = 0, years = 2))
    private val focus = Focus(
        focusId = "infant_health",
        period = LifePeriod.INFANT,
        label = "건강",
        statEffects = StatDelta(health = 1),
        description = "건강하게 자란다.",
    )
    private val milestone = DecisionEvent(
        eventId = "first_step",
        periods = setOf(LifePeriod.INFANT),
        kind = DecisionKind.MILESTONE,
        title = "첫걸음",
        text = "첫걸음을 내디뎠다.",
        choices = listOf(
            DecisionOption(
                choiceId = "A",
                label = "계속 걷는다.",
                outcomes = listOf(Outcome("walked", "한 걸음 더 걸었다.", chance = 100)),
            ),
        ),
        yearInPeriod = 1,
    )

    @Test
    fun sessionConnectsChapterDecisionAndNextYear() {
        val session = session()

        assertEquals(TurnPhase.CHAPTER, session.start().phase)
        val pending = session.selectFocus(focus.focusId)
        assertEquals(TurnPhase.DECISION, pending.phase)
        assertEquals("first_step", session.state!!.pendingDecisionId)

        val resolved = session.choose("A")
        assertEquals("walked", resolved.outcome.outcomeId)
        assertNull(session.state!!.pendingDecisionId)
        assertEquals(2, session.state!!.yearInPeriod)
        assertTrue(resolved.step.canAutoAdvance)
    }

    @Test
    fun restoringPendingDecisionKeepsAutomaticProgressStopped() {
        val original = session()
        original.start()
        val pending = original.selectFocus(focus.focusId)

        val restored = session().restore(pending.state)

        assertEquals(TurnPhase.DECISION, restored.phase)
        assertEquals("first_step", restored.pendingDecision!!.eventId)
        assertEquals(false, restored.canAutoAdvance)
    }

    private fun session() = PeriodGameSession(
        periods = periods,
        focuses = listOf(focus),
        decisions = listOf(milestone),
        runIdFactory = { "test-run" },
        runSeedFactory = { 42L },
    )
}
