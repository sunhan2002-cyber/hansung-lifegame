package com.example.lifegame.ui

import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.dice.Dice3DStage
import com.example.lifegame.ui.components.dice.DiceRenderedResult
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.Rule
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class Dice3DTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun realGlSurfaceProducesAFrameAndKeepsSuppliedResults() {
        var first by mutableIntStateOf(1)
        var second by mutableIntStateOf(3)
        var nonce by mutableIntStateOf(1)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalSystemAnimatorScale provides 1f) {
                    Dice3DStage(first, second, nonce)
                }
            }
        }
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 1, 주사위 3, 합계 4")
        compose.waitUntil(5_000) { compose.onAllNodesWithTag("dice_static_fallback").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithTag("dice_gl_surface").assertExists()
        compose.runOnIdle { first = 6; second = 2; nonce++ }
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 6, 주사위 2, 합계 8")
        waitForResult("6,2:settled")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.onNodeWithTag("dice_gl_surface").assertExists()
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 6, 주사위 2, 합계 8")
    }

    @Test fun allSixFacesAndReducedMotionHaveNativeScreenshotEvidence() {
        var first by mutableIntStateOf(1)
        var second by mutableIntStateOf(6)
        var nonce by mutableIntStateOf(0)
        var reduced by mutableStateOf(false)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = !reduced),
                    LocalSystemAnimatorScale provides 1f,
                ) { Dice3DStage(first, second, rollNonce = nonce) }
            }
        }
        for (value in 1..3) {
            compose.runOnIdle { first = value; second = 7 - value }
            waitForResult("$value,${7 - value}:settled")
            screenshot("settled-$value-${7 - value}.png")
        }
        compose.runOnIdle { reduced = true; first = 1; second = 3; nonce++ }
        waitForResult("1,3:settled")
        screenshot("reduced-1-3.png")
    }

    @Test fun unavailableGlHasVisibleStaticResultsIncludingReducedMotionAndUpdates() {
        var reduced by mutableStateOf(false)
        var first by mutableIntStateOf(5)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = !reduced),
                    LocalSystemAnimatorScale provides 1f,
                ) { Dice3DStage(first, 4, 1, allow3D = false) }
            }
        }
        compose.onNodeWithTag("dice_static_fallback").assertExists()
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 5, 주사위 4, 합계 9")
        compose.runOnIdle { reduced = true; first = 2 }
        compose.onNodeWithTag("dice_static_fallback").assertExists()
        compose.onNodeWithTag("dice_3d_stage").assertContentDescriptionEquals("운동능력: 주사위 2, 주사위 4, 합계 6")
    }

    @Test fun stalledGlRerollFallsBackToTheNewResultWhileActivityStaysResumed() {
        var first by mutableIntStateOf(1)
        var second by mutableIntStateOf(3)
        var nonce by mutableIntStateOf(0)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false),
                    LocalSystemAnimatorScale provides 1f,
                ) { Dice3DStage(first, second, nonce) }
            }
        }
        waitForResult("1,3:settled")
        compose.onNodeWithTag("dice_static_fallback").assertDoesNotExist()
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle {
                assertEquals(Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState)
                // Fault injection: stop only the GL worker, leaving the app lifecycle active.
                checkNotNull(surfaceIn(compose.activity.window.decorView)).onPause()
                first = 6
                second = 2
                nonce++
            }
            compose.mainClock.advanceTimeByFrame()
            compose.mainClock.advanceTimeBy(1_000)
            compose.onNodeWithTag("dice_gl_surface").assertExists()
            compose.onNodeWithTag("dice_static_fallback").assertDoesNotExist()
            assertEquals("1,3:settled", compose.onNodeWithTag("dice_3d_stage").fetchSemanticsNode().config[DiceRenderedResult])

            // The per-result watchdog expires at 2 seconds; allow the following UI frame.
            compose.mainClock.advanceTimeBy(1_100)
            compose.onNodeWithTag("dice_static_fallback").assertIsDisplayed()
            compose.onNodeWithTag("dice_gl_surface").assertDoesNotExist()
            compose.onNodeWithTag("dice_3d_stage")
                .assertContentDescriptionEquals("운동능력: 주사위 6, 주사위 2, 합계 8")
            compose.runOnIdle { assertEquals(Lifecycle.State.RESUMED, compose.activity.lifecycle.currentState) }
            screenshot("stalled-reroll-fallback-6-2.png")
        } finally { compose.mainClock.autoAdvance = true }
    }

    @Test fun scrolledProfileRerollsPresentTheSamePixelsAsFreshSettledDice() {
        // The isolated renderer's callback runs before EGL presentation. Exercise the real
        // scrolling profile and compare its presented SurfaceView pixels with a fresh scene.
        var reference by mutableStateOf<NativeDiceCapture?>(null)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(),
                    LocalSystemAnimatorScale provides 1f,
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        val expected = reference
                        if (expected == null) {
                            CharacterSelectScreen(starters = emptyList(), onStart = {})
                        } else key(expected) {
                            Box(Modifier.width(with(LocalDensity.current) { expected.bitmap.width.toDp() })) {
                                Dice3DStage(expected.first, expected.second, rollNonce = 0)
                            }
                        }
                    }
                }
            }
        }
        val captures = mutableListOf<NativeDiceCapture>()
        var settledStageBounds: List<Int>? = null
        try {
            repeat(StartingDice.REROLLS + 1) { index ->
                val tag = if (index == 0) "dice_roll" else "dice_reroll"
                if (index == 0) {
                    compose.onNodeWithTag(tag).performScrollTo()
                } else {
                    assertProfileStageAndRerollAreFullyVisible()
                    assertEquals("The stage must stay in place before reroll $index", settledStageBounds, nativeStageBounds().toList())
                }
                compose.onNodeWithTag(tag).assertIsDisplayed().assertIsEnabled()
                compose.waitForIdle()
                compose.onNodeWithTag(tag).performTouchInput { click() }
                if (index < StartingDice.REROLLS) {
                    compose.onNodeWithTag("dice_reroll").assertTextEquals("다시 굴리기 (${StartingDice.REROLLS - index}번 남음)")
                } else {
                    compose.onNodeWithTag("dice_no_reroll").assertExists()
                }
                if (index == 0) {
                    // Align only once, after the first roll creates the panel. Empty starters
                    // omit the tendency block, so the whole scene and reroll fit together.
                    compose.onNodeWithTag("dice_reroll").performScrollTo()
                    assertProfileStageAndRerollAreFullyVisible()
                    settledStageBounds = nativeStageBounds().toList()
                }
                // Never scroll after a reroll: a SurfaceView position transaction could
                // refresh a stale buffer and hide the regression this test is checking.
                compose.onNodeWithTag("dice_3d_stage").assertIsDisplayed()
                // 2026-11-20: 능력치 줄은 최종값 · 증감만 읽으므로, 실제로 굴린 두 눈은 3D 주사위의 낭독 문구에서 읽는다.
                val description = compose.onNodeWithTag("dice_3d_stage")
                    .fetchSemanticsNode().config[SemanticsProperties.ContentDescription].single()
                val match = checkNotNull(Regex("주사위 ([1-6]), 주사위 ([1-6])").find(description))
                val first = match.groupValues[1].toInt()
                val second = match.groupValues[2].toInt()
                // The same pair can be rolled twice; do not accept the preceding roll's
                // identical settled marker before this roll has had time to animate.
                android.os.SystemClock.sleep(600)
                waitForResult("$first,$second:settled")
                // Native EGL uses uptime; the profile's input lock uses Compose's test
                // clock. Advance both before trying the next real touch (still < 2s watchdog).
                compose.mainClock.advanceTimeBy(600)
                compose.waitForIdle()
                assertEquals("Reroll $index must not move the stage before capture", settledStageBounds, nativeStageBounds().toList())
                val bitmap = captureNativeStage()
                saveBitmap(bitmap, "profile-roll-$index-$first-$second.png")
                captures += NativeDiceCapture(first, second, bitmap)
            }
            captures.forEachIndexed { index, expected ->
                compose.runOnIdle { reference = expected }
                waitForResult("${expected.first},${expected.second}:settled")
                // A new SurfaceView's first buffer is presented asynchronously. Give it
                // the same real-time settling interval used for the profile capture.
                android.os.SystemClock.sleep(600)
                val actual = captureNativeStage()
                try {
                    saveBitmap(actual, "profile-reference-$index-${expected.first}-${expected.second}.png")
                    assertPresentedDiceMatch(expected.bitmap, actual, "roll $index: ${expected.first},${expected.second}")
                } finally { actual.recycle() }
            }
        } finally { captures.forEach { it.bitmap.recycle() } }
    }

    private data class NativeDiceCapture(val first: Int, val second: Int, val bitmap: Bitmap)

    private fun surfaceIn(view: View): GLSurfaceView? {
        if (view is GLSurfaceView) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            surfaceIn(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun nativeStageBounds(): IntArray = compose.runOnIdle {
        val surface = checkNotNull(surfaceIn(compose.activity.window.decorView))
        val position = IntArray(2).also(surface::getLocationOnScreen)
        intArrayOf(position[0], position[1], surface.width, surface.height)
    }

    private fun assertProfileStageAndRerollAreFullyVisible() {
        val viewport = compose.onNodeWithTag("profile_content").fetchSemanticsNode().boundsInRoot
        val stage = compose.onNodeWithTag("dice_gl_surface").fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithTag("dice_reroll").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val native = nativeStageBounds()
        assertEquals("The GL stage must not be vertically clipped", native[3].toFloat(), stage.height, 1f)
        assertEquals("The GL stage must not be horizontally clipped", native[2].toFloat(), stage.width, 1f)
        assertTrue("The stage and reroll button must fit in the same viewport", stage.top >= viewport.top && stage.bottom <= viewport.bottom && button.top >= viewport.top && button.bottom <= viewport.bottom)
    }

    private fun captureNativeStage(): Bitmap {
        val bounds = nativeStageBounds()
        android.os.SystemClock.sleep(120)
        val full = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try {
            assertTrue("The entire GL stage must be on screen", bounds[0] >= 0 && bounds[1] >= 0 && bounds[0] + bounds[2] <= full.width && bounds[1] + bounds[3] <= full.height)
            return Bitmap.createBitmap(full, bounds[0], bounds[1], bounds[2], bounds[3])
        } finally { full.recycle() }
    }

    private fun assertPresentedDiceMatch(profile: Bitmap, fresh: Bitmap, label: String) {
        assertEquals("$label width", profile.width, fresh.width)
        assertEquals("$label height", profile.height, fresh.height)
        var different = 0
        var sampled = 0
        // Exclude rounded clip edges and backgrounds; retain both cube faces and their pips.
        for (y in (profile.height * .18f).toInt() until (profile.height * .86f).toInt()) {
            for (x in (profile.width * .15f).toInt() until (profile.width * .85f).toInt()) {
                val left = profile.getPixel(x, y)
                val right = fresh.getPixel(x, y)
                if (maxOf(abs(Color.red(left) - Color.red(right)), abs(Color.green(left) - Color.green(right)), abs(Color.blue(left) - Color.blue(right))) > 12) different++
                sampled++
            }
        }
        assertTrue("$label presented stale/different dice pixels: $different of $sampled", different.toDouble() / sampled <= .0001)
    }

    private fun saveBitmap(bitmap: Bitmap, name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "dice-native").apply { mkdirs() }
        File(directory, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }

    private fun waitForResult(expected: String) {
        compose.waitUntil(5_000) {
            compose.onNodeWithTag("dice_3d_stage").fetchSemanticsNode().config[DiceRenderedResult] == expected
        }
    }

    private fun screenshot(name: String) {
        // UiAutomation includes the SurfaceView's EGL layer; Compose captureToImage does not.
        // The renderer callback precedes eglSwapBuffers; allow the surface transaction to present.
        android.os.SystemClock.sleep(80)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "dice-native").apply { mkdirs() }
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally { bitmap.recycle() }
    }
}
