/**
 * Area: play — its "연출 비교" sections. Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls this; only its body changes:
 * - `@Composable internal fun PlayMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "6. 플레이 화면": choice reveal, picked choice, stat change, big result. One PhoneFrame hosting the real
 *   DecisionScreen on a fake PeriodPlayUiState (avoid fake states that turn on its BackHandlers).
 * Tags: choice_reveal_*, choice_pick_*, stat_change_*, big_result_*.
 *
 * Stub: one "준비 중" line.
 */
package com.example.lifegame.ui.screens

import androidx.compose.runtime.Composable
import com.example.lifegame.ui.components.MotionStyle

/** Group 6 · 플레이 화면 (choiceReveal · choicePick · statChange · bigResult). Stub. */
@Composable
internal fun PlayMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}
