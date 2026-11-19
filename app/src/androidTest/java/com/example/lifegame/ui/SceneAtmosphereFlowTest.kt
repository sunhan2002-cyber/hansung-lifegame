package com.example.lifegame.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ImageRoutingRepository
import com.example.lifegame.domain.model.EventImageExposureStatus
import com.example.lifegame.domain.model.PresentationState
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.SceneAtmosphereKindKey
import com.example.lifegame.ui.images.PixelAssetLoader
import com.example.lifegame.ui.images.coverPlacement
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.theme.LifeGameTheme
import java.io.File
import kotlin.math.abs
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SceneAtmosphereFlowTest {
    @get:Rule val compose = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val repository = ContentRepository(context)
    private val events = repository.loadDecisionEvents()
    private val content = PlayContent(
        periods = repository.loadPeriodDefinitions(), focuses = emptyList(),
        decisions = events.associateBy { it.eventId }, imageFiles = emptyMap(),
        imageRouting = ImageRoutingRepository(context).load(),
    )

    private fun state(eventId: String): PeriodPlayUiState {
        val event = events.first { it.eventId == eventId }
        val period = event.periods.minOf { it }
        val definition = content.periods.first { it.period == period }
        val decision = restoredDecisionUi(event, content, null, period).copy(
            text = event.text.replace("{nameCall}", "재겸아").replace("{name}", "재겸"),
            choices = event.choices.take(3).mapIndexed { index, option ->
                DecisionChoiceUi(option.choiceId, ('A' + index).toString(), option.label, hint = "")
            },
        )
        return PeriodPlayUiState(
            runKey = "atmosphere-test", playerName = "재겸", phase = PlayPhase.DECISION,
            periodName = definition.displayName, chapterLabel = "제${definition.order}장", periodYears = definition.years, periodIntro = "",
            yearInPeriod = 1, periodMarks = content.periods.sortedBy { it.order }.map { PeriodMarkUi(it.shortName(), 0f, it.period == period) }, previousPeriodName = null, stats = Stats(),
            focus = null, focusOptions = emptyList(), sceneImageId = "", sceneFile = null,
            log = emptyList(), yearKey = eventId, decision = decision, outcome = null,
            isDead = false, autoPlaying = false, speed = 1,
            presentation = PresentationState(exposedEventId = eventId, eventImageExposure = EventImageExposureStatus.SHOWN),
        )
    }

    @Test fun threeRealDecisionScenesAnimateAndMotionOffAndSceneInspectionStayStatic() {
        val examples = listOf("teen_024" to "CLASSROOM_DAY", "adult_072" to "DESK_DAY", "adult_067" to "DESK_NIGHT")
        var current by mutableStateOf(state(examples.first().first))
        var motionEnabled by mutableStateOf(true)
        var chooseCalls = 0
        var continueCalls = 0
        // ComposeTestRule cancels infinite animations while autoAdvance is true.
        // Control the clock before composition so the real ambient transition remains active.
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motionEnabled),
                    LocalSystemAnimatorScale provides 1f,
                ) { DecisionScreen(current, onChoose = { chooseCalls++ }, onContinue = { continueCalls++ }) }
            }
        }
        for ((eventId, kind) in examples) {
            compose.runOnIdle { current = state(eventId); motionEnabled = true }
            compose.waitUntil(5_000) {
                // Artwork decoding finishes on IO; commit its source/status callback while
                // retaining manual control of the infinite animation's frame time.
                compose.mainClock.advanceTimeByFrame()
                compose.onAllNodesWithTag("scene_atmosphere").fetchSemanticsNodes().singleOrNull()
                    ?.config?.get(SceneAtmosphereKindKey) == kind
            }
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithTag("scene_depth").assertDoesNotExist()
            val before = capture("$eventId-motion-a.png")
            compose.mainClock.advanceTimeBy(3_000)
            val after = capture("$eventId-motion-b.png")
            try { assertFalse("$eventId must visibly animate", before.sameAs(after)) }
            finally { before.recycle(); after.recycle() }

            compose.runOnIdle { motionEnabled = false }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("scene_atmosphere").assertDoesNotExist()
            val reduced = capture("$eventId-motion-off.png")
            compose.mainClock.advanceTimeBy(3_000)
            val reducedLater = capture()
            try { assertTrue("$eventId motion-off must stay pixel-stable", reduced.sameAs(reducedLater)) }
            finally { reduced.recycle(); reducedLater.recycle() }

            compose.runOnIdle { motionEnabled = true }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("scene_view_button", useUnmergedTree = true).performClick()
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithTag("scene_only").assertExists()
            compose.onNodeWithTag("scene_atmosphere").assertDoesNotExist()
            awaitUnblurredArtwork(current.decision!!.images)
            val inspection = capture("$eventId-original-inspection.png")
            compose.mainClock.advanceTimeBy(3_000)
            val inspectionLater = capture()
            try { assertTrue("$eventId scene inspection retains the static artwork", inspection.sameAs(inspectionLater)) }
            finally { inspection.recycle(); inspectionLater.recycle() }
        }
        compose.runOnIdle { assertEquals(0, chooseCalls); assertEquals(0, continueCalls) }
    }

    @Test fun depthPlaysOnlyDuringClearIntroductionAndStopsBeforeReading() {
        val examples = listOf("teen_024", "adult_072", "adult_067")
        var current by mutableStateOf(state(examples.first()).copy(presentation = PresentationState()))
        val shown = mutableListOf<String>()
        var chooseCalls = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = true),
                    LocalSystemAnimatorScale provides 1f,
                ) {
                    DecisionScreen(current, onChoose = { chooseCalls++ }, onContinue = {}, onEventImageShown = { shown += it })
                }
            }
        }
        for (eventId in examples) {
            compose.runOnIdle { current = state(eventId).copy(presentation = PresentationState()) }
            if (current.decision!!.isIncident) {
                // These real events enter through the incident popup before image preview.
                compose.mainClock.advanceTimeBy(1_500)
                compose.onNodeWithText("사건 보기").performClick()
            }
            try {
                compose.waitUntil(15_000) {
                    compose.mainClock.advanceTimeBy(80)
                    compose.onAllNodesWithTag("scene_depth").fetchSemanticsNodes().isNotEmpty()
                }
            } catch (error: Throwable) {
                capture("$eventId-depth-missing.png").recycle()
                throw AssertionError("$eventId depth never appeared: ${compose.onRoot(useUnmergedTree = true).printToString()}", error)
            }
            val before = capture("$eventId-depth-intro-a.png")
            compose.mainClock.advanceTimeBy(800)
            compose.onNodeWithTag("scene_depth").assertExists()
            val after = capture("$eventId-depth-intro-b.png")
            try { assertFalse("$eventId clear introduction visibly moves", before.sameAs(after)) }
            finally { before.recycle(); after.recycle() }
            compose.mainClock.advanceTimeBy(5_000)
            compose.onNodeWithTag("scene_depth").assertDoesNotExist()
            compose.onNodeWithTag("scene_blurred").assertExists()
            compose.runOnIdle { assertEquals(1, shown.count { it == eventId }); assertEquals(0, chooseCalls) }
        }
    }

    /** A scene-only tag can precede animateDpAsState's zero-duration blur reset by a frame.
     * Wait for the actual selected PNG, not just the overlay semantics, before measuring
     * stability. This also fails if the renderer keeps showing a placeholder or wrong art.
     */
    private fun awaitUnblurredArtwork(candidates: List<com.example.lifegame.ui.images.ImageCandidate>) {
        val scene = compose.onNodeWithTag("scene_clear")
        val size = scene.fetchSemanticsNode().size
        val loaded = requireNotNull(PixelAssetLoader.load(context, candidates, size.width, size.height))
        val placement = coverPlacement(loaded.bitmap.width, loaded.bitmap.height, size.width, size.height, loaded.focal)
        val expected = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
        Canvas(expected).drawBitmap(loaded.bitmap, null,
            RectF(placement.left, placement.top, placement.left + placement.width, placement.top + placement.height),
            Paint().apply { isFilterBitmap = false; isDither = false })
        try {
            compose.waitUntil(5_000) {
                compose.mainClock.advanceTimeByFrame()
                val actual = scene.captureToImage().asAndroidBitmap()
                try {
                    var matching = 0
                    var total = 0
                    for (gy in 1 until 48) for (gx in 1 until 24) {
                        val x = size.width * gx / 24
                        val y = size.height * gy / 48
                        val a = expected.getPixel(x, y)
                        val b = actual.getPixel(x, y)
                        total++
                        if (abs(AndroidColor.red(a) - AndroidColor.red(b)) <= 12 &&
                            abs(AndroidColor.green(a) - AndroidColor.green(b)) <= 12 &&
                            abs(AndroidColor.blue(a) - AndroidColor.blue(b)) <= 12) matching++
                    }
                    matching / total.toFloat() >= 0.99f
                } finally { actual.recycle() }
            }
        } finally { expected.recycle() }
    }

    private fun capture(name: String? = null): Bitmap {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        if (name != null) {
            val output = File(context.getExternalFilesDir(null), "scene-atmosphere").apply { mkdirs() }
            File(output, name).outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        }
        return bitmap
    }
}
