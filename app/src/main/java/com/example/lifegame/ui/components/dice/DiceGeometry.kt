package com.example.lifegame.ui.components.dice

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Presentation math only. No RNG, game state or result callbacks belong in the renderer. */
internal object DiceGeometry {
    const val ROLL_DURATION_MS = 490L
    data class Rotation(val x: Float, val y: Float)
    data class Pose(val rotation: Rotation, val lift: Float, val scale: Float)

    // Opposite faces sum to seven: +Z=1, -Z=6, +X=3, -X=4, +Y=2, -Y=5.
    fun finalRotation(value: Int): Rotation = when (value.coerceIn(1, 6)) {
        1 -> Rotation(0f, 0f)
        2 -> Rotation(90f, 0f)
        3 -> Rotation(0f, -90f)
        4 -> Rotation(0f, 90f)
        5 -> Rotation(-90f, 0f)
        else -> Rotation(0f, 180f)
    }

    fun frontNormal(value: Int): FloatArray = when (value.coerceIn(1, 6)) {
        1 -> floatArrayOf(0f, 0f, 1f)
        2 -> floatArrayOf(0f, 1f, 0f)
        3 -> floatArrayOf(1f, 0f, 0f)
        4 -> floatArrayOf(-1f, 0f, 0f)
        5 -> floatArrayOf(0f, -1f, 0f)
        else -> floatArrayOf(0f, 0f, -1f)
    }

    fun rotatedFrontNormal(value: Int): FloatArray {
        val normal = frontNormal(value)
        val rotation = finalRotation(value)
        val rx = rotation.x * PI / 180.0
        val ry = rotation.y * PI / 180.0
        val y = normal[1] * cos(rx) - normal[2] * sin(rx)
        val z = normal[1] * sin(rx) + normal[2] * cos(rx)
        return floatArrayOf((normal[0] * cos(ry) + z * sin(ry)).toFloat(), y.toFloat(), (-normal[0] * sin(ry) + z * cos(ry)).toFloat())
    }

    fun pose(value: Int, elapsedMs: Long, animate: Boolean, second: Boolean): Pose {
        val final = finalRotation(value)
        if (!animate || elapsedMs >= ROLL_DURATION_MS) return Pose(final, 0f, 1f)
        val progress = (elapsedMs.toFloat() / ROLL_DURATION_MS).coerceIn(0f, 1f)
        val remaining = (1f - progress) * (1f - progress) * (1f - progress)
        return Pose(
            Rotation(final.x + remaining * if (second) -540f else 540f, final.y + remaining * 720f),
            (sin(progress * PI) * 0.34).toFloat(),
            1f + (sin(progress * PI) * 0.06).toFloat(),
        )
    }
}
