package com.example.lifegame.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.unit.IntSize
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.images.LoadedPixelAsset
import kotlin.math.abs

/** Two brief eyelid closures in the existing twelve-second ambient cycle; no extra timer or RNG. */
internal fun avatarEyeOpenness(phase: Float): Float {
    val distance = minOf(abs(phase - .28f), abs(phase - .73f))
    return (distance / .0075f).coerceIn(.08f, 1f)
}

/** Authored eye rows, excluding eyebrows. Aged layers keep their wrinkles outside this band. */
internal fun eyeRows(look: AvatarLook): IntRange {
    val narrow = look.eyesId == "eyes_narrow"
    val big = look.eyesId == "eyes_big"
    return when (look.period) {
        LifePeriod.INFANT -> 180 until if (narrow) 185 else 191
        LifePeriod.CHILD -> 133 until if (narrow) 139 else 144
        LifePeriod.TEEN -> 77 until if (narrow) 83 else 89
        LifePeriod.YOUNG_ADULT -> (if (big || narrow) 71 else 72) until if (narrow) 78 else 86
        LifePeriod.MIDDLE -> 72 until if (narrow) 79 else 85
        LifePeriod.OLD_AGE -> 83 until if (narrow) 90 else 96
    }
}

/**
 * Draw the original transparent eye layer, compressing only its eye band for a blink.
 * Brows, wrinkles, hair, saved appearance and photo faces are never substituted.
 * This reads the shared phase during drawing, so the avatar tree does not recompose per frame.
 */
@Composable
internal fun BlinkingEyeLayer(loaded: LoadedPixelAsset, look: AvatarLook, phase: State<Float>, modifier: Modifier) {
    val bitmap = remember(loaded.bitmap) { loaded.bitmap.asImageBitmap() }
    val rows = remember(look.period, look.eyesId) { eyeRows(look) }
    Canvas(modifier) {
        val factor = minOf(size.width / bitmap.width, size.height / bitmap.height)
        val left = (size.width - bitmap.width * factor) / 2f
        val top = size.height - bitmap.height * factor
        val eyeTop = rows.first * bitmap.height / PhotoFaceSheet.CANVAS_HEIGHT.toFloat()
        val eyeBottom = (rows.last + 1) * bitmap.height / PhotoFaceSheet.CANVAS_HEIGHT.toFloat()
        val openness = avatarEyeOpenness(phase.value)
        translate(left, top) {
            scale(factor, factor, Offset.Zero) {
                if (openness == 1f) {
                    drawImage(bitmap, dstSize = IntSize(bitmap.width, bitmap.height), filterQuality = FilterQuality.None)
                } else {
                    clipRect(bottom = eyeTop) { drawImage(bitmap, dstSize = IntSize(bitmap.width, bitmap.height), filterQuality = FilterQuality.None) }
                    clipRect(top = eyeBottom) { drawImage(bitmap, dstSize = IntSize(bitmap.width, bitmap.height), filterQuality = FilterQuality.None) }
                    scale(1f, openness, Offset(0f, (eyeTop + eyeBottom) / 2f)) {
                        clipRect(top = eyeTop, bottom = eyeBottom) { drawImage(bitmap, dstSize = IntSize(bitmap.width, bitmap.height), filterQuality = FilterQuality.None) }
                    }
                }
            }
        }
    }
}
