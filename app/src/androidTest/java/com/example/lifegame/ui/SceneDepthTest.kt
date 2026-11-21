package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.ui.components.SceneDepthMesh
import com.example.lifegame.ui.components.SceneImageSource
import com.example.lifegame.ui.components.sceneAtmosphereFor
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.images.coverPlacement
import java.io.File
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneDepthTest {
    @Test fun inspectedDepthMovesForegroundWhileKeepingDistantArtworkAndReturningExactlyToRest() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val routing = ImageRoutingRepository(context).load()
        val examples = mapOf("teen_024" to "img_teen_wrong_answer_001", "adult_072" to "img_pov_first_card_001", "adult_067" to "img_adult_sunday_night_001")
        val output = requireNotNull(context.getExternalFilesDir("scene-depth")).also { it.mkdirs() }
        for ((event, id) in examples) {
            val loaded = requireNotNull(PixelAssetLoader.load(context, listOf(requireNotNull(routing.assets[id])), 360, 800))
            val source = loaded.bitmap
            val crop = coverPlacement(source.width, source.height, 360, 800, loaded.focal)
            val fitted = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888)
            AndroidCanvas(fitted).drawBitmap(source, null, RectF(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height), Paint().apply { isFilterBitmap = false })
            val profile = requireNotNull(sceneAtmosphereFor(event, SceneImageSource(loaded.candidate, source.width, source.height, loaded.focal)))
            val mesh = SceneDepthMesh(360, 800, profile)
            fun frame(phase: Float): Bitmap = Bitmap.createBitmap(360, 800, Bitmap.Config.ARGB_8888).also { bitmap ->
                CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(bitmap.asImageBitmap()), Size(360f, 800f)) {
                    mesh.draw(this, fitted.asImageBitmap(), phase)
                }
            }
            val resting = frame(0f)
            val moved = frame(.25f)
            val restored = frame(1f)
            assertTrue("$event rest must preserve source pixels", fitted.sameAs(resting))
            assertTrue("$event complete cycle must return exactly", resting.sameAs(restored))
            var nearChanges = 0
            for (y in 0 until 800) for (x in 12 until 348) {
                if (y < 400) assertEquals("$event distant art changed at $x,$y", resting.getPixel(x, y), moved.getPixel(x, y))
                if (y > 680 && resting.getPixel(x, y) != moved.getPixel(x, y)) nearChanges++
            }
            assertTrue("$event foreground did not move", nearChanges > 100)
            for ((name, bitmap) in listOf("rest" to resting, "depth" to moved)) {
                File(output, "$event-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            fitted.recycle(); resting.recycle(); moved.recycle(); restored.recycle()
        }
    }
}
