package com.example.lifegame.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.components.EntranceStyle
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.RiseEasing
import com.example.lifegame.ui.components.StampSpec
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.components.rememberReduceMotion
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/** Remembers whether the lobby intro already ran in this process: later visits get the quick version. */
internal object LobbyIntroMemory {
    var playedThisProcess = false
}

/**
 * How the lobby enters. [key] restarts every animation that reads this state (used by the
 * comparison screen's replay button). Inactive state means every element simply rests visible.
 */
@Immutable
internal data class LobbyIntroState(
    val style: EntranceStyle,
    val active: Boolean,
    val quick: Boolean,
    val key: Int = 0,
) {
    val stampTitle: Boolean get() = active && !quick && style == EntranceStyle.STAMP_STAGGER
    val showUnderline: Boolean get() = style == EntranceStyle.STAMP_STAGGER
    private val stagger: Long get() = if (quick) 45L else 110L
    val duration: Int get() = if (quick) 320 else 560

    /** Elements from [TITLE_INDEX] on wait for the stamped title to land before rising. */
    fun delayFor(index: Int): Long =
        index * stagger + if (stampTitle && index > TITLE_INDEX) TITLE_LETTER_START + 4 * TITLE_LETTER_GAP - 2 * stagger else 0L

    companion object {
        const val TITLE_INDEX = 1
        const val TITLE_LETTER_START = 160L
        const val TITLE_LETTER_GAP = 120L
    }
}

@Composable
internal fun rememberLobbyIntro(): LobbyIntroState {
    val style = LocalMotionStyle.current.entrance
    val reduceMotion = rememberReduceMotion()
    // Plain remember: coming back to the lobby from another screen plays the quick intro again.
    val quick = remember { LobbyIntroMemory.playedThisProcess.also { LobbyIntroMemory.playedThisProcess = true } }
    var finished by remember { mutableStateOf(false) }
    val intro = LobbyIntroState(style, active = !finished && !reduceMotion && style != EntranceStyle.NONE, quick = quick)
    LaunchedEffect(Unit) {
        awaitFrameMillis(intro.delayFor(8) + intro.duration)
        finished = true
    }
    return intro
}

/**
 * Fades an element into place, [index] steps after the intro begins.
 * - [rise]: also lift it 22dp. Interactive elements pass false: graphicsLayer moves hit testing with the
 *   pixels, so a rising button would take taps meant for whatever will sit where it currently floats.
 * - [groupOpacity]: fade the element as one flattened layer. Only for layered art (the avatar) where
 *   parts would show through each other. Otherwise alpha is applied per draw (ModulateAlpha), which —
 *   unlike an offscreen layer — does not clip what the element draws outside its bounds (the CTA halo,
 *   stamped title glyphs) during the fade.
 */
@Composable
internal fun Modifier.lobbyEntrance(
    intro: LobbyIntroState,
    index: Int,
    rise: Boolean = true,
    groupOpacity: Boolean = false,
): Modifier {
    val progress = remember(intro.key) { Animatable(if (intro.active) 0f else 1f) }
    LaunchedEffect(intro.key) {
        if (progress.value < 1f) {
            progress.animateTo(1f, tween(intro.duration, delayMillis = intro.delayFor(index).toInt(), easing = RiseEasing))
        }
    }
    return graphicsLayer {
        val p = progress.value
        alpha = p
        if (rise) translationY = (1f - p) * 22.dp.toPx()
        compositingStrategy = if (groupOpacity) CompositingStrategy.Auto else CompositingStrategy.ModulateAlpha
    }
}

/**
 * The lobby title. While stamping, each glyph drops in large and lands with a small overshoot;
 * the real text node stays in place (transparent) so layout, semantics and tests see one title.
 */
@Composable
internal fun StampedTitle(text: String, titleStyle: TextStyle, accent: Color, intro: LobbyIntroState, modifier: Modifier = Modifier) {
    // Same inheritance as Text(color = …, fontSize = …): the theme's text style (line height etc.) stays underneath.
    val style = LocalTextStyle.current.merge(titleStyle)
    var stamping by remember(intro.key) { mutableStateOf(intro.stampTitle) }
    val letters = remember(intro.key, text) { text.map { Animatable(if (stamping) 0f else 1f) } }
    val underline = remember(intro.key) { Animatable(if (stamping) 0f else 1f) }
    LaunchedEffect(intro.key) {
        if (!stamping) return@LaunchedEffect
        coroutineScope {
            letters.mapIndexed { index, letter ->
                async {
                    awaitFrameMillis(LobbyIntroState.TITLE_LETTER_START + index * LobbyIntroState.TITLE_LETTER_GAP)
                    letter.animateTo(1f, StampSpec.spring())
                }
            }.let { jobs ->
                underline.animateTo(1f, tween(420,
                    delayMillis = (LobbyIntroState.TITLE_LETTER_START + text.length * LobbyIntroState.TITLE_LETTER_GAP).toInt(),
                    easing = FastOutSlowInEasing))
                jobs.awaitAll()
            }
        }
        stamping = false
    }
    Box(modifier.drawBehind {
        if (!intro.showUnderline) return@drawBehind
        val width = 54.dp.toPx() * underline.value
        if (width > 0f) drawRect(accent, Offset(2.dp.toPx(), size.height - 3.dp.toPx()), Size(width, 2.dp.toPx()))
    }) {
        // The glyph overlay comes first and the real (transparent while stamping) heading last, so the
        // heading is the topmost node for accessibility hit-testing even mid-stamp.
        // The overlay is a plain left-to-right Row: Hangul titles are LTR whatever the app's layout direction.
        if (stamping) CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Row(Modifier.matchParentSize().clearAndSetSemantics { testTag = STAMPING_TAG }) {
                text.forEachIndexed { index, glyph ->
                    Text(glyph.toString(), style = style, modifier = Modifier.graphicsLayer {
                        val p = letters[index].value
                        alpha = StampSpec.alphaAt(p)
                        val scale = StampSpec.scaleAt(p)
                        scaleX = scale
                        scaleY = scale
                        rotationZ = StampSpec.tiltAt(index, p)
                        transformOrigin = StampSpec.Pivot
                    })
                }
            }
        }
        Text(
            text,
            style = style,
            // A single-line Row cannot follow a wrapped title (huge font scale, narrow window): skip the stamp.
            onTextLayout = { if (it.lineCount > 1) stamping = false },
            modifier = Modifier.semantics { heading() }.graphicsLayer { alpha = if (stamping) 0f else 1f },
        )
    }
}

/** Present only while the title glyphs are stamping in; lets tests observe the intro's phases. */
internal const val STAMPING_TAG = "lobby_title_stamping"
