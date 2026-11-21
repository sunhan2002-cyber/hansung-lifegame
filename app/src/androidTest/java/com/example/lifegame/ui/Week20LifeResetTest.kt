package com.example.lifegame.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-20 과제(김재겸 프론트): 진행 중 "인생 리셋".
 * 자동 진행 중 · 선택 대기 · 결과 표시 중 · 이어하기 뒤에 각각 확인 · 취소를 확인한다. 취소는 진행과 화면을 그대로 두고,
 * 확인은 진행 슬롯만 지워 새 인생 프로필로 보낸다(완료 기록 · 칭호는 그대로). 확인 연타 · 재실행 뒤에 지운 인생이 다시 열리거나
 * 완료 기록이 늘지 않는다. 저장이 실패하면 지금 인생을 그대로 두고 알린다. 360×640dp · 글꼴 200%에서도 메뉴와 확인 창을 쓸 수 있다.
 */
@RunWith(AndroidJUnit4::class)
class Week20LifeResetTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week20_reset_${System.nanoTime()}_"

    /** 켜 두면 이 테스트 저장소의 쓰기가 실패한다(디스크 쓰기 실패 흉내). */
    @Volatile private var failWrites = false

    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
            val real = base.getSharedPreferences(prefix + name, mode).also { prefNames += prefix + name }
            return FailableSharedPreferences(real) { failWrites }
        }
    }

    @After fun cleanOnlyTestPreferences() {
        prefNames.forEach {
            base.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(it)
        }
    }

    private var fontScale by mutableFloatStateOf(1f)
    private var startedNewLife = 0

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun count(tag: String) = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun awaitTag(tag: String, timeoutMs: Long = 8_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (count(tag) == 0) {
            check(System.currentTimeMillis() < end) { "$tag 화면이 나타나지 않았다" }
            Thread.sleep(10)
            compose.mainClock.advanceTimeByFrame()
        }
    }

    /** 돌발 팝업("사건 보기") · 그림 선노출을 넘겨 사건 본문(상단 메뉴가 있는 화면)까지 간다. */
    private fun awaitDecisionBody(timeoutMs: Long = 10_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (count("decision_body") == 0) {
            check(System.currentTimeMillis() < end) { "사건 본문이 나타나지 않았다" }
            when {
                count("incident_popup") > 0 -> {
                    compose.onNodeWithText("사건 보기").performClick()
                    advance(400)
                }
                count("scene_preview") > 0 -> {
                    compose.onNodeWithTag("scene_preview").performClick()
                    advance(400)
                }
                else -> {
                    Thread.sleep(10)
                    compose.mainClock.advanceTimeByFrame()
                }
            }
        }
        // 본문이 타이핑되는 동안 첫 탭은 "본문 다 보기"로 쓰인다(기존 동작). 본문을 끝내고 터치 보호 시간을 넘긴다.
        compose.onNodeWithTag("decision_title").performClick()
        advance(800)
    }

    /** 실제 저장이 디스크까지 끝나 리셋 화면이 넘어갈 때까지 기다린다. */
    private fun awaitNewLife(before: Int, timeoutMs: Long = 8_000) {
        val end = System.currentTimeMillis() + timeoutMs
        while (startedNewLife == before) {
            check(System.currentTimeMillis() < end) { "리셋 뒤 새 인생 화면으로 가지 않았다" }
            Thread.sleep(20)
            compose.mainClock.advanceTimeByFrame()
        }
    }

    private lateinit var vm: LifeGameViewModel
    private var shown by mutableStateOf<LifeGameViewModel?>(null)

    private fun startLife(skipCutscene: Boolean = true) {
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null, runSeed = 20261120L))
            if (skipCutscene) vm.setCutsceneStatus(CutsceneStatus.SKIPPED)
        }
    }

    private fun showHost() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = fontScale)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) {
                        shown?.let { PlayHost(gameViewModel = it, onGoHome = {}, onStartNewLife = { startedNewLife++ }) }
                    }
                }
            }
        }
        shown = vm
        advance(400)
    }

    private fun drive(until: (PeriodPlayUiState) -> Boolean) {
        repeat(300) {
            var current: PeriodPlayUiState? = null
            compose.runOnUiThread { current = vm.play }
            val play = requireNotNull(current)
            if (until(play)) return
            compose.runOnUiThread {
                when (play.phase) {
                    PlayPhase.DECISION -> if (play.outcome != null) vm.continueAfterOutcome()
                        else vm.chooseDecision(requireNotNull(play.decision).choices.first { it.available }.choiceId)
                    PlayPhase.CHAPTER -> vm.selectFocus(play.focusOptions.first().focusId)
                    PlayPhase.AUTO -> vm.advanceYear()
                    PlayPhase.ENDED -> error("리셋을 확인하기 전에 인생이 끝났다")
                }
            }
        }
        error("원하는 화면에 닿지 못했다")
    }

    private fun openResetDialog() {
        awaitTag("play_menu")
        compose.onNodeWithTag("play_menu", useUnmergedTree = true).performClick()
        compose.mainClock.advanceTimeByFrame()
        awaitTag("play_menu_reset")
        compose.onNodeWithTag("play_menu_reset").performClick()
        compose.mainClock.advanceTimeByFrame()
        awaitTag("life_reset_dialog")
        compose.onNodeWithText("현재 진행 중인 인생은 사라집니다. 새 인생을 시작할까요?", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("life_reset_cancel").assertTextContains("취소")
        compose.onNodeWithTag("life_reset_confirm").assertTextContains("새 인생 시작")
    }

    private fun store() = V4ProgressStore(app)

    private fun assertResetDone(before: Int, completedBefore: Int) {
        awaitNewLife(before)
        assertEquals("새 인생 화면으로 한 번만 간다", before + 1, startedNewLife)
        assertNull("진행 중 인생이 비었다", vm.play)
        assertFalse("진행 슬롯이 지워졌다", store().hasSave())
        assertEquals("완료 기록은 그대로다", completedBefore, store().loadCompletedLives().size)
        // 앱을 다시 켜도 지운 인생이 다시 열리지 않는다.
        lateinit var restarted: LifeGameViewModel
        compose.runOnUiThread { restarted = LifeGameViewModel(app) }
        assertFalse(restarted.hasStoredRun)
        assertNull(restarted.lifeCard)
        assertEquals(completedBefore, restarted.completedLives.size)
    }

    @Test
    fun cancelDuringAutoProgressKeepsTheLifeAndResumes() {
        startLife()
        drive { it.phase == PlayPhase.AUTO }
        showHost()
        val before = requireNotNull(vm.play)
        assertTrue(before.autoPlaying)
        openResetDialog()
        assertFalse("확인 창이 떠 있는 동안 자동 진행은 멈춘다", requireNotNull(vm.play).autoPlaying)
        advance(YEAR_MILLIS_AT_1X * 3)
        assertEquals("멈춘 동안 해가 넘어가지 않는다", before.yearKey, requireNotNull(vm.play).yearKey)
        compose.onNodeWithTag("life_reset_cancel", useUnmergedTree = true).performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onAllNodesWithTag("life_reset_dialog").assertCountEquals(0)
        val after = requireNotNull(vm.play)
        assertEquals(PlayPhase.AUTO, after.phase)
        assertEquals(before.yearKey, after.yearKey)
        assertTrue("취소하면 다시 흐른다", after.autoPlaying)
        assertTrue(store().hasSave())
        assertEquals(0, startedNewLife)
    }

    @Test
    fun confirmWhileAChoiceIsWaitingGoesToANewLifeOnce() {
        startLife()
        drive { it.phase == PlayPhase.DECISION && it.outcome == null && it.decision?.eventId != "infant_007" }
        showHost()
        awaitDecisionBody()
        // 이미 완료한 인생이 있어도 리셋은 지난 기록을 건드리지 않는다.
        val completedBefore = store().loadCompletedLives().size
        openResetDialog()
        // 확인 연타: 두 번 눌러도 한 번만 처리한다.
        compose.onNodeWithTag("life_reset_confirm", useUnmergedTree = true).performClick()
        compose.onAllNodesWithTag("life_reset_confirm", useUnmergedTree = true).fetchSemanticsNodes().takeIf { it.isNotEmpty() }
            ?.let { compose.onNodeWithTag("life_reset_confirm", useUnmergedTree = true).performClick() }
        assertResetDone(0, completedBefore)
    }

    @Test
    fun confirmWhileAResultIsShownDiscardsOnlyTheCurrentLife() {
        startLife()
        drive { it.phase == PlayPhase.DECISION && it.outcome == null && it.decision?.eventId != "infant_007" }
        compose.runOnUiThread { vm.chooseDecision(requireNotNull(vm.play?.decision).choices.first { it.available }.choiceId) }
        assertNotNull("결과 표시 중", vm.play?.outcome)
        showHost()
        openResetDialog()
        compose.onNodeWithTag("life_reset_cancel", useUnmergedTree = true).performClick()
        compose.mainClock.advanceTimeByFrame()
        assertNotNull("취소하면 결과 화면 그대로", vm.play?.outcome)
        openResetDialog()
        compose.onNodeWithTag("life_reset_confirm", useUnmergedTree = true).performClick()
        assertResetDone(0, 0)
    }

    @Test
    fun afterContinuingASavedLifeCancelAndConfirmBothWork() {
        startLife()
        drive { it.phase == PlayPhase.AUTO }
        compose.runOnUiThread { vm = LifeGameViewModel(app).apply { continueLife() } }
        assertNotNull("이어하기로 복원", vm.play)
        fontScale = 2f
        showHost()
        openResetDialog()
        compose.onNodeWithTag("life_reset_cancel", useUnmergedTree = true).performClick()
        compose.mainClock.advanceTimeByFrame()
        assertTrue(store().hasSave())
        openResetDialog()
        compose.onNodeWithTag("life_reset_confirm", useUnmergedTree = true).performClick()
        assertResetDone(0, 0)
    }

    @Test
    fun aFailedClearKeepsTheLifeAndSaysSo() {
        startLife()
        drive { it.phase == PlayPhase.AUTO }
        showHost()
        val before = requireNotNull(vm.play)
        openResetDialog()
        failWrites = true
        compose.onNodeWithTag("life_reset_confirm", useUnmergedTree = true).performClick()
        compose.mainClock.advanceTimeByFrame()
        awaitTag("life_reset_error")
        compose.onNodeWithText("진행 중인 인생은 그대로 있어요", substring = true, useUnmergedTree = true).assertExists()
        assertEquals(0, startedNewLife)
        assertEquals("인생은 그대로", before.runKey, vm.play?.runKey)
        failWrites = false
        assertTrue("저장 슬롯도 그대로", store().hasSave())
    }

    @Test
    fun theMenuIsReachableOnASmallScreenWithLargeText() {
        startLife()
        drive { it.phase == PlayPhase.AUTO }
        fontScale = 2f
        showHost()
        awaitTag("play_menu")
        val root = compose.onRoot().fetchSemanticsNode().size
        val menu = compose.onNodeWithTag("play_menu", useUnmergedTree = true).fetchSemanticsNode()
        assertTrue("메뉴 버튼이 화면 안", menu.positionInRoot.x + menu.size.width <= root.width + 1 && menu.positionInRoot.y >= 0)
        assertTrue("메뉴 버튼 높이 48dp 이상", menu.size.height >= with(compose.density) { 47.dp.roundToPx() })
        openResetDialog()
    }
}

/** 테스트 전용: [failing]이 참이면 쓰기를 실패시킨다(apply는 예외, commit은 false). 읽기는 그대로다. */
private class FailableSharedPreferences(
    private val real: SharedPreferences,
    private val failing: () -> Boolean,
) : SharedPreferences by real {
    override fun edit(): SharedPreferences.Editor {
        val editor = real.edit()
        return object : SharedPreferences.Editor by editor {
            override fun apply() {
                if (failing()) throw IllegalStateException("test write failure") else editor.apply()
            }

            override fun commit(): Boolean = if (failing()) false else editor.commit()
        }
    }
}
