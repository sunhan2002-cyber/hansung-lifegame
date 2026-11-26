/**
 * Area: lobby2 (로비 아래 버튼 · 새 칭호 알림 점). The lobby footer moved here from HomeScreen.kt in stage 2.
 *
 * Each footer button (지난 인생 · 칭호 수집 · 설정) gets a small drawn icon — a book, a medal, a gear — that acts out
 * its button when pressed ([FooterIconStyle]), and 칭호 수집 carries the gold new-title dot ([NewBadgeStyle]).
 * Presentation only: the clickable, its haptic click, the onClick timing, the tag and the merged label are exactly as
 * before. The motion starts from the button's own PressInteraction (a cancelled press still wiggles but never clicks;
 * an accessibility click clicks without wiggling). Icons and the dot are drawn, so they add no semantics nodes; the
 * dot adds the stateDescription '새 칭호 있음' to its button. Under reduced motion everything rests in its static pose.
 */
package com.example.lifegame.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.components.FooterIconStyle
import com.example.lifegame.ui.components.HighlightStyle
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.NewBadgeStyle
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.components.experiencePress
import com.example.lifegame.ui.components.inkSplash
import com.example.lifegame.ui.components.pressIndication
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.rememberExperienceClick
import com.example.lifegame.ui.components.rememberHighlightPhase
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameLobbyPaper
import com.example.lifegame.ui.theme.LifeGameNight
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sin

/** The drawn icon of a lobby footer button. */
internal enum class LobbyIcon { BOOK, MEDAL, GEAR }

/** The 칭호 수집 button's stateDescription while the new-title dot shows. */
internal const val LOBBY_NEW_TITLES_STATE = "새 칭호 있음"

/** HomeScreen's lobbyEntrance index of the footer row. */
private const val LOBBY_FOOTER_INDEX = 6

/** Without a lobby intro the dot pops this long after the lobby appears. */
private const val LOBBY_DOT_DELAY_MS = 240L

private val LobbyIconStroke = LifeGameLobbyPaper.copy(alpha = 0.9f)

/**
 * When the new-title dot pops: just as the footer row finishes rising (0.7 of the intro's rise into its step), or
 * [LOBBY_DOT_DELAY_MS] without an intro. Read it once when the lobby enters, so the intro settling never restarts it.
 */
internal fun lobbyNewDotDelayMillis(intro: LobbyIntroState): Long =
    if (intro.active) intro.delayFor(LOBBY_FOOTER_INDEX) + (intro.duration * 0.7f).roundToLong() else LOBBY_DOT_DELAY_MS

/**
 * One lobby footer button. [stacked] puts the icon above the label (the narrow three-button row: widths stay
 * `weight(1f)`, so '칭호 수집' never wraps at large font scales); otherwise the icon sits left of the label (the
 * large-text column). [showNewDot] draws the gold new-title dot, popping [newDotDelayMillis] after it first shows.
 */
@Composable
internal fun LobbyFooter(
    label: String,
    tag: String,
    icon: LobbyIcon,
    stacked: Boolean,
    modifier: Modifier = Modifier,
    showNewDot: Boolean = false,
    newDotDelayMillis: Long = 0L,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val click = rememberExperienceClick(onClick)
    Box(modifier.heightIn(min = 48.dp).experiencePress(interactions).clip(RoundedCornerShape(8.dp))
        .inkSplash(interactions, LifeGameLobbyPaper.copy(alpha = 0.14f))
        .clickable(interactionSource = interactions, indication = pressIndication(), role = Role.Button, onClick = click)
        .then(if (showNewDot) Modifier.semantics { stateDescription = LOBBY_NEW_TITLES_STATE } else Modifier)
        .padding(horizontal = 4.dp, vertical = if (stacked) 8.dp else 12.dp).testTag(tag), contentAlignment = Alignment.Center) {
        if (stacked) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LobbyFooterIcon(icon, interactions, showNewDot, newDotDelayMillis)
                Text(label, color = LobbyIconStroke, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LobbyFooterIcon(icon, interactions, showNewDot, newDotDelayMillis)
                Text(label, color = LobbyIconStroke, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

/**
 * The 18dp icon. Its press motion is drawn inside the icon (CHARACTER) or in its own layer (WIGGLE); the dot is drawn
 * by the wrapper after the icon, outside that layer, so it never wiggles with it.
 */
@Composable
private fun LobbyFooterIcon(icon: LobbyIcon, interactions: InteractionSource, showNewDot: Boolean, newDotDelayMillis: Long) {
    val style = LocalMotionStyle.current
    val iconStyle = style.footerIcon
    val reduceMotion = rememberReduceMotion()
    // press: 0 → 1 over one motion (1 = at rest). gear: degrees, one 45° notch per press (8 teeth look the same).
    val press = remember { Animatable(1f) }
    val gear = remember { Animatable(0f) }
    LaunchedEffect(interactions, iconStyle, reduceMotion) {
        if (reduceMotion || iconStyle == FooterIconStyle.NONE) {
            press.snapTo(1f)
            gear.snapTo(gear.targetValue)
            return@LaunchedEffect
        }
        interactions.interactions.collect { interaction ->
            if (interaction !is PressInteraction.Press) return@collect
            if (iconStyle == FooterIconStyle.CHARACTER && icon == LobbyIcon.GEAR) {
                launch { gear.animateTo(gear.targetValue + 45f, spring(dampingRatio = 0.45f, stiffness = 260f)) }
            } else {
                launch {
                    press.snapTo(0f)
                    press.animateTo(1f, tween(lobbyIconMotionMillis(iconStyle, icon), easing = LinearEasing))
                }
            }
        }
    }
    val dot = rememberLobbyNewDot(showNewDot, style.newBadge, newDotDelayMillis)
    Box(Modifier.size(18.dp).drawWithContent {
        drawContent()
        if (showNewDot) drawLobbyNewDot(dot)
    }) {
        Canvas(Modifier.matchParentSize().then(
            if (iconStyle == FooterIconStyle.WIGGLE) Modifier.graphicsLayer {
                val ms = press.value * LOBBY_WIGGLE_MS
                rotationZ = lobbyWiggleDegrees(ms)
                val pop = lobbyWigglePop(ms)
                scaleX = pop
                scaleY = pop
                transformOrigin = TransformOrigin(0.5f, 0.85f)
            } else Modifier,
        )) {
            val character = iconStyle == FooterIconStyle.CHARACTER
            when (icon) {
                LobbyIcon.BOOK -> drawLobbyBook(if (character) lobbyBookCoverScale(press.value * LOBBY_BOOK_MS) else 1f)
                LobbyIcon.MEDAL -> drawLobbyMedal(if (character) lobbyMedalSwingDegrees(press.value * LOBBY_MEDAL_MS) else 0f)
                LobbyIcon.GEAR -> drawLobbyGear(if (character) gear.value % 45f else 0f)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------
// Press motion (pure, unit-tested): every curve is exactly at rest at its end, so the resting icon is the static one.
// ---------------------------------------------------------------------------------------------------------

internal const val LOBBY_WIGGLE_MS = 560
internal const val LOBBY_BOOK_MS = 560
internal const val LOBBY_MEDAL_MS = 900

private fun lobbyIconMotionMillis(style: FooterIconStyle, icon: LobbyIcon): Int = when {
    style == FooterIconStyle.WIGGLE -> LOBBY_WIGGLE_MS
    icon == LobbyIcon.BOOK -> LOBBY_BOOK_MS
    else -> LOBBY_MEDAL_MS
}

/** WIGGLE: a toy-like damped wobble, 14° · e^(−t/0.12 s) · sin(2π · 5.5 Hz · t); 0 from [LOBBY_WIGGLE_MS] on. */
internal fun lobbyWiggleDegrees(ms: Float): Float {
    if (ms <= 0f || ms >= LOBBY_WIGGLE_MS) return 0f
    val t = ms / 1000f
    return 14f * exp(-t / 0.12f) * sin(2f * PI.toFloat() * 5.5f * t)
}

/** WIGGLE: a 10 % pop over the first 180 ms; exactly 1 at rest. */
internal fun lobbyWigglePop(ms: Float): Float {
    if (ms <= 0f || ms >= 180f) return 1f
    return 1f + 0.10f * sin(PI.toFloat() * min(ms / 180f, 1f))
}

/**
 * CHARACTER · BOOK: horizontal scale of the cover around its spine. It flaps open past the spine (−0.55), holds,
 * snaps shut with a small overshoot (1.06) and settles at exactly 1.
 */
internal fun lobbyBookCoverScale(ms: Float): Float = when {
    ms <= 0f || ms >= LOBBY_BOOK_MS -> 1f
    ms < 180f -> 1f + (-0.55f - 1f) * FastOutSlowInEasing.transform(ms / 180f)
    ms < 240f -> -0.55f
    ms < 440f -> -0.55f + (1.06f + 0.55f) * FastOutSlowInEasing.transform((ms - 240f) / 200f)
    else -> 1.06f + (1f - 1.06f) * FastOutSlowInEasing.transform((ms - 440f) / 120f)
}

/** CHARACTER · MEDAL: swing on its ribbon, 22° · e^(−t/0.25 s) · sin(2π · 1.7 Hz · t); 0 from [LOBBY_MEDAL_MS] on. */
internal fun lobbyMedalSwingDegrees(ms: Float): Float {
    if (ms <= 0f || ms >= LOBBY_MEDAL_MS) return 0f
    val t = ms / 1000f
    // A short taper over the last 10 % lands the residue (well under 1°) exactly on 0.
    val taper = ((LOBBY_MEDAL_MS - ms) / (LOBBY_MEDAL_MS * 0.1f)).coerceAtMost(1f)
    return 22f * exp(-t / 0.25f) * sin(2f * PI.toFloat() * 1.7f * t) * taper
}

// ---------------------------------------------------------------------------------------------------------
// Icons: 18dp canvases, 1.5dp round strokes in lobby paper with small gold accents.
// ---------------------------------------------------------------------------------------------------------

private fun DrawScope.lobbyStroke() = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

/** A standing book: the spine block on the left, the cover hinged at x = 6.5dp, a gold ribbon below the pages. */
private fun DrawScope.drawLobbyBook(coverScale: Float) {
    val u = 1.dp.toPx()
    val top = 2.5f * u
    val bottom = 15.5f * u
    val spine = 6.5f * u
    val corner = 1.6f * u
    val stroke = lobbyStroke()
    // The spine block: an open "[" from the hinge round the left edge back to the hinge.
    drawPath(Path().apply {
        moveTo(spine, top)
        lineTo(4f * u + corner, top)
        quadraticTo(4f * u, top, 4f * u, top + corner)
        lineTo(4f * u, bottom - corner)
        quadraticTo(4f * u, bottom, 4f * u + corner, bottom)
        lineTo(spine, bottom)
    }, LobbyIconStroke, style = stroke)
    // While the cover is lifted the pages show; once it swings past the spine, two page lines appear.
    if (coverScale < 1f) {
        drawRoundRect(LobbyIconStroke.copy(alpha = 0.55f), Offset(spine, top + 0.7f * u), Size(6.8f * u, bottom - top - 1.4f * u),
            CornerRadius(1.2f * u), style = Stroke(1.1.dp.toPx()))
        if (coverScale < 0.3f) {
            val pageStroke = 1.1.dp.toPx()
            drawLine(LobbyIconStroke.copy(alpha = 0.7f), Offset(8.6f * u, 7f * u), Offset(12f * u, 7f * u), pageStroke, StrokeCap.Round)
            drawLine(LobbyIconStroke.copy(alpha = 0.7f), Offset(8.6f * u, 10f * u), Offset(12f * u, 10f * u), pageStroke, StrokeCap.Round)
        }
    }
    val cover = Path().apply {
        moveTo(spine, top)
        lineTo(14f * u - corner, top)
        quadraticTo(14f * u, top, 14f * u, top + corner)
        lineTo(14f * u, bottom - corner)
        quadraticTo(14f * u, bottom, 14f * u - corner, bottom)
        lineTo(spine, bottom)
        close()
    }
    withTransform({ scale(scaleX = coverScale, scaleY = 1f, pivot = Offset(spine, 0f)) }) {
        drawPath(cover, LifeGameLobbyPaper.copy(alpha = 0.18f))
        drawPath(cover, LobbyIconStroke, style = stroke)
    }
    // The bookmark ribbon belongs to the pages, so it stays while the cover flaps.
    drawLine(LifeGameGold, Offset(11.5f * u, bottom), Offset(11.5f * u, 17.4f * u), 1.5.dp.toPx(), StrokeCap.Round)
}

/** A medal on a V ribbon, swinging [swing]° around the ribbon top (9dp, 2dp); its centre is gold. */
private fun DrawScope.drawLobbyMedal(swing: Float) {
    val u = 1.dp.toPx()
    rotate(swing, Offset(9f * u, 2f * u)) {
        val stroke = lobbyStroke()
        drawLine(LobbyIconStroke, Offset(6f * u, 2f * u), Offset(8.1f * u, 7.4f * u), stroke.width, StrokeCap.Round)
        drawLine(LobbyIconStroke, Offset(12f * u, 2f * u), Offset(9.9f * u, 7.4f * u), stroke.width, StrokeCap.Round)
        drawCircle(LobbyIconStroke, 4.1f * u, Offset(9f * u, 11.4f * u), style = stroke)
        drawCircle(LifeGameGold, 1.7f * u, Offset(9f * u, 11.4f * u))
    }
}

/** An eight-tooth gear turned [degrees] (any multiple of 45° looks identical to 0°). */
private fun DrawScope.drawLobbyGear(degrees: Float) {
    val u = 1.dp.toPx()
    val centre = Offset(9f * u, 9f * u)
    val outer = 7.3f * u
    val root = 5.5f * u
    val gear = Path()
    for (tooth in 0 until 8) {
        val mid = tooth * PI.toFloat() / 4f
        val points = floatArrayOf(mid - 0.25f, mid - 0.15f, mid + 0.15f, mid + 0.25f)
        val radii = floatArrayOf(root, outer, outer, root)
        for (i in points.indices) {
            val x = centre.x + cos(points[i]) * radii[i]
            val y = centre.y + sin(points[i]) * radii[i]
            if (tooth == 0 && i == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
        }
    }
    gear.close()
    rotate(degrees, centre) {
        drawPath(gear, LobbyIconStroke, style = lobbyStroke())
        drawCircle(LobbyIconStroke, 2.2f * u, centre, style = lobbyStroke())
    }
}

// ---------------------------------------------------------------------------------------------------------
// The new-title dot
// ---------------------------------------------------------------------------------------------------------

/** What the dot's draw reads: its scale, the one-shot ping (1 = no ring) and the BREATH phase (null = no halo). */
private class LobbyNewDot(val scale: State<Float>, val ping: State<Float>, val breath: State<Float>?, val restingHalo: Boolean)

@Composable
private fun rememberLobbyNewDot(show: Boolean, style: NewBadgeStyle, delayMillis: Long): LobbyNewDot {
    // The pop starts from nothing, so it is an entrance: none under reduced motion or touch exploration.
    val pops = show && style == NewBadgeStyle.POP_PING && rememberEntranceMotionAllowed()
    val scale = remember(show) { Animatable(if (pops) 0f else 1f) }
    val ping = remember(show) { Animatable(1f) }
    LaunchedEffect(scale, pops) {
        if (!pops) {
            scale.snapTo(1f)
            ping.snapTo(1f)
            return@LaunchedEffect
        }
        if (scale.value >= 1f) return@LaunchedEffect
        awaitFrameMillis(delayMillis)
        var pinged = false
        val scope = this
        scale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f)) {
            if (!pinged && value >= 0.95f) {
                pinged = true
                scope.launch {
                    ping.snapTo(0f)
                    ping.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
                }
            }
        }
    }
    val breathing = show && style == NewBadgeStyle.BREATH
    // rememberHighlightPhase returns null under reduced motion: BREATH then draws a still halo at mid-breath.
    val breath = if (breathing) rememberHighlightPhase(HighlightStyle.PULSE) else null
    return LobbyNewDot(scale.asState(), ping.asState(), breath, restingHalo = breathing && breath == null)
}

private val LobbyDotCentre = Offset(16f, 2.5f)
private const val LOBBY_DOT_RADIUS_DP = 3.2f

private fun DrawScope.drawLobbyNewDot(dot: LobbyNewDot) {
    val centre = Offset(LobbyDotCentre.x.dp.toPx(), LobbyDotCentre.y.dp.toPx())
    val radius = LOBBY_DOT_RADIUS_DP.dp.toPx()
    val b = dot.breath?.let { (sin(it.value * 2f * PI.toFloat()) + 1f) / 2f } ?: if (dot.restingHalo) 0.5f else null
    var scale = dot.scale.value
    if (b != null) {
        val halo = radius + (2f + 3f * b).dp.toPx()
        drawCircle(Brush.radialGradient(listOf(LifeGameGold.copy(alpha = 0.35f * (1f - 0.5f * b)), LifeGameGold.copy(alpha = 0f)),
            center = centre, radius = halo), halo, centre)
        scale *= 1f + 0.1f * b
    }
    val ping = dot.ping.value
    if (ping < 1f) {
        drawCircle(LifeGameGold.copy(alpha = 0.65f * (1f - ping)), radius + 6.dp.toPx() * ping, centre, style = Stroke(1.2.dp.toPx()))
    }
    if (scale > 0f) {
        drawCircle(LifeGameNight, (radius + 1.2.dp.toPx()) * scale, centre)
        drawCircle(LifeGameGold, radius * scale, centre)
    }
}
