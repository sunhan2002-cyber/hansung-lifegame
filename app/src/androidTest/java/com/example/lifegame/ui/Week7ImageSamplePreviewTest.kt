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
import org.json.JSONArray
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
        // 실물 후보가 debug APK에 "있어야 할 정확한 크기"로 들어 있는지 확인한다(2026-11-16 기존 실패 수정).
        // main e16faf5가 승인되지 않은 그림을 720×540으로 시연용 재압축했고 083d7ed가 승인 그림만 원본으로 되돌렸다.
        // 그래서 기대 크기는 그림 등록 파일(image_ids.sample.json)의 크기이고, 등록되지 않은 디자인 원본 · 돌잡이 컷신은
        // 원본 1448×1086이다. 크기를 느슨하게 보지 않고 그림마다 정확히 비교한다(승인 SHA는 JVM ApprovedImageIntegrityTest).
        val registry = JSONArray(context.assets.open("image_ids.sample.json").bufferedReader().use { it.readText() }).let { array ->
            List(array.length()) { array.getJSONObject(it) }
                .filter { it.has("width") && it.has("height") }
                .associate { it.getString("fileName") to (it.getInt("width") to it.getInt("height")) }
        }
        val checked = Week7ImageSamples.map { sample ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.assets.open("images/${sample.imageFile}").use { BitmapFactory.decodeStream(it, null, options) }
            val expected = registry[sample.imageFile.substringAfterLast('/')] ?: (1448 to 1086)
            assertEquals("${sample.imageFile} width", expected.first, options.outWidth)
            assertEquals("${sample.imageFile} height", expected.second, options.outHeight)
            expected
        }
        assertEquals(80, checked.size)
        assertEquals(mapOf((1448 to 1086) to 15, (720 to 540) to 65), checked.groupingBy { it }.eachCount())
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
