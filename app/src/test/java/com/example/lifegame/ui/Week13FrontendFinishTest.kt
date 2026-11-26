package com.example.lifegame.ui

import com.example.lifegame.data.AutomaticEventParser
import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.engine.AutomaticEventPicker
import com.example.lifegame.domain.engine.AutomaticEventRule
import com.example.lifegame.domain.engine.AutomaticEventStage
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.engine.PeriodGameSession
import com.example.lifegame.domain.model.AutomaticEventRecord
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import java.io.File
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 2026-11-16 프론트엔드 마무리 과제: 자동 사건 화면 연결(과제 2)과 확장 UI의 실제 데이터 연결(과제 3).
 * 앱과 같은 파서 · 세션 · 콘텐츠 파일로 검증한다.
 */
class Week13FrontendFinishTest {
    private val assets = File("src/main/assets")
    private fun json(name: String) = File(assets, name).readText()
    private val periods = V4ContentParser.parsePeriods(json("periods.sample.json"))
    private val focuses = V4ContentParser.parseFocuses(json("focuses.sample.json"))
    private val decisions = V4ContentParser.parseDecisionEvents(json("events.sample.json"))
    private val factRules = V4ContentParser.parseLifeFactRules(json("life_facts.sample.json"))
    private val endings = V4ContentParser.parseEndings(json("endings.sample.json"))
    private val rules = AutomaticEventParser.parse(json("auto_events.sample.json"))
    private val ruleById = rules.associateBy { it.event.eventId }
    private val extensions = ScenarioExtensions.parse(json("scenario_extensions.sample.json"))

    private fun session(seed: Long, withAutoEvents: Boolean = true) = PeriodGameSession(
        periods, focuses, decisions, endings, factRules,
        engine = GameEngine(skipInfantFocus = true, guaranteeYearlyIncident = true),
        runIdFactory = { "finish-$seed" },
        runSeedFactory = { seed },
        automaticEventPicker = if (withAutoEvents) AutomaticEventPicker(rules, periods) else null,
    )

    private data class Trace(val before: PeriodRunState?, val step: TurnStep)

    /** 앱처럼 장 표지 → 초점, 결정 → 선택, 자동 진행 → 다음 해를 끝까지 반복한다. 선택은 시드로 고른다. */
    private fun play(seed: Long, withAutoEvents: Boolean = true): List<Trace> {
        val random = Random(seed)
        val s = session(seed, withAutoEvents)
        val trace = mutableListOf(Trace(null, s.start()))
        var guard = 0
        while (trace.last().step.phase != TurnPhase.ENDING && guard++ < 600) {
            val step = trace.last().step
            val before = step.state
            val next = when (step.phase) {
                TurnPhase.CHAPTER -> s.selectFocus(focuses.filter { it.period == before.period }.random(random).focusId)
                TurnPhase.DECISION -> s.choose(step.pendingDecision!!.choices.filter { it.available }.random(random).choiceId).step
                TurnPhase.AUTO_PROGRESS -> s.advance()
                TurnPhase.ENDING -> break
            }
            trace += Trace(before, next)
        }
        assertEquals("seed $seed did not reach an ending", TurnPhase.ENDING, trace.last().step.phase)
        return trace
    }

    // ---- 과제 2: 자동 사건 ----

    @Test
    fun `all authored automatic events are read with their stage, conditions and record-only effect`() {
        assertEquals(49, rules.size)
        assertEquals(LifePeriod.entries.toSet(), rules.flatMap { it.event.periods }.toSet())
        rules.filter { it.recordOnly }.forEach { assertEquals(it.event.eventId, StatDelta(), it.event.statDelta) }
        assertEquals(21, rules.count { it.recordOnly })
        assertTrue(rules.filterNot { it.recordOnly }.all { it.event.statDelta != StatDelta() })
        rules.filter { it.stage == AutomaticEventStage.AFTER_EVENT }.forEach { assertNotNull(it.afterEventId) }
        assertEquals(
            setOf("auto_infant_dol_photo_music", "auto_infant_dol_photo_gavel", "auto_infant_dol_photo_cash"),
            rules.filter { it.dedupeKey == "auto_infant_dol_photo" }.map { it.event.eventId }.toSet(),
        )
        assertEquals(StatDelta(health = -2), ruleById.getValue("auto_infant_cold").event.statDelta)
        assertEquals(1, ruleById.getValue("auto_mid_child_school").lifeFactConditions.minChildrenCount)
        assertEquals(setOf("dating", "partner_second_chance"), ruleById.getValue("auto_ya_love_start").event.addFlags)
    }

    @Test
    fun `automatic events never preempt a decision, never repeat a stage and keep their rules`() {
        val stagesSeen = mutableSetOf<AutomaticEventStage>()
        var total = 0
        (1L..60L).forEach { seed ->
            val trace = play(20261116L + seed)
            val autoSteps = trace.filter { it.step.automaticEvent != null }
            total += autoSteps.size
            autoSteps.forEach { (before, step) ->
                val record = step.automaticEvent!!
                val rule = ruleById.getValue(record.eventId)
                stagesSeen += rule.stage
                requireNotNull(before)
                // 결정 대기 · 결말 중에는 나오지 않고, 해를 넘기지 않는다(그해 결정은 다음 단계에서 그대로 나온다).
                assertNull(before.pendingDecisionId)
                assertFalse(before.isEnding)
                assertEquals(TurnPhase.AUTO_PROGRESS, step.phase)
                assertEquals(before.period, step.state.period)
                assertEquals(before.yearInPeriod, step.state.yearInPeriod)
                assertTrue(step.canAutoAdvance)
                // 조건은 적용 전 상태로 판정한다.
                assertTrue(rule.event.eventId, before.flags.containsAll(rule.requiredFlags))
                assertTrue(rule.event.eventId, rule.blockedFlags.none(before.flags::contains))
                assertTrue(rule.lifeFactConditions.matches(before.lifeFacts))
                // 능력치: 기록만 남기는 사건은 그대로, STAT 사건은 그 변화만. 자동 사건으로는 죽지 않는다.
                if (rule.recordOnly) assertEquals(before.stats, step.state.stats)
                else assertEquals(before.stats.plusClamped(rule.event.statDelta), step.state.stats)
                assertTrue(step.state.stats.health > 0)
                assertFalse(step.state.isDead)
                assertEquals(record.text, step.recordText)
            }
            val records = trace.last().step.state.automaticEventRecords
            assertEquals(records.size, autoSteps.size)
            assertEquals("same event twice in seed $seed", records.size, records.map { it.eventId }.distinct().size)
            assertEquals(records.size, records.map { it.triggerToken }.distinct().size)
            assertTrue(records.count { it.eventId.startsWith("auto_infant_dol_photo") } <= 1)
            records.groupBy { it.period }.forEach { (period, inPeriod) ->
                assertTrue("$period cap", inPeriod.size <= AutomaticEventPicker.periodLimit(period))
            }
            // Automatic events never replace a decision. Only the authored marriage sequence
            // can add another decision to the same slot.
            val decisionsPerYear = trace.filter { it.step.phase == TurnPhase.DECISION }
                .groupBy { it.step.state.period to it.step.state.yearInPeriod }
            val marriageFollowUps = mapOf(
                "adult_032" to setOf("love_family_meet_01", "path_soc_05"),
                "love_marriage_talk_01" to setOf("love_family_meet_01", "path_soc_05"),
                "love_family_meet_01" to setOf("love_wedding_prep_01"),
                "path_soc_05" to setOf("love_wedding_prep_01"),
                "love_wedding_prep_01" to setOf("love_wedding_01", "love_wedding_first_love", "love_wedding_music", "love_wedding_book"),
            )
            decisionsPerYear.values.forEach { decisions ->
                decisions.zipWithNext().forEach { (previous, next) ->
                    val from = previous.step.state.pendingDecisionId
                    val to = next.step.state.pendingDecisionId
                    assertEquals("seed $seed: $from → $to must be consecutive", from, next.before?.pendingDecisionId)
                    assertTrue("seed $seed: unexpected decision in the same slot: $from → $to", to in marriageFollowUps[from].orEmpty())
                }
            }
            val baseline = play(20261116L + seed, withAutoEvents = false)
                .filter { it.step.phase == TurnPhase.DECISION }
                .map { it.step.state.period to it.step.state.yearInPeriod }.toSet()
            val yearsWithAuto = decisionsPerYear.keys
            // 사망으로 일찍 끝난 인생이 아니면 자동 사건이 있어도 같은 해 수만큼 결정이 나온다.
            if (!trace.last().step.state.isDead && trace.last().step.state.automaticEventRecords.isNotEmpty()) {
                assertEquals(baseline.size, yearsWithAuto.size)
            }
        }
        assertTrue("자동 사건이 거의 나오지 않음: $total", total >= 60 * 6)
        assertEquals(AutomaticEventStage.entries.toSet(), stagesSeen)
    }

    @Test
    fun `the doljabi photo follows the actual doljabi choice right after it`() {
        val trace = play(20261116L + 3)
        val dolIndex = trace.indexOfFirst { it.before?.pendingDecisionId == "infant_007" }
        val choiceFlag = trace[dolIndex].step.state.flags.first { it in setOf("music_toy", "judge_gavel", "cash_grabber") }
        // 결과 화면 → 다음 단계가 돌잡이 사진 자동 사건이다.
        val next = trace[dolIndex + 1].step.automaticEvent
        assertNotNull(next)
        val expected = mapOf(
            "music_toy" to "auto_infant_dol_photo_music",
            "judge_gavel" to "auto_infant_dol_photo_gavel",
            "cash_grabber" to "auto_infant_dol_photo_cash",
        ).getValue(choiceFlag)
        assertEquals(expected, next!!.eventId)
        assertEquals(StatDelta(), next.appliedDelta)
    }

    @Test
    fun `restoring at an automatic event does not apply it again and the year's decision follows`() {
        val first = session(77L)
        var step = first.start()
        var guard = 0
        while (step.automaticEvent == null && guard++ < 200) {
            step = when (step.phase) {
                TurnPhase.CHAPTER -> first.selectFocus(focuses.first { it.period == step.state.period }.focusId)
                TurnPhase.DECISION -> first.choose(step.pendingDecision!!.choices.first { it.available }.choiceId).step
                else -> first.advance()
            }
        }
        val saved = step.state
        val record = step.automaticEvent!!
        assertEquals(AutomaticEventPicker.stageToken(saved), record.triggerToken)

        val restored = session(77L)
        val again = restored.restoreSaved(saved)
        assertEquals(TurnPhase.AUTO_PROGRESS, again.phase)
        assertEquals(saved.stats, again.state.stats)
        val next = restored.advance()
        assertNull(next.automaticEvent)
        assertEquals(TurnPhase.DECISION, next.phase)
        assertEquals(1, next.state.automaticEventRecords.count { it.eventId == record.eventId })
        // 같은 시드 · 같은 선택이면 자동 사건도 같다(결정적 추첨).
        assertEquals(play(4242L).mapNotNull { it.step.automaticEvent?.eventId }, play(4242L).mapNotNull { it.step.automaticEvent?.eventId })
    }

    @Test
    fun `the play screen shows an automatic event card with changes or record-only, and logs it as automatic`() {
        val trace = (1L..30L).flatMap { play(9000L + it) }
        val content = PlayContent(
            periods = periods,
            focuses = focuses,
            decisions = decisions.associateBy { it.eventId },
            imageFiles = emptyMap(),
            extensions = extensions,
            automaticEvents = ruleById,
        )
        val stat = trace.first { it.step.automaticEvent?.let { r -> !ruleById.getValue(r.eventId).recordOnly } == true }.step
        val statUi = buildPeriodPlayState(stat, content, "재겸").automaticEvent!!
        assertEquals(PlayPhase.AUTO, buildPeriodPlayState(stat, content, "재겸").phase)
        assertFalse(statUi.recordOnly)
        assertEquals(stat.automaticEvent!!.appliedDelta.toChanges(), statUi.changes)
        assertEquals(ruleById.getValue(stat.automaticEvent!!.eventId).topic, statUi.topic)

        val recordOnly = trace.first { it.step.automaticEvent?.let { r -> ruleById.getValue(r.eventId).recordOnly } == true }.step
        val recordUi = buildPeriodPlayState(recordOnly, content, "재겸")
        assertTrue(recordUi.automaticEvent!!.recordOnly)
        assertTrue(recordUi.automaticEvent!!.changes.isEmpty())
        val autoLine = recordUi.log.last { it.text == recordOnly.automaticEvent!!.text }
        assertEquals(LogKind.AUTO, autoLine.kind)
        // 선택 기록과 섞지 않는다: 자동 사건 문장은 "내 선택"으로 표시되지 않는다.
        assertTrue(recordUi.log.filter { it.kind == LogKind.CHOICE }.none { it.text == recordOnly.automaticEvent!!.text })

        // 카드는 한 해(3초)보다 길게, 4~8초 안에서 글자 수만큼 머문다.
        rules.forEach { rule ->
            val hold = automaticEventHoldMillis(AutomaticEventUi(rule.event.eventId, rule.topic, rule.event.text, emptyList()))
            assertTrue(rule.event.eventId, hold in 4_000L..8_000L && hold > YEAR_MILLIS_AT_1X)
        }

        // 결과를 보는 중에는 자동 사건 카드가 없다(자동 사건은 결과 · 결정 · 결말보다 앞서지 않는다).
        val withOutcome = buildPeriodPlayState(stat, content, "재겸", outcome = OutcomeUi("고른 것", "결과", emptyList(), false))
        assertNull(withOutcome.automaticEvent)
        val decision = trace.first { it.step.phase == TurnPhase.DECISION }.step
        assertNull(buildPeriodPlayState(decision, content, "재겸").automaticEvent)
    }

    // ---- 과제 3: 확장 UI와 실제 데이터 ----

    private fun record(eventId: String, choiceId: String, period: LifePeriod = LifePeriod.CHILD, year: Int = 2, title: String = eventId) =
        RunChoiceRecord(eventId, choiceId, "o", period, year, title, "고른 것.", "결과")

    @Test
    fun `turning point cards pick 2 to 3 real events, spreading fields first`() {
        val familyOnly = listOf(
            record("love_marriage_talk_01", "A", LifePeriod.YOUNG_ADULT, 3, "결혼 이야기"),
            record("love_first_child_01", "A", LifePeriod.MIDDLE, 1, "첫아이"),
            record("love_wedding_01", "A", LifePeriod.YOUNG_ADULT, 4, "결혼식"),
        )
        val familyFlags = setOf("v4:event:love_marriage_talk_01", "married", "has_child")
        val family = extensions.turningPointsFor(familyOnly, familyFlags, periods)
        assertEquals(listOf("tp_family_first_child", "tp_family_wedding", "tp_family_proposal"), family.map { it.cardId })
        assertTrue(family.all { it.choiceLabel == "고른 것" })

        val mixed = familyOnly + record("path_int_07", "B", LifePeriod.TEEN, 5, "졸업 앞의 세 갈래")
        val mixedCards = extensions.turningPointsFor(mixed, familyFlags + "v4:event:path_int_07", periods)
        assertEquals(3, mixedCards.size)
        assertEquals(setOf("가족", "교육"), mixedCards.take(2).map { it.fieldLabel }.toSet())

        val single = extensions.turningPointsFor(listOf(familyOnly[1]), setOf("has_child"), periods)
        assertEquals(1, single.size)
        assertTrue(extensions.turningPointsFor(emptyList(), familyFlags, periods).isEmpty())
    }

    @Test
    fun `a transfer friend returns in a yearly automatic event only when the relationship really formed`() {
        val kind = listOf(record("child_025", "A", title = "혼자 밥 먹는 전학생"))
        val flags = setOf("kind_heart", "v4:event:child_025")
        val npc = extensions.npcForEvent("auto_child_transfer_friend", kind, flags, periods, ScenarioExtensions.APPEARANCE_AUTO)
        assertEquals("npc_transfer_child", npc?.npcId)
        assertEquals("혼자 밥 먹는 전학생", npc?.originTitle)
        // 친구들이랑 먹은(B) 인생에서는 다시 나오지 않는다.
        assertNull(extensions.npcForEvent("auto_child_transfer_friend", listOf(record("child_025", "B")), setOf("v4:event:child_025"), periods, ScenarioExtensions.APPEARANCE_AUTO))
        // 결정 사건 목록에서 자동 사건 id를 찾지 않는다.
        assertNull(extensions.npcForEvent("auto_child_transfer_friend", kind, flags, periods))

        val met = extensions.npcsMetAgain(
            kind,
            flags,
            periods,
            autoAppearances = listOf(AutoAppearance("auto_child_transfer_friend", "전학생 친구", LifePeriod.CHILD)),
        )
        assertEquals(listOf("전학생 친구"), met.single { it.npcId == "npc_transfer_child" }.metAgainTitles)
    }

    @Test
    fun `an automatic-only partner is not treated as a recorded origin`() {
        val loveStart = AutoAppearance("auto_ya_love_start", "다시 연락 온 사람", LifePeriod.YOUNG_ADULT)
        val flags = setOf("partner_second_chance", "dating", "rel_conflict")
        val npc = extensions.npcForEvent("love_conflict_01", emptyList(), flags, periods, priorAutoAppearances = listOf(loveStart))
        assertNull(npc)
        // 관계 플래그가 하나도 없으면 다시 나오지 않는다.
        assertNull(extensions.npcForEvent("love_conflict_01", emptyList(), setOf("dating", "rel_conflict"), periods, priorAutoAppearances = listOf(loveStart)))
    }

    @Test
    fun `a doljabi recall shown on the automatic photo is not repeated in a later event`() {
        val dol = listOf(record("infant_007", "A", LifePeriod.INFANT, 1, "돌잡이"))
        val flags = setOf("music_toy", "v4:event:infant_007")
        val onPhoto = extensions.recallForEvent("auto_infant_dol_photo_music", dol, flags)
        assertNull(onPhoto)
        assertNotNull(extensions.recallForEvent("path_hap_11", dol, flags, priorAutoEventIds = setOf("auto_infant_dol_photo_music")))
        assertNotNull(extensions.recallForEvent("path_hap_11", dol, flags))
        // 근거 선택이 다르면(판사봉) 피아노 회상은 나오지 않는다.
        assertNull(extensions.recallForEvent("auto_infant_dol_photo_music", listOf(record("infant_007", "B", LifePeriod.INFANT, 1)), setOf("judge_gavel"), emptySet()))
    }

    @Test
    fun `the life record lists automatic events apart from choices with record-only marks`() {
        val trace = play(20261116L + 11)
        val records: List<AutomaticEventRecord> = trace.last().step.state.automaticEventRecords
        assertTrue(records.isNotEmpty())
        val lines = autoRecordLines(records, periods, ruleById)
        assertEquals(records.map { it.text }, lines.map { it.text })
        lines.zip(records).forEach { (line, record) ->
            assertEquals(ruleById.getValue(record.eventId).recordOnly, line.changes.isEmpty())
            assertTrue(line.label.endsWith("${record.yearInPeriod}번째 해"))
        }
    }

    /** 문서에 남길 수치: 200개 인생에서 자동 사건 빈도 · 단계 분포와, 자동 사건이 사망률을 바꾸지 않는지. */
    @Test
    fun `automatic event summary over 200 lives`() {
        val seeds = (1L..200L).map { 20261116_000L + it }
        val withAuto = seeds.map { play(it) }
        val without = seeds.map { play(it, withAutoEvents = false) }
        val perLife = withAuto.map { trace -> trace.count { it.step.automaticEvent != null } }
        val byStage = withAuto.flatMap { trace -> trace.mapNotNull { it.step.automaticEvent?.eventId } }
            .groupingBy { ruleById.getValue(it).stage }.eachCount()
        val recordOnly = withAuto.sumOf { trace -> trace.count { t -> t.step.automaticEvent?.let { ruleById.getValue(it.eventId).recordOnly } == true } }
        val distinctEvents = withAuto.flatMap { trace -> trace.mapNotNull { it.step.automaticEvent?.eventId } }.toSet()
        // 노년기를 다 채우지 못하고 끝난 인생(건강 악화 · 사고 등)
        fun early(trace: List<Trace>) = trace.last().step.state.let { it.isDead && (it.endingPeriod ?: it.period) != LifePeriod.OLD_AGE }
        val deathsWith = withAuto.count(::early)
        val deathsWithout = without.count(::early)
        val decisionsWith = withAuto.sumOf { t -> t.count { it.step.phase == TurnPhase.DECISION } }
        val decisionsWithout = without.sumOf { t -> t.count { it.step.phase == TurnPhase.DECISION } }
        println(
            "AUTO_SUMMARY lives=${seeds.size} total=${perLife.sum()} min=${perLife.min()} max=${perLife.max()} " +
                "avg=${"%.2f".format(perLife.average())} recordOnly=$recordOnly distinct=${distinctEvents.size}/49 " +
                "stages=$byStage deathsWith=$deathsWith deathsWithout=$deathsWithout " +
                "decisionsWith=$decisionsWith decisionsWithout=$decisionsWithout",
        )
        assertTrue(perLife.min() >= 1)
        assertTrue(perLife.max() <= 14)
        assertEquals(AutomaticEventStage.entries.toSet(), byStage.keys)
    }
}
