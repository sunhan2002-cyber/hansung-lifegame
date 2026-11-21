package com.example.lifegame.ui

import androidx.activity.ComponentActivity
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.components.CompletionEffectProgress
import com.example.lifegame.ui.components.CompletionSealHeader
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.TitleCollectionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompletionCelebrationTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun progress() = compose.onNodeWithTag("completion_header").fetchSemanticsNode().config[CompletionEffectProgress]

    /** Capture the real EndingFlowScreen root, including title, badge, body and usable controls. */
    private fun saveCertificateCapture(name: String) {
        val directory = requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext
            .getExternalFilesDir("completion-celebration")).also { it.mkdirs() }
        val shownProgress = progress()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        try {
            File(directory, "$name.png").outputStream().use { output ->
                assertTrue("Certificate PNG must be written", bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            File(directory, "$name.txt").writeText("screen=EndingFlowScreen\nrunId=completion-presentation\nprogress=$shownProgress\nwidth=${bitmap.width}\nheight=${bitmap.height}\n")
        } finally { bitmap.recycle() }
    }
    private fun record() = buildLifeRecordUi(
        CompletedLifeRecord(
            runId = "completion-presentation", playerName = "민서", summary = "지나온 삶",
            endingId = null, endingTitle = "지나온 삶", endingSummary = "하루하루를 쌓아 왔다", choices = emptyList(),
            finalState = PeriodRunState("completion-presentation", 41L, LifePeriod.OLD_AGE, isEnding = true),
            awardedTitle = AwardedTitle("kept_title", "함께 걸어온 사람", "곁에 있던 사람들을 기억했다"),
        ),
        RecordContent(emptyList()),
    )

    @Test fun revealKeepsInputEnabledAndDoesNotRepeatAfterStateRestoration() {
        var consumed = 0
        var clicks = 0
        compose.mainClock.autoAdvance = false
        // The paused clock needs a separate disposal frame; runOnIdle alone can coalesce both states.
        val restoration = ManualRestoreHost(compose)
        restoration.setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(), LocalSystemAnimatorScale provides 1f) {
                    Column {
                        CompletionSealHeader("one-run", true, true, { consumed++ })
                        Button(onClick = { clicks++ }, modifier = Modifier.testTag("completion_input")) { Text("계속") }
                    }
                }
            }
        }
        compose.mainClock.advanceTimeBy(96)
        assertTrue(progress() < 1f)
        compose.onNodeWithTag("completion_input").performClick()
        compose.runOnIdle { assertEquals(1, clicks); assertEquals(1, consumed) }
        restoration.recreate()
        assertEquals(1, restoration.recreations)
        compose.mainClock.advanceTimeBy(96)
        assertEquals(1f, progress(), .001f)
        compose.runOnIdle { assertEquals(1, consumed) }
    }

    @Test fun motionPreferenceAndInactiveLifecycleSettleWithoutReplaying() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED }
        var motion by mutableStateOf(true)
        var consumed = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalLifecycleOwner provides owner,
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion),
                    LocalSystemAnimatorScale provides 1f,
                ) { CompletionSealHeader("lifecycle", true, true, { consumed++ }) }
            }
        }
        compose.mainClock.advanceTimeBy(96)
        assertTrue(progress() < 1f)
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1f, progress(), .001f)
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.RESUMED; motion = false }
        compose.mainClock.advanceTimeBy(96)
        assertEquals(1f, progress(), .001f)
        compose.runOnUiThread { motion = true }
        compose.mainClock.advanceTimeBy(96)
        assertEquals(1f, progress(), .001f)
        compose.runOnIdle { assertEquals(1, consumed) }
        compose.runOnUiThread { owner.registry.currentState = Lifecycle.State.DESTROYED }
    }

    @Test fun freshCertificateUsesRecordedTitleAndReplayDoesNotRepeatCelebration() {
        val saved = record()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(), LocalSystemAnimatorScale provides 1f) {
                    EndingFlowScreen(saved, GenerationUi(saved.runId, GenerationStatus.FALLBACK), {}, {}, {})
                }
            }
        }
        compose.onNodeWithTag("recap_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("future_next").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(96)
        compose.onNodeWithTag("certificate_title_badge").assertIsDisplayed()
        assertTrue(progress() < 1f)
        compose.mainClock.advanceTimeBy(160)
        assertTrue("Capture the actual in-progress reveal", progress() in 0.05f..0.95f)
        saveCertificateCapture("certificate-reveal-mid")
        compose.mainClock.advanceTimeBy(900)
        assertEquals(1f, progress(), .001f)
        saveCertificateCapture("certificate-reveal-settled")
        compose.onNodeWithTag("certificate_replay").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("recap_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("future_next").performScrollTo().performClick()
        compose.mainClock.advanceTimeBy(96)
        assertEquals(1f, progress(), .001f)
        assertEquals("kept_title", saved.title?.titleId)
    }

    @Test fun oldRecordsOpenQuietlyAndOnlyEarnedTitlesShowBadges() {
        var showCollection by mutableStateOf(false)
        val saved = record().copy(title = null)
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(LocalExperiencePreferences provides ExperiencePreferences(), LocalSystemAnimatorScale provides 0f) {
                    if (!showCollection) EndingFlowScreen(saved, null, {}, {}, { showCollection = true }, initialStep = EndingStep.CERTIFICATE)
                    else TitleCollectionScreen(TitleCollectionUi(listOf(
                        TitleEntryUi("earned", "받은 칭호", "실제 기록", true, listOf("민서")),
                        TitleEntryUi("locked", "미획득 칭호", "목표", false, emptyList()),
                    )), {})
                }
            }
        }
        assertEquals(1f, progress(), .001f)
        compose.onNodeWithTag("certificate_title_badge").assertDoesNotExist()
        compose.onNodeWithTag("certificate_done").performClick()
        compose.onNodeWithTag("earned_badge_earned", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("earned_badge_locked", useUnmergedTree = true).assertDoesNotExist()
    }
}
