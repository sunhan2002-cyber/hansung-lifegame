package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.model.*
import java.io.File
import org.junit.Assert.*
import org.junit.Test

class Week13WooyoungCompletionTest {
    private fun autoEvents() = V4ContentParser.parseAutomaticEvents(
        File("src/main/assets/auto_events.sample.json").readText(),
    )

    @Test fun automaticEventJsonLoadsAllRuntimeFields() {
        val events = autoEvents()
        assertEquals(49, events.size)
        assertTrue(events.any { it.trigger == AutomaticEventTrigger.ELAPSED_TIME })
        assertTrue(events.any { it.triggerRule.stage == AutomaticEventStage.AFTER_EVENT })
        val recordOnly = events.first { it.effect == AutomaticEventEffect.RECORD_ONLY }
        assertEquals(StatDelta(), recordOnly.statDelta)
        assertTrue(recordOnly.dedupeKey.isNotBlank())
    }

    @Test fun periodSessionUsesProgressStageWithoutAnnualCounter() {
        val period = PeriodDefinition(LifePeriod.CHILD, 0, 3)
        val focus = Focus("child_focus", LifePeriod.CHILD, "놀이", StatDelta(), "놀이")
        val event = AutomaticEvent(
            "auto_start", setOf(LifePeriod.CHILD), "시기가 시작됐다.",
            trigger = AutomaticEventTrigger.PROGRESS_STAGE,
            triggerRule = AutomaticEventTriggerRule(stage = AutomaticEventStage.PERIOD_START),
            effect = AutomaticEventEffect.RECORD_ONLY,
        )
        val session = PeriodGameSession(
            listOf(period), listOf(focus), emptyList(), automaticEvents = listOf(event),
            engine = GameEngine(), runIdFactory = { "run" }, runSeedFactory = { 7L },
        )
        session.start()

        val step = session.selectFocus(focus.focusId)

        assertEquals("auto_start", step.automaticEvent?.eventId)
        assertEquals(1, step.state.automaticEventRecords.size)
        assertEquals(Stats.DEFAULT_STAT, step.state.stats.health)
    }

    @Test fun decisionPreemptsAutomaticEventAndDuplicateTriggerIsRejected() {
        val period = PeriodDefinition(LifePeriod.CHILD, 0, 3)
        val focus = Focus("child_focus", LifePeriod.CHILD, "놀이", StatDelta(), "놀이")
        val decision = DecisionEvent(
            "child_decision", setOf(LifePeriod.CHILD), DecisionKind.MILESTONE, "결정", "결정",
            listOf(DecisionOption("A", "선택", listOf(Outcome("o", "결과", chance = 100)))),
            yearInPeriod = 1,
        )
        val automatic = AutomaticEvent(
            "auto_start", setOf(LifePeriod.CHILD), "자동",
            trigger = AutomaticEventTrigger.PROGRESS_STAGE,
            triggerRule = AutomaticEventTriggerRule(stage = AutomaticEventStage.PERIOD_START),
        )
        val session = PeriodGameSession(
            listOf(period), listOf(focus), listOf(decision), automaticEvents = listOf(automatic),
            engine = GameEngine(), runIdFactory = { "run" }, runSeedFactory = { 8L },
        )
        session.start()
        val decisionStep = session.selectFocus(focus.focusId)
        assertEquals(TurnPhase.DECISION, decisionStep.phase)
        assertTrue(decisionStep.state.automaticEventRecords.isEmpty())

        val engine = GameEngine()
        val base = PeriodRunState("dedupe", 9L, LifePeriod.CHILD)
        val first = requireNotNull(engine.applyAutomaticEvent(base, listOf(automatic), "same", stage = AutomaticEventStage.PERIOD_START))
        assertNull(engine.applyAutomaticEvent(first.state, listOf(automatic), "same", stage = AutomaticEventStage.PERIOD_START))
    }

    @Test fun recallAndNpcRequireActualChoiceAndRelationshipFlag() {
        val actual = choice("origin", "A")
        assertNull(RunExtensionApi.recallEvidence("missing", emptySet(), listOf(actual)))
        assertNotNull(RunExtensionApi.recallEvidence("origin", setOf("A"), listOf(actual)))
        assertFalse(RunExtensionApi.canNpcReturn(listOf("origin"), setOf("A"), setOf("friend"), listOf(actual), emptySet()))
        assertFalse(RunExtensionApi.canNpcReturn(listOf("missing"), emptySet(), setOf("friend"), listOf(actual), setOf("friend")))
        assertTrue(RunExtensionApi.canNpcReturn(listOf("origin"), setOf("A"), setOf("friend"), listOf(actual), setOf("friend")))
    }

    @Test fun catalystsUseOnlyActualRecordsAndNeedTwoCards() {
        val choices = listOf(choice("school", "A"), choice("career", "B"), choice("family", "C"))
        assertTrue(RunExtensionApi.catalystRecords(listOf("school", "missing"), choices).isEmpty())
        assertEquals(
            listOf("school", "career", "family"),
            RunExtensionApi.catalystRecords(listOf("school", "career", "family", "missing"), choices).map { it.eventId },
        )
    }

    private fun choice(eventId: String, choiceId: String) = RunChoiceRecord(
        eventId, choiceId, "outcome", LifePeriod.YOUNG_ADULT, 1, eventId, choiceId, "결과",
    )
}
