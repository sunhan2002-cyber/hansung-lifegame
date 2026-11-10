package com.example.lifegame.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * 시계를 멈춘 테스트(`mainClock.autoAdvance = false`)에서도 화면을 실제로 버렸다가 저장된 상태로 다시 만든다.
 *
 * `StateRestorationTester`는 버리기와 다시 만들기를 `runOnIdle`로만 기다려서, 시계가 멈춰 있으면 두 변경이
 * 같은 프레임에서 상쇄돼 화면이 그대로 남는다(복원이 일어나지 않는다). 여기서는 저장 → 한 프레임 비움 →
 * 저장값으로 새로 만들기를 프레임 단위로 진행해 `remember`는 새로, `rememberSaveable`만 되살아나게 한다.
 */
class ManualRestoreHost(private val compose: ComposeContentTestRule) {
    private var shown by mutableStateOf(true)
    private var saved: Map<String, List<Any?>>? = null
    private var registry: SaveableStateRegistry? = null

    /** 지금까지 다시 만든 횟수. 복원이 실제로 일어났는지 테스트에서 확인할 수 있다. */
    var recreations = 0
        private set

    fun setContent(content: @Composable () -> Unit) {
        compose.setContent {
            if (shown) {
                val current = remember { SaveableStateRegistry(saved) { true } }
                registry = current
                CompositionLocalProvider(LocalSaveableStateRegistry provides current) { content() }
            }
        }
    }

    fun recreate() {
        saved = requireNotNull(registry) { "setContent를 먼저 불러야 한다" }.performSave()
        shown = false
        compose.mainClock.advanceTimeByFrame()
        shown = true
        compose.mainClock.advanceTimeByFrame()
        recreations++
    }
}
