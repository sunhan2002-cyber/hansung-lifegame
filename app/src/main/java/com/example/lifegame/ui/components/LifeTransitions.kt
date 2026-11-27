/**
 * Area: time — 시기 전환 장 넘김 (stage 2, 2026-11-26).
 *
 * Frozen contract: `@Composable internal fun PeriodTurnOverlay(clock: LifeClock?, modifier: Modifier = Modifier)`.
 * PlayHost composes it right after PlayPhaseScreen, inside the CompositionLocalProvider that also provides
 * LocalLifeClock, with `Modifier.fillMaxSize()`: above the new screen, below the AlertDialogs and the 300 ms
 * screen_change_tap_guard, and never in the finishing / resetting branches.
 *
 * The overlay is draw-only (no semantics, no testTag, no pointer input: touches reach the new screen underneath) and
 * composes nothing while no turn runs or for a null clock. The old screen is never kept composed: the outgoing chapter
 * ("제2장 · 유년기 · 끝") is only drawn. A turn starts in the composition that first shows the new screen (LifeClock.kt),
 * so its cover is in that very frame; when it ends or leaves composition it releases the lifeline waiting on it.
 *
 * Variants (PeriodTransitionStyle):
 * - FILM_ADVANCE: the old chapter's end card advances one frame like the year film (hold → 지지직 advance with its hitch
 *   → the gate opens onto the live new screen).
 * - PAGE_TURN: the old chapter's last page peels from the bottom-right corner toward the upper left, so the new screen's
 *   focus cards and buttons (bottom right) show first.
 * Playback speed shortens a turn only when it lands on auto progress (R-21; [PeriodTurn.speed]).
 */
package com.example.lifegame.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** The period turn drawn over PlayHost when the run moves into a new period. Nothing while idle or without a clock. */
@Composable
internal fun PeriodTurnOverlay(clock: LifeClock?, modifier: Modifier = Modifier) {
    val turn = clock?.turn ?: return
    if (!clock.turning) return
    val memory = clock.memory
    val style = LocalMotionStyle.current.periodTransition
    val allowed = rememberEntranceMotionAllowed()
    if (style == PeriodTransitionStyle.NONE || !allowed) {
        // Motion turned off (or the style changed) during the turn: end it now; the lifeline must not wait for it.
        LaunchedEffect(memory, turn.serial) { memory.finishTurn(turn.serial) }
        return
    }
    key(turn.serial) { PeriodTurnCanvas(turn, style, memory, modifier) }
}

/** One turn: a frame-clock timeline drawn into a single full-screen Spacer. */
@Composable
private fun PeriodTurnCanvas(turn: PeriodTurn, style: PeriodTransitionStyle, memory: LifeClockMemory, modifier: Modifier) {
    val timing = remember(style, turn.speed) { periodTurnTiming(style, turn.speed) }
    val currentTiming = rememberUpdatedState(timing)
    // Starts at 0 in this very composition: the first frame is fully covered, the new screen never flashes.
    val clockMs = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(memory, turn.serial) {
        var start = -1L
        while (true) {
            val now = withFrameMillis { it }
            if (start < 0L) start = now
            val elapsed = (now - start).toFloat()
            clockMs.floatValue = elapsed
            if (elapsed >= currentTiming.value.totalMs) break
        }
        memory.finishTurn(turn.serial)
    }
    // Leaving composition early (the run ended, motion turned off) still releases the waiting lifeline.
    DisposableEffect(memory, turn.serial) { onDispose { memory.finishTurn(turn.serial) } }
    val measurer = rememberTextMeasurer()
    // The new screens pad by the safe-drawing inset; the old page's header sits where theirs will. Only the part of the
    // inset the overlay actually lies under counts (none inside the comparison screen's phone frame).
    val windowInsetTop = WindowInsets.safeDrawing.getTop(LocalDensity.current).toFloat()
    val topInWindow = remember { mutableFloatStateOf(0f) }
    val paths = remember { PeriodTurnPaths() }
    Spacer(
        modifier
            .onGloballyPositioned { topInWindow.floatValue = it.positionInWindow().y }
            .drawWithCache {
                val page = periodTurnPage(measurer, turn.from)
                onDrawBehind {
                    val t = clockMs.floatValue
                    val now = currentTiming.value
                    val topInset = max(0f, windowInsetTop - topInWindow.floatValue)
                    when (style) {
                        PeriodTransitionStyle.PAGE_TURN -> drawPageTurn(t, now, page, topInset, paths)
                        PeriodTransitionStyle.FILM_ADVANCE -> drawFilmAdvance(t, now, page, paths)
                        PeriodTransitionStyle.NONE -> Unit
                    }
                }
            },
    )
}

// ---------------------------------------------------------------------------------------------------------
// Timing and geometry (pure: unit-tested in LifeClockTest)
// ---------------------------------------------------------------------------------------------------------

/**
 * A turn's segments (ms). FILM_ADVANCE: hold, advance, open. PAGE_TURN: the whole peel is [moveMs].
 * Their sum is when the overlay leaves composition.
 */
@Immutable
internal data class PeriodTurnTiming(val holdMs: Float, val moveMs: Float, val openMs: Float) {
    val totalMs: Float get() = holdMs + moveMs + openMs
}

/**
 * The timing for [style] at playback [speed] ([PeriodTurn.speed]: 1 unless the turn lands on auto progress). Faster
 * playback divides each segment, down to a floor that still reads as a turn.
 */
internal fun periodTurnTiming(style: PeriodTransitionStyle, speed: Int): PeriodTurnTiming {
    val s = speed.coerceAtLeast(1).toFloat()
    return when (style) {
        PeriodTransitionStyle.FILM_ADVANCE -> PeriodTurnTiming(
            holdMs = max(60f, 160f / s),
            moveMs = max(120f, NOISE_ADVANCE_MILLIS / s),
            openMs = max(140f, 320f / s),
        )
        PeriodTransitionStyle.PAGE_TURN -> PeriodTurnTiming(holdMs = 0f, moveMs = max(280f, 760f / s), openMs = 0f)
        PeriodTransitionStyle.NONE -> PeriodTurnTiming(0f, 0f, 0f)
    }
}

/** The peel's pace: a slow lift, a quick sweep and a soft settle. */
private val PageTurnEasing = safeCubicBezierEasing(0.5f, 0f, 0.25f, 1f)

/** The direction the page corner is dragged: toward the upper left, a little flatter than the diagonal. */
internal val PageTurnDirection: Offset = Offset(-1f, -0.42f).let { it / it.getDistance() }

/**
 * One frame of the page turn, in px. The fold line is {p : (p − C)·u = d} with C the bottom-right corner, u
 * [PageTurnDirection] and d growing with the progress. [remaining] is the old page still lying flat, [revealed] where
 * the new screen shows, [flap] the lifted part folded back across the fold (it lies over [remaining]). [foldStart] and
 * [foldEnd] are the crease inside the screen, [foldPoint] the fold's point on the corner's path and [corner] the page
 * corner's position now (C reflected). Polygons are lists of corners in order; empty when there is nothing.
 */
@Immutable
internal data class PageTurnFrame(
    val remaining: List<Offset>,
    val revealed: List<Offset>,
    val flap: List<Offset>,
    val foldStart: Offset,
    val foldEnd: Offset,
    val foldPoint: Offset,
    val corner: Offset,
)

/**
 * The page turn at [progress] (0 = nothing lifted, 1 = the page and its flap have left the screen) for a [width] ×
 * [height] screen. [overshoot] (px) carries the fold past the far corner so the flap's shadow leaves too.
 */
internal fun pageTurnFrame(width: Float, height: Float, progress: Float, overshoot: Float): PageTurnFrame {
    val u = PageTurnDirection
    val c = Offset(width, height)
    val d = progress.coerceIn(0f, 1f) * (width * abs(u.x) + height * abs(u.y) + overshoot)
    fun side(p: Offset): Float = (p.x - c.x) * u.x + (p.y - c.y) * u.y - d
    val rect = listOf(Offset(0f, 0f), Offset(width, 0f), c, Offset(0f, height))
    val remaining = pageTurnClip(rect) { side(it) }.takeIf { pageTurnArea(it) > 0f }.orEmpty()
    val revealed = pageTurnClip(rect) { -side(it) }.takeIf { pageTurnArea(it) > 0f }.orEmpty()
    val flap = revealed.map { p ->
        val s = side(p)
        Offset(p.x - 2f * s * u.x, p.y - 2f * s * u.y)
    }
    val epsilon = (width + height) * 1e-4f
    val fold = remaining.filter { abs(side(it)) <= epsilon }.ifEmpty { revealed.filter { abs(side(it)) <= epsilon } }
    val foldPoint = Offset(c.x + d * u.x, c.y + d * u.y)
    return PageTurnFrame(
        remaining = remaining,
        revealed = revealed,
        flap = flap,
        foldStart = fold.firstOrNull() ?: foldPoint,
        foldEnd = fold.lastOrNull() ?: foldPoint,
        foldPoint = foldPoint,
        corner = Offset(c.x + 2f * d * u.x, c.y + 2f * d * u.y),
    )
}

/** The area of a simple polygon (shoelace), always ≥ 0. */
internal fun pageTurnArea(polygon: List<Offset>): Float {
    if (polygon.size < 3) return 0f
    var twice = 0f
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        twice += a.x * b.y - b.x * a.y
    }
    return abs(twice) / 2f
}

/** Sutherland–Hodgman: the part of the convex [polygon] where [inside] (a signed distance, linear along edges) is ≥ 0. */
private inline fun pageTurnClip(polygon: List<Offset>, inside: (Offset) -> Float): List<Offset> {
    val out = ArrayList<Offset>(polygon.size + 1)
    for (i in polygon.indices) {
        val a = polygon[i]
        val b = polygon[(i + 1) % polygon.size]
        val fa = inside(a)
        val fb = inside(b)
        if (fa >= 0f) out += a
        if ((fa >= 0f) != (fb >= 0f)) {
            val t = fa / (fa - fb)
            out += Offset(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t)
        }
    }
    return out
}

// ---------------------------------------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------------------------------------

/** Paths reused every frame (reset, never reallocated). */
private class PeriodTurnPaths {
    val remaining = Path()
    val revealed = Path()
    val flap = Path()
    val shadow = Path()
    val film = Path()
    val card = Path()
}

/** The outgoing chapter's lines, measured once per size: "제2장" · "유년기" · gold rule · "끝". */
private class PeriodTurnPage(
    val label: TextLayoutResult?,
    val title: TextLayoutResult,
    val end: TextLayoutResult,
    val labelGap: Float,
    val titleGap: Float,
    val rule: Size,
    val ruleGap: Float,
) {
    val height: Float
        get() = (label?.let { it.size.height + labelGap } ?: 0f) + title.size.height + titleGap + rule.height + ruleGap + end.size.height
}

/** Measures the page text like ChapterFocusScreen's header (13sp label, 34sp/42sp serif title), within 20dp margins. */
private fun CacheDrawScope.periodTurnPage(measurer: TextMeasurer, from: LifeMark): PeriodTurnPage {
    val constraints = Constraints(maxWidth = (size.width - 40.dp.toPx()).roundToInt().coerceAtLeast(1))
    val label = from.chapterLabel.takeIf { it.isNotBlank() }?.let {
        measurer.measure(it, TextStyle(color = LifeGameInkSecondary, fontSize = 13.sp), constraints = constraints)
    }
    val title = measurer.measure(
        from.periodName,
        TextStyle(color = LifeGameInk, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp),
        constraints = constraints,
    )
    // ChapterFocusScreen already says "유년기 끝" under the growth figures; the old page ends the same way.
    val end = measurer.measure("끝", TextStyle(color = LifeGameSpecial, fontWeight = FontWeight.Bold, fontSize = 15.sp), constraints = constraints)
    return PeriodTurnPage(
        label = label,
        title = title,
        end = end,
        labelGap = 12.dp.toPx(),
        titleGap = 14.dp.toPx(),
        rule = Size(56.dp.toPx(), 2.dp.toPx()),
        ruleGap = 10.dp.toPx(),
    )
}

/** Draws the page's lines from [top]: left-aligned at [x], or each line centred on [x] when [centered]. */
private fun DrawScope.drawPeriodTurnText(page: PeriodTurnPage, x: Float, top: Float, centered: Boolean, alpha: Float = 1f) {
    fun lineX(width: Float) = if (centered) x - width / 2f else x
    var y = top
    page.label?.let {
        drawText(it, topLeft = Offset(lineX(it.size.width.toFloat()), y), alpha = alpha)
        y += it.size.height + page.labelGap
    }
    drawText(page.title, topLeft = Offset(lineX(page.title.size.width.toFloat()), y), alpha = alpha)
    y += page.title.size.height + page.titleGap
    drawRect(LifeGameGold.copy(alpha = alpha), topLeft = Offset(lineX(page.rule.width), y), size = page.rule)
    y += page.rule.height + page.ruleGap
    drawText(page.end, topLeft = Offset(lineX(page.end.size.width.toFloat()), y), alpha = alpha)
}

/** Makes [this] the closed polygon through [points] (moved by [offset]). */
private fun Path.periodTurnPolygon(points: List<Offset>, offset: Offset = Offset.Zero): Path {
    reset()
    points.forEachIndexed { index, point ->
        if (index == 0) moveTo(point.x + offset.x, point.y + offset.y) else lineTo(point.x + offset.x, point.y + offset.y)
    }
    close()
    return this
}

/** PAGE_TURN: the old page peels from the bottom-right corner; the live new screen shows where it has lifted. */
private fun DrawScope.drawPageTurn(t: Float, timing: PeriodTurnTiming, page: PeriodTurnPage, topInset: Float, paths: PeriodTurnPaths) {
    val progress = PageTurnEasing.transform((t / timing.moveMs.coerceAtLeast(1f)).coerceIn(0f, 1f))
    val frame = pageTurnFrame(size.width, size.height, progress, 16.dp.toPx())
    val u = PageTurnDirection
    if (frame.remaining.isNotEmpty()) {
        val remaining = paths.remaining.periodTurnPolygon(frame.remaining)
        clipPath(remaining) {
            drawRect(LifeGamePaper)
            // Laid out like ChapterFocusScreen's header, so the new chapter's title appears where the old one was.
            drawPeriodTurnText(page, 20.dp.toPx(), topInset + 24.dp.toPx(), centered = false)
        }
    }
    if (frame.revealed.isNotEmpty()) {
        // The lifted page's shadow on the new screen, along the fold.
        val shadowEnd = Offset(frame.foldPoint.x - u.x * 28.dp.toPx(), frame.foldPoint.y - u.y * 28.dp.toPx())
        clipPath(paths.revealed.periodTurnPolygon(frame.revealed)) {
            drawRect(Brush.linearGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent), start = frame.foldPoint, end = shadowEnd))
        }
    }
    if (frame.flap.size < 3) return
    if (frame.remaining.isNotEmpty()) {
        val shadow = paths.shadow.periodTurnPolygon(frame.flap, Offset(2.dp.toPx(), 3.dp.toPx()))
        clipPath(paths.remaining) { drawPath(shadow, Color.Black.copy(alpha = 0.1f)) }
    }
    drawPath(
        paths.flap.periodTurnPolygon(frame.flap),
        Brush.linearGradient(listOf(Color(0xFFF2EADB), Color(0xFFE0D4BC)), start = frame.foldPoint, end = frame.corner),
    )
    // Outline the flap's free edges (not the fold), then the crease.
    val epsilon = (size.width + size.height) * 1e-4f
    fun onFold(p: Offset) = abs((p.x - size.width) * u.x + (p.y - size.height) * u.y -
        ((frame.foldPoint.x - size.width) * u.x + (frame.foldPoint.y - size.height) * u.y)) <= epsilon
    val outline = LifeGameRule.copy(alpha = 0.8f)
    for (index in frame.flap.indices) {
        val a = frame.flap[index]
        val b = frame.flap[(index + 1) % frame.flap.size]
        if (!(onFold(a) && onFold(b))) drawLine(outline, a, b, strokeWidth = 1.dp.toPx())
    }
    drawLine(Color.White.copy(alpha = 0.6f), frame.foldStart, frame.foldEnd, strokeWidth = 0.5.dp.toPx())
}

/** The full-screen film's measures (Week7 film tokens, scaled up for the whole screen). */
private object PeriodTurnFilm {
    val Band = 22.dp
    val Side = 12.dp
    val HoleWidth = 10.dp
    val HoleHeight = 7.dp
    val HolePitch = 20.dp
    val GateCorner = 3.dp
    const val FLASH_MS = 120f
    const val NOISE_STEP_MS = 60f
}

/**
 * FILM_ADVANCE: frame 0 is the old chapter's end card, frame 1 an unpainted gate through which the live new screen
 * shows. The film holds, advances one frame with the year film's hitch (지지직 on the outgoing card), then its bands and
 * borders slide off and it fades.
 */
private fun DrawScope.drawFilmAdvance(t: Float, timing: PeriodTurnTiming, page: PeriodTurnPage, paths: PeriodTurnPaths) {
    val width = size.width
    val height = size.height
    val band = PeriodTurnFilm.Band.toPx()
    val side = PeriodTurnFilm.Side.toPx()
    val corner = CornerRadius(PeriodTurnFilm.GateCorner.toPx())
    val moveStart = timing.holdMs
    val openStart = timing.holdMs + timing.moveMs
    val advance = if (t <= moveStart) 0f else normalAdvance(((t - moveStart) / timing.moveMs).coerceIn(0f, 1f))
    val trackX = -advance * width
    val open = stepProgress(t, openStart, timing.openMs, FastOutSlowInEasing)
    val alpha = 1f - ((open - 0.6f) / 0.4f).coerceIn(0f, 1f)
    if (alpha <= 0f) return
    val bandShift = band * open
    val sideShift = side * open
    // Frame 1's gate is a hole in the film base (even-odd), growing to the whole screen as the film opens.
    val gate = Rect(trackX + width + side - sideShift, band - bandShift, trackX + 2f * width - side + sideShift, height - band + bandShift)
    val film = paths.film.apply {
        reset()
        fillType = PathFillType.EvenOdd
        addRect(Rect(0f, 0f, width, height))
        addRoundRect(RoundRect(gate, corner))
    }
    drawPath(film, LifeGameInk.copy(alpha = alpha))
    val card = Rect(trackX + side, band, trackX + width - side, height - band)
    if (card.right > 0f) {
        clipPath(paths.card.apply { reset(); addRoundRect(RoundRect(card, corner)) }) {
            drawRect(LifeGamePaper.copy(alpha = alpha), topLeft = card.topLeft, size = card.size)
            drawPeriodTurnText(page, card.center.x, card.center.y - page.height / 2f, centered = true, alpha = alpha)
            if (t > moveStart && t < openStart) {
                // Deterministic crackle: the seed steps every 60 ms of the turn (never the game's random stream).
                drawNoise(card, seed = 1 + ((t - moveStart) / PeriodTurnFilm.NOISE_STEP_MS).toInt())
            }
        }
    }
    // The perforations ride with the film; the bands slide off as the gate opens.
    translate(top = -bandShift) {
        drawPerforations(
            trackX = trackX,
            strip = band,
            gateHeight = height - 2f * band + 2f * bandShift,
            holeWidth = PeriodTurnFilm.HoleWidth,
            holeHeight = PeriodTurnFilm.HoleHeight,
            holePitch = PeriodTurnFilm.HolePitch,
            color = LifeGamePaper.copy(alpha = alpha),
        )
    }
    val flash = 1f - ((t - openStart) / PeriodTurnFilm.FLASH_MS).coerceIn(0f, 1f)
    if (t >= openStart && flash > 0f) drawRect(Color.White.copy(alpha = 0.06f * flash), topLeft = gate.topLeft, size = gate.size)
}
