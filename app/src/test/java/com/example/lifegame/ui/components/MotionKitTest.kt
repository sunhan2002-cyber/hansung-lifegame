package com.example.lifegame.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.exp

class MotionKitTest {
    @Test fun stepProgressWaitsForItsStartClampsAndEndsExactlyAtOne() {
        assertEquals(0f, stepProgress(clockMs = 0f, startMs = 100f, durationMs = 400f), 0f)
        assertEquals(0f, stepProgress(100f, 100f, 400f), 0f)
        assertEquals(0.5f, stepProgress(300f, 100f, 400f), 1e-6f)
        assertEquals(1f, stepProgress(500f, 100f, 400f), 0f)
        assertEquals(1f, stepProgress(5_000f, 100f, 400f), 0f)
        // Curves are applied to the clamped fraction and still land exactly on 1.
        for (easing in listOf(RiseEasing, EaseOutBack, FastOutSlowInEasing)) {
            assertEquals(1f, stepProgress(900f, 100f, 400f, easing), 0f)
            assertEquals(0f, stepProgress(-50f, 100f, 400f, easing), 0f)
        }
        // A zero-length step is a jump at its start.
        assertEquals(0f, stepProgress(99f, 100f, 0f), 0f)
        assertEquals(1f, stepProgress(100f, 100f, 0f), 0f)
    }

    @Test fun dampedShakeStartsAtRestStaysInsideItsEnvelopeAndDiesOut() {
        assertEquals(0f, dampedShake(0f, amplitude = 6f, periodMs = 62f, decayMs = 120f), 0f)
        assertEquals(0f, dampedShake(-30f, 6f, 62f, 120f), 0f)
        var peak = 0f
        for (t in 1..600) {
            val value = dampedShake(t.toFloat(), 6f, 62f, 120f)
            assertTrue("t=$t stays inside the decaying envelope", abs(value) <= 6f * exp(-t / 120f) + 1e-4f)
            peak = maxOf(peak, abs(value))
        }
        assertTrue("it really shakes", peak > 3f)
        assertTrue("it has settled after about five decay times", abs(dampedShake(600f, 6f, 62f, 120f)) < 0.05f)
        // Deterministic: the same inputs always give the same offset.
        assertEquals(dampedShake(37f, 6f, 62f, 120f), dampedShake(37f, 6f, 62f, 120f), 0f)
    }

    @Test fun curvesStartAtZeroEndAtOneAndOnlyBackOutOvershoots() {
        assertEquals(0f, RiseEasing.transform(0f), 0f)
        assertEquals(1f, RiseEasing.transform(1f), 0f)
        assertEquals(0f, EaseOutBack.transform(0f), 1e-6f)
        assertEquals(1f, EaseOutBack.transform(1f), 1e-6f)
        val samples = (0..100).map { it / 100f }
        assertTrue("RiseEasing settles without overshoot", samples.all { RiseEasing.transform(it) <= 1f + 1e-4f })
        val backPeak = samples.maxOf { EaseOutBack.transform(it) }
        assertTrue("EaseOutBack passes the target by about 10% ($backPeak)", backPeak in 1.08f..1.12f)
    }

    @Test fun stampSpecKeepsTheLobbyTitleStampValues() {
        assertEquals(0.48f, StampSpec.DAMPING, 0f)
        assertEquals(Spring.StiffnessMediumLow, StampSpec.STIFFNESS, 0f)
        assertEquals(0.9f, StampSpec.SCALE_BOOST, 0f)
        assertEquals(-9f, StampSpec.TILT_EVEN, 0f)
        assertEquals(7f, StampSpec.TILT_ODD, 0f)
        assertEquals(0.5f, StampSpec.Pivot.pivotFractionX, 0f)
        assertEquals(0.62f, StampSpec.Pivot.pivotFractionY, 0f)
        val spring = StampSpec.spring<Float>()
        assertEquals(0.48f, spring.dampingRatio, 0f)
        assertEquals(Spring.StiffnessMediumLow, spring.stiffness, 0f)
        // Dropped: 1.9× and tilted, transparent. Landed: identity.
        assertEquals(1.9f, StampSpec.scaleAt(0f), 1e-6f)
        assertEquals(1f, StampSpec.scaleAt(1f), 0f)
        assertEquals(-9f, StampSpec.tiltAt(0, 0f), 0f)
        assertEquals(7f, StampSpec.tiltAt(1, 0f), 0f)
        assertEquals(0f, StampSpec.tiltAt(4, 1f), 0f)
        assertEquals(0f, StampSpec.alphaAt(0f), 0f)
        assertEquals(1f, StampSpec.alphaAt(1f / 3f), 1e-6f)
        assertEquals(1f, StampSpec.alphaAt(1f), 0f)
    }

    @Test fun theTitlePopupStartsOnlyAfterTheCertificateEntranceHasEnded() {
        assertEquals(1_000, MotionTimeline.CERTIFICATE_REVEAL_MS)
        assertEquals(1_100, MotionTimeline.TITLE_UNLOCK_START_MS)
        assertTrue(MotionTimeline.TITLE_UNLOCK_START_MS > MotionTimeline.CERTIFICATE_REVEAL_MS)
        assertTrue("recap motion ends well before the 2.8 s auto-advance", MotionTimeline.RECAP_MOTION_MAX_MS < 2_800)
    }

    @Test fun motionHapticsKeepTheirDistanceAndFallBackBelowApi30() {
        assertTrue(motionHapticAllowed(lastFiredAtMs = null, nowMs = 0L))
        assertFalse(motionHapticAllowed(lastFiredAtMs = 1_000L, nowMs = 1_119L))
        assertTrue(motionHapticAllowed(lastFiredAtMs = 1_000L, nowMs = 1_120L))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, MotionHaptic.CONFIRM.feedbackConstant(29))
        assertEquals(HapticFeedbackConstants.CONFIRM, MotionHaptic.CONFIRM.feedbackConstant(30))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, MotionHaptic.REJECT.feedbackConstant(26))
        assertEquals(HapticFeedbackConstants.REJECT, MotionHaptic.REJECT.feedbackConstant(35))
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, MotionHaptic.TICK.feedbackConstant(26))
        assertEquals(HapticFeedbackConstants.CONTEXT_CLICK, MotionHaptic.CONTEXT.feedbackConstant(26))
        assertEquals(HapticFeedbackConstants.LONG_PRESS, MotionHaptic.HEAVY.feedbackConstant(35))
    }
}
