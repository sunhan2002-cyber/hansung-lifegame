package com.example.lifegame.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.dp
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.ui.OutcomeUi
import com.example.lifegame.ui.components.BigResultStyle
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionHaptic
import com.example.lifegame.ui.components.awaitFrameMillis
import com.example.lifegame.ui.components.dampedShake
import com.example.lifegame.ui.components.drawOffset
import com.example.lifegame.ui.components.drawRimGlow
import com.example.lifegame.ui.components.rememberMotionHaptics
import com.example.lifegame.ui.components.rememberOneShotClock
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.components.statChangeSpring
import com.example.lifegame.ui.theme.LifeGameGold
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameStatDown
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/*
 * play · 큰 결과 연출 (bigResult). 결과가 클 때만 짧게 흔들리거나 빛이 번지고 진동이 세진다. 평범한 결과는 조용하다.
 *
 * - 등급은 UI가 이미 가진 OutcomeUi(변화량 · 새 사실 · 열린 길 · 사망)와 사건 종류만으로 정한다([resultImpactOf]).
 * - 변화 칩과 같은 한 번 규칙: 끝나지 않은 결과가 처음 보일 때 한 번. 복원 · 다시 연 앱(이미 CONSUMED) · 장면 보기에서
 *   돌아올 때는 다시 하지 않는다. 시작 시점 t0은 실시간 선택이면 PICK_RESULT_DELAY_MS, 아니면 0이다.
 * - 흔들림은 스크롤된 본문에만 그리기 단계로 준다(위 상단 줄 · 시기선 · 그림은 그대로). 빛 · 그늘은 의미 노드도 터치도
 *   없는 Canvas다. 난수를 쓰지 않는다.
 * - 진동은 의미가 있는 진동이라 rememberMotionHaptics(‘버튼 진동’만 따른다, 움직임 줄이기에서도 울림)를 쓴다.
 *   NONE은 아무 진동도 더하지 않는다.
 */

/** How much a result deserves, from quiet to the end of a life. */
internal enum class ResultImpactTier { CALM, NOTABLE, BIG, FINAL }

/** A result's tier and whether it reads as good news (gold) or bad news (red). */
internal data class ResultImpact(val tier: ResultImpactTier, val good: Boolean)

/** 테스트가 결과 카드의 등급을 읽을 수 있게 한 번 붙이는 의미 속성. 화면에는 보이지 않는다. */
internal val ResultImpactTierKey = SemanticsPropertyKey<ResultImpactTier>("ResultImpactTier")
internal var SemanticsPropertyReceiver.resultImpactTier by ResultImpactTierKey

private const val HEALTH = "건강"

/**
 * The tier of [outcome] in a decision of [kind] (thresholds calibrated on the authored outcomes: about 57% calm,
 * 31% notable, 12% big):
 * - FINAL when the life ends;
 * - BIG for a health loss or a total loss of 15+, a new 업적 / 가족 / 결혼 / 이혼 fact, or a big incident that moves
 *   the stats by 10+ in all;
 * - NOTABLE for any new fact or opened path, a health loss of 5+, or 12+ moved in all;
 * - CALM otherwise.
 * It is good unless it is a divorce or costs 15+ health, and either a milestone or a net gain.
 */
internal fun resultImpactOf(outcome: OutcomeUi, kind: DecisionKind): ResultImpact {
    val healthLoss = max(0, -(outcome.changes.firstOrNull { it.name == HEALTH }?.delta ?: 0))
    val totalLoss = outcome.changes.filter { it.delta < 0 }.sumOf { -it.delta }
    val moved = outcome.changes.sumOf { abs(it.delta) }
    val net = outcome.changes.sumOf { it.delta }
    val divorce = outcome.factChanges.any { it.category == "관계" && it.text == "이혼" }
    val lifeEvent = outcome.factChanges.any {
        it.category == "업적" || it.category == "가족" || (it.category == "관계" && (it.text == "결혼" || it.text == "이혼"))
    }
    val tier = when {
        outcome.endsLife -> ResultImpactTier.FINAL
        healthLoss >= 15 || totalLoss >= 15 || lifeEvent || (kind == DecisionKind.BIG_INCIDENT && moved >= 10) -> ResultImpactTier.BIG
        outcome.factChanges.isNotEmpty() || outcome.openedEvents.isNotEmpty() || healthLoss >= 5 || moved >= 12 -> ResultImpactTier.NOTABLE
        else -> ResultImpactTier.CALM
    }
    val good = !divorce && healthLoss < 15 && (kind == DecisionKind.MILESTONE || net >= 0)
    return ResultImpact(tier, good)
}

/** How long [impact]'s effect lasts after its start, per style (0: nothing to draw). */
internal fun resultImpactDurationMs(style: BigResultStyle, impact: ResultImpact): Int = when (style) {
    BigResultStyle.NONE -> 0
    BigResultStyle.THUD -> when (impact.tier) {
        ResultImpactTier.CALM -> 0
        ResultImpactTier.NOTABLE -> 500
        ResultImpactTier.BIG -> if (impact.good) 520 else 620
        ResultImpactTier.FINAL -> FINAL_TOTAL_MS
    }
    BigResultStyle.GLOW -> when (impact.tier) {
        ResultImpactTier.CALM -> 0
        ResultImpactTier.NOTABLE -> 700
        ResultImpactTier.BIG -> 900
        ResultImpactTier.FINAL -> FINAL_TOTAL_MS
    }
}

/** The haptic of [impact] (null: none). FINAL buzzes once at the ink vignette's peak. */
internal fun resultImpactHaptic(impact: ResultImpact): MotionHaptic? = when (impact.tier) {
    ResultImpactTier.CALM -> null
    ResultImpactTier.NOTABLE -> MotionHaptic.CONTEXT
    ResultImpactTier.BIG -> if (impact.good) MotionHaptic.CONFIRM else MotionHaptic.REJECT
    ResultImpactTier.FINAL -> MotionHaptic.HEAVY
}

private const val FINAL_RISE_MS = 480f
private const val FINAL_HOLD_MS = 320f
private const val FINAL_FALL_MS = 900f
private const val FINAL_TOTAL_MS = 1_700

/**
 * The scrolled content's offset in dp [tMs] after the impact (THUD only; GLOW never moves): a good NOTABLE lifts
 * 3dp and settles, a bad one shakes 2.5dp; a good BIG lands from 6dp lower on a bouncy spring, a bad one shakes 6dp.
 */
internal fun resultImpactOffsetDp(style: BigResultStyle, impact: ResultImpact, tMs: Float): Offset {
    if (style != BigResultStyle.THUD || tMs < 0f) return Offset.Zero
    return when (impact.tier) {
        ResultImpactTier.NOTABLE -> when {
            impact.good -> if (tMs > 400f) Offset.Zero else Offset(0f, -3f * (1f - statChangeSpring(tMs, 0.6f, 650f)))
            tMs > 160f -> Offset.Zero
            else -> Offset(dampedShake(tMs, 2.5f, 55f, 35f), 0f)
        }
        ResultImpactTier.BIG -> when {
            impact.good -> if (tMs > 500f) Offset.Zero else Offset(0f, 6f * (1f - statChangeSpring(tMs, 0.42f, 700f)))
            tMs > 420f -> Offset.Zero
            else -> Offset(dampedShake(tMs, 6f, 62f, 120f), 0f)
        }
        ResultImpactTier.CALM, ResultImpactTier.FINAL -> Offset.Zero
    }
}

/** A swell that rises to 1 at [peakMs] and falls back to 0 at [endMs]. */
internal fun resultSwell(tMs: Float, peakMs: Float, endMs: Float): Float = when {
    tMs <= 0f || tMs >= endMs -> 0f
    tMs < peakMs -> FastOutSlowInEasing.transform(tMs / peakMs)
    else -> 1f - FastOutSlowInEasing.transform((tMs - peakMs) / (endMs - peakMs))
}

/** FINAL's ink vignette: up to 0.5 over 480 ms, held 320 ms, back to 0 over 900 ms. */
internal fun resultFinalInk(tMs: Float): Float = when {
    tMs <= 0f -> 0f
    tMs < FINAL_RISE_MS -> 0.5f * FastOutSlowInEasing.transform(tMs / FINAL_RISE_MS)
    tMs < FINAL_RISE_MS + FINAL_HOLD_MS -> 0.5f
    tMs < FINAL_TOTAL_MS -> 0.5f * (1f - FastOutSlowInEasing.transform((tMs - FINAL_RISE_MS - FINAL_HOLD_MS) / FINAL_FALL_MS))
    else -> 0f
}

/**
 * The result impact as one composition sees it. [impact] is null when nothing plays (no result, CALM, NONE, or a
 * result already shown); [ms] counts from the frame the result appeared and the effect starts at [startMs].
 * Read [ms] only in draw lambdas.
 */
@Stable
internal class ResultImpactMotion internal constructor(
    val impact: ResultImpact?,
    val style: BigResultStyle,
    val startMs: Float,
    private val clock: State<Float>,
) {
    internal val ms: Float get() = clock.value

    /** Draw-phase offset of the scrolled content (and of a picked card's exit, so they move together). */
    fun contentOffset(scope: DrawScope): Offset {
        val impact = impact ?: return Offset.Zero
        val dp = resultImpactOffsetDp(style, impact, ms - startMs)
        if (dp == Offset.Zero) return Offset.Zero
        return with(scope) { Offset(dp.x.dp.toPx(), dp.y.dp.toPx()) }
    }
}

/**
 * The impact of the result on screen. [key] is the screen's introKey (it changes with every result);
 * [shownBefore] is true when the stored presentation already consumed this result; [startDelayMillis] is t0
 * (PICK_RESULT_DELAY_MS after a live pick). The tier haptic fires once at t0 even under reduced motion.
 */
@Composable
internal fun rememberResultImpact(
    outcome: OutcomeUi?,
    kind: DecisionKind,
    key: String,
    shownBefore: Boolean,
    startDelayMillis: Int,
): ResultImpactMotion {
    val style = LocalMotionStyle.current.bigResult
    val reduceMotion = rememberReduceMotion()
    val impact = remember(outcome, kind) { outcome?.let { resultImpactOf(it, kind) } }
    // 칩과 같은 한 번 규칙. 저장 가능한 표시라 회전 · 복원 뒤에는 다시 하지 않는다.
    var played by rememberSaveable(key) { mutableStateOf(shownBefore) }
    val playNow = remember(key) { !played && outcome != null }
    val startMs = remember(key) { startDelayMillis.coerceAtLeast(0) }
    val effective = impact?.takeIf {
        playNow && style != BigResultStyle.NONE && it.tier != ResultImpactTier.CALM
    }
    val total = if (effective == null) 0 else startMs + resultImpactDurationMs(style, effective)
    val clock = rememberOneShotClock(key = key, active = effective != null, totalMs = total)
    val haptics = rememberMotionHaptics()
    LaunchedEffect(key) {
        if (!playNow) return@LaunchedEffect
        played = true
        val haptic = effective?.let(::resultImpactHaptic) ?: return@LaunchedEffect
        val atPeak = effective.tier == ResultImpactTier.FINAL && !reduceMotion
        awaitFrameMillis(startMs + if (atPeak) FINAL_RISE_MS.toLong() else 0L)
        haptics(haptic)
    }
    return remember(effective, style, startMs, clock) { ResultImpactMotion(effective, style, startMs.toFloat(), clock) }
}

/** Offsets the scrolled content by the impact (draw only). Put it right after the content's verticalScroll. */
internal fun Modifier.resultImpactShake(motion: ResultImpactMotion): Modifier =
    if (motion.impact == null) this else drawOffset { motion.contentOffset(this) }

/**
 * A one-time rim glow around the outcome card (gold for good news, red for bad): THUD's NOTABLE and every GLOW
 * tier but FINAL. Put it on the box that holds the card, before anything that clips, so it can spill out.
 */
internal fun Modifier.resultImpactGlow(motion: ResultImpactMotion): Modifier {
    val impact = motion.impact ?: return this
    return drawBehind {
        val t = motion.ms - motion.startMs
        if (t < 0f) return@drawBehind
        val color = if (impact.good) LifeGameGold else LifeGameStatDown
        val corner = 12.dp.toPx()
        when (motion.style) {
            BigResultStyle.THUD -> if (impact.tier == ResultImpactTier.NOTABLE) {
                drawRimGlow(color, alpha = 0.25f * (1f - t / 500f).coerceIn(0f, 1f), reach = 6.dp.toPx(), corner = corner)
            }
            BigResultStyle.GLOW -> when (impact.tier) {
                ResultImpactTier.NOTABLE ->
                    drawRimGlow(color, alpha = 0.3f * (1f - t / 700f).coerceIn(0f, 1f), reach = 8.dp.toPx(), corner = corner)
                ResultImpactTier.BIG -> drawRimGlow(
                    color,
                    alpha = 0.45f * (1f - t / 900f).coerceIn(0f, 1f),
                    reach = (if (impact.good) 16 else 14).dp.toPx(),
                    corner = corner,
                    layers = 4,
                )
                ResultImpactTier.CALM, ResultImpactTier.FINAL -> Unit
            }
            BigResultStyle.NONE -> Unit
        }
    }
}

/** 붉은 먹(나쁜 큰 결과의 가장자리 그늘). */
private val ResultRedInk = Color(0xFF4A0E12)

/**
 * The impact's whole-screen light, above the top bar too: THUD's gold wash from the outcome card (good BIG) or red
 * ink at the edges (bad BIG), GLOW's paper-white veil or slower red edge, and FINAL's quiet ink vignette. A Canvas
 * with no semantics and no pointer input, composed only while a result plays such an effect. [center] gives the
 * outcome card's centre in this overlay's coordinates (null: the middle of the screen).
 */
@Composable
internal fun ResultImpactOverlay(motion: ResultImpactMotion, center: () -> Offset?, modifier: Modifier = Modifier) {
    val impact = motion.impact ?: return
    val screenWide = impact.tier == ResultImpactTier.FINAL || impact.tier == ResultImpactTier.BIG
    if (!screenWide) return
    Canvas(modifier) {
        val t = motion.ms - motion.startMs
        if (t <= 0f) return@Canvas
        if (impact.tier == ResultImpactTier.FINAL) {
            drawEdgeShade(LifeGameInk, resultFinalInk(t))
            return@Canvas
        }
        when (motion.style) {
            BigResultStyle.THUD -> if (impact.good) {
                val alpha = 0.30f * resultSwell(t, 80f, 520f)
                if (alpha > 0f) {
                    val origin = center() ?: Offset(size.width / 2f, size.height / 2f)
                    val radius = 0.6f * hypot(size.width, size.height)
                    drawRect(Brush.radialGradient(listOf(LifeGameGold.copy(alpha = alpha), Color.Transparent), center = origin, radius = radius))
                }
            } else {
                drawEdgeShade(ResultRedInk, 0.45f * resultSwell(t, 70f, 620f))
            }
            BigResultStyle.GLOW -> if (impact.good) {
                val alpha = 0.14f * resultSwell(t, 60f, 380f)
                if (alpha > 0f) drawRect(LifeGamePaper.copy(alpha = alpha))
            } else {
                drawEdgeShade(ResultRedInk, 0.32f * resultSwell(t, 120f, 800f))
            }
            BigResultStyle.NONE -> Unit
        }
    }
}

/** A vignette: clear in the middle (to 55% of the half-diagonal), [color] at [alpha] in the corners. */
private fun DrawScope.drawEdgeShade(color: Color, alpha: Float) {
    if (alpha <= 0f) return
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent,
            0.55f to Color.Transparent,
            1f to color.copy(alpha = alpha.coerceIn(0f, 1f)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = hypot(size.width, size.height) / 2f,
        ),
    )
}
