package com.example.lifegame.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.StartingRoll
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.components.AppearancePopStyle
import com.example.lifegame.ui.components.EaseOutBack
import com.example.lifegame.ui.components.GlyphDigits
import com.example.lifegame.ui.components.GlyphStampTiming
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.ProfileEntranceStyle
import com.example.lifegame.ui.components.RiseEasing
import com.example.lifegame.ui.components.StampSpec
import com.example.lifegame.ui.components.StatCountUpStyle
import com.example.lifegame.ui.components.glyphStamp
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.rememberOneShotClock
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.components.stepProgress
import com.example.lifegame.ui.components.withStampTransform
import com.example.lifegame.ui.startingStatRows
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * ProfileMotion — the stage-2 presentation of the 새 인생 (profile) screen (bookends, 2026-11-26):
 * - profileEntrance: the step groups enter one after another (the title stamped in for STAMP_RISE).
 * - appearancePop: the four age previews pop once the new look is really drawn.
 * - statCountUp: after the dice land, the stat rows count up to their final values.
 * Presentation only: no callback, enabled flag, rolling lock or random stream changes. Every animated value is read in
 * a layer or draw lambda, and under reduced motion (for the entrance also under touch exploration) every variant rests
 * in its final state from the first frame.
 */

// ---------------------------------------------------------------------------------------------------------
// profileEntrance
// ---------------------------------------------------------------------------------------------------------

/** Timing of the profile entrance, in ms on one clock that starts with the screen. */
internal object ProfileEntranceTimeline {
    /** Step groups, top to bottom: title · subtitle(+target) · name · gender · dice · challenge+appearance+photo. */
    const val GROUP_COUNT = 6
    const val TITLE_GROUP = 0
    /** Holds the GL dice stage once a roll exists: it rests from then on (never faded or moved). */
    const val DICE_GROUP = 4
    /** The start button enters with the last group. */
    const val BUTTON_GROUP = 5

    /** NavHost cross-fades destinations for 700 ms, so the first step waits a beat. */
    const val LEAD_MS = 100
    const val RISE_GAP_MS = 70
    const val RISE_MS = 420
    val Rise: Dp = 16.dp
    val ButtonRise: Dp = 10.dp

    /** STAMP_RISE: the title glyphs are stamped in, then groups 1–5 rise from here on. */
    const val STAMP_GROUPS_START_MS = 220
    val TitleStamp = GlyphStampTiming(startMs = 160f, gapMs = 90f, durationMs = 300f)

    const val UNFOLD_GAP_MS = 80
    const val UNFOLD_MS = 520
    /** A folded sheet starts tilted this far back around its top edge (UNFOLD). */
    const val UNFOLD_DEGREES = -78f
    /** Perspective of the fold, as graphicsLayer cameraDistance (× density). */
    const val UNFOLD_CAMERA_DISTANCE = 14f
    /** Swings a little (about 2%) past flat, then settles. */
    val UnfoldEasing = CubicBezierEasing(0.2f, 1.25f, 0.4f, 1f)

    fun startMs(style: ProfileEntranceStyle, group: Int): Float = when (style) {
        ProfileEntranceStyle.NONE -> 0f
        ProfileEntranceStyle.RISE -> (LEAD_MS + group * RISE_GAP_MS).toFloat()
        ProfileEntranceStyle.STAMP_RISE -> if (group == TITLE_GROUP) TitleStamp.startMs
            else (STAMP_GROUPS_START_MS + (group - 1) * RISE_GAP_MS).toFloat()
        ProfileEntranceStyle.UNFOLD -> (LEAD_MS + group * UNFOLD_GAP_MS).toFloat()
    }

    fun durationMs(style: ProfileEntranceStyle): Float = when (style) {
        ProfileEntranceStyle.NONE -> 0f
        ProfileEntranceStyle.RISE, ProfileEntranceStyle.STAMP_RISE -> RISE_MS.toFloat()
        ProfileEntranceStyle.UNFOLD -> UNFOLD_MS.toFloat()
    }

    /**
     * When every step of [style] has ended, for a title of [titleGlyphs] visible glyphs. NONE reports the longest
     * entrance, so a clock created at rest never starts running if the style is switched later (debug comparison).
     */
    fun totalMs(style: ProfileEntranceStyle, titleGlyphs: Int): Int {
        if (style == ProfileEntranceStyle.NONE) {
            return ProfileEntranceStyle.entries.filter { it != ProfileEntranceStyle.NONE }.maxOf { totalMs(it, titleGlyphs) }
        }
        val groups = (0 until GROUP_COUNT).maxOf { startMs(style, it) + durationMs(style) }
        val title = if (style == ProfileEntranceStyle.STAMP_RISE) TitleStamp.endMs(titleGlyphs) else 0f
        return maxOf(groups, title).roundToInt()
    }
}

/** One profile entrance: the style and its clock (ms), which is read only while drawing. */
@Stable
internal class ProfileEntranceState(val style: ProfileEntranceStyle, val clock: State<Float>)

/**
 * The entrance of one profile screen. It plays once per screen instance (a new life, or HOME → PROFILE): rotation and
 * process restore bring back the saved "played" flag and show the screen settled. It rests for NONE, reduced motion
 * and touch exploration.
 */
@Composable
internal fun rememberProfileEntrance(titleGlyphs: Int): ProfileEntranceState {
    val style = LocalMotionStyle.current.profileEntrance
    val played = rememberSaveable { mutableStateOf(false) }
    // Read once without observing: marking it played must not recompose the whole screen mid-entrance.
    val playsNow = remember { !Snapshot.withoutReadObservation { played.value } }
    LaunchedEffect(Unit) { played.value = true }
    val allowed = rememberEntranceMotionAllowed()
    val clock = rememberOneShotClock(
        key = Unit,
        active = playsNow && allowed && style != ProfileEntranceStyle.NONE,
        totalMs = ProfileEntranceTimeline.totalMs(style, titleGlyphs),
    )
    return remember(style, clock) { ProfileEntranceState(style, clock) }
}

/** Opacity of a step at clock [clockMs]: RISE / STAMP_RISE follow the rise curve, UNFOLD is inked well before it lands. */
internal fun profileEntranceAlpha(style: ProfileEntranceStyle, clockMs: Float, startMs: Float, durationMs: Float): Float =
    when (style) {
        ProfileEntranceStyle.NONE -> 1f
        ProfileEntranceStyle.RISE, ProfileEntranceStyle.STAMP_RISE -> stepProgress(clockMs, startMs, durationMs, RiseEasing)
        ProfileEntranceStyle.UNFOLD -> (stepProgress(clockMs, startMs, durationMs) * 2.2f).coerceAtMost(1f)
    }.coerceIn(0f, 1f)

/**
 * Writes into [target] the projection of a sheet tilted [degrees] around its top edge (x axis through `y = 0`), seen
 * from [cameraDistancePx] away: y shrinks by cos θ and the far/near edge narrows/widens by perspective. Pivot at the
 * top centre is applied by the caller. Identity at 0°.
 */
internal fun profileUnfoldMatrix(target: Matrix, degrees: Float, cameraDistancePx: Float) {
    val radians = degrees / 180f * PI.toFloat()
    target.reset()
    target.values[Matrix.ScaleY] = cos(radians)
    target.values[Matrix.Perspective1] = if (cameraDistancePx > 0f) sin(radians) / cameraDistancePx else 0f
}

/**
 * The entrance of profile step [group] (or, with [button], of the start button that follows group 5).
 * Opacity comes from a layer; the movement (the rise, or the UNFOLD fold) is drawn only, so layout, semantics and hit
 * areas stay put and a tap during the entrance lands where it is aimed (the groups hold fields, cards and buttons).
 * [rest] pins the resting state whatever the clock says (the dice group once it holds the GL stage). NONE adds nothing,
 * and the STAMP_RISE title is stamped by [profileTitleStamp] instead.
 */
internal fun Modifier.profileEntranceStep(
    entrance: ProfileEntranceState,
    group: Int,
    rest: Boolean = false,
    button: Boolean = false,
): Modifier {
    val style = entrance.style
    if (style == ProfileEntranceStyle.NONE) return this
    if (style == ProfileEntranceStyle.STAMP_RISE && group == ProfileEntranceTimeline.TITLE_GROUP && !button) return this
    val clock = entrance.clock
    val start = ProfileEntranceTimeline.startMs(style, group)
    val duration = ProfileEntranceTimeline.durationMs(style)
    val rise = if (button) ProfileEntranceTimeline.ButtonRise else ProfileEntranceTimeline.Rise
    return graphicsLayer {
        alpha = if (rest) 1f else profileEntranceAlpha(style, clock.value, start, duration)
        // Per-draw alpha: no offscreen buffer, so the drawn-only offset below is never clipped to the bounds.
        compositingStrategy = CompositingStrategy.ModulateAlpha
    }.drawWithCache {
        val fold = Matrix()
        val camera = ProfileEntranceTimeline.UNFOLD_CAMERA_DISTANCE * PROFILE_CAMERA_INCH * density
        onDrawWithContent {
            if (rest) {
                drawContent()
                return@onDrawWithContent
            }
            val t = clock.value
            when (style) {
                ProfileEntranceStyle.UNFOLD -> {
                    val degrees = ProfileEntranceTimeline.UNFOLD_DEGREES *
                        (1f - stepProgress(t, start, duration, ProfileEntranceTimeline.UnfoldEasing))
                    if (degrees == 0f) {
                        drawContent()
                    } else {
                        profileUnfoldMatrix(fold, degrees, camera)
                        val content = this
                        withTransform({
                            translate(size.width / 2f, 0f)
                            transform(fold)
                            translate(-size.width / 2f, 0f)
                        }) { content.drawContent() }
                    }
                }
                else -> {
                    val dy = (1f - stepProgress(t, start, duration, RiseEasing)) * rise.toPx()
                    if (dy == 0f) drawContent() else {
                        val content = this
                        translate(0f, dy) { content.drawContent() }
                    }
                }
            }
        }
    }
}

/** STAMP_RISE: stamps the "새 인생" glyphs in on the entrance clock (draw only; the heading Text is untouched). */
internal fun Modifier.profileTitleStamp(entrance: ProfileEntranceState, layout: State<TextLayoutResult?>): Modifier =
    if (entrance.style != ProfileEntranceStyle.STAMP_RISE) this
    else glyphStamp(layout, entrance.clock, ProfileEntranceTimeline.TitleStamp)

/** graphicsLayer cameraDistance is in inches of a 72-point camera: this many px per unit (× density). */
private const val PROFILE_CAMERA_INCH = 72f

// ---------------------------------------------------------------------------------------------------------
// appearancePop
// ---------------------------------------------------------------------------------------------------------

/** Timing of the pop of the four age previews (ms after the new look is drawn). */
internal object ProfileAppearancePopTimeline {
    const val AVATARS = 4
    const val BOUNCE_MS = 560
    /** WAVE: the bounce travels from infancy to old age. */
    const val WAVE_GAP_MS = 70
    const val INK_MS = 520
    const val INK_GAP_MS = 40
    const val INK_SCALE = 0.03f
    val WaveHop: Dp = 7.dp
    /** The pop waits for all four avatars to draw the new look (layers decode off the main thread), at most this long. */
    const val DRAWN_WAIT_MS = 300L

    fun startMs(style: AppearancePopStyle, index: Int): Float = when (style) {
        AppearancePopStyle.WAVE -> (index * WAVE_GAP_MS).toFloat()
        AppearancePopStyle.INK -> (index * INK_GAP_MS).toFloat()
        AppearancePopStyle.NONE, AppearancePopStyle.BOUNCE -> 0f
    }

    fun durationMs(style: AppearancePopStyle): Float = when (style) {
        AppearancePopStyle.NONE -> 0f
        AppearancePopStyle.INK -> INK_MS.toFloat()
        AppearancePopStyle.BOUNCE, AppearancePopStyle.WAVE -> BOUNCE_MS.toFloat()
    }

    fun totalMs(style: AppearancePopStyle): Int = (startMs(style, AVATARS - 1) + durationMs(style)).roundToInt()
}

/**
 * Squash, stretch and a damped settle around the feet (BOUNCE, and each WAVE avatar) at progress [p]:
 * squash to 1.07 × 0.90, stretch to 0.97 × 1.05, then settle. Exactly (1, 1) at 0 and from 1 on.
 */
internal fun profileAppearanceBounce(p: Float): ScaleFactor {
    val squashEnd = 0.18f
    val stretchEnd = 0.5f
    return when {
        p <= 0f || p >= 1f -> ScaleFactor(1f, 1f)
        p <= squashEnd -> {
            val u = EaseOut.transform(p / squashEnd)
            ScaleFactor(1f + 0.07f * u, 1f - 0.10f * u)
        }
        p <= stretchEnd -> {
            val u = EaseInOut.transform((p - squashEnd) / (stretchEnd - squashEnd))
            ScaleFactor(1.07f + (0.97f - 1.07f) * u, 0.90f + (1.05f - 0.90f) * u)
        }
        else -> {
            val e = (p - stretchEnd) / (1f - stretchEnd)
            val settle = (1f - e) * (1f - e) * cos(2.5f * PI.toFloat() * e)
            ScaleFactor(1f - 0.03f * settle, 1f + 0.05f * settle)
        }
    }
}

/** WAVE hop height as a fraction of [ProfileAppearancePopTimeline.WaveHop]: one arc during the stretch; exactly 0 at rest. */
internal fun profileAppearanceHop(p: Float): Float {
    val f = ((p - 0.15f) / 0.5f).coerceIn(0f, 1f)
    return if (f <= 0f || f >= 1f) 0f else sin(PI.toFloat() * f)
}

/**
 * The pop of the four age previews. It fires when the look options change and all four avatars report the new look
 * drawn (PlayerAvatar.onShown), or [ProfileAppearancePopTimeline.DRAWN_WAIT_MS] after the change, whichever is first;
 * never on the first display. NONE and reduced motion only swap the avatars, as before.
 */
@Stable
internal class ProfileAppearancePop internal constructor(
    val style: AppearancePopStyle,
    val clock: State<Float>,
    private val tracker: ProfileAppearanceTracker,
) {
    /** PlayerAvatar.onShown for avatar [index] (a stable lambda). */
    fun reporter(index: Int): (AvatarLook) -> Unit = tracker.reporters[index]
}

/** Plain (non-snapshot) bookkeeping of the looks requested and drawn; read only by the waiting effect. */
internal class ProfileAppearanceTracker(initial: List<AvatarLook>) {
    /** The requested looks the last pop was armed for; the first display is the baseline and never pops. */
    var armedFor: List<AvatarLook> = initial
    private val drawn = arrayOfNulls<AvatarLook>(initial.size)
    val reporters: List<(AvatarLook) -> Unit> = List(initial.size) { index -> { look: AvatarLook -> drawn[index] = look } }

    fun allDrawn(looks: List<AvatarLook>): Boolean = looks.indices.all { drawn.getOrNull(it) == looks[it] }
}

@Composable
internal fun rememberProfileAppearancePop(looks: List<AvatarLook>): ProfileAppearancePop {
    val style = LocalMotionStyle.current.appearancePop
    val animates = style != AppearancePopStyle.NONE && !rememberReduceMotion()
    val tracker = remember { ProfileAppearanceTracker(looks) }
    var popKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(looks, animates) {
        if (looks == tracker.armedFor) return@LaunchedEffect
        tracker.armedFor = looks
        if (!animates) return@LaunchedEffect
        // Frame clock, like the animation itself: wait until the new look is on screen, but not for long.
        val start = withFrameMillis { it }
        while (!tracker.allDrawn(looks) && withFrameMillis { it } - start < ProfileAppearancePopTimeline.DRAWN_WAIT_MS) Unit
        popKey++
    }
    val clock = rememberOneShotClock(popKey, active = popKey > 0 && animates, totalMs = ProfileAppearancePopTimeline.totalMs(style))
    return remember(style, clock, tracker) { ProfileAppearancePop(style, clock, tracker) }
}

/**
 * The pop of age preview [index]: a squash-and-stretch around the feet (BOUNCE; WAVE adds a hop and a stagger from
 * infancy to old age), or an ink ripple spreading behind the figure inside the card (INK). Layer and draw only; it
 * belongs at the call site, before the avatar's size (PlayerAvatar itself draws exactly as before).
 */
internal fun Modifier.profileAppearancePop(pop: ProfileAppearancePop, index: Int): Modifier {
    val style = pop.style
    if (style == AppearancePopStyle.NONE) return this
    val clock = pop.clock
    val start = ProfileAppearancePopTimeline.startMs(style, index)
    val duration = ProfileAppearancePopTimeline.durationMs(style)
    val feet = TransformOrigin(0.5f, 1f)
    if (style == AppearancePopStyle.INK) {
        return drawBehind {
            val q = stepProgress(clock.value, start, duration, FastOutSlowInEasing)
            if (q <= 0f || q >= 1f) return@drawBehind
            val radius = hypot(size.width, size.height) * (0.35f + 0.70f * q)
            val centre = Offset(size.width / 2f, size.height * 0.58f)
            val ink = LifeGameInk.copy(alpha = 0.14f * (1f - q))
            drawCircle(
                Brush.radialGradient(0f to ink, 0.7f to ink, 1f to Color.Transparent, center = centre, radius = radius),
                radius = radius,
                center = centre,
            )
        }.graphicsLayer {
            val p = stepProgress(clock.value, start, duration)
            val bump = if (p <= 0f || p >= 1f) 1f else 1f + ProfileAppearancePopTimeline.INK_SCALE * sin(PI.toFloat() * p)
            scaleX = bump
            scaleY = bump
            transformOrigin = feet
        }
    }
    return graphicsLayer {
        val p = stepProgress(clock.value, start, duration)
        val scale = profileAppearanceBounce(p)
        scaleX = scale.scaleX
        scaleY = scale.scaleY
        transformOrigin = feet
        if (style == AppearancePopStyle.WAVE) {
            translationY = -ProfileAppearancePopTimeline.WaveHop.toPx() * profileAppearanceHop(p)
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// statCountUp
// ---------------------------------------------------------------------------------------------------------

/** Timing of the stat rows after the dice land (ms after the start). Row i counts top to bottom. */
internal object ProfileStatCountTimeline {
    const val ROWS = 6
    const val TICK_GAP_MS = 60
    const val TICK_COUNT_MS = 340
    const val TICK_POP_MS = 120
    const val TICK_POP_SCALE = 0.14f
    const val SLOT_GAP_MS = 90
    const val SLOT_ROW_MS = 560
    const val SLOT_UNDERLINE_MS = 200
    val SlotUnderline: Dp = 2.dp
    const val STAMP_GAP_MS = 80
    const val STAMP_MS = 300
    const val STAMP_RING_FROM = 0.8f
    const val STAMP_RING_TO = 1.6f

    fun rowStartMs(style: StatCountUpStyle, row: Int): Float = when (style) {
        StatCountUpStyle.NONE -> 0f
        StatCountUpStyle.TICK -> (row * TICK_GAP_MS).toFloat()
        StatCountUpStyle.SLOT -> (row * SLOT_GAP_MS).toFloat()
        StatCountUpStyle.STAMP -> (row * STAMP_GAP_MS).toFloat()
    }

    /** From this clock time row [row] draws its real text again. */
    fun rowEndMs(style: StatCountUpStyle, row: Int): Float = rowStartMs(style, row) + when (style) {
        StatCountUpStyle.NONE -> 0
        StatCountUpStyle.TICK -> TICK_COUNT_MS + TICK_POP_MS
        StatCountUpStyle.SLOT -> SLOT_ROW_MS + SLOT_UNDERLINE_MS
        StatCountUpStyle.STAMP -> STAMP_MS
    }

    /** The whole count of [rows] rows; NONE reports the longest so a resting clock never starts later (debug switch). */
    fun totalMs(style: StatCountUpStyle, rows: Int = ROWS): Int =
        if (style == StatCountUpStyle.NONE) StatCountUpStyle.entries.filter { it != StatCountUpStyle.NONE }.maxOf { totalMs(it, rows) }
        else rowEndMs(style, rows - 1).roundToInt()
}

/** TICK: the value shown at count progress [p]: exactly [from] before the start and [to] from the end, rounded between. */
internal fun profileTickValue(from: Int, to: Int, p: Float): Int = when {
    p <= 0f -> from
    p >= 1f -> to
    else -> (from + (to - from) * p).roundToInt()
}

/**
 * SLOT: the position of the reel for decimal [place] (0 = ones) of a count from [from] to [to], at eased progress [e]
 * (EaseOutBack, so it clunks a little past the stop). The ones reel always turns forward one extra round; higher reels
 * move by the signed difference. The digit shown is [profileReelDigit] of it and lands on [to]'s digit at e = 1.
 */
internal fun profileSlotReel(from: Int, to: Int, place: Int, e: Float): Float {
    var unit = 1
    repeat(place) { unit *= 10 }
    val start = Math.floorMod(from / unit, 10)
    val steps = if (place == 0) 10 + Math.floorMod(to % 10 - start, 10) else to / unit - from / unit
    return start + steps * e
}

/** The digit a reel at [position] shows (the next one rolls in below it). */
internal fun profileReelDigit(position: Float): Int = Math.floorMod(floor(position).toInt(), 10)

/** Finals last on screen; a plain holder written after each composition, read when a new roll starts. */
internal class ProfileStatCountMemory {
    var finals: List<Int>? = null
}

/**
 * The count of one roll. Only the player's own roll counts (a challenge code that replaces it shows its values at once),
 * and only after the dice have really landed: Dice3DStage.onSettled, which waits for the GL settle (or the final static
 * pair) and the 490 ms roll floor. Until then the rows draw the values that were on screen before the roll.
 */
@Stable
internal class ProfileStatCountState internal constructor(
    val style: StatCountUpStyle,
    /** This roll counts: a style, motion on, a roll by the player that is still the one on screen. */
    val counting: Boolean,
    /** Counting, but the dice have not landed yet. */
    val waiting: Boolean,
    /** The finals on screen before the roll (null before the first roll: count from the base). */
    val from: List<Int>?,
    val clock: State<Float>,
    val digits: GlyphDigits,
    /** Dice3DStage.onSettled. */
    val onSettled: (Int) -> Unit,
)

/** [digitStyle] is the style of the row's final value (the formula's bold ink span). */
@Composable
internal fun rememberProfileStatCount(roll: StartingRoll?, rollNonce: Int, digitStyle: TextStyle): ProfileStatCountState {
    val style = LocalMotionStyle.current.statCountUp
    val reduceMotion = rememberReduceMotion()
    val memory = remember { ProfileStatCountMemory() }
    val finals = remember(roll) { roll?.let { rolled -> startingStatRows(rolled).map { it.finalValue } } }
    val from = remember(rollNonce) { memory.finals }
    SideEffect { memory.finals = finals }
    val rolled = remember(rollNonce) { roll }
    val counting = style != StatCountUpStyle.NONE && !reduceMotion && rollNonce > 0 && roll != null && roll == rolled
    var settledNonce by remember { mutableIntStateOf(0) }
    val landed = settledNonce == rollNonce
    val clock = rememberOneShotClock(
        key = rollNonce to landed,
        active = counting && landed,
        totalMs = ProfileStatCountTimeline.totalMs(style),
    )
    val measurer = rememberTextMeasurer()
    val digits = remember(measurer, digitStyle) { GlyphDigits(measurer, digitStyle) }
    val onSettled = remember { { nonce: Int -> settledNonce = nonce } }
    return ProfileStatCountState(style, counting, counting && !landed, from, clock, digits, onSettled)
}

/** Boxes of [finalValue]'s digits at the end of a formula's layout, or null when the text does not end with them on one line. */
internal fun profileFinalDigitBoxes(layout: TextLayoutResult, finalValue: Int): List<Rect>? {
    if (finalValue < 0) return null
    val text = layout.layoutInput.text.text
    val digits = finalValue.toString()
    if (!text.endsWith(digits)) return null
    val start = text.length - digits.length
    if (layout.getLineForOffset(start) != layout.getLineForOffset(text.length - 1)) return null
    return (start until text.length).map { layout.getBoundingBox(it) }
}

/**
 * Draws the count of stat row [row] over its formula Text (whose layout is [layout]). The Text keeps its final string,
 * its semantics and its own drawing outside the final number; only the number's glyphs are covered and painted:
 * TICK counts and pops, SLOT rolls one reel per digit, STAMP stamps the number in. At rest it is plain drawContent().
 */
internal fun Modifier.profileStatCount(
    count: ProfileStatCountState?,
    row: Int,
    base: Int,
    finalValue: Int,
    layout: State<TextLayoutResult?>,
): Modifier {
    if (count == null || count.style == StatCountUpStyle.NONE) return this
    return drawWithCache {
        val text = layout.value
        val boxes = text?.let { profileFinalDigitBoxes(it, finalValue) }
        val number = boxes?.takeIf { it.isNotEmpty() }?.let { digitBoxes ->
            Rect(digitBoxes.minOf { it.left }, digitBoxes.minOf { it.top }, digitBoxes.maxOf { it.right }, digitBoxes.maxOf { it.bottom })
        }
        val baseline = if (text != null && boxes != null) text.getLineBaseline(text.getLineForOffset(text.layoutInput.text.length - 1)) else 0f
        val stampPaint = Paint()
        onDrawWithContent {
            if (!count.counting || boxes == null || number == null) {
                drawContent()
                return@onDrawWithContent
            }
            val t = count.clock.value
            val from = count.from?.getOrNull(row) ?: base
            val start = ProfileStatCountTimeline.rowStartMs(count.style, row)
            when (count.style) {
                StatCountUpStyle.TICK -> {
                    val countP = stepProgress(t, start, ProfileStatCountTimeline.TICK_COUNT_MS.toFloat(), FastOutSlowInEasing)
                    val popQ = stepProgress(t, start + ProfileStatCountTimeline.TICK_COUNT_MS, ProfileStatCountTimeline.TICK_POP_MS.toFloat())
                    when {
                        count.waiting -> {
                            profileDrawWithout(number)
                            count.digits.drawNumber(this, from, number.right, baseline)
                        }
                        popQ >= 1f -> drawContent()
                        popQ > 0f -> {
                            profileDrawWithout(number)
                            val pop = 1f + ProfileStatCountTimeline.TICK_POP_SCALE * sin(PI.toFloat() * popQ)
                            val content = this
                            scale(pop, pop, number.center) {
                                clipRect(number.left, number.top, number.right, number.bottom) { content.drawContent() }
                            }
                        }
                        else -> {
                            profileDrawWithout(number)
                            count.digits.drawNumber(this, profileTickValue(from, finalValue, countP), number.right, baseline)
                        }
                    }
                }
                StatCountUpStyle.SLOT -> {
                    val p = if (count.waiting) 0f else stepProgress(t, start, ProfileStatCountTimeline.SLOT_ROW_MS.toFloat())
                    if (p < 1f) {
                        profileDrawWithout(number)
                        val e = EaseOutBack.transform(p)
                        boxes.forEachIndexed { k, box ->
                            val position = profileSlotReel(from, finalValue, place = boxes.size - 1 - k, e = e)
                            profileDrawReel(count.digits, box, baseline, position)
                        }
                    } else {
                        drawContent()
                        val q = stepProgress(t, start + ProfileStatCountTimeline.SLOT_ROW_MS, ProfileStatCountTimeline.SLOT_UNDERLINE_MS.toFloat())
                        if (q < 1f) {
                            val stroke = ProfileStatCountTimeline.SlotUnderline.toPx()
                            val y = number.bottom - stroke / 2f
                            drawLine(LifeGameGold.copy(alpha = 1f - q), Offset(number.left, y), Offset(number.right, y), stroke)
                        }
                    }
                }
                StatCountUpStyle.STAMP -> {
                    val p = if (count.waiting) 0f else stepProgress(t, start, ProfileStatCountTimeline.STAMP_MS.toFloat())
                    when {
                        p >= 1f -> drawContent()
                        p <= 0f -> profileDrawWithout(number)
                        else -> {
                            profileDrawWithout(number)
                            val pivot = Offset(
                                number.left + number.width * StampSpec.Pivot.pivotFractionX,
                                number.top + number.height * StampSpec.Pivot.pivotFractionY,
                            )
                            val content = this
                            stampPaint.alpha = StampSpec.alphaAt(p)
                            drawIntoCanvas { it.saveLayer(Rect(pivot, number.maxDimension * (1f + StampSpec.SCALE_BOOST)), stampPaint) }
                            withStampTransform(EaseOutBack.transform(p), row, pivot) {
                                clipRect(number.left, number.top, number.right, number.bottom) { content.drawContent() }
                            }
                            drawIntoCanvas { it.restore() }
                            val halfDiagonal = hypot(number.width, number.height) / 2f
                            val ring = halfDiagonal * (ProfileStatCountTimeline.STAMP_RING_FROM +
                                (ProfileStatCountTimeline.STAMP_RING_TO - ProfileStatCountTimeline.STAMP_RING_FROM) * p)
                            drawCircle(LifeGameInk.copy(alpha = 0.25f * (1f - p)), ring, number.center, style = Stroke(1.dp.toPx()))
                        }
                    }
                }
                StatCountUpStyle.NONE -> drawContent()
            }
        }
    }
}

/** The Text as usual, except inside [hole] (where the final number sits). */
private fun ContentDrawScope.profileDrawWithout(hole: Rect) {
    val content = this
    clipRect(hole.left, hole.top, hole.right, hole.bottom, ClipOp.Difference) { content.drawContent() }
}

/** One SLOT reel in a digit [box]: the digit at [position] rolls up while the next one comes in below it. */
private fun ContentDrawScope.profileDrawReel(digits: GlyphDigits, box: Rect, baseline: Float, position: Float) {
    val digit = profileReelDigit(position)
    val next = (digit + 1) % 10
    val rolled = position - floor(position)
    val lineHeight = box.height
    clipRect(box.left, box.top, box.right, box.bottom) {
        digits.drawDigit(this, digit, box.left + (box.width - digits.advance(digit)) / 2f, baseline, dy = -rolled * lineHeight)
        if (rolled > 0f) {
            digits.drawDigit(this, next, box.left + (box.width - digits.advance(next)) / 2f, baseline, dy = (1f - rolled) * lineHeight)
        }
    }
}
