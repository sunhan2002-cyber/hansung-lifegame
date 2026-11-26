/**
 * Area: time — its "연출 비교" sections. Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — MotionCompareScreen (integrator) calls this; only its body changes:
 * - `@Composable internal fun TimeMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)` — group
 *   "7. 해 · 시기 전환": the year stamp on the real PeriodTrack and the period turn (real PeriodTurnOverlay, a demo
 *   overload is allowed). Demos provide their own fake LocalLifeClock. The stamp label is the "N번째 해" yearLabel;
 *   no ages anywhere.
 * Tags: age_stamp_*, period_transition_*.
 *
 * Stub: one "준비 중" line.
 */
package com.example.lifegame.ui.screens

import androidx.compose.runtime.Composable
import com.example.lifegame.ui.components.MotionStyle

/** Group 7 · 해 · 시기 전환 (ageStamp · periodTransition). Stub. */
@Composable
internal fun TimeMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    CompareNote("준비 중")
}
