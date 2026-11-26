package com.example.lifegame.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.HighlightStyle
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.PressStyle
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.screens.LobbyIntroMemory
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.screens.STAMPING_TAG
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LobbyMotionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before @After fun clearStoredStyle() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
    }

    private fun screen(motion: Boolean, content: @Composable () -> Unit) {
        compose.setContent {
            LifeGameTheme {
                // Pin the system animator scale too: on a device with animations off, "motion on" would
                // otherwise silently run the reduced-motion path.
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides if (motion) 1f else 0f,
                ) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) { content() }
                }
            }
        }
    }

    @Test fun comparisonScreenAppliesAndResetsTheStyleForTheWholeApp() {
        val default = MotionStyle.Default
        // Pick variants that differ from the shipped default so every apply really changes something.
        val press = PressStyle.entries.first { it != default.press }
        val highlight = HighlightStyle.entries.first { it != default.highlight }
        screen(motion = false) { ProvideMotionStyle(allowOverride = true) { MotionCompareScreen(onBack = {}) } }
        compose.onNodeWithTag("motion_applied").assertTextContains("누름 ${default.press.label}", substring = true)
        compose.onNodeWithTag("press_${press.name}_apply").performScrollTo().performClick()
        compose.onNodeWithTag("highlight_${highlight.name}_apply").performScrollTo().performClick()
        compose.onNodeWithTag("motion_applied").performScrollTo().assertTextContains("누름 ${press.label}", substring = true)
        compose.onNodeWithTag("motion_applied").assertTextContains("강조 ${highlight.label}", substring = true)
        assertEquals(default.copy(press = press, highlight = highlight), MotionStyleStore(context).read())
        compose.onNodeWithTag("motion_reset").performScrollTo().performClick()
        compose.onNodeWithTag("motion_applied").performScrollTo().assertTextContains("누름 ${default.press.label}", substring = true)
        compose.onNodeWithTag("motion_applied").assertTextContains("강조 ${default.highlight.label}", substring = true)
        assertEquals(default, MotionStyleStore(context).read())
    }

    @Test fun everyPressAndHighlightVariantClicksExactlyOnceWithMotionOn() {
        var clicks = 0
        screen(motion = true) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PressStyle.entries.forEach { press ->
                    HighlightStyle.entries.forEach { highlight ->
                        CompositionLocalProvider(LocalMotionStyle provides MotionStyle(press = press, highlight = highlight)) {
                            InkButton("${press.name}-${highlight.name}", { clicks++ }, Modifier.testTag("ink_${press.name}_${highlight.name}"),
                                compact = true, highlight = true)
                        }
                    }
                    CompositionLocalProvider(LocalMotionStyle provides MotionStyle(press = press)) {
                        LineButton(press.name, { clicks++ }, Modifier.testTag("line_${press.name}"))
                    }
                }
            }
        }
        PressStyle.entries.forEach { press ->
            HighlightStyle.entries.forEach { highlight -> compose.onNodeWithTag("ink_${press.name}_${highlight.name}").performScrollTo().performClick() }
            compose.onNodeWithTag("line_${press.name}").performScrollTo().performClick()
        }
        compose.runOnIdle { assertEquals(PressStyle.entries.size * (HighlightStyle.entries.size + 1), clicks) }
    }

    @Test fun fullIntroStampsTheTitleKeepsOneHeadingAndSettles() {
        LobbyIntroMemory.playedThisProcess = false
        var started = 0
        compose.mainClock.autoAdvance = false
        screen(motion = true) { HomeScreen(null, emptyList(), {}, { started++ }) }
        // Mid-stamp: the glyph overlay is up, yet tests and TalkBack still see exactly one title node.
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag(STAMPING_TAG).assertExists()
        compose.onAllNodesWithText("인생게임").assertCountEquals(1)
        // Settled: the overlay is gone and the real title remains.
        compose.mainClock.advanceTimeBy(2_500)
        compose.onNodeWithTag(STAMPING_TAG).assertDoesNotExist()
        compose.onAllNodesWithText("인생게임").assertCountEquals(1)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("home_new_life").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, started) }
    }

    @Test fun quickIntroOnRevisitDoesNotStampTheTitle() {
        LobbyIntroMemory.playedThisProcess = true
        compose.mainClock.autoAdvance = false
        screen(motion = true) { HomeScreen(null, emptyList(), {}, {}) }
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithTag(STAMPING_TAG).assertDoesNotExist()
        compose.onAllNodesWithText("인생게임").assertCountEquals(1)
    }

    @Test fun reducedMotionNeverStampsAndRestsImmediately() {
        LobbyIntroMemory.playedThisProcess = false
        compose.mainClock.autoAdvance = false
        screen(motion = false) { HomeScreen(null, emptyList(), {}, {}) }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag(STAMPING_TAG).assertDoesNotExist()
        compose.onAllNodesWithText("인생게임").assertCountEquals(1)
    }
}
