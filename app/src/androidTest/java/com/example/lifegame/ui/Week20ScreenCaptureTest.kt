package com.example.lifegame.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.screens.DicePanel
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 2026-11-20 과제의 화면 전후 캡처(김재겸). 같은 테스트를 변경 전 · 후 APK에서 돌려 `-e captureLabel before|after`로
 * 기기 `files/w20-captures/<label>/`에 남긴다. 판정은 하지 않고 화면만 남긴다(판정은 각 기능 테스트가 한다).
 * - 6개 시기마다 처음 나오는 자동 진행 화면(선택 결과 · 이야기 · 자동 사건이 섞인 기록), 글꼴 100% · 200%
 * - 프로필 주사위 패널(고정 굴림), 글꼴 100% · 200%
 * 실제 ViewModel은 테스트 전용 이름의 저장소를 쓰고 끝나면 지운다.
 */
@RunWith(AndroidJUnit4::class)
class Week20ScreenCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val label = InstrumentationRegistry.getArguments().getString("captureLabel") ?: "run"
    private val prefNames = mutableSetOf<String>()
    private val prefix = "week20_capture_${System.nanoTime()}_"
    private val app = object : Application() {
        init { attachBaseContext(base) }
        override fun getSharedPreferences(name: String, mode: Int) =
            base.getSharedPreferences(prefix + name, mode).also { prefNames += prefix + name }
    }
    private var fontScale by mutableFloatStateOf(1f)

    @After fun cleanOnlyTestPreferences() {
        prefNames.forEach {
            base.getSharedPreferences(it, android.content.Context.MODE_PRIVATE).edit().clear().commit()
            base.deleteSharedPreferences(it)
        }
    }

    private fun framed(width: Int, height: Int, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale = fontScale)) {
                LifeGameTheme {
                    // 캡처는 몇 프레임 뒤에 찍으므로 정지 화면으로 찍는다(시기 전환 · 해 도장이 반쯤 걸린 채 찍히지 않게).
                    // LifeGameTheme가 저장된 설정으로 이 값을 다시 제공하므로, 테마 안쪽에서 덮어써야 효과가 있다.
                    CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false)) {
                        Box(Modifier.requiredSize(width.dp, height.dp).clipToBounds()) { content() }
                    }
                }
            }
        }
    }

    private fun settle() {
        repeat(6) {
            Thread.sleep(60)
            compose.mainClock.advanceTimeByFrame()
        }
    }

    private fun save(name: String) {
        settle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(requireNotNull(base.getExternalFilesDir("w20-captures")), label)
        assertTrue(directory.isDirectory || directory.mkdirs())
        File(directory, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun bothScales(name: String) {
        fontScale = 1f
        save("${name}_100")
        fontScale = 2f
        save("${name}_200")
        fontScale = 1f
        settle()
    }

    @Test
    fun progressScreenInEveryPeriod() {
        compose.mainClock.autoAdvance = false
        val captured = mutableSetOf<String>()
        var autoCaptured = false
        var shown by androidx.compose.runtime.mutableStateOf<LifeGameViewModel?>(null)
        framed(360, 740) { shown?.let { PlayHost(gameViewModel = it, onGoHome = {}) } }
        for (seed in listOf(20261120L, 20261121L, 20261122L)) {
            lateinit var vm: LifeGameViewModel
            compose.runOnUiThread {
                vm = LifeGameViewModel(app)
                vm.startNewLife(NewLifeProfile("재겸", Gender.FEMALE, null, runSeed = seed))
            }
            shown = vm
            settle()
            repeat(700) {
                var current: PeriodPlayUiState? = null
                compose.runOnUiThread { current = vm.play }
                val play = current ?: return@repeat
                if (play.phase == PlayPhase.AUTO) {
                    if (play.periodName !in captured) {
                        captured += play.periodName
                        bothScales("progress_${captured.size}_${play.periodName}")
                    }
                    if (!autoCaptured && play.automaticEvent != null) {
                        autoCaptured = true
                        bothScales("progress_auto_event_${play.periodName}")
                    }
                }
                if (captured.size == 6 || play.phase == PlayPhase.ENDED) return@repeat
                compose.runOnUiThread {
                    when (play.phase) {
                        PlayPhase.DECISION -> if (play.outcome != null) vm.continueAfterOutcome()
                            else play.decision?.choices?.firstOrNull { it.available }?.let { vm.chooseDecision(it.choiceId) }
                        PlayPhase.CHAPTER -> vm.selectFocus(play.focusOptions.first().focusId)
                        PlayPhase.AUTO -> vm.advanceYear()
                        PlayPhase.ENDED -> Unit
                    }
                }
            }
            if (captured.size == 6) break
        }
        assertTrue("시기 화면을 찍지 못했다: $captured", captured.size == 6)
    }

    @Test
    fun dicePanelNormalAndLarge() {
        compose.mainClock.autoAdvance = false
        framed(360, 1100) {
            Box(Modifier.background(LifeGamePaper).verticalScroll(rememberScrollState()).padding(16.dp)) {
                DicePanel(
                    roll = StartingRoll.decode("3,4,2,5,6,1,1,4,5,4,2,2"),
                    rollNonce = 0,
                    tendency = null,
                    rerollsLeft = 2,
                    onRoll = {},
                )
            }
        }
        bothScales("dice_panel")
    }
}
