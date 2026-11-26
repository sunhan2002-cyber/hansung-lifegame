/**
 * Area: lobby2 (칭호 획득 팝업).
 *
 * When a live run's ending first reaches its certificate, the earned title arrives as a window-less popup over the
 * certificate's scroll area: the medal drops and spins in (DROP_SPIN), stamps down (STAMP) or flips over from its dark
 * back (FLIP_REVEAL), and its light then breathes with the lobby's main button. '간직하기', a touch outside the card or
 * (motion on) 4.2 s of rest send the medal flying into the certificate header's own medal, and the popup is gone.
 * Whether the title is new or earned again is counted from `completedLives` in the UI; nothing under domain/ or data/
 * and nothing in the ViewModel changes.
 *
 * Frozen contract — EndingFlowScreen.kt (bookends) and LifeGameApp.kt (integrator) call these; the signatures stay:
 * - `data class TitleUnlockUi(val timesEarned: Int) { val firstTime: Boolean }` — PUBLIC (the public EndingFlowScreen takes it).
 * - `@Stable internal class TitleUnlockState` exposing `val celebratesTitle: Boolean`: true exactly when this
 *   certificate celebrates its title with the popup. It is computed synchronously in the first composition from the
 *   latched `celebrate`, the unlock, `record.title` and the style (no effect, no one-frame flip), saved with the popup
 *   and constant afterwards, so bookends can rest its titleReveal on it from the first frame.
 * - `rememberTitleUnlock(record, lives)` — LifeGameApp calls it only in PlayHost's ENDED branch. Routes.RECORD and every
 *   direct EndingFlowScreen caller pass no unlock: no popup there.
 * - `rememberTitleUnlockState(record, unlock, celebrate)` — the first line of CertificateView. `celebrate` is true only on
 *   the first certificate frame of a live ending (CompletionSealHeader consumes it), so it is latched per runId.
 * - `Modifier.titleUnlockOutsideTouch(state)` — on the Box around the certificate's scroll Column. It only observes:
 *   a touch outside the card dismisses the popup and still reaches the certificate (it scrolls as usual).
 * - `Modifier.titleUnlockAnchor(state)` — CompletionSealHeader's badgeModifier: it only observes where the header medal
 *   is (the flight's landing spot); the badge itself is never moved, hidden or re-timed.
 * - `TitleUnlockOverlay(state, record, modifier)` — the Box's last child with matchParentSize(): it covers only the
 *   scroll area, never the certificate_replay / certificate_done bar, whose touches nothing here consumes.
 *
 * Timing: the entrance starts MotionTimeline.TITLE_UNLOCK_START_MS after the certificate's first frame (frame clock),
 * after the bookends certificate entrance has ended (CERTIFICATE_REVEAL_MS). Without entrance motion (reduced motion,
 * touch exploration) the popup is settled from the first frame and never collapses by itself; under reduced motion
 * '간직하기' removes it at once. A lifecycle pause during the entrance settles it ("settle, never restart"); recreation
 * restores a shown popup settled and a leaving one gone. Leaving the certificate within the pre-delay skips the popup.
 * No Dialog, Popup or BackHandler: one compose root, and back steps exactly as before.
 */
package com.example.lifegame.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.TitleUi
import com.example.lifegame.ui.components.HighlightStyle
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.RiseEasing
import com.example.lifegame.ui.components.StampSpec
import com.example.lifegame.ui.components.TitleMedallion
import com.example.lifegame.ui.components.TitleUnlockStyle
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.rememberHighlightPhase
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.components.rememberTouchExploration
import com.example.lifegame.ui.components.stepProgress
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameLobbyPaper
import com.example.lifegame.ui.theme.LifeGameNight
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** How often the player has now earned this record's title, counting this run. */
data class TitleUnlockUi(val timesEarned: Int) {
    val firstTime: Boolean get() = timesEarned <= 1
}

/** Test-only: the popup entrance's progress, 0 → 1 (1 = settled), on the `title_unlock` card. */
internal val TitleUnlockProgress = SemanticsPropertyKey<Float>("TitleUnlockProgress")

// ---------------------------------------------------------------------------------------------------------
// Rules (pure, unit-tested)
// ---------------------------------------------------------------------------------------------------------

/**
 * The unlock for [record]'s title: earned once more than in the *other* completed lives (this run may already be
 * among [lives] once its ending is saved). Null when the record has no title.
 */
internal fun titleUnlockFor(record: LifeRecordUi, lives: List<CompletedLifeRecord>): TitleUnlockUi? {
    val title = record.title ?: return null
    val before = lives.count { it.runId != record.runId && it.awardedTitle?.titleId == title.titleId }
    return TitleUnlockUi(timesEarned = before + 1)
}

/** The card's eyebrow: a first title, or how many times this one has now been earned. */
internal fun titleUnlockEyebrow(unlock: TitleUnlockUi): String =
    if (unlock.firstTime) "새 칭호를 받았어요" else "다시 받은 칭호 · ${unlock.timesEarned}번째"

// ---------------------------------------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------------------------------------

/** PENDING waits out the pre-delay; then ENTERING → SHOWING → LEAVING → DONE. Ineligible states start DONE. */
internal enum class TitleUnlockPhase { PENDING, ENTERING, SHOWING, LEAVING, DONE }

/**
 * The popup's presentation state for one certificate. Its clocks are read only in draw, layer and semantics blocks;
 * the layout handles are plain fields, read only in pointer and draw code.
 */
@Stable
internal class TitleUnlockState private constructor(
    /** True exactly when the popup will show for this certificate; the certificate's own title reveal rests meanwhile. */
    val celebratesTitle: Boolean,
    internal val style: TitleUnlockStyle,
    internal val timesEarned: Int,
    phase: TitleUnlockPhase,
) {
    internal var phase by mutableStateOf(phase)
    internal val entranceTotalMs: Int = TitleUnlockTimeline.totalMs(style)

    /** Entrance-local milliseconds: 0 when the card starts to appear, [entranceTotalMs] when it has settled. */
    internal val entrance = Animatable(if (phase == TitleUnlockPhase.PENDING) 0f else entranceTotalMs.toFloat())

    /** Leave-local milliseconds of the exit, 0 → [TitleUnlockTimeline.LEAVE_MS]. */
    internal val leave = Animatable(0f)

    /** Under reduced motion a dismissal removes the popup at once (no flight, no landing ring). */
    internal var instantDismiss = false

    internal var anchor: LayoutCoordinates? = null
    internal var touchArea: LayoutCoordinates? = null
    internal var overlay: LayoutCoordinates? = null
    internal var card: LayoutCoordinates? = null
    internal var stage: LayoutCoordinates? = null

    internal val unlock: TitleUnlockUi get() = TitleUnlockUi(timesEarned)

    /** '간직하기', a touch outside the card or the auto-collapse. Nothing happens before the card is up or once it leaves. */
    internal fun dismiss() {
        if (phase == TitleUnlockPhase.ENTERING || phase == TitleUnlockPhase.SHOWING) {
            phase = if (instantDismiss) TitleUnlockPhase.DONE else TitleUnlockPhase.LEAVING
        }
    }

    /** Settle, never restart: jump to the settled card. */
    internal suspend fun settle() {
        entrance.snapTo(entranceTotalMs.toFloat())
        phase = TitleUnlockPhase.SHOWING
    }

    /** Whether a touch at [position] (in the certificate area's coordinates) lands on the card. Unknown counts as on it. */
    internal fun cardContains(position: Offset): Boolean {
        val area = touchArea?.takeIf { it.isAttached } ?: return true
        val card = card?.takeIf { it.isAttached } ?: return true
        return area.localBoundingBoxOf(card, clipBounds = false).contains(position)
    }

    /** The popup medal's resting centre in overlay coordinates (the middle of its stage). */
    internal fun medalCentre(): Offset? {
        val overlay = overlay?.takeIf { it.isAttached } ?: return null
        val stage = stage?.takeIf { it.isAttached } ?: return null
        return overlay.localPositionOf(stage, Offset(stage.size.width / 2f, stage.size.height / 2f))
    }

    /** The header medal's centre in overlay coordinates, or null when it is detached or scrolled out of the region. */
    internal fun flightTarget(): Offset? {
        val overlay = overlay?.takeIf { it.isAttached } ?: return null
        val anchor = anchor?.takeIf { it.isAttached } ?: return null
        val centre = overlay.localPositionOf(anchor, Offset(anchor.size.width / 2f, anchor.size.height / 2f))
        val inside = centre.x in 0f..overlay.size.width.toFloat() && centre.y in 0f..overlay.size.height.toFloat()
        return centre.takeIf { inside }
    }

    companion object {
        fun create(eligible: Boolean, style: TitleUnlockStyle, timesEarned: Int, settled: Boolean) = TitleUnlockState(
            celebratesTitle = eligible,
            style = style,
            timesEarned = timesEarned,
            phase = when {
                !eligible -> TitleUnlockPhase.DONE
                settled -> TitleUnlockPhase.SHOWING
                else -> TitleUnlockPhase.PENDING
            },
        )

        /** Saved as plain ints. A popup that was waiting or showing comes back settled; one that was leaving is gone. */
        val Saver: Saver<TitleUnlockState, Any> = listSaver(
            save = { listOf(if (it.celebratesTitle) 1 else 0, it.style.ordinal, it.timesEarned, it.phase.ordinal) },
            restore = { saved ->
                val eligible = saved[0] == 1
                val phase = when (TitleUnlockPhase.entries[saved[3]]) {
                    TitleUnlockPhase.PENDING, TitleUnlockPhase.ENTERING, TitleUnlockPhase.SHOWING -> TitleUnlockPhase.SHOWING
                    TitleUnlockPhase.LEAVING, TitleUnlockPhase.DONE -> TitleUnlockPhase.DONE
                }
                TitleUnlockState(eligible, TitleUnlockStyle.entries[saved[1]], saved[2], if (eligible) phase else TitleUnlockPhase.DONE)
            },
        )
    }
}

/** The unlock to celebrate when a live run ends, or null without a title. */
@Composable
internal fun rememberTitleUnlock(record: LifeRecordUi, lives: List<CompletedLifeRecord>): TitleUnlockUi? =
    remember(record.runId, record.title, lives) { titleUnlockFor(record, lives) }

/**
 * The popup state of one certificate. Eligibility is latched per runId in the first composition (and saved):
 * `celebrate` turns false one frame later, and 주마등 다시 보기, archived records and recreation never re-trigger it.
 */
@Composable
internal fun rememberTitleUnlockState(record: LifeRecordUi, unlock: TitleUnlockUi?, celebrate: Boolean): TitleUnlockState {
    val style = LocalMotionStyle.current.titleUnlock
    val entranceAllowed = rememberEntranceMotionAllowed()
    val state = rememberSaveable(record.runId, saver = TitleUnlockState.Saver) {
        val eligible = celebrate && unlock != null && record.title != null && style != TitleUnlockStyle.NONE
        TitleUnlockState.create(eligible, style, unlock?.timesEarned ?: 1, settled = !entranceAllowed)
    }
    if (state.celebratesTitle) TitleUnlockDriver(state, entranceAllowed)
    return state
}

/** Runs the phases on the frame clock: pre-delay, entrance, the rest before auto-collapse, the exit. */
@Composable
private fun TitleUnlockDriver(state: TitleUnlockState, entranceAllowed: Boolean) {
    val reduceMotion = rememberReduceMotion()
    val touchExploration = rememberTouchExploration()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resumed = lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    SideEffect { state.instantDismiss = reduceMotion }
    val phase = state.phase
    LaunchedEffect(state, phase, entranceAllowed, reduceMotion, touchExploration, resumed) {
        when (phase) {
            TitleUnlockPhase.PENDING -> if (!entranceAllowed || !resumed) state.settle() else {
                awaitFrameMillis(MotionTimeline.TITLE_UNLOCK_START_MS.toLong())
                state.phase = TitleUnlockPhase.ENTERING
            }
            TitleUnlockPhase.ENTERING -> if (!entranceAllowed || !resumed) state.settle() else {
                val total = state.entranceTotalMs.toFloat()
                val remaining = (total - state.entrance.value).roundToInt()
                if (remaining > 0) state.entrance.animateTo(total, tween(remaining, easing = LinearEasing))
                state.phase = TitleUnlockPhase.SHOWING
            }
            // No auto-collapse when nothing would animate it away, nor while TalkBack may still be reading the card.
            TitleUnlockPhase.SHOWING -> if (!reduceMotion && !touchExploration && resumed) {
                awaitFrameMillis(TitleUnlockTimeline.AUTO_COLLAPSE_MS.toLong())
                state.dismiss()
            }
            TitleUnlockPhase.LEAVING -> if (reduceMotion) state.phase = TitleUnlockPhase.DONE else {
                state.entrance.snapTo(state.entranceTotalMs.toFloat())
                val remaining = (TitleUnlockTimeline.LEAVE_MS - state.leave.value).roundToInt()
                if (remaining > 0) state.leave.animateTo(TitleUnlockTimeline.LEAVE_MS.toFloat(), tween(remaining, easing = LinearEasing))
                state.phase = TitleUnlockPhase.DONE
            }
            TitleUnlockPhase.DONE -> Unit
        }
    }
}

/**
 * Observe-only outside-touch dismissal on the certificate area: a press outside the card dismisses the popup and is
 * never consumed, so the certificate still scrolls. A no-op for a certificate that does not celebrate its title.
 */
internal fun Modifier.titleUnlockOutsideTouch(state: TitleUnlockState): Modifier {
    if (!state.celebratesTitle) return this
    return this
        .onPlaced { state.touchArea = it }
        .pointerInput(state) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (!state.cardContains(down.position)) state.dismiss()
            }
        }
}

/** Marks the certificate's header medal as the popup medal's landing spot; it only observes where the medal is. */
internal fun Modifier.titleUnlockAnchor(state: TitleUnlockState): Modifier =
    if (!state.celebratesTitle) this else onPlaced { state.anchor = it }

// ---------------------------------------------------------------------------------------------------------
// Timeline (pure, unit-tested). Entrance-local ms: 0 = the card starts to appear.
// ---------------------------------------------------------------------------------------------------------

internal object TitleUnlockTimeline {
    const val AUTO_COLLAPSE_MS = 4_200
    const val TEXT_MS = 360
    const val HALO_IN_MS = 500
    const val FLIGHT_MS = 560
    const val LAND_FADE_MS = 120
    const val LAND_RING_MS = 320
    const val CARD_OUT_MS = 220
    const val SCRIM_OUT_MS = 400

    /** The whole exit: the flight, then the landing ring (the medal's 120 ms fade ends inside it). */
    const val LEAVE_MS = FLIGHT_MS + LAND_RING_MS

    /** Every entrance animation of [style] has ended by then (the card is settled). */
    fun totalMs(style: TitleUnlockStyle): Int = when (style) {
        TitleUnlockStyle.NONE -> 0
        TitleUnlockStyle.DROP_SPIN -> 1_360
        TitleUnlockStyle.STAMP -> 1_260
        TitleUnlockStyle.FLIP_REVEAL -> 1_700
    }

    /** The medal lands (DROP_SPIN, STAMP) or turns face up (FLIP_REVEAL): rings, glints and bursts start here. */
    fun impactMs(style: TitleUnlockStyle): Int = when (style) {
        TitleUnlockStyle.NONE -> 0
        TitleUnlockStyle.DROP_SPIN -> 540
        TitleUnlockStyle.STAMP -> 440
        TitleUnlockStyle.FLIP_REVEAL -> 1_000
    }

    fun haloStartMs(style: TitleUnlockStyle): Int = if (style == TitleUnlockStyle.STAMP) 700 else impactMs(style)

    /** Start of text [index]: 0 eyebrow, 1 title, 2 description, 3 the keep button. */
    fun textStartMs(style: TitleUnlockStyle, index: Int): Int = when (style) {
        TitleUnlockStyle.NONE -> 0
        TitleUnlockStyle.DROP_SPIN -> when (index) { 0 -> 600; 1 -> 700; 2 -> 800; else -> 1_000 }
        TitleUnlockStyle.STAMP -> when (index) { 0 -> 520; 1 -> 560; 2 -> 720; else -> 900 }
        TitleUnlockStyle.FLIP_REVEAL -> when (index) { 0 -> 1_000; 1 -> 1_050; 2 -> 1_150; else -> 1_300 }
    }
}

internal fun titleUnlockScrimIn(t: Float): Float = stepProgress(t, 0f, 240f)
internal fun titleUnlockCardIn(t: Float): Float = stepProgress(t, 0f, 260f, FastOutSlowInEasing)
internal fun titleUnlockCardScale(t: Float): Float = titleUnlockCardIn(t).let { if (it >= 1f) 1f else 0.92f + 0.08f * it }

internal fun titleUnlockTextIn(style: TitleUnlockStyle, index: Int, t: Float): Float =
    stepProgress(t, TitleUnlockTimeline.textStartMs(style, index).toFloat(), TitleUnlockTimeline.TEXT_MS.toFloat(), RiseEasing)

internal fun titleUnlockHaloIn(style: TitleUnlockStyle, t: Float): Float =
    if (style == TitleUnlockStyle.NONE) 0f
    else stepProgress(t, TitleUnlockTimeline.haloStartMs(style).toFloat(), TitleUnlockTimeline.HALO_IN_MS.toFloat())

internal fun titleUnlockFlight(leaveMs: Float): Float =
    stepProgress(leaveMs, 0f, TitleUnlockTimeline.FLIGHT_MS.toFloat(), FastOutSlowInEasing)

internal fun titleUnlockCardOut(leaveMs: Float): Float = stepProgress(leaveMs, 0f, TitleUnlockTimeline.CARD_OUT_MS.toFloat())
internal fun titleUnlockScrimOut(leaveMs: Float): Float = stepProgress(leaveMs, 0f, TitleUnlockTimeline.SCRIM_OUT_MS.toFloat())

internal fun titleUnlockLandFade(leaveMs: Float): Float =
    stepProgress(leaveMs, TitleUnlockTimeline.FLIGHT_MS.toFloat(), TitleUnlockTimeline.LAND_FADE_MS.toFloat())

internal fun titleUnlockLandRing(leaveMs: Float): Float =
    stepProgress(leaveMs, TitleUnlockTimeline.FLIGHT_MS.toFloat(), TitleUnlockTimeline.LAND_RING_MS.toFloat())

/**
 * Displacement of a Compose spring (mass 1) released from 1 toward 0 at rest, [ms] after release:
 * `e^(−ζωt) · (cos ω_d t + ζω / ω_d · sin ω_d t)` with ω = √stiffness. Evaluated on the entrance clock, so every
 * frame of the popup is a pure function of one clock.
 */
internal fun titleUnlockSpring(ms: Float, dampingRatio: Float, stiffness: Float): Float {
    if (ms <= 0f) return 1f
    val t = ms / 1000f
    val omega = sqrt(stiffness)
    if (dampingRatio >= 1f) return exp(-omega * t) * (1f + omega * t)
    val damped = omega * sqrt(1f - dampingRatio * dampingRatio)
    return exp(-dampingRatio * omega * t) * (cos(damped * t) + dampingRatio * omega / damped * sin(damped * t))
}

/** A piecewise curve through ([times], [values]) with one easing per segment; it holds its ends outside them. */
private class TitleUnlockKeyframes(private val times: FloatArray, private val values: FloatArray, private val easings: List<Easing>) {
    fun at(ms: Float): Float {
        if (ms <= times.first()) return values.first()
        for (i in 1 until times.size) {
            if (ms < times[i]) {
                val fraction = (ms - times[i - 1]) / (times[i] - times[i - 1])
                return values[i - 1] + (values[i] - values[i - 1]) * easings[i - 1].transform(fraction)
            }
        }
        return values.last()
    }
}

/** How the popup medal is posed at one moment of its entrance. */
@Immutable
internal data class TitleUnlockMedalPose(
    /** DROP_SPIN: 1 = one stage height above the region's top edge, 0 = in place. */
    val fall: Float = 0f,
    /** Vertical offset in dp, negative = up (the bounce). */
    val liftDp: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val pivotY: Float = 0.5f,
    val alpha: Float = 1f,
    /** FLIP_REVEAL: the dark back is up. */
    val backFace: Boolean = false,
) {
    companion object {
        val Rest = TitleUnlockMedalPose()
    }
}

private val TitleUnlockFallEasing = CubicBezierEasing(0.55f, 0.085f, 0.68f, 0.53f)
private val TitleUnlockSpinEasing = CubicBezierEasing(0.15f, 0.6f, 0.25f, 1f)
private val TitleUnlockStampDownEasing = CubicBezierEasing(0.6f, 0f, 0.9f, 0.4f)
private val TitleUnlockFlipEasing = CubicBezierEasing(0.34f, 1.4f, 0.64f, 1f)

/** One real bounce after the landing: up 12dp, down, up 4dp, down (dp, ms after the impact). */
private val TitleUnlockBounce = TitleUnlockKeyframes(
    floatArrayOf(0f, 140f, 280f, 360f, 440f), floatArrayOf(0f, -12f, 0f, -4f, 0f),
    listOf(LinearOutSlowInEasing, FastOutLinearInEasing, LinearOutSlowInEasing, FastOutLinearInEasing),
)

/** The landing wobble (degrees, ms after the impact). */
private val TitleUnlockWobble = TitleUnlockKeyframes(
    floatArrayOf(0f, 60f, 160f, 260f, 380f), floatArrayOf(0f, 7f, -5f, 2.5f, 0f),
    List(4) { FastOutSlowInEasing },
)

/** STAMP: the card's impact shake (dp, ms after the impact). */
private val TitleUnlockCardShake = TitleUnlockKeyframes(
    floatArrayOf(0f, 40f, 90f, 140f, 190f, 240f), floatArrayOf(0f, 4f, -3f, 2f, -1f, 0f),
    List(5) { FastOutSlowInEasing },
)

/** FLIP_REVEAL: the tremble before the flip (degrees, ms after 260 ms). */
private val TitleUnlockTremble = TitleUnlockKeyframes(
    FloatArray(7) { it * 520f / 6f }, floatArrayOf(0f, -3f, 3f, -3.5f, 3.5f, -2f, 0f),
    List(6) { FastOutSlowInEasing },
)

/** The medal's pose at entrance-local [t] ms; exactly [TitleUnlockMedalPose.Rest] once the entrance has settled. */
internal fun titleUnlockMedalPose(style: TitleUnlockStyle, t: Float): TitleUnlockMedalPose {
    if (t >= TitleUnlockTimeline.totalMs(style)) return TitleUnlockMedalPose.Rest
    return when (style) {
        TitleUnlockStyle.NONE -> TitleUnlockMedalPose.Rest
        TitleUnlockStyle.DROP_SPIN -> titleUnlockDropSpinPose(t)
        TitleUnlockStyle.STAMP -> titleUnlockStampPose(t)
        TitleUnlockStyle.FLIP_REVEAL -> titleUnlockFlipPose(t)
    }
}

private fun titleUnlockDropSpinPose(t: Float): TitleUnlockMedalPose {
    // From 120 ms it falls in 420 ms (ease-in, it gathers speed) while spinning −540° → 0° over 980 ms,
    // so it is still turning when it lands at 540 ms.
    val fall = 1f - stepProgress(t, 120f, 420f, TitleUnlockFallEasing)
    val spin = -540f * (1f - stepProgress(t, 120f, 980f, TitleUnlockSpinEasing))
    val since = t - TitleUnlockTimeline.impactMs(TitleUnlockStyle.DROP_SPIN)
    if (since < 0f) return TitleUnlockMedalPose(fall = fall, rotationY = spin)
    val squash = titleUnlockSpring(since, dampingRatio = 0.35f, stiffness = 500f)
    return TitleUnlockMedalPose(
        liftDp = TitleUnlockBounce.at(since),
        rotationY = spin,
        rotationZ = TitleUnlockWobble.at(since),
        scaleX = 1f + 0.10f * squash,
        scaleY = 1f - 0.16f * squash,
        pivotY = 0.92f,
    )
}

private fun titleUnlockStampPose(t: Float): TitleUnlockMedalPose {
    val pivot = StampSpec.Pivot.pivotFractionY
    val since = t - TitleUnlockTimeline.impactMs(TitleUnlockStyle.STAMP)
    if (since < 0f) {
        // Comes down from 2.3× (ease-in, like a stamp) onto the paper, slightly squashed at contact.
        val scale = 2.3f + (0.9f - 2.3f) * stepProgress(t, 180f, 260f, TitleUnlockStampDownEasing)
        return TitleUnlockMedalPose(rotationZ = -14f, scaleX = scale, scaleY = scale, pivotY = pivot, alpha = stepProgress(t, 180f, 100f))
    }
    // Lands on the lobby title's stamp spring: 0.9 → 1 and −14° → 0°.
    val spring = titleUnlockSpring(since, StampSpec.DAMPING, StampSpec.STIFFNESS)
    val scale = 1f - 0.1f * spring
    return TitleUnlockMedalPose(rotationZ = -14f * spring, scaleX = scale, scaleY = scale, pivotY = pivot)
}

/** FLIP_REVEAL's flip, 0 → 1 with a slight overshoot, from 780 ms. */
internal fun titleUnlockFlipProgress(t: Float): Float = stepProgress(t, 780f, 460f, TitleUnlockFlipEasing)

private fun titleUnlockFlipPose(t: Float): TitleUnlockMedalPose {
    val flip = titleUnlockFlipProgress(t)
    val tremble = if (t in 260f..780f) TitleUnlockTremble.at(t - 260f) else 0f
    val swell = 1f + 0.06f * stepProgress(t, 260f, 520f, FastOutLinearInEasing) * (1f - stepProgress(t, 780f, 460f, FastOutSlowInEasing))
    // The back turns 0° → 90°, then the front −90° → 0°: the front is never left mirrored.
    val rotationY = if (flip < 0.5f) flip * 180f else (flip - 1f) * 180f
    return TitleUnlockMedalPose(
        rotationY = rotationY, rotationZ = tremble, scaleX = swell, scaleY = swell,
        alpha = titleUnlockCardIn(t), backFace = flip < 0.5f,
    )
}

/** STAMP: the card shake in dp. */
internal fun titleUnlockCardShakeDp(style: TitleUnlockStyle, t: Float): Float =
    if (style != TitleUnlockStyle.STAMP || t >= TitleUnlockTimeline.totalMs(style)) 0f
    else TitleUnlockCardShake.at(t - TitleUnlockTimeline.impactMs(style))

/** STAMP: the title stamps in as one block on the stamp spring (progress may overshoot 1). */
private fun titleUnlockTitleStamp(t: Float): Float =
    if (t >= TitleUnlockTimeline.totalMs(TitleUnlockStyle.STAMP)) 1f
    else if (t < 560f) 0f
    else 1f - titleUnlockSpring(t - 560f, StampSpec.DAMPING, StampSpec.STIFFNESS)

// ---------------------------------------------------------------------------------------------------------
// Overlay
// ---------------------------------------------------------------------------------------------------------

private const val TITLE_UNLOCK_SCRIM_ALPHA = 0.55f
private val TitleUnlockEyebrowColor = Color(0xFF8E612D)
private val TitleUnlockDeepGold = Color(0xFFC79B47)
private val TitleUnlockGlintColor = Color(0xFFD6AF61)
private val TitleUnlockKeepInk = Color(0xFF253631)

/**
 * The window-less popup over the certificate's scroll area. The scrim has no pointer handling, so touches outside the
 * card fall through to the certificate; the card keeps touches to itself. Composes nothing for a certificate that does
 * not celebrate its title, before the entrance starts and after the popup is gone.
 */
@Composable
internal fun TitleUnlockOverlay(state: TitleUnlockState, record: LifeRecordUi, modifier: Modifier = Modifier) {
    val title = record.title ?: return
    val phase = state.phase
    if (!state.celebratesTitle || phase == TitleUnlockPhase.PENDING || phase == TitleUnlockPhase.DONE) return
    // The 2.6 s breath of the lobby's main button and the slow ambient turn; both rest under reduced motion.
    val breath = rememberHighlightPhase(HighlightStyle.PULSE)
    val ambient = rememberAmbientPhase()
    BoxWithConstraints(
        modifier
            .clipToBounds()
            .onPlaced { state.overlay = it }
            .drawWithContent {
                val scrim = TITLE_UNLOCK_SCRIM_ALPHA * titleUnlockScrimIn(state.entrance.value) * (1f - titleUnlockScrimOut(state.leave.value))
                if (scrim > 0f) drawRect(LifeGameNight.copy(alpha = scrim))
                drawContent()
                drawTitleUnlockLanding(state)
            },
        contentAlignment = Alignment.Center,
    ) {
        // A short region or very large text: a smaller medal stage, so the card still fits.
        val compact = maxHeight < 420.dp || LocalDensity.current.fontScale > 1.5f
        TitleUnlockCard(state, title, compact, maxHeight, leaving = phase == TitleUnlockPhase.LEAVING, breath, ambient)
    }
}

@Composable
private fun TitleUnlockCard(
    state: TitleUnlockState,
    title: TitleUi,
    compact: Boolean,
    regionHeight: Dp,
    leaving: Boolean,
    breath: State<Float>?,
    ambient: State<Float>,
) {
    val style = state.style
    val total = state.entranceTotalMs.toFloat()
    Column(
        Modifier
            .padding(12.dp)
            .widthIn(max = 320.dp)
            .fillMaxWidth()
            .heightIn(max = (regionHeight - 24.dp).coerceAtLeast(0.dp))
            .onPlaced { state.card = it }
            .drawWithContent {
                // Draw-only entrance scale and (STAMP) impact shake: the card's hit area never moves.
                val t = state.entrance.value
                val cardScale = titleUnlockCardScale(t)
                val shake = titleUnlockCardShakeDp(style, t).dp.toPx()
                if (cardScale == 1f && shake == 0f) {
                    drawContent()
                } else {
                    withTransform({
                        translate(left = shake)
                        scale(cardScale, cardScale, center)
                    }) { this@drawWithContent.drawContent() }
                }
            }
            .drawBehind {
                // The card's paper is drawn, not a layer alpha, so the medal can fly out of the fading card.
                val out = titleUnlockCardOut(state.leave.value)
                val alpha = titleUnlockCardIn(state.entrance.value) * (1f - out)
                if (alpha > 0f) scale(1f - 0.04f * out) {
                    val corner = CornerRadius(16.dp.toPx())
                    drawRoundRect(LifeGameCard.copy(alpha = alpha), cornerRadius = corner)
                    drawRoundRect(LifeGameRule.copy(alpha = alpha), cornerRadius = corner, style = Stroke(1.dp.toPx()))
                }
            }
            // The card keeps its touches from the certificate underneath (a lower sibling is never hit-tested).
            .then(if (leaving) Modifier else Modifier.pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false) } })
            .semantics(mergeDescendants = true) {
                liveRegion = LiveRegionMode.Polite
                this[TitleUnlockProgress] = if (total <= 0f) 1f else (state.entrance.value / total).coerceIn(0f, 1f)
            }
            .testTag("title_unlock")
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TitleUnlockStage(state, if (compact) 96.dp else 132.dp, if (compact) 72.dp else 104.dp, breath, ambient)
        // Only the words scroll (large text); the medal stage stays unclipped so the medal can fly out. The scroll
        // spans the card's full width with the side padding inside it, so a stamping title has room before the clip.
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(4.dp))
            Text(titleUnlockEyebrow(state.unlock), color = TitleUnlockEyebrowColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp, textAlign = TextAlign.Center, modifier = Modifier.titleUnlockTextLayer(state, 0))
            Spacer(Modifier.height(6.dp))
            // Titles run to 14 glyphs and may wrap, so the title moves as one block.
            Text(title.text, color = LifeGameInk, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp,
                lineHeight = 29.sp, textAlign = TextAlign.Center, modifier = Modifier.titleUnlockTitleLayer(state))
            Spacer(Modifier.height(6.dp))
            Text(title.description, color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center,
                modifier = Modifier.titleUnlockTextLayer(state, 2))
            Spacer(Modifier.height(14.dp))
            // The button only fades, in place: a rising button would move its hit area with it.
            InkButton("간직하기", { state.dismiss() },
                Modifier.graphicsLayer {
                    alpha = titleUnlockTextIn(style, 3, state.entrance.value) * (1f - titleUnlockCardOut(state.leave.value))
                }.testTag("title_unlock_keep"),
                compact = true, containerColor = LifeGameGold, contentColor = TitleUnlockKeepInk)
        }
    }
}

/** Fades text [index] in while it rises 10dp, and out with the card. */
private fun Modifier.titleUnlockTextLayer(state: TitleUnlockState, index: Int): Modifier = graphicsLayer {
    val p = titleUnlockTextIn(state.style, index, state.entrance.value)
    alpha = p * (1f - titleUnlockCardOut(state.leave.value))
    translationY = (1f - p) * 10.dp.toPx()
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

/** The title: stamped as one block (STAMP), swelling in (FLIP_REVEAL), or rising like the other texts. */
private fun Modifier.titleUnlockTitleLayer(state: TitleUnlockState): Modifier = graphicsLayer {
    val t = state.entrance.value
    val fade = 1f - titleUnlockCardOut(state.leave.value)
    when (state.style) {
        TitleUnlockStyle.STAMP -> {
            val p = titleUnlockTitleStamp(t)
            alpha = StampSpec.alphaAt(p) * fade
            // Up to 1.45×, but never wider than the card (the words' scroll clip spans it): a long title stays whole.
            val room = state.card?.size?.width?.toFloat() ?: size.width
            val scale = (1f + 0.45f * (1f - p)).coerceAtMost((room / size.width.coerceAtLeast(1f)).coerceAtLeast(1f))
            scaleX = scale
            scaleY = scale
            transformOrigin = StampSpec.Pivot
        }
        TitleUnlockStyle.FLIP_REVEAL -> {
            val p = titleUnlockTextIn(state.style, 1, t)
            alpha = p * fade
            val scale = 0.96f + 0.04f * p
            scaleX = scale
            scaleY = scale
        }
        else -> {
            val p = titleUnlockTextIn(state.style, 1, t)
            alpha = p * fade
            translationY = (1f - p) * 10.dp.toPx()
        }
    }
    compositingStrategy = CompositingStrategy.ModulateAlpha
}

/** The medal on its stage, with the halo, rays, ink and one-shot bursts drawn around it. */
@Composable
private fun TitleUnlockStage(state: TitleUnlockState, stageSize: Dp, medalSize: Dp, breath: State<Float>?, ambient: State<Float>) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(stageSize)
            .onPlaced { state.stage = it }
            .drawWithCache {
                val medal = medalSize.toPx()
                // TitleMedallion draws its disc (radius 0.31 of its size) at 44 % of its height.
                val disc = medal * 0.31f
                val centre = Offset(size.width / 2f, size.height / 2f - medal * 0.06f)
                val halo = disc * 2f
                val unitGlow = Brush.radialGradient(listOf(LifeGameGold, LifeGameGold.copy(alpha = 0f)), center = Offset.Zero, radius = 1f)
                val unitInk = Brush.radialGradient(
                    0f to LifeGameGold.copy(alpha = 0.55f), 0.7f to LifeGameGold.copy(alpha = 0.44f), 1f to LifeGameGold.copy(alpha = 0f),
                    center = Offset.Zero, radius = 1f,
                )
                val rayBrush = Brush.radialGradient(
                    0f to LifeGameGold, 0.55f to LifeGameGold, 1f to LifeGameGold.copy(alpha = 0f),
                    center = centre, radius = halo * 1.45f,
                )
                val shortRay = titleUnlockWedge(centre, halo * 1.25f)
                val longRay = titleUnlockWedge(centre, halo * 1.45f)
                onDrawWithContent {
                    val t = state.entrance.value
                    val fade = 1f - titleUnlockCardOut(state.leave.value)
                    val b = breath?.let { (sin(it.value * 2f * PI.toFloat()) + 1f) / 2f } ?: 0.5f
                    val style = state.style
                    if (style == TitleUnlockStyle.STAMP) drawTitleUnlockInk(t, fade, centre, disc, unitInk)
                    val haloIn = titleUnlockHaloIn(style, t) * fade
                    if (haloIn > 0f) {
                        if (style != TitleUnlockStyle.STAMP) {
                            // Twelve soft rays repeat every 30°, so turning 30° per ambient loop is seamless.
                            for (ray in 0 until 12) {
                                rotate(ray * 30f + 30f * ambient.value, centre) {
                                    drawPath(if (ray % 2 == 0) shortRay else longRay, rayBrush, alpha = 0.16f * haloIn)
                                }
                            }
                        }
                        val radius = halo + 10.dp.toPx() * b
                        translate(centre.x, centre.y) {
                            scale(radius, radius, Offset.Zero) {
                                drawCircle(unitGlow, radius = 1f, center = Offset.Zero, alpha = (0.30f - 0.14f * b) * haloIn)
                            }
                        }
                    }
                    drawTitleUnlockBursts(style, t, fade, centre, disc, halo)
                    drawContent()
                    drawTitleUnlockGlints(style, t, fade, centre, medal)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        TitleUnlockMedal(state, medalSize)
    }
}

/** A 9° wedge from [centre] pointing up, [length] long; rotated copies make the rays. */
private fun titleUnlockWedge(centre: Offset, length: Float): Path {
    val half = (4.5f * PI / 180f).toFloat()
    return Path().apply {
        moveTo(centre.x, centre.y)
        lineTo(centre.x + sin(-half) * length, centre.y - cos(half) * length)
        lineTo(centre.x + sin(half) * length, centre.y - cos(half) * length)
        close()
    }
}

/** STAMP: gold ink spreading from under the medal, settling into a faint imprint (alpha 1 → 0.22). */
private fun DrawScope.drawTitleUnlockInk(t: Float, fade: Float, centre: Offset, disc: Float, unitInk: Brush) {
    val impact = TitleUnlockTimeline.impactMs(TitleUnlockStyle.STAMP).toFloat()
    if (t < impact) return
    val ink = stepProgress(t, impact, 620f)
    val radius = disc * (0.7f + 1.1f * FastOutSlowInEasing.transform(ink))
    val alpha = (1f - 0.78f * ink) * fade
    if (alpha <= 0f) return
    translate(centre.x, centre.y) {
        scale(radius, radius, Offset.Zero) { drawCircle(unitInk, radius = 1f, center = Offset.Zero, alpha = alpha) }
    }
}

/** One-shot accents behind the medal: DROP_SPIN's landing ring, FLIP_REVEAL's burst rays and pixel sparkles. */
private fun DrawScope.drawTitleUnlockBursts(style: TitleUnlockStyle, t: Float, fade: Float, centre: Offset, disc: Float, halo: Float) {
    val impact = TitleUnlockTimeline.impactMs(style).toFloat()
    when (style) {
        TitleUnlockStyle.DROP_SPIN -> {
            val ring = stepProgress(t, impact, 600f)
            if (ring > 0f && ring < 1f) {
                drawCircle(LifeGameGold.copy(alpha = 0.7f * (1f - ring) * fade), disc * (1f + 0.9f * FastOutSlowInEasing.transform(ring)),
                    centre, style = Stroke(2.dp.toPx()))
            }
        }
        TitleUnlockStyle.FLIP_REVEAL -> {
            val rays = stepProgress(t, impact, 520f)
            if (rays > 0f && rays < 1f) {
                val outer = halo * (0.75f + 0.75f * FastOutSlowInEasing.transform(rays))
                val color = TitleUnlockDeepGold.copy(alpha = 0.5f * (1f - rays) * fade)
                for (ray in 0 until 10) {
                    val angle = (ray * 36f - 90f) * PI.toFloat() / 180f
                    val direction = Offset(cos(angle), sin(angle))
                    drawLine(color, centre + direction * (halo * 0.75f), centre + direction * outer, 2.dp.toPx(), StrokeCap.Round)
                }
            }
            val sparkle = stepProgress(t, impact, 700f)
            if (sparkle > 0f && sparkle < 1f) {
                val side = 2.dp.toPx()
                val drift = 14.dp.toPx() * sparkle * sparkle
                val distance = halo * (0.8f + 1.1f * FastOutSlowInEasing.transform(sparkle))
                for (i in 0 until 12) {
                    // Fixed angles, no Random.
                    val angle = (i * 30f + (i % 2) * 8f) * PI.toFloat() / 180f
                    val at = centre + Offset(cos(angle), sin(angle)) * distance + Offset(0f, drift)
                    val color = (if (i % 2 == 0) LifeGameGold else TitleUnlockDeepGold).copy(alpha = (1f - sparkle) * fade)
                    drawRect(color, Offset(at.x - side / 2f, at.y - side / 2f), Size(side, side))
                }
            }
        }
        else -> Unit
    }
}

/** Two cross glints beside the medal as it lands or turns face up (DROP_SPIN, FLIP_REVEAL). */
private fun DrawScope.drawTitleUnlockGlints(style: TitleUnlockStyle, t: Float, fade: Float, centre: Offset, medal: Float) {
    if (style != TitleUnlockStyle.DROP_SPIN && style != TitleUnlockStyle.FLIP_REVEAL) return
    val glint = stepProgress(t, TitleUnlockTimeline.impactMs(style).toFloat(), 600f)
    if (glint <= 0f || glint >= 1f) return
    val color = TitleUnlockGlintColor.copy(alpha = sin(PI.toFloat() * glint) * fade)
    val arm = medal * 0.06f
    val stroke = medal * 0.022f
    for (side in listOf(-1f, 1f)) {
        val point = centre + Offset(side * medal * 0.42f, -medal * 0.21f)
        drawLine(color, point - Offset(arm, 0f), point + Offset(arm, 0f), stroke, StrokeCap.Round)
        drawLine(color, point - Offset(0f, arm), point + Offset(0f, arm), stroke, StrokeCap.Round)
    }
}

/** The landing ring at the header medal as the flying medal arrives (28dp → 44dp, alpha 0.6 → 0). */
private fun DrawScope.drawTitleUnlockLanding(state: TitleUnlockState) {
    if (state.phase != TitleUnlockPhase.LEAVING) return
    val ring = titleUnlockLandRing(state.leave.value)
    if (ring <= 0f || ring >= 1f) return
    val target = state.flightTarget() ?: return
    val radius = 28.dp.toPx() + (44.dp.toPx() - 28.dp.toPx()) * ring
    drawCircle(TitleUnlockDeepGold.copy(alpha = 0.6f * (1f - ring)), radius, target, style = Stroke(2.dp.toPx()))
}

/**
 * The popup medal: the real TitleMedallion (bookends owns its signature; only `progress` is passed, by name). Its layer
 * poses it for the entrance, then flies it into the header medal. FLIP_REVEAL also draws the dark back while it is up.
 */
@Composable
private fun TitleUnlockMedal(state: TitleUnlockState, medalSize: Dp) {
    val flips = state.style == TitleUnlockStyle.FLIP_REVEAL
    Box(Modifier.size(medalSize).graphicsLayer {
        cameraDistance = 12f * density
        if (state.phase == TitleUnlockPhase.LEAVING) titleUnlockFlightLayer(state) else titleUnlockPoseLayer(state)
    }) {
        if (flips) {
            Spacer(Modifier.matchParentSize().drawWithCache {
                val edge = size.minDimension
                val centre = Offset(size.width / 2f, size.height * .44f)
                val radius = edge * .31f
                val leftRibbon = titleUnlockRibbon(centre, edge, -1f)
                val rightRibbon = titleUnlockRibbon(centre, edge, 1f)
                val star = titleUnlockStar(centre, radius)
                onDrawBehind {
                    if (!titleUnlockShowsBack(state)) return@onDrawBehind
                    drawPath(leftRibbon, Color(0xFF1E2B27))
                    drawPath(rightRibbon, Color(0xFF16211E))
                    drawCircle(Color.Black.copy(alpha = .13f), radius + edge * .025f, centre + Offset(0f, edge * .045f))
                    drawCircle(Color(0xFF253631), radius, centre)
                    drawCircle(Color(0xFF1C2925), radius * .86f, centre)
                    drawCircle(LifeGameLobbyPaper.copy(alpha = .18f), radius * .83f, centre, style = Stroke(edge * .014f))
                    drawPath(star, LifeGameLobbyPaper.copy(alpha = .25f), style = Stroke(edge * .014f))
                }
            })
        }
        TitleMedallion(
            Modifier.matchParentSize().graphicsLayer { alpha = if (flips && titleUnlockShowsBack(state)) 0f else 1f },
            progress = { 1f },
        )
    }
}

private fun titleUnlockShowsBack(state: TitleUnlockState): Boolean =
    state.style == TitleUnlockStyle.FLIP_REVEAL && state.phase != TitleUnlockPhase.LEAVING &&
        titleUnlockFlipProgress(state.entrance.value) < 0.5f

private fun GraphicsLayerScope.titleUnlockPoseLayer(state: TitleUnlockState) {
    val pose = titleUnlockMedalPose(state.style, state.entrance.value)
    var dy = pose.liftDp.dp.toPx()
    var hidden = false
    if (pose.fall > 0f) {
        // From one stage height above the region's top edge (clipped there) down into place.
        val centre = state.medalCentre()
        val stageHeight = state.stage?.size?.height?.toFloat()
        if (centre == null || stageHeight == null) hidden = true else dy -= pose.fall * (centre.y + stageHeight)
    }
    translationY = dy
    rotationY = pose.rotationY
    rotationZ = pose.rotationZ
    scaleX = pose.scaleX
    scaleY = pose.scaleY
    alpha = if (hidden) 0f else pose.alpha
    transformOrigin = TransformOrigin(0.5f, pose.pivotY)
}

/**
 * The exit: the medal arcs (36dp up at mid-flight) into the header medal, shrinking to its size, then fades over it.
 * Without a landing spot (header detached or scrolled away) it shrinks to 0.6 and fades in place.
 */
private fun GraphicsLayerScope.titleUnlockFlightLayer(state: TitleUnlockState) {
    val leave = state.leave.value
    val flight = titleUnlockFlight(leave)
    transformOrigin = TransformOrigin.Center
    val from = state.medalCentre()
    val target = state.flightTarget()
    val anchorWidth = state.anchor?.size?.width?.toFloat()
    if (from != null && target != null && anchorWidth != null && size.width > 0f) {
        val arc = sin(PI.toFloat() * flight)
        translationX = (target.x - from.x) * flight
        translationY = (target.y - from.y) * flight - arc * 36.dp.toPx()
        val scale = 1f + (anchorWidth / size.width - 1f) * flight
        scaleX = scale
        scaleY = scale
        rotationZ = -10f * arc
        alpha = 1f - titleUnlockLandFade(leave)
    } else {
        val scale = 1f - 0.4f * flight
        scaleX = scale
        scaleY = scale
        alpha = 1f - flight
    }
}

/** TitleMedallion's ribbon geometry, so the dark back's silhouette matches the front. */
private fun titleUnlockRibbon(centre: Offset, edge: Float, side: Float) = Path().apply {
    moveTo(centre.x + side * edge * .04f, centre.y + edge * .15f)
    lineTo(centre.x + side * edge * .23f, centre.y + edge * .11f)
    lineTo(centre.x + side * edge * .27f, centre.y + edge * .48f)
    lineTo(centre.x + side * edge * .15f, centre.y + edge * .39f)
    lineTo(centre.x + side * edge * .04f, centre.y + edge * .46f)
    close()
}

/** TitleMedallion's five-point star. */
private fun titleUnlockStar(centre: Offset, radius: Float) = Path().apply {
    repeat(10) { index ->
        val angle = -PI / 2 + index * PI / 5
        val r = radius * if (index % 2 == 0) .55f else .25f
        val x = centre.x + cos(angle).toFloat() * r
        val y = centre.y + sin(angle).toFloat() * r
        if (index == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
