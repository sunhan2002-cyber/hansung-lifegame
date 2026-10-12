package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.MainActivity
import com.example.lifegame.ui.preview.Week7ImageSamples
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Week7ImageSamplePreviewTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun allEightOriginalsLoadAndCanBeBrowsedWithoutStartingGame() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // 실물 후보가 debug APK에 원본 크기로 포함되었는지 확인한다.
        Week7ImageSamples.forEach { sample ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open("images/${sample.imageFile}").use { BitmapFactory.decodeStream(it, null, options) }
            assertEquals(1448, options.outWidth)
            assertEquals(1086, options.outHeight)
        }
        composeRule.onNodeWithText("디자인 미리보기").performClick()
        val directory = requireNotNull(context.getExternalFilesDir("week7-uiux"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        Week7ImageSamples.forEachIndexed { index, sample ->
            composeRule.onNodeWithTag("image_sample_title").assertTextEquals(sample.title)
            composeRule.onNodeWithTag("image_sample_film").performScrollTo().assertIsDisplayed()
            composeRule.waitUntil(10_000) {
                composeRule.onAllNodesWithText("이미지를 준비 중입니다").fetchSemanticsNodes().isEmpty()
            }
            val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
            File(directory, "image-sample-${index + 1}.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            composeRule.onNodeWithTag("image_sample_next").performScrollTo().performClick()
        }
        composeRule.onNodeWithTag("image_sample_title").assertTextEquals(Week7ImageSamples.first().title)
        composeRule.onNodeWithTag("image_sample_previous").performScrollTo().performClick()
        composeRule.onNodeWithTag("image_sample_title").assertTextEquals(Week7ImageSamples.last().title)
        composeRule.onNodeWithTag("image_sample_menu").performScrollTo().performClick()
        composeRule.onNodeWithTag("image_sample_select_1").performScrollTo().performClick()
        composeRule.onNodeWithTag("image_sample_title").assertTextEquals(Week7ImageSamples[1].title)
        composeRule.onNodeWithTag("preview_reset").performClick()
        composeRule.onNodeWithTag("image_sample_title").assertTextEquals(Week7ImageSamples.first().title)
        composeRule.onNodeWithTag("preview_exit").performClick()
        composeRule.onNodeWithText("새 게임").assertIsDisplayed()
        composeRule.onNodeWithText("저장된 게임이 없습니다.").assertIsDisplayed()
    }
}
