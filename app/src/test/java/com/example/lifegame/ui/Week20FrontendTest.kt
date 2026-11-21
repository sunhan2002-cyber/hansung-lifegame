package com.example.lifegame.ui

import com.example.lifegame.data.AutomaticEventParser
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.AutomaticEventRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.ui.screens.yearHeader
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2026-11-20 과제(김재겸 프론트): 반복 문구 정리, 연애 · 결혼식의 같은 상대 표시. 실제 콘텐츠로 확인한다. */
class Week20FrontendTest {
    private fun asset(name: String) = File("src/main/assets/$name").readText()
    private val periods = V4ContentParser.parsePeriods(asset("periods.sample.json"))
    private val decisions = V4ContentParser.parseDecisionEvents(asset("events.sample.json")).associateBy { it.eventId }
    private val autoRules = AutomaticEventParser.parse(asset("auto_events.sample.json")).associateBy { it.event.eventId }

    private fun record(eventId: String, choiceId: String, outcomeId: String, period: LifePeriod = LifePeriod.YOUNG_ADULT, year: Int = 2) =
        RunChoiceRecord(eventId, choiceId, outcomeId, period, year, decisions.getValue(eventId).title, "고른 것.", "결과")

    // ---- 반복 문구: 기록의 해 표시는 해가 바뀔 때 한 번만 ----

    @Test
    fun `year headers appear once per year and drop the current period name`() {
        assertEquals("2번째 해", yearHeader("영아기 · 2번째 해", null, "영아기"))
        assertNull("같은 해는 다시 쓰지 않는다", yearHeader("영아기 · 2번째 해", "영아기 · 2번째 해", "영아기"))
        assertEquals("1번째 해", yearHeader("영아기 · 1번째 해", "영아기 · 2번째 해", "영아기"))
        assertEquals("지난 시기는 시기 이름을 남긴다", "영아기 5번째 해", yearHeader("영아기 · 5번째 해", "유년기 · 1번째 해", "유년기"))
        assertNull(yearHeader("", null, "영아기"))
    }

    // ---- 연애 · 결혼식: 같은 상대 ----

    @Test
    fun `the partner is named after the real event that formed the relationship`() {
        val met = record("adult_075", "A", "a_spark", year = 1)
        val flags = setOf("dating", "partner_music", "met_music_campfire")
        val partner = partnerOf(flags, listOf(met), emptyList(), decisions, autoRules, periods)
        assertNotNull(partner)
        assertEquals("partner_music", partner!!.flag)
        assertEquals("「모닥불 옆자리」에서 만난 사람", partner.label)
        assertEquals("청년기", partner.originPeriodName)
        assertEquals(1, partner.originYear)
        assertEquals("교제 중", partner.status)
        // 상대 플래그를 붙이지 않은 결과(친구로 남음)는 상대가 아니다.
        assertNull(partnerOf(setOf("lifelong_friend"), listOf(record("adult_075", "A", "a_chord")), emptyList(), decisions, autoRules, periods))
        // 기록 없이 플래그만 있으면(예: 옛 저장) 이름을 지어내지 않는다.
        assertNull(partnerOf(flags, emptyList(), emptyList(), decisions, autoRules, periods))
    }

    @Test
    fun `a partner that started in an automatic event is named after it`() {
        val auto = AutomaticEventRecord("auto_ya_love_start", "t", LifePeriod.YOUNG_ADULT, 3, "text", StatDelta())
        val partner = partnerOf(setOf("dating", "partner_second_chance"), emptyList(), listOf(auto), decisions, autoRules, periods)
        assertEquals("「연애 시작」에서 만난 사람", partner?.label)
    }

    @Test
    fun `the life record keeps the same partner, the love events and only a real wedding`() {
        val choices = listOf(
            record("adult_075", "B", "b_song", year = 1),
            record("love_date_01", "A", "a_steady", year = 2),
            record("love_marriage_talk_01", "A", "a_yes", year = 3),
            record("love_family_meet_01", "B", "b_team", year = 4),
            record("love_wedding_music", "A", "a_sing", year = 5),
        )
        val married = setOf("dating", "partner_music", "rel_steady", "engaged", "families_met", "married")
        val story = partnerStoryOf(married, choices, emptyList(), decisions, autoRules, periods)
        assertNotNull(story)
        assertEquals("「모닥불 옆자리」에서 만난 사람", story!!.partner?.label)
        assertEquals("결혼", story.status)
        assertEquals(listOf("love_date_01", "love_marriage_talk_01", "love_family_meet_01"), story.moments.map { it.eventId })
        assertEquals("love_wedding_music", story.wedding?.eventId)
        assertEquals("고른 것", story.wedding?.choiceLabel)

        // 결혼하지 않았으면 결혼식은 없다(약혼에서 멈춘 인생).
        val engaged = partnerStoryOf(married - "married", choices.dropLast(1), emptyList(), decisions, autoRules, periods)
        assertNull(engaged?.wedding)
        assertEquals("약혼", engaged?.status)

        // 친구 결혼식(하객)은 결혼식으로 세지 않는다.
        assertTrue(!PartnerStory.isWeddingEvent("adult_073"))
        // 연애 기록이 하나도 없으면 인연 칸을 그리지 않는다.
        assertNull(partnerStoryOf(emptySet(), listOf(record("adult_073", "B", "b_solo")), emptyList(), decisions, autoRules, periods))
    }

    @Test
    fun `korean particles for the dice speech follow the number`() {
        assertEquals(listOf("과", "와", "과", "와", "와", "과"), (1..6).map { StartingStatRowUi.andParticle(it) })
    }
}
