package com.example.lifegame.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.lifegame.data.*
import com.example.lifegame.domain.engine.*
import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Week8SaveIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val content = ContentRepository(context).loadV4Content()
    private val endings = ContentRepository(context).loadEndings()
    private fun session(seed: Long) = PeriodGameSession(
        content.periods, content.focuses, content.decisions, endings, content.lifeFactRules,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "store-$seed" }, runSeedFactory = { seed },
    )

    @Test fun realSharedPreferencesRoundTripPreservesEveryPendingDecisionAndOutcome() {
        // Keep test state separate from a player's actual continue-game slot.
        val prefsName = "week8_qa_${System.nanoTime()}"
        val isolated = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int) =
                context.getSharedPreferences(prefsName, mode)
        }
        val store = V4ProgressStore(isolated)
        try {
            LifePathKind.entries.forEach { target ->
                val original = session(20261020L)
                var step = original.start()
                var actions = 0
                while (!step.state.isEnding && actions++ < 150) {
                    store.save(V4SavedRun("검증", step.state))
                    val loaded = requireNotNull(store.load())
                    assertEquals(V4SavedRun("검증", step.state), loaded)
                    val resumed = session(20261020L)
                    val restored = resumed.restoreSaved(loaded.state)
                    assertEquals(step.state, restored.state)
                    assertEquals(step.phase, restored.phase)
                    when (step.phase) {
                        TurnPhase.CHAPTER -> {
                            val focus = content.focuses.filter { it.period == step.state.period }
                                .maxBy { it.statEffects.toDisplayMap()[target.stat] ?: 0 }.focusId
                            step = original.selectFocus(focus)
                            assertEquals(step.state, resumed.selectFocus(focus).state)
                        }
                        TurnPhase.AUTO_PROGRESS -> {
                            step = original.advance()
                            assertEquals(step.state, resumed.advance().state)
                        }
                        TurnPhase.DECISION -> {
                            val pending = requireNotNull(step.pendingDecision)
                            val event = content.decisions.first { it.eventId == pending.eventId }
                            val choice = pending.choices.filter { it.available }.maxBy { preview ->
                                val o = event.choices.first { it.choiceId == preview.choiceId }.outcomes.first { it.primary }
                                (o.statDelta.toDisplayMap()[target.stat] ?: 0) * 10 + o.statDelta.health
                            }
                            val result = original.choose(choice.choiceId)
                            val replay = resumed.choose(choice.choiceId)
                            assertEquals(result.outcome, replay.outcome)
                            assertEquals(result.step.state, replay.step.state)
                            step = result.step
                        }
                        TurnPhase.ENDING -> error("handled by while condition")
                    }
                }
                assertTrue(step.state.isEnding)
                store.save(V4SavedRun("검증", step.state))
                val resumed = session(20261020L)
                resumed.restoreSaved(requireNotNull(store.load()).state)
                assertEquals(original.ending, resumed.ending)
            }
        } finally {
            context.deleteSharedPreferences(prefsName)
        }
    }
}
