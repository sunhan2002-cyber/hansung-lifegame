package com.example.lifegame.ui

import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Matrix
import com.example.lifegame.ui.components.AppearancePopStyle
import com.example.lifegame.ui.components.CertificateSealStyle
import com.example.lifegame.ui.components.CompletionTitleHaloTimeline
import com.example.lifegame.ui.components.GlyphStampTiming
import com.example.lifegame.ui.components.MotionTimeline
import com.example.lifegame.ui.components.ProfileEntranceStyle
import com.example.lifegame.ui.components.RecapMomentStyle
import com.example.lifegame.ui.components.StatCountUpStyle
import com.example.lifegame.ui.components.TitleRevealStyle
import com.example.lifegame.ui.components.completionTitleHalo
import com.example.lifegame.ui.screens.CertificateRevealTimeline
import com.example.lifegame.ui.screens.ProfileAppearancePopTimeline
import com.example.lifegame.ui.screens.ProfileEntranceTimeline
import com.example.lifegame.ui.screens.ProfileStatCountTimeline
import com.example.lifegame.ui.screens.RecapMomentTimeline
import com.example.lifegame.ui.screens.certificateBump
import com.example.lifegame.ui.screens.certificateSealSpecks
import com.example.lifegame.ui.screens.certificateTitleRevealFor
import com.example.lifegame.ui.screens.profileAppearanceBounce
import com.example.lifegame.ui.screens.profileAppearanceHop
import com.example.lifegame.ui.screens.profileEntranceAlpha
import com.example.lifegame.ui.screens.profileReelDigit
import com.example.lifegame.ui.screens.profileSlotReel
import com.example.lifegame.ui.screens.profileTickValue
import com.example.lifegame.ui.screens.profileUnfoldMatrix
import com.example.lifegame.ui.screens.recapDevelopMatrix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The bookends timelines and curves (stage 2): every effect rests exactly in its final state, the recap stays inside
 * the 2.8 s moment's budget, the certificate inside its reveal, and counts land on the real values.
 */
class CreationEndingTimelineTest {
    private val samples = (0..200).map { it / 200f }

    // ---- profileEntrance --------------------------------------------------------------------------------

    @Test fun profileEntranceTotalsMatchTheDesignAndCoverEveryStep() {
        val titleGlyphs = "새 인생".count { !it.isWhitespace() }
        assertEquals(870, ProfileEntranceTimeline.totalMs(ProfileEntranceStyle.RISE, titleGlyphs))
        assertEquals(920, ProfileEntranceTimeline.totalMs(ProfileEntranceStyle.STAMP_RISE, titleGlyphs))
        assertEquals(1_020, ProfileEntranceTimeline.totalMs(ProfileEntranceStyle.UNFOLD, titleGlyphs))
        // The stamped title has landed well before the last group; the groups still follow it.
        assertEquals(640f, ProfileEntranceTimeline.TitleStamp.endMs(titleGlyphs), 0f)
        assertTrue(ProfileEntranceTimeline.startMs(ProfileEntranceStyle.STAMP_RISE, 1) > ProfileEntranceTimeline.TitleStamp.startMs)
        // A resting clock (NONE) never sits below a later style's total, so switching styles cannot start it.
        val none = ProfileEntranceTimeline.totalMs(ProfileEntranceStyle.NONE, titleGlyphs)
        ProfileEntranceStyle.entries.forEach { assertTrue(none >= ProfileEntranceTimeline.totalMs(it, titleGlyphs)) }
        for (style in ProfileEntranceStyle.entries - ProfileEntranceStyle.NONE) {
            val total = ProfileEntranceTimeline.totalMs(style, titleGlyphs).toFloat()
            for (group in 0 until ProfileEntranceTimeline.GROUP_COUNT) {
                val start = ProfileEntranceTimeline.startMs(style, group)
                assertTrue("$style group $group starts after the nav fade's first beat", start >= 100f)
                assertTrue("$style group $group ends inside the clock", start + ProfileEntranceTimeline.durationMs(style) <= total)
                assertEquals(0f, profileEntranceAlpha(style, 0f, start, ProfileEntranceTimeline.durationMs(style)), 0f)
                assertEquals(1f, profileEntranceAlpha(style, total, start, ProfileEntranceTimeline.durationMs(style)), 0f)
            }
        }
    }

    @Test fun unfoldSwingsSlightlyPastFlatAndIsIdentityAtRest() {
        val easing = ProfileEntranceTimeline.UnfoldEasing
        assertEquals(1f, easing.transform(1f), 1e-5f)
        val peak = samples.maxOf { easing.transform(it) }
        assertTrue("a little past flat, then back ($peak)", peak in 1.01f..1.08f)
        val matrix = Matrix()
        profileUnfoldMatrix(matrix, 0f, cameraDistancePx = 2_000f)
        assertTrue("0° is the identity", matrix.values.contentEquals(Matrix().values))
        profileUnfoldMatrix(matrix, ProfileEntranceTimeline.UNFOLD_DEGREES, cameraDistancePx = 2_000f)
        // Folded back 78°: a fifth of its height, and the lower edge comes towards the viewer (w < 1 below the hinge).
        assertEquals(0.208f, matrix.values[Matrix.ScaleY], 0.001f)
        assertTrue(matrix.values[Matrix.Perspective1] < 0f)
    }

    // ---- appearancePop ----------------------------------------------------------------------------------

    @Test fun appearanceBounceSquashesStretchesAndLandsExactlyOnOne() {
        assertEquals(1f, profileAppearanceBounce(0f).scaleX, 0f)
        assertEquals(1f, profileAppearanceBounce(1f).scaleY, 0f)
        assertEquals(1.07f, profileAppearanceBounce(0.18f).scaleX, 1e-4f)
        assertEquals(0.90f, profileAppearanceBounce(0.18f).scaleY, 1e-4f)
        assertEquals(0.97f, profileAppearanceBounce(0.5f).scaleX, 1e-4f)
        assertEquals(1.05f, profileAppearanceBounce(0.5f).scaleY, 1e-4f)
        // Continuous: no jump between neighbouring samples anywhere on the curve.
        samples.zipWithNext().forEach { (a, b) ->
            assertTrue("x jumps at $a", abs(profileAppearanceBounce(a).scaleX - profileAppearanceBounce(b).scaleX) < 0.02f)
            assertTrue("y jumps at $a", abs(profileAppearanceBounce(a).scaleY - profileAppearanceBounce(b).scaleY) < 0.03f)
        }
        assertTrue("the hop stays small and is gone at rest", samples.all { profileAppearanceHop(it) in 0f..1f })
        assertEquals(0f, profileAppearanceHop(0f), 0f)
        assertEquals(0f, profileAppearanceHop(1f), 0f)
        assertEquals(1f, profileAppearanceHop(0.4f), 1e-5f)
    }

    @Test fun appearancePopTotalsFollowTheStagger() {
        assertEquals(560, ProfileAppearancePopTimeline.totalMs(AppearancePopStyle.BOUNCE))
        assertEquals(770, ProfileAppearancePopTimeline.totalMs(AppearancePopStyle.WAVE))
        assertEquals(640, ProfileAppearancePopTimeline.totalMs(AppearancePopStyle.INK))
        // WAVE travels from infancy to old age.
        val starts = (0 until ProfileAppearancePopTimeline.AVATARS).map { ProfileAppearancePopTimeline.startMs(AppearancePopStyle.WAVE, it) }
        assertEquals(starts.sorted(), starts)
    }

    // ---- statCountUp ------------------------------------------------------------------------------------

    @Test fun tickCountsFromTheOldValueAndLandsOnTheFinalOne() {
        for ((from, to) in listOf(50 to 55, 55 to 47, 47 to 47, 50 to 62)) {
            assertEquals(from, profileTickValue(from, to, 0f))
            assertEquals(to, profileTickValue(from, to, 1f))
            val values = samples.map { profileTickValue(from, to, it) }
            assertTrue("$from→$to stays between its ends", values.all { it in minOf(from, to)..maxOf(from, to) })
            assertEquals("$from→$to never turns back", if (to >= from) values.sorted() else values.sortedDescending(), values)
        }
    }

    @Test fun slotReelsLandOnEveryDigitOfTheFinalValue() {
        for (from in 30..70) for (to in 30..70) {
            to.toString().reversed().forEachIndexed { place, digit ->
                val landed = profileReelDigit(profileSlotReel(from, to, place, 1f))
                assertEquals("$from→$to place $place", digit - '0', landed)
            }
            // The reel starts on the old value's digit.
            assertEquals(from % 10, profileReelDigit(profileSlotReel(from, to, 0, 0f)))
            // The ones reel always turns forward at least one full round.
            assertTrue(profileSlotReel(from, to, 0, 1f) - profileSlotReel(from, to, 0, 0f) >= 10f)
        }
    }

    @Test fun statCountTotalsAndRowOrder() {
        assertEquals(760, ProfileStatCountTimeline.totalMs(StatCountUpStyle.TICK))
        assertEquals(1_210, ProfileStatCountTimeline.totalMs(StatCountUpStyle.SLOT))
        assertEquals(700, ProfileStatCountTimeline.totalMs(StatCountUpStyle.STAMP))
        val none = ProfileStatCountTimeline.totalMs(StatCountUpStyle.NONE)
        StatCountUpStyle.entries.forEach { assertTrue(none >= ProfileStatCountTimeline.totalMs(it)) }
        for (style in StatCountUpStyle.entries - StatCountUpStyle.NONE) {
            val ends = (0 until ProfileStatCountTimeline.ROWS).map { ProfileStatCountTimeline.rowEndMs(style, it) }
            assertEquals("$style rows settle top to bottom", ends.sorted(), ends)
        }
    }

    // ---- recapMoment ------------------------------------------------------------------------------------

    @Test fun everyRecapMomentEndsWellInsideItsTwoPointEightSeconds() {
        for (style in RecapMomentStyle.entries) {
            val total = RecapMomentTimeline.totalMs(style)
            assertTrue("$style ends by ${MotionTimeline.RECAP_MOTION_MAX_MS} ms ($total)", total <= MotionTimeline.RECAP_MOTION_MAX_MS)
            for (line in 0 until RecapMomentTimeline.LINES) {
                val end = RecapMomentTimeline.lineStartMs(style, line) + RecapMomentTimeline.lineDurationMs(style, line)
                assertTrue("$style line $line ends inside its clock", end <= total)
            }
        }
        assertEquals(900, RecapMomentTimeline.totalMs(RecapMomentStyle.SURFACE))
        assertEquals(1_080, RecapMomentTimeline.totalMs(RecapMomentStyle.DEVELOP))
        assertTrue(RecapMomentTimeline.LANTERN_SLIDE_MS <= MotionTimeline.RECAP_MOTION_MAX_MS)
        // Lines appear in reading order.
        for (style in listOf(RecapMomentStyle.SURFACE, RecapMomentStyle.DEVELOP)) {
            val starts = (0 until RecapMomentTimeline.LINES).map { RecapMomentTimeline.lineStartMs(style, it) }
            assertEquals(starts.sorted(), starts)
        }
    }

    @Test fun developRunsFromSepiaToTheIdentity() {
        val matrix = ColorMatrix()
        recapDevelopMatrix(matrix, 1f)
        assertTrue("fully developed is the identity", matrix.values.contentEquals(ColorMatrix().values))
        recapDevelopMatrix(matrix, 0f)
        assertEquals(0.393f, matrix.values[0], 1e-6f)
        assertEquals(0.769f, matrix.values[1], 1e-6f)
        assertEquals(1f, matrix.values[18], 0f) // alpha is never touched
    }

    // ---- certificateSeal · titleReveal -----------------------------------------------------------------

    @Test fun theWholeCertificateRevealEndsByItsBudget() {
        val budget = MotionTimeline.CERTIFICATE_REVEAL_MS
        CertificateSealStyle.entries.forEach { assertTrue("$it", CertificateRevealTimeline.sealEndMs(it) <= budget) }
        for (glyphs in 1..30) {
            TitleRevealStyle.entries.forEach { assertTrue("$it with $glyphs glyphs", CertificateRevealTimeline.titleEndMs(it, glyphs) <= budget) }
        }
        assertEquals(910, CertificateRevealTimeline.sealEndMs(CertificateSealStyle.STAMP))
        assertEquals(880, CertificateRevealTimeline.sealEndMs(CertificateSealStyle.INK_BLEED))
        // The seal lands only after the card has risen, and the title-unlock popup waits for the whole reveal.
        assertTrue(CertificateRevealTimeline.SEAL_DROP_START_MS >= CertificateRevealTimeline.CARD_RISE_MS)
        assertTrue(MotionTimeline.TITLE_UNLOCK_START_MS >= budget)
    }

    @Test fun titleHaloBreathesOnceAndIsGoneAtRest() {
        assertEquals(0f, completionTitleHalo(0f), 0f)
        assertEquals(0f, completionTitleHalo(CompletionTitleHaloTimeline.RISE_START_MS.toFloat()), 0f)
        assertEquals(1f, completionTitleHalo(CompletionTitleHaloTimeline.PEAK_MS.toFloat()), 1e-5f)
        assertEquals(0f, completionTitleHalo(CompletionTitleHaloTimeline.END_MS.toFloat()), 0f)
        assertEquals(0f, completionTitleHalo(MotionTimeline.CERTIFICATE_REVEAL_MS.toFloat()), 0f)
        val values = (0..1_000 step 10).map { completionTitleHalo(it.toFloat()) }
        assertTrue(values.all { it in 0f..1f })
        // One breath: never falls before the peak, never rises after it.
        val peak = values.indexOf(values.max())
        assertEquals(CompletionTitleHaloTimeline.PEAK_MS / 10, peak)
        assertTrue(values.take(peak + 1).zipWithNext().all { (a, b) -> b >= a })
        assertTrue(values.drop(peak).zipWithNext().all { (a, b) -> b <= a })
    }

    @Test fun glyphStampTimingShrinksItsGapForLongTitles() {
        val stamp = CertificateRevealTimeline.TitleStamp
        assertEquals(45f, stamp.gapFor(3), 0f)
        assertEquals(200f / 19f, stamp.gapFor(20), 1e-4f)
        assertTrue(stamp.endMs(40) <= MotionTimeline.CERTIFICATE_REVEAL_MS)
        val lobbyLike = GlyphStampTiming(startMs = 160f, gapMs = 90f, durationMs = 300f)
        assertEquals(0f, lobbyLike.progress(159f, 0, 3), 0f)
        assertEquals(1f, lobbyLike.progress(640f, 2, 3), 0f)
        assertEquals(160f, lobbyLike.endMs(0), 0f)
    }

    @Test fun sealBumpsReturnToRestAndTheSealIsTheSameForTheSameRecord() {
        assertEquals(0f, certificateBump(0f), 0f)
        assertEquals(0f, certificateBump(1f), 0f)
        assertEquals(1f, certificateBump(0.5f), 1e-5f)
        val specks = certificateSealSpecks("run-42")
        assertEquals(7, specks.size)
        assertEquals("deterministic: no random stream", specks, certificateSealSpecks("run-42"))
        assertNotEquals(specks, certificateSealSpecks("run-43"))
        specks.forEach { speck ->
            assertTrue("inside the inner frame", abs(speck.x) <= 0.36f && abs(speck.y) <= 0.36f)
            assertTrue(speck.radius in 0.010f..0.032f)
        }
    }

    @Test fun titleRevealRestsWithoutATitleAndWhileThePopupCelebrates() {
        for (style in TitleRevealStyle.entries) {
            assertEquals(style, certificateTitleRevealFor(style, hasTitle = true, popupCelebrates = false))
            assertEquals(TitleRevealStyle.NONE, certificateTitleRevealFor(style, hasTitle = true, popupCelebrates = true))
            assertEquals(TitleRevealStyle.NONE, certificateTitleRevealFor(style, hasTitle = false, popupCelebrates = false))
        }
    }
}
