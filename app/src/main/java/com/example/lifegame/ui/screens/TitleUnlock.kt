/**
 * Area: lobby2 (칭호 획득 팝업). Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — EndingFlowScreen.kt (bookends) and LifeGameApp.kt (integrator) call these, so the signatures
 * stay exactly as they are (bodies change freely; overloads may be added):
 * - `data class TitleUnlockUi(val timesEarned: Int) { val firstTime: Boolean }` — PUBLIC, because the public
 *   EndingFlowScreen takes it.
 * - `@Stable internal class TitleUnlockState` exposing `val celebratesTitle: Boolean` — true exactly when the popup
 *   will show for this certificate; bookends rests its titleReveal while it is true.
 * - `@Composable internal fun rememberTitleUnlock(record, lives): TitleUnlockUi?` — LifeGameApp calls it only in
 *   PlayHost's ENDED branch (the live ending). Routes.RECORD never passes an unlock.
 * - `@Composable internal fun rememberTitleUnlockState(record, unlock, celebrate): TitleUnlockState` — the first
 *   line of CertificateView. `celebrate` is true only on the first certificate frame of a live ending (it turns false
 *   one frame later when CompletionSealHeader consumes it), so latch it.
 * - `internal fun Modifier.titleUnlockOutsideTouch(state)` — on the Box around the certificate's scroll Column.
 * - `internal fun Modifier.titleUnlockAnchor(state)` — passed as CompletionSealHeader's badgeModifier (flight target).
 * - `@Composable internal fun TitleUnlockOverlay(state, record, modifier)` — the Box's last child, with
 *   `Modifier.matchParentSize()`: it covers only the scroll area, never the certificate_replay / certificate_done bar.
 *
 * Timing contract: the popup entrance starts MotionTimeline.TITLE_UNLOCK_START_MS after the certificate's first frame
 * (frame clock), after the bookends certificate entrance has ended (MotionTimeline.CERTIFICATE_REVEAL_MS), and it
 * appears already settled when rememberEntranceMotionAllowed() is false.
 *
 * Stub: no unlock, no popup, no pointer handling — the certificate behaves exactly as before stage 2.
 */
package com.example.lifegame.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.ui.LifeRecordUi

/** How often the player has now earned this record's title, counting this run. */
data class TitleUnlockUi(val timesEarned: Int) {
    val firstTime: Boolean get() = timesEarned <= 1
}

/** The popup's presentation state for one certificate. Stub: never celebrates. */
@Stable
internal class TitleUnlockState {
    /** True exactly when the popup will show; the certificate's own title reveal rests meanwhile. */
    val celebratesTitle: Boolean get() = false
}

/** The unlock to celebrate when a live run ends, or null. Stub: null. */
@Composable
internal fun rememberTitleUnlock(record: LifeRecordUi, lives: List<CompletedLifeRecord>): TitleUnlockUi? = null

/** Stub: a state that never celebrates. */
@Composable
internal fun rememberTitleUnlockState(record: LifeRecordUi, unlock: TitleUnlockUi?, celebrate: Boolean): TitleUnlockState =
    remember { TitleUnlockState() }

/** Observe-only outside-touch dismissal on the certificate area. Stub: no-op. */
internal fun Modifier.titleUnlockOutsideTouch(state: TitleUnlockState): Modifier = this

/** Marks the certificate's header medal as the popup medal's landing spot. Stub: no-op. */
internal fun Modifier.titleUnlockAnchor(state: TitleUnlockState): Modifier = this

/** The window-less popup over the certificate's scroll area. Stub: composes nothing. */
@Composable
internal fun TitleUnlockOverlay(state: TitleUnlockState, record: LifeRecordUi, modifier: Modifier = Modifier) {
    // Stub: no popup until the lobby2 area fills this in.
}
