package com.example.lifegame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameStatDown
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * play · 능력치 변화 연출 (statChange): 결과 화면의 변화 칩(DecisionScreen)과 PlayTopBar의 건강 게이지.
 *
 * - 칩과 게이지의 배치 · 글자 · 의미 · 태그는 그대로다. 굴러가는 숫자, 떠오르는 숫자, 미니 막대, 잔상, 흔들림은
 *   모두 그리기 단계에서만 그린다(의미 노드가 없어 "숫자 없는 게이지" 검사에도 보이지 않는다).
 * - 시계는 프레임 시계다(rememberOneShotClock · Animatable · tween). wall-clock delay()를 쓰지 않는다.
 * - 움직임 줄이기: 칩은 원래대로 고정 문구(EffectText)로 대신하고, 게이지는 바로 새 값이 된다.
 * - 게이지는 처음 그려질 때(화면에 들어올 때 · 장면 보기에서 돌아올 때 · 복원) 움직이지 않는다.
 */

// ---------------------------------------------------------------------------------------------------------
// Spring curves shared by the play area (chips, gauge, choice reveal / pick, result impact)
// ---------------------------------------------------------------------------------------------------------

/**
 * Step response of a damped spring (unit mass) at [tMs] ms: 0 at t ≤ 0, overshoots when [dampingRatio] < 1 and
 * settles at 1. Evaluated on one frame clock instead of an Animatable per part, so a whole sequence stays
 * deterministic and rests exactly where the clock rests.
 */
internal fun statChangeSpring(tMs: Float, dampingRatio: Float, stiffness: Float): Float {
    if (tMs <= 0f) return 0f
    val w0 = sqrt(stiffness.coerceAtLeast(1f))
    val t = tMs / 1000f
    if (dampingRatio >= 1f) return 1f - exp(-w0 * t) * (1f + w0 * t)
    val zeta = dampingRatio.coerceAtLeast(0.01f)
    val wd = w0 * sqrt(1f - zeta * zeta)
    val decay = zeta * w0
    return 1f - exp(-decay * t) * (kotlin.math.cos(wd * t) + decay / wd * sin(wd * t))
}

/**
 * Impulse response of the same spring, scaled so that its first peak is 1: a quick push away from rest that
 * swings back (a "pop" or a "dip"). 0 at t ≤ 0; decays toward 0.
 */
internal fun statChangeSpringPulse(tMs: Float, dampingRatio: Float, stiffness: Float): Float {
    if (tMs <= 0f) return 0f
    val w0 = sqrt(stiffness.coerceAtLeast(1f))
    val zeta = dampingRatio.coerceIn(0.01f, 0.99f)
    val wd = w0 * sqrt(1f - zeta * zeta)
    val decay = zeta * w0
    val peakT = atan2(wd, decay) / wd
    val peak = exp(-decay * peakT) * sin(wd * peakT)
    val t = tMs / 1000f
    return exp(-decay * t) * sin(wd * t) / peak
}

// ---------------------------------------------------------------------------------------------------------
// Result chips (FloatingChanges)
// ---------------------------------------------------------------------------------------------------------

private const val ROLL_STAGGER_MS = 60f
private const val ROLL_FADE_MS = 160f
private const val ROLL_LAND_MS = 280f
private const val ROLL_COUNT_DELAY_MS = 200f
private const val ROLL_COUNT_MS = 420f
private const val ROLL_ACCENT_MS = 280f
private const val ROLL_POP_MS = 420f
private const val FLOAT_STAGGER_MS = 70f
private const val FLOAT_FADE_MS = 180f
private const val FLOAT_START_MS = 120f
private const val FLOAT_RISE_MS = 780f
private const val FLOAT_DROP_MS = 640f

/** 떠오르는 숫자: 이정표와 같은 따뜻한 금빛(얻음), 돌발 표시와 같은 붉은빛(잃음). */
private val FloatGain = Color(0xFFF3D27A)
private val FloatLoss = Color(0xFFFF8A80)

/** A chip bundle's style and clock (ms since the chips appeared). Read [ms] only in draw lambdas. */
@Stable
internal class StatChangeChipMotion internal constructor(
    val style: StatChangeStyle,
    private val clock: State<Float>,
) {
    internal val ms: Float get() = clock.value
}

/**
 * The clock for one bundle of change chips, started in the composition where the chips appear (call it inside
 * the AnimatedVisibility content). It rests at its end for NONE and under reduced motion.
 */
@Composable
internal fun rememberStatChangeChips(style: StatChangeStyle, count: Int): StatChangeChipMotion {
    val clock = rememberOneShotClock(
        key = Unit,
        // An entrance (chips start transparent and away from their place): rests under TalkBack touch exploration too.
        active = style != StatChangeStyle.NONE && rememberEntranceMotionAllowed(),
        totalMs = statChangeChipsTotalMs(style, count),
    )
    return remember(style, clock) { StatChangeChipMotion(style, clock) }
}

/** When the last chip of a bundle of [count] has settled, per style. */
internal fun statChangeChipsTotalMs(style: StatChangeStyle, count: Int): Int {
    val last = (count - 1).coerceAtLeast(0)
    return when (style) {
        StatChangeStyle.NONE -> 0
        StatChangeStyle.ROLL ->
            (last * ROLL_STAGGER_MS + maxOf(ROLL_COUNT_DELAY_MS + ROLL_COUNT_MS, ROLL_LAND_MS + ROLL_POP_MS)).roundToInt()
        StatChangeStyle.FLOAT -> (last * FLOAT_STAGGER_MS + FLOAT_START_MS + FLOAT_RISE_MS).roundToInt()
    }
}

/** ROLL: chip [index] lands (0 → 1, RiseEasing) 60 ms after the previous one. */
internal fun statChangeLanding(ms: Float, index: Int): Float =
    stepProgress(ms, ROLL_STAGGER_MS * index, ROLL_LAND_MS, RiseEasing)

/** ROLL: chip [index]'s count (0 → 1) starts 200 ms after its landing starts. */
internal fun statChangeCount(ms: Float, index: Int): Float =
    stepProgress(ms, ROLL_STAGGER_MS * index + ROLL_COUNT_DELAY_MS, ROLL_COUNT_MS, FastOutSlowInEasing)

/** ROLL: ms since chip [index] landed (the pop or shake accent starts at 0). */
internal fun statChangeAccentMs(ms: Float, index: Int): Float = ms - (ROLL_STAGGER_MS * index + ROLL_LAND_MS)

/**
 * A chip's opacity. The bundle itself does not fade for ROLL and FLOAT: an AnimatedVisibility fade flattens the
 * row into an offscreen layer that would cut off chips landing from outside it and numbers floating above it.
 */
internal fun statChangeChipAlpha(style: StatChangeStyle, ms: Float, index: Int): Float = when (style) {
    StatChangeStyle.NONE -> 1f
    StatChangeStyle.ROLL -> stepProgress(ms, ROLL_STAGGER_MS * index, ROLL_FADE_MS)
    StatChangeStyle.FLOAT -> stepProgress(ms, FLOAT_STAGGER_MS * index, FLOAT_FADE_MS)
}

/** FLOAT: ms since chip [index]'s number left the chip. */
internal fun statChangeFloatMs(ms: Float, index: Int): Float = ms - (FLOAT_STAGGER_MS * index + FLOAT_START_MS)

/** The rolling number at one moment: [value] slides up and out while [next] comes in from below by [fraction]. */
internal data class StatChangeOdometerFrame(val value: Int, val next: Int, val fraction: Float)

/** The odometer for a change of [magnitude] at count progress [progress] (0..1). At 1 it rests on [magnitude]. */
internal fun statChangeOdometerFrame(magnitude: Int, progress: Float): StatChangeOdometerFrame {
    val top = magnitude.coerceAtLeast(0)
    val shown = top * progress.coerceIn(0f, 1f)
    val whole = floor(shown).toInt().coerceIn(0, top)
    return if (whole >= top) StatChangeOdometerFrame(top, top, 0f) else StatChangeOdometerFrame(whole, whole + 1, shown - whole)
}

/** Holds a chip's last text layout (set from onTextLayout, read in draw; not snapshot state). */
internal class StatChangeTextHolder {
    var layout: TextLayoutResult? = null
}

/** Where the chips of a FLOAT bundle sit, so their numbers can be drawn above the whole row. */
internal class StatChangeChipPlaces(count: Int) {
    internal var row: LayoutCoordinates? = null
    internal val chips = arrayOfNulls<LayoutCoordinates>(count)
}

/**
 * Draw-only motion of one change chip; its layout, text and semantics never move.
 * - ROLL: fades in while it lands from below (gain) or above (loss); then a gain pops (1 → 1.08 → 1) and a loss
 *   shakes with a red border flash. While it counts, a 2dp mini bar in the chip's bottom padding fills or drains
 *   from the old value ([fromValue]) to the new one ([toValue]); both null skips the bar.
 * - FLOAT: fades in on a 70 ms stagger; a loss chip shakes as its number drops (the numbers themselves are drawn by
 *   [statChangeFloatingNumbers], which reads the chip's place from [places]).
 * The fade is a layer alpha that modulates each draw (no offscreen buffer, so nothing drawn outside the pill is
 * cut off, and no geometry changes). Put it first in the chip's chain, with no clip in front of it.
 */
internal fun Modifier.statChangeChip(
    motion: StatChangeChipMotion,
    index: Int,
    delta: Int,
    fromValue: Int?,
    toValue: Int?,
    tone: Color,
    places: StatChangeChipPlaces? = null,
): Modifier {
    if (motion.style == StatChangeStyle.NONE) return this
    val faded = graphicsLayer {
        alpha = statChangeChipAlpha(motion.style, motion.ms, index)
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
    return when (motion.style) {
        StatChangeStyle.ROLL -> faded.drawWithContent {
            val t = motion.ms
            val rise = (if (delta > 0) 10.dp else (-10).dp).toPx() * (1f - statChangeLanding(t, index))
            val accent = statChangeAccentMs(t, index)
            val shake = if (delta < 0 && accent <= ROLL_ACCENT_MS) dampedShake(accent, 3.dp.toPx(), 70f, 90f) else 0f
            val pop = if (delta > 0 && accent <= ROLL_POP_MS) 1f + 0.08f * statChangeSpringPulse(accent, 0.45f, 800f) else 1f
            withTransform({
                translate(shake, rise)
                if (pop != 1f) scale(pop, pop, center)
            }) {
                this@drawWithContent.drawContent()
                if (delta < 0 && accent in 0f..ROLL_ACCENT_MS) {
                    // 원래 테두리(0.35) 위에 덧그려 0.35 → 0.9 → 0.35로 번쩍인다.
                    val flash = 0.846f * sin(PI.toFloat() * accent / ROLL_ACCENT_MS)
                    val stroke = 1.dp.toPx()
                    drawRoundRect(
                        tone.copy(alpha = flash.coerceIn(0f, 1f)),
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius((size.height - stroke) / 2f),
                        style = Stroke(stroke),
                    )
                }
                if (fromValue != null && toValue != null) drawChipBar(tone, fromValue, toValue, statChangeCount(t, index))
            }
        }
        else -> faded
            .then(
                if (places == null) Modifier
                else Modifier.onPlaced { coordinates -> if (index in places.chips.indices) places.chips[index] = coordinates },
            )
            .drawWithContent {
                val shakeAt = statChangeFloatMs(motion.ms, index)
                val shake = if (delta < 0 && shakeAt <= ROLL_ACCENT_MS) dampedShake(shakeAt, 3.dp.toPx(), 70f, 90f) else 0f
                if (shake == 0f) drawContent() else translate(left = shake) { this@drawWithContent.drawContent() }
            }
    }
}

/** The 2dp mini gauge inside the chip's existing 5dp bottom padding, 10dp in from each side. */
private fun DrawScope.drawChipBar(tone: Color, fromValue: Int, toValue: Int, progress: Float) {
    val inset = 10.dp.toPx()
    val width = size.width - inset * 2f
    if (width <= 0f) return
    val y = size.height - 2.5.dp.toPx()
    val stroke = 2.dp.toPx()
    val from = fromValue.coerceIn(0, 100) / 100f
    val to = toValue.coerceIn(0, 100) / 100f
    val level = from + (to - from) * progress
    drawLine(tone.copy(alpha = 0.18f), Offset(inset, y), Offset(inset + width, y), stroke, StrokeCap.Round)
    val end = width * level
    if (end > 0.5f) drawLine(tone.copy(alpha = 0.85f), Offset(inset, y), Offset(inset + end, y), stroke, StrokeCap.Round)
}

/**
 * ROLL's counter, drawn over the chip's own text: the name stays the real text; the signed number rolls up from 0
 * to the change (the next value slides in from below), clipped to the line box. Once counting ends it simply
 * draws the real text again. [numberStart] is the index of the sign in the chip's text. Put it after the chip's
 * padding (its draw scope is then the text box) and read the layout through [text] (set it from onTextLayout).
 */
internal fun Modifier.statChangeOdometer(
    motion: StatChangeChipMotion,
    index: Int,
    delta: Int,
    numberStart: Int,
    text: StatChangeTextHolder,
    measurer: TextMeasurer,
): Modifier = if (motion.style != StatChangeStyle.ROLL) this else drawWithContent {
    val layout = text.layout
    val progress = statChangeCount(motion.ms, index)
    if (layout == null || progress >= 1f || numberStart !in 1 until layout.layoutInput.text.length) {
        drawContent()
        return@drawWithContent
    }
    val numberLeft = layout.getHorizontalPosition(numberStart, usePrimaryDirection = true)
    clipRect(right = numberLeft) { this@drawWithContent.drawContent() }
    val style = layout.layoutInput.style
    val sign = measurer.measure(if (delta >= 0) "+" else "−", style)
    drawText(sign, topLeft = Offset(numberLeft, 0f))
    val digitsLeft = numberLeft + sign.size.width
    val lineHeight = layout.size.height.toFloat()
    val frame = statChangeOdometerFrame(kotlin.math.abs(delta), progress)
    clipRect(left = digitsLeft, top = 0f, right = size.width + lineHeight, bottom = lineHeight) {
        drawText(measurer.measure(frame.value.toString(), style), topLeft = Offset(digitsLeft, -frame.fraction * lineHeight))
        if (frame.next != frame.value) {
            drawText(measurer.measure(frame.next.toString(), style), topLeft = Offset(digitsLeft, (1f - frame.fraction) * lineHeight))
        }
    }
}

/**
 * FLOAT's detached numbers ('+5' rises in gold, '−3' drops in red), one from each chip's right edge. Drawn by the
 * row after its chips so a number is never covered by the next chip. Put it on the chips' row; each chip reports
 * its place through [statChangeChip]'s `places`.
 */
internal fun Modifier.statChangeFloatingNumbers(
    motion: StatChangeChipMotion,
    places: StatChangeChipPlaces,
    deltas: List<Int>,
    measurer: TextMeasurer,
): Modifier = if (motion.style != StatChangeStyle.FLOAT) this else onPlaced { places.row = it }.drawWithContent {
    drawContent()
    val row = places.row?.takeIf { it.isAttached } ?: return@drawWithContent
    deltas.forEachIndexed { index, delta ->
        val chip = places.chips.getOrNull(index)?.takeIf { it.isAttached } ?: return@forEachIndexed
        val topLeft = row.localPositionOf(chip, Offset.Zero)
        val chipSize = chip.size.toSize()
        val x = topLeft.x + chipSize.width - 4.dp.toPx()
        val y = if (delta > 0) topLeft.y + 2.dp.toPx() else topLeft.y + chipSize.height - 2.dp.toPx()
        drawStatChangeFloat(measurer, delta, Offset(x, y), statChangeFloatMs(motion.ms, index))
    }
}

/** Where a FLOAT number is and how it looks [tMs] after it left (null once it is gone). */
internal data class StatChangeFloatFrame(val dxDp: Float, val dyDp: Float, val scale: Float, val alpha: Float)

internal fun statChangeFloatFrame(delta: Int, tMs: Float): StatChangeFloatFrame? {
    if (tMs < 0f || delta == 0) return null
    return if (delta > 0) {
        val u = tMs / FLOAT_RISE_MS
        if (u >= 1f) return null
        StatChangeFloatFrame(
            dxDp = 0f,
            dyDp = -24f * FastOutSlowInEasing.transform(u),
            scale = 1.2f - 0.2f * (tMs / 160f).coerceIn(0f, 1f),
            alpha = if (u < 0.55f) 1f else 1f - (u - 0.55f) / 0.45f,
        )
    } else {
        val u = tMs / FLOAT_DROP_MS
        if (u >= 1f) return null
        StatChangeFloatFrame(
            dxDp = 2f * sin(2f * PI.toFloat() * tMs / 160f) * (1f - u),
            dyDp = 14f * FastOutLinearInEasing.transform(u),
            scale = 1f,
            alpha = if (u < 0.35f) 1f else 1f - (u - 0.35f) / 0.65f,
        )
    }
}

/** Draws a FLOAT number centred on [anchor] (moving from there). */
private fun DrawScope.drawStatChangeFloat(measurer: TextMeasurer, delta: Int, anchor: Offset, tMs: Float) {
    val frame = statChangeFloatFrame(delta, tMs) ?: return
    val label = if (delta > 0) "+$delta" else "−${-delta}"
    val layout = measurer.measure(
        label,
        TextStyle(color = if (delta > 0) FloatGain else FloatLoss, fontSize = 15.sp, fontWeight = FontWeight.Bold, shadow = OnSceneShadow),
    )
    val center = Offset(anchor.x + frame.dxDp.dp.toPx(), anchor.y + frame.dyDp.dp.toPx())
    scale(frame.scale, frame.scale, pivot = center) {
        drawText(
            layout,
            topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f),
            alpha = frame.alpha.coerceIn(0f, 1f),
        )
    }
}

// ---------------------------------------------------------------------------------------------------------
// Health gauge (HealthGauge in PlayChrome.kt)
// ---------------------------------------------------------------------------------------------------------

private const val GAUGE_FILL_MS = 560f
private const val GAUGE_TRAIL_DELAY_MS = 220f
private const val GAUGE_TRAIL_MS = 420f
private const val GAUGE_FLASH_MS = 220f
private const val GAUGE_SHAKE_MS = 240f
private const val GAUGE_TOTAL_MS = 800f
private const val GAUGE_REST_MS = 100_000f

/**
 * One health gauge's change: from which value to which, when it starts ([delayMs] on the clock) and whether the
 * accents (pale trail with a red flash, a short shake, FLOAT's number) are on. Remembered for as long as the gauge
 * is composed; [ms] is read only in draw lambdas.
 */
@Stable
internal class StatChangeGaugeMotion internal constructor(initial: Int, internal val measurer: TextMeasurer) {
    internal var from by mutableIntStateOf(initial)
        private set
    internal var to by mutableIntStateOf(initial)
        private set
    internal var delayMs by mutableFloatStateOf(0f)
        private set
    internal var accents by mutableStateOf(false)
        private set
    internal var floats by mutableStateOf(false)
        private set
    private val clock = Animatable(GAUGE_REST_MS)

    internal val ms: Float get() = clock.value

    internal suspend fun snap(value: Int) {
        from = value
        to = value
        floats = false
        clock.snapTo(GAUGE_REST_MS)
    }

    internal suspend fun play(from: Int, to: Int, delayMs: Float, accents: Boolean, floats: Boolean) {
        this.from = from
        this.to = to
        this.delayMs = delayMs
        this.accents = accents
        this.floats = floats
        val total = delayMs + GAUGE_TOTAL_MS
        clock.snapTo(0f)
        clock.animateTo(total, tween(total.roundToInt(), easing = LinearEasing))
    }

    /** Where the bar visibly ends now (0..100): the growing fill on a gain, the draining trail on a loss. */
    internal fun apparentLevel(): Int = when {
        to > from -> statChangeGaugeFillLevel(from, to, ms, delayMs)
        to < from -> statChangeGaugeTrailLevel(from, to, ms, delayMs, accents)
        else -> to.toFloat()
    }.roundToInt()

    /** The fill's drawn end (0..[value]). Before the effect has seen a new [value] it still shows the old level. */
    internal fun fillEnd(value: Int): Float = when {
        to != value -> minOf(to, value).toFloat()
        to > from -> statChangeGaugeFillLevel(from, to, ms, delayMs)
        else -> value.toFloat()
    }

    /** The end of the lost segment drawn behind the fill; [value] when there is none. */
    internal fun trailEnd(value: Int): Float = when {
        to != value -> maxOf(to, value).toFloat()
        to < from -> statChangeGaugeTrailLevel(from, to, ms, delayMs, accents)
        else -> value.toFloat()
    }

    /** Whether the loss has "hit" (accents only): the lost segment turns into a pale trail that flashes red. */
    internal fun hit(value: Int): Boolean = accents && to == value && to < from && ms >= delayMs && ms < delayMs + GAUGE_TOTAL_MS

    internal fun flashAlpha(value: Int): Float =
        if (!hit(value)) 0f else 0.45f * (1f - stepProgress(ms, delayMs, GAUGE_FLASH_MS))

    internal fun shakePx(scope: DrawScope, value: Int): Float {
        if (!hit(value)) return 0f
        val t = ms - delayMs
        return if (t > GAUGE_SHAKE_MS) 0f else with(scope) { dampedShake(t, 2.dp.toPx(), 60f, 52f) }
    }

    /** Whether the fill is still growing after the start (for the leading-edge highlight). */
    internal fun growing(value: Int): Boolean = accents && to == value && to > from && ms >= delayMs && ms < delayMs + GAUGE_FILL_MS
}

/** A growing fill's drawn end (0..100) [ms] after a change from [from] to [to] that starts at [delayMs]. */
internal fun statChangeGaugeFillLevel(from: Int, to: Int, ms: Float, delayMs: Float): Float =
    if (to <= from) to.toFloat() else from + (to - from) * stepProgress(ms, delayMs, GAUGE_FILL_MS, FastOutSlowInEasing)

/**
 * The lost segment's end (0..100) after a drop from [from] to [to]. With accents it holds for 220 ms after the hit
 * and then drains over 420 ms; the calm gauge simply drains over 560 ms.
 */
internal fun statChangeGaugeTrailLevel(from: Int, to: Int, ms: Float, delayMs: Float, accents: Boolean): Float = when {
    to >= from -> to.toFloat()
    accents -> from + (to - from) * stepProgress(ms, delayMs + GAUGE_TRAIL_DELAY_MS, GAUGE_TRAIL_MS, FastOutSlowInEasing)
    else -> from + (to - from) * stepProgress(ms, delayMs, GAUGE_FILL_MS, FastOutSlowInEasing)
}

/**
 * The gauge's change motion, or null when the gauge must simply show [value] (statChange NONE, reduced motion):
 * every gauge modifier is then a no-op and the bar is exactly today's. A first composition never animates.
 * [accents] adds the trail flash, the shake and (FLOAT) the floating number; [changeDelayMillis] holds the change
 * (frame clock) so it lands with the result effects after a picked card has left.
 */
@Composable
internal fun rememberStatChangeGauge(value: Int, accents: Boolean, changeDelayMillis: Long): StatChangeGaugeMotion? {
    val measurer = rememberTextMeasurer()
    val motion = remember { StatChangeGaugeMotion(value, measurer) }
    val style = LocalMotionStyle.current.statChange
    val animate = style != StatChangeStyle.NONE && !rememberReduceMotion()
    val latestAccents by rememberUpdatedState(accents)
    val latestDelay by rememberUpdatedState(changeDelayMillis)
    val latestStyle by rememberUpdatedState(style)
    LaunchedEffect(value, animate) {
        when {
            !animate -> motion.snap(value)
            value != motion.to -> motion.play(
                from = motion.apparentLevel(),
                to = value,
                delayMs = latestDelay.coerceAtLeast(0L).toFloat(),
                accents = latestAccents,
                floats = latestAccents && latestStyle == StatChangeStyle.FLOAT,
            )
        }
    }
    return motion.takeIf { animate }
}

/**
 * Shake (accents) and FLOAT's number above the bar's new end. Put it first on the bar, before its size and clip,
 * so it moves the whole bar and may draw above it.
 */
internal fun Modifier.statChangeGaugeShake(motion: StatChangeGaugeMotion?, value: Int): Modifier =
    if (motion == null) this else drawWithContent {
        val dx = motion.shakePx(this, value)
        if (dx == 0f) drawContent() else translate(left = dx) { this@drawWithContent.drawContent() }
        if (motion.floats && motion.to == value && motion.from != value) {
            val anchor = Offset(size.width * value.coerceIn(0, 100) / 100f, -10.dp.toPx())
            drawStatChangeFloat(motion.measurer, value - motion.from, anchor, motion.ms - motion.delayMs)
        }
    }

/**
 * The lost segment on the track, under the fill: in the fill's colour until the loss lands (so the bar still reads
 * the old length), then — with accents — a pale trail with a red flash; the calm gauge just drains in the fill's
 * colour. Put it right after the track's background.
 */
internal fun Modifier.statChangeGaugeTrail(motion: StatChangeGaugeMotion?, value: Int, fillColor: Color, onDark: Boolean): Modifier =
    if (motion == null) this else drawWithContent {
        val end = motion.trailEnd(value).coerceIn(0f, 100f)
        if (end > value) {
            val width = size.width * end / 100f
            val corner = CornerRadius(size.height / 2f)
            val color = when {
                !motion.hit(value) -> fillColor
                onDark -> LifeGamePaper.copy(alpha = 0.75f)
                else -> fillColor.copy(alpha = 0.4f)
            }
            drawRoundRect(color, size = Size(width, size.height), cornerRadius = corner)
            val flash = motion.flashAlpha(value)
            if (flash > 0f) drawRoundRect(LifeGameStatDown.copy(alpha = flash), size = Size(width, size.height), cornerRadius = corner)
        }
        drawContent()
    }

/**
 * A growing fill: drawn only up to its current end (a rounded bar), with a soft highlight riding the leading edge
 * when accents are on. The fill's layout width is always the new value. Put it after the fill's clip.
 */
internal fun Modifier.statChangeGaugeFill(motion: StatChangeGaugeMotion?, value: Int, fillColor: Color): Modifier =
    if (motion == null) this else drawWithContent {
        val shown = motion.fillEnd(value)
        if (value <= 0 || shown >= value) {
            drawContent()
            return@drawWithContent
        }
        val width = size.width * (shown / value).coerceIn(0f, 1f)
        if (width <= 0f) return@drawWithContent
        drawRoundRect(fillColor, size = Size(width, size.height), cornerRadius = CornerRadius(size.height / 2f))
        if (motion.growing(value)) {
            val glow = minOf(6.dp.toPx(), width)
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.55f)), startX = width - glow, endX = width),
                topLeft = Offset(width - glow, 0f),
                size = Size(glow, size.height),
            )
        }
    }
