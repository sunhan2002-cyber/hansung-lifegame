package com.example.lifegame.ui

import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LifeFactsTest {

    private val catalog = LifeFactCatalog(
        parseLifeFacts(
            """
            [
              {"flag":"university","fact":"educationLevel","value":"UNIVERSITY","label":"대학 진학"},
              {"flag":"edu_graduate","fact":"educationLevel","value":"GRADUATE","label":"대학원 진학"},
              {"flag":"career_researcher","fact":"careerTrack","value":"RESEARCHER","label":"연구원"},
              {"flag":"level_senior","fact":"careerLevel","value":"SENIOR","label":"책임자"},
              {"flag":"dating","fact":"maritalStatus","value":"DATING","label":"연애 중"},
              {"flag":"married","fact":"maritalStatus","value":"MARRIED","label":"결혼"},
              {"flag":"v4:event:adult_039","fact":"childrenAtLeast","value":1,"label":"첫 아이"},
              {"flag":"housing_jeonse","fact":"housingType","value":"JEONSE","label":"전세"},
              {"flag":"achv_breakthrough","fact":"achievement","value":"BREAKTHROUGH","label":"연구 돌파구"}
            ]
            """.trimIndent(),
        ),
    )
    private val periods = listOf(
        PeriodDefinition(LifePeriod.TEEN, order = 2, years = 5, displayName = "청소년기"),
        PeriodDefinition(LifePeriod.YOUNG_ADULT, order = 3, years = 5, displayName = "청년기"),
        PeriodDefinition(LifePeriod.OLD_AGE, order = 5, years = 5, displayName = "노년기"),
    )

    @Test
    fun `later flags overwrite earlier facts and achievements accumulate`() {
        val facts = buildLifeFacts(
            listOf("university", "dating", "edu_graduate", "career_researcher", "level_senior", "married",
                "v4:event:adult_039", "housing_jeonse", "achv_breakthrough", "unknown_flag"),
            Stats(wealth = 80),
            catalog,
        )
        assertEquals("대학원 진학", facts.education)
        assertEquals("결혼", facts.marital)
        assertEquals(1, facts.children)
        assertEquals("전세", facts.housing)
        assertEquals("넉넉함", facts.wealthBand)
        assertEquals(listOf("연구 돌파구"), facts.achievements)
        assertEquals("연구원(책임자)", facts.careerText())
        assertEquals("대학원 진학 · 연구원(책임자) · 결혼 · 자녀 1명 · 전세 · 경제력 넉넉함 · 업적 1개", facts.shortLine)
    }

    @Test
    fun `empty facts still show explicit default lines`() {
        val facts = buildLifeFacts(emptyList(), Stats(wealth = 10), catalog)
        val lines = facts.educationCareerLines() + facts.familyLifeLines()
        assertEquals(listOf("교육", "전공", "직업", "일", "관계", "자녀", "주거", "경제력", "건강"), lines.map { it.label })
        assertTrue(lines.none { it.value.isBlank() })
        assertEquals("특별한 진학 기록 없음", lines.first().value)
        assertFalse(lines.first().known)
        assertEquals("빠듯함", lines.first { it.label == "경제력" }.value)
    }

    @Test
    fun `fact changes list only what the choice added`() {
        val before = buildLifeFacts(listOf("university"), Stats(wealth = 40), catalog)
        val after = buildLifeFacts(listOf("university", "career_researcher", "married", "achv_breakthrough"), Stats(wealth = 60), catalog)
        assertEquals(
            listOf(
                FactChangeUi("직업", "연구원"),
                FactChangeUi("관계", "결혼"),
                FactChangeUi("경제력", "보통 → 여유"),
                FactChangeUi("업적", "연구 돌파구"),
            ),
            factChanges(before, after),
        )
    }

    private val examEvent = DecisionEvent(
        eventId = "teen_019",
        periods = setOf(LifePeriod.TEEN),
        kind = DecisionKind.MILESTONE,
        title = "고등학교 원서",
        text = "원서를 쓴다.",
        choices = listOf(
            DecisionOption("A", "일반고", listOf(Outcome("a", "일반고에 갔다.", chance = 100, addFlags = setOf("school_regular")))),
        ),
    )

    @Test
    fun `lock reasons name the real stat and flag conditions`() {
        val lookup = FlagLookup.from(listOf(examEvent), catalog)
        val option = DecisionOption(
            choiceId = "B",
            label = "대학원에 간다",
            outcomes = listOf(Outcome("b", "갔다.", chance = 100)),
            requiredFlags = setOf("university", "v4:event:teen_019", "school_regular", "mystery_flag"),
            blockedFlags = setOf("married"),
            statConditions = StatConditions(minIntelligence = 60, maxHappiness = 30),
        )
        val reasons = option.lockReasons(Stats(intelligence = 52, happiness = 45), setOf("married"), lookup)
        assertEquals(
            listOf(
                "지력 60 필요 (지금 52)",
                "행복 30 이하일 때만 (지금 45)",
                "대학 진학 필요",
                "「고등학교 원서」 이후 가능",
                "「고등학교 원서」에서 이어지는 선택 필요",
                "앞선 선택이 필요해요",
                "이미 '결혼' 상태라 고를 수 없음",
            ),
            reasons,
        )
        assertTrue(option.lockReasons(Stats(intelligence = 70, happiness = 10),
            setOf("university", "v4:event:teen_019", "school_regular", "mystery_flag"), lookup).isEmpty())
    }

    @Test
    fun `newly opened events come from flags added by this choice`() {
        val firstJob = DecisionEvent(
            eventId = "path_int_07",
            periods = setOf(LifePeriod.YOUNG_ADULT),
            kind = DecisionKind.MILESTONE,
            title = "첫 연구실",
            text = "",
            choices = examEvent.choices,
            requiredFlags = setOf("university"),
        )
        val past = firstJob.copy(eventId = "teen_old", periods = setOf(LifePeriod.TEEN), title = "지난 일")
        val opened = newlyOpenedEvents(
            before = setOf("a"),
            after = setOf("a", "university"),
            current = LifePeriod.YOUNG_ADULT,
            decisions = listOf(firstJob, past),
            periods = periods,
        )
        assertEquals(listOf("첫 연구실"), opened)
        assertTrue(newlyOpenedEvents(setOf("university"), setOf("university"), LifePeriod.TEEN, listOf(firstJob), periods).isEmpty())
    }

    @Test
    fun `early health death headlines the real period and cause`() {
        val state = PeriodRunState(
            runId = "r",
            runSeed = 1,
            period = LifePeriod.OLD_AGE,
            stats = Stats(health = 0),
            flags = setOf("university"),
            moments = listOf(LifeMoment(LifePeriod.TEEN, 3, "밤샘 공부 끝에 쓰러졌다.")),
            isEnding = true,
            isDead = true,
            endingPeriod = LifePeriod.TEEN,
        )
        val facts = buildLifeFacts(state.flags, state.stats, catalog)
        val ending = buildEndingSummary("재겸", state, periods, facts, endingTitle = "무난한 어른", endingSummary = "요약", choices = emptyList())
        assertEquals("청소년기 3번째 해에 생을 마쳤다", ending.headline)
        assertEquals("건강이 바닥남", ending.death?.cause)
        assertTrue(ending.death!!.early)
        assertNull("어린 사망에는 성취형 결말 설명을 붙이지 않는다", ending.endingSummary)
        assertTrue(ending.lifeLine.endsWith("청소년기에 마침"))
    }

    @Test
    fun `natural death uses the ending title and old age`() {
        val state = PeriodRunState(
            runId = "r",
            runSeed = 1,
            period = LifePeriod.OLD_AGE,
            stats = Stats(intelligence = 90),
            moments = listOf(LifeMoment(LifePeriod.OLD_AGE, 5, NATURAL_DEATH_TEXT)),
            isEnding = true,
            isDead = true,
            endingPeriod = LifePeriod.OLD_AGE,
        )
        val ending = buildEndingSummary("재겸", state, periods, buildLifeFacts(emptyList(), state.stats, catalog),
            endingTitle = "늦게 피었지만 오래 가는 개발자가 되었다", endingSummary = "설명", choices = emptyList())
        assertEquals("늦게 피었지만 오래 가는 개발자가 되었다", ending.headline)
        assertEquals("노환 (자연사)", ending.death?.cause)
        assertNull(ending.death?.detail)
        assertEquals("설명", ending.endingSummary)
        assertTrue(ending.pathLine.contains("지력 90"))
    }
}
