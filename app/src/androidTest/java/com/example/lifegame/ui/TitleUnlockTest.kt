package com.example.lifegame.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.components.ExperiencePreferences
import com.example.lifegame.ui.components.LocalExperiencePreferences
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.LocalSystemAnimatorScale
import com.example.lifegame.ui.components.LocalTouchExploration
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.TitleSeenStore
import com.example.lifegame.ui.components.TitleUnlockStyle
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.TitleUnlockProgress
import com.example.lifegame.ui.screens.TitleUnlockTimeline
import com.example.lifegame.ui.screens.TitleUnlockUi
import com.example.lifegame.ui.screens.rememberTitleUnlockState
import com.example.lifegame.ui.theme.LifeGameTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 칭호 획득 팝업(lobby2). 시계를 멈추고 실제 EndingFlowScreen을 미래 모습 → 인생기록증으로 넘겨 본다.
 * 팝업은 창이 아니라 기록증 스크롤 영역 위의 덮개라서, 아래 "주마등 다시 보기 · 홈으로"와 기록증 스크롤이 그대로 동작해야 한다.
 */
@RunWith(AndroidJUnit4::class)
class TitleUnlockTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /** One certificate setup. Switching to another (or the next [take]) starts the ending over, like a new life. */
    private data class Scenario(
        val style: TitleUnlockStyle = TitleUnlockStyle.DROP_SPIN,
        val motion: Boolean = true,
        val unlock: TitleUnlockUi? = TitleUnlockUi(1),
        val initialStep: EndingStep = EndingStep.FUTURE,
        val touchExploration: Boolean = false,
        val take: Int = 0,
    )

    private var scenario by mutableStateOf(Scenario())

    @Before @After fun clearStores() {
        context.deleteSharedPreferences(MotionStyleStore.PREFERENCES_NAME)
        context.deleteSharedPreferences(TitleSeenStore.PREFERENCES_NAME)
    }

    private fun record() = buildLifeRecordUi(
        CompletedLifeRecord(
            runId = "title-unlock-run", playerName = "민서", summary = "지나온 삶", endingId = null, endingTitle = "지나온 삶",
            endingSummary = "하루하루를 쌓아 왔다", choices = emptyList(),
            finalState = PeriodRunState("title-unlock-run", 41L, LifePeriod.OLD_AGE, isEnding = true),
            awardedTitle = AwardedTitle("kept_title", "함께 걸어온 사람", "곁에 있던 사람들을 기억했다"),
        ),
        RecordContent(emptyList()),
    )

    private fun progress() = compose.onNodeWithTag("title_unlock").fetchSemanticsNode().config[TitleUnlockProgress]
    private fun popupCount() = compose.onAllNodesWithTag("title_unlock").fetchSemanticsNodes().size

    /** The live ending at 미래 모습: clock paused, motion pinned, touch exploration overridden, the variant provided. */
    private fun ending(first: Scenario = Scenario(), host: ManualRestoreHost? = null, onDone: () -> Unit = {}) {
        scenario = first
        val saved = record()
        compose.mainClock.autoAdvance = false
        val content: @Composable () -> Unit = {
            val current = scenario
            LifeGameTheme {
                CompositionLocalProvider(
                    LocalExperiencePreferences provides ExperiencePreferences(motionEnabled = current.motion, hapticsEnabled = false),
                    LocalSystemAnimatorScale provides if (current.motion) 1f else 0f,
                    LocalTouchExploration provides current.touchExploration,
                    LocalMotionStyle provides MotionStyle.Default.copy(titleUnlock = current.style),
                ) {
                    Box(Modifier.requiredSize(360.dp, 640.dp).clipToBounds()) {
                        key(current) {
                            EndingFlowScreen(saved, GenerationUi(saved.runId, GenerationStatus.FALLBACK), {}, {}, onDone,
                                initialStep = current.initialStep, titleUnlock = current.unlock)
                        }
                    }
                }
            }
        }
        if (host != null) host.setContent(content) else compose.setContent(content)
        compose.mainClock.advanceTimeByFrame()
    }

    private fun restart(next: Scenario) {
        compose.runOnUiThread { scenario = next }
        compose.mainClock.advanceTimeByFrame()
    }

    /**
     * performScrollTo() keeps re-issuing an animated semantics scroll until the node is fully visible. With the main
     * clock paused that animation never runs, so the call never returns: let the clock run just for the scroll.
     */
    private fun SemanticsNodeInteraction.scrollToWithRunningClock(): SemanticsNodeInteraction {
        val paused = !compose.mainClock.autoAdvance
        compose.mainClock.autoAdvance = true
        try {
            performScrollTo()
        } finally {
            if (paused) compose.mainClock.autoAdvance = false
        }
        return this
    }

    private fun openCertificate() {
        compose.onNodeWithTag("future_next").scrollToWithRunningClock().performClick()
        compose.mainClock.advanceTimeByFrame()
    }

    private fun settle(style: TitleUnlockStyle = TitleUnlockStyle.DROP_SPIN) {
        compose.mainClock.advanceTimeBy(MotionTimeline.TITLE_UNLOCK_START_MS + TitleUnlockTimeline.totalMs(style) + 200L)
    }

    @Test fun thePopupWaitsForTheCertificateEntranceThenSettlesForEveryVariant() {
        ending()
        TitleUnlockStyle.entries.filter { it != TitleUnlockStyle.NONE }.forEachIndexed { take, style ->
            restart(Scenario(style = style, take = take + 1))
            openCertificate()
            assertEquals("$style waits out the certificate entrance", 0, popupCount())
            compose.mainClock.advanceTimeBy(MotionTimeline.TITLE_UNLOCK_START_MS + 200L)
            assertEquals("$style is up", 1, popupCount())
            assertTrue("$style is entering", progress() < 1f)
            settle(style)
            assertEquals(1f, progress(), 0.001f)
            compose.onNodeWithTag("title_unlock_keep").assertIsDisplayed()
        }
    }

    @Test fun bottomButtonsStayUsableWhileThePopupShows() {
        var done = 0
        ending(onDone = { done++ })
        openCertificate()
        settle()
        assertEquals(1, popupCount())
        // The popup covers only the scroll area: the bar below gets its own touches, once each.
        compose.onNodeWithTag("certificate_done").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { assertEquals(1, done) }
        compose.onNodeWithTag("certificate_replay").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, compose.onAllNodesWithTag("recap_screen").fetchSemanticsNodes().size)
        assertEquals(0, popupCount())
        compose.runOnIdle { assertEquals(1, done) }
    }

    @Test fun keepFliesTheMedalHomeAndReplayNeverBringsThePopupBack() {
        ending()
        openCertificate()
        settle()
        compose.onNodeWithTag("title_unlock_keep").performClick()
        compose.mainClock.advanceTimeBy(1_200)
        assertEquals(0, popupCount())
        // 주마등 다시 보기 → 미래 모습 → 인생기록증: the celebration was consumed.
        compose.onNodeWithTag("certificate_replay").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("recap_skip").performClick()
        compose.mainClock.advanceTimeByFrame()
        openCertificate()
        compose.mainClock.advanceTimeBy(MotionTimeline.TITLE_UNLOCK_START_MS + 3_000L)
        assertEquals(0, popupCount())
    }

    @Test fun anOutsideSwipeDismissesThePopupAndStillScrollsTheCertificate() {
        ending()
        openCertificate()
        settle()
        val before = compose.onNodeWithTag("certificate").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("the certificate must be scrollable for this check", before.maxValue() > 0f)
        assertEquals(0f, before.value(), 0.5f)
        // Starts below the centred card, on the certificate itself.
        compose.onNodeWithTag("certificate").performTouchInput {
            swipeUp(startY = bottom - 8.dp.toPx(), endY = top + 8.dp.toPx(), durationMillis = 300)
        }
        compose.mainClock.advanceTimeBy(1_500)
        assertEquals(0, popupCount())
        val after = compose.onNodeWithTag("certificate").fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("the swipe still scrolled the certificate", after.value() > 0f)
    }

    @Test fun motionOnCollapsesByItselfAfterARest() {
        ending()
        openCertificate()
        settle()
        assertEquals(1, popupCount())
        compose.mainClock.advanceTimeBy(TitleUnlockTimeline.AUTO_COLLAPSE_MS + TitleUnlockTimeline.LEAVE_MS + 300L)
        assertEquals(0, popupCount())
    }

    @Test fun recreationMidDropRestoresTheSettledPopup() {
        val host = ManualRestoreHost(compose)
        ending(host = host)
        openCertificate()
        compose.mainClock.advanceTimeBy(MotionTimeline.TITLE_UNLOCK_START_MS + 300L)
        assertTrue(progress() < 1f)
        host.recreate()
        assertEquals(1, host.recreations)
        compose.mainClock.advanceTimeByFrame()
        assertEquals(1, popupCount())
        assertEquals(1f, progress(), 0.001f)
    }

    @Test fun reducedMotionShowsTheSettledPopupAtOnceAndRemovesItAtOnce() {
        ending(Scenario(motion = false))
        openCertificate()
        assertEquals("settled on the certificate's first frame", 1, popupCount())
        assertEquals(1f, progress(), 0.001f)
        // Nothing would animate it away, so it never collapses by itself.
        compose.mainClock.advanceTimeBy(TitleUnlockTimeline.AUTO_COLLAPSE_MS + 2_000L)
        assertEquals(1, popupCount())
        compose.onNodeWithTag("title_unlock_keep").performClick()
        compose.mainClock.advanceTimeByFrame()
        assertEquals(0, popupCount())
    }

    @Test fun touchExplorationShowsItSettledAndNeverCollapsesItByItself() {
        ending(Scenario(touchExploration = true))
        openCertificate()
        assertEquals(1, popupCount())
        assertEquals(1f, progress(), 0.001f)
        compose.mainClock.advanceTimeBy(TitleUnlockTimeline.AUTO_COLLAPSE_MS + 2_000L)
        assertEquals(1, popupCount())
    }

    @Test fun noPopupForNoneWithoutAnUnlockOrForAnArchivedRecord() {
        ending(Scenario(style = TitleUnlockStyle.NONE))
        openCertificate()
        compose.mainClock.advanceTimeBy(4_000)
        assertEquals(0, popupCount())

        restart(Scenario(unlock = null))
        openCertificate()
        compose.mainClock.advanceTimeBy(4_000)
        assertEquals(0, popupCount())

        restart(Scenario(initialStep = EndingStep.CERTIFICATE))
        compose.mainClock.advanceTimeBy(4_000)
        assertEquals(1, compose.onAllNodesWithTag("certificate").fetchSemanticsNodes().size)
        assertEquals(0, popupCount())
    }

    @Test fun celebratesTitleIsKnownInTheFirstComposition() {
        val saved = record()
        val seen = mutableListOf<Boolean>()
        var setup by mutableStateOf(Triple<TitleUnlockStyle, TitleUnlockUi?, Boolean>(TitleUnlockStyle.DROP_SPIN, TitleUnlockUi(1), true))
        compose.setContent {
            val (style, unlock, celebrate) = setup
            LifeGameTheme {
                CompositionLocalProvider(LocalMotionStyle provides MotionStyle.Default.copy(titleUnlock = style)) {
                    key(setup) {
                        val state = rememberTitleUnlockState(saved, unlock, celebrate)
                        SideEffect { seen += state.celebratesTitle }
                    }
                }
            }
        }
        fun check(style: TitleUnlockStyle, unlock: TitleUnlockUi?, celebrate: Boolean, expected: Boolean) {
            compose.runOnIdle { seen.clear(); setup = Triple(style, unlock, celebrate) }
            compose.runOnIdle {
                assertEquals("$style $unlock $celebrate", expected, seen.first())
                assertTrue("never flips", seen.all { it == expected })
            }
        }
        compose.runOnIdle { assertEquals(true, seen.first()) }
        check(TitleUnlockStyle.NONE, TitleUnlockUi(1), celebrate = true, expected = false)
        check(TitleUnlockStyle.STAMP, null, celebrate = true, expected = false)
        check(TitleUnlockStyle.FLIP_REVEAL, TitleUnlockUi(2), celebrate = false, expected = false)
        check(TitleUnlockStyle.FLIP_REVEAL, TitleUnlockUi(2), celebrate = true, expected = true)
    }

    @Test fun theRootCaptureWorksWhileThePopupShows() {
        ending()
        openCertificate()
        compose.mainClock.advanceTimeBy(MotionTimeline.TITLE_UNLOCK_START_MS + 500L)
        assertEquals(1, popupCount())
        // One compose root, no Dialog or Popup window.
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        assertTrue(bitmap.width > 0 && bitmap.height > 0)
    }
}
