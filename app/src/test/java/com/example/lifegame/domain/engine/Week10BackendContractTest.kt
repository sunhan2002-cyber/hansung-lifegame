package com.example.lifegame.domain.engine

import com.example.lifegame.data.V4ContentParser
import com.example.lifegame.domain.generation.FaceSheetRequest
import com.example.lifegame.domain.generation.FakeGenerationClient
import com.example.lifegame.domain.generation.FakeGenerationMode
import com.example.lifegame.domain.generation.GenerationResult
import com.example.lifegame.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class Week10BackendContractTest {
    private val periods = listOf(PeriodDefinition(LifePeriod.YOUNG_ADULT, 0, 3))

    @Test fun parserReadsOutcomeRemoveFlagsAndMaxStats() {
        val event = V4ContentParser.parseDecisionEvents(
            """[{"eventId":"e","periods":["YOUNG_ADULT"],"kind":"INCIDENT","title":"t","text":"x","choices":[{"choiceId":"A","label":"a","maxStats":{"건강":34},"outcomes":[{"outcomeId":"o","text":"r","chance":100,"removeFlags":["married"]}]}]}]""",
        ).single()
        assertEquals(setOf("married"), event.choices.single().outcomes.single().removeFlags)
        assertTrue(event.choices.single().statConditions.matches(Stats(health = 34)))
        assertFalse(event.choices.single().statConditions.matches(Stats(health = 35)))
    }

    @Test fun removeHappensBeforeAdditionAndAdditionWinsConflict() {
        val event = event(Outcome("o", "변화", addFlags = setOf("married", "divorced"), chance = 100, removeFlags = setOf("married")))
        val state = PeriodRunState(
            "run", 1L, LifePeriod.YOUNG_ADULT,
            flags = linkedSetOf("married"),
            lifeFacts = LifeFacts(maritalStatus = "MARRIED"),
            pendingDecisionId = event.eventId,
        )
        val rules = listOf(
            LifeFactRule("married", "maritalStatus", "MARRIED"),
            LifeFactRule("divorced", "maritalStatus", "DIVORCED"),
        )
        val result = GameEngine().resolvePeriodDecision(state, event, "A", periods, rules).step.state
        assertTrue("addition must win a remove/add conflict", "married" in result.flags)
        assertTrue("divorced" in result.flags)
        assertEquals("DIVORCED", result.lifeFacts.maritalStatus)
    }

    @Test fun healthDeltaCombinesAuthoredFieldsAndAnnualDeltaExactlyOnce() {
        val outcome = Outcome("o", "결과", statDelta = StatDelta(health = -10), chance = 100, lifeDelta = 2)
        assertEquals(-13, GameEngine().resolveOutcomeStatDelta(LifePeriod.YOUNG_ADULT, outcome).health)
        assertEquals(-8, GameEngine().resolveOutcomeStatDelta(LifePeriod.TEEN, outcome).health)
    }

    @Test fun explicitBigIncidentDoesNotGainHiddenDamage() {
        val event = V4ContentParser.parseDecisionEvents(
            """[{"eventId":"big","periods":["YOUNG_ADULT"],"kind":"BIG_INCIDENT","title":"위기","text":"x","choices":[{"choiceId":"A","label":"회피","outcomes":[{"outcomeId":"safe","text":"피했다","chance":100,"statDelta":{"건강":3}}]}]}]""",
        ).single()
        val outcome = event.choices.single().outcomes.single()
        assertEquals(0, outcome.lifeDelta)
        assertEquals(-2, GameEngine().resolveOutcomeStatDelta(LifePeriod.YOUNG_ADULT, outcome).health)
    }

    @Test fun titleUsesPriorityThenStableIdTieBreak() {
        val state = PeriodRunState("r", 1L, LifePeriod.OLD_AGE, stats = Stats(intelligence = 90))
        val condition = EndingConditions(stats = StatConditions(minIntelligence = 80))
        val titles = listOf(
            LifeTitle("z", "Z", "", 50, condition),
            LifeTitle("a", "A", "", 50, condition),
            LifeTitle("high", "HIGH", "", 70, condition),
            LifeTitle("default", "기본", "", 0, isDefault = true),
        )
        assertEquals("high", GameEngine().resolveTitle(state, titles).titleId)
        assertEquals("a", GameEngine().resolveTitle(state, titles.filterNot { it.titleId == "high" }).titleId)
    }

    @Test fun titleFallsBackToExactlyOneDefault() {
        val titles = listOf(
            LifeTitle("locked", "잠김", "", 100, EndingConditions(requiredFlags = setOf("never"))),
            LifeTitle("default", "보통의 삶", "", 0, isDefault = true),
        )
        assertEquals("default", GameEngine().resolveTitle(PeriodRunState("r", 1L, LifePeriod.OLD_AGE), titles).titleId)
    }

    @Test fun bodyTypeBoundaryFollowsFitnessFirstPolicy() {
        assertEquals(BodyType.MUSCULAR, bodyTypeFor(Stats(health = 10, fitness = 70)))
        assertEquals(BodyType.SLIM, bodyTypeFor(Stats(health = 34, fitness = 69)))
        assertEquals(BodyType.SLIM, bodyTypeFor(Stats(health = 35, fitness = 29)))
        assertEquals(BodyType.STANDARD, bodyTypeFor(Stats(health = 35, fitness = 30)))
    }

    @Test fun fakeGenerationCoversSuccessFailureTimeoutAndCancel() {
        val request = FaceSheetRequest("r", "local://photo", consent = true)
        assertTrue(FakeGenerationClient().createFaceSheet(request) is GenerationResult.Success)
        assertTrue(FakeGenerationClient(FakeGenerationMode.FAILURE).createFaceSheet(request) is GenerationResult.Failure)
        assertSame(GenerationResult.TimedOut, FakeGenerationClient(FakeGenerationMode.TIMEOUT).createFaceSheet(request))
        val cancelled = FakeGenerationClient().also { it.cancel("r") }
        assertSame(GenerationResult.Cancelled, cancelled.createFaceSheet(request))
        assertTrue(FakeGenerationClient().createFaceSheet(request.copy(consent = false)) is GenerationResult.Failure)
    }

    @Test fun recapUsesOnlyActuallyRecordedImagesInChronologicalOrder() {
        fun record(event: String, outcome: String, image: String?) = RunChoiceRecord(
            event, "A", outcome, LifePeriod.TEEN, 1, event, "선택", "결과", image,
        )
        val first = record("first", "ok", "img_first")
        val duplicate = record("first", "ok", "img_first")
        val second = record("second", "ok", "img_second")
        assertEquals(listOf(first, second), recapMomentsFrom(listOf(first, duplicate, record("none", "ok", null), second)))
    }

    private fun event(outcome: Outcome) = DecisionEvent(
        eventId = "event", periods = setOf(LifePeriod.YOUNG_ADULT), kind = DecisionKind.INCIDENT,
        title = "사건", text = "본문", choices = listOf(DecisionOption("A", "선택", listOf(outcome))),
    )
}
