package com.example.lifegame.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.view.HapticFeedbackConstants
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * MotionKit — the shared motion vocabulary of stage 2 (2026-11-26).
 *
 * Owner: the integrator. Written before the four areas (play · time · lobby2 · bookends) branch, then frozen
 * while they work in parallel. Areas use these helpers instead of declaring their own copies, so the merged
 * build never sees two `stepProgress`s or two `RiseEasing`s in one package. A missing helper is requested from
 * the integrator; meanwhile an area keeps its own copy `private` (or area-prefixed) and swaps after the merge.
 *
 * Rules every helper here follows (and every effect built on them must follow):
 * - Presentation only: nothing here clicks, gates input, calls back, navigates or touches the game's random stream.
 * - Timing runs on the frame clock (Animatable, tween delayMillis, [awaitFrameMillis]); never a wall-clock delay()
 *   before visible state, so Compose tests that pause or fast-forward the clock see a consistent sequence.
 * - Animated values are read in draw / layout lambdas (graphicsLayer, drawWithContent), never in composition.
 * - Under reduced motion ([rememberReduceMotion]) every effect rests in its final static state.
 */

// ---------------------------------------------------------------------------------------------------------
// Curves
// ---------------------------------------------------------------------------------------------------------

/**
 * A cubic-bézier easing that never throws. Compose 1.7's CubicBezierEasing fails to solve some curves for a fraction a
 * hair below 1 ("The cubic curve … has no solution at 0.99999994"): RiseEasing and the title-unlock flip did, and the
 * exception took the app down while the 연출 비교 lobby preview replayed its entrance. The ends are exact, and a failed
 * solve falls back to the fraction itself (the curve is at its end value there anyway). Every curve we define uses this.
 */
internal fun safeCubicBezierEasing(a: Float, b: Float, c: Float, d: Float): Easing {
    val curve = CubicBezierEasing(a, b, c, d)
    return Easing { fraction ->
        when {
            fraction <= 0f -> 0f
            fraction >= 1f -> 1f
            else -> try {
                curve.transform(fraction)
            } catch (e: IllegalArgumentException) {
                fraction
            }
        }
    }
}

/** The lobby entrance curve the user picked in stage 1: a quick lift that settles softly (no overshoot). */
internal val RiseEasing: Easing = safeCubicBezierEasing(0.18f, 0.9f, 0.24f, 1f)

/**
 * Back-out (Penner, overshoot constant 1.70158): passes the target by about 10% near 58% of the way, then
 * settles. Exactly 0 at 0 and 1 at 1. Note: this deliberately shadows Compose's own cubic-Bezier
 * `androidx.compose.animation.core.EaseOutBack` inside this package; other packages import this one explicitly.
 */
internal val EaseOutBack: Easing = Easing { t ->
    val c1 = 1.70158f
    val c3 = c1 + 1f
    val u = t - 1f
    1f + c3 * u * u * u + c1 * u * u
}

/**
 * The glyph-stamp vocabulary of the lobby title (stage 1 StampedTitle), shared so the lobby title, the year
 * stamp, the profile title and the certificate stamps feel like one hand: each glyph drops in [SCALE_BOOST]
 * larger, tilted ([TILT_EVEN] / [TILT_ODD]), and lands on [spring] around [Pivot].
 */
internal object StampSpec {
    const val DAMPING = 0.48f
    const val STIFFNESS = Spring.StiffnessMediumLow
    const val SCALE_BOOST = 0.9f
    const val TILT_EVEN = -9f
    const val TILT_ODD = 7f

    /** Glyph pivot: horizontally centred, a little below the middle (the baseline side), like a hand stamp. */
    val Pivot = TransformOrigin(0.5f, 0.62f)

    /** The landing spring (0.48 · StiffnessMediumLow). */
    fun <T> spring(visibilityThreshold: T? = null): SpringSpec<T> =
        androidx.compose.animation.core.spring(dampingRatio = DAMPING, stiffness = STIFFNESS, visibilityThreshold = visibilityThreshold)

    /** Scale of a glyph at landing progress [progress] (0 = just dropped, 1 = landed). */
    fun scaleAt(progress: Float): Float = 1f + (1f - progress) * SCALE_BOOST

    /** Tilt in degrees of glyph [index] at [progress]; even glyphs lean left, odd ones right. */
    fun tiltAt(index: Int, progress: Float): Float = (1f - progress) * if (index % 2 == 0) TILT_EVEN else TILT_ODD

    /** Opacity of a glyph at [progress]: fully inked by a third of the way. */
    fun alphaAt(progress: Float): Float = (progress * 3f).coerceIn(0f, 1f)
}

/**
 * Draws [block] with the stamp's scale and tilt for glyph [index] at [progress], around [pivot] (for a glyph box
 * use `Offset(box.left + box.width * StampSpec.Pivot.pivotFractionX, box.top + box.height * StampSpec.Pivot.pivotFractionY)`).
 * Opacity stays with the caller ([StampSpec.alphaAt]). Draw phase only: no nodes, no semantics.
 */
internal inline fun DrawScope.withStampTransform(progress: Float, index: Int, pivot: Offset, block: DrawScope.() -> Unit) {
    val glyphScale = StampSpec.scaleAt(progress)
    val tilt = StampSpec.tiltAt(index, progress)
    withTransform({
        rotate(tilt, pivot)
        scale(glyphScale, glyphScale, pivot)
    }, block)
}

// ---------------------------------------------------------------------------------------------------------
// Cross-area timeline
// ---------------------------------------------------------------------------------------------------------

/** Timing contracts between areas. Values only an area itself reads stay in that area's files. */
internal object MotionTimeline {
    /** bookends: the whole certificate entrance (card, body, seal, title reveal) has ended by this point. */
    const val CERTIFICATE_REVEAL_MS = 1_000

    /**
     * lobby2: the title-unlock popup entrance starts this long after the certificate's first frame
     * (frame-driven: tween delayMillis, never delay()). Under reduced motion or touch exploration it appears at once.
     */
    const val TITLE_UNLOCK_START_MS = 1_100

    /** bookends: every recap (주마등) moment animation ends by this point; RecapView advances every 2.8 s. */
    const val RECAP_MOTION_MAX_MS = 1_100

    /** time: while PeriodTrack(stampYield = true) a running year stamp absorbs or fades within this time. */
    const val STAMP_YIELD_MS = 200

    /** Two motion haptics never fire closer than this. The click tick of rememberExperienceClick is exempt. */
    const val HAPTIC_MIN_GAP_MS = 120
}

// ---------------------------------------------------------------------------------------------------------
// Pure progress helpers (unit-tested in MotionKitTest)
// ---------------------------------------------------------------------------------------------------------

/**
 * Progress of one step of a sequence on a shared clock: 0 before [startMs], [easing] of the linear fraction
 * while running, exactly `easing(1)` (1 for every curve here) from `startMs + durationMs` on.
 * A non-positive [durationMs] is a jump at [startMs].
 */
internal fun stepProgress(clockMs: Float, startMs: Float, durationMs: Float, easing: Easing = LinearEasing): Float {
    if (durationMs <= 0f) return if (clockMs >= startMs) 1f else 0f
    val fraction = ((clockMs - startMs) / durationMs).coerceIn(0f, 1f)
    return easing.transform(fraction)
}

/**
 * A deterministic damped shake offset (no Random): `amplitude · e^(−t/decay) · sin(2πt/period)`.
 * 0 at t = 0 and for t < 0, and the envelope shrinks to 1% by t = 4.6 · [decayMs].
 */
internal fun dampedShake(tMs: Float, amplitude: Float, periodMs: Float, decayMs: Float): Float {
    if (tMs <= 0f || periodMs <= 0f || decayMs <= 0f) return 0f
    return amplitude * exp(-tMs / decayMs) * sin(2f * PI.toFloat() * tMs / periodMs)
}

// ---------------------------------------------------------------------------------------------------------
// Draw-phase modifiers
// ---------------------------------------------------------------------------------------------------------

/**
 * Fades an element in while it rises [rise] into place, on a one-shot clock ([rememberOneShotClock]):
 * `p = stepProgress(clock, startMs, durationMs, easing)`, alpha = p, translationY = (1 − p) · rise.
 * - graphicsLayer moves hit testing with the pixels: keep `rise = 0.dp` for interactive elements, or move them
 *   with [drawOffset] instead.
 * - [rest] pins the resting state (alpha 1, no offset) whatever the clock says (e.g. a group that holds a GL stage).
 * - [groupOpacity] fades the element as one flattened layer (layered art). Otherwise alpha is applied per draw
 *   (ModulateAlpha), which does not clip what the element draws outside its bounds during the fade.
 * The clock is read only inside the layer block.
 */
internal fun Modifier.riseIn(
    clock: State<Float>,
    startMs: Float,
    durationMs: Float,
    rise: Dp,
    rest: Boolean = false,
    easing: Easing = RiseEasing,
    groupOpacity: Boolean = false,
): Modifier = graphicsLayer {
    val p = if (rest) 1f else stepProgress(clock.value, startMs, durationMs, easing)
    alpha = p.coerceIn(0f, 1f)
    translationY = (1f - p) * rise.toPx()
    compositingStrategy = if (groupOpacity) CompositingStrategy.Auto else CompositingStrategy.ModulateAlpha
}

/**
 * Moves only the drawing by [offset] (read in the draw phase). Layout, semantics bounds, pointer hit testing and
 * BringIntoView targets all stay at the resting position, so a card can glide in without moving its tap target.
 */
internal fun Modifier.drawOffset(offset: DrawScope.() -> Offset): Modifier = drawWithContent {
    val shift = offset()
    if (shift == Offset.Zero) {
        drawContent()
    } else {
        translate(shift.x, shift.y) { this@drawWithContent.drawContent() }
    }
}

/**
 * A soft halo around a rounded rect in the ctaGlow look: [layers] rounded rects growing out to [reach] px, with
 * [alpha] (0..1, multiplied by the colour's own alpha) split evenly between them. Draw it behind the content
 * (drawBehind, before any clip) so it can spill past the bounds. Nothing is drawn at alpha 0.
 */
internal fun DrawScope.drawRimGlow(
    color: Color,
    alpha: Float,
    reach: Float,
    corner: Float,
    layers: Int = 3,
    topLeft: Offset = Offset.Zero,
    size: Size = this.size,
) {
    if (alpha <= 0f || reach <= 0f || layers <= 0) return
    val layerColor = color.copy(alpha = (color.alpha * alpha / layers).coerceIn(0f, 1f))
    for (layer in layers downTo 1) {
        val grow = reach * layer / layers
        drawRoundRect(
            layerColor,
            topLeft = Offset(topLeft.x - grow, topLeft.y - grow),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(corner + grow),
        )
    }
}

// ---------------------------------------------------------------------------------------------------------
// Clocks
// ---------------------------------------------------------------------------------------------------------

/**
 * Waits on the frame clock, the same clock the animations run on. A wall-clock delay() keeps counting
 * while frames are paused (cold start behind the splash, a paused test clock), so a stagger built on it
 * collapses into everything starting at once.
 */
internal suspend fun awaitFrameMillis(millis: Long) {
    if (millis <= 0L) return
    val start = withFrameMillis { it }
    while (withFrameMillis { it } - start < millis) Unit
}

/**
 * One clock in milliseconds for a one-shot sequence (0 → [totalMs], linear), to be read only in draw / layout
 * lambdas together with [stepProgress].
 * - It starts at 0 in the very composition where [key] changes, so the final state never flashes for a frame.
 * - It rests at [totalMs] when [active] is false at that moment or under reduced motion, and never replays for the
 *   same key afterwards (turning [active] on later does nothing).
 * - It snaps to [totalMs] on the next frame when [active] turns false or reduced motion turns on mid-run
 *   ("settle, never restart").
 * Callers decide everything else through [active]: the style is not NONE, [rememberEntranceMotionAllowed] for
 * entrances, a rememberSaveable "already played" flag so rotation does not replay, and so on. It does not watch the
 * lifecycle: a destination is only STARTED during NavHost's cross-fade, which would cancel every entrance.
 */
@Composable
internal fun rememberOneShotClock(key: Any?, active: Boolean, totalMs: Int): State<Float> {
    val reduceMotion = rememberReduceMotion()
    val total = totalMs.coerceAtLeast(0).toFloat()
    val running = active && !reduceMotion
    val clock = remember(key) { Animatable(if (running) 0f else total) }
    LaunchedEffect(clock, running, total) {
        if (!running) {
            clock.snapTo(total)
        } else if (clock.value < total) {
            val remaining = (total - clock.value).roundToInt().coerceAtLeast(1)
            clock.animateTo(total, tween(remaining, easing = LinearEasing))
        }
    }
    return clock.asState()
}

// ---------------------------------------------------------------------------------------------------------
// Accessibility gates
// ---------------------------------------------------------------------------------------------------------

/**
 * Test override for [rememberTouchExploration] (null = ask the system), like LocalSystemAnimatorScale for
 * animations: device tests cannot turn TalkBack on, so they provide `true` here instead.
 */
internal val LocalTouchExploration = compositionLocalOf<Boolean?> { null }

/** Whether TalkBack-style touch exploration is on, observed live (an override from [LocalTouchExploration] wins). */
@Composable
internal fun rememberTouchExploration(): Boolean {
    val override = LocalTouchExploration.current
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        val added = manager?.addTouchExplorationStateChangeListener(listener) == true
        enabled = manager?.isTouchExplorationEnabled == true
        onDispose { if (added) manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return override ?: enabled
}

/**
 * Whether a one-shot entrance (something that starts transparent or away from its place) may animate: false under
 * reduced motion and under touch exploration, where a node announced while still at alpha 0 would be read but not
 * seen. Every one-shot entrance gates on this; loops and draw-only accents only need [rememberReduceMotion].
 */
@Composable
internal fun rememberEntranceMotionAllowed(): Boolean = !rememberReduceMotion() && !rememberTouchExploration()

// ---------------------------------------------------------------------------------------------------------
// Haptics
// ---------------------------------------------------------------------------------------------------------

/** Motion haptics, from lightest to heaviest. */
internal enum class MotionHaptic { TICK, CONTEXT, CONFIRM, REJECT, HEAVY }

/** The platform constant for [this] on API [sdkInt]: CONFIRM and REJECT exist from API 30, below that LONG_PRESS. */
@SuppressLint("InlinedApi") // Guarded by sdkInt.
internal fun MotionHaptic.feedbackConstant(sdkInt: Int): Int = when (this) {
    MotionHaptic.TICK -> HapticFeedbackConstants.CLOCK_TICK
    MotionHaptic.CONTEXT -> HapticFeedbackConstants.CONTEXT_CLICK
    MotionHaptic.CONFIRM -> if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
    MotionHaptic.REJECT -> if (sdkInt >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    MotionHaptic.HEAVY -> HapticFeedbackConstants.LONG_PRESS
}

/** The rule behind the app-wide gate: a motion haptic may fire when none fired in the last HAPTIC_MIN_GAP_MS. */
internal fun motionHapticAllowed(lastFiredAtMs: Long?, nowMs: Long): Boolean =
    lastFiredAtMs == null || nowMs - lastFiredAtMs >= MotionTimeline.HAPTIC_MIN_GAP_MS

/** One gate for the whole app, so two areas' effects landing together never buzz twice (main thread only). */
private object MotionHapticGate {
    private var lastFiredAtMs: Long? = null

    fun tryFire(nowMs: Long): Boolean {
        if (!motionHapticAllowed(lastFiredAtMs, nowMs)) return false
        lastFiredAtMs = nowMs
        return true
    }
}

/**
 * Haptics that carry meaning (a result's tier, an award): they follow only the '버튼 진동' setting
 * (ExperiencePreferences.hapticsEnabled), not reduced motion, because a vibration is not motion.
 * - No IGNORE_* flags, so the system and view haptic settings are respected; failures are swallowed (runCatching).
 * - CONFIRM / REJECT fall back to LONG_PRESS below API 30 (minSdk is 26).
 * - At most one motion haptic per [MotionTimeline.HAPTIC_MIN_GAP_MS] across the app; extra calls are dropped.
 * Fire it once per event from an effect or animation callback, never from composition.
 */
@Composable
internal fun rememberMotionHaptics(): (MotionHaptic) -> Unit {
    val view = LocalView.current
    val preferences by rememberUpdatedState(LocalExperiencePreferences.current)
    return remember(view) {
        { haptic: MotionHaptic ->
            if (preferences.hapticsEnabled && MotionHapticGate.tryFire(SystemClock.uptimeMillis())) {
                runCatching { view.performHapticFeedback(haptic.feedbackConstant(Build.VERSION.SDK_INT)) }
            }
        }
    }
}

/**
 * Decorative haptics tied to an animation beat (a seal landing, a stamp impact). Same as [rememberMotionHaptics],
 * and also silent under reduced motion. Call it only from the running animation when it reaches the beat, never
 * from a branch that snaps or rests: a beat that is not animated is not felt either.
 */
@Composable
internal fun rememberBeatHaptics(): (MotionHaptic) -> Unit {
    val haptics = rememberMotionHaptics()
    val reduceMotion by rememberUpdatedState(rememberReduceMotion())
    return remember(haptics) { { haptic: MotionHaptic -> if (!reduceMotion) haptics(haptic) } }
}
