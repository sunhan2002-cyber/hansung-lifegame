package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.SaveState
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.Stats
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Real Activity recreation while the ending's disk fence is deliberately held open. */
@RunWith(AndroidJUnit4::class)
class EndingDurabilityRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val base = instrumentation.targetContext
    private val prefix = "ending_durable_${System.nanoTime()}_"
    private val preferenceNames = linkedSetOf<String>()
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val isolated = prefix + name
            preferenceNames += isolated
            return base.getSharedPreferences(isolated, mode)
        }
    }
    private val holdCommit = AtomicBoolean(false)
    private val enteredCommit = CountDownLatch(1)
    private val releaseCommit = CountDownLatch(1)
    private var storage: V4ProgressStore? = null
    private val countHomeEntries = AtomicBoolean(false)
    private val homeEntries = AtomicInteger(0)

    @After
    fun removeOnlyTestPreferences() {
        releaseCommit.countDown()
        storage?.awaitDurable()?.get(10, TimeUnit.SECONDS)
        preferenceNames.forEach { name ->
            base.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(name)
        }
    }

    /** A plain ComponentActivity has no onCreate UI; reinstall the same tree after real recreation. */
    private fun installContent(vm: LifeGameViewModel) {
        compose.activityRule.scenario.onActivity { activity ->
            activity.setContent {
                val nav = rememberNavController()
                DisposableEffect(nav) {
                    val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
                        if (destination.route == Routes.HOME && countHomeEntries.get()) homeEntries.incrementAndGet()
                    }
                    nav.addOnDestinationChangedListener(listener)
                    onDispose { nav.removeOnDestinationChangedListener(listener) }
                }
                LifeGameApp(navController = nav, gameViewModel = vm)
            }
        }
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(8_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test
    fun finishingWaitsThroughActivityRecreationAndBackUntilDurableThenNavigatesOnce() {
        val prefs = app.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
        val tracked = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val actual = prefs.edit()
                return object : SharedPreferences.Editor by actual {
                    override fun commit(): Boolean {
                        if (holdCommit.get()) {
                            enteredCommit.countDown()
                            check(releaseCommit.await(30, TimeUnit.SECONDS)) { "ending fence was not released" }
                        }
                        return actual.commit()
                    }
                }
            }
        }
        val store = V4ProgressStore(tracked).also { storage = it }
        val runId = "ending-durable-recreation"
        // Restore a valid ended checkpoint, avoiding a synthetic UI-only ending or replayed choices.
        store.save(V4SavedRun("기록", PeriodRunState(runId, 42L, LifePeriod.OLD_AGE,
            stats = Stats(health = 0), isEnding = true, isDead = true, endingPeriod = LifePeriod.OLD_AGE)))
        assertTrue(store.awaitDurable().get(10, TimeUnit.SECONDS))
        lateinit var vm: LifeGameViewModel
        instrumentation.runOnMainSync { vm = LifeGameViewModel(app, ContentRepository(app), store) }
        installContent(vm)
        compose.onNodeWithTag("home_continue").performScrollTo().performClick()
        awaitTag("recap_skip")
        compose.onNodeWithTag("recap_skip").performClick()
        awaitTag("future_next")
        compose.onNodeWithTag("future_next").performScrollTo().performClick()
        awaitTag("certificate_done")
        assertTrue(vm.awaitSave().get(10, TimeUnit.SECONDS))
        assertEquals(PlayPhase.ENDED, vm.play?.phase)

        holdCommit.set(true)
        countHomeEntries.set(true)
        compose.onNodeWithTag("certificate_done").assertIsDisplayed().performClick()
        assertTrue(enteredCommit.await(5, TimeUnit.SECONDS))
        awaitTag("ending_save_pending")
        assertTrue(store.saveState.value is SaveState.Pending)
        assertFalse(store.hasSave()) // Memory publication alone is not permission to navigate.

        compose.activityRule.scenario.recreate()
        installContent(vm) // The same retained VM models an Activity configuration change.
        awaitTag("ending_save_pending")
        compose.onNodeWithTag("ending_save_pending").assertIsDisplayed()
        compose.onNodeWithTag("home_new_life").assertDoesNotExist()
        assertEquals(0, homeEntries.get())
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
        compose.onNodeWithTag("ending_save_pending").assertIsDisplayed()
        assertEquals(0, homeEntries.get())

        releaseCommit.countDown()
        awaitTag("home_new_life")
        compose.onNodeWithTag("ending_save_pending").assertDoesNotExist()
        assertTrue(store.saveState.value is SaveState.Durable)
        compose.waitForIdle()
        assertEquals(1, homeEntries.get())
        assertEquals(listOf(runId), store.loadCompletedLives().map { it.runId })
    }
}
