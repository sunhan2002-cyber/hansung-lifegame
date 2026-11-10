package com.example.lifegame.ui

import android.app.Application
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.V4ProgressStore
import com.example.lifegame.data.V4SavedRun
import com.example.lifegame.ui.screens.CHANGE_HOLD_MILLIS
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.PREVIEW_DELAY_MILLIS
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.screens.TYPE_MILLIS
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-09 보완 1 · 2 · 5번: 실제 ViewModel과 저장소(테스트 전용 이름, 실제 저장 파일은 건드리지 않음)에 화면을 붙여
 * 본문 → 그림 선노출 → 선택지 순서, 타이핑 중 첫 터치, 선택 연타, 결과 변화 애니메이션 중 연타 · 화면 복원 ·
 * ViewModel 재생성 · 동작 줄이기 · 계속하기 연타에서 보상(능력치 · 선택 기록)이 정확히 한 번만 반영되는지 센다.
 */
@RunWith(AndroidJUnit4::class)
class ResultRewardOnceTest {
    @get:Rule val compose = createComposeRule()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week11_reward_${System.nanoTime()}_"
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int) =
            base.getSharedPreferences(prefix + name, mode).also { prefNames += prefix + name }
    }
    private lateinit var vm: LifeGameViewModel

    /** 저장소는 apply()로 늦게 쓰므로, commit()으로 앞선 쓰기를 모두 끝낸 뒤 지운다(지운 뒤 파일이 다시 생기지 않게). */
    @After fun cleanOnlyTestPreferences() {
        prefNames.forEach {
            base.getSharedPreferences(it, android.content.Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(it)
        }
    }

    private fun saved(): V4SavedRun = requireNotNull(V4ProgressStore(app).load()) { "진행 중인 인생이 저장돼 있어야 한다" }
    private fun state() = requireNotNull(vm.play)

    private fun startAtFirstDecision() {
        compose.runOnUiThread {
            vm = LifeGameViewModel(app)
            vm.startNewLife(NewLifeProfile("검증", Gender.FEMALE, null))
            var guard = 0
            while (vm.play?.phase != PlayPhase.DECISION && guard++ < 30) {
                val s = requireNotNull(vm.play)
                when (s.phase) {
                    PlayPhase.CHAPTER -> vm.selectFocus(s.focusOptions.first().focusId)
                    PlayPhase.AUTO -> vm.skipToNextDecision()
                    else -> break
                }
            }
        }
        assertEquals(PlayPhase.DECISION, state().phase)
    }

    private fun bind(restoration: ManualRestoreHost? = null) {
        compose.mainClock.autoAdvance = false
        val content: @Composable () -> Unit = {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                    vm.play?.takeIf { it.decision != null }?.let { s ->
                        DecisionScreen(s, onChoose = vm::chooseDecision, onContinue = vm::continueAfterOutcome)
                    }
                }
            }
        }
        if (restoration != null) restoration.setContent(content) else compose.setContent(content)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun tap(tag: String) {
        compose.onNodeWithTag(tag, useUnmergedTree = true).performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun firstAvailableKey() = requireNotNull(state().decision).choices.first { it.available }.key

    /** 타이핑 중 첫 터치는 전문만, 그다음 그림 선노출, 한 번 터치 뒤 선택지. 그 사이 보상은 없다. */
    private fun revealChoices(reducedMotion: Boolean = false) {
        val before = state().stats
        val recorded = saved().choices.size
        if (!reducedMotion) {
            advance(TYPE_MILLIS * 3)
            tap("decision_body")
        } else {
            advance(TYPE_MILLIS)
        }
        assertEquals("첫 터치는 보상을 부르지 않는다", before, state().stats)
        assertNull(state().outcome)
        assertEquals(recorded, saved().choices.size)
        compose.onAllNodesWithTag("decision_choice_A").assertCountEquals(0)
        compose.onAllNodesWithTag("scene_view_button").assertCountEquals(0)
        advance(PREVIEW_DELAY_MILLIS + 16)
        compose.onNodeWithTag("scene_preview").assertIsDisplayed()
        compose.onAllNodesWithTag("decision_choice_A").assertCountEquals(0)
        tap("scene_preview")
        advance(TAP_GUARD_MILLIS + 50)
        compose.onNodeWithTag("decision_choice_A").assertExists()
        assertEquals(before, state().stats)
    }

    @Test
    fun rapidChoiceTapsAndTapsDuringTheResultAnimationApplyTheRewardOnce() {
        startAtFirstDecision()
        bind()
        val before = state().stats
        val recorded = saved().choices.size
        revealChoices()
        val key = firstAvailableKey()
        // 다섯 번 연타를 한 묶음으로 보낸다(첫 터치 뒤 화면이 바뀌기 전에 모두 같은 카드에 닿는다).
        compose.onNodeWithTag("decision_choice_$key").performTouchInput { repeat(5) { click(center) } }
        compose.mainClock.advanceTimeByFrame()
        val outcome = requireNotNull(state().outcome)
        val after = state().stats
        val record = saved()
        assertEquals("선택 기록은 한 번", recorded + 1, record.choices.size)
        assertEquals(before, record.pendingOutcome?.beforeStats)
        assertEquals(after, record.pendingOutcome?.afterStats)
        assertTrue("첫 결정은 능력치를 바꾼다", outcome.changes.isNotEmpty())
        advance(300)
        compose.onNodeWithTag("outcome_floating_changes", useUnmergedTree = true).assertExists()
        repeat(5) { tap("outcome_text") }
        advance(CHANGE_HOLD_MILLIS + 800)
        compose.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
        assertEquals(after, state().stats)
        assertEquals(outcome, state().outcome)
        assertEquals(recorded + 1, saved().choices.size)
    }

    @Test
    fun restoringTheScreenOrTheViewModelDuringTheAnimationNeitherReplaysNorReapplies() {
        startAtFirstDecision()
        val restoration = ManualRestoreHost(compose)
        bind(restoration)
        revealChoices()
        tap("decision_choice_${firstAvailableKey()}")
        val outcome = requireNotNull(state().outcome)
        val after = state().stats
        val recorded = saved().choices.size
        advance(400)
        compose.onNodeWithTag("outcome_floating_changes", useUnmergedTree = true).assertExists()
        // 애니메이션 도중 화면이 다시 만들어져도(회전 · 다크 모드 전환 등) 다시 띄우지 않는다.
        restoration.recreate()
        assertEquals(1, restoration.recreations)
        advance(300)
        compose.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithTag("outcome_text").assertIsDisplayed()
        assertEquals(after, state().stats)
        assertEquals(recorded, saved().choices.size)
        // 앱을 닫았다가 이어하기: 결과는 그대로 복원되고 보상은 다시 반영되지 않는다.
        var restored: LifeGameViewModel? = null
        compose.runOnUiThread { restored = LifeGameViewModel(app).apply { continueLife() } }
        assertEquals(outcome, restored?.play?.outcome)
        assertEquals(after, restored?.play?.stats)
        assertEquals(recorded, saved().choices.size)
    }

    @Test
    fun reducedMotionShowsStaticChangesAndRapidContinueTapsMoveOnOnce() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        fun shell(command: String): String =
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }
        val original = shell("settings get global animator_duration_scale")
        try {
            shell("settings put global animator_duration_scale 0")
            startAtFirstDecision()
            bind()
            revealChoices(reducedMotion = true)
            tap("decision_choice_${firstAvailableKey()}")
            val after = state().stats
            val recorded = saved().choices.size
            advance(300)
            compose.onNodeWithTag("outcome_stat_changes", useUnmergedTree = true).assertExists()
            compose.onAllNodesWithTag("outcome_floating_changes", useUnmergedTree = true).assertCountEquals(0)
            compose.onNodeWithText("계속하기").performTouchInput { repeat(4) { click(center) } }
            compose.mainClock.advanceTimeByFrame()
            assertNull(state().outcome)
            assertNull(saved().pendingOutcome)
            assertEquals(after, state().stats)
            assertEquals(recorded, saved().choices.size)
        } finally {
            shell("settings put global animator_duration_scale ${original.takeUnless { it.isBlank() || it == "null" } ?: "1"}")
        }
    }

    /**
     * 2026-11-09 실기(360×640dp): "계속하기" 연타의 남은 터치가 바로 뜬 진행 화면의 4× 버튼을 눌러 배속이 바뀌었다.
     * 화면이 바뀐 직후 연타 보호 시간 동안은 새 화면 버튼이 눌리지 않고, 그 뒤에는 정상으로 눌린다.
     */
    @Test
    fun tapsRightAfterContinueDoNotPressButtonsOnTheNextScreen() {
        startAtFirstDecision()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { PlayHost(vm, onGoHome = {}) }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        advance(TAP_GUARD_MILLIS + 50)
        revealChoices()
        tap("decision_choice_${firstAvailableKey()}")
        advance(CHANGE_HOLD_MILLIS + 800)
        val speed = state().speed
        val recorded = saved().choices.size
        compose.onNodeWithText("계속하기").performTouchInput { click(center) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("screen_change_tap_guard").assertExists()
        when (state().phase) {
            PlayPhase.AUTO -> {
                compose.onNodeWithText("4×").performTouchInput { click(center) }
                compose.mainClock.advanceTimeByFrame()
                assertEquals("화면이 바뀐 직후 터치는 배속을 바꾸지 않는다", speed, state().speed)
                advance(TAP_GUARD_MILLIS + 50)
                compose.onAllNodesWithTag("screen_change_tap_guard").assertCountEquals(0)
                compose.onNodeWithText("4×").performTouchInput { click(center) }
                compose.mainClock.advanceTimeByFrame()
                assertEquals("보호 시간이 지나면 정상으로 눌린다", 4, state().speed)
            }
            PlayPhase.CHAPTER -> {
                val focus = state().focusOptions.first()
                compose.onNodeWithText(focus.label, substring = true).performTouchInput { click(center) }
                compose.mainClock.advanceTimeByFrame()
                assertEquals("화면이 바뀐 직후 터치는 초점을 고르지 않는다", PlayPhase.CHAPTER, state().phase)
            }
            else -> error("계속하기 뒤 화면: ${state().phase}")
        }
        assertEquals(recorded, saved().choices.size)
    }
}
