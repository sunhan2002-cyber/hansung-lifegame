package com.example.lifegame.data

import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.model.AutomaticEventRecord
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** 신기훈 auto_events.sample.json(schemaVersion 2)이 앱 모델로 읽히고, 조건·진행 단계·중복 방지가 지켜지는지. */
class AutomaticEventContentTest {
    private val catalog = AutomaticEventCatalog.parse(File("src/main/assets/auto_events.sample.json").readText())
    private val ids get() = catalog.events.map { it.event.eventId }

    private fun state(
        period: LifePeriod,
        year: Int,
        flags: Set<String> = emptySet(),
        children: Int = 0,
        moments: Int = 0,
        records: List<String> = emptyList(),
    ) = PeriodRunState(
        runId = "auto-test",
        runSeed = 7L,
        period = period,
        yearInPeriod = year,
        flags = flags,
        lifeFacts = LifeFacts(childrenCount = children),
        moments = List(moments) { LifeMoment(period, 1, "기록 $it") },
        automaticEventRecords = records.map { AutomaticEventRecord(it, "token-$it", period, 1, it) },
    )

    private fun candidateIds(s: PeriodRunState, years: Int = 5) = catalog.candidatesFor(s, years).map { it.eventId }

    @Test fun realFileParsesIntoEngineModelWithFiveOrMorePerPeriod() {
        assertEquals(49, catalog.events.size)
        assertEquals(ids.size, ids.toSet().size)
        val decisionIds = JSONArray(File("src/main/assets/events.sample.json").readText()).let { array ->
            List(array.length()) { array.getJSONObject(it).getString("eventId") }.toSet()
        }
        assertTrue("자동 사건 ID가 선택 사건 ID와 겹치지 않는다", ids.none { it in decisionIds })
        LifePeriod.values().forEach { period ->
            val inPeriod = catalog.events.filter { period in it.event.periods }
            val open = inPeriod.filter {
                it.requiredFlags.isEmpty() && it.blockedFlags.isEmpty() && it.lifeFactConditions.minChildrenCount == null
            }
            assertTrue("$period 자동 사건 5개 이상", inPeriod.size >= 5)
            assertTrue("$period 조건 없는 자동 사건 5개 이상", open.size >= 5)
        }
        catalog.events.forEach { content ->
            assertEquals("${content.event.eventId} statEffect와 statDelta", content.appliesStats, content.event.statDelta != StatDelta())
            assertTrue(content.dedupeKey.isNotBlank())
            assertFalse(content.event.repeatable)
        }
    }

    @Test fun jobAndChildEventsOnlyOpenForMatchingLives() {
        val unemployed = state(LifePeriod.YOUNG_ADULT, 2)
        assertFalse("auto_ya_payday" in candidateIds(unemployed))
        assertTrue("auto_ya_payday" in candidateIds(unemployed.copy(flags = setOf("job_employed"))))
        val middle = state(LifePeriod.MIDDLE, 2)
        assertFalse("auto_mid_child_school" in candidateIds(middle))
        assertTrue("auto_mid_child_school" in candidateIds(middle.copy(lifeFacts = LifeFacts(childrenCount = 1))))
        assertFalse("auto_mid_anniversary" in candidateIds(middle))
        assertTrue("auto_mid_anniversary" in candidateIds(middle.copy(flags = setOf("married"))))
    }

    @Test fun progressStageAndElapsedTimeAreNotYearlyCounters() {
        assertTrue("auto_child_allowance" in candidateIds(state(LifePeriod.CHILD, 1)))
        assertFalse("auto_child_allowance" in candidateIds(state(LifePeriod.CHILD, 2)))
        assertFalse("auto_infant_cold" in candidateIds(state(LifePeriod.INFANT, 1, moments = 0)))
        assertTrue("auto_infant_cold" in candidateIds(state(LifePeriod.INFANT, 1, moments = 1)))
        assertFalse("auto_old_album" in candidateIds(state(LifePeriod.OLD_AGE, 4)))
        assertTrue("auto_old_album" in candidateIds(state(LifePeriod.OLD_AGE, 5)))
        val dol = setOf("music_toy", "v4:event:infant_007")
        assertTrue("auto_infant_dol_photo_music" in candidateIds(state(LifePeriod.INFANT, 2, dol)))
        assertFalse("auto_infant_dol_photo_music" in candidateIds(state(LifePeriod.INFANT, 2, setOf("music_toy"))))
    }

    @Test fun dedupeKeyPeriodCapAndPendingDecisionAreRespected() {
        val both = setOf("music_toy", "cash_grabber", "v4:event:infant_007")
        val afterMusic = state(LifePeriod.INFANT, 2, both, records = listOf("auto_infant_dol_photo_music"))
        assertFalse("같은 dedupeKey 묶음은 하나만", "auto_infant_dol_photo_cash" in candidateIds(afterMusic))
        val capped = state(LifePeriod.CHILD, 3, moments = 9, records = listOf("auto_child_errand", "auto_child_friend_fight"))
        assertTrue("유년 시기 상한 2건", candidateIds(capped).isEmpty())
        assertTrue(candidateIds(state(LifePeriod.CHILD, 3, moments = 9).copy(pendingDecisionId = "child_003")).isEmpty())
    }

    @Test fun engineAppliesOnlyEligibleCandidatesFromTheRealFile() {
        val engine = GameEngine()
        var s = state(LifePeriod.YOUNG_ADULT, 4, moments = 9)
        repeat(5) { step ->
            val candidates = catalog.candidatesFor(s, 5)
            val applied = engine.applyAutomaticEvent(s, candidates, "token-$step") ?: return@repeat
            val record = requireNotNull(applied.automaticEvent)
            assertFalse("무직 인생에 월급 사건이 붙지 않는다", record.eventId == "auto_ya_payday")
            assertFalse("연인이 없으면 100일 사건이 붙지 않는다", record.eventId == "auto_ya_hundred_days")
            s = applied.state
        }
        assertTrue("청년 시기 상한 2건", s.automaticEventRecords.size <= 2)
    }
}
