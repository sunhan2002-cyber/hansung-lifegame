/**
 * Area: lobby2 (시간대 로비).
 *
 * The lobby's light follows the device's time of day: morning light from the left, today's daylight, a low orange
 * sunset from the right and a navy night. LIVING adds morning light shafts, sunset embers (the existing motes,
 * recoloured) and night stars and fireflies. Presentation only: the hour comes from [LobbyClock], never from the
 * game, and every particle seed is a fixed index hash (no Random).
 *
 * Frozen contract — LifeGameApp.kt (integrator) and HomeScreen.kt call these, so the signatures stay as they are:
 * - PUBLIC `fun interface LobbyClock { fun hourOfDay(): Float }` with `companion object { const val NEUTRAL_HOUR = 13f;
 *   val System: LobbyClock; fun fixed(hour: Float): LobbyClock }` — public because the public LifeGameApp takes
 *   `lobbyClock: LobbyClock = LobbyClock.System`. Hours run 0f until 24f, device local time.
 * - `@Composable internal fun rememberLobbyHour(clock: LobbyClock): Float` — read once per lobby composition and again
 *   on every resume; it never ticks while the lobby is open.
 *
 * HomeScreen's default `hourOfDay = LobbyClock.NEUTRAL_HOUR` (13 h) gives [LobbyLook.Day], today's lobby exactly, so
 * direct HomeScreen tests stay deterministic. Drawing follows SceneAtmosphere: brushes are built in drawWithCache,
 * per frame only translation and alpha change, SrcOver only (no blend modes, no offscreen layers), and the ambient
 * phase is read only inside draw lambdas.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.lifegame.ui.components.LobbyTimeStyle
import com.example.lifegame.ui.components.rememberReduceMotion
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** Where the lobby reads the time of day from. Tests and the comparison screen inject [fixed]. */
fun interface LobbyClock {
    /** Local time of day in hours, 0f until 24f (18.5f = 18:30). */
    fun hourOfDay(): Float

    companion object {
        /** Early afternoon: the lobby exactly as it looked before time-of-day skies. */
        const val NEUTRAL_HOUR = 13f

        /** The device's local time. */
        val System: LobbyClock = LobbyClock { LocalTime.now().let { it.hour + it.minute / 60f } }

        fun fixed(hour: Float): LobbyClock = LobbyClock { hour }
    }
}

/**
 * The hour the lobby shows: read from [clock] when the lobby composes and again on every resume (coming back from
 * another screen or from the background), so it never ticks while the lobby is open.
 */
@Composable
internal fun rememberLobbyHour(clock: LobbyClock): Float {
    var hour by remember(clock) { mutableFloatStateOf(lobbyNormalizedHour(clock.hourOfDay())) }
    LifecycleResumeEffect(clock) {
        hour = lobbyNormalizedHour(clock.hourOfDay())
        onPauseOrDispose { }
    }
    return hour
}

/** [hour] folded into 0f until 24f (24 h is midnight again); a non-finite hour falls back to the neutral hour. */
internal fun lobbyNormalizedHour(hour: Float): Float {
    if (!hour.isFinite()) return LobbyClock.NEUTRAL_HOUR
    val folded = hour % 24f
    return if (folded < 0f) folded + 24f else folded
}

/** The coarse time of day of a lobby look. Tests read it through [HomeLightKey]. */
internal enum class LobbyTimeBucket { MORNING, DAY, SUNSET, NIGHT }

/** Test-only: the bucket of the light HomeSceneBackground draws ([LobbyTimeBucket] name). */
internal val HomeLightKey = SemanticsPropertyKey<String>("HomeLight")

/**
 * One lobby light. [lightX], [lightY] are fractions of the scene, [lightRadius] a fraction of its larger side.
 * [gradeTop] / [gradeBottom] carry their own alpha (transparent = no grade). [moteAlpha] and [moteScale] scale the
 * existing rising motes, and [stars], [fireflies], [shafts] (0..1) the LIVING particles.
 */
@Immutable
internal data class LobbyLook(
    val lightColor: Color,
    val lightAlpha: Float,
    val lightX: Float,
    val lightY: Float,
    val lightRadius: Float,
    val gradeTop: Color,
    val gradeBottom: Color,
    val moteColor: Color,
    val moteAlpha: Float,
    val moteScale: Float,
    val stars: Float,
    val fireflies: Float,
    val shafts: Float,
    val bucket: LobbyTimeBucket,
) {
    val hasGrade: Boolean get() = gradeTop.alpha > 0f || gradeBottom.alpha > 0f
    val hasParticles: Boolean get() = stars > 0f || fireflies > 0f || shafts > 0f

    companion object {
        /** Today's lobby: exactly the literals HomeSceneBackground drew before stage 2. */
        val Day = LobbyLook(
            lightColor = Color(0xFFFFDEA0), lightAlpha = 0.15f, lightX = 0.82f, lightY = 0.16f, lightRadius = 0.65f,
            gradeTop = Color.Transparent, gradeBottom = Color.Transparent,
            moteColor = Color(0xFFFFE7B7), moteAlpha = 1f, moteScale = 1f,
            stars = 0f, fireflies = 0f, shafts = 0f, bucket = LobbyTimeBucket.DAY,
        )
    }
}

// Keyframes of the day (GRADE colours; the particle amounts are LIVING's and GRADE drops them).
private val LobbyNightLook = LobbyLook(
    lightColor = Color(0xFFCFE0FF), lightAlpha = 0.10f, lightX = 0.20f, lightY = 0.10f, lightRadius = 0.50f,
    gradeTop = Color(0xFF0A1630).copy(alpha = 0.42f), gradeBottom = Color(0xFF0A1630).copy(alpha = 0.30f),
    moteColor = Color(0xFFFFE7B7), moteAlpha = 0f, moteScale = 1f,
    stars = 1f, fireflies = 1f, shafts = 0f, bucket = LobbyTimeBucket.NIGHT,
)
private val LobbyDawnLook = LobbyLook(
    lightColor = Color(0xFFFFE4C4), lightAlpha = 0.20f, lightX = 0.14f, lightY = 0.30f, lightRadius = 0.75f,
    gradeTop = Color(0xFF9FB8D8).copy(alpha = 0.16f), gradeBottom = Color(0xFFFFD8A8).copy(alpha = 0.10f),
    moteColor = Color(0xFFFFF1D6), moteAlpha = 0.8f, moteScale = 1f,
    stars = 0.25f, fireflies = 0f, shafts = 0.7f, bucket = LobbyTimeBucket.MORNING,
)
private val LobbyMorningLook = LobbyLook(
    lightColor = Color(0xFFFFF0D2), lightAlpha = 0.20f, lightX = 0.22f, lightY = 0.20f, lightRadius = 0.72f,
    gradeTop = Color(0xFFCFE3F4).copy(alpha = 0.10f), gradeBottom = Color(0xFFCFE3F4).copy(alpha = 0f),
    moteColor = Color(0xFFFFF4DE), moteAlpha = 1f, moteScale = 1f,
    stars = 0f, fireflies = 0f, shafts = 1f, bucket = LobbyTimeBucket.MORNING,
)
private val LobbySunsetLook = LobbyLook(
    lightColor = Color(0xFFFFA45E), lightAlpha = 0.30f, lightX = 0.88f, lightY = 0.40f, lightRadius = 0.85f,
    gradeTop = Color(0xFF4E3566).copy(alpha = 0.22f), gradeBottom = Color(0xFFFF9858).copy(alpha = 0.18f),
    // Embers: the existing motes, recoloured and a little larger.
    moteColor = Color(0xFFFFC07A), moteAlpha = 1f, moteScale = 1.35f,
    stars = 0f, fireflies = 0f, shafts = 0f, bucket = LobbyTimeBucket.SUNSET,
)
private val LobbyDuskLook = LobbyLook(
    lightColor = Color(0xFFFF8E66), lightAlpha = 0.14f, lightX = 0.92f, lightY = 0.55f, lightRadius = 0.80f,
    gradeTop = Color(0xFF18203F).copy(alpha = 0.36f), gradeBottom = Color(0xFF9C4E4A).copy(alpha = 0.16f),
    moteColor = Color(0xFFFFC07A), moteAlpha = 0.35f, moteScale = 1.2f,
    stars = 0.55f, fireflies = 0.5f, shafts = 0f, bucket = LobbyTimeBucket.SUNSET,
)

/** (hour, look) keyframes over one day; neighbours are blended with a smoothstep. 24 h repeats 0 h. */
private val LobbyDayKeyframes: List<Pair<Float, LobbyLook>> = listOf(
    0f to LobbyNightLook, 4.5f to LobbyNightLook, 6f to LobbyDawnLook, 8f to LobbyMorningLook,
    11f to LobbyLook.Day, 15.5f to LobbyLook.Day, 17.6f to LobbySunsetLook, 19.2f to LobbyDuskLook,
    20.5f to LobbyNightLook, 24f to LobbyNightLook,
)

/** MORNING [5, 11), DAY [11, 16.5), SUNSET [16.5, 19.5), NIGHT otherwise. */
internal fun lobbyTimeBucket(hour: Float): LobbyTimeBucket {
    val h = lobbyNormalizedHour(hour)
    return when {
        h >= 5f && h < 11f -> LobbyTimeBucket.MORNING
        h >= 11f && h < 16.5f -> LobbyTimeBucket.DAY
        h >= 16.5f && h < 19.5f -> LobbyTimeBucket.SUNSET
        else -> LobbyTimeBucket.NIGHT
    }
}

/**
 * The lobby's look at [hour] for [style]. NONE is always [LobbyLook.Day]; GRADE blends the keyframes (no particles);
 * LIVING adds the particles. Every style returns exactly [LobbyLook.Day] from 11 h until 15.5 h.
 */
internal fun lobbyLookAt(hour: Float, style: LobbyTimeStyle): LobbyLook {
    if (style == LobbyTimeStyle.NONE) return LobbyLook.Day
    val h = lobbyNormalizedHour(hour)
    val next = LobbyDayKeyframes.indexOfFirst { it.first > h }.coerceIn(1, LobbyDayKeyframes.lastIndex)
    val (fromHour, from) = LobbyDayKeyframes[next - 1]
    val (toHour, to) = LobbyDayKeyframes[next]
    val t = ((h - fromHour) / (toHour - fromHour)).coerceIn(0f, 1f)
    val blended = lobbyBlend(from, to, t * t * (3f - 2f * t)).copy(bucket = lobbyTimeBucket(h))
    return if (style == LobbyTimeStyle.GRADE) blended.copy(stars = 0f, fireflies = 0f, shafts = 0f) else blended
}

private fun lobbyBlend(a: LobbyLook, b: LobbyLook, t: Float): LobbyLook = when {
    a == b || t <= 0f -> a
    t >= 1f -> b
    else -> LobbyLook(
        lightColor = lerp(a.lightColor, b.lightColor, t),
        lightAlpha = lobbyLerp(a.lightAlpha, b.lightAlpha, t),
        lightX = lobbyLerp(a.lightX, b.lightX, t),
        lightY = lobbyLerp(a.lightY, b.lightY, t),
        lightRadius = lobbyLerp(a.lightRadius, b.lightRadius, t),
        gradeTop = lobbyTintLerp(a.gradeTop, b.gradeTop, t),
        gradeBottom = lobbyTintLerp(a.gradeBottom, b.gradeBottom, t),
        // Motes that are off on one side keep the other side's colour while they fade.
        moteColor = when {
            a.moteAlpha <= 0f -> b.moteColor
            b.moteAlpha <= 0f -> a.moteColor
            else -> lerp(a.moteColor, b.moteColor, t)
        },
        moteAlpha = lobbyLerp(a.moteAlpha, b.moteAlpha, t),
        moteScale = lobbyLerp(a.moteScale, b.moteScale, t),
        stars = lobbyLerp(a.stars, b.stars, t),
        fireflies = lobbyLerp(a.fireflies, b.fireflies, t),
        shafts = lobbyLerp(a.shafts, b.shafts, t),
        bucket = a.bucket,
    )
}

private fun lobbyLerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** A tint that is clear on one side only fades its alpha, instead of drifting toward transparent black. */
private fun lobbyTintLerp(a: Color, b: Color, t: Float): Color = when {
    a.alpha <= 0f && b.alpha <= 0f -> a
    a.alpha <= 0f -> b.copy(alpha = b.alpha * t)
    b.alpha <= 0f -> a.copy(alpha = a.alpha * (1f - t))
    else -> lerp(a, b, t)
}

private fun lobbyFrac(value: Float): Float = value - floor(value)

private val LobbyStarColor = Color(0xFFF3F6FF)
private val LobbyFireflyCore = Color(0xFFF8FFD0)
private val LobbyFireflyGlow = Color(0xFFD9F27A)
private const val LOBBY_STAR_COUNT = 22
private const val LOBBY_FIREFLY_COUNT = 9

/** Twinkle factor 0.1..1 of star [index] at ambient [phase]; integer frequencies keep the 12 s loop seamless. */
internal fun lobbyStarTwinkle(index: Int, phase: Float): Float {
    val k = 1 + index % 3
    val seed = lobbyFrac(index * 0.53f + 0.21f)
    return 0.55f + 0.45f * sin(2f * PI.toFloat() * (k * phase + seed))
}

/** Firefly blink 0.1..1 at ambient [phase]: mostly dim, with soft flashes twice or three times per loop. */
internal fun lobbyFireflyBlink(index: Int, phase: Float): Float {
    val c = 2 + index % 2
    val seed = lobbyFrac(index * 0.37f + 0.13f)
    val wave = max(0f, sin(2f * PI.toFloat() * (c * phase + seed)))
    return wave * wave * 0.9f + 0.1f
}

/**
 * LIVING's particles over the lobby art (above HomeScreen's night gradient, below the content): night stars in the
 * top quarter, fireflies in the scene band around the avatar, morning light shafts from the light source. Compose
 * it only when [look] has particles. Under reduced motion it draws one still frame (stars at a fixed brightness,
 * fireflies at their seed positions, still shafts). No semantics, no pointer input.
 */
@Composable
internal fun LobbyTimeParticles(look: LobbyLook, phase: State<Float>, modifier: Modifier = Modifier) {
    val still = rememberReduceMotion()
    Spacer(modifier.drawWithCache {
        val glowRadius = 6.dp.toPx()
        val firefly = Brush.radialGradient(
            0f to LobbyFireflyCore, 0.3f to LobbyFireflyGlow.copy(alpha = 0.7f), 1f to LobbyFireflyGlow.copy(alpha = 0f),
            center = Offset.Zero, radius = glowRadius,
        )
        val shaftWidths = floatArrayOf(34.dp.toPx(), 22.dp.toPx(), 48.dp.toPx())
        val shaftBrushes = shaftWidths.map { width ->
            Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0f), 0.5f to Color.White, 1f to Color.White.copy(alpha = 0f),
                startX = -width / 2f, endX = width / 2f,
            )
        }
        onDrawBehind {
            val p = if (still) 0f else phase.value
            if (look.stars > 0f) drawLobbyStars(look.stars, p, still)
            if (look.fireflies > 0f) drawLobbyFireflies(look.fireflies, p, still, firefly, glowRadius)
            if (look.shafts > 0f) drawLobbyShafts(look, p, shaftWidths, shaftBrushes)
        }
    })
}

private fun DrawScope.drawLobbyStars(amount: Float, phase: Float, still: Boolean) {
    val fadeEnd = size.height * 0.26f
    repeat(LOBBY_STAR_COUNT) { index ->
        val x = lobbyFrac(index * 0.618f + 0.11f) * size.width
        val y = (0.02f + 0.22f * lobbyFrac(index * 0.371f + 0.07f)) * size.height
        val twinkle = if (still) 0.8f else lobbyStarTwinkle(index, phase)
        // Brighter near the top, fading toward 26 % of the screen where the scene begins.
        val fade = (1f - y / fadeEnd).coerceIn(0f, 1f) * 0.7f + 0.3f
        val alpha = (0.6f * amount * twinkle * fade).coerceIn(0f, 1f)
        val side = (1.2f + (index % 3) * 0.5f).dp.toPx()
        drawRect(LobbyStarColor.copy(alpha = alpha), Offset(x - side / 2f, y - side / 2f), Size(side, side))
        // Every 6th star flashes a small four-arm sparkle at the top of its twinkle.
        if (!still && index % 6 == 0 && twinkle > 0.8f) {
            val arm = 3.dp.toPx()
            val sparkle = LobbyStarColor.copy(alpha = alpha * (twinkle - 0.8f) / 0.2f)
            val stroke = 0.8.dp.toPx()
            drawLine(sparkle, Offset(x - arm, y), Offset(x + arm, y), stroke, StrokeCap.Round)
            drawLine(sparkle, Offset(x, y - arm), Offset(x, y + arm), stroke, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawLobbyFireflies(amount: Float, phase: Float, still: Boolean, glow: Brush, glowRadius: Float) {
    val driftX = 8.dp.toPx()
    val driftY = 4.5.dp.toPx()
    val turn = 2f * PI.toFloat()
    repeat(LOBBY_FIREFLY_COUNT) { index ->
        val baseX = (0.10f + 0.80f * lobbyFrac(index * 0.381f + 0.17f)) * size.width
        val baseY = (0.36f + 0.24f * lobbyFrac(index * 0.587f + 0.31f)) * size.height
        val seed = lobbyFrac(index * 0.27f + 0.05f)
        // A small Lissajous drift with integer frequencies (1–2 per 12 s loop).
        val x = if (still) baseX else baseX + driftX * sin(turn * ((1 + index % 2) * phase + seed))
        val y = if (still) baseY else baseY + driftY * sin(turn * ((2 - index % 2) * phase + seed * 1.7f))
        val alpha = (if (still) 0.55f else 0.8f * lobbyFireflyBlink(index, phase)) * amount
        translate(x, y) { drawCircle(glow, radius = glowRadius, center = Offset.Zero, alpha = alpha.coerceIn(0f, 1f)) }
    }
}

private fun DrawScope.drawLobbyShafts(look: LobbyLook, phase: Float, widths: FloatArray, brushes: List<Brush>) {
    val light = Offset(size.width * look.lightX, size.height * look.lightY)
    val length = size.height * 1.1f
    val offsets = floatArrayOf(-30f, 10f, 46f)
    val alphas = floatArrayOf(0.06f, 0.05f, 0.07f)
    val drift = 10.dp.toPx()
    val segments = 6
    // Bands fall 24° from vertical, away from the light, each fading out along its length.
    rotate(-24f, light) {
        for (band in offsets.indices) {
            val shift = offsets[band].dp.toPx() + drift * sin(2f * PI.toFloat() * (phase + band * 0.31f))
            translate(light.x + shift, light.y) {
                val width = widths[band]
                for (segment in 0 until segments) {
                    val alpha = alphas[band] * look.shafts * (1f - segment / segments.toFloat())
                    drawRect(brushes[band], Offset(-width / 2f, length * segment / segments), Size(width, length / segments), alpha = alpha)
                }
            }
        }
    }
}
