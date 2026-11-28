/**
 * Area: time — 해 도장 · 시기선 점 이동 (stage 2, 2026-11-26).
 *
 * PlayHost keeps one [LifeClock] per run ([rememberLifeClock]) and provides it as [LocalLifeClock] around the play
 * screens. The period-turn overlay (LifeTransitions.kt) and every PeriodTrack read it. Nothing here changes layout,
 * semantics, input, callbacks or the game: the marker travel and the year stamp are drawn inside PeriodTrack's Canvas.
 *
 * Frozen contract (PlayChrome.kt and LifeGameApp.kt call these; class bodies may grow and overloads may be added):
 * - `@Stable internal class LifeClock`, [LocalLifeClock], `rememberLifeClock(play)`.
 * - `@Stable internal class PeriodTrackMotion` with [PeriodTrackMotion.Rest], `rememberPeriodTrackMotion(marks,
 *   yearLabel, stampYield)`.
 * - `DrawScope.drawPeriodTrackLine(marks, currentIndex, motion)`: with [PeriodTrackMotion.Rest] (and whenever nothing
 *   moves) exactly the lifeline PeriodTrack drew before stage 2.
 *
 * How one change plays:
 * 1. PlayHost sees a new [LifeMark] in composition. A forward period change into a chapter cover or auto progress becomes
 *    a [PeriodTurn] in that same composition (remember(mark) against the last committed mark; plain fields are written
 *    only in SideEffect), so the cover is drawn in the very frame the new screen appears.
 * 2. The PeriodTrack on screen compares the mark with [LifeClockMemory.trackShown], the last mark a track finished
 *    presenting. Its first frame still shows that old state; it waits for the turn, moves the marker to the new period,
 *    stamps the year label and commits trackShown when the stamp lands. A track that leaves the screen before that (장면
 *    보기, the image preview) replays the same change when it comes back.
 * 3. The first mark a PlayHost sees (new life, 이어하기, recreation) is a silent baseline. Under reduced motion or with
 *    AgeStampStyle.NONE every mark is committed at once, so nothing stale replays later.
 *
 * No ages (user decision, 2026-11-26): the stamp shows only the label PeriodTrack already has ("3번째 해").
 */
package com.example.lifegame.ui.components

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.PlayPhase
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameNight
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ---------------------------------------------------------------------------------------------------------
// Where the run is in time
// ---------------------------------------------------------------------------------------------------------

/**
 * One place of a run on the lifeline, as the play screen shows it (UI only: no age is derived).
 * [periodIndex] is the index of the current PeriodMarkUi (−1 if none) and [year] the "N번째 해" number. [periodName],
 * [chapterLabel], [phase] and [speed] describe the screen at that place: the period turn prints the outgoing chapter
 * from them, and speed scales a turn that lands on auto progress.
 */
@Immutable
internal data class LifeMark(
    val runKey: String,
    val periodIndex: Int,
    val year: Int,
    val periodName: String,
    val chapterLabel: String,
    val phase: PlayPhase,
    val speed: Int = 1,
) {
    /** Strictly later on the same run's lifeline (the screen and the speed do not matter). */
    fun isAfter(other: LifeMark): Boolean =
        runKey == other.runKey && (periodIndex > other.periodIndex || (periodIndex == other.periodIndex && year > other.year))
}

/** The mark PlayHost's current play state shows. */
internal fun lifeMarkOf(play: PeriodPlayUiState): LifeMark = LifeMark(
    runKey = play.runKey,
    periodIndex = play.periodMarks.indexOfFirst { it.isCurrent },
    year = play.yearInPeriod,
    periodName = play.periodName,
    chapterLabel = play.chapterLabel,
    phase = play.phase,
    speed = play.speed,
)

/**
 * A period change PlayHost covers with the period-turn overlay: [from] is the last committed mark (the outgoing chapter),
 * [to] the new screen. [serial] counts the turns of one PlayHost; the overlay's lifetime is keyed by it, so later marks
 * (a 4× year tick during the turn) never restart it.
 */
@Immutable
internal data class PeriodTurn(val from: LifeMark, val to: LifeMark, val serial: Int) {
    /** Playback speed scales the turn only when it lands on auto progress (R-21); a chapter cover always turns at 1×. */
    val speed: Int get() = if (to.phase == PlayPhase.AUTO) to.speed.coerceAtLeast(1) else 1
}

/**
 * Whether moving from [from] to [to] gets a period turn: a forward period change within one run that lands on a chapter
 * cover (유년기 · 청소년기 · 청년기 · 중년기) or on auto progress (중년기 → 노년기 has no cover). Never for the first mark,
 * another run, a decision or the ending.
 */
internal fun isPeriodTurn(from: LifeMark?, to: LifeMark?): Boolean =
    from != null && to != null && from.runKey == to.runKey && from.periodIndex >= 0 &&
        to.periodIndex > from.periodIndex && (to.phase == PlayPhase.CHAPTER || to.phase == PlayPhase.AUTO)

/**
 * What one PlayHost (or one comparison demo) remembers between compositions. The plain fields are written only after a
 * composition is committed (SideEffect) or from effects, never during composition; [finishedTurnSerial] is the only
 * snapshot state, so a waiting lifeline and the overlay see a turn end.
 */
internal class LifeClockMemory(seed: LifeMark? = null) {
    /** The mark of the last committed composition. */
    var committed: LifeMark? = seed

    /** The last mark a PeriodTrack finished presenting (its stamp landed), or that was presented at once. */
    var trackShown: LifeMark? = seed

    /**
     * The furthest period the lifeline marker has reached in [arrivedRunKey] — its travel finished — even if that
     * mark's stamp has not landed (so [trackShown] is still older). A newer track travels only from here, so once the
     * marker has arrived it never jumps back a period to travel again (a travel cut off before arriving replays).
     */
    private var arrivedRunKey: String? = seed?.runKey
    private var arrivedIndex: Int = seed?.periodIndex ?: -1

    /** The marker now stands on [mark]'s period. */
    fun arrive(mark: LifeMark) {
        if (arrivedRunKey != mark.runKey) {
            arrivedRunKey = mark.runKey
            arrivedIndex = mark.periodIndex
        } else if (mark.periodIndex > arrivedIndex) {
            arrivedIndex = mark.periodIndex
        }
    }

    /** [from] with its period moved up to where the marker already stands: the start of a new track's travel. */
    fun travelFrom(from: LifeMark): LifeMark =
        if (arrivedRunKey == from.runKey && arrivedIndex > from.periodIndex) from.copy(periodIndex = arrivedIndex) else from

    /** The newest turn; its serial is the pending turn serial. Kept after the turn ends so later marks keep it. */
    var latestTurn: PeriodTurn? = null

    /** The highest serial whose turn has ended (or will never run). */
    val finishedTurnSerial = mutableIntStateOf(0)

    fun finishTurn(serial: Int) {
        if (finishedTurnSerial.intValue < serial) finishedTurnSerial.intValue = serial
    }

    /** After a committed composition: remember [mark], adopt [turn], keep the baseline and the at-once presentations. */
    fun commit(mark: LifeMark?, turn: PeriodTurn?, presentAtOnce: Boolean) {
        committed = mark
        if (turn != null && turn.serial > (latestTurn?.serial ?: 0)) latestTurn = turn
        if (mark == null) return
        val shown = trackShown
        if (shown == null || shown.runKey != mark.runKey || presentAtOnce) {
            trackShown = mark
            arrive(mark)
        }
    }

    /** A PeriodTrack has shown [mark] (its stamp landed, or it rested at once). */
    fun present(mark: LifeMark) {
        trackShown = mark
        arrive(mark)
    }
}

/** What PlayHost knows about the run's position in time, for the lifeline and the period turn. */
@Stable
internal class LifeClock(
    /** Where the run is now. */
    val mark: LifeMark,
    /** The newest period turn of this PlayHost, kept after it ended ([turning] says whether it still runs). */
    val turn: PeriodTurn?,
    internal val memory: LifeClockMemory,
) {
    /** True while [turn] still covers the screen. A snapshot read. */
    val turning: Boolean get() = turn != null && memory.finishedTurnSerial.intValue < turn.serial
}

/** Provided by PlayHost next to LocalPlayMenu; null everywhere else, which keeps every time effect off. */
internal val LocalLifeClock = compositionLocalOf<LifeClock?> { null }

/** PlayHost's clock for the current run (null while there is no play state). Its first mark is a silent baseline. */
@Composable
internal fun rememberLifeClock(play: PeriodPlayUiState?): LifeClock? = rememberLifeClockFor(play?.let(::lifeMarkOf))

/**
 * The clock for a stream of marks: the same detection PlayHost uses, fed directly (the comparison screen and tests).
 * [seed] pre-fills the memory as if that mark had been committed and shown, so the very first composition already plays
 * the change seed → mark; PlayHost passes none. [periodTurns] = false never starts a turn (a demo without the overlay).
 */
@Composable
internal fun rememberLifeClockFor(mark: LifeMark?, seed: LifeMark? = null, periodTurns: Boolean = true): LifeClock? {
    val memory = remember { LifeClockMemory(seed) }
    val style = LocalMotionStyle.current
    val reduceMotion = rememberReduceMotion()
    // The turn hides the new screen for a moment: an entrance, so it also rests under touch exploration.
    val entranceAllowed = rememberEntranceMotionAllowed()
    val turnsAllowed = periodTurns && entranceAllowed && style.periodTransition != PeriodTransitionStyle.NONE
    // Decided once per mark, in the composition that first shows it: no snapshot write, no one-frame flash.
    val turn = remember(mark) {
        val previous = memory.committed
        if (turnsAllowed && isPeriodTurn(previous, mark)) {
            PeriodTurn(from = previous!!, to = mark!!, serial = (memory.latestTurn?.serial ?: 0) + 1)
        } else {
            memory.latestTurn
        }
    }
    val presentAtOnce = reduceMotion || style.ageStamp == AgeStampStyle.NONE
    SideEffect { memory.commit(mark, turn, presentAtOnce) }
    return remember(mark, turn, memory) { mark?.let { LifeClock(it, turn, memory) } }
}

// ---------------------------------------------------------------------------------------------------------
// The year stamp's timeline (pure: unit-tested in LifeClockTest)
// ---------------------------------------------------------------------------------------------------------

/** Timing of the lifeline motion (ms). The stamp shapes follow the stage-2 design; values only this file reads. */
private object LifeClockTiming {
    const val TRAVEL = 560f

    /** The shortest travel (at high auto-progress speeds). */
    const val TRAVEL_MIN = 240f
    // SEAL: drop spring, impact ring, hold, lift.
    const val SEAL_DAMPING = 0.42f
    const val SEAL_STIFFNESS = 520f
    const val SEAL_SCALE_FROM = 1.65f
    const val SEAL_TILT_FROM = -14f
    const val SEAL_TILT_REST = -8f
    const val SEAL_FADE_IN = 90f
    const val SEAL_RING = 300f
    const val SEAL_HOLD = 720f
    const val SEAL_LIFT = 260f
    // BIG_STAMP: glyphs on the StampSpec spring, underline, hold, absorb into the marker.
    const val GLYPH_FIRST = 60f
    const val GLYPH_GAP = 90f
    const val UNDERLINE = 300f
    const val BIG_HOLD = 420f
    const val ABSORB = 340f
    const val ABSORB_SCALE = 0.28f
    const val MARKER_RING = 260f
    // The marker hop after a seal impact or an absorbed stamp; under 3% of its size left after HOP.
    const val HOP_DAMPING = 0.38f
    const val HOP_STIFFNESS = 600f
    const val HOP = 300f
    const val SEAL_HOP = 0.32f
    const val BIG_HOP = 0.4f
    // ROLL: tag in, number roll, hold, fade.
    const val TAG_IN = 160f
    const val ROLL_DAMPING = 0.7f
    const val ROLL_STIFFNESS = 700f
    const val ROLL = 380f
    const val ROLL_HOLD = 900f
    const val TAG_OUT = 220f

    /** Longest a lifeline waits for a period turn (frame clock), in case no overlay is there to end it. */
    const val TURN_WAIT_CAP = 1_500L

    /** A glyph has landed when its spring first reaches its resting size (≈118 ms). */
    val GLYPH_LAND: Float = lifeClockSpringReach(1f, StampSpec.DAMPING, StampSpec.STIFFNESS)

    /** The seal hits the paper when its drop scale first comes within 2% of rest (≈95 ms). */
    val SEAL_IMPACT: Float = lifeClockSpringReach((SEAL_SCALE_FROM - 1.02f) / (SEAL_SCALE_FROM - 1f), SEAL_DAMPING, SEAL_STIFFNESS)
}

/**
 * Position of a spring released at 0 toward 1 with no initial velocity (unit mass), [tMs] after release: the same
 * damped-oscillator model Compose's spring() follows, as a pure function of one clock. 0 for t ≤ 0.
 */
internal fun lifeClockSpring(tMs: Float, dampingRatio: Float, stiffness: Float): Float {
    if (tMs <= 0f) return 0f
    val t = tMs / 1_000f
    val omega = sqrt(stiffness)
    return when {
        dampingRatio < 1f -> {
            val decay = dampingRatio * omega
            val damped = omega * sqrt(1f - dampingRatio * dampingRatio)
            1f - exp(-decay * t) * (cos(damped * t) + decay / damped * sin(damped * t))
        }
        dampingRatio == 1f -> 1f - exp(-omega * t) * (1f + omega * t)
        else -> {
            val root = sqrt(dampingRatio * dampingRatio - 1f)
            val r1 = -omega * (dampingRatio - root)
            val r2 = -omega * (dampingRatio + root)
            1f + (r2 * exp(r1 * t) - r1 * exp(r2 * t)) / (r1 - r2)
        }
    }
}

/** The first whole millisecond at which [lifeClockSpring] reaches [level] (capped at 2 s). */
internal fun lifeClockSpringReach(level: Float, dampingRatio: Float, stiffness: Float): Float {
    var t = 0f
    while (t < 2_000f && lifeClockSpring(t, dampingRatio, stiffness) < level) t += 1f
    return t
}

/**
 * One lifeline sequence, fixed when PeriodTrack sees a change: the marker travels from [fromIndex] to [toIndex] (when
 * the period changed), then [style]'s stamp plays. Every *At function is a pure function of the sequence clock tMs (ms
 * since the sequence started, after any period turn) and yieldAtMs (when stampYield turned true, or < 0): a yielding
 * stamp skips its hold and absorbs or fades within MotionTimeline.STAMP_YIELD_MS.
 * [glyphCount] is how many glyphs the big stamp drops (whitespace is only spacing; 0 = no label to stamp) and [rolls]
 * whether ROLL has a changed number to turn over.
 */
@Immutable
internal data class PeriodTrackPlan(
    val style: AgeStampStyle,
    val fromIndex: Int,
    val toIndex: Int,
    val glyphCount: Int,
    val rolls: Boolean,
    /** The auto-progress speed of an AUTO target (1 otherwise): the travel shrinks with it, like the period turn. */
    val speed: Int = 1,
) {
    private val hasStamp: Boolean get() = glyphCount > 0

    /**
     * The marker's travel time; 0 when the period stayed the same. On an AUTO screen at 4× a year lasts 750 ms, and
     * turn (280 ms) + a fixed 560 ms travel would outlast it, cutting the travel short so the next screen travelled it
     * again; so the travel scales with the speed (floored), as the turn does.
     */
    val travelMs: Float = when {
        fromIndex == toIndex || fromIndex < 0 -> 0f
        speed > 1 -> max(LifeClockTiming.TRAVEL_MIN, LifeClockTiming.TRAVEL / speed)
        else -> LifeClockTiming.TRAVEL
    }

    /** When the stamp itself starts: after the travel (ROLL's tag rides along with the marker during it). */
    val stampStartMs: Float = when {
        !hasStamp -> travelMs
        style == AgeStampStyle.ROLL -> max(travelMs, LifeClockTiming.TAG_IN)
        else -> travelMs
    }

    private val sealImpactMs: Float = stampStartMs + LifeClockTiming.SEAL_IMPACT
    private val lastGlyphLandMs: Float =
        stampStartMs + LifeClockTiming.GLYPH_FIRST + (glyphCount - 1).coerceAtLeast(0) * LifeClockTiming.GLYPH_GAP + LifeClockTiming.GLYPH_LAND
    private val absorbStartMs: Float = lastGlyphLandMs + LifeClockTiming.UNDERLINE + LifeClockTiming.BIG_HOLD
    private val arrivalMs: Float = absorbStartMs + LifeClockTiming.ABSORB
    private val rollEndMs: Float = stampStartMs + if (rolls) LifeClockTiming.ROLL else 0f

    /** The stamp lands here: trackShown is committed and (if not yielding) the beat haptic fires. */
    val commitMs: Float = when {
        !hasStamp -> travelMs
        style == AgeStampStyle.SEAL -> sealImpactMs
        style == AgeStampStyle.BIG_STAMP -> lastGlyphLandMs
        else -> rollEndMs
    }

    /** The beat haptic for the landing, or null when nothing lands (only the marker moved, or the number did not roll). */
    val beat: MotionHaptic? = when {
        !hasStamp -> null
        style == AgeStampStyle.ROLL -> if (rolls) MotionHaptic.TICK else null
        else -> MotionHaptic.CONTEXT
    }

    /** Everything has settled; from here PeriodTrack draws its resting line. */
    val totalMs: Float = when {
        !hasStamp -> travelMs
        style == AgeStampStyle.SEAL -> sealImpactMs + LifeClockTiming.SEAL_HOLD + LifeClockTiming.SEAL_LIFT
        style == AgeStampStyle.BIG_STAMP -> arrivalMs + LifeClockTiming.HOP
        else -> rollEndMs + LifeClockTiming.ROLL_HOLD + LifeClockTiming.TAG_OUT
    }

    /** The sequence ends here: [totalMs], or STAMP_YIELD_MS after a yield (whichever is first). */
    fun endMs(yieldAtMs: Float): Float =
        if (yieldAtMs < 0f) totalMs else min(totalMs, yieldAtMs + MotionTimeline.STAMP_YIELD_MS)

    /** When trackShown is committed: at the landing, or at the end of a yield that came before it. */
    fun commitAtMs(yieldAtMs: Float): Float = min(commitMs, endMs(yieldAtMs))

    /** 0 → 1 over the STAMP_YIELD_MS after a yield; 0 without one. */
    fun yieldAt(tMs: Float, yieldAtMs: Float): Float =
        if (yieldAtMs < 0f) 0f else ((tMs - yieldAtMs) / MotionTimeline.STAMP_YIELD_MS).coerceIn(0f, 1f)

    /** Marker travel: 0 = at [fromIndex], 1 = at [toIndex]. A yield finishes it within the yield time. */
    fun travelAt(tMs: Float, yieldAtMs: Float): Float {
        if (travelMs <= 0f) return 1f
        return max(stepProgress(tMs, 0f, travelMs, FastOutSlowInEasing), yieldAt(tMs, yieldAtMs))
    }

    /**
     * The marker ring's extra size (radius × (1 + hop)) after a seal impact or an absorbed stamp. A hop that has begun
     * runs on through a yield (snapping it would jump the ring); the sequence's end cuts what little is left.
     */
    fun hopAt(tMs: Float, yieldAtMs: Float): Float {
        if (!hasStamp || tMs >= endMs(yieldAtMs)) return 0f
        val (start, amplitude) = when (style) {
            AgeStampStyle.SEAL -> sealImpactMs to LifeClockTiming.SEAL_HOP
            AgeStampStyle.BIG_STAMP -> arrivalMs to LifeClockTiming.BIG_HOP
            else -> return 0f
        }
        val since = tMs - start
        if (since < 0f || since >= LifeClockTiming.HOP) return 0f
        return amplitude * (1f - lifeClockSpring(since, LifeClockTiming.HOP_DAMPING, LifeClockTiming.HOP_STIFFNESS))
    }

    /** The ink ring on the marker after the big stamp is absorbed: 0..1, or −1 when not drawn. */
    fun markerRingAt(tMs: Float, yieldAtMs: Float): Float {
        if (style != AgeStampStyle.BIG_STAMP || !hasStamp || tMs >= endMs(yieldAtMs)) return -1f
        val q = (tMs - arrivalMs) / LifeClockTiming.MARKER_RING
        return if (q < 0f || q >= 1f) -1f else q
    }

    /**
     * Opacity of the whole stamp (0 = nothing drawn); the big stamp's glyph opacity multiplies on top. A yield never lets
     * a stamp brighten: the seal and the tag fade from the opacity they had at that moment (so one that had not appeared
     * yet never does), and the big stamp absorbs.
     */
    fun stampAlphaAt(tMs: Float, yieldAtMs: Float): Float {
        if (!hasStamp || tMs >= endMs(yieldAtMs)) return 0f
        if (style == AgeStampStyle.BIG_STAMP) {
            return if (tMs < stampStartMs) 0f else 1f - ((absorbAt(tMs, yieldAtMs) - 0.6f) / 0.4f).coerceIn(0f, 1f)
        }
        val shown = if (yieldAtMs >= 0f) min(tMs, yieldAtMs) else tMs
        val alpha = when (style) {
            AgeStampStyle.SEAL -> {
                if (shown < stampStartMs) return 0f
                val fadeIn = ((shown - stampStartMs) / LifeClockTiming.SEAL_FADE_IN).coerceIn(0f, 1f)
                fadeIn * (1f - sealLiftAt(shown))
            }
            AgeStampStyle.ROLL -> {
                val tagIn = (shown / LifeClockTiming.TAG_IN).coerceIn(0f, 1f)
                val fadeOut = ((shown - rollEndMs - LifeClockTiming.ROLL_HOLD) / LifeClockTiming.TAG_OUT).coerceIn(0f, 1f)
                tagIn * (1f - fadeOut)
            }
            else -> 0f
        }
        return alpha * (1f - yieldAt(tMs, yieldAtMs))
    }

    // ---- SEAL ----

    /** The seal's drop: 0 at the start, 1 at rest (the spring passes 1 a little: the press). */
    fun sealDropAt(tMs: Float): Float =
        lifeClockSpring(tMs - stampStartMs, LifeClockTiming.SEAL_DAMPING, LifeClockTiming.SEAL_STIFFNESS)

    fun sealScaleAt(tMs: Float): Float = lerp(LifeClockTiming.SEAL_SCALE_FROM, 1f, sealDropAt(tMs))

    fun sealTiltAt(tMs: Float): Float = lerp(LifeClockTiming.SEAL_TILT_FROM, LifeClockTiming.SEAL_TILT_REST, sealDropAt(tMs))

    /** The expanding ink ring at the impact: 0..1, or −1 when not drawn. */
    fun sealRingAt(tMs: Float, yieldAtMs: Float): Float {
        if (style != AgeStampStyle.SEAL || !hasStamp || (yieldAtMs in 0f..sealImpactMs)) return -1f
        val q = (tMs - sealImpactMs) / LifeClockTiming.SEAL_RING
        return if (q < 0f || q >= 1f) -1f else FastOutSlowInEasing.transform(q)
    }

    /** The lift at the end: 0 resting, 1 gone (it rises 6dp while it fades). */
    fun sealLiftAt(tMs: Float): Float =
        stepProgress(tMs, sealImpactMs + LifeClockTiming.SEAL_HOLD, LifeClockTiming.SEAL_LIFT, LinearOutSlowInEasing)

    // ---- BIG_STAMP ----

    /**
     * Landing progress of visible glyph [index] on the StampSpec spring (0 before it drops). After a yield, glyphs that
     * were already falling land as they absorb; the ones that had not started never drop.
     */
    fun glyphAt(index: Int, tMs: Float, yieldAtMs: Float = -1f): Float {
        val start = stampStartMs + LifeClockTiming.GLYPH_FIRST + index * LifeClockTiming.GLYPH_GAP
        if (yieldAtMs >= 0f && start > yieldAtMs) return 0f
        return lifeClockSpring(tMs - start, StampSpec.DAMPING, StampSpec.STIFFNESS)
    }

    /** The gold underline under the number: 0..1 of its width. */
    fun underlineAt(tMs: Float): Float = stepProgress(tMs, lastGlyphLandMs, LifeClockTiming.UNDERLINE, FastOutSlowInEasing)

    /** Absorption into the marker: 0 in place, 1 shrunk into the ring. A yield runs it to 1 within the yield time. */
    fun absorbAt(tMs: Float, yieldAtMs: Float): Float =
        max(stepProgress(tMs, absorbStartMs, LifeClockTiming.ABSORB, FastOutLinearInEasing), yieldAt(tMs, yieldAtMs))

    // ---- ROLL ----

    /** The tag rises 4dp into place while it fades in: 1 = still low, 0 = in place. */
    fun tagLowAt(tMs: Float): Float = 1f - stepProgress(tMs, 0f, LifeClockTiming.TAG_IN, FastOutSlowInEasing)

    /** The number roll: 0 = old number in place, 1 = new number in place (the spring passes 1 a little). */
    fun rollAt(tMs: Float): Float =
        if (!rolls) 1f else lifeClockSpring(tMs - stampStartMs, LifeClockTiming.ROLL_DAMPING, LifeClockTiming.ROLL_STIFFNESS)

    companion object {
        /** The absorbed stamp's final scale. */
        const val ABSORB_SCALE = LifeClockTiming.ABSORB_SCALE
    }
}

/** From this auto-progress speed on, years pass too fast for a stamp; only the marker travels. */
internal const val FAST_AUTO_SPEED = 4

/**
 * The plan for a change from [from] to [to], or null when nothing moves (NONE, or neither a new period nor a label to
 * stamp). [yearLabel] is PeriodTrack's label for [to] ("3번째 해"); without one only the marker travels.
 */
internal fun periodTrackPlan(style: AgeStampStyle, from: LifeMark, to: LifeMark, yearLabel: String?): PeriodTrackPlan? {
    if (style == AgeStampStyle.NONE) return null
    val traveling = from.periodIndex >= 0 && from.periodIndex != to.periodIndex
    // At 4× auto progress a year lasts 750 ms, shorter than any stamp: it would be cut and restamped every year (with a
    // haptic each time). There only the marker moves; the track's own label still shows the new year.
    val fast = to.phase == PlayPhase.AUTO && to.speed >= FAST_AUTO_SPEED
    val glyphs = if (fast) 0 else yearStampGlyphs(yearLabel).count { it.stamped }
    if (!traveling && glyphs == 0) return null
    val number = yearStampNumber(yearLabel)
    return PeriodTrackPlan(
        style = style,
        fromIndex = if (traveling) from.periodIndex else to.periodIndex,
        toIndex = to.periodIndex,
        glyphCount = glyphs,
        rolls = number != null && number.digits != from.year.toString(),
        speed = if (to.phase == PlayPhase.AUTO) to.speed.coerceAtLeast(1) else 1,
    )
}

// ---------------------------------------------------------------------------------------------------------
// The year label (pure: unit-tested)
// ---------------------------------------------------------------------------------------------------------

/** One glyph of the big stamp. Digits are set [large]; whitespace is not [stamped] (it only keeps its width). */
@Immutable
internal data class YearStampGlyph(val text: String, val large: Boolean, val stamped: Boolean)

/** The big stamp's glyphs in label order ("3번째 해" → 3 · 번 · 째 · space · 해). Empty for a null or blank label. */
internal fun yearStampGlyphs(label: String?): List<YearStampGlyph> {
    if (label.isNullOrBlank()) return emptyList()
    val glyphs = ArrayList<YearStampGlyph>(label.length)
    var index = 0
    while (index < label.length) {
        val end = if (label[index].isHighSurrogate() && index + 1 < label.length) index + 2 else index + 1
        val text = label.substring(index, end)
        glyphs += YearStampGlyph(text, large = text[0].isDigit(), stamped = !text.isBlank())
        index = end
    }
    return glyphs
}

/** The number inside a year label: "3번째 해" → prefix "", digits "3", suffix "번째 해". */
@Immutable
internal data class YearStampNumber(val prefix: String, val digits: String, val suffix: String)

/** The first run of digits in [label], or null when there is none (then nothing rolls). */
internal fun yearStampNumber(label: String?): YearStampNumber? {
    if (label == null) return null
    val start = label.indexOfFirst { it.isDigit() }
    if (start < 0) return null
    var end = start
    while (end < label.length && label[end].isDigit()) end++
    return YearStampNumber(label.substring(0, start), label.substring(start, end), label.substring(end))
}

/** A paper speck inside the seal: at [angle] (radians), [distance] × the seal radius from its centre, [radius] in dp. */
@Immutable
internal data class YearStampSpeck(val angle: Float, val distance: Float, val radius: Float)

/** Six specks for the seal's imperfect print. Seeded by the mark's period and year, never by the game's random stream. */
internal fun yearStampSpecks(periodIndex: Int, year: Int): List<YearStampSpeck> {
    val random = Random(periodIndex * 1_000 + year)
    return List(6) {
        YearStampSpeck(
            angle = random.nextFloat() * 2f * PI.toFloat(),
            distance = 0.38f + random.nextFloat() * 0.5f,
            radius = 0.5f + random.nextFloat() * 0.6f,
        )
    }
}

/** [value] clamped to [low]..[high], or the middle of the two when the range is empty (a label wider than the track). */
internal fun lifeClockClamp(value: Float, low: Float, high: Float): Float =
    if (low > high) (low + high) / 2f else value.coerceIn(low, high)

// ---------------------------------------------------------------------------------------------------------
// PeriodTrack's motion
// ---------------------------------------------------------------------------------------------------------

/**
 * The lifeline's animated state for one change (marker travel, hop, year stamp), read only while drawing. Each change
 * gets a fresh instance whose first frame already shows the old state, so the final state never flashes first.
 */
@Stable
internal class PeriodTrackMotion(
    internal val plan: PeriodTrackPlan? = null,
    internal val art: PeriodTrackStampArt? = null,
) {
    /** The sequence clock (ms): < 0 while waiting for a period turn, +∞ once settled. Read only in draw. */
    internal val clockMs = mutableFloatStateOf(if (plan == null) Float.POSITIVE_INFINITY else -1f)

    /** When stampYield turned true on the sequence clock, or −1. Read only in draw. */
    internal val yieldAtMs = mutableFloatStateOf(-1f)

    /** Reused for the roll tag's pointer; created on first use. */
    internal val pointer: Path by lazy { Path() }

    companion object {
        /** Nothing moving: [drawPeriodTrackLine] draws today's static lifeline. */
        val Rest = PeriodTrackMotion()
    }
}

/**
 * What a stamp draws, measured once per change in composition (never per frame): glyph layouts, colours and specks.
 * Only the parts the plan's style needs are filled.
 */
internal class PeriodTrackStampArt(
    /** The decision screen's dark scene sits above the track: light glyphs on a dark halo. */
    val onScene: Boolean,
    // BIG_STAMP
    val glyphs: List<TextLayoutResult> = emptyList(),
    val glyphX: FloatArray = FloatArray(0),
    val labelWidth: Float = 0f,
    val capHeight: Float = 0f,
    val underlineStart: Float = 0f,
    val underlineWidth: Float = 0f,
    val glyphShadow: Shadow? = null,
    // SEAL
    val sealNumber: TextLayoutResult? = null,
    val sealSuffix: TextLayoutResult? = null,
    val sealSuffixScale: Float = 1f,
    val specks: List<YearStampSpeck> = emptyList(),
    // ROLL
    val rollPrefix: TextLayoutResult? = null,
    val rollOld: TextLayoutResult? = null,
    val rollNew: TextLayoutResult? = null,
    val rollSuffix: TextLayoutResult? = null,
) {
    val glyphColor: Color get() = if (onScene) LifeGamePaper else LifeGameInk
    val haloColor: Color get() = if (onScene) LifeGameNight.copy(alpha = 0.45f) else LifeGamePaper.copy(alpha = 0.9f)
}

/** Sizes of the lifeline motion that only this file reads. */
private object LifeClockGeometry {
    /** PeriodTrack's top padding (PlayChrome.kt): its Canvas starts this far below the track's top edge. */
    val TrackTop = 8.dp

    /** The seal's radius (Ø 60dp: room for "N번째 해" in two lines inside the inner ring). */
    val SealRadius = 30.dp
}

/**
 * The motion for one PeriodTrack. [yearLabel] is the label PeriodTrack shows ("3번째 해"); [stampYield] is true while
 * something more important needs the bottom of the screen (DecisionScreen passes `choicesVisible`): a running stamp then
 * skips its hold and absorbs or fades within MotionTimeline.STAMP_YIELD_MS, and no new stamp starts.
 * Rest outside PlayHost (no [LocalLifeClock]) and for a track that is not the clock's.
 */
@Composable
internal fun rememberPeriodTrackMotion(marks: List<PeriodMarkUi>, yearLabel: String?, stampYield: Boolean): PeriodTrackMotion {
    val clock = LocalLifeClock.current ?: return PeriodTrackMotion.Rest
    val mark = clock.mark
    val currentIndex = marks.indexOfFirst { it.isCurrent }
    if (currentIndex < 0 || mark.periodIndex != currentIndex) return PeriodTrackMotion.Rest
    val memory = clock.memory
    val style = LocalMotionStyle.current.ageStamp
    val reduceMotion = rememberReduceMotion()
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val motion = remember(memory, mark.runKey, mark.periodIndex, mark.year) {
        val from = memory.trackShown?.takeIf { it.runKey == mark.runKey }
        val plan = if (reduceMotion || stampYield || from == null || !mark.isAfter(from)) null
            else periodTrackPlan(style, memory.travelFrom(from), mark, yearLabel)
        if (plan == null) {
            PeriodTrackMotion.Rest
        } else {
            val art = periodTrackStampArt(plan, yearLabel, from!!.year, mark, measurer, density)
            PeriodTrackMotion(plan, art)
        }
    }
    val beat = rememberBeatHaptics()
    val yieldNow = rememberUpdatedState(stampYield)
    val reduceNow = rememberUpdatedState(reduceMotion)
    // One run per change (Rest is shared, so the place is part of the key); the clock captured here is that change's.
    LaunchedEffect(motion, memory, mark.runKey, mark.periodIndex, mark.year) {
        if (motion.plan == null) {
            memory.present(mark)
        } else {
            motion.play(turnSerial = clock.turn?.serial ?: 0, memory = memory, yieldNow = yieldNow, reduceNow = reduceNow,
                onArrive = { memory.arrive(mark) }, onCommit = { memory.present(mark) }, onBeat = beat)
        }
    }
    return motion
}

/**
 * Runs one sequence on the frame clock: wait for the period turn, then travel and stamp. [onCommit] fires once, when the
 * stamp lands (or the sequence ends early); a cancelled sequence (the track left the screen) never commits, so the next
 * track replays the same change. Reduced motion snaps to the end on the next frame.
 */
private suspend fun PeriodTrackMotion.play(
    turnSerial: Int,
    memory: LifeClockMemory,
    yieldNow: State<Boolean>,
    reduceNow: State<Boolean>,
    onArrive: () -> Unit,
    onCommit: () -> Unit,
    onBeat: (MotionHaptic) -> Unit,
) {
    val plan = plan ?: return
    var waited = 0L
    var last = -1L
    while (memory.finishedTurnSerial.intValue < turnSerial && waited < LifeClockTiming.TURN_WAIT_CAP && !reduceNow.value) {
        val now = withFrameMillis { it }
        if (last >= 0L) waited += now - last
        last = now
    }
    var committed = false
    var arrived = false
    // No new stamp over visible choices, and none under reduced motion: show the new state as it is.
    if (!reduceNow.value && !yieldNow.value) {
        var start = -1L
        var beaten = false
        while (true) {
            val now = withFrameMillis { it }
            if (start < 0L) start = now
            val t = (now - start).toFloat()
            if (reduceNow.value) break
            // The marker's travel is done: a track that replaces this one (a newer mark) starts from here.
            if (!arrived && t >= plan.travelMs) {
                arrived = true
                onArrive()
            }
            if (yieldAtMs.floatValue < 0f && yieldNow.value) yieldAtMs.floatValue = t
            val yieldAt = yieldAtMs.floatValue
            clockMs.floatValue = t
            val beatHaptic = plan.beat
            if (!beaten && beatHaptic != null && yieldAt < 0f && t >= plan.commitMs) {
                beaten = true
                onBeat(beatHaptic)
            }
            if (!committed && t >= plan.commitAtMs(yieldAt)) {
                committed = true
                onCommit()
            }
            if (t >= plan.endMs(yieldAt)) break
        }
    }
    // A sequence cut off before the marker arrived (장면 보기, the image preview) replays the whole change, travel
    // included, on the next track — by design. Travel at high auto speeds is short enough to arrive within a year.
    if (!committed) onCommit()
    clockMs.floatValue = Float.POSITIVE_INFINITY
}

/** A stamp's text size, capped like PeriodTrack's labels: it grows with the font scale only up to 1.3×. */
private fun Density.lifeClockSp(size: Float): TextUnit = (size * min(fontScale, 1.3f) / fontScale).sp

/** Measures what [plan]'s stamp draws for [label] (the new mark's "N번째 해"); [fromYear] is the number it rolls from. */
private fun periodTrackStampArt(
    plan: PeriodTrackPlan,
    label: String?,
    fromYear: Int,
    mark: LifeMark,
    measurer: TextMeasurer,
    density: Density,
): PeriodTrackStampArt? {
    if (plan.glyphCount == 0 || label == null) return null
    val onScene = mark.phase == PlayPhase.DECISION
    return with(density) {
        when (plan.style) {
            AgeStampStyle.BIG_STAMP -> {
                val large = SpanStyle(fontSize = lifeClockSp(50f))
                val small = SpanStyle(fontSize = lifeClockSp(22f))
                val base = TextStyle(fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold)
                val glyphs = yearStampGlyphs(label)
                val whole = measurer.measure(
                    AnnotatedString.Builder().apply {
                        glyphs.forEach { glyph -> pushStyle(if (glyph.large) large else small); append(glyph.text); pop() }
                    }.toAnnotatedString(),
                    style = base,
                    softWrap = false,
                    maxLines = 1,
                )
                val layouts = ArrayList<TextLayoutResult>()
                val xs = ArrayList<Float>()
                var offset = 0
                var underlineStart = Float.NaN
                var underlineEnd = 0f
                glyphs.forEach { glyph ->
                    val x = whole.getHorizontalPosition(offset, usePrimaryDirection = true)
                    if (glyph.large) {
                        if (underlineStart.isNaN()) underlineStart = x
                        underlineEnd = whole.getHorizontalPosition(offset + glyph.text.length, usePrimaryDirection = true)
                    }
                    if (glyph.stamped) {
                        layouts += measurer.measure(glyph.text, base.merge(if (glyph.large) large else small), softWrap = false, maxLines = 1)
                        xs += x
                    }
                    offset += glyph.text.length
                }
                val width = whole.size.width.toFloat()
                val hasDigits = !underlineStart.isNaN()
                val largest = layouts.maxByOrNull { it.size.height }
                PeriodTrackStampArt(
                    onScene = onScene,
                    glyphs = layouts,
                    glyphX = xs.toFloatArray(),
                    labelWidth = width,
                    // Cap height of the large digits (≈ 0.7 em): where the halo centres and the absorb starts.
                    capHeight = (largest?.firstBaseline ?: 0f) * 0.62f,
                    underlineStart = if (hasDigits) underlineStart else 0f,
                    underlineWidth = if (hasDigits) underlineEnd - underlineStart else width,
                    glyphShadow = if (onScene) Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2.dp.toPx()), 6.dp.toPx()) else null,
                )
            }
            AgeStampStyle.SEAL -> {
                val number = yearStampNumber(label)
                val numberStyle = TextStyle(fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = lifeClockSp(24f))
                val suffixStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = lifeClockSp(11f))
                val suffixText = if (number == null) label.trim() else (number.prefix + number.suffix).trim()
                val numberLayout = number?.let { measurer.measure(it.digits, numberStyle, softWrap = false, maxLines = 1) }
                val suffixLayout = suffixText.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, suffixStyle, softWrap = false, maxLines = 1) }
                // The suffix sits just below the centre; keep it inside the inner ring's chord there.
                val inner = (LifeClockGeometry.SealRadius - 5.dp).toPx()
                val chordY = 5.dp.toPx() + (suffixLayout?.size?.height ?: 0) / 2f
                val chord = 2f * sqrt(max(inner * inner - chordY * chordY, 0f))
                val suffixWidth = suffixLayout?.size?.width?.toFloat() ?: 0f
                PeriodTrackStampArt(
                    onScene = onScene,
                    sealNumber = numberLayout,
                    sealSuffix = suffixLayout,
                    sealSuffixScale = if (suffixWidth > chord && suffixWidth > 0f) chord / suffixWidth else 1f,
                    specks = yearStampSpecks(mark.periodIndex, mark.year),
                )
            }
            AgeStampStyle.ROLL -> {
                val style = TextStyle(fontWeight = FontWeight.Bold, fontSize = lifeClockSp(13f))
                val number = yearStampNumber(label)
                fun measure(text: String) = text.takeIf { it.isNotEmpty() }?.let { measurer.measure(it, style, softWrap = false, maxLines = 1) }
                if (number == null) {
                    PeriodTrackStampArt(onScene = onScene, rollNew = measure(label))
                } else {
                    PeriodTrackStampArt(
                        onScene = onScene,
                        rollPrefix = measure(number.prefix),
                        rollOld = if (plan.rolls) measure(fromYear.toString()) else null,
                        rollNew = measure(number.digits),
                        rollSuffix = measure(number.suffix),
                    )
                }
            }
            AgeStampStyle.NONE -> null
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// Drawing
// ---------------------------------------------------------------------------------------------------------

/**
 * PeriodTrack's lifeline -O-O-O-, drawn into its 24dp Canvas. At rest (and with [PeriodTrackMotion.Rest]) this is exactly
 * the drawing PeriodTrack had before stage 2. While a change plays, the marker travels and hops and the year stamp is
 * drawn above the Canvas (no ancestor clips). Animated values are read only here, in the draw phase.
 */
internal fun DrawScope.drawPeriodTrackLine(marks: List<PeriodMarkUi>, currentIndex: Int, motion: PeriodTrackMotion) {
    if (marks.isEmpty()) return
    val plan = motion.plan
    if (plan == null || plan.toIndex != currentIndex) {
        drawRestingTrackLine(marks, currentIndex)
        return
    }
    val clock = motion.clockMs.floatValue
    val yieldAt = motion.yieldAtMs.floatValue
    if (clock >= plan.endMs(yieldAt)) {
        drawRestingTrackLine(marks, currentIndex)
        return
    }
    // Waiting for a period turn draws the sequence's first frame: the old state.
    val t = max(clock, 0f)
    val cellWidth = size.width / marks.size
    fun cx(index: Int) = cellWidth * (index + 0.5f)
    val travel = plan.travelAt(t, yieldAt)
    val ringX = lerp(cx(plan.fromIndex), cx(plan.toIndex), travel)
    drawMovingTrackLine(marks, plan, ringX, travel, plan.hopAt(t, yieldAt))
    val ring = Offset(ringX, size.height / 2f)
    plan.markerRingAt(t, yieldAt).takeIf { it >= 0f }?.let { q ->
        drawCircle(
            LifeGameSpecial.copy(alpha = 0.3f * (1f - q)),
            radius = lerp(9.dp.toPx(), 22.dp.toPx(), q),
            center = ring,
            style = Stroke(2.dp.toPx()),
        )
    }
    val art = motion.art ?: return
    when (plan.style) {
        AgeStampStyle.SEAL -> drawSealStamp(plan, art, t, yieldAt, cx(plan.toIndex))
        AgeStampStyle.BIG_STAMP -> drawBigStamp(plan, art, t, yieldAt, cx(plan.toIndex), ring)
        AgeStampStyle.ROLL -> drawRollTag(plan, art, motion.pointer, t, yieldAt, ringX)
        AgeStampStyle.NONE -> Unit
    }
}

/** Today's static lifeline, verbatim from PlayChrome.kt before stage 2. */
private fun DrawScope.drawRestingTrackLine(marks: List<PeriodMarkUi>, currentIndex: Int) {
    // 원은 아래 이름 칸의 한가운데에 놓는다(이름과 원이 어긋나지 않게).
    val y = size.height / 2f
    val cellWidth = size.width / marks.size
    fun cx(index: Int) = cellWidth * (index + 0.5f)
    val small = 6.dp.toPx()
    val big = 9.dp.toPx()
    val line = 3.dp.toPx()
    // 원과 원 사이를 잇는 선. 지나온 구간은 진하게, 남은 구간은 옅게.
    for (index in 0 until marks.lastIndex) {
        val passed = currentIndex >= 0 && index < currentIndex
        val startX = cx(index) + (if (index == currentIndex) big else small)
        val endX = cx(index + 1) - (if (index + 1 == currentIndex) big else small)
        if (endX > startX) {
            drawLine(
                color = if (passed) LifeGameInk else LifeGameRule,
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = line,
            )
        }
    }
    marks.forEachIndexed { index, mark ->
        val center = Offset(cx(index), y)
        when {
            mark.isCurrent -> {
                drawCircle(LifeGamePaper, radius = big, center = center)
                drawCircle(LifeGameSpecial, radius = big - 1.5.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
                drawCircle(LifeGameSpecial, radius = big * 0.42f, center = center)
            }
            index < currentIndex -> drawCircle(LifeGameInk, radius = small, center = center)
            else -> {
                drawCircle(LifeGamePaper, radius = small, center = center)
                drawCircle(LifeGameRule, radius = small - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
            }
        }
    }
}

/**
 * The lifeline while the marker moves: the line is ink behind the ring and rule ahead of it; the dot the ring left takes
 * its passed style (growing 0.6 → 1), dots the ring has passed turn ink, the target keeps its future style until the
 * ring covers it. With no travel this is the resting line with a hopping ring.
 */
private fun DrawScope.drawMovingTrackLine(marks: List<PeriodMarkUi>, plan: PeriodTrackPlan, ringX: Float, travel: Float, hop: Float) {
    val y = size.height / 2f
    val cellWidth = size.width / marks.size
    fun cx(index: Int) = cellWidth * (index + 0.5f)
    val small = 6.dp.toPx()
    val big = 9.dp.toPx()
    val line = 3.dp.toPx()
    val from = plan.fromIndex
    val to = plan.toIndex
    for (index in 0 until marks.lastIndex) {
        // The ring's paper fill hides the line under it, so every gap is a small dot's.
        val startX = cx(index) + small
        val endX = cx(index + 1) - small
        if (endX <= startX) continue
        when {
            endX <= ringX -> drawLine(LifeGameInk, Offset(startX, y), Offset(endX, y), strokeWidth = line)
            startX >= ringX -> drawLine(LifeGameRule, Offset(startX, y), Offset(endX, y), strokeWidth = line)
            else -> {
                drawLine(LifeGameInk, Offset(startX, y), Offset(ringX, y), strokeWidth = line)
                drawLine(LifeGameRule, Offset(ringX, y), Offset(endX, y), strokeWidth = line)
            }
        }
    }
    marks.indices.forEach { index ->
        val center = Offset(cx(index), y)
        val passed = index < from || (index in from until to && ringX >= center.x)
        when {
            index == to && from == to -> Unit // the ring below
            passed -> {
                val grow = if (index == from && from != to) 0.6f + 0.4f * travel else 1f
                drawCircle(LifeGameInk, radius = small * grow, center = center)
            }
            else -> {
                drawCircle(LifeGamePaper, radius = small, center = center)
                drawCircle(LifeGameRule, radius = small - 1.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
            }
        }
    }
    val ring = Offset(ringX, y)
    val radius = big * (1f + hop)
    drawCircle(LifeGamePaper, radius = radius, center = ring)
    drawCircle(LifeGameSpecial, radius = radius - 1.5.dp.toPx(), center = ring, style = Stroke(3.dp.toPx()))
    drawCircle(LifeGameSpecial, radius = radius * 0.42f, center = ring)
}

/** SEAL: a red "N번째 해" seal thumps down above the marker, rings once, holds and lifts away. */
private fun DrawScope.drawSealStamp(plan: PeriodTrackPlan, art: PeriodTrackStampArt, t: Float, yieldAt: Float, targetX: Float) {
    val radius = LifeClockGeometry.SealRadius.toPx()
    val margin = 10.dp.toPx()
    val x = lifeClockClamp(targetX, radius - margin, size.width - radius + margin)
    val y = -(LifeClockGeometry.TrackTop + 8.dp).toPx() - radius
    val impact = Offset(x, y)
    plan.sealRingAt(t, yieldAt).takeIf { it >= 0f }?.let { q ->
        drawCircle(
            LifeGameSpecial.copy(alpha = 0.28f * (1f - q) * (1f - plan.yieldAt(t, yieldAt))),
            radius = radius * (1f + 0.55f * q),
            center = impact,
            style = Stroke(2.dp.toPx()),
        )
    }
    val alpha = plan.stampAlphaAt(t, yieldAt)
    if (alpha <= 0f) return
    val center = Offset(x, y - 6.dp.toPx() * plan.sealLiftAt(t))
    val scale = plan.sealScaleAt(t)
    withTransform({
        rotate(plan.sealTiltAt(t), center)
        scale(scale, scale, center)
    }) {
        drawCircle(LifeGamePaper.copy(alpha = 0.94f * alpha), radius = radius, center = center)
        drawCircle(LifeGameSpecial.copy(alpha = alpha), radius = radius - 1.25.dp.toPx(), center = center, style = Stroke(2.5.dp.toPx()))
        drawCircle(LifeGameSpecial.copy(alpha = alpha), radius = radius - 4.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
        val number = art.sealNumber
        number?.let {
            drawText(it, color = LifeGameSpecial, topLeft = Offset(center.x - it.size.width / 2f, center.y + 3.dp.toPx() - it.firstBaseline), alpha = alpha)
        }
        art.sealSuffix?.let { suffix ->
            val top = if (number != null) center.y + 5.dp.toPx() else center.y - suffix.size.height / 2f
            val topLeft = Offset(center.x - suffix.size.width / 2f, top)
            val pivot = Offset(center.x, top + suffix.size.height / 2f)
            withTransform({ scale(art.sealSuffixScale, art.sealSuffixScale, pivot) }) {
                drawText(suffix, color = LifeGameSpecial, topLeft = topLeft, alpha = alpha)
            }
        }
        art.specks.forEach { speck ->
            val reach = speck.distance * radius
            drawCircle(
                LifeGamePaper.copy(alpha = alpha),
                radius = speck.radius.dp.toPx(),
                center = Offset(center.x + cos(speck.angle) * reach, center.y + sin(speck.angle) * reach),
            )
        }
    }
}

/**
 * BIG_STAMP: "3번째 해" stamped glyph by glyph above the track (the lobby title's stamp), underlined in gold, then
 * absorbed into the marker.
 */
private fun DrawScope.drawBigStamp(plan: PeriodTrackPlan, art: PeriodTrackStampArt, t: Float, yieldAt: Float, targetX: Float, ring: Offset) {
    val glyphs = art.glyphs
    if (glyphs.isEmpty()) return
    val alpha = plan.stampAlphaAt(t, yieldAt)
    if (alpha <= 0f) return
    val width = art.labelWidth
    val edge = 16.dp.toPx()
    val centerX = lifeClockClamp(targetX, width / 2f + edge, size.width - width / 2f - edge)
    val left = centerX - width / 2f
    val baseline = -(LifeClockGeometry.TrackTop + 22.dp).toPx()
    val labelCenter = Offset(centerX, baseline - art.capHeight / 2f)
    val absorb = plan.absorbAt(t, yieldAt)
    val scale = lerp(1f, PeriodTrackPlan.ABSORB_SCALE, absorb)
    val shift = (ring - labelCenter) * absorb
    // The halo inks in with the first glyph and keeps the label legible over the scene or the year log. It stops at the
    // track's top edge: the lifeline itself stays crisp under it.
    val haloAlpha = StampSpec.alphaAt(plan.glyphAt(0, t, yieldAt)) * alpha
    if (haloAlpha > 0f) {
        val haloRadius = width / 2f + 28.dp.toPx()
        val halo = art.haloColor
        clipRect(left = -size.width, top = -size.height - haloRadius * 3f, right = size.width * 2f, bottom = -LifeClockGeometry.TrackTop.toPx()) {
            withTransform({
                translate(shift.x, shift.y)
                scale(scale, scale, labelCenter)
            }) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(halo.copy(alpha = halo.alpha * haloAlpha), Color.Transparent),
                        center = labelCenter,
                        radius = haloRadius,
                    ),
                    radius = haloRadius,
                    center = labelCenter,
                )
            }
        }
    }
    withTransform({
        translate(shift.x, shift.y)
        scale(scale, scale, labelCenter)
    }) {
        glyphs.forEachIndexed { index, glyph ->
            val p = plan.glyphAt(index, t, yieldAt)
            val glyphAlpha = StampSpec.alphaAt(p) * alpha
            if (glyphAlpha <= 0f) return@forEachIndexed
            val topLeft = Offset(left + art.glyphX[index], baseline - glyph.firstBaseline)
            val pivot = Offset(
                topLeft.x + glyph.size.width * StampSpec.Pivot.pivotFractionX,
                topLeft.y + glyph.size.height * StampSpec.Pivot.pivotFractionY,
            )
            withStampTransform(p, index, pivot) {
                drawText(glyph, color = art.glyphColor, topLeft = topLeft, alpha = glyphAlpha, shadow = art.glyphShadow)
            }
        }
        val underline = plan.underlineAt(t)
        if (underline > 0f && art.underlineWidth > 0f) {
            drawRect(
                LifeGameGold.copy(alpha = alpha),
                topLeft = Offset(left + art.underlineStart, baseline + 4.dp.toPx()),
                size = Size(art.underlineWidth * underline, 2.5.dp.toPx()),
            )
        }
    }
}

/** ROLL: a small tag riding on the marker; its number turns over to the new year. */
private fun DrawScope.drawRollTag(plan: PeriodTrackPlan, art: PeriodTrackStampArt, pointer: Path, t: Float, yieldAt: Float, ringX: Float) {
    val new = art.rollNew ?: return
    val alpha = plan.stampAlphaAt(t, yieldAt)
    if (alpha <= 0f) return
    val old = art.rollOld
    val prefixWidth = art.rollPrefix?.size?.width?.toFloat() ?: 0f
    val suffixWidth = art.rollSuffix?.size?.width?.toFloat() ?: 0f
    val numberWidth = max(old?.size?.width ?: 0, new.size.width).toFloat()
    val padding = 10.dp.toPx()
    val height = 26.dp.toPx()
    val notch = 5.dp.toPx()
    val width = prefixWidth + numberWidth + suffixWidth + padding * 2f
    val margin = 10.dp.toPx()
    val centerX = lifeClockClamp(ringX, width / 2f - margin, size.width - width / 2f + margin)
    val left = centerX - width / 2f
    // The pointer's tip stops 2dp above the marker ring; the tag starts 4dp lower and rises into place.
    val tipY = size.height / 2f - 9.dp.toPx() - 2.dp.toPx() + 4.dp.toPx() * plan.tagLowAt(t)
    val bottom = tipY - notch
    val top = bottom - height
    val corner = CornerRadius(height / 2f)
    drawRoundRect(Color.Black.copy(alpha = 0.08f * alpha), topLeft = Offset(left, top + 2.dp.toPx()), size = Size(width, height), cornerRadius = corner)
    drawRoundRect(LifeGameCard.copy(alpha = alpha), topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner)
    drawRoundRect(LifeGameRule.copy(alpha = alpha), topLeft = Offset(left, top), size = Size(width, height), cornerRadius = corner, style = Stroke(1.dp.toPx()))
    val tipX = lifeClockClamp(ringX, left + height / 2f, left + width - height / 2f)
    pointer.reset()
    pointer.moveTo(tipX - notch, bottom - 1.dp.toPx())
    pointer.lineTo(tipX + notch, bottom - 1.dp.toPx())
    pointer.lineTo(tipX, tipY)
    pointer.close()
    drawPath(pointer, LifeGameCard.copy(alpha = alpha))
    val edge = 1.dp.toPx()
    drawLine(LifeGameRule.copy(alpha = alpha), Offset(tipX - notch, bottom - edge / 2f), Offset(tipX, tipY), strokeWidth = edge)
    drawLine(LifeGameRule.copy(alpha = alpha), Offset(tipX + notch, bottom - edge / 2f), Offset(tipX, tipY), strokeWidth = edge)
    val textTop = top + (height - new.size.height) / 2f
    var x = left + padding
    art.rollPrefix?.let {
        drawText(it, color = LifeGameInk, topLeft = Offset(x, textTop), alpha = alpha)
        x += prefixWidth
    }
    val roll = plan.rollAt(t)
    val travel = 14.dp.toPx()
    clipRect(left, top + edge, left + width, bottom - edge) {
        if (old != null && roll < 1f) {
            drawText(old, color = LifeGameInk, topLeft = Offset(x, textTop - roll * travel), alpha = alpha * (1f - roll).coerceIn(0f, 1f))
        }
        drawText(new, color = LifeGameInk, topLeft = Offset(x, textTop + (1f - roll) * travel), alpha = alpha * roll.coerceIn(0f, 1f))
    }
    x += new.size.width
    art.rollSuffix?.let { drawText(it, color = LifeGameInk, topLeft = Offset(x, textTop), alpha = alpha) }
}
