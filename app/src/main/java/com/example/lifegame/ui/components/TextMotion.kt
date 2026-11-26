package com.example.lifegame.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText

/*
 * TextMotion — draw-phase text effects (bookends, stage 2, 2026-11-26).
 *
 * Nothing here adds a node or changes a string: the real Text keeps its layout, semantics and TalkBack focus, and
 * these helpers only decide how its glyphs are painted for a moment. Glyph positions come from the Text's own
 * TextLayoutResult (onTextLayout), so wrapped lines work too. Animated values are read only while drawing.
 */

/**
 * When the glyphs of a stamped run land: visible glyph k (spaces skipped) starts at `startMs + k · gap` and takes
 * [durationMs]. With many glyphs the gap shrinks so the last one still starts within [maxSpanMs] of the first.
 */
@Immutable
internal data class GlyphStampTiming(
    val startMs: Float,
    val gapMs: Float,
    val durationMs: Float,
    val maxSpanMs: Float = Float.POSITIVE_INFINITY,
) {
    fun gapFor(count: Int): Float = if (count <= 1) 0f else minOf(gapMs, maxSpanMs / (count - 1))

    fun startOf(ordinal: Int, count: Int): Float = when {
        count <= 1 -> startMs
        gapMs * (count - 1) <= maxSpanMs -> startMs + ordinal * gapMs
        // Spread over exactly the span (multiply first, so the last glyph starts exactly at startMs + maxSpanMs).
        else -> startMs + maxSpanMs * ordinal / (count - 1)
    }

    /** The clock time at which all [count] glyphs have landed. */
    fun endMs(count: Int): Float = if (count <= 0) startMs else startOf(count - 1, count) + durationMs

    /** Linear progress (0..1) of glyph [ordinal] of [count] at [clockMs]. */
    fun progress(clockMs: Float, ordinal: Int, count: Int): Float =
        stepProgress(clockMs, startOf(ordinal, count), durationMs)
}

/** Boxes of the visible glyphs of [layout] from [fromOffset] on, in the Text's coordinates (spaces and breaks skipped). */
internal fun glyphStampBoxes(layout: TextLayoutResult, fromOffset: Int = 0): List<Rect> {
    val text = layout.layoutInput.text.text
    val boxes = ArrayList<Rect>(text.length)
    var offset = fromOffset.coerceAtLeast(0)
    while (offset < text.length) {
        val codePoint = text.codePointAt(offset)
        if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
            val box = layout.getBoundingBox(offset)
            if (box.width > 0f && box.height > 0f) boxes += box
        }
        offset += Character.charCount(codePoint)
    }
    return boxes
}

/**
 * Stamps a Text's glyphs in one by one in the lobby title's hand ([StampSpec]): each glyph drops in large and tilted,
 * lands with a small overshoot ([EaseOutBack]) and is fully inked by a third of its way.
 * - The real Text keeps painting itself (no zero-alpha layer), so its heading and TalkBack focus stay; a glyph whose
 *   turn has not come is simply not painted yet.
 * - Glyphs before [fromOffset] (a prefix such as "칭호 · ") and the spaces draw as usual throughout.
 * - Once the last glyph has landed, or when [active] is false, it is a plain drawContent().
 * [layout] is the Text's TextLayoutResult from onTextLayout; it and [clock] are read only while drawing.
 */
internal fun Modifier.glyphStamp(
    layout: State<TextLayoutResult?>,
    clock: State<Float>,
    timing: GlyphStampTiming,
    fromOffset: Int = 0,
    active: Boolean = true,
): Modifier = if (!active) this else drawWithCache {
    val boxes = layout.value?.let { glyphStampBoxes(it, fromOffset) }.orEmpty()
    val count = boxes.size
    val endMs = timing.endMs(count)
    val stamped = Path().apply { boxes.forEach { addRect(it) } }
    val glyphPaint = Paint()
    onDrawWithContent {
        val t = clock.value
        if (count == 0 || t >= endMs) {
            drawContent()
            return@onDrawWithContent
        }
        val content = this
        clipPath(stamped, ClipOp.Difference) { content.drawContent() }
        boxes.forEachIndexed { ordinal, box ->
            val p = timing.progress(t, ordinal, count)
            when {
                p <= 0f -> Unit
                p >= 1f -> clipRect(box.left, box.top, box.right, box.bottom) { content.drawContent() }
                else -> {
                    val pivot = Offset(
                        box.left + box.width * StampSpec.Pivot.pivotFractionX,
                        box.top + box.height * StampSpec.Pivot.pivotFractionY,
                    )
                    // The layer only needs to hold the glyph at its largest and most tilted.
                    val reach = maxOf(box.width, box.height) * (1f + StampSpec.SCALE_BOOST)
                    glyphPaint.alpha = StampSpec.alphaAt(p)
                    drawIntoCanvas { it.saveLayer(Rect(pivot, reach), glyphPaint) }
                    withStampTransform(EaseOutBack.transform(p), ordinal, pivot) {
                        clipRect(box.left, box.top, box.right, box.bottom) { content.drawContent() }
                    }
                    drawIntoCanvas { it.restore() }
                }
            }
        }
    }
}

/**
 * The ten decimal digits of one text style, measured once (on first use, in the draw phase) so a counting number can be
 * painted every frame without measuring text. Digits are placed by baseline, so they sit exactly where the real
 * glyphs of the same style sit on any line of a wrapped Text.
 */
internal class GlyphDigits(private val measurer: TextMeasurer, private val style: TextStyle) {
    private val layouts: Array<TextLayoutResult> by lazy(LazyThreadSafetyMode.NONE) {
        Array(10) { digit -> measurer.measure(digit.toString(), style, softWrap = false, maxLines = 1) }
    }

    /** The advance of [digit] (0..9). */
    fun advance(digit: Int): Float = layouts[digit].getBoundingBox(0).width

    /** The advance of [value] written in decimal (negative values count their digits only). */
    fun width(value: Int): Float = digitsOf(value).sumOf { advance(it).toDouble() }.toFloat()

    /** Paints [digit] with its left edge at [left] and its baseline at [baseline] (+ [dy]). */
    fun drawDigit(scope: DrawScope, digit: Int, left: Float, baseline: Float, dy: Float = 0f) {
        val layout = layouts[digit]
        scope.drawText(layout, topLeft = Offset(left, baseline - layout.firstBaseline + dy))
    }

    /** Paints [value] right-aligned to [right] on [baseline]. */
    fun drawNumber(scope: DrawScope, value: Int, right: Float, baseline: Float) {
        var x = right
        digitsOf(value).asReversed().forEach { digit ->
            x -= advance(digit)
            drawDigit(scope, digit, x, baseline)
        }
    }

    private fun digitsOf(value: Int): List<Int> = kotlin.math.abs(value).toString().map { it - '0' }
}
