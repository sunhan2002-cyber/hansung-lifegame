package com.example.lifegame.ui

import android.content.ContextWrapper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.ExperiencePreferencesStore
import com.example.lifegame.ui.components.ExperienceSettingsDialog
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.PlayTopBar
import com.example.lifegame.ui.components.SceneViewButton
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class, androidx.compose.ui.ExperimentalComposeUiApi::class)
class ExperienceEffectsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefix = "experience_test_${System.nanoTime()}_"
    private val context = object : ContextWrapper(base) {
        override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences(prefix + name, mode)
    }

    @After fun cleanTestPreferences() {
        base.deleteSharedPreferences(prefix + "lifegame_ui_prefs")
    }

    @Test fun settingsPersistAndExternalPreferenceChangesReachTheVisibleDialog() {
        val store = ExperiencePreferencesStore(context)
        compose.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                LifeGameTheme { ExperienceSettingsDialog(onDismiss = {}) }
            }
        }
        compose.onNodeWithTag("experience_motion").assertIsOn().performClick().assertIsOff()
        compose.onNodeWithTag("experience_sound").assertIsOff().performClick().assertIsOn()
        compose.runOnIdle {
            assertEquals(ExperiencePreferences(motionEnabled = false, soundEnabled = true), ExperiencePreferencesStore(context).read())
            // Another screen/store can change settings without recreating this composition.
            store.write(ExperiencePreferences(motionEnabled = true, hapticsEnabled = false))
        }
        compose.onNodeWithTag("experience_motion").assertIsOn()
        compose.onNodeWithTag("experience_haptics").assertIsOff()
        compose.onNodeWithTag("experience_sound").assertIsOff()
    }

    @Test fun buttonsExecuteOnceForKeyboardTouchAndAccessibilityAndIgnoreDisabledTouch() {
        val inkFocus = FocusRequester()
        val lineFocus = FocusRequester()
        var inkClicks = 0
        var lineClicks = 0
        var enabled by mutableStateOf(true)
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(hapticsEnabled = false)) {
                    Column {
                        InkButton("주 버튼", { inkClicks++ }, Modifier.focusRequester(inkFocus).testTag("ink"), enabled = enabled)
                        LineButton("보조 버튼", { lineClicks++ }, Modifier.focusRequester(lineFocus).testTag("line"), enabled = enabled)
                    }
                }
            }
        }
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)); inkFocus.requestFocus() }
        compose.onNodeWithTag("ink").assertIsFocused()
        compose.onNodeWithTag("ink").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, inkClicks); lineFocus.requestFocus() }
        compose.onNodeWithTag("line").assertIsFocused()
        compose.onNodeWithTag("line").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("ink").performTouchInput { click() }
        compose.onNodeWithTag("line").performClick()
        compose.runOnIdle {
            assertEquals(2, inkClicks)
            assertEquals(2, lineClicks)
            enabled = false
        }
        compose.onNodeWithTag("ink").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithTag("line").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(2, inkClicks); assertEquals(2, lineClicks) }
    }

    @Test fun systemMotionSettingUpdatesAnExistingCompositionImmediately() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val resolver = base.contentResolver
        val original = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        automation.adoptShellPermissionIdentity(android.Manifest.permission.WRITE_SECURE_SETTINGS)
        try {
            assertTrue(Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
            compose.setContent {
                CompositionLocalProvider(LocalContext provides context) {
                    LifeGameTheme { Text(if (rememberReduceMotion()) "reduced" else "motion", Modifier.testTag("motion_state")) }
                }
            }
            compose.onNodeWithTag("motion_state").assertTextEquals("motion")
            assertTrue(Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f))
            compose.waitUntil { compose.onNodeWithTag("motion_state").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text == "reduced" }
            compose.onNodeWithTag("motion_state").assertTextEquals("reduced")
        } finally {
            Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, original)
            automation.dropShellPermissionIdentity()
        }
    }

    @Test fun customPlayControlsKeepTheirKeyboardAndAccessibilityClickPaths() {
        val sceneFocus = FocusRequester()
        var details = 0
        var sceneViews = 0
        var enabled by mutableStateOf(true)
        lateinit var inputModeManager: InputModeManager
        compose.setContent {
            inputModeManager = LocalInputModeManager.current
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false, hapticsEnabled = false)) {
                    Column {
                        PlayTopBar(50, onOpenDetails = { details++ }, avatar = { Text("캐릭터", it) })
                        SceneViewButton({ sceneViews++ }, Modifier.focusRequester(sceneFocus), enabled)
                    }
                }
            }
        }
        compose.onNodeWithTag("player_avatar_button").performClick()
        compose.onNodeWithTag("player_avatar_button").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(2, details)
            assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard))
            sceneFocus.requestFocus()
        }
        compose.onNodeWithTag("scene_view_button").assertIsFocused()
        compose.onNodeWithTag("scene_view_button").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, sceneViews); enabled = false }
        compose.onNodeWithTag("scene_view_button").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, sceneViews) }
    }

    @Test fun aButtonUsesTheLatestCallbackAfterAnEarlierClick() {
        var firstCalls = 0
        var updatedCalls = 0
        var action by mutableStateOf<() -> Unit>({ firstCalls++ })
        compose.setContent {
            LifeGameTheme { LineButton("적용", action, Modifier.testTag("updated_callback")) }
        }
        compose.onNodeWithTag("updated_callback").performClick()
        compose.runOnIdle {
            assertEquals(1, firstCalls)
            action = { updatedCalls++ }
        }
        compose.onNodeWithTag("updated_callback").performClick()
        compose.runOnIdle { assertEquals(1, firstCalls); assertEquals(1, updatedCalls) }
    }

    @Test fun ambientTransitionDoesNotKeepComposeBusyAndMotionOffRestsAtZero() {
        var motion by mutableStateOf(true)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion),
                    LocalSystemAnimatorScale provides 1f,
                ) {
                    val phase by rememberAmbientPhase()
                    Text(phase.toString(), Modifier.testTag("ambient"))
                }
            }
        }
        // InfiniteTransition is understood by Compose's idling system; this must return.
        compose.waitForIdle()
        compose.runOnIdle { motion = false }
        compose.onNodeWithTag("ambient").assertTextEquals("0.0")
    }

    @Test fun ambientStopsWhenItsScreenLeavesResumedState() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.mainClock.autoAdvance = false
        var observedPhase = 0f
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalLifecycleOwner provides owner, LocalSystemAnimatorScale provides 1f) {
                    val phase by rememberAmbientPhase()
                    observedPhase = phase
                    Text(phase.toString(), Modifier.testTag("ambient_lifecycle"))
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.mainClock.advanceTimeBy(3_000)
        compose.runOnIdle { assertTrue(observedPhase > 0f); owner.registry.currentState = Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("ambient_lifecycle").assertTextEquals("0.0")
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.DESTROYED }
    }
}
