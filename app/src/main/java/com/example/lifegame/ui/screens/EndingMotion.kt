package com.example.lifegame.ui.screens

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.components.CertificateSealStyle
import com.example.lifegame.ui.components.CompletionRevealState
import com.example.lifegame.ui.components.CompletionTitleHaloTimeline
import com.example.lifegame.ui.components.GlyphStampTiming
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionHaptic
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.RecapMomentStyle
import com.example.lifegame.ui.components.RiseEasing
import com.example.lifegame.ui.components.StampSpec
import com.example.lifegame.ui.components.TitleRevealStyle
import com.example.lifegame.ui.components.completionTitleHalo
import com.example.lifegame.ui.components.glyphStamp
import com.example.lifegame.ui.components.rememberBeatHaptics
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.rememberOneShotClock
import com.example.lifegame.ui.components.riseIn
import com.example.lifegame.ui.components.stepProgress
import com.example.lifegame.ui.components.withStampTransform
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameLobbyPaper
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import kotlinx.coroutines.flow.first
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/*
 * EndingMotion — the stage-2 presentation of the end of a life (bookends, 2026-11-26):
 * - recapMoment: how each 주마등 moment enters (its own clock, all done within MotionTimeline.RECAP_MOTION_MAX_MS so
 *   the 2.8 s auto-advance and tests that wait for idle never cascade; nothing loops).
 * - certificateSeal / titleReveal: the first opening of a fresh 인생기록증, on the reveal clock of
 *   rememberCompletionReveal (done within MotionTimeline.CERTIFICATE_REVEAL_MS; it rests for archived records, replays,
 *   recreation and background pauses).
 * Presentation only. Every animated value is read in a layer or draw lambda; reduced motion and touch exploration rest
 * every variant in its final state.
 */

// ---------------------------------------------------------------------------------------------------------
// recapMoment
// ---------------------------------------------------------------------------------------------------------

/** Timing of one recap moment (ms from the moment's arrival). Lines: 0 place · 1 title · 2 choice · 3 result. */
internal object RecapMomentTimeline {
    const val LINES = 4
    const val SURFACE_PICTURE_MS = 900
    /** SURFACE: the picture starts this much larger and settles into focus. */
    const val SURFACE_ZOOM = 0.05f
    const val SURFACE_LINE_MS = 420
    val SurfaceLineRise: Dp = 12.dp
    const val DEVELOP_PICTURE_MS = 800
    const val DEVELOP_PAPER_ALPHA = 0.5f
    val DevelopFeather: Dp = 18.dp
    const val LANTERN_SLIDE_MS = 520
    const val LANTERN_FADE_IN_MS = 360
    const val LANTERN_FADE_OUT_MS = 300
    const val LANTERN_LIGHT_MS = 700
    const val LANTERN_LIGHT_ALPHA = 0.22f
    val LanternLight = Color(0xFFFFDEA0)

    fun lineStartMs(style: RecapMomentStyle, line: Int): Float = when (style) {
        RecapMomentStyle.SURFACE -> listOf(160, 260, 380, 480)[line].toFloat()
        RecapMomentStyle.DEVELOP -> listOf(240, 340, 460, 560)[line].toFloat()
        RecapMomentStyle.NONE, RecapMomentStyle.LANTERN -> 0f
    }

    fun lineDurationMs(style: RecapMomentStyle, line: Int): Float = when (style) {
        RecapMomentStyle.SURFACE -> SURFACE_LINE_MS.toFloat()
        RecapMomentStyle.DEVELOP -> listOf(300, 360, 300, 520)[line].toFloat()
        RecapMomentStyle.NONE, RecapMomentStyle.LANTERN -> 0f
    }

    /** When everything of [style] has ended (its moment clock's length). NONE reports the cap so a resting clock never starts. */
    fun totalMs(style: RecapMomentStyle): Int = when (style) {
        RecapMomentStyle.NONE -> MotionTimeline.RECAP_MOTION_MAX_MS
        RecapMomentStyle.SURFACE -> max(SURFACE_PICTURE_MS, lineEnd(style))
        RecapMomentStyle.DEVELOP -> max(DEVELOP_PICTURE_MS, lineEnd(style))
        RecapMomentStyle.LANTERN -> max(LANTERN_LIGHT_MS, LANTERN_SLIDE_MS)
    }

    private fun lineEnd(style: RecapMomentStyle): Int =
        (0 until LINES).maxOf { lineStartMs(style, it) + lineDurationMs(style, it) }.toInt()
}

/** The motion of one recap moment: the style and the moment's own clock (ms), read only while drawing. */
@Stable
internal class RecapMomentMotion(val style: RecapMomentStyle, val clock: State<Float>)

/**
 * A clock per moment ([key]): each AnimatedContent target gets its own. It rests for NONE, reduced motion and touch
 * exploration (recap_moment is a polite live region: its text must be visible when it is announced).
 */
@Composable
internal fun rememberRecapMomentMotion(key: String): RecapMomentMotion {
    val style = LocalMotionStyle.current.recapMoment
    val clock = rememberOneShotClock(
        key = key,
        active = style != RecapMomentStyle.NONE && rememberEntranceMotionAllowed(),
        totalMs = RecapMomentTimeline.totalMs(style),
    )
    return remember(style, clock) { RecapMomentMotion(style, clock) }
}

/**
 * The change between moments. Reduced motion snaps (as before), LANTERN slides the moments past like a turning lantern
 * when entrances may move, and everything else keeps today's crossfade. These are layout offsets inside AnimatedContent,
 * which clips them; the first moment has no transition.
 */
internal fun recapTransition(style: RecapMomentStyle, reduceMotion: Boolean, entrancesMove: Boolean): ContentTransform = when {
    reduceMotion -> fadeIn(snap()) togetherWith fadeOut(snap())
    style == RecapMomentStyle.LANTERN && entrancesMove ->
        (slideInHorizontally(tween(RecapMomentTimeline.LANTERN_SLIDE_MS, easing = FastOutSlowInEasing)) { it / 4 } +
            fadeIn(tween(RecapMomentTimeline.LANTERN_FADE_IN_MS))) togetherWith
            (slideOutHorizontally(tween(RecapMomentTimeline.LANTERN_SLIDE_MS, easing = FastOutSlowInEasing)) { -it / 4 } +
                fadeOut(tween(RecapMomentTimeline.LANTERN_FADE_OUT_MS)))
    else -> fadeIn() togetherWith fadeOut()
}

/**
 * The picture of a moment, inside its rounded clip: SURFACE settles from 5% larger into focus; DEVELOP finds its colour
 * like a developing photo (sepia under a paper veil, back to full colour); LANTERN passes a warm light across it once.
 */
internal fun Modifier.recapPicture(motion: RecapMomentMotion): Modifier {
    val clock = motion.clock
    return when (motion.style) {
        RecapMomentStyle.NONE -> this
        RecapMomentStyle.SURFACE -> graphicsLayer {
            val zoom = 1f + RecapMomentTimeline.SURFACE_ZOOM *
                (1f - stepProgress(clock.value, 0f, RecapMomentTimeline.SURFACE_PICTURE_MS.toFloat(), EaseOut))
            scaleX = zoom
            scaleY = zoom
        }
        RecapMomentStyle.DEVELOP -> drawWithCache {
            val developing = Paint()
            val matrix = ColorMatrix()
            val bounds = Rect(Offset.Zero, size)
            onDrawWithContent {
                val p = stepProgress(clock.value, 0f, RecapMomentTimeline.DEVELOP_PICTURE_MS.toFloat(), LinearOutSlowInEasing)
                if (p >= 1f) {
                    drawContent()
                    return@onDrawWithContent
                }
                recapDevelopMatrix(matrix, p)
                developing.colorFilter = ColorFilter.colorMatrix(matrix)
                drawIntoCanvas { it.saveLayer(bounds, developing) }
                drawContent()
                drawIntoCanvas { it.restore() }
                drawRect(LifeGameLobbyPaper, alpha = RecapMomentTimeline.DEVELOP_PAPER_ALPHA * (1f - p))
            }
        }
        RecapMomentStyle.LANTERN -> drawWithContent {
            drawContent()
            val p = stepProgress(clock.value, 0f, RecapMomentTimeline.LANTERN_LIGHT_MS.toFloat())
            if (p <= 0f || p >= 1f) return@drawWithContent
            val glow = RecapMomentTimeline.LANTERN_LIGHT_ALPHA * sin(PI.toFloat() * p)
            val centre = Offset(size.width * (0.15f + 0.70f * p), size.height * 0.42f)
            val reach = size.maxDimension * 0.55f
            drawCircle(
                Brush.radialGradient(
                    listOf(RecapMomentTimeline.LanternLight.copy(alpha = glow), RecapMomentTimeline.LanternLight.copy(alpha = 0f)),
                    centre, reach,
                ),
                radius = reach,
                center = centre,
            )
        }
    }
}

/**
 * Text line [line] of a moment: SURFACE lets it rise into place in turn; DEVELOP lets the words seep through from the
 * top like ink, under a curtain in the recap's own background colour ([LifeGameInk]). LANTERN and NONE leave it still.
 */
internal fun Modifier.recapLine(motion: RecapMomentMotion, line: Int): Modifier {
    val style = motion.style
    val clock = motion.clock
    val start = RecapMomentTimeline.lineStartMs(style, line)
    val duration = RecapMomentTimeline.lineDurationMs(style, line)
    return when (style) {
        RecapMomentStyle.SURFACE -> riseIn(clock, start, duration, RecapMomentTimeline.SurfaceLineRise)
        RecapMomentStyle.DEVELOP -> drawWithContent {
            drawContent()
            val p = stepProgress(clock.value, start, duration, FastOutSlowInEasing)
            if (p >= 1f) return@drawWithContent
            val feather = RecapMomentTimeline.DevelopFeather.toPx()
            val front = p * (size.height + feather)
            drawRect(
                Brush.verticalGradient(listOf(LifeGameInk.copy(alpha = 0f), LifeGameInk), startY = front - feather, endY = front),
                topLeft = Offset(0f, front - feather),
                size = Size(size.width, feather),
            )
            if (front < size.height) drawRect(LifeGameInk, topLeft = Offset(0f, front), size = Size(size.width, size.height - front))
        }
        RecapMomentStyle.NONE, RecapMomentStyle.LANTERN -> this
    }
}

/** DEVELOP: writes into [target] the colour matrix from sepia (p = 0) to identity (p = 1). */
internal fun recapDevelopMatrix(target: ColorMatrix, p: Float) {
    val k = p.coerceIn(0f, 1f)
    for (index in 0 until 20) {
        // Exact at both ends: pure sepia at 0, exactly the identity at 1.
        target.values[index] = RecapSepia[index] * (1f - k) + RecapIdentity[index] * k
    }
}

private val RecapIdentity = ColorMatrix().values

private val RecapSepia = floatArrayOf(
    0.393f, 0.769f, 0.189f, 0f, 0f,
    0.349f, 0.686f, 0.168f, 0f, 0f,
    0.272f, 0.534f, 0.131f, 0f, 0f,
    0f, 0f, 0f, 1f, 0f,
)

// ---------------------------------------------------------------------------------------------------------
// certificateSeal · titleReveal
// ---------------------------------------------------------------------------------------------------------

/** The certificate's first opening, in ms on the reveal clock. Everything has ended by CERTIFICATE_REVEAL_MS. */
internal object CertificateRevealTimeline {
    // STAMP
    const val CARD_RISE_MS = 420
    val CardRise: Dp = 14.dp
    const val BODY_START_MS = 90
    const val BODY_RISE_MS = 420
    val BodyRise: Dp = 14.dp
    const val SEAL_DROP_START_MS = 420
    const val SEAL_DROP_MS = 110
    /** The seal hits the paper; the decorative haptic beat. */
    const val SEAL_IMPACT_MS = SEAL_DROP_START_MS + SEAL_DROP_MS
    const val SEAL_SQUASH_MS = 170
    const val SEAL_SQUASH = 0.06f
    const val CARD_THUD_MS = 150
    val CardThud: Dp = 2.dp
    const val INK_RING_MS = 380
    const val INK_RING_ALPHA = 0.32f
    // INK_BLEED
    const val BLEED_FADE_MS = 500
    const val BLEED_BODY_START_MS = 80
    const val BLEED_SEAL_START_MS = 360
    const val BLEED_SEAL_MS = 520
    const val BLEED_SEAL_INK_MS = 240
    val BleedFeather: Dp = 6.dp
    const val BLEED_HALO_ALPHA = 0.10f
    // the seal
    val SealSize: Dp = 40.dp
    const val SEAL_ALPHA = 0.86f
    const val SEAL_TILT = -10f
    // titleReveal SPIN_STAMP: the title glyphs after "칭호 · " (the medal turns on the header's 720 ms progress)
    val TitleStamp = GlyphStampTiming(startMs = 560f, gapMs = 45f, durationMs = 240f, maxSpanMs = 200f)

    /** When everything of [style] has ended (the seal and its ring, the card and the body). */
    fun sealEndMs(style: CertificateSealStyle): Int = when (style) {
        CertificateSealStyle.NONE -> 0
        CertificateSealStyle.STAMP -> maxOf(SEAL_IMPACT_MS + SEAL_SQUASH_MS, SEAL_IMPACT_MS + CARD_THUD_MS,
            SEAL_IMPACT_MS + INK_RING_MS, BODY_START_MS + BODY_RISE_MS, CARD_RISE_MS)
        CertificateSealStyle.INK_BLEED -> maxOf(BLEED_SEAL_START_MS + BLEED_SEAL_MS, BLEED_BODY_START_MS + BLEED_FADE_MS)
    }

    /** When the title reveal of [style] has ended for a title of [glyphs] visible glyphs. */
    fun titleEndMs(style: TitleRevealStyle, glyphs: Int): Int = when (style) {
        TitleRevealStyle.NONE -> 0
        TitleRevealStyle.GLOW -> CompletionTitleHaloTimeline.END_MS
        TitleRevealStyle.SPIN_STAMP -> TitleStamp.endMs(glyphs).toInt()
    }
}

/** A short up-and-down bump, 0 at both ends and outside (0..1). */
internal fun certificateBump(q: Float): Float = if (q <= 0f || q >= 1f) 0f else sin(PI.toFloat() * q)

/**
 * The title reveal the certificate actually plays: none without a title, and none while the title-unlock popup
 * celebrates the title (it then has the stage).
 */
internal fun certificateTitleRevealFor(style: TitleRevealStyle, hasTitle: Boolean, popupCelebrates: Boolean): TitleRevealStyle =
    if (!hasTitle || popupCelebrates) TitleRevealStyle.NONE else style

/** The identity card's entrance: STAMP rises it in and lets it give under the seal's impact; INK_BLEED fades it in. */
internal fun Modifier.certificateCardEntrance(style: CertificateSealStyle, clock: State<Float>): Modifier = when (style) {
    CertificateSealStyle.NONE -> this
    CertificateSealStyle.STAMP -> graphicsLayer {
        val t = clock.value
        val p = stepProgress(t, 0f, CertificateRevealTimeline.CARD_RISE_MS.toFloat(), RiseEasing)
        val thud = certificateBump(stepProgress(t, CertificateRevealTimeline.SEAL_IMPACT_MS.toFloat(), CertificateRevealTimeline.CARD_THUD_MS.toFloat()))
        alpha = p
        translationY = (1f - p) * CertificateRevealTimeline.CardRise.toPx() + thud * CertificateRevealTimeline.CardThud.toPx()
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
    CertificateSealStyle.INK_BLEED -> graphicsLayer {
        alpha = stepProgress(clock.value, 0f, CertificateRevealTimeline.BLEED_FADE_MS.toFloat())
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }
}

/** The body below the card follows it in (STAMP rises, INK_BLEED fades). */
internal fun Modifier.certificateBodyEntrance(style: CertificateSealStyle, clock: State<Float>): Modifier = when (style) {
    CertificateSealStyle.NONE -> this
    CertificateSealStyle.STAMP -> riseIn(clock, CertificateRevealTimeline.BODY_START_MS.toFloat(),
        CertificateRevealTimeline.BODY_RISE_MS.toFloat(), CertificateRevealTimeline.BodyRise)
    CertificateSealStyle.INK_BLEED -> riseIn(clock, CertificateRevealTimeline.BLEED_BODY_START_MS.toFloat(),
        CertificateRevealTimeline.BLEED_FADE_MS.toFloat(), 0.dp)
}

/** One dry speck of a seal: where the ink did not take, in fractions of the seal's side around its centre. */
internal data class CertificateSealSpeck(val x: Float, val y: Float, val radius: Float)

/**
 * Seven specks placed by an integer hash of [runId] (FNV-1a, then xorshift): the same record always shows the same seal,
 * and the game's random stream is never touched. Positions stay inside the inner frame (|x|, |y| ≤ 0.36); radii are
 * 0.010–0.032 of the side.
 */
internal fun certificateSealSpecks(runId: String): List<CertificateSealSpeck> {
    var hash = 0x811C9DC5.toInt()
    runId.forEach { char -> hash = (hash xor char.code) * 0x01000193 }
    var state = if (hash == 0) 0x2545F491 else hash
    fun next(): Float {
        state = state xor (state shl 13)
        state = state xor (state ushr 17)
        state = state xor (state shl 5)
        return (state ushr 8) / 16_777_216f
    }
    return List(7) { CertificateSealSpeck(x = (next() - 0.5f) * 0.72f, y = (next() - 0.5f) * 0.72f, radius = 0.010f + next() * 0.022f) }
}

/** What the seal of one record looks like: its lettering (measured once per draw cache) and its specks. */
@Stable
internal class CertificateSealArt(val measurer: TextMeasurer, runId: String) {
    val specks: List<CertificateSealSpeck> = certificateSealSpecks(runId)
    val lettering = TextStyle(color = LifeGameSpecial, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
}

@Composable
internal fun rememberCertificateSealArt(runId: String): CertificateSealArt {
    val measurer = rememberTextMeasurer()
    return remember(measurer, runId) { CertificateSealArt(measurer, runId) }
}

/**
 * The red '인생 / 기록' seal across the lower-right corner of the portrait (the identity card's 64 × 96 dp avatar,
 * after its 14 dp padding), drawn after the card so neither the card's clip nor the avatar hides it. It adds no node.
 * - STAMP: dropped from above in the stamp hand ([StampSpec]) at 420 ms, squashes on impact and spreads an ink ring.
 * - INK_BLEED: seeps into the paper from its centre with a faint wet halo.
 * Both print the same seal at rest, so archived and replayed certificates always look stamped. NONE draws nothing.
 * Chain it before the card's clip.
 */
internal fun Modifier.certificateSeal(style: CertificateSealStyle, clock: State<Float>, art: CertificateSealArt): Modifier =
    if (style == CertificateSealStyle.NONE) this else drawWithCache {
        val top = art.measurer.measure("인생", art.lettering)
        val bottom = art.measurer.measure("기록", art.lettering)
        val side = CertificateRevealTimeline.SealSize.toPx()
        val centre = Offset(
            (14.dp + 64.dp - 4.dp).toPx(),
            (size.height + 96.dp.toPx()) / 2f - 12.dp.toPx(),
        )
        val layer = Rect(centre, side * (1f + StampSpec.SCALE_BOOST))
        val sealPaint = Paint()
        onDrawWithContent {
            drawContent()
            val t = clock.value
            when (style) {
                CertificateSealStyle.STAMP -> certificateDrawStampedSeal(t, centre, side, layer, sealPaint, top, bottom, art.specks)
                CertificateSealStyle.INK_BLEED -> certificateDrawBleedingSeal(t, centre, side, layer, sealPaint, top, bottom, art.specks)
                CertificateSealStyle.NONE -> Unit
            }
        }
    }

private fun DrawScope.certificateDrawStampedSeal(
    t: Float, centre: Offset, side: Float, layer: Rect, paint: Paint,
    top: TextLayoutResult, bottom: TextLayoutResult, specks: List<CertificateSealSpeck>,
) {
    val timeline = CertificateRevealTimeline
    if (t < timeline.SEAL_DROP_START_MS) return
    val drop = stepProgress(t, timeline.SEAL_DROP_START_MS.toFloat(), timeline.SEAL_DROP_MS.toFloat())
    val impact = stepProgress(t, timeline.SEAL_IMPACT_MS.toFloat(), timeline.SEAL_SQUASH_MS.toFloat())
    val ring = stepProgress(t, timeline.SEAL_IMPACT_MS.toFloat(), timeline.INK_RING_MS.toFloat())
    if (ring > 0f && ring < 1f) {
        val radius = side * (0.75f + 0.60f * FastOutSlowInEasing.transform(ring))
        drawCircle(LifeGameSpecial.copy(alpha = timeline.INK_RING_ALPHA * (1f - ring)), radius, centre, style = Stroke(1.5.dp.toPx()))
    }
    val squash = 1f - timeline.SEAL_SQUASH * certificateBump(impact)
    paint.alpha = timeline.SEAL_ALPHA * StampSpec.alphaAt(drop)
    withStampTransform(FastOutLinearInEasing.transform(drop), 0, centre) {
        scale(squash, squash, centre) {
            drawIntoCanvas { it.saveLayer(layer, paint) }
            certificateDrawSealArt(centre, side, top, bottom, specks)
            drawIntoCanvas { it.restore() }
        }
    }
}

private fun DrawScope.certificateDrawBleedingSeal(
    t: Float, centre: Offset, side: Float, layer: Rect, paint: Paint,
    top: TextLayoutResult, bottom: TextLayoutResult, specks: List<CertificateSealSpeck>,
) {
    val timeline = CertificateRevealTimeline
    val q = stepProgress(t, timeline.BLEED_SEAL_START_MS.toFloat(), timeline.BLEED_SEAL_MS.toFloat(), FastOutSlowInEasing)
    if (q <= 0f) return
    paint.alpha = timeline.SEAL_ALPHA * stepProgress(t, timeline.BLEED_SEAL_START_MS.toFloat(), timeline.BLEED_SEAL_INK_MS.toFloat())
    if (q < 1f) {
        val wet = side * 0.9f
        drawCircle(
            Brush.radialGradient(listOf(LifeGameSpecial.copy(alpha = timeline.BLEED_HALO_ALPHA * (1f - q)), LifeGameSpecial.copy(alpha = 0f)), centre, wet),
            radius = wet,
            center = centre,
        )
    }
    drawIntoCanvas { it.saveLayer(layer, paint) }
    certificateDrawSealArt(centre, side, top, bottom, specks)
    val reach = side * 0.75f * q
    if (q < 1f) {
        // Keep only the ink within the spreading radius (a soft edge of BleedFeather), everywhere in the layer.
        if (reach < 0.5f) {
            drawRect(Color.Black, layer.topLeft, layer.size, blendMode = BlendMode.Clear)
        } else {
            val solid = ((reach - timeline.BleedFeather.toPx()) / reach).coerceIn(0f, 1f)
            drawRect(
                Brush.radialGradient(0f to Color.Black, solid to Color.Black, 1f to Color.Transparent, center = centre, radius = reach),
                layer.topLeft, layer.size, blendMode = BlendMode.DstIn,
            )
        }
    }
    drawIntoCanvas { it.restore() }
}

/** The seal itself, opaque (the caller's layer gives it its 0.86 ink): frame, lettering and dry specks punched out. */
private fun DrawScope.certificateDrawSealArt(
    centre: Offset, side: Float, top: TextLayoutResult, bottom: TextLayoutResult, specks: List<CertificateSealSpeck>,
) {
    rotate(CertificateRevealTimeline.SEAL_TILT, centre) {
        val outer = 2.2.dp.toPx()
        val inset = 4.dp.toPx()
        val corner = 4.dp.toPx()
        val half = side / 2f
        drawRoundRect(
            LifeGameSpecial,
            topLeft = Offset(centre.x - half + outer / 2f, centre.y - half + outer / 2f),
            size = Size(side - outer, side - outer),
            cornerRadius = CornerRadius(corner),
            style = Stroke(outer),
        )
        drawRoundRect(
            LifeGameSpecial,
            topLeft = Offset(centre.x - half + inset, centre.y - half + inset),
            size = Size(side - inset * 2f, side - inset * 2f),
            cornerRadius = CornerRadius(corner / 2f),
            style = Stroke(0.9.dp.toPx()),
        )
        val gap = 1.dp.toPx()
        drawText(top, topLeft = Offset(centre.x - top.size.width / 2f, centre.y - gap / 2f - top.size.height))
        drawText(bottom, topLeft = Offset(centre.x - bottom.size.width / 2f, centre.y + gap / 2f))
        specks.forEach { speck ->
            drawCircle(LifeGameCard, speck.radius * side, Offset(centre.x + speck.x * side, centre.y + speck.y * side), blendMode = BlendMode.DstOut)
        }
    }
}

/**
 * The title line's reveal ('칭호 · …', laid out as [layout]): GLOW breathes gold behind each line once, in step with the
 * medal's halo; SPIN_STAMP stamps the title's glyphs in after the prefix while the medal turns. Draw only: the
 * certificate_title text and its semantics never change. NONE (also while the title-unlock popup celebrates) is still.
 */
internal fun Modifier.certificateTitleReveal(style: TitleRevealStyle, clock: State<Float>, layout: State<TextLayoutResult?>, prefixLength: Int): Modifier =
    when (style) {
        TitleRevealStyle.NONE -> this
        TitleRevealStyle.SPIN_STAMP -> glyphStamp(layout, clock, CertificateRevealTimeline.TitleStamp, fromOffset = prefixLength)
        TitleRevealStyle.GLOW -> drawBehind {
            val glow = completionTitleHalo(clock.value)
            val text = layout.value
            if (glow <= 0f || text == null) return@drawBehind
            val spill = 10.dp.toPx()
            for (line in 0 until text.lineCount) {
                val left = text.getLineLeft(line) - spill
                val right = text.getLineRight(line) + spill
                val lineTop = text.getLineTop(line)
                val lineBottom = text.getLineBottom(line)
                val centre = Offset((left + right) / 2f, (lineTop + lineBottom) / 2f)
                val rx = (right - left) / 2f
                val ry = (lineBottom - lineTop) * 0.45f
                if (rx <= 0f || ry <= 0f) continue
                scale(1f, ry / rx, centre) {
                    drawCircle(
                        Brush.radialGradient(listOf(LifeGameGold.copy(alpha = 0.5f * glow), LifeGameGold.copy(alpha = 0f)), centre, rx),
                        radius = rx,
                        center = centre,
                    )
                }
            }
        }
    }

/**
 * STAMP's decorative haptic: one beat when the seal hits the paper, felt only if the reveal is really animating at that
 * moment (never when it rests or snaps to its end) and never under reduced motion (rememberBeatHaptics).
 */
@Composable
internal fun CertificateSealHaptic(reveal: CompletionRevealState, style: CertificateSealStyle) {
    val beat = rememberBeatHaptics()
    LaunchedEffect(reveal, style) {
        if (style != CertificateSealStyle.STAMP) return@LaunchedEffect
        snapshotFlow { reveal.clockMs.value }.first { it >= CertificateRevealTimeline.SEAL_IMPACT_MS }
        if (reveal.animating) beat(MotionHaptic.CONFIRM)
    }
}
