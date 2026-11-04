package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.SceneImage
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.theme.LifeGamePicture
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** PR #51에서 반려된 adult_002 파일럿의 실제 앱 중앙 크롭 회귀 검사. */
@RunWith(AndroidJUnit4::class)
class Adult002CropReviewTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val candidate = ImageCandidate(
        imageId = "img_adult_pro_offer_001",
        path = "followup_20261031/img_adult_pro_offer_001.png",
        usage = "event",
    )

    @Test
    fun revisedPilotMatchesTheRealSmallPhoneCenterCrop() {
        composeRule.setContent {
            LifeGameTheme {
                Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) {
                    SceneImage(
                        shot = FilmShot(
                            key = "adult_002",
                            imageId = candidate.imageId,
                            imageFile = candidate.path,
                            isSpecial = false,
                            candidates = listOf(candidate),
                        ),
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
        }

        val source = context.assets.open("images/${candidate.path}").use {
            android.graphics.BitmapFactory.decodeStream(it)
        }
        val root = composeRule.onRoot()
        val size = root.fetchSemanticsNode().size
        val expected = centerCrop(source, size.width, size.height)
        val match = awaitMatch(expected) { root.captureToImage().asAndroidBitmap() }
        assertTrue("adult_002 app crop must match the revised PNG ($match)", match >= 0.95f)

        val output = requireNotNull(context.getExternalFilesDir("adult-002-crop-review"))
        assertTrue(output.isDirectory || output.mkdirs())
        File(output, "adult_002_360x740_app_crop.png").outputStream().use {
            assertTrue(root.captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun awaitMatch(expected: Bitmap, capture: () -> Bitmap): Float {
        var best = 0f
        repeat(60) {
            best = max(best, similarity(expected, capture()))
            if (best >= 0.95f) return best
            Thread.sleep(50)
        }
        return best
    }

    private fun centerCrop(source: Bitmap, width: Int, height: Int): Bitmap {
        val scale = max(width / source.width.toFloat(), height / source.height.toFloat())
        val scaled = Bitmap.createScaledBitmap(
            source,
            (source.width * scale).roundToInt().coerceAtLeast(1),
            (source.height * scale).roundToInt().coerceAtLeast(1),
            false,
        )
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).apply {
                drawColor(LifeGamePicture.toArgb())
                drawBitmap(
                    scaled,
                    (width - scaled.width) / 2f,
                    (height - scaled.height) / 2f,
                    Paint().apply { isFilterBitmap = false; isDither = false },
                )
            }
        }
    }

    private fun similarity(expected: Bitmap, actual: Bitmap): Float {
        if (expected.width != actual.width || expected.height != actual.height) return 0f
        var same = 0
        var total = 0
        for (gy in 1 until 48) for (gx in 1 until 24) {
            val x = expected.width * gx / 24
            val y = expected.height * gy / 48
            val a = expected.getPixel(x, y)
            val b = actual.getPixel(x, y)
            total++
            if (
                abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 12 &&
                abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 12 &&
                abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 12
            ) same++
        }
        return same / total.toFloat()
    }
}
