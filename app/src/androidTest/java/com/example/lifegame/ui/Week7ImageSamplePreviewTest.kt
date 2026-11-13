package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
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

    /** Visual evidence for the approved PNG; this does not exercise the pending new-game cutscene. */
    @Test
    fun approvedDoljabiCanBeInspectedFullScreen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val index = Week7ImageSamples.indexOfFirst {
            it.imageFile == "cutscenes/week11/img_doljabi_grasp_001.png"
        }
        assertTrue("Approved doljabi image must be available in the preview", index >= 0)
        val label = InstrumentationRegistry.getArguments().getString("reviewSize") ?: "phone"
        val directory = requireNotNull(context.getExternalFilesDir("doljabi-20261113"))
        assertTrue(directory.isDirectory || directory.mkdirs())

        fun reveal(tag: String) {
            composeRule.onNodeWithTag("image_sample_content").performScrollToNode(hasTestTag(tag))
        }
        composeRule.onNodeWithText("디자인 미리보기").performScrollTo().assertIsDisplayed().performClick()
        reveal("image_sample_menu")
        composeRule.onNodeWithTag("image_sample_menu").performClick()
        composeRule.onNodeWithTag("image_sample_select_$index").performScrollTo().performClick()
        reveal("image_sample_title")
        composeRule.onNodeWithTag("image_sample_title")
            .assertTextEquals("첫 생일의 돌잡이")
        reveal("image_sample_film")
        composeRule.onNodeWithTag("image_sample_film").assertIsDisplayed()
        // Asset decoding runs on IO, outside Compose's idle tracking. Captures are reviewed visually.
        Thread.sleep(750)
        File(directory, "doljabi-$label-preview.png").outputStream().use {
            assertTrue(captureScreen().compress(Bitmap.CompressFormat.PNG, 100, it))
        }

        reveal("image_sample_fullscreen")
        composeRule.onNodeWithTag("image_sample_fullscreen").performClick()
        composeRule.onNodeWithTag("image_sample_fullscreen_image").assertIsDisplayed()
        Thread.sleep(750)
        File(directory, "doljabi-$label-fullscreen.png").outputStream().use {
            assertTrue(captureScreen().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        composeRule.onNodeWithTag("image_sample_fullscreen_image").performClick()
        reveal("image_sample_title")
        composeRule.onNodeWithTag("image_sample_title")
            .assertTextEquals("첫 생일의 돌잡이")
        composeRule.onNodeWithTag("preview_exit").performClick()
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
    }

    @Test
    fun allSamplesLoadAndCanBeBrowsedWithoutStartingGame() {
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
            val image = captureScreen()
            File(directory, "image-sample-${index + 1}.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            composeRule.onNodeWithTag("image_sample_fullscreen").performScrollTo().performClick()
            composeRule.onNodeWithTag("image_sample_fullscreen_image").assertIsDisplayed()
            composeRule.waitForIdle()
            // 몸살·회사 출입문과 추가 사건 네 장의 실제 세로 크롭을 기록한다.
            if (index >= 6) {
                val fullScreen = captureScreen()
                File(directory, "applied-scene-${index + 1}-fullscreen.png").outputStream().use {
                    fullScreen.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            }
            composeRule.onNodeWithTag("image_sample_fullscreen_image").performClick()
            composeRule.onNodeWithTag("image_sample_title").assertTextEquals(sample.title)
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
        composeRule.onNodeWithTag("home_screen").assertIsDisplayed()
        composeRule.onNodeWithText("새 인생 살기").performScrollTo().assertIsDisplayed()
    }

    private fun captureScreen(): Bitmap {
        composeRule.waitForIdle()
        // Compose의 레이아웃/그리기는 같은 UI 스레드에서 실행한다.
        return captureAppWindow()
    }
}
