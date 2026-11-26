package com.example.lifegame.ui

import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.StampSpec
import com.example.lifegame.ui.components.TitleUnlockStyle
import com.example.lifegame.ui.components.titleSeenEarnedIds
import com.example.lifegame.ui.components.titleSeenUnseenIds
import com.example.lifegame.ui.screens.TitleUnlockMedalPose
import com.example.lifegame.ui.screens.TitleUnlockTimeline
import com.example.lifegame.ui.screens.TitleUnlockUi
import com.example.lifegame.ui.screens.titleUnlockCardIn
import com.example.lifegame.ui.screens.titleUnlockCardOut
import com.example.lifegame.ui.screens.titleUnlockCardScale
import com.example.lifegame.ui.screens.titleUnlockCardShakeDp
import com.example.lifegame.ui.screens.titleUnlockEyebrow
import com.example.lifegame.ui.screens.titleUnlockFlight
import com.example.lifegame.ui.screens.titleUnlockFlipProgress
import com.example.lifegame.ui.screens.titleUnlockFor
import com.example.lifegame.ui.screens.titleUnlockHaloIn
import com.example.lifegame.ui.screens.titleUnlockLandFade
import com.example.lifegame.ui.screens.titleUnlockLandRing
import com.example.lifegame.ui.screens.titleUnlockMedalPose
import com.example.lifegame.ui.screens.titleUnlockScrimIn
import com.example.lifegame.ui.screens.titleUnlockScrimOut
import com.example.lifegame.ui.screens.titleUnlockSpring
import com.example.lifegame.ui.screens.titleUnlockTextIn
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 칭호 획득 팝업 · 새 칭호 알림 점(lobby2)의 규칙과 연출 시간표. */
class TitleUnlockRulesTest {
    private val animated = TitleUnlockStyle.entries.filter { it != TitleUnlockStyle.NONE }

    private fun life(runId: String, titleId: String?) = CompletedLifeRecord(
        runId = runId, playerName = "민서", summary = "지나온 삶", endingId = null, endingTitle = null, endingSummary = null,
        awardedTitle = titleId?.let { AwardedTitle(it, "칭호 $it", "설명 $it") },
        finalState = PeriodRunState(runId, 41L, LifePeriod.OLD_AGE, isEnding = true), choices = emptyList(),
    )

    private fun record(runId: String, titleId: String?) = buildLifeRecordUi(life(runId, titleId), RecordContent(emptyList()))

    @Test fun anUnlockCountsOnlyOtherRunsWithTheSameTitle() {
        val record = record("now", "kept")
        assertEquals(TitleUnlockUi(1), titleUnlockFor(record, emptyList()))
        // This run's own saved record never counts, nor do other titles or untitled lives.
        assertEquals(TitleUnlockUi(1), titleUnlockFor(record, listOf(life("now", "kept"), life("a", "other"), life("b", null))))
        val again = titleUnlockFor(record, listOf(life("now", "kept"), life("a", "kept"), life("b", "kept"), life("c", "other")))
        assertEquals(TitleUnlockUi(3), again)
        assertFalse(again!!.firstTime)
        assertTrue(TitleUnlockUi(1).firstTime)
    }

    @Test fun aRecordWithoutATitleHasNoUnlock() {
        assertNull(titleUnlockFor(record("now", null), listOf(life("a", "kept"))))
    }

    @Test fun theEyebrowSaysFirstOrHowManyTimes() {
        assertEquals("새 칭호를 받았어요", titleUnlockEyebrow(TitleUnlockUi(1)))
        assertEquals("다시 받은 칭호 · 3번째", titleUnlockEyebrow(TitleUnlockUi(3)))
        // Never one of the exact strings other screens' tests look up.
        listOf(TitleUnlockUi(1), TitleUnlockUi(2)).forEach { unlock ->
            val copy = titleUnlockEyebrow(unlock)
            assertFalse(copy == "인생기록증" || copy.startsWith("칭호 · ") || copy == "확인" || copy == "닫기")
        }
    }

    @Test fun nothingIsNewBeforeTheSeenStoreWasSeeded() {
        assertEquals(emptySet<String>(), titleSeenUnseenIds(setOf("a", "b"), null))
        assertEquals(setOf("b"), titleSeenUnseenIds(setOf("a", "b"), setOf("a")))
        assertEquals(emptySet<String>(), titleSeenUnseenIds(setOf("a"), setOf("a", "gone")))
        assertEquals(setOf("kept", "other"), titleSeenEarnedIds(listOf(life("a", "kept"), life("b", "other"), life("c", null), life("d", "kept"))))
    }

    @Test fun thePopupWaitsForTheCertificateEntrance() {
        assertTrue(MotionTimeline.TITLE_UNLOCK_START_MS >= MotionTimeline.CERTIFICATE_REVEAL_MS)
        assertEquals(0, TitleUnlockTimeline.totalMs(TitleUnlockStyle.NONE))
    }

    @Test fun everyVariantStartsHiddenAndSettlesExactlyAtRest() {
        animated.forEach { style ->
            val total = TitleUnlockTimeline.totalMs(style).toFloat()
            assertEquals("$style medal at rest", TitleUnlockMedalPose.Rest, titleUnlockMedalPose(style, total))
            assertEquals(TitleUnlockMedalPose.Rest, titleUnlockMedalPose(style, total + 5_000f))
            for (index in 0..3) {
                assertEquals("$style text $index starts hidden", 0f, titleUnlockTextIn(style, index, 0f), 0f)
                assertEquals("$style text $index settled", 1f, titleUnlockTextIn(style, index, total), 0f)
            }
            assertEquals(1f, titleUnlockHaloIn(style, total), 0f)
            assertEquals(0f, titleUnlockHaloIn(style, 0f), 0f)
            assertEquals(0f, titleUnlockCardShakeDp(style, total), 0f)
        }
        assertEquals(0f, titleUnlockScrimIn(0f), 0f)
        assertEquals(0f, titleUnlockCardIn(0f), 0f)
        assertEquals(0.92f, titleUnlockCardScale(0f), 1e-6f)
        assertEquals(1f, titleUnlockCardScale(TitleUnlockTimeline.totalMs(TitleUnlockStyle.DROP_SPIN).toFloat()), 1e-6f)
    }

    @Test fun dropSpinFallsFromAboveSpinsAndLandsWithOneBounce() {
        val start = titleUnlockMedalPose(TitleUnlockStyle.DROP_SPIN, 0f)
        assertEquals("waits above the region", 1f, start.fall, 0f)
        assertEquals(-540f, start.rotationY, 1e-3f)
        val landing = titleUnlockMedalPose(TitleUnlockStyle.DROP_SPIN, 540f)
        assertEquals("landed", 0f, landing.fall, 1e-6f)
        assertTrue("still turning as it lands", abs(landing.rotationY) > 10f)
        assertTrue("squashed at impact", landing.scaleY < 0.9f && landing.scaleX > 1.05f)
        assertTrue("one real bounce", titleUnlockMedalPose(TitleUnlockStyle.DROP_SPIN, 680f).liftDp < -10f)
    }

    @Test fun stampComesDownLargeAndLandsOnTheStampSpring() {
        val start = titleUnlockMedalPose(TitleUnlockStyle.STAMP, 0f)
        assertEquals(0f, start.alpha, 0f)
        assertEquals(2.3f, start.scaleX, 1e-6f)
        assertEquals(-14f, start.rotationZ, 0f)
        assertEquals(StampSpec.Pivot.pivotFractionY, start.pivotY, 0f)
        val impact = TitleUnlockTimeline.impactMs(TitleUnlockStyle.STAMP).toFloat()
        assertEquals(0.9f, titleUnlockMedalPose(TitleUnlockStyle.STAMP, impact).scaleX, 1e-4f)
        assertTrue("the card shakes on impact", abs(titleUnlockCardShakeDp(TitleUnlockStyle.STAMP, impact + 40f)) > 3f)
        assertEquals(0f, titleUnlockCardShakeDp(TitleUnlockStyle.DROP_SPIN, impact + 40f), 0f)
    }

    @Test fun flipShowsTheBackThenTurnsTheFrontUpNeverMirrored() {
        assertTrue(titleUnlockMedalPose(TitleUnlockStyle.FLIP_REVEAL, 300f).backFace)
        assertTrue(titleUnlockMedalPose(TitleUnlockStyle.FLIP_REVEAL, 779f).backFace)
        val total = TitleUnlockTimeline.totalMs(TitleUnlockStyle.FLIP_REVEAL)
        for (t in 0..total step 10) {
            val pose = titleUnlockMedalPose(TitleUnlockStyle.FLIP_REVEAL, t.toFloat())
            // The back turns 0° → 90°, the front −90° → 0° (a small overshoot past 0° at most).
            if (pose.backFace) assertTrue("t=$t", pose.rotationY in 0f..90f)
            else assertTrue("t=$t", pose.rotationY in -90f..30f)
        }
        assertEquals(1f, titleUnlockFlipProgress(1_240f), 0f)
        assertFalse(titleUnlockMedalPose(TitleUnlockStyle.FLIP_REVEAL, 1_240f).backFace)
    }

    @Test fun theExitFliesThenFadesOverTheHeaderMedal() {
        assertEquals(0f, titleUnlockFlight(0f), 0f)
        assertEquals(1f, titleUnlockFlight(TitleUnlockTimeline.FLIGHT_MS.toFloat()), 0f)
        assertEquals("the medal is opaque until it lands", 0f, titleUnlockLandFade(TitleUnlockTimeline.FLIGHT_MS.toFloat()), 0f)
        assertEquals(1f, titleUnlockLandFade((TitleUnlockTimeline.FLIGHT_MS + TitleUnlockTimeline.LAND_FADE_MS).toFloat()), 0f)
        assertEquals(1f, titleUnlockLandRing(TitleUnlockTimeline.LEAVE_MS.toFloat()), 0f)
        assertEquals(1f, titleUnlockCardOut(TitleUnlockTimeline.CARD_OUT_MS.toFloat()), 0f)
        assertEquals(1f, titleUnlockScrimOut(TitleUnlockTimeline.SCRIM_OUT_MS.toFloat()), 0f)
        assertEquals(880, TitleUnlockTimeline.LEAVE_MS)
    }

    @Test fun theAnalyticSpringStartsAtOneAndSettles() {
        assertEquals(1f, titleUnlockSpring(0f, 0.35f, 500f), 0f)
        assertTrue("an underdamped spring overshoots", (1..400).minOf { titleUnlockSpring(it.toFloat(), 0.35f, 500f) } < -0.2f)
        assertTrue(abs(titleUnlockSpring(1_200f, 0.35f, 500f)) < 0.001f)
        assertTrue(abs(titleUnlockSpring(1_000f, StampSpec.DAMPING, StampSpec.STIFFNESS)) < 0.001f)
        assertTrue(abs(titleUnlockSpring(800f, 1f, 400f)) < 0.001f)
    }
}
