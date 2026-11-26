package com.example.lifegame.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val CompletionEffectProgress = SemanticsPropertyKey<Float>("CompletionEffectProgress")
private const val COMPLETION_DURATION_MS = 720

/**
 * Only a saved completed record opts in. This component cannot award a title or mutate a run.
 * [badgeModifier] is chained after the title badge's size and tag; the title-unlock popup (lobby2) uses it only
 * as the landing spot of its medal (it must never move, hide or re-time the badge).
 */
@Composable
internal fun CompletionSealHeader(
    runId: String,
    titleEarned: Boolean,
    celebrate: Boolean,
    onConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    badgeModifier: Modifier = Modifier,
) {
    val eligibleAtEntry = remember(runId) { celebrate }
    var started by rememberSaveable(runId) { mutableStateOf(false) }
    val progress = remember(runId) { Animatable(1f) }
    val consumed by rememberUpdatedState(onConsumed)
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val reduced = rememberReduceMotion()
    LaunchedEffect(runId, lifecycle, reduced) {
        if (lifecycle != Lifecycle.State.RESUMED) {
            progress.snapTo(1f)
        } else if (!started) {
            started = true
            if (eligibleAtEntry) consumed()
            if (eligibleAtEntry && !reduced) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(COMPLETION_DURATION_MS, easing = FastOutSlowInEasing))
            } else progress.snapTo(1f)
        } else {
            // A background pause or runtime motion-setting change settles, never restarts, a reveal.
            progress.snapTo(1f)
        }
    }
    Box(
        modifier.fillMaxWidth().heightIn(min = 58.dp)
            .testTag("completion_header")
            .semantics { this[CompletionEffectProgress] = progress.value },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val p = progress.value
            val light = sin(PI * p).toFloat().coerceAtLeast(0f)
            if (light > 0f) {
                val x = size.width * p
                drawLine(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFFC79B47).copy(alpha = light * .7f), Color.Transparent),
                        startX = x - size.width * .24f, endX = x + size.width * .24f),
                    Offset(0f, size.height - 2.dp.toPx()), Offset(size.width, size.height - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "인생기록증", color = LifeGameInkSecondary, fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (titleEarned) TitleMedallion(Modifier.size(56.dp).testTag("certificate_title_badge").then(badgeModifier)) { progress.value }
        }
    }
}

/** Small dimensional illustration; no input handler, random numbers, timer, or spoken duplicate. */
@Composable
internal fun TitleMedallion(modifier: Modifier = Modifier, progress: () -> Float = { 1f }) {
    Canvas(modifier.graphicsLayer {
        val p = progress()
        scaleX = .82f + p * .18f
        scaleY = scaleX
        rotationY = (1f - p) * -24f
        rotationZ = (1f - p) * -8f
        cameraDistance = 12f * density
    }) {
        val edge = size.minDimension
        val centre = Offset(size.width / 2f, size.height * .44f)
        val radius = edge * .31f
        fun ribbon(side: Float) = Path().apply {
            moveTo(centre.x + side * edge * .04f, centre.y + edge * .15f)
            lineTo(centre.x + side * edge * .23f, centre.y + edge * .11f)
            lineTo(centre.x + side * edge * .27f, centre.y + edge * .48f)
            lineTo(centre.x + side * edge * .15f, centre.y + edge * .39f)
            lineTo(centre.x + side * edge * .04f, centre.y + edge * .46f)
            close()
        }
        drawPath(ribbon(-1f), Color(0xFF5D6750))
        drawPath(ribbon(1f), Color(0xFF3E4D40))
        drawCircle(Color.Black.copy(alpha = .13f), radius + edge * .025f, centre + Offset(0f, edge * .045f))
        drawCircle(Brush.linearGradient(listOf(Color(0xFF916332), Color(0xFFECCB7D), Color(0xFF936530)),
            centre - Offset(radius, radius), centre + Offset(radius, radius)), radius, centre)
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFEDB7), Color(0xFFDAAD55), Color(0xFFB77D38)),
            centre - Offset(radius, radius), centre + Offset(radius, radius)), radius * .86f, centre)
        drawCircle(Color(0xFFFFF3CE).copy(alpha = .8f), radius * .83f, centre, style = Stroke(edge * .014f))
        val star = Path().apply {
            repeat(10) { index ->
                val angle = -PI / 2 + index * PI / 5
                val r = radius * if (index % 2 == 0) .55f else .25f
                val x = centre.x + cos(angle).toFloat() * r
                val y = centre.y + sin(angle).toFloat() * r
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(star, Color(0xFF8E612D))
        drawPath(star, Color(0xFFFFE5A0), style = Stroke(edge * .01f))
        val glow = sin(PI * progress()).toFloat().coerceAtLeast(0f)
        if (glow > 0f) {
            listOf(-1f, 1f).forEach { side ->
                val point = centre + Offset(side * edge * (.39f + .05f * progress()), -edge * .21f)
                drawLine(Color(0xFFD6AF61).copy(alpha = glow), point - Offset(edge * .045f, 0f), point + Offset(edge * .045f, 0f), edge * .02f)
                drawLine(Color(0xFFD6AF61).copy(alpha = glow), point - Offset(0f, edge * .045f), point + Offset(0f, edge * .045f), edge * .02f)
            }
        }
    }
}
