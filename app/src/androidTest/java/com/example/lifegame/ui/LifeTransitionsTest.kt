package com.example.lifegame.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.ui.components.AgeStampStyle
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LifeMark
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalLifeClock
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.LocalTouchExploration
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.PeriodTrack
import com.example.lifegame.ui.components.PeriodTransitionStyle
import com.example.lifegame.ui.components.PeriodTurnOverlay
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.components.rememberLifeClockFor
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameSpecial
import com.example.lifegame.ui.theme.LifeGameTheme
import kotlin.math.abs
import kotlin.math.roundToInt
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 해 도장 · 시기 전환(2단계 time). PlayHost와 같은 감지(rememberLifeClockFor)를 가짜 표시로 돌리고, 진짜 PeriodTrack과
 * PeriodTurnOverlay를 수동 시계로 본다. 연출은 그리기 전용이다: 의미 트리 · 배치 · 터치는 그대로이고, 끝나면 지금과 같은
 * 화면으로 돌아온다. 시스템 애니메이션이 꺼진 기기에서도 움직이는 쪽을 보도록 배율을 1로 고정한다.
 */
@RunWith(AndroidJUnit4::class)
class LifeTransitionsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private var mark by mutableStateOf(markAt(1, 2))
    private var round by mutableIntStateOf(0)
    private var motion by mutableStateOf(true)
    private var style by mutableStateOf(MotionStyle.Default)
    private var showTrack by mutableStateOf(true)
    private var stampYield by mutableStateOf(false)
    private var clicks = 0

    @Before @After fun clearStoredStyle() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
    }

    private fun markAt(period: Int, year: Int, phase: PlayPhase = PlayPhase.AUTO, run: String = "life-transitions") =
        LifeMark(run, period, year, Periods[period], "제${period + 1}장", phase)

    /** The host: a dark screen with a tappable probe, the real PeriodTrack (not on a chapter cover) and the real overlay. */
    @Composable
    private fun TimeHost() {
        val current = mark
        val clock = rememberLifeClockFor(current)
        CompositionLocalProvider(LocalLifeClock provides clock) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().background(Screen)) {
                    Box(
                        Modifier.weight(1f).fillMaxWidth().testTag("probe")
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { clicks++ },
                    )
                    if (showTrack && current.phase != PlayPhase.CHAPTER) {
                        PeriodTrack(
                            marks = Periods.mapIndexed { index, name -> PeriodMarkUi(name.removeSuffix("기"), 0f, index == current.periodIndex) },
                            yearLabel = "${current.year}번째 해",
                            stampYield = stampYield,
                        )
                    }
                }
                PeriodTurnOverlay(clock, Modifier.fillMaxSize())
            }
        }
    }

    private fun host() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 1f,
                    LocalTouchExploration provides false,
                    LocalMotionStyle provides style,
                ) {
                    Box(Modifier.requiredSize(360.dp, 740.dp).clipToBounds()) { key(round) { TimeHost() } }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
    }

    /** A fresh clock (new memory) whose first mark is [first]: a silent baseline, as for a new PlayHost. */
    private fun restart(first: LifeMark, withStyle: MotionStyle = style, withMotion: Boolean = motion) {
        compose.runOnIdle {
            style = withStyle
            motion = withMotion
            stampYield = false
            showTrack = true
            mark = first
            round++
        }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun moveTo(next: LifeMark) {
        compose.runOnIdle { mark = next }
        compose.mainClock.advanceTimeByFrame()
    }

    private fun Dp.px(): Float = with(compose.density) { toPx() }

    private fun root(): Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()

    private fun track(): Bitmap = compose.onNodeWithTag("period_track").captureToImage().asAndroidBitmap()

    /** A point inside the probe (above the track), as a fraction of its size, in root pixels. */
    private fun probePixel(image: Bitmap, fx: Float, fy: Float): Int {
        val bounds = compose.onNodeWithTag("probe").fetchSemanticsNode().boundsInRoot
        return image.getPixel(
            (bounds.left + bounds.width * fx).roundToInt().coerceIn(0, image.width - 1),
            (bounds.top + bounds.height * fy).roundToInt().coerceIn(0, image.height - 1),
        )
    }

    /** The centre pixel of lifeline dot [index] in a capture of the track (14dp side and 8dp top padding, 24dp canvas). */
    private fun dot(image: Bitmap, index: Int): Int {
        val side = 14.dp.px()
        val cell = (image.width - 2f * side) / Periods.size
        return image.getPixel((side + cell * (index + 0.5f)).roundToInt(), (8.dp.px() + 12.dp.px()).roundToInt())
    }

    private fun Int.isNear(color: Color, tolerance: Int = 40): Boolean {
        val c = color.toArgb()
        return abs(android.graphics.Color.red(this) - android.graphics.Color.red(c)) <= tolerance &&
            abs(android.graphics.Color.green(this) - android.graphics.Color.green(c)) <= tolerance &&
            abs(android.graphics.Color.blue(this) - android.graphics.Color.blue(c)) <= tolerance
    }

    private fun stamp(stamp: AgeStampStyle) = MotionStyle.Default.copy(ageStamp = stamp, periodTransition = PeriodTransitionStyle.NONE)

    private fun turn(transition: PeriodTransitionStyle) = MotionStyle.Default.copy(ageStamp = AgeStampStyle.NONE, periodTransition = transition)

    // ---- 해 도장 ----

    @Test fun everyYearStampIsDrawnAboveTheTrackAndSettlesBackToTheExactRestingLifeline() {
        host()
        val resting = mutableMapOf<AgeStampStyle, Bitmap>()
        for (variant in AgeStampStyle.entries) {
            restart(markAt(1, 2), stamp(variant))
            moveTo(markAt(1, 3))
            compose.mainClock.advanceTimeBy(450)
            val mid = root()
            compose.mainClock.advanceTimeBy(2_500)
            val settledRoot = root()
            resting[variant] = track()
            if (variant == AgeStampStyle.NONE) {
                assertTrue("NONE draws nothing extra", mid.sameAs(settledRoot))
            } else {
                assertFalse("$variant shows a stamp mid-way", mid.sameAs(settledRoot))
            }
        }
        val none = requireNotNull(resting[AgeStampStyle.NONE])
        resting.forEach { (variant, image) -> assertTrue("$variant ends on today's lifeline", image.sameAs(none)) }
    }

    @Test fun theStampIsDrawOnlyItAddsNoNodeAndTouchesBelowItStillLand() {
        host()
        for (variant in listOf(AgeStampStyle.SEAL, AgeStampStyle.BIG_STAMP, AgeStampStyle.ROLL)) {
            restart(markAt(2, 1), stamp(variant))
            moveTo(markAt(2, 2))
            compose.mainClock.advanceTimeBy(400)
            val midTree = compose.onRoot(useUnmergedTree = true).printToString()
            clicks = 0
            // Right under the stamp: the bottom of the probe, just above the track.
            compose.onNodeWithTag("probe").performTouchInput { click(Offset(centerX, bottom - 40.dp.toPx())) }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertEquals("$variant: one tap, one click", 1, clicks) }
            compose.mainClock.advanceTimeBy(2_500)
            assertEquals("$variant: the semantics tree is the same during and after the stamp", midTree,
                compose.onRoot(useUnmergedTree = true).printToString())
        }
    }

    @Test fun reducedMotionShowsTheNewYearAtOnce() {
        host()
        restart(markAt(1, 2), stamp(AgeStampStyle.BIG_STAMP), withMotion = false)
        moveTo(markAt(1, 3))
        val first = root()
        compose.mainClock.advanceTimeBy(2_500)
        assertTrue("the first frame is already the final one", first.sameAs(root()))
        // The marker does not travel either: a new period is drawn in place at once.
        moveTo(markAt(2, 1))
        val image = track()
        assertTrue(dot(image, 2).isNear(LifeGameSpecial))
        assertTrue(dot(image, 1).isNear(LifeGameInk))
    }

    @Test fun aTrackThatLeavesBeforeTheStampLandsReplaysTheSameChange() {
        host()
        restart(markAt(1, 5), stamp(AgeStampStyle.SEAL))
        moveTo(markAt(2, 1))
        // The first frame still shows the old period: the marker on 유년, 청소년 not yet reached.
        var image = track()
        assertTrue(dot(image, 1).isNear(LifeGameSpecial))
        assertTrue(dot(image, 2).isNear(LifeGamePaper))
        compose.mainClock.advanceTimeBy(100)
        compose.runOnIdle { showTrack = false } // 장면 보기 / the image preview hides the play chrome mid-travel
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { showTrack = true }
        compose.mainClock.advanceTimeByFrame()
        image = track()
        assertTrue("the new track starts again from the same place", dot(image, 1).isNear(LifeGameSpecial))
        assertTrue(dot(image, 2).isNear(LifeGamePaper))
        compose.mainClock.advanceTimeBy(3_000)
        image = track()
        assertTrue("…and lands on the new period", dot(image, 2).isNear(LifeGameSpecial))
        assertTrue(dot(image, 1).isNear(LifeGameInk))
        // Landed once: hiding and showing again replays nothing.
        val landed = root()
        compose.runOnIdle { showTrack = false }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { showTrack = true }
        compose.mainClock.advanceTimeByFrame()
        assertTrue(landed.sameAs(root()))
    }

    @Test fun aStampYieldsToTheChoicesWithinTheYieldTimeAndNoneStartsOverThem() {
        host()
        restart(markAt(3, 1), stamp(AgeStampStyle.BIG_STAMP))
        moveTo(markAt(3, 2, PlayPhase.DECISION))
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle { stampYield = true } // the choices appeared
        compose.mainClock.advanceTimeBy(MotionTimeline.STAMP_YIELD_MS + 64L)
        val yielded = root()
        compose.mainClock.advanceTimeBy(2_500)
        assertTrue("gone within STAMP_YIELD_MS", yielded.sameAs(root()))

        // Choices already on screen when the year changes: no stamp at all.
        compose.runOnIdle { stampYield = true }
        compose.mainClock.advanceTimeByFrame()
        moveTo(markAt(3, 3, PlayPhase.DECISION))
        compose.mainClock.advanceTimeBy(300)
        val during = root()
        compose.mainClock.advanceTimeBy(2_500)
        assertTrue(during.sameAs(root()))
    }

    @Test fun theFirstMarkIsASilentBaseline() {
        host()
        restart(markAt(2, 3), stamp(AgeStampStyle.BIG_STAMP))
        val first = root()
        compose.mainClock.advanceTimeBy(2_500)
        assertTrue(first.sameAs(root()))
    }

    // ---- 시기 전환 ----

    @Test fun aForwardPeriodChangeIsCoveredFromItsFirstFrameAndTouchesPassThrough() {
        host()
        restart(markAt(1, 5), turn(PeriodTransitionStyle.NONE))
        moveTo(markAt(2, 1, PlayPhase.CHAPTER))
        assertTrue("NONE: the cover appears at once", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        compose.mainClock.advanceTimeBy(1_000)
        val plain = root()
        for (variant in listOf(PeriodTransitionStyle.FILM_ADVANCE, PeriodTransitionStyle.PAGE_TURN)) {
            restart(markAt(1, 5), turn(variant))
            moveTo(markAt(2, 1, PlayPhase.CHAPTER))
            assertTrue("$variant covers the new screen in its first frame", probePixel(root(), 0.5f, 0.75f).isNear(LifeGamePaper, 12))
            compose.mainClock.advanceTimeBy(350)
            clicks = 0
            compose.onNodeWithTag("probe").performTouchInput { click(center) }
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { assertEquals("$variant: a tap during the turn reaches the new screen once", 1, clicks) }
            compose.mainClock.advanceTimeBy(650)
            assertTrue("$variant has left no trace after 1 s", plain.sameAs(root()))
        }
    }

    @Test fun noTurnForTheFirstMarkAnotherRunADecisionTheEndingOrReducedMotion() {
        host()
        val page = turn(PeriodTransitionStyle.PAGE_TURN)
        restart(markAt(2, 1, PlayPhase.CHAPTER), page)
        assertTrue("first mark", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        restart(markAt(1, 5, run = "a"), page)
        moveTo(markAt(2, 1, PlayPhase.CHAPTER, run = "b"))
        assertTrue("another run", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        restart(markAt(4, 5), page)
        moveTo(markAt(5, 2, PlayPhase.DECISION))
        assertTrue("a decision", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        restart(markAt(4, 5), page)
        moveTo(markAt(5, 1, PlayPhase.ENDED))
        assertTrue("the ending", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        restart(markAt(1, 5), page, withMotion = false)
        moveTo(markAt(2, 1, PlayPhase.CHAPTER))
        assertTrue("reduced motion", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
        // Same period, new screen: no turn either.
        restart(markAt(2, 1, PlayPhase.CHAPTER), page, withMotion = true)
        moveTo(markAt(2, 1))
        assertTrue("same period", probePixel(root(), 0.5f, 0.75f).isNear(Screen, 12))
    }

    @Test fun intoOldAgeTheLifelineWaitsForTheTurnThenMovesToTheNewPeriod() {
        host()
        // Without a turn the marker has long arrived at 790 ms …
        restart(markAt(4, 5), MotionStyle.Default.copy(ageStamp = AgeStampStyle.BIG_STAMP, periodTransition = PeriodTransitionStyle.NONE))
        moveTo(markAt(5, 1))
        compose.mainClock.advanceTimeBy(790)
        assertTrue(dot(track(), 4).isNear(LifeGameInk))
        compose.mainClock.advanceTimeBy(3_000)
        val settled = root()
        // … with the page turn (760 ms) it still waits on 중년, then travels and stamps.
        restart(markAt(4, 5), MotionStyle.Default.copy(ageStamp = AgeStampStyle.BIG_STAMP, periodTransition = PeriodTransitionStyle.PAGE_TURN))
        moveTo(markAt(5, 1))
        compose.mainClock.advanceTimeBy(790)
        assertTrue("the marker waited for the page", dot(track(), 4).isNear(LifeGameSpecial))
        compose.mainClock.advanceTimeBy(3_000)
        assertTrue(settled.sameAs(root()))
    }

    // ---- 연출 비교 ----

    @Test fun comparisonGroupPreviewsTheRealTrackAndAppliesBothTimeEffects() {
        compose.setContent {
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = false, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides 0f,
                ) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) {
                        ProvideMotionStyle(allowOverride = true) { MotionCompareScreen(onBack = {}) }
                    }
                }
            }
        }
        compose.onNodeWithTag("motion_group_time").performScrollTo().performClick()
        compose.onNode(hasTestTag("period_track") and hasAnyAncestor(hasTestTag("age_stamp_preview"))).assertExists()
        compose.onNodeWithTag("age_stamp_SEAL").performScrollTo().performClick()
        compose.onNodeWithTag("age_stamp_year").performScrollTo().performClick()
        compose.onNodeWithTag("age_stamp_apply").performScrollTo().performClick()
        compose.onNodeWithTag("period_transition_FILM_ADVANCE").performScrollTo().performClick()
        compose.onNodeWithTag("period_transition_next").performScrollTo().performClick()
        compose.onNodeWithTag("period_transition_card_0").performScrollTo().performClick()
        compose.onNodeWithTag("period_transition_apply").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(
                MotionStyle.Default.copy(ageStamp = AgeStampStyle.SEAL, periodTransition = PeriodTransitionStyle.FILM_ADVANCE),
                MotionStyleStore(context).read(),
            )
        }
        compose.onNodeWithTag("motion_applied_time").assertExists()
    }

    private companion object {
        val Periods = listOf("영아기", "유년기", "청소년기", "청년기", "중년기", "노년기")
        val Screen = Color(0xFF203040)
    }
}
