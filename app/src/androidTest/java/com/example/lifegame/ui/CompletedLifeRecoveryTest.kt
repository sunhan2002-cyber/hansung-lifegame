package com.example.lifegame.ui

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.Week10ProgressCodec
import com.example.lifegame.domain.model.*
import org.json.JSONObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletedLifeRecoveryTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
    private var backup: Map<String, *> = emptyMap<String, Any>()
    @Before fun prepare() { backup = prefs.all.toMap(); prefs.edit().clear().commit() }
    @After fun restore() {
        val editor = prefs.edit().clear()
        backup.forEach { (key, value) -> if (value is String) editor.putString(key, value) }
        editor.commit()
    }
    private fun record(id: String) = CompletedLifeRecord(id, "사용자", "요약", null, null, null, null,
        PeriodRunState(id, 1, LifePeriod.OLD_AGE), emptyList())

    @Test fun partialDamagePreservesGoodLifeAndBacksUpOriginal() {
        val root = JSONObject(Week10ProgressCodec.encodeCompletedLives(listOf(record("good"), record("bad"))))
        root.getJSONArray("lives").getJSONObject(1).remove("runId")
        val raw = root.toString()
        prefs.edit().putString("completedLives", raw).commit()
        val store = V4ProgressStore(context)
        assertEquals(listOf("good"), store.loadCompletedLives().map { it.runId })
        store.saveCompletedLife(record("new"))
        assertEquals(listOf("new", "good"), V4ProgressStore(context).loadCompletedLives().map { it.runId })
        assertTrue(prefs.all.any { (key, value) -> key.startsWith("completedLivesRecovery_") && value == raw })
    }

    @Test fun futureAndBrokenArchivesRemainIntactWhileNewLivesStayReadable() {
        for (raw in listOf("broken", """{"saveFormatVersion":999,"lives":[]}""")) {
            prefs.edit().clear().putString("completedLives", raw).commit()
            V4ProgressStore(context).saveCompletedLife(record("new"))
            assertEquals(raw, prefs.getString("completedLives", null))
            assertEquals(listOf("new"), V4ProgressStore(context).loadCompletedLives().map { it.runId })
        }
    }
}
