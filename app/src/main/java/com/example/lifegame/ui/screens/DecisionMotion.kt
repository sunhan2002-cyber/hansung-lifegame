package com.example.lifegame.ui.screens

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.GraphicsContext
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.lerp
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.components.ChoicePickStyle
import com.example.lifegame.ui.components.ChoiceRevealStyle
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.RiseEasing
import com.example.lifegame.ui.components.drawOffset
import com.example.lifegame.ui.components.drawRimGlow
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.rememberOneShotClock
import com.example.lifegame.ui.components.statChangeSpring
import com.example.lifegame.ui.components.statChangeSpringPulse
import com.example.lifegame.ui.components.stepProgress
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import kotlin.math.abs
import kotlin.math.hypot
import kotlinx.coroutines.flow.first

/*
 * play · 선택지 등장(choiceReveal)과 고른 선택지 강조(choicePick). DecisionScreen 전용.
 *
 * - 움직임은 모두 그리기 단계에서 읽는다. 카드의 배치 · 의미 경계 · 터치 영역 · 자동 스크롤 대상은 언제나 쉬는 자리다.
 *   클릭 처리 · 입력 보호(enabled, swallowTaps, 완성 터치) · 콜백과 그 시점은 그대로다.
 * - 시계는 프레임 시계다(rememberOneShotClock). 움직임 줄이기 · 터치 탐색에서는 처음부터 마지막 모습이고,
 *   도중에 꺼지면 바로 마지막 모습으로 간다.
 * - 고른 카드: VM은 고른 그 프레임에 결과를 낸다. 엔진이 기록 한 줄을 더해 yearKey(=introKey)도 함께 바뀌므로
 *   remember(introKey) 상태와 카드가 모두 사라진다. 그래서 카드가 그려질 때마다 그 모습을 GraphicsLayer에 녹화해
 *   두고, 고른 순간 화면의 자리와 함께 키 없는 상태로 들고 있다가, 의미 노드도 터치도 없는 오버레이에 그려 물러나게
 *   한다(다시 조립한 카드는 useUnmergedTree 글자 검사에 잡히므로 쓰지 않는다). onChoose는 지금처럼 클릭 처리에서
 *   곧바로 한 번만 불리고, 그 앞에 화면 상태 한 줄만 쓴다.
 */

/** 고른 카드가 물러나는 시간. 이 뒤로는 오버레이가 아무것도 그리지 않는다. */
internal const val PICK_EXIT_MS = 460

/**
 * 고른 순간부터 결과 연출(변화 칩 · 건강 게이지 · 큰 결과)이 시작하기까지. 표시만 늦추고 콜백 시점
 * (onResultAnimationConsumed는 결과가 나온 뒤 CHANGE_HOLD_MILLIS)은 그대로다.
 */
internal const val PICK_RESULT_DELAY_MS = 180

/** 결과 화면이 모두 자리 잡는 시간(계속하기 버튼이 마지막이다). */
private const val PICK_SETTLED_MS = 520

private const val CARD_CORNER_DP = 12f

// ---------------------------------------------------------------------------------------------------------
// 선택지 등장 (choiceReveal)
// ---------------------------------------------------------------------------------------------------------

private const val RISE_STAGGER_MS = 70f
private const val RISE_MS = 320f
private const val RISE_BADGE_AT = 0.7f
private const val BADGE_SETTLE_MS = 380f
private const val DEAL_STAGGER_MS = 90f
private const val DEAL_SETTLE_MS = 560f
private const val INK_STAGGER_MS = 80f
private const val INK_MS = 440f
private const val REVEAL_FADE_MS = 220f
private val InkEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/**
 * One choice reveal. When [animates] is false every reveal modifier is a no-op (the cards simply appear, as
 * before). [ms] is the time since the choices appeared; read it only in draw lambdas.
 */
@Stable
internal class ChoiceRevealMotion internal constructor(
    val style: ChoiceRevealStyle,
    val animates: Boolean,
    private val clock: State<Float>,
) {
    internal val ms: Float get() = clock.value
}

/**
 * The reveal of [count] choice cards. [live] is true only in the composition where the choices first become
 * [visible] after the body finished in this screen (the screen saves "revealed" right after), so it never plays on
 * restoration, rotation, an app restart onto a seen event, or a result page. Gated on the style and
 * [rememberEntranceMotionAllowed].
 */
@Composable
internal fun rememberChoiceReveal(key: Any, visible: Boolean, live: Boolean, count: Int): ChoiceRevealMotion {
    val style = LocalMotionStyle.current.choiceReveal
    // 선택지가 처음 보이는 composition에서 잡은 값을 그 사건 동안 들고 있다(그다음 프레임부터 live는 거짓이다).
    val started = remember(key, visible) { live }
    val animates = started && style != ChoiceRevealStyle.NONE && rememberEntranceMotionAllowed()
    val clock = rememberOneShotClock(key = key to visible, active = animates, totalMs = choiceRevealTotalMs(style, count))
    return remember(style, animates, clock) { ChoiceRevealMotion(style, animates, clock) }
}

/** When the last of [count] cards has settled, per style. */
internal fun choiceRevealTotalMs(style: ChoiceRevealStyle, count: Int): Int {
    val last = (count - 1).coerceAtLeast(0)
    val total = when (style) {
        ChoiceRevealStyle.NONE -> 0f
        ChoiceRevealStyle.RISE -> last * RISE_STAGGER_MS + RISE_MS * RISE_BADGE_AT + BADGE_SETTLE_MS
        ChoiceRevealStyle.DEAL -> last * DEAL_STAGGER_MS + DEAL_SETTLE_MS
        ChoiceRevealStyle.INK -> last * INK_STAGGER_MS + INK_MS
    }
    return kotlin.math.ceil(total).toInt()
}

/** RISE: card [index]'s linear progress (0 → 1 over 320 ms, 70 ms after the previous card). */
internal fun choiceRiseProgress(ms: Float, index: Int): Float = stepProgress(ms, RISE_STAGGER_MS * index, RISE_MS)

/** RISE: the ABC badge's stamp scale. It lands at 1.22 when its card is 70% risen and springs back to 1. */
internal fun choiceBadgeScale(ms: Float, index: Int): Float {
    val sinceStamp = ms - (RISE_STAGGER_MS * index + RISE_MS * RISE_BADGE_AT)
    if (sinceStamp < 0f || sinceStamp >= BADGE_SETTLE_MS) return 1f
    return 1f + 0.22f * (1f - statChangeSpring(sinceStamp, 0.5f, 700f))
}

/** DEAL: card [index]'s spring progress (overshoots a little past 1, rests exactly at 1). */
internal fun choiceDealProgress(ms: Float, index: Int): Float {
    val t = ms - DEAL_STAGGER_MS * index
    return if (t >= DEAL_SETTLE_MS) 1f else statChangeSpring(t, 0.62f, 340f)
}

/** INK: card [index]'s bloom progress. */
internal fun choiceInkProgress(ms: Float, index: Int): Float = stepProgress(ms, INK_STAGGER_MS * index, INK_MS, InkEasing)

/** Opacity of what appears with the cards but is not a card (the stat-tag toggle, notes, 장면 보기). */
internal fun choiceRevealFadeAlpha(ms: Float): Float = stepProgress(ms, 0f, REVEAL_FADE_MS)

/**
 * The entrance of choice card [index]: RISE floats it up 16dp as it fades in, DEAL slides it in from the side
 * with a slight tilt and settles on a spring, INK blooms it out of its ABC badge. Opacity uses a layer alpha
 * (no transform) and every move is a draw transform, so the card's bounds, tap target and BringIntoView target
 * stay at rest the whole time. At rest it draws exactly the card.
 */
internal fun Modifier.choiceEntrance(reveal: ChoiceRevealMotion, index: Int): Modifier {
    if (!reveal.animates) return this
    return when (reveal.style) {
        ChoiceRevealStyle.NONE -> this
        ChoiceRevealStyle.RISE -> graphicsLayer {
            alpha = (1.6f * choiceRiseProgress(reveal.ms, index)).coerceAtMost(1f)
        }.drawOffset {
            Offset(0f, (1f - RiseEasing.transform(choiceRiseProgress(reveal.ms, index))) * 16.dp.toPx())
        }
        ChoiceRevealStyle.DEAL -> graphicsLayer {
            alpha = (2.5f * choiceDealProgress(reveal.ms, index)).coerceIn(0f, 1f)
        }.drawWithContent {
            val away = 1f - choiceDealProgress(reveal.ms, index)
            if (away == 0f) {
                drawContent()
            } else {
                val slide = 36.dp.toPx() * away
                val corner = Offset(0f, size.height)
                withTransform({
                    translate(left = slide)
                    rotate(5f * away, pivot = corner)
                }) { this@drawWithContent.drawContent() }
            }
        }
        ChoiceRevealStyle.INK -> graphicsLayer {
            val p = choiceInkProgress(reveal.ms, index)
            alpha = (p * 4f).coerceAtMost(1f)
            compositingStrategy = if (p < 1f) CompositingStrategy.Offscreen else CompositingStrategy.Auto
        }.drawWithContent {
            drawContent()
            val p = choiceInkProgress(reveal.ms, index)
            if (p < 1f) {
                // ABC 표식(왼쪽 위 36×32)의 가운데에서 먹물이 번지듯: 반지름 밖은 지우고, 가장자리 18dp는 부드럽게.
                val edge = 18.dp.toPx()
                val radius = (0.08f + 0.92f * p) * hypot(size.width, size.height)
                val soft = radius / (radius + edge)
                drawRect(
                    Brush.radialGradient(
                        0f to Color.Transparent,
                        soft to Color.Transparent,
                        1f to Color.Black,
                        center = Offset(18.dp.toPx(), 16.dp.toPx()),
                        radius = radius + edge,
                    ),
                    blendMode = BlendMode.DstOut,
                )
            }
        }
    }
}

/**
 * RISE's badge stamp: the ABC badge pops to 1.22 and springs back to 1 around its top-left corner, so it stays
 * pinned to the card's corner. A draw transform (no layer): the card's snapshot for [choicePickRecord] then has no
 * nested layer.
 */
internal fun Modifier.choiceBadgeStamp(reveal: ChoiceRevealMotion, index: Int): Modifier {
    if (!reveal.animates || reveal.style != ChoiceRevealStyle.RISE) return this
    return drawWithContent {
        val s = choiceBadgeScale(reveal.ms, index)
        if (s == 1f) drawContent() else scale(s, s, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
    }
}

/** Fades in (alpha only) what appears together with the cards: the stat-tag toggle, the notes and 장면 보기. */
internal fun Modifier.choiceRevealFade(reveal: ChoiceRevealMotion): Modifier =
    if (!reveal.animates) this else graphicsLayer { alpha = choiceRevealFadeAlpha(reveal.ms) }

// ---------------------------------------------------------------------------------------------------------
// 고른 선택지 강조 (choicePick)
// ---------------------------------------------------------------------------------------------------------

/** Parts of the result page that enter after a live pick. */
internal enum class ChoicePickEntrance { TEXT, CARD, BUTTON }

/** One part's entrance window on the pick clock; [rises] adds a 12dp draw-only rise (RiseEasing). */
internal data class ChoicePickWindow(val startMs: Int, val endMs: Int, val rises: Boolean)

/**
 * When each part of the result page fades in after a live pick: the event text first, then the outcome card,
 * then the continue button (settled by 520 ms). MORPH's card only fades, so the arriving card lands on it.
 */
internal fun choicePickWindow(style: ChoicePickStyle, part: ChoicePickEntrance): ChoicePickWindow? = when (style) {
    ChoicePickStyle.NONE -> null
    ChoicePickStyle.LIFT, ChoicePickStyle.STAMP -> when (part) {
        ChoicePickEntrance.TEXT -> ChoicePickWindow(140, 420, rises = false)
        ChoicePickEntrance.CARD -> ChoicePickWindow(180, 460, rises = true)
        ChoicePickEntrance.BUTTON -> ChoicePickWindow(240, PICK_SETTLED_MS, rises = true)
    }
    ChoicePickStyle.MORPH -> when (part) {
        ChoicePickEntrance.TEXT -> ChoicePickWindow(120, 360, rises = false)
        ChoicePickEntrance.CARD -> ChoicePickWindow(160, 420, rises = false)
        ChoicePickEntrance.BUTTON -> ChoicePickWindow(240, PICK_SETTLED_MS, rises = true)
    }
}

/** LIFT's gold flash envelope: up in 70 ms, gone by 320 ms. */
internal fun choicePickFlash(ms: Float): Float = when {
    ms < 0f -> 0f
    ms < 70f -> ms / 70f
    else -> (1f - (ms - 70f) / 250f).coerceAtLeast(0f)
}

/** A card that was on screen at the pick: where it was (root of the screen) and its last recorded look. */
internal class ChoicePickGhost(val index: Int, val rect: Rect, val layer: GraphicsLayer)

/**
 * What a pick left behind: which event and card, the cards' last looks and where the choice list's viewport was.
 * It is plain remember state, so it survives the introKey change of the pick and is gone after recreation.
 */
@Stable
internal class ChoicePicked internal constructor(
    val eventId: String,
    val chosenIndex: Int,
    val ghosts: List<ChoicePickGhost>,
    val viewport: Rect,
) {
    /** Set once the exit has ended: the overlay and the result-page entrance then step aside. */
    internal var finished by mutableStateOf(false)
}

/**
 * The long-lived side of the pick motion, remembered without a key for as long as the decision screen is
 * composed: layout coordinates written by placement callbacks (plain fields, no recomposition), the cards'
 * snapshot layers (one per choice key, reused across decisions, released with the screen) and the last pick.
 */
@Stable
internal class ChoicePickMotion internal constructor(private val graphics: GraphicsContext) {
    internal var root: LayoutCoordinates? = null
    internal var viewport: LayoutCoordinates? = null
    internal var outcomeCard: LayoutCoordinates? = null
    private val cards = HashMap<String, LayoutCoordinates>()
    private val layers = HashMap<String, GraphicsLayer>()
    private var released = false
    internal var picked by mutableStateOf<ChoicePicked?>(null)

    internal val rootTracker: Modifier = Modifier.onPlaced { root = it }
    internal val viewportTracker: Modifier = Modifier.onPlaced { viewport = it }
    internal val outcomeTracker: Modifier = Modifier.onPlaced { outcomeCard = it }

    internal fun trackCard(key: String, coordinates: LayoutCoordinates) {
        cards[key] = coordinates
    }

    internal fun layerFor(key: String): GraphicsLayer? {
        if (released) return null
        return layers.getOrPut(key) { graphics.createGraphicsLayer() }
    }

    /** Captures where the cards are and keeps their last looks, right before the unchanged onChoose. */
    internal fun capture(eventId: String, choices: List<DecisionChoiceUi>, chosenId: String) {
        val origin = root?.takeIf { it.isAttached } ?: return
        val chosenIndex = choices.indexOfFirst { it.choiceId == chosenId }
        if (chosenIndex < 0) return
        val ghosts = choices.mapIndexedNotNull { index, choice ->
            val card = cards[choice.key]?.takeIf { it.isAttached } ?: return@mapIndexedNotNull null
            val layer = layers[choice.key] ?: return@mapIndexedNotNull null
            ChoicePickGhost(index, Rect(origin.localPositionOf(card, Offset.Zero), card.size.toSize()), layer)
        }
        if (ghosts.none { it.index == chosenIndex }) return
        val window = viewport?.takeIf { it.isAttached }?.let { origin.localBoundingBoxOf(it, clipBounds = false) }
            ?: Rect(Offset.Zero, origin.size.toSize())
        // 카드 밖에서 그려질 마지막 모습은 카드 모양(둥근 모서리, 녹화 때 정한 윤곽)으로 자른다. 살아 있는 카드는
        // 자기 모서리 자르기가 이미 있으므로 녹화 중에는 자르지 않는다(같은 모양을 두 번 자르지 않게).
        ghosts.forEach { it.layer.clip = true }
        picked = ChoicePicked(eventId, chosenIndex, ghosts, window)
    }

    /** The outcome card in the screen's coordinates, or null before it is placed. Read in draw. */
    internal fun outcomeRect(): Rect? {
        val origin = root?.takeIf { it.isAttached } ?: return null
        val card = outcomeCard?.takeIf { it.isAttached } ?: return null
        return origin.localBoundingBoxOf(card, clipBounds = false)
    }

    internal fun release() {
        released = true
        layers.values.forEach(graphics::releaseGraphicsLayer)
        layers.clear()
        cards.clear()
    }
}

/**
 * The pick motion as one composition sees it. [live] is the pick being shown now (null: nothing to play), [clock]
 * its time since the result appeared, read only in draw lambdas.
 */
@Stable
internal class ChoicePickState internal constructor(
    val motion: ChoicePickMotion,
    val style: ChoicePickStyle,
    /** Cards record their looks (style on, motion allowed). Off under reduced motion, so motion-off pixels stay put. */
    val recording: Boolean,
    val live: ChoicePicked?,
    val clock: State<Float>,
) {
    /** How long the result effects (chips, gauge, impact) wait: only after a live pick. */
    val resultDelayMillis: Int get() = if (live != null) PICK_RESULT_DELAY_MS else 0

    /** Writes the pick (presentation state only). Call it in the click handler right before the unchanged onChoose. */
    fun onPick(eventId: String, choices: List<DecisionChoiceUi>, chosenId: String) {
        if (recording) motion.capture(eventId, choices, chosenId)
    }

    /** Remembers where card [key] is (no semantics, no pointer input). Put it outside the card's press scale. */
    fun track(key: String): Modifier = if (!recording) Modifier else Modifier.onPlaced { motion.trackCard(key, it) }

    /** Records card [key]'s look into its snapshot layer every time the card draws. Put it right after the card's clip. */
    fun record(key: String): Modifier = if (!recording) Modifier else Modifier.choicePickRecord(motion, key)

    /** The result page's entrance for [part] after a live pick: a fade (layer alpha) plus, for some parts, a 12dp draw-only rise. */
    fun entrance(part: ChoicePickEntrance): Modifier {
        if (live == null) return Modifier
        val window = choicePickWindow(style, part) ?: return Modifier
        val start = window.startMs.toFloat()
        val duration = (window.endMs - window.startMs).toFloat()
        val faded = Modifier.graphicsLayer { alpha = stepProgress(clock.value, start, duration) }
        return if (!window.rises) faded else faded.drawOffset {
            Offset(0f, (1f - stepProgress(clock.value, start, duration, RiseEasing)) * 12.dp.toPx())
        }
    }

    /** The outcome card's centre in the screen's coordinates (for the result impact's wash). Read in draw. */
    fun outcomeCenter(): Offset? = motion.outcomeRect()?.center
}

/**
 * The pick motion for the decision screen showing [eventId]. A pick is shown when the result of that same event
 * is on screen in the frame after the click (the VM publishes synchronously), the style is not NONE and entrance
 * motion is allowed; a pick whose result did not arrive (a rejected choice, a test callback) is dropped at once.
 */
@Composable
internal fun rememberChoicePick(eventId: String, showingOutcome: Boolean): ChoicePickState {
    val graphics = LocalGraphicsContext.current
    val motion = remember(graphics) { ChoicePickMotion(graphics) }
    DisposableEffect(motion) { onDispose { motion.release() } }
    val style = LocalMotionStyle.current.choicePick
    val allowed = style != ChoicePickStyle.NONE && rememberEntranceMotionAllowed()
    val picked = motion.picked
    val arrived = picked != null && showingOutcome && picked.eventId == eventId
    val live = picked?.takeIf { allowed && arrived && !it.finished }
    val clock = rememberOneShotClock(key = picked, active = live != null, totalMs = PICK_SETTLED_MS)
    if (live != null) {
        LaunchedEffect(live) {
            snapshotFlow { clock.value >= PICK_SETTLED_MS }.first { it }
            live.finished = true
        }
    } else if (picked != null && !picked.finished && !arrived) {
        // 결과가 고른 그 프레임에 오지 않았다(선택이 거절됐거나 결과를 내지 않는 콜백). 그 선택은 바로 버린다:
        // 나중에 같은 사건의 결과가 와도 물러나는 그림 · 늦춤 없이 지금처럼 바로 보인다.
        SideEffect { picked.finished = true }
    }
    return remember(motion, style, allowed, live, clock) { ChoicePickState(motion, style, allowed, live, clock) }
}

/**
 * Records the card's content into its snapshot layer (the documented record-then-draw pattern) and draws that
 * layer, so the card looks exactly the same while its last frame stays available after it leaves composition.
 * The layer carries the card's rounded outline but only clips to it once it is captured as a ghost (the live card
 * already clips). Software canvases (which cannot draw a RenderNode) skip the recording and draw the card directly.
 */
private fun Modifier.choicePickRecord(motion: ChoicePickMotion, key: String): Modifier = drawWithContent {
    val layer = motion.layerFor(key)
    if (layer == null || !drawContext.canvas.nativeCanvas.isHardwareAccelerated) {
        drawContent()
        return@drawWithContent
    }
    layer.alpha = 1f
    layer.clip = false
    layer.setRoundRectOutline(Offset.Zero, size, CARD_CORNER_DP.dp.toPx())
    layer.record { this@drawWithContent.drawContent() }
    drawLayer(layer)
}

/** The seal ring of STAMP, a darker gold than the disc. */
private val PickSealRing = Color(0xFFB89255)

/**
 * The exit of a live pick: the cards' last looks leave from where they were, clipped to the choice list's
 * viewport — LIFT lifts the chosen card in a gold flash while the others fall away, STAMP stamps it with a gold
 * '선택' seal while the others sink under an ink veil, MORPH slides it onto the outcome card while the others
 * dissolve. It is a Canvas with no semantics and no pointer input (taps reach the result page underneath), and it
 * follows the result impact's content offset ([contentOffset]) so a shake moves everything together.
 * Composed only while a pick is live; it draws nothing after [PICK_EXIT_MS].
 */
@Composable
internal fun ChoiceExitOverlay(pick: ChoicePickState, contentOffset: DrawScope.() -> Offset) {
    val live = pick.live ?: return
    val measurer = rememberTextMeasurer()
    Canvas(Modifier.fillMaxSize()) {
        val t = pick.clock.value
        if (t >= PICK_EXIT_MS || !drawContext.canvas.nativeCanvas.isHardwareAccelerated) return@Canvas
        val shift = contentOffset()
        val view = live.viewport
        clipRect(view.left, view.top, view.right, view.bottom) {
            translate(shift.x, shift.y) {
                live.ghosts.forEach { ghost ->
                    if (ghost.index != live.chosenIndex) drawOtherGhost(pick.style, ghost, live.chosenIndex, t)
                }
                live.ghosts.firstOrNull { it.index == live.chosenIndex }?.let { ghost ->
                    when (pick.style) {
                        ChoicePickStyle.LIFT -> drawLiftedGhost(ghost, t)
                        ChoicePickStyle.STAMP -> drawStampedGhost(ghost, t, measurer)
                        ChoicePickStyle.MORPH -> drawMorphingGhost(ghost, t, pick.motion.outcomeRect())
                        ChoicePickStyle.NONE -> Unit
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawGhostLayer(ghost: ChoicePickGhost, alpha: Float) {
    if (alpha <= 0f || ghost.layer.isReleased) return
    ghost.layer.alpha = alpha.coerceIn(0f, 1f)
    drawLayer(ghost.layer)
}

/** The cards that were not chosen. LIFT: fall with a little gravity and tilt away; STAMP: sink under an ink veil; MORPH: dissolve. */
private fun DrawScope.drawOtherGhost(style: ChoicePickStyle, ghost: ChoicePickGhost, chosenIndex: Int, t: Float) {
    val size = ghost.rect.size
    val middle = Offset(size.width / 2f, size.height / 2f)
    when (style) {
        ChoicePickStyle.LIFT -> {
            val u = ((t - 30f * abs(ghost.index - chosenIndex)) / 300f).coerceIn(0f, 1f)
            if (u >= 1f) return
            val tilt = (if (ghost.index < chosenIndex) -2.5f else 2.5f) * u
            translate(ghost.rect.left, ghost.rect.top + 30.dp.toPx() * u * u) {
                rotate(tilt, pivot = middle) { drawGhostLayer(ghost, 1f - u) }
            }
        }
        ChoicePickStyle.STAMP -> {
            val u = (t / 260f).coerceIn(0f, 1f)
            if (u >= 1f) return
            val s = 1f - 0.04f * u
            translate(ghost.rect.left, ghost.rect.top + 6.dp.toPx() * u) {
                scale(s, s, pivot = middle) {
                    drawGhostLayer(ghost, 1f - u)
                    drawRoundRect(
                        LifeGameInk.copy(alpha = (0.35f * u * (1f - u) * 2f).coerceIn(0f, 1f)),
                        size = size,
                        cornerRadius = CornerRadius(CARD_CORNER_DP.dp.toPx()),
                    )
                }
            }
        }
        ChoicePickStyle.MORPH -> {
            val u = (t / 180f).coerceIn(0f, 1f)
            if (u >= 1f) return
            translate(ghost.rect.left, ghost.rect.top + 8.dp.toPx() * u) { drawGhostLayer(ghost, 1f - u) }
        }
        ChoicePickStyle.NONE -> Unit
    }
}

/**
 * LIFT: the chosen card starts at the pressed scale (0.985, where the BOUNCE press left it), springs up past
 * 1.02 while it lifts 8dp then 4dp more, flashes gold (fill, 2dp stroke and a soft outer glow) and fades from 240 ms.
 */
private fun DrawScope.drawLiftedGhost(ghost: ChoicePickGhost, t: Float) {
    val size = ghost.rect.size
    val corner = CARD_CORNER_DP.dp.toPx()
    val scale = 0.985f + 0.035f * statChangeSpring(t, 0.5f, 520f)
    val lift = 8.dp.toPx() * FastOutSlowInEasing.transform((t / 260f).coerceIn(0f, 1f)) +
        4.dp.toPx() * FastOutSlowInEasing.transform(((t - 240f) / 220f).coerceIn(0f, 1f))
    val alpha = 1f - stepProgress(t, 240f, (PICK_EXIT_MS - 240).toFloat())
    val flash = choicePickFlash(t) * alpha
    translate(ghost.rect.left, ghost.rect.top - lift) {
        scale(scale, scale, pivot = Offset(size.width / 2f, size.height / 2f)) {
            drawRimGlow(LifeGameGold, alpha = 0.3f * flash, reach = 8.dp.toPx(), corner = corner, size = size)
            drawGhostLayer(ghost, alpha)
            if (flash > 0f) {
                val stroke = 2.dp.toPx()
                drawRoundRect(LifeGameGold.copy(alpha = 0.5f * flash), size = size, cornerRadius = CornerRadius(corner))
                drawRoundRect(
                    LifeGameGold.copy(alpha = (0.9f * flash).coerceAtMost(1f)),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(corner - stroke / 2f),
                    style = Stroke(stroke),
                )
            }
        }
    }
}

/**
 * STAMP: a gold '선택' seal (44dp disc, darker ring, tilted −8°) drops from 1.9× onto the chosen card's right side;
 * on impact (~80 ms) the card dips and one gold ink ring spreads from the seal; card and seal fade from 300 ms.
 */
private fun DrawScope.drawStampedGhost(ghost: ChoicePickGhost, t: Float, measurer: TextMeasurer) {
    val size = ghost.rect.size
    val fade = 1f - stepProgress(t, 300f, (PICK_EXIT_MS - 300).toFloat())
    val dip = 1f - 0.03f * statChangeSpringPulse(t - 80f, 0.45f, 800f)
    translate(ghost.rect.left, ghost.rect.top) {
        scale(dip, dip, pivot = Offset(size.width / 2f, size.height / 2f)) { drawGhostLayer(ghost, fade) }
        val seal = Offset(size.width - 56.dp.toPx(), size.height / 2f)
        val ring = ((t - 80f) / 300f).coerceIn(0f, 1f)
        if (t >= 80f && ring < 1f) {
            drawCircle(
                LifeGameGold.copy(alpha = 0.35f * (1f - ring) * fade),
                radius = 60.dp.toPx() * FastOutSlowInEasing.transform(ring),
                center = seal,
                style = Stroke(2.dp.toPx()),
            )
        }
        val sealScale = 1f + 0.9f * (1f - statChangeSpring(t, 0.42f, 900f))
        val sealAlpha = (t / 60f).coerceIn(0f, 1f) * fade
        if (sealAlpha <= 0f) return@translate
        val label = measurer.measure("선택", TextStyle(color = LifeGameInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        withTransform({
            rotate(-8f, seal)
            scale(sealScale, sealScale, seal)
        }) {
            drawCircle(LifeGameGold.copy(alpha = 0.92f * sealAlpha), radius = 22.dp.toPx(), center = seal)
            drawCircle(PickSealRing.copy(alpha = sealAlpha), radius = 21.dp.toPx(), center = seal, style = Stroke(2.dp.toPx()))
            drawText(
                label,
                topLeft = Offset(seal.x - label.size.width / 2f, seal.y - label.size.height / 2f),
                alpha = sealAlpha,
            )
        }
    }
}

/**
 * MORPH: the chosen card slides onto the outcome card (FastOutSlowIn over 380 ms; widths are matched if they ever
 * differ) and fades over 180–400 ms. Without a placed outcome card it just fades where it was.
 */
private fun DrawScope.drawMorphingGhost(ghost: ChoicePickGhost, t: Float, target: Rect?) {
    val alpha = 1f - stepProgress(t, 180f, 220f)
    if (target == null) {
        translate(ghost.rect.left, ghost.rect.top) { drawGhostLayer(ghost, alpha) }
        return
    }
    val p = FastOutSlowInEasing.transform((t / 380f).coerceIn(0f, 1f))
    val widthScale = if (ghost.rect.width > 0f) lerp(1f, target.width / ghost.rect.width, p) else 1f
    translate(lerp(ghost.rect.left, target.left, p), lerp(ghost.rect.top, target.top, p)) {
        scale(widthScale, 1f, pivot = Offset.Zero) { drawGhostLayer(ghost, alpha) }
    }
}
