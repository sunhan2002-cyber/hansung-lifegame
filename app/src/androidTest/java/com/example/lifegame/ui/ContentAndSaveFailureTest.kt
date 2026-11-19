package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.CachedContentLoader
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.SaveState
import com.example.lifegame.data.V4LoadResult
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Actual SharedPreferences, ViewModel entry points, and an injected disk-failure boundary. */
@RunWith(AndroidJUnit4::class)
class ContentAndSaveFailureTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val base = instrumentation.targetContext
    private val prefix = "p1_save_${System.nanoTime()}_"
    private val preferenceNames = linkedSetOf<String>()
    private val stores = mutableListOf<V4ProgressStore>()
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val isolated = prefix + name
            preferenceNames += isolated
            return base.getSharedPreferences(isolated, mode)
        }
    }
    private val prefs get() = app.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
    private fun store() = V4ProgressStore(app).also { stores += it }
    private fun onMain(block: () -> Unit) = instrumentation.runOnMainSync(block)
    private fun saved(id: String = "test-run") = V4SavedRun("검증", PeriodRunState(id, 42, LifePeriod.INFANT))
    private fun record(id: String) = CompletedLifeRecord(id, "검증", "요약", null, null, null, null,
        PeriodRunState(id, 42, LifePeriod.OLD_AGE), emptyList())

    @After
    fun removeOnlyIsolatedPreferences() {
        stores.forEach { it.awaitDurable().get(10, TimeUnit.SECONDS) }
        preferenceNames.forEach { name ->
            base.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(name)
        }
    }

    @Test
    fun unreadableAndFutureActiveSlotsSurviveContinueAndAreArchivedOnExplicitNewLife() {
        for (raw in listOf("broken save bytes", """{"saveFormatVersion":999,"futureField":"retain"}""")) {
            prefs.edit().clear().putString("savedRun", raw).commit()
            val storage = store()
            lateinit var vm: LifeGameViewModel
            onMain {
                vm = LifeGameViewModel(app, ContentRepository(app), storage)
                vm.continueLife()
                assertNull(vm.play)
                assertTrue(vm.hasStoredRun)
                assertTrue(vm.playError.orEmpty().contains("원본"))
            }
            assertEquals(raw, prefs.getString("savedRun", null))
            onMain { vm.startNewLife(NewLifeProfile("새 시작", Gender.FEMALE, null, runSeed = 42L)) }
            assertNotNull(vm.play)
            assertTrue(vm.awaitSave().get(10, TimeUnit.SECONDS))
            assertTrue(storage.loadResult() is V4LoadResult.Loaded)
            assertTrue(prefs.all.any { (key, value) -> key.startsWith("savedRunRecovery_") && value == raw })
        }
    }

    @Test
    fun nonStringCorruptionIsRetainedWithItsOriginalPreferenceType() {
        prefs.edit().putInt("savedRun", 7).commit()
        val storage = store()
        assertSame(V4LoadResult.Invalid, storage.loadResult())
        onMain {
            val vm = LifeGameViewModel(app, ContentRepository(app), storage)
            vm.continueLife()
            assertNull(vm.play)
            assertEquals(7, prefs.getInt("savedRun", -1))
            vm.startNewLife(NewLifeProfile("새 시작", Gender.FEMALE, null, runSeed = 42L))
        }
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertTrue(prefs.all.any { (key, value) -> key.startsWith("savedRunRecovery_") && value == 7 })
        assertTrue(storage.loadResult() is V4LoadResult.Loaded)
    }

    @Test
    fun malformedPendingArchiveIsCopiedBeforeAReplacementRecordIsWritten() {
        val future = """{"saveFormatVersion":999,"lives":[]}"""
        prefs.edit().putString("completedLives", future).putInt("completedLivesPending", 19).commit()
        val storage = store()
        assertTrue(storage.loadCompletedLives().isEmpty())
        storage.saveCompletedLife(record("new-record"))
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertEquals(future, prefs.getString("completedLives", null))
        assertTrue(prefs.all.any { (key, value) -> key.startsWith("completedLivesRecovery_") && value == 19 })
        assertEquals(listOf("new-record"), storage.loadCompletedLives().map { it.runId })
    }

    @Test
    fun corruptRequiredContentCannotStartOrOverwriteAnExistingLife() {
        val storage = store()
        storage.save(saved())
        storage.saveCompletedLife(record("completed"))
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        val original = prefs.getString("savedRun", null)
        val broken = ContentRepository(CachedContentLoader { name ->
            if (name == "events.sample.json") "broken" else base.assets.open(name).bufferedReader().use { it.readText() }
        })
        onMain {
            val vm = LifeGameViewModel(app, broken, storage)
            vm.continueLife()
            assertNull(vm.play)
            assertNotNull(vm.contentWarning)
            assertTrue(vm.hasStoredRun)
            assertNull(vm.lifeCard)
            assertEquals(1, vm.finishedLives.size)
            assertNotNull(vm.recordFor("completed"))
            vm.startNewLife(NewLifeProfile("실패해도 보존", Gender.FEMALE, null))
            assertNull(vm.play)
            assertNotNull(vm.playError)
        }
        assertEquals(original, prefs.getString("savedRun", null))
    }

    @Test
    fun immediateReadAndDurabilityAreSeparateAndCommitRunsOffMain() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val committedOnMain = AtomicBoolean(false)
        val tracked = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val actual = prefs.edit()
                return object : SharedPreferences.Editor by actual {
                    override fun commit(): Boolean {
                        committedOnMain.set(Looper.myLooper() == Looper.getMainLooper())
                        entered.countDown()
                        check(release.await(10, TimeUnit.SECONDS))
                        return actual.commit()
                    }
                }
            }
        }
        val storage = V4ProgressStore(tracked).also { stores += it }
        try {
            onMain {
                assertTrue(storage.save(saved()))
                assertEquals(saved(), V4ProgressStore(app).load())
            }
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            assertTrue(storage.saveState.value is SaveState.Pending)
            assertFalse(committedOnMain.get())
        } finally { release.countDown() }
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertTrue(storage.saveState.value is SaveState.Durable)
    }

    @Test
    fun failedCommitCanRetryTheSameCheckpointWithoutRepeatingTheGameAction() {
        val fail = AtomicBoolean(true)
        val tracked = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val actual = prefs.edit()
                return object : SharedPreferences.Editor by actual {
                    override fun commit(): Boolean = if (fail.get()) false else actual.commit()
                }
            }
        }
        val storage = V4ProgressStore(tracked).also { stores += it }
        val expected = saved()
        onMain { assertTrue(storage.save(expected)) }
        assertFalse(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertTrue(storage.saveState.value is SaveState.Failed)
        assertEquals(expected, storage.load())
        fail.set(false)
        storage.retrySave()
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertEquals(expected, storage.load())
        assertTrue(storage.saveState.value is SaveState.Durable)
    }

    @Test
    fun failedMemoryPublicationRetainsTheCheckpointForExplicitRetry() {
        val fail = AtomicBoolean(true)
        val tracked = object : SharedPreferences by prefs {
            override fun edit(): SharedPreferences.Editor {
                val actual = prefs.edit()
                return object : SharedPreferences.Editor by actual {
                    override fun apply() {
                        if (fail.get()) error("injected publication failure")
                        actual.apply()
                    }
                }
            }
        }
        val storage = V4ProgressStore(tracked).also { stores += it }
        val expected = saved()
        onMain { assertFalse(storage.save(expected)) }
        assertNull(storage.load())
        assertFalse(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        fail.set(false)
        storage.retrySave()
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertEquals(expected, storage.load())
    }

    @Test
    fun endingCheckpointAndArchiveAreVisibleAtomicallyAndFinishKeepsOneRecord() {
        val storage = store()
        val completed = record("atomic-ending")
        val observations = mutableListOf<Boolean>()
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { observed, key ->
            if (key == "savedRun" || key == "completedLives") {
                observations += observed.contains("savedRun") && storage.loadCompletedLives().any { it.runId == completed.runId }
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        try {
            onMain { assertTrue(storage.save(saved(completed.runId), completed)) }
            assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
            assertTrue(observations.isNotEmpty())
            assertTrue(observations.all { it })
        } finally { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        onMain {
            assertTrue(storage.finish(completed))
            assertTrue(storage.finish(completed))
        }
        assertTrue(storage.awaitDurable().get(10, TimeUnit.SECONDS))
        assertFalse(storage.hasSave())
        assertEquals(listOf(completed.runId), storage.loadCompletedLives().map { it.runId })
    }
}
