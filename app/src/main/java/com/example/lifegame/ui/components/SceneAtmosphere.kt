package com.example.lifegame.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.images.coverPlacement
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class AtmosphereKind { CLASSROOM_DAY, DESK_DAY, DESK_NIGHT }
internal data class SceneAtmosphereProfile(val kind: AtmosphereKind, val source: SceneImageSource)
internal data class SceneCameraPose(val scale: Float, val x: Float, val y: Float)
internal val SceneAtmosphereKindKey = SemanticsPropertyKey<String>("SceneAtmosphereKind")

/** Opt in only for inspected artwork. An unrelated loader fallback must keep its own lighting. */
internal fun sceneAtmosphereFor(eventId: String, source: SceneImageSource?): SceneAtmosphereProfile? {
    source ?: return null
    val candidate = source.candidate
    val kind = when {
        eventId == "teen_024" && candidate.imageId == "img_teen_wrong_answer_001" &&
            candidate.path in setOf("followup_20261031/img_teen_wrong_answer_001.png",
                "presentation/portraits/img_teen_wrong_answer_001_portrait_v1.png") -> AtmosphereKind.CLASSROOM_DAY
        eventId == "adult_072" && candidate.imageId == "img_pov_first_card_001" && candidate.path in setOf(
            "followup_20261031/img_pov_first_card_001.png",
            "presentation/portraits/img_pov_first_card_001_portrait_v1.png",
        ) -> AtmosphereKind.DESK_DAY
        eventId == "adult_067" && candidate.imageId == "img_adult_sunday_night_001" &&
            candidate.path in setOf("followup_20261031/img_adult_sunday_night_001.png",
                "presentation/portraits/img_adult_sunday_night_001_portrait_v1.png") -> AtmosphereKind.DESK_NIGHT
        else -> return null
    }
    return SceneAtmosphereProfile(kind, source)
}

internal fun sceneCameraPose(kind: AtmosphereKind, phase: Float): SceneCameraPose {
    val angle = phase * (2 * PI).toFloat()
    return when (kind) {
        AtmosphereKind.CLASSROOM_DAY -> SceneCameraPose(1.030f, sin(angle) * .004f, (cos(angle) - 1f) * .001f)
        AtmosphereKind.DESK_DAY -> SceneCameraPose(1.030f, sin(angle) * .002f, sin(angle) * .003f)
        AtmosphereKind.DESK_NIGHT -> SceneCameraPose(1.030f, sin(angle) * .0015f, (cos(angle) - 1f) * .0015f)
    }
}

/** A single artwork layer: the source composition, written details and POV hands stay intact. */
internal fun Modifier.sceneCamera(profile: SceneAtmosphereProfile?, phase: State<Float>, enabled: Boolean): Modifier = graphicsLayer {
    scaleX = 1f
    scaleY = 1f
    translationX = 0f
    translationY = 0f
    if (enabled && profile != null) {
        val pose = sceneCameraPose(profile.kind, phase.value)
        scaleX = pose.scale
        scaleY = pose.scale
        translationX = pose.x * size.width
        translationY = pose.y * size.height
    }
}

/** Map inspected source anchors through the same focal crop as the artwork, including variants. */
internal fun SceneAtmosphereProfile.anchor(x: Float, y: Float, width: Int, height: Int): Offset {
    val crop = coverPlacement(source.width, source.height, width, height, source.focal)
    return Offset(crop.left + crop.width * x, crop.top + crop.height * y)
}

/**
 * Three presentation planes: distant window light, intact artwork/camera, near reflected light
 * and motes. These are lighting planes, not invented cut-outs of people or scenery. No weather,
 * game randomness, input handlers, raster copies, blurs or offscreen alpha layers are created.
 */
@Composable
internal fun SceneAtmosphere(profile: SceneAtmosphereProfile, phase: State<Float>, modifier: Modifier = Modifier) {
    Box(modifier
        .testTag("scene_atmosphere")
        .clearAndSetSemantics { this[SceneAtmosphereKindKey] = profile.kind.name }
        .drawWithCache {
            val portraitDesk = profile.source.candidate.path.startsWith("presentation/portraits/")
            fun point(x: Float, y: Float) = profile.anchor(x, y, size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1))
            val crop = coverPlacement(profile.source.width, profile.source.height, size.width.toInt().coerceAtLeast(1), size.height.toInt().coerceAtLeast(1), profile.source.focal)
            val window = when (profile.kind) {
                AtmosphereKind.CLASSROOM_DAY -> point(.16f, .23f)
                AtmosphereKind.DESK_DAY -> if (portraitDesk) point(.50f, .20f) else point(.50f, .08f)
                AtmosphereKind.DESK_NIGHT -> if (portraitDesk) point(.65f, .20f) else point(.58f, .17f)
            }
            val nearLight = when (profile.kind) {
                AtmosphereKind.CLASSROOM_DAY -> point(.54f, .91f)
                AtmosphereKind.DESK_DAY -> if (portraitDesk) point(.67f, .54f) else point(.67f, .57f)
                AtmosphereKind.DESK_NIGHT -> if (portraitDesk) point(.25f, .60f) else point(.32f, .70f)
            }
            val night = profile.kind == AtmosphereKind.DESK_NIGHT
            val windowRadius = crop.height * if (night) .37f else .68f
            val nearRadius = crop.height * if (night) .46f else .35f
            val windowBrush = Brush.radialGradient(
                listOf((if (night) Color(0xFF9EBAFF) else Color(0xFFFFE6AC)).copy(alpha = if (night) .085f else .075f), Color.Transparent),
                center = window, radius = windowRadius,
            )
            val nearBrush = Brush.radialGradient(
                listOf(Color(0xFFFFCF85).copy(alpha = if (night) .13f else .07f), Color.Transparent),
                center = nearLight, radius = nearRadius,
            )
            // Fixed source-space locations. Faster near drift adds depth without animating
            // a teacher, written document, clock, or a player's authored POV hands.
            val motes = List(if (night) 5 else 8) { index ->
                when (profile.kind) {
                    AtmosphereKind.CLASSROOM_DAY -> point(.43f + index * .042f, .30f + index * .073f)
                    AtmosphereKind.DESK_DAY -> point(.36f + index * .043f, (if (portraitDesk) .36f else .24f) + index * .044f)
                    AtmosphereKind.DESK_NIGHT -> point(.36f + index * .045f, .46f + index * .047f)
                }
            }
            val farDrift = 2.dp.toPx()
            val nearDrift = 9.dp.toPx()
            val moteRadius = .85.dp.toPx()
            onDrawBehind {
                val angle = phase.value * (2 * PI).toFloat()
                // Cached brushes move slowly; no shader/bitmap is allocated on each frame.
                translate(left = sin(angle) * farDrift, top = cos(angle) * farDrift) {
                    drawCircle(windowBrush, windowRadius, window, alpha = .84f + sin(angle) * .16f)
                }
                translate(left = -sin(angle) * nearDrift, top = cos(angle) * nearDrift * .3f) {
                    drawCircle(nearBrush, nearRadius, nearLight, alpha = .86f + cos(angle) * .14f)
                }
                motes.forEachIndexed { index, position ->
                    val depth = if (index % 2 == 0) .55f else 1f
                    val drift = nearDrift * depth
                    val alpha = (.10f + .045f * sin(angle + index)).coerceAtLeast(0f)
                    drawCircle(Color(0xFFFFEAC2).copy(alpha = alpha), moteRadius * depth,
                        Offset(position.x + sin(angle + index) * drift, position.y + cos(angle + index * .7f) * drift * 1.5f))
                }
            }
        })
}
