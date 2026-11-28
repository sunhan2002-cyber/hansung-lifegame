package com.example.lifegame.ui

import androidx.compose.ui.graphics.Color
import com.example.lifegame.ui.components.LobbyTimeStyle
import com.example.lifegame.ui.screens.LOBBY_BOOK_MS
import com.example.lifegame.ui.screens.LOBBY_MEDAL_MS
import com.example.lifegame.ui.screens.LOBBY_WIGGLE_MS
import com.example.lifegame.ui.screens.LobbyClock
import com.example.lifegame.ui.screens.LobbyLook
import com.example.lifegame.ui.screens.LobbyTimeBucket
import com.example.lifegame.ui.screens.lobbyBookCoverScale
import com.example.lifegame.ui.screens.lobbyFireflyBlink
import com.example.lifegame.ui.screens.lobbyLookAt
import com.example.lifegame.ui.screens.lobbyMedalSwingDegrees
import com.example.lifegame.ui.screens.lobbyNormalizedHour
import com.example.lifegame.ui.screens.lobbyStarTwinkle
import com.example.lifegame.ui.screens.lobbyTimeBucket
import com.example.lifegame.ui.screens.lobbyWiggleDegrees
import com.example.lifegame.ui.screens.lobbyWigglePop
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시간대 로비(lobby2): 시각 → 로비 빛, 그리고 로비 아래 아이콘 움직임의 쉬는 자세. */
class LobbyTimeTest {
    private val allDay = (0..96).map { it / 4f }

    @Test fun theNeutralHourIsTodaysLobbyForEveryStyle() {
        LobbyTimeStyle.entries.forEach { style ->
            assertEquals("$style at 13 h", LobbyLook.Day, lobbyLookAt(LobbyClock.NEUTRAL_HOUR, style))
        }
        // Day keeps exactly the literals HomeSceneBackground drew before stage 2.
        with(LobbyLook.Day) {
            assertEquals(Color(0xFFFFDEA0), lightColor)
            assertEquals(0.15f, lightAlpha, 0f)
            assertEquals(0.82f, lightX, 0f)
            assertEquals(0.16f, lightY, 0f)
            assertEquals(0.65f, lightRadius, 0f)
            assertEquals(Color(0xFFFFE7B7), moteColor)
            assertEquals(1f, moteAlpha, 0f)
            assertEquals(1f, moteScale, 0f)
            assertFalse(hasGrade)
            assertFalse(hasParticles)
            assertEquals(LobbyTimeBucket.DAY, bucket)
        }
        // The whole midday plateau is today's lobby too.
        listOf(11f, 12f, 14.5f, 15.5f).forEach { assertEquals(LobbyLook.Day, lobbyLookAt(it, LobbyTimeStyle.LIVING)) }
    }

    @Test fun noneIsTodaysLobbyAtEveryHour() {
        allDay.forEach { assertEquals("NONE at $it h", LobbyLook.Day, lobbyLookAt(it, LobbyTimeStyle.NONE)) }
    }

    @Test fun bucketsFollowTheHour() {
        assertEquals(LobbyTimeBucket.MORNING, lobbyTimeBucket(7f))
        assertEquals(LobbyTimeBucket.DAY, lobbyTimeBucket(13f))
        assertEquals(LobbyTimeBucket.SUNSET, lobbyTimeBucket(18f))
        assertEquals(LobbyTimeBucket.NIGHT, lobbyTimeBucket(22f))
        assertEquals(LobbyTimeBucket.NIGHT, lobbyTimeBucket(3f))
        // Edges: MORNING [5, 11), DAY [11, 16.5), SUNSET [16.5, 19.5).
        assertEquals(LobbyTimeBucket.MORNING, lobbyTimeBucket(5f))
        assertEquals(LobbyTimeBucket.DAY, lobbyTimeBucket(11f))
        assertEquals(LobbyTimeBucket.SUNSET, lobbyTimeBucket(16.5f))
        assertEquals(LobbyTimeBucket.NIGHT, lobbyTimeBucket(19.5f))
        for (style in listOf(LobbyTimeStyle.GRADE, LobbyTimeStyle.LIVING)) {
            allDay.forEach { hour -> assertEquals("$style at $hour h", lobbyTimeBucket(hour), lobbyLookAt(hour, style).bucket) }
        }
    }

    @Test fun theDayBlendsWithoutJumpsAndMidnightComesRoundAgain() {
        for (style in listOf(LobbyTimeStyle.GRADE, LobbyTimeStyle.LIVING)) {
            assertEquals(lobbyLookAt(0f, style), lobbyLookAt(24f, style))
            var previous = lobbyLookAt(0f, style)
            for (minute in 1..24 * 60) {
                val hour = minute / 60f
                val look = lobbyLookAt(hour, style)
                val jump = numbers(look).zip(numbers(previous)).maxOf { (a, b) -> abs(a - b) }
                assertTrue("$style jumps by $jump at $hour h", jump < 0.03f)
                previous = look
            }
        }
    }

    @Test fun gradeNeverDrawsParticlesAndLivingOnlyWhenItIsDarkOrEarly() {
        allDay.forEach { assertFalse("GRADE at $it h", lobbyLookAt(it, LobbyTimeStyle.GRADE).hasParticles) }
        val night = lobbyLookAt(22f, LobbyTimeStyle.LIVING)
        assertTrue(night.stars > 0.9f && night.fireflies > 0.9f)
        assertEquals(0f, night.shafts, 0f)
        val morning = lobbyLookAt(8f, LobbyTimeStyle.LIVING)
        assertTrue(morning.shafts > 0.9f)
        assertEquals(0f, morning.stars, 0f)
        assertFalse(lobbyLookAt(13f, LobbyTimeStyle.LIVING).hasParticles)
        // GRADE is LIVING without the particles: the same light at every hour.
        allDay.forEach { hour ->
            assertEquals(lobbyLookAt(hour, LobbyTimeStyle.LIVING).copy(stars = 0f, fireflies = 0f, shafts = 0f), lobbyLookAt(hour, LobbyTimeStyle.GRADE))
        }
    }

    @Test fun theLightMovesLikeTheSun() {
        val morning = lobbyLookAt(8f, LobbyTimeStyle.GRADE)
        val sunset = lobbyLookAt(17.6f, LobbyTimeStyle.GRADE)
        val night = lobbyLookAt(22f, LobbyTimeStyle.GRADE)
        assertTrue("morning light comes from the left", morning.lightX < 0.3f)
        assertTrue("the sunset sits low on the right", sunset.lightX > 0.85f && sunset.lightY > LobbyLook.Day.lightY)
        assertTrue("sunset is warmer and stronger than day", sunset.lightAlpha > LobbyLook.Day.lightAlpha)
        assertTrue("night is graded navy", night.hasGrade && night.gradeTop.alpha > night.gradeBottom.alpha)
        assertEquals("no motes at night", 0f, night.moteAlpha, 0f)
        assertTrue("sunset motes are embers", sunset.moteScale > 1f)
    }

    @Test fun twinkleAndBlinkStayInRangeAndLoopSeamlessly() {
        for (index in 0 until 22) {
            assertEquals(lobbyStarTwinkle(index, 0f), lobbyStarTwinkle(index, 1f), 1e-4f)
            (0..100).forEach { step -> assertTrue(lobbyStarTwinkle(index, step / 100f) in 0.099f..1.001f) }
        }
        for (index in 0 until 9) {
            assertEquals(lobbyFireflyBlink(index, 0f), lobbyFireflyBlink(index, 1f), 1e-4f)
            (0..100).forEach { step -> assertTrue(lobbyFireflyBlink(index, step / 100f) in 0.099f..1.001f) }
        }
    }

    @Test fun hoursFoldIntoOneDay() {
        assertEquals(0f, lobbyNormalizedHour(24f), 0f)
        assertEquals(23f, lobbyNormalizedHour(-1f), 1e-5f)
        assertEquals(1.5f, lobbyNormalizedHour(25.5f), 1e-5f)
        assertEquals(LobbyClock.NEUTRAL_HOUR, lobbyNormalizedHour(Float.NaN), 0f)
        assertEquals(18.5f, LobbyClock.fixed(18.5f).hourOfDay(), 0f)
    }

    @Test fun footerIconMotionsStartAndEndExactlyAtRest() {
        // The resting icon is the static icon: every press motion is exactly at rest at both ends.
        assertEquals(0f, lobbyWiggleDegrees(0f), 0f)
        assertEquals(0f, lobbyWiggleDegrees(LOBBY_WIGGLE_MS.toFloat()), 0f)
        assertEquals(1f, lobbyWigglePop(0f), 0f)
        assertEquals(1f, lobbyWigglePop(LOBBY_WIGGLE_MS.toFloat()), 0f)
        assertEquals(1f, lobbyBookCoverScale(0f), 0f)
        assertEquals(1f, lobbyBookCoverScale(LOBBY_BOOK_MS.toFloat()), 0f)
        assertEquals(0f, lobbyMedalSwingDegrees(0f), 0f)
        assertEquals(0f, lobbyMedalSwingDegrees(LOBBY_MEDAL_MS.toFloat()), 0f)
        // ...and really moves in between: the book flaps open past its spine, the medal swings, the wiggle wobbles.
        assertEquals(-0.55f, lobbyBookCoverScale(200f), 1e-5f)
        assertTrue((0..LOBBY_MEDAL_MS).maxOf { abs(lobbyMedalSwingDegrees(it.toFloat())) } > 10f)
        assertTrue((0..LOBBY_WIGGLE_MS).maxOf { abs(lobbyWiggleDegrees(it.toFloat())) } > 6f)
        assertTrue((0..LOBBY_MEDAL_MS).all { abs(lobbyMedalSwingDegrees(it.toFloat())) <= 22f })
    }

    /** The look's numbers, colours premultiplied (a clear colour may change hue without being seen). */
    private fun numbers(look: LobbyLook): List<Float> = buildList {
        add(look.lightAlpha); add(look.lightX); add(look.lightY); add(look.lightRadius)
        add(look.moteAlpha); add(look.moteScale); add(look.stars); add(look.fireflies); add(look.shafts)
        addAll(channels(look.lightColor, 1f))
        addAll(channels(look.gradeTop, look.gradeTop.alpha))
        addAll(channels(look.gradeBottom, look.gradeBottom.alpha))
        addAll(channels(look.moteColor, look.moteAlpha))
    }

    private fun channels(color: Color, weight: Float) = listOf(color.red * weight, color.green * weight, color.blue * weight)
}
