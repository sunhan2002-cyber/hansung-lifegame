/**
 * Area: time (시기 전환 장 넘김). Integrator stub written before the stage-2 branches.
 *
 * Frozen contract: `@Composable internal fun PeriodTurnOverlay(clock: LifeClock?, modifier: Modifier = Modifier)`.
 * PlayHost composes it right after PlayPhaseScreen, inside the CompositionLocalProvider that also provides
 * LocalLifeClock, with `Modifier.fillMaxSize()`: above the new screen, below the AlertDialogs and the 300 ms
 * screen_change_tap_guard, and never in the finishing / resetting branches. The overlay must stay draw-only (no
 * semantics, no testTag, no pointer input), compose nothing while no turn runs, and compose nothing for a null clock.
 * Overloads (for example a demo overload for the comparison screen) may be added.
 *
 * Stub: composes nothing.
 */
package com.example.lifegame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The period turn drawn over PlayHost when the run moves into a new period. Stub: nothing. */
@Composable
internal fun PeriodTurnOverlay(clock: LifeClock?, modifier: Modifier = Modifier) {
    // Stub: no overlay until the time area fills this in.
}
