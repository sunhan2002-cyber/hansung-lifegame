package com.example.lifegame.ui.components

import android.graphics.Paint
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.PI
import kotlin.math.sin

/**
 * Inspected depth bands in original-image coordinates. The intact source is used as a mesh,
 * so foreground motion cannot expose an invented clean plate or duplicate a cut-out edge.
 * Far background stays fixed; middle/near bands meet continuously without seams.
 */
internal data class SceneDepthBands(val middleStart: Float, val nearStart: Float, val nearEnd: Float)

internal fun sceneDepthBands(profile: SceneAtmosphereProfile): SceneDepthBands = when (profile.kind) {
    AtmosphereKind.CLASSROOM_DAY -> SceneDepthBands(.70f, .84f, 1f)
    AtmosphereKind.DESK_DAY -> if (profile.source.candidate.path.startsWith("presentation/portraits/"))
        SceneDepthBands(.64f, .80f, 1f) else SceneDepthBands(.64f, .84f, 1f)
    AtmosphereKind.DESK_NIGHT -> SceneDepthBands(.63f, .81f, 1f)
}

internal class SceneDepthMesh(private val width: Int, private val height: Int, profile: SceneAtmosphereProfile) {
    private val columns = 4
    private val rows = 32
    private val vertices = FloatArray((columns + 1) * (rows + 1) * 2)
    private val bands = sceneDepthBands(profile)
    private val sourceTop = profile.anchor(0f, 0f, width, height).y
    private val sourceHeight = profile.anchor(0f, 1f, width, height).y - sourceTop
    private val paint = Paint().apply { isFilterBitmap = false; isDither = false; isAntiAlias = false }

    private fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

    fun draw(scope: DrawScope, image: ImageBitmap, phase: Float) {
        val movement = sin(phase * (2 * PI).toFloat())
        var index = 0
        for (row in 0..rows) {
            val y = height * row / rows.toFloat()
            val sourceY = (y - sourceTop) / sourceHeight
            // The middle plane moves at most 0.05% of the screen; the nearest edge at 0.7%.
            // Documents, people and object silhouettes remain a single continuous raster.
            val middle = smooth((sourceY - bands.middleStart) / (bands.nearStart - bands.middleStart))
            val near = smooth((sourceY - bands.nearStart) / (bands.nearEnd - bands.nearStart))
            val depth = middle * .0005f + near * .0065f
            for (column in 0..columns) {
                val x = width * column / columns.toFloat()
                vertices[index++] = x + movement * width * depth
                vertices[index++] = y
            }
        }
        scope.drawContext.canvas.nativeCanvas.drawBitmapMesh(
            image.asAndroidBitmap(), columns, rows, vertices, 0, null, 0, paint,
        )
    }
}
