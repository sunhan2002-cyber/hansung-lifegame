package com.example.lifegame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The material ripple for the classic styles. The ink style, and every style under reduced motion,
 * draws its own (static when reduced) feedback via [inkSplash] instead of the animated ripple.
 */
@Composable
internal fun pressIndication(): Indication? =
    if (usesInkFeedback()) null else LocalIndication.current

@Composable
private fun usesInkFeedback(): Boolean =
    LocalMotionStyle.current.press == PressStyle.BOUNCE_INK || rememberReduceMotion()

/** Under reduced motion a quick tap still shows the static tint for at least this long. */
private const val REDUCED_PRESS_HOLD_MILLIS = 120L

/**
 * Ink spreading from the touch point, drawn above the background and below the label.
 * Presentation only: the caller's clickable stays the single source of clicks and semantics.
 * Focus and hover keep a visible tint because this replaces the ripple indication.
 */
@Composable
internal fun Modifier.inkSplash(interactions: InteractionSource, color: Color, enabled: Boolean = true): Modifier {
    if (!usesInkFeedback()) return this
    val reduceMotion = rememberReduceMotion()
    val spread = remember { Animatable(0f) }
    val strength = remember { Animatable(0f) }
    var origin by remember { mutableStateOf(Offset.Unspecified) }
    var focused by remember { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }
    LaunchedEffect(interactions, reduceMotion) {
        var reducedRelease: Job? = null
        interactions.interactions.collect { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    origin = interaction.pressPosition
                    reducedRelease?.cancel()
                    launch {
                        spread.snapTo(if (reduceMotion) 1f else 0f)
                        if (!reduceMotion) spread.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
                    }
                    launch { strength.snapTo(1f) }
                }
                is PressInteraction.Release, is PressInteraction.Cancel ->
                    if (reduceMotion) {
                        // A wall-clock hold, not an AnimationSpec: animator scale 0 would cut a spec to nothing.
                        reducedRelease = launch {
                            delay(REDUCED_PRESS_HOLD_MILLIS)
                            strength.snapTo(0f)
                        }
                    } else {
                        launch { strength.animateTo(0f, tween(460, delayMillis = 60)) }
                    }
                is FocusInteraction.Focus -> focused = true
                is FocusInteraction.Unfocus -> focused = false
                is HoverInteraction.Enter -> hovered = true
                is HoverInteraction.Exit -> hovered = false
            }
        }
    }
    return drawWithContent {
        if (enabled && focused) drawRect(color.copy(alpha = 0.12f))
        else if (enabled && hovered) drawRect(color.copy(alpha = 0.08f))
        val alpha = strength.value
        if (enabled && alpha > 0f) {
            val center = if (origin.isSpecified) origin else Offset(size.width / 2f, size.height / 2f)
            val far = max(
                max(hypot(center.x, center.y), hypot(size.width - center.x, center.y)),
                max(hypot(center.x, size.height - center.y), hypot(size.width - center.x, size.height - center.y)),
            )
            val radius = far * (0.12f + 0.88f * spread.value) + 1f
            val ink = color.copy(alpha = color.alpha * alpha)
            // A dense core and a softer bleeding edge read as ink soaking into paper.
            drawCircle(
                Brush.radialGradient(
                    0f to ink, 0.7f to ink.copy(alpha = ink.alpha * 0.8f), 1f to Color.Transparent,
                    center = center, radius = radius,
                ),
                radius = radius, center = center,
            )
        }
        drawContent()
    }
}

private val GlowStyles = setOf(HighlightStyle.SHINE_GLOW, HighlightStyle.PULSE, HighlightStyle.PULSE_SHINE)
private val ShineStyles = setOf(HighlightStyle.SHINE, HighlightStyle.SHINE_GLOW, HighlightStyle.PULSE_SHINE)

/** How long the shine band takes to cross the button (0.3 of SHINE's 3.8 s cycle). */
private const val SHINE_SWEEP_MS = 1_140f

/**
 * One highlight cycle. PULSE breathes every 2.6 s; SHINE sweeps once per 3.8 s; PULSE_SHINE keeps PULSE's breath and
 * lets the band pass every other breath (5.2 s), so the button stays calm between sweeps.
 */
private fun highlightPeriodMillis(style: HighlightStyle): Int = when (style) {
    HighlightStyle.PULSE -> 2_600
    HighlightStyle.PULSE_SHINE -> 5_200
    HighlightStyle.NONE, HighlightStyle.SHINE, HighlightStyle.SHINE_GLOW -> 3_800
}

/**
 * A looping 0..1 phase for the call-to-action highlight. Null (nothing drawn) for NONE and under
 * reduced motion; while the screen is not resumed (navigation transitions) it rests at phase 0, so the
 * halo stays visible instead of vanishing and popping back.
 */
@Composable
internal fun rememberHighlightPhase(style: HighlightStyle = LocalMotionStyle.current.highlight): State<Float>? {
    if (style == HighlightStyle.NONE || rememberReduceMotion()) return null
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val resting = remember { mutableFloatStateOf(0f) }
    if (lifecycleState != Lifecycle.State.RESUMED) return resting
    return rememberInfiniteTransition(label = "cta_highlight").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(highlightPeriodMillis(style), easing = LinearEasing),
            RepeatMode.Restart,
        ),
        label = "cta_highlight_phase",
    )
}

/** Soft halo outside the button. Place before the button's clip so it can draw past its bounds. */
internal fun Modifier.ctaGlow(phase: State<Float>?, style: HighlightStyle, color: Color, corner: Dp): Modifier {
    if (phase == null || style !in GlowStyles) return this
    // PULSE_SHINE's phase spans two breaths (so the shine passes every other breath): breathe twice per cycle.
    val breaths = if (style == HighlightStyle.PULSE_SHINE) 2f else 1f
    return drawBehind {
        val breath = (sin(phase.value * breaths * 2f * PI.toFloat()) + 1f) / 2f
        val pulse = style == HighlightStyle.PULSE || style == HighlightStyle.PULSE_SHINE
        val reach = (if (pulse) 3f + breath * 9f else 2f + breath * 4f).dp.toPx()
        val peak = if (pulse) 0.34f - breath * 0.2f else 0.16f + breath * 0.14f
        val layers = 4
        for (layer in layers downTo 1) {
            val grow = reach * layer / layers
            drawRoundRect(
                color.copy(alpha = peak / layers),
                topLeft = Offset(-grow, -grow),
                size = Size(size.width + grow * 2f, size.height + grow * 2f),
                cornerRadius = CornerRadius(corner.toPx() + grow),
            )
        }
    }
}

/** A diagonal band of light sweeping across the button. Place after the clip and background. */
internal fun Modifier.ctaShine(phase: State<Float>?, style: HighlightStyle): Modifier {
    if (phase == null || style !in ShineStyles) return this
    // The band always takes SHINE_SWEEP_MS to cross, whatever the cycle length.
    val window = SHINE_SWEEP_MS / highlightPeriodMillis(style)
    return drawWithContent {
        drawContent()
        val sweep = phase.value / window
        if (sweep < 1f) {
            val band = size.width * 0.16f
            val x = -band * 2f + (size.width + band * 4f) * FastOutSlowInEasing.transform(sweep)
            drawRect(
                Brush.linearGradient(
                    0f to Color.Transparent, 0.5f to Color.White.copy(alpha = 0.42f), 1f to Color.Transparent,
                    start = Offset(x - band, 0f), end = Offset(x + band, size.height),
                ),
            )
        }
    }
}
