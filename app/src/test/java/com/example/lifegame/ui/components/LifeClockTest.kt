package com.example.lifegame.ui.components

import androidx.compose.ui.geometry.Offset
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.PlayPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sqrt

/**
 * 해 도장 · 시기 전환(2단계 time)의 순수 계산: 표시(mark)와 시기 넘김 판정, 기억, 도장 시간표, 해 표기 쪼개기, 책장 넘김 기하.
 * 나이는 만들지 않는다: 도장은 PeriodTrack의 "N번째 해"만 쓴다.
 */
class LifeClockTest {
    private fun mark(period: Int, year: Int, phase: PlayPhase = PlayPhase.AUTO, run: String = "run", speed: Int = 1) =
        LifeMark(run, period, year, periodName = "시기$period", chapterLabel = "제${period + 1}장", phase = phase, speed = speed)

    // ---- marks and turns ----

    @Test fun markComesFromThePlayStateAndCarriesNoAge() {
        val play = PeriodPlayUiState(
            runKey = "run-1", playerName = "재겸", phase = PlayPhase.AUTO, periodName = "유년기", chapterLabel = "제2장",
            periodYears = 5, periodIntro = "", yearInPeriod = 3,
            periodMarks = listOf(PeriodMarkUi("영아", 1f, false), PeriodMarkUi("유년", 0.6f, true), PeriodMarkUi("청소년", 0f, false)),
            previousPeriodName = "영아기", stats = Stats(), focus = null, focusOptions = emptyList(), sceneImageId = "",
            sceneFile = null, log = emptyList(), yearKey = "k", decision = null, outcome = null, isDead = false,
            autoPlaying = true, speed = 2,
        )
        assertEquals(LifeMark("run-1", 1, 3, "유년기", "제2장", PlayPhase.AUTO, 2), lifeMarkOf(play))
        // No current period on the track: −1, and nothing can turn from it.
        val loose = lifeMarkOf(play.copy(periodMarks = play.periodMarks.map { it.copy(isCurrent = false) }))
        assertEquals(-1, loose.periodIndex)
        assertFalse(isPeriodTurn(loose, mark(2, 1, PlayPhase.CHAPTER, run = "run-1")))
    }

    @Test fun aTurnIsAForwardPeriodChangeOntoAChapterCoverOrAutoProgress() {
        assertTrue("유년기 끝 → 청소년기 표지", isPeriodTurn(mark(1, 5), mark(2, 1, PlayPhase.CHAPTER)))
        assertTrue("결과 화면에서 다음 시기 표지로", isPeriodTurn(mark(1, 5, PlayPhase.DECISION), mark(2, 1, PlayPhase.CHAPTER)))
        assertTrue("중년기 → 노년기는 표지 없이 자동 진행", isPeriodTurn(mark(4, 5), mark(5, 1, PlayPhase.AUTO)))
        assertTrue("두 시기를 건너뛰어도", isPeriodTurn(mark(1, 3), mark(3, 1, PlayPhase.CHAPTER)))
        assertFalse("첫 표시는 기준만 잡는다", isPeriodTurn(null, mark(2, 1, PlayPhase.CHAPTER)))
        assertFalse(isPeriodTurn(mark(1, 5), null))
        assertFalse("사건 화면으로는 넘기지 않는다", isPeriodTurn(mark(4, 5), mark(5, 2, PlayPhase.DECISION)))
        assertFalse("결말로는 넘기지 않는다", isPeriodTurn(mark(5, 5), mark(6, 1, PlayPhase.ENDED)))
        assertFalse("다른 인생", isPeriodTurn(mark(1, 5, run = "a"), mark(2, 1, PlayPhase.CHAPTER, run = "b")))
        assertFalse("같은 시기", isPeriodTurn(mark(2, 1, PlayPhase.CHAPTER), mark(2, 1, PlayPhase.AUTO)))
        assertFalse("뒤로 가지 않는다", isPeriodTurn(mark(2, 3), mark(1, 5, PlayPhase.CHAPTER)))
    }

    @Test fun playbackSpeedShortensOnlyATurnThatLandsOnAutoProgress() {
        assertEquals(4, PeriodTurn(mark(4, 5), mark(5, 1, PlayPhase.AUTO, speed = 4), serial = 1).speed)
        assertEquals(1, PeriodTurn(mark(1, 5, speed = 4), mark(2, 1, PlayPhase.CHAPTER, speed = 4), serial = 1).speed)
        assertEquals(1, PeriodTurn(mark(4, 5), mark(5, 1, PlayPhase.AUTO, speed = 0), serial = 1).speed)
    }

    @Test fun memoryKeepsTheFirstMarkAsABaselineAndPresentsAtOnceOnlyWhenAsked() {
        val memory = LifeClockMemory()
        memory.commit(mark(1, 2), null, presentAtOnce = false)
        assertEquals("the first mark is a silent baseline", mark(1, 2), memory.trackShown)
        memory.commit(mark(1, 3), null, presentAtOnce = false)
        assertEquals("a later year waits for a PeriodTrack to present it", mark(1, 2), memory.trackShown)
        assertEquals(mark(1, 3), memory.committed)
        memory.commit(mark(1, 4), null, presentAtOnce = true)
        assertEquals("reduced motion or NONE commit at once", mark(1, 4), memory.trackShown)
        memory.present(mark(1, 5))
        assertEquals(mark(1, 5), memory.trackShown)
        memory.commit(mark(0, 1, run = "next"), null, presentAtOnce = false)
        assertEquals("a new run starts a new baseline", mark(0, 1, run = "next"), memory.trackShown)
        memory.commit(null, null, presentAtOnce = false)
        assertNull(memory.committed)
        assertEquals(mark(0, 1, run = "next"), memory.trackShown)

        val first = PeriodTurn(mark(1, 5), mark(2, 1, PlayPhase.CHAPTER), serial = 1)
        memory.commit(first.to, first, presentAtOnce = false)
        assertEquals(first, memory.latestTurn)
        memory.commit(mark(2, 1), PeriodTurn(mark(0, 1), mark(1, 1, PlayPhase.CHAPTER), serial = 1), presentAtOnce = false)
        assertEquals("only a newer serial replaces the turn", first, memory.latestTurn)
        memory.finishTurn(1)
        memory.finishTurn(0)
        assertEquals("finished serials only grow", 1, memory.finishedTurnSerial.intValue)
    }

    // ---- period turn timing and geometry ----

    @Test fun turnTimingFollowsTheDesignAndOnlyShrinksDownToItsFloors() {
        val film = periodTurnTiming(PeriodTransitionStyle.FILM_ADVANCE, speed = 1)
        assertEquals(PeriodTurnTiming(160f, NOISE_ADVANCE_MILLIS.toFloat(), 320f), film)
        assertEquals(820f, film.totalMs, 0f)
        assertEquals(PeriodTurnTiming(80f, 170f, 160f), periodTurnTiming(PeriodTransitionStyle.FILM_ADVANCE, speed = 2))
        assertEquals(PeriodTurnTiming(60f, 120f, 140f), periodTurnTiming(PeriodTransitionStyle.FILM_ADVANCE, speed = 4))
        assertEquals(760f, periodTurnTiming(PeriodTransitionStyle.PAGE_TURN, speed = 1).totalMs, 0f)
        assertEquals(380f, periodTurnTiming(PeriodTransitionStyle.PAGE_TURN, speed = 2).totalMs, 0f)
        assertEquals(280f, periodTurnTiming(PeriodTransitionStyle.PAGE_TURN, speed = 4).totalMs, 0f)
        assertEquals(0f, periodTurnTiming(PeriodTransitionStyle.NONE, speed = 1).totalMs, 0f)
        assertEquals(film, periodTurnTiming(PeriodTransitionStyle.FILM_ADVANCE, speed = 0))
    }

    @Test fun pageTurnRevealsFromTheCornerCoversNothingAtTheEndAndFoldsOntoThePage() {
        val w = 360f
        val h = 640f
        val screen = w * h
        val start = pageTurnFrame(w, h, 0f, 16f)
        assertEquals(screen, pageTurnArea(start.remaining), 0.5f)
        assertTrue("nothing is revealed before the page lifts", start.revealed.isEmpty() && start.flap.isEmpty())
        val end = pageTurnFrame(w, h, 1f, 16f)
        assertTrue("nothing of the old page is left", end.remaining.isEmpty())
        assertEquals(screen, pageTurnArea(end.revealed), 0.5f)
        val u = PageTurnDirection
        assertEquals(1f, sqrt(u.x * u.x + u.y * u.y), 1e-5f)
        // At the end even the folded flap has left the screen.
        end.flap.forEach { assertTrue("flap $it is off screen", it.x < 0f || it.y < 0f) }

        var revealedBefore = 0f
        for (progress in listOf(0.05f, 0.2f, 0.35f, 0.5f, 0.65f, 0.8f, 0.95f)) {
            val frame = pageTurnFrame(w, h, progress, 16f)
            val revealed = pageTurnArea(frame.revealed)
            assertEquals("p=$progress: the two parts tile the screen", screen, pageTurnArea(frame.remaining) + revealed, screen * 1e-4f)
            assertTrue("p=$progress: the reveal only grows", revealed > revealedBefore)
            revealedBefore = revealed
            // d from the fold point: (foldPoint − C)·u
            val d = (frame.foldPoint.x - w) * u.x + (frame.foldPoint.y - h) * u.y
            fun side(p: Offset) = (p.x - w) * u.x + (p.y - h) * u.y - d
            frame.flap.forEach { assertTrue("p=$progress: flap $it lies over the remaining page", side(it) >= -0.01f) }
            frame.revealed.forEach { assertTrue("p=$progress: revealed $it is on the corner's side", side(it) <= 0.01f) }
            assertEquals(0f, side(frame.foldStart), 0.05f)
            assertEquals(0f, side(frame.foldEnd), 0.05f)
            // The flap is the revealed part mirrored: same area.
            assertEquals(revealed, pageTurnArea(frame.flap), screen * 1e-4f)
        }
        // The bottom-right corner (focus cards, start and playback buttons) is uncovered first.
        val early = pageTurnFrame(w, h, 0.1f, 16f)
        assertTrue(early.revealed.any { abs(it.x - w) < 0.01f && abs(it.y - h) < 0.01f })
        assertTrue(early.remaining.any { it.x == 0f && it.y == 0f })
    }

    // ---- the year label ----

    @Test fun yearLabelIsStampedGlyphByGlyphWithALargeNumber() {
        assertEquals(
            listOf(
                YearStampGlyph("3", large = true, stamped = true),
                YearStampGlyph("번", large = false, stamped = true),
                YearStampGlyph("째", large = false, stamped = true),
                YearStampGlyph(" ", large = false, stamped = false),
                YearStampGlyph("해", large = false, stamped = true),
            ),
            yearStampGlyphs("3번째 해"),
        )
        assertEquals(listOf(true, true, false, false, false, false), yearStampGlyphs("12번째 해").map { it.large })
        assertTrue(yearStampGlyphs(null).isEmpty())
        assertTrue(yearStampGlyphs("  ").isEmpty())
        // Never an age: nothing here produces "세".
        assertFalse(yearStampGlyphs("3번째 해").any { it.text == "세" })
    }

    @Test fun theRolledNumberIsTheFirstRunOfDigits() {
        assertEquals(YearStampNumber("", "3", "번째 해"), yearStampNumber("3번째 해"))
        assertEquals(YearStampNumber("", "12", "번째 해"), yearStampNumber("12번째 해"))
        assertEquals(YearStampNumber("제", "2", "장"), yearStampNumber("제2장"))
        assertNull(yearStampNumber("마지막 해"))
        assertNull(yearStampNumber(null))
    }

    @Test fun sealSpecksComeFromThePlaceNeverFromTheGame() {
        val specks = yearStampSpecks(periodIndex = 2, year = 3)
        assertEquals(6, specks.size)
        assertEquals("deterministic", specks, yearStampSpecks(2, 3))
        assertNotEquals(specks, yearStampSpecks(2, 4))
        assertNotEquals(specks, yearStampSpecks(3, 3))
        specks.forEach {
            assertTrue("inside the inner ring: $it", it.distance in 0.38f..0.88f)
            assertTrue(it.angle in 0f..(2f * PI.toFloat()))
            assertTrue(it.radius in 0.5f..1.1f)
        }
    }

    @Test fun clampFallsBackToTheMiddleWhenTheLabelIsWiderThanTheTrack() {
        assertEquals(50f, lifeClockClamp(10f, 50f, 300f), 0f)
        assertEquals(300f, lifeClockClamp(400f, 50f, 300f), 0f)
        assertEquals(120f, lifeClockClamp(120f, 50f, 300f), 0f)
        assertEquals(150f, lifeClockClamp(10f, 200f, 100f), 0f)
    }

    // ---- the lifeline sequence ----

    @Test fun planTravelsOnANewPeriodAndStampsTheLabel() {
        assertNull("NONE moves nothing", periodTrackPlan(AgeStampStyle.NONE, mark(1, 2), mark(2, 1), "1번째 해"))
        assertNull("no new period and no label", periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(1, 2), mark(1, 3), null))
        val year = periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(1, 2), mark(1, 3), "3번째 해")!!
        assertEquals(0f, year.travelMs, 0f)
        assertEquals(1 to 1, year.fromIndex to year.toIndex)
        assertEquals(4, year.glyphCount)
        val period = periodTrackPlan(AgeStampStyle.SEAL, mark(1, 5), mark(2, 1), "1번째 해")!!
        assertEquals(560f, period.travelMs, 0f)
        assertEquals(1 to 2, period.fromIndex to period.toIndex)
        val travelOnly = periodTrackPlan(AgeStampStyle.ROLL, mark(1, 5), mark(2, 1), null)!!
        assertEquals(0, travelOnly.glyphCount)
        assertEquals("only the marker moves", travelOnly.travelMs, travelOnly.totalMs, 0f)
        assertNull(travelOnly.beat)
        assertTrue(periodTrackPlan(AgeStampStyle.ROLL, mark(1, 2), mark(1, 3), "3번째 해")!!.rolls)
        assertFalse("the same number does not roll", periodTrackPlan(AgeStampStyle.ROLL, mark(1, 1), mark(2, 1), "1번째 해")!!.rolls)
    }

    @Test fun theMarkerTravelShrinksWithAutoSpeedSoItFitsTheYear() {
        val at1x = periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(4, 5), mark(5, 1, speed = 1), "1번째 해")!!
        assertEquals(560f, at1x.travelMs, 0f)
        assertEquals(280f, periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(4, 5), mark(5, 1, speed = 2), "1번째 해")!!.travelMs, 0f)
        val at4x = periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(4, 5), mark(5, 1, speed = 4), "1번째 해")!!
        assertEquals("floored", 240f, at4x.travelMs, 0f)
        // At 4× a year is 750 ms: the 280 ms turn plus the travel must land inside it.
        assertTrue(280f + at4x.travelMs < 750f)
        // At 4× years pass faster than a stamp: no stamp, only the marker (and nothing at all within a period).
        assertEquals(0, at4x.glyphCount)
        assertNull(periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(5, 1, speed = 4), mark(5, 2, speed = 4), "2번째 해"))
        assertEquals(4, periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(5, 1, speed = 2), mark(5, 2, speed = 2), "2번째 해")!!.glyphCount)
        // Only AUTO screens run at a speed: a decision screen keeps the full travel.
        assertEquals(560f, periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(4, 5), mark(5, 1, PlayPhase.DECISION, speed = 4), "1번째 해")!!.travelMs, 0f)
    }

    @Test fun aNewerTrackTravelsOnlyFromWhereTheMarkerAlreadyStands() {
        // 4×: the marker reached period 2 for (2,1) but that stamp never landed, so trackShown is still (1,5).
        val memory = LifeClockMemory(seed = mark(1, 5))
        memory.arrive(mark(2, 1))
        val from = memory.travelFrom(mark(1, 5))
        assertEquals("the travel starts at the reached period", 2, from.periodIndex)
        assertEquals("the year still rolls from the last landed stamp", 5, from.year)
        val next = periodTrackPlan(AgeStampStyle.BIG_STAMP, from, mark(2, 2), "2번째 해")!!
        assertEquals("no second travel", 0f, next.travelMs, 0f)
        assertEquals(2 to 2, next.fromIndex to next.toIndex)
        // Without an arrival nothing changes, and another run's arrival never applies.
        assertEquals(mark(1, 5), LifeClockMemory(seed = mark(1, 5)).travelFrom(mark(1, 5)))
        val other = LifeClockMemory(seed = mark(1, 5)).apply { arrive(mark(3, 1, run = "other")) }
        assertEquals(1, other.travelFrom(mark(1, 5)).periodIndex)
        // The marker never moves back: an older arrival does not lower it.
        memory.arrive(mark(1, 4))
        assertEquals(2, memory.travelFrom(mark(1, 5)).periodIndex)
    }

    /** How strongly the stamp shows at [t]: the big stamp's label opacity times its most inked glyph. */
    private fun visible(plan: PeriodTrackPlan, t: Float, yieldAt: Float = -1f): Float {
        val label = plan.stampAlphaAt(t, yieldAt)
        if (plan.style != AgeStampStyle.BIG_STAMP) return label
        return label * (0 until plan.glyphCount).maxOf { StampSpec.alphaAt(plan.glyphAt(it, t, yieldAt)) }
    }

    private fun plans(): List<PeriodTrackPlan> = listOf(AgeStampStyle.SEAL, AgeStampStyle.BIG_STAMP, AgeStampStyle.ROLL).flatMap { style ->
        listOf(
            periodTrackPlan(style, mark(1, 2), mark(1, 3), "3번째 해")!!,
            periodTrackPlan(style, mark(1, 5), mark(2, 1), "1번째 해")!!,
            periodTrackPlan(style, mark(1, 9), mark(1, 10), "10번째 해")!!,
        )
    }

    @Test fun everyStampStartsHiddenLandsBeforeItEndsAndRestsExactlyAtTheEnd() {
        for (plan in plans()) {
            val name = "${plan.style} ${plan.fromIndex}→${plan.toIndex}"
            assertTrue("$name lands before it ends", plan.commitMs in plan.stampStartMs..plan.totalMs)
            assertEquals("$name: the old place first", if (plan.travelMs > 0f) 0f else 1f, plan.travelAt(0f, -1f), 0f)
            assertEquals("$name: nothing stamped at the start", 0f, visible(plan, 0f), 0f)
            assertTrue("$name: something is stamped after landing", visible(plan, plan.commitMs + 1f) > 0.5f)
            // The resting state: marker in place, no hop, no ring, no stamp.
            for (t in listOf(plan.totalMs, plan.totalMs + 500f)) {
                assertEquals(name, 1f, plan.travelAt(t, -1f), 0f)
                assertEquals(name, 0f, plan.hopAt(t, -1f), 0f)
                assertEquals(name, 0f, plan.stampAlphaAt(t, -1f), 0f)
                assertEquals(name, -1f, plan.markerRingAt(t, -1f), 0f)
                assertEquals(name, -1f, plan.sealRingAt(t, -1f), 0f)
            }
            assertEquals(name, plan.totalMs, plan.endMs(-1f), 0f)
            assertTrue("$name stays short (≈2 s)", plan.totalMs < 2_600f)
        }
    }

    @Test fun theStampWaitsForTheMarkerToArriveButTheRollTagRidesAlong() {
        val seal = periodTrackPlan(AgeStampStyle.SEAL, mark(1, 5), mark(2, 1), "1번째 해")!!
        val big = periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(1, 5), mark(2, 1), "1번째 해")!!
        val roll = periodTrackPlan(AgeStampStyle.ROLL, mark(1, 5), mark(2, 1), "1번째 해")!!
        assertEquals(0f, seal.stampAlphaAt(300f, -1f), 0f)
        assertEquals(0f, big.stampAlphaAt(300f, -1f), 0f)
        assertEquals("the tag shows the old year while it travels", 1f, roll.stampAlphaAt(300f, -1f), 0f)
        assertEquals(560f, big.stampStartMs, 0f)
        assertEquals(1f, big.travelAt(big.stampStartMs, -1f), 0f)
    }

    @Test fun theBigStampLandsWhenItsLastGlyphLandsAndThenSinksIntoTheMarker() {
        val plan = periodTrackPlan(AgeStampStyle.BIG_STAMP, mark(1, 2), mark(1, 3), "3번째 해")!!
        val lastStart = plan.stampStartMs + 60f + 3 * 90f
        assertEquals(0f, plan.glyphAt(3, lastStart), 0f)
        assertTrue("the last glyph has landed at the commit", plan.glyphAt(3, plan.commitMs) >= 1f)
        assertTrue("…and not a millisecond earlier", plan.glyphAt(3, plan.commitMs - 1f) < 1f)
        assertTrue("the first glyph is down long before", plan.glyphAt(0, plan.commitMs) > 0.9f)
        assertEquals(MotionHaptic.CONTEXT, plan.beat)
        assertEquals(0f, plan.underlineAt(plan.commitMs), 0f)
        assertEquals(1f, plan.underlineAt(plan.commitMs + 300f), 0f)
        assertEquals("still in place while it holds", 0f, plan.absorbAt(plan.commitMs + 300f + 420f, -1f), 0f)
        assertEquals(1f, plan.absorbAt(plan.commitMs + 720f + 340f, -1f), 0f)
        assertTrue("the marker hops when it arrives", plan.hopAt(plan.commitMs + 720f + 340f + 1f, -1f) > 0.35f)
        assertTrue(plan.markerRingAt(plan.commitMs + 720f + 340f + 1f, -1f) >= 0f)
    }

    @Test fun theSealThumpsAndTheRollTurnsOverBeforeTheyLand() {
        val seal = periodTrackPlan(AgeStampStyle.SEAL, mark(1, 2), mark(1, 3), "3번째 해")!!
        assertTrue("dropped big and tilted", seal.sealScaleAt(0f) > 1.6f && seal.sealTiltAt(0f) < -13f)
        assertTrue("within 2% of rest at the impact", seal.sealScaleAt(seal.commitMs) <= 1.0201f)
        assertTrue("…and not a millisecond earlier", seal.sealScaleAt(seal.commitMs - 1f) > 1.0199f)
        assertTrue("the ink ring starts at the impact", seal.sealRingAt(seal.commitMs, -1f) >= 0f)
        assertTrue(seal.hopAt(seal.commitMs + 1f, -1f) > 0.3f)
        assertEquals(0f, seal.sealLiftAt(seal.commitMs + 719f), 0f)
        assertEquals(MotionHaptic.CONTEXT, seal.beat)
        val roll = periodTrackPlan(AgeStampStyle.ROLL, mark(1, 2), mark(1, 3), "3번째 해")!!
        assertEquals(0f, roll.rollAt(roll.stampStartMs), 0f)
        assertEquals("rolled at the commit", 1f, roll.rollAt(roll.commitMs), 0.02f)
        assertEquals(1f, roll.tagLowAt(0f), 0f)
        assertEquals(0f, roll.tagLowAt(160f), 0f)
        assertEquals(MotionHaptic.TICK, roll.beat)
        assertNull("nothing turns over, nothing to feel", periodTrackPlan(AgeStampStyle.ROLL, mark(1, 1), mark(2, 1), "1번째 해")!!.beat)
    }

    @Test fun aYieldingStampAbsorbsOrFadesWithinTheYieldTime() {
        for (plan in plans()) {
            for (yieldAt in listOf(0f, 100f, plan.stampStartMs + 50f, plan.commitMs - 10f, plan.commitMs + 200f, plan.totalMs - 100f)) {
                val name = "${plan.style} ${plan.fromIndex}→${plan.toIndex} yield@$yieldAt"
                val end = plan.endMs(yieldAt)
                assertTrue(name, end <= yieldAt + MotionTimeline.STAMP_YIELD_MS + 1e-3f)
                assertTrue("$name commits by the end", plan.commitAtMs(yieldAt) <= end)
                assertEquals("$name: nothing left at the end", 0f, plan.stampAlphaAt(end, yieldAt), 0f)
                assertEquals("$name: the marker has arrived by the end", 1f, plan.travelAt(end, yieldAt), 0f)
                assertEquals("$name: nothing left at the end", 0f, visible(plan, end, yieldAt), 0f)
                // The seal and the tag only fade; a big-stamp glyph already falling may still ink in while it absorbs.
                if (plan.style != AgeStampStyle.BIG_STAMP) {
                    assertTrue("$name never brightens while yielding", visible(plan, yieldAt + 150f, yieldAt) <= visible(plan, yieldAt) + 1e-4f)
                }
            }
            // Yielding from the first frame on: nothing ever shows ("no new stamp starts").
            var t = 0f
            while (t <= plan.endMs(0f)) {
                assertEquals("${plan.style} ${plan.fromIndex}→${plan.toIndex} yield@0 t=$t", 0f, visible(plan, t, 0f), 0f)
                t += 10f
            }
        }
    }

    // ---- the spring ----

    @Test fun springIsTheDampedOscillatorComposeUses() {
        assertEquals(0f, lifeClockSpring(0f, 0.48f, 400f), 0f)
        assertEquals(0f, lifeClockSpring(-20f, 0.48f, 400f), 0f)
        assertEquals(1f, lifeClockSpring(3_000f, 0.48f, 400f), 1e-3f)
        val peak = (1..600).maxOf { lifeClockSpring(it.toFloat(), 0.48f, 400f) }
        val expected = 1f + exp(-0.48f * PI.toFloat() / sqrt(1f - 0.48f * 0.48f))
        assertEquals("the StampSpec press overshoots by e^(−ζπ/√(1−ζ²))", expected, peak, 2e-3f)
        assertTrue((1..600).all { lifeClockSpring(it.toFloat(), 1f, 400f) <= 1f + 1e-5f })
        val over = (1..600).map { lifeClockSpring(it.toFloat(), 1.4f, 400f) }
        assertTrue("overdamped rises without passing 1", over.zipWithNext().all { (a, b) -> b >= a - 1e-6f } && over.last() <= 1f)
        assertEquals(118f, lifeClockSpringReach(1f, StampSpec.DAMPING, StampSpec.STIFFNESS), 3f)
    }
}
