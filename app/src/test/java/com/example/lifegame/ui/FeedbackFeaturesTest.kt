package com.example.lifegame.ui

import com.example.lifegame.data.FaceSheetResponse
import com.example.lifegame.data.Multipart
import com.example.lifegame.data.MultipartPart
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatDelta
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-05 피드백: 해마다 이야기(4번) · 주사위 시작 능력치(1번) · AI 얼굴 시트 응답(2번). */
class FeedbackFeaturesTest {

    private val stories = YearlyStoryBook.parse(File("src/main/assets/yearly_stories.sample.json").readText())

    private fun period(p: LifePeriod, name: String, order: Int) =
        PeriodDefinition(period = p, displayName = name, order = order, years = 5)

    @Test
    fun everyPeriodYearHasAStoryAndTheChoiceIsStablePerRun() {
        LifePeriod.entries.forEach { p ->
            (1..5).forEach { year ->
                val line = stories.lineFor(42L, p, year)
                assertNotNull("$p $year", line)
                assertEquals(line, stories.lineFor(42L, p, year))
            }
        }
        // 다른 인생에서는 다른 문장이 나올 수 있다(후보가 둘 이상).
        val variety = (0L until 20L).map { stories.lineFor(it, LifePeriod.CHILD, 1) }.toSet()
        assertTrue(variety.size > 1)
    }

    @Test
    fun storyLinesComeFirstInEachYearAndReplaceTheEngineFillerLine() {
        val periods = listOf(period(LifePeriod.INFANT, "영아기", 0), period(LifePeriod.CHILD, "유년기", 1))
        val moments = listOf(
            LifeMoment(LifePeriod.INFANT, 1, "영아기 1번째 해, 세상을 처음 배웠다."),
            LifeMoment(LifePeriod.INFANT, 1, "엄마 품에서 잠들었다."),
            LifeMoment(LifePeriod.CHILD, 1, "친구와 놀이터에 갔다."),
        )
        val lines = buildLogLines(moments, periods, stories, runSeed = 7L)
        assertEquals(stories.lineFor(7L, LifePeriod.INFANT, 1), lines[0].text)
        assertEquals("엄마 품에서 잠들었다.", lines[1].text)
        assertEquals(stories.lineFor(7L, LifePeriod.CHILD, 1), lines[2].text)
        assertEquals("친구와 놀이터에 갔다.", lines[3].text)
        assertFalse(lines.any { it.text.contains("세상을 처음 배웠다") })
        assertEquals("유년기 · 1번째 해", lines[3].label)
    }

    @Test
    fun withoutStoriesTheLogIsUnchanged() {
        val periods = listOf(period(LifePeriod.INFANT, "영아기", 0))
        val moments = listOf(LifeMoment(LifePeriod.INFANT, 1, "영아기 1번째 해, 세상을 처음 배웠다."))
        val lines = buildLogLines(moments, periods)
        assertEquals(listOf("영아기 1번째 해, 세상을 처음 배웠다."), lines.map { it.text })
    }

    @Test
    fun diceRollsStayInRangeAndSurviveEncoding() {
        val random = Random(2026)
        repeat(200) {
            val roll = StartingDice.roll(random)
            assertEquals(StartingDice.STAT_LABELS, roll.rolls.map { it.label })
            roll.rolls.forEach { assertTrue(it.delta in -10..10) }
            assertEquals(roll, StartingRoll.decode(roll.encode()))
            assertEquals(0, roll.delta.health)
        }
        assertNull(StartingRoll.decode("1,2,3"))
        assertNull(StartingRoll.decode("0,0,0,0,0,0,0,0,0,0,0,0"))
    }

    @Test
    fun diceSumAndFinalStatUseTheSameRawEyesShownToPlayer() {
        val roll = StatRoll("지력", 1, 3)
        assertEquals(4, roll.sum)
        assertEquals(-6, roll.delta)
        assertEquals(44, roll.finalValue)
    }

    @Test
    fun tendencyFollowsTheStrongestRolledStats() {
        val starters = listOf(
            StarterUi("starter_loud", "활발한 편", "", null, StatDelta(fitness = 5, social = 3, intelligence = -3), setOf("starter_loud")),
            StarterUi("starter_curious", "파고드는 편", "", null, StatDelta(intelligence = 5, luck = 3, fitness = -3), setOf("starter_curious")),
            StarterUi("starter_quirky", "엉뚱한 편", "", null, StatDelta(happiness = 5, luck = 3, social = -3), setOf("starter_quirky")),
        )
        assertEquals("starter_loud", StartingDice.tendencyOf(StatDelta(fitness = 10, social = 6), starters)?.starterId)
        assertEquals("starter_curious", StartingDice.tendencyOf(StatDelta(intelligence = 10, luck = 4), starters)?.starterId)
        assertEquals("starter_quirky", StartingDice.tendencyOf(StatDelta(happiness = 10, social = -6), starters)?.starterId)
        assertNull(StartingDice.tendencyOf(StatDelta(), emptyList()))
    }

    @Test
    fun appTextIsLargerButNeverPastTheSystemMaximum() {
        assertEquals(1.12f, com.example.lifegame.ui.theme.appFontScale(1f), 0.001f)
        assertEquals(1.68f, com.example.lifegame.ui.theme.appFontScale(1.5f), 0.001f)
        assertEquals(2f, com.example.lifegame.ui.theme.appFontScale(1.8f), 0.001f)
        assertEquals(2f, com.example.lifegame.ui.theme.appFontScale(2f), 0.001f)
    }

    @Test
    fun faceSheetResponseReadsTheImageOrAReadableError() {
        val ok = FaceSheetResponse.parse(200, """{"created":1,"data":[{"b64_json":"iVBORw0KGgo="}]}""")
        assertEquals(FaceSheetResponse.Image("iVBORw0KGgo="), ok)
        val empty = FaceSheetResponse.parse(200, """{"data":[]}""")
        assertTrue(empty is FaceSheetResponse.Error)
        val unauthorized = FaceSheetResponse.parse(401, """{"error":{"message":"Incorrect API key"}}""") as FaceSheetResponse.Error
        assertFalse(unauthorized.retryable)
        assertFalse(unauthorized.message.contains("API key"))
        val limited = FaceSheetResponse.parse(429, "") as FaceSheetResponse.Error
        assertTrue(limited.retryable)
        val unsafe = FaceSheetResponse.parse(400, """{"error":{"message":"Your request was rejected by the safety system."}}""") as FaceSheetResponse.Error
        assertTrue(unsafe.message.contains("다른 사진"))
    }

    @Test
    fun multipartBodyHasEveryFieldAndBothImages() {
        val body = Multipart.encode(
            "b",
            listOf(
                MultipartPart.Field("model", "gpt-image-2.5-flare"),
                MultipartPart.File("image[]", "photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3)),
                MultipartPart.File("image[]", "style_reference.png", "image/png", byteArrayOf(4)),
            ),
        ).toString(Charsets.ISO_8859_1)
        assertTrue(body.startsWith("--b\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\ngpt-image-2.5-flare\r\n"))
        assertEquals(2, Regex("""name="image\[\]"; filename=""").findAll(body).count())
        assertTrue(body.endsWith("--b--\r\n"))
    }
}
