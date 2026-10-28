package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.AvatarConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 10주차 저장 연결: 실제 [LifeGameViewModel] + 김우영 저장소(V4ProgressStore, SharedPreferences)로
 * 인생 하나를 끝까지 살고, 뷰모델을 새로 만들어(앱 재실행과 같은 조건) 기록 · 칭호 · 결과 화면이 그대로인지 본다.
 * 시작 전에 이 앱의 저장값을 백업하고 끝나면 되돌린다.
 */
@RunWith(AndroidJUnit4::class)
class Week10RecordPersistenceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as Application
    private val prefs = app.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
    private var backup: Map<String, *> = emptyMap<String, Any>()

    @Before
    fun backUp() {
        backup = prefs.all.toMap()
        prefs.edit().clear().commit()
    }

    @After
    fun restore() {
        val editor = prefs.edit().clear()
        backup.forEach { (key, value) -> if (value is String) editor.putString(key, value) }
        editor.commit()
    }

    private fun <T> onMain(block: () -> T): T {
        var result: T? = null
        instrumentation.runOnMainSync { result = block() }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    private val profile = NewLifeProfile(
        name = "기록",
        gender = Gender.FEMALE,
        starterId = null,
        avatar = AvatarConfig(skinToneId = "skin_tan", hairId = "hair_long_black", eyesId = "eyes_big"),
    )

    /** 고를 수 있는 첫 선택지로 결정 · 결과 · 장 표지 · 자동 진행을 끝까지 넘긴다. stopAtOutcome이면 첫 결과에서 멈춘다. */
    private fun playUntilEnd(vm: LifeGameViewModel, stopAtOutcome: Boolean = false) {
        var guard = 0
        while (guard++ < 1_500) {
            val play = onMain { vm.play } ?: error("play state disappeared: ${vm.playError}")
            when (play.phase) {
                PlayPhase.ENDED -> return
                PlayPhase.CHAPTER -> onMain { vm.selectFocus(play.focusOptions.first().focusId) }
                PlayPhase.AUTO -> onMain { vm.skipToNextDecision() }
                PlayPhase.DECISION -> {
                    if (play.outcome != null) {
                        if (stopAtOutcome) return
                        onMain { vm.continueAfterOutcome() }
                    } else {
                        val choice = requireNotNull(play.decision).choices.first { it.available }
                        onMain { vm.chooseDecision(choice.choiceId) }
                    }
                }
            }
        }
        error("life did not end")
    }

    @Test
    fun finishedLifeSurvivesRestartOnceAndCanBeReopened() {
        val first = onMain { LifeGameViewModel(app) }
        onMain { first.startNewLife(profile) }
        playUntilEnd(first)

        // 결말에 닿는 순간 완료 기록이 저장된다(홈으로 누르기 전).
        val runId = onMain { requireNotNull(first.currentRecord).runId }
        assertEquals(1, onMain { first.completedLives.count { it.runId == runId } })
        val record = onMain { requireNotNull(first.currentRecord) }
        assertTrue("주마등은 실제 선택에서 만든다", record.recap.isNotEmpty())
        assertEquals("hair_long_black", record.avatar.hairId)

        // 앱이 꺼졌다 켜진 것처럼 새 뷰모델: 기록은 남고, 끝난 상태로 이어하면 다시 저장해도 하나뿐이다.
        val second = onMain { LifeGameViewModel(app) }
        assertEquals(1, onMain { second.completedLives.count { it.runId == runId } })
        onMain { second.continueLife() }
        assertEquals(PlayPhase.ENDED, onMain { second.play?.phase })
        assertEquals(1, onMain { second.completedLives.count { it.runId == runId } })
        onMain { second.finishLife() }
        onMain { second.finishLife() }
        assertNull("현재 슬롯은 비워진다", onMain { second.lifeCard })

        val third = onMain { LifeGameViewModel(app) }
        assertEquals(1, onMain { third.completedLives.size })
        val reopened = onMain { third.recordFor(runId) }
        assertNotNull(reopened)
        assertEquals(record.summary.headline, reopened!!.summary.headline)
        assertEquals(record.title?.text, reopened.title?.text)
        assertEquals(1, onMain { third.titleCollection().earnedCount })

        // 새 인생을 시작해도 지난 인생은 지워지지 않는다.
        onMain { third.startNewLife(profile.copy(name = "둘째")) }
        assertEquals(1, onMain { third.completedLives.size })
    }

    @Test
    fun relaunchWhileReadingAnOutcomeShowsTheSameOutcomeWithoutApplyingItTwice() {
        val first = onMain { LifeGameViewModel(app) }
        onMain { first.startNewLife(profile) }
        playUntilEnd(first, stopAtOutcome = true)
        val before = onMain { requireNotNull(first.play) }
        val outcome = requireNotNull(before.outcome)

        val second = onMain { LifeGameViewModel(app) }
        onMain { second.continueLife() }
        val restored = onMain { requireNotNull(second.play) }
        assertEquals(PlayPhase.DECISION, restored.phase)
        assertEquals(outcome.text, restored.outcome?.text)
        assertEquals(outcome.changes, restored.outcome?.changes)
        assertEquals(before.decision?.title, restored.decision?.title)
        assertEquals("결과가 다시 적용되면 안 된다", before.stats, restored.stats)

        onMain { second.continueAfterOutcome() }
        assertNull(onMain { second.play?.outcome })
    }

    @Test
    fun futureLookIsRequestedOncePerRunAndCancelFallsBack() {
        val vm = onMain { LifeGameViewModel(app) }
        onMain { vm.startNewLife(profile) }
        playUntilEnd(vm)
        val runId = onMain { requireNotNull(vm.currentRecord).runId }

        onMain { vm.generationScenario = FakeScenario.SLOW }
        onMain { vm.requestFutureLook(runId) }
        assertEquals(GenerationStatus.LOADING, onMain { vm.futureLook?.status })
        onMain { vm.requestFutureLook(runId) } // 다시 들어와도 새로 요청하지 않는다
        onMain { vm.cancelFutureLook() }
        val cancelled = onMain { requireNotNull(vm.futureLook) }
        assertEquals(GenerationStatus.FALLBACK, cancelled.status)
        assertTrue(cancelled.message.orEmpty().contains("기본 그림"))

        // 실패도 진행을 막지 않고 대체 문구로 끝난다.
        val other = onMain { LifeGameViewModel(app) }
        onMain { other.generationScenario = FakeScenario.FAILURE }
        onMain { other.requestFutureLook(runId) }
        var waited = 0
        while (onMain { other.futureLook?.status } == GenerationStatus.LOADING && waited++ < 50) Thread.sleep(100)
        assertEquals(GenerationStatus.FALLBACK, onMain { other.futureLook?.status })
    }
}
