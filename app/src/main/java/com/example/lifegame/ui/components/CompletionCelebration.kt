package com.example.lifegame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val CompletionEffectProgress = SemanticsPropertyKey<Float>("CompletionEffectProgress")
private const val COMPLETION_DURATION_MS = 720

/**
 * The one-shot reveal of a freshly completed record's certificate, shared by the header (gold sweep and medal) and the
 * certificate's own entrance (card, body, seal, title reveal), so they always play — or rest — together.
 * - [progress]: the header's 720 ms sweep, exposed as [CompletionEffectProgress] (0..1).
 * - [clockMs]: milliseconds into the certificate entrance, 0..[MotionTimeline.CERTIFICATE_REVEAL_MS] (linear); read
 *   it only while drawing.
 * Both rest at their ends whenever the reveal does not play: an archived record, 주마등 다시 보기, recreation, a
 * background pause, reduced motion. Under touch exploration the entrance ([clockMs]) rests too, because its card and
 * body start transparent and must be visible when they are announced; the header's sweep plays as before.
 * A fresh reveal starts at its beginning, so the final state never flashes.
 */
@Stable
internal class CompletionRevealState internal constructor(fresh: Boolean, freshEntrance: Boolean) {
    private val progressAnimatable = Animatable(if (fresh) 0f else 1f)
    private val clockAnimatable = Animatable(if (freshEntrance) 0f else COMPLETION_REVEAL_END_MS)

    val progress: Float get() = progressAnimatable.value
    val clockMs: State<Float> = clockAnimatable.asState()

    /** True only while the entrance is actually animating (never in a branch that rests or snaps). */
    val animating: Boolean get() = clockAnimatable.isRunning

    internal suspend fun play(entrance: Boolean) {
        progressAnimatable.snapTo(0f)
        if (!entrance) {
            clockAnimatable.snapTo(COMPLETION_REVEAL_END_MS)
            progressAnimatable.animateTo(1f, tween(COMPLETION_DURATION_MS, easing = FastOutSlowInEasing))
            return
        }
        clockAnimatable.snapTo(0f)
        coroutineScope {
            launch(start = CoroutineStart.UNDISPATCHED) {
                clockAnimatable.animateTo(COMPLETION_REVEAL_END_MS, tween(MotionTimeline.CERTIFICATE_REVEAL_MS, easing = LinearEasing))
            }
            progressAnimatable.animateTo(1f, tween(COMPLETION_DURATION_MS, easing = FastOutSlowInEasing))
        }
    }

    internal suspend fun settle() {
        progressAnimatable.snapTo(1f)
        clockAnimatable.snapTo(COMPLETION_REVEAL_END_MS)
    }

    /** Ends the entrance at once (a running one is cancelled); the header's sweep is left alone. */
    internal suspend fun settleEntrance() {
        clockAnimatable.snapTo(COMPLETION_REVEAL_END_MS)
    }
}

private val COMPLETION_REVEAL_END_MS = MotionTimeline.CERTIFICATE_REVEAL_MS.toFloat()

/**
 * The reveal gate: only a saved completed record that reaches its certificate live ([celebrate] at entry) plays it,
 * once, when the screen is RESUMED; [onConsumed] fires then (also when the reveal itself rests under reduced motion).
 * A background pause or a motion-setting change settles a running reveal; nothing ever restarts it.
 * This component cannot award a title or mutate a run.
 */
@Composable
internal fun rememberCompletionReveal(runId: String, celebrate: Boolean, onConsumed: () -> Unit): CompletionRevealState {
    val eligibleAtEntry = remember(runId) { celebrate }
    var started by rememberSaveable(runId) { mutableStateOf(false) }
    val reduced = rememberReduceMotion()
    val entrances = rememberEntranceMotionAllowed()
    val entrancesNow by rememberUpdatedState(entrances)
    val reveal = remember(runId) {
        val fresh = eligibleAtEntry && !started && !reduced
        CompletionRevealState(fresh = fresh, freshEntrance = fresh && entrances)
    }
    val consumed by rememberUpdatedState(onConsumed)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    LaunchedEffect(runId, lifecycle, reduced) {
        if (lifecycle != Lifecycle.State.RESUMED) {
            reveal.settle()
        } else if (!started) {
            started = true
            if (eligibleAtEntry) consumed()
            if (eligibleAtEntry && !reduced) reveal.play(entrance = entrancesNow) else reveal.settle()
        } else {
            // A background pause or runtime motion-setting change settles, never restarts, a reveal.
            reveal.settle()
        }
    }
    // Touch exploration turning on mid-reveal ends the entrance at once; nothing ever starts it again.
    LaunchedEffect(reveal, entrances) { if (!entrances) reveal.settleEntrance() }
    return reveal
}

/**
 * Only a saved completed record opts in. This component cannot award a title or mutate a run.
 * [badgeModifier] is chained after the title badge's size and tag; the title-unlock popup (lobby2) uses it only
 * as the landing spot of its medal (it must never move, hide or re-time the badge).
 */
@Composable
internal fun CompletionSealHeader(
    runId: String,
    titleEarned: Boolean,
    celebrate: Boolean,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
) {
    CompletionSealHeader(rememberCompletionReveal(runId, celebrate, onConsumed), titleEarned, modifier, badgeModifier)
}

/**
 * The header of [reveal]: the gold sweep, the heading and, when a title was earned, its medal. [titleReveal] is how the
 * medal joins the reveal (GLOW: one breath of gold behind it; SPIN_STAMP: a full turn as it lands); the certificate
 * passes NONE while the title-unlock popup celebrates the title instead.
 */
@Composable
internal fun CompletionSealHeader(
    reveal: CompletionRevealState,
    titleEarned: Boolean,
    modifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
    titleReveal: TitleRevealStyle = LocalMotionStyle.current.titleReveal,
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = 58.dp)
            .testTag("completion_header")
            .semantics { this[CompletionEffectProgress] = reveal.progress },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val p = reveal.progress
            val light = sin(PI * p).toFloat().coerceAtLeast(0f)
            if (light > 0f) {
                val x = size.width * p
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFC79B47).copy(alpha = light * .7f), Color.Transparent),
                        startX = x - size.width * .24f, endX = x + size.width * .24f),
                    Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "인생기록증", color = LifeGameInkSecondary, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (titleEarned) {
                val clock = reveal.clockMs
                TitleMedallion(
                    Modifier.size(56.dp).testTag("certificate_title_badge").then(badgeModifier),
                    spin = titleReveal == TitleRevealStyle.SPIN_STAMP,
                    halo = if (titleReveal == TitleRevealStyle.GLOW) ({ completionTitleHalo(clock.value) }) else CompletionNoHalo,
                ) { reveal.progress }
            }
        }
    }
}

/** titleReveal GLOW: the gold breath behind the medal and the title, 0 (nothing drawn) outside 560–1000 ms. */
internal object CompletionTitleHaloTimeline {
    const val RISE_START_MS = 560
    const val PEAK_MS = 760
    const val END_MS = 1_000
}

/** The GLOW envelope at [clockMs] of the reveal: eases out from 0 to 1 by the peak, eases back in to 0 by the end. */
internal fun completionTitleHalo(clockMs: Float): Float = when {
    clockMs <= CompletionTitleHaloTimeline.RISE_START_MS || clockMs >= CompletionTitleHaloTimeline.END_MS -> 0f
    clockMs <= CompletionTitleHaloTimeline.PEAK_MS -> EaseOut.transform(
        stepProgress(clockMs, CompletionTitleHaloTimeline.RISE_START_MS.toFloat(),
            (CompletionTitleHaloTimeline.PEAK_MS - CompletionTitleHaloTimeline.RISE_START_MS).toFloat()),
    )
    else -> 1f - EaseIn.transform(
        stepProgress(clockMs, CompletionTitleHaloTimeline.PEAK_MS.toFloat(),
            (CompletionTitleHaloTimeline.END_MS - CompletionTitleHaloTimeline.PEAK_MS).toFloat()),
    )
}

private val CompletionNoHalo: () -> Float = { 0f }

/**
 * Small dimensional illustration; no input handler, random numbers, timer, or spoken duplicate.
 * - [spin] (titleReveal SPIN_STAMP): the medal turns a full round and settles from 0.6× with a small overshoot,
 *   instead of the gentle turn-in.
 * - [halo] (titleReveal GLOW, 0..1): a gold glow behind the medal; nothing is drawn at 0. The canvas does not clip it.
 * Both stay inside the medal's own layer, so the tagged node's bounds never change. [progress] stays the last
 * parameter, so a trailing lambda keeps binding to it.
 */
@Composable
internal fun TitleMedallion(
    modifier: Modifier = Modifier,
    spin: Boolean = false,
    halo: () -> Float = CompletionNoHalo,
    progress: () -> Float = { 1f },
) {
    Canvas(modifier.graphicsLayer {
        val p = progress()
        if (spin) {
            val landed = .6f + .4f * EaseOutBack.transform(p)
            scaleX = landed
            scaleY = landed
            rotationY = (1f - p) * -360f
        } else {
            scaleX = .82f + p * .18f
            scaleY = scaleX
            rotationY = (1f - p) * -24f
        }
        rotationZ = (1f - p) * -8f
        cameraDistance = 12f * density
    }) {
        val edge = size.minDimension
        val centre = Offset(size.width / 2f, size.height * .44f)
        val radius = edge * .31f
        val glow = halo().coerceIn(0f, 1f)
        if (glow > 0f) {
            val reach = radius * (1.3f + .5f * glow)
            drawCircle(
                Brush.radialGradient(listOf(LifeGameGold.copy(alpha = .45f * glow), LifeGameGold.copy(alpha = 0f)), centre, reach),
                radius = reach,
                center = centre,
            )
        }
        fun ribbon(side: Float) = Path().apply {
            moveTo(centre.x + side * edge * .04f, centre.y + edge * .15f)
            lineTo(centre.x + side * edge * .23f, centre.y + edge * .11f)
            lineTo(centre.x + side * edge * .27f, centre.y + edge * .48f)
            lineTo(centre.x + side * edge * .15f, centre.y + edge * .39f)
            lineTo(centre.x + side * edge * .04f, centre.y + edge * .46f)
            close()
        }
        drawPath(ribbon(-1f), Color(0xFF5D6750))
        drawPath(ribbon(1f), Color(0xFF3E4D40))
        drawCircle(Color.Black.copy(alpha = .13f), radius + edge * .025f, centre + Offset(0f, edge * .045f))
        drawCircle(Brush.linearGradient(listOf(Color(0xFF916332), Color(0xFFECCB7D), Color(0xFF936530)),
            centre - Offset(radius, radius), centre + Offset(radius, radius)), radius, centre)
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFEDB7), Color(0xFFDAAD55), Color(0xFFB77D38)),
            centre - Offset(radius, radius), centre + Offset(radius, radius)), radius * .86f, centre)
        drawCircle(Color(0xFFFFF3CE).copy(alpha = .8f), radius * .83f, centre, style = Stroke(edge * .014f))
        val star = Path().apply {
            repeat(10) { index ->
                val angle = -PI / 2 + index * PI / 5
                val r = radius * if (index % 2 == 0) .55f else .25f
                val x = centre.x + cos(angle).toFloat() * r
                val y = centre.y + sin(angle).toFloat() * r
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, Color(0xFF8E612D))
        drawPath(star, Color(0xFFFFE5A0), style = Stroke(edge * .01f))
        val glint = sin(PI * progress()).toFloat().coerceAtLeast(0f)
        if (glint > 0f) {
            listOf(-1f, 1f).forEach { side ->
                val point = centre + Offset(side * edge * (.39f + .05f * progress()), -edge * .21f)
                drawLine(Color(0xFFD6AF61).copy(alpha = glint), point - Offset(edge * .045f, 0f), point + Offset(edge * .045f, 0f), edge * .02f)
                drawLine(Color(0xFFD6AF61).copy(alpha = glint), point - Offset(0f, edge * .045f), point + Offset(0f, edge * .045f), edge * .02f)
            }
        }
    }
}
