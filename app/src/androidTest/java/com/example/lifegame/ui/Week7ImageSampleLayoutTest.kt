package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.ui.screens.ImageSamplePreviewScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Week7ImageSampleLayoutTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun imageAndControlsRemainAccessibleAt320By400WithLargeText() {
        var index by mutableStateOf(2)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                LifeGameTheme {
                    Box(Modifier.requiredSize(320.dp, 400.dp).clipToBounds()) {
                        ImageSamplePreviewScreen(index, { index = it })
                    }
                }
            }
        }
        composeRule.onNodeWithTag("image_sample_title").performScrollTo().assertTextEquals("청소년기 아르바이트")
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithTag("image_sample_title").performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertTrue(layouts.isNotEmpty())
        assertFalse(layouts.first().hasVisualOverflow)
        composeRule.onNodeWithTag("image_sample_film").performScrollTo().assertIsDisplayed()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("이미지를 준비 중입니다").fetchSemanticsNodes().isEmpty()
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = requireNotNull(context.getExternalFilesDir("week7-uiux"))
        assertTrue(directory.isDirectory || directory.mkdirs())
        val image = composeRule.onRoot().captureToImage().asAndroidBitmap()
        File(directory, "image-sample-small-large-text.png").outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        composeRule.onNodeWithTag("image_sample_next").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(index == 3) }
        composeRule.onNodeWithTag("image_sample_previous").performScrollTo().assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertTrue(index == 2) }
        composeRule.onNodeWithTag("image_sample_menu").performScrollTo().assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("image_sample_select_7").performScrollTo().performClick()
        composeRule.runOnIdle { assertTrue(index == 7) }
        composeRule.onNodeWithText("원본 1448×1086 · 4:3").performScrollTo().assertIsDisplayed()
    }
}
