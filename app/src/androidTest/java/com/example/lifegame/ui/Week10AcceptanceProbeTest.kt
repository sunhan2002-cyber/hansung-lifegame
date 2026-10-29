package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Final acceptance probes deliberately fail while the corresponding feature is unfinished. */
@RunWith(AndroidJUnit4::class)
class Week10AcceptanceProbeTest {
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week10_qa_${System.nanoTime()}_"
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int) =
            base.getSharedPreferences(prefix + name, mode).also { prefNames += prefix + name }
    }

    @After fun cleanOnlyTestPreferences() { prefNames.forEach { base.deleteSharedPreferences(it) } }
    private fun newLife(vm: LifeGameViewModel) = vm.startNewLife(NewLifeProfile("검증", Gender.FEMALE, null))

    @Test fun pendingDecisionSurvivesViewModelRecreation() {
        val original = LifeGameViewModel(app)
        newLife(original)
        val before = requireNotNull(original.play)
        val restored = LifeGameViewModel(app).apply { continueLife() }
        assertEquals(before.runKey, restored.play?.runKey)
        assertEquals(before.stats, restored.play?.stats)
        assertEquals(before.decision, restored.play?.decision)
    }

    @Test fun selectedResultSurvivesViewModelRecreationWithoutReapplyingReward() {
        val original = LifeGameViewModel(app)
        newLife(original)
        val option = requireNotNull(original.play?.decision).choices.first { it.available }
        original.chooseDecision(option.choiceId)
        val before = requireNotNull(original.play)
        assertNotNull(before.outcome)
        val restored = LifeGameViewModel(app).apply { continueLife() }
        assertNotNull("F10-07: selected result must be restored, not skipped", restored.play?.outcome)
        assertEquals(before.outcome, restored.play?.outcome)
        assertEquals(before.stats, restored.play?.stats)
    }

    @Test fun threeCompletedLivesSurviveViewModelRecreation() {
        val original = LifeGameViewModel(app)
        repeat(3) {
            newLife(original)
            var guard = 0
            while (original.play?.phase != PlayPhase.ENDED && guard++ < 150) {
                val state = requireNotNull(original.play)
                when {
                    state.outcome != null -> original.continueAfterOutcome()
                    state.phase == PlayPhase.CHAPTER -> original.selectFocus(state.focusOptions.first().focusId)
                    state.phase == PlayPhase.AUTO -> original.skipToNextDecision()
                    state.phase == PlayPhase.DECISION -> original.chooseDecision(requireNotNull(state.decision).choices.first { c -> c.available }.choiceId)
                }
            }
            assertEquals(PlayPhase.ENDED, original.play?.phase)
            original.finishLife()
        }
        assertEquals(3, original.finishedLives.size)
        assertEquals("F10-07: completed lives cannot live only in memory", original.finishedLives, LifeGameViewModel(app).finishedLives)
    }

    @Test fun unsupportedFutureSaveVersionIsNotSilentlyLoaded() {
        val store = V4ProgressStore(app)
        store.save(V4SavedRun("검증", PeriodRunState("future-schema", 1, LifePeriod.INFANT)))
        val prefs = app.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
        val raw = JSONObject(requireNotNull(prefs.getString("savedRun", null))).put("saveFormatVersion", 999)
        prefs.edit().putString("savedRun", raw.toString()).commit()
        assertNull("F10-07: unknown future schema must be rejected or explicitly migrated", store.load())
    }
}
