package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EventConditions
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GameEngineTest {
    private val engine = GameEngine(rollPercent = { 25 })

    @Test
    fun clampStat_limitsValuesToZeroThroughOneHundred() {
        assertEquals(0, engine.clampStat(-1))
        assertEquals(50, engine.clampStat(50))
        assertEquals(100, engine.clampStat(101))
    }

    @Test
    fun createInitialProgress_startsWithDefaultStatsAndInfantStage() {
        val initial = engine.createInitialProgress("run-new")

        assertEquals(Stats(), initial.stats)
        assertEquals(LifeStage.INFANT, initial.stage)
        assertEquals("run-new:pending:0", initial.currentEventOccurrenceId)
        assertEquals(0, initial.stageEventCount)
    }

    @Test
    fun applyChoice_addsIntelligenceDelta() {
        val event = event(choice(statDelta = StatDelta(intelligence = 6)))

        val result = engine.applyChoice(progress(), event, event.choices.single())

        assertTrue(result.wasApplied)
        assertEquals(56, result.afterStats.intelligence)
        assertEquals(1, result.progress.choiceHistory.size)
    }

    @Test
    fun applyChoice_clampsUpperAndLowerBounds() {
        val event = event(
            choice(statDelta = StatDelta(health = 5, social = -5)),
        )
        val progress = progress(stats = Stats(health = 98, social = 2))

        val result = engine.applyChoice(progress, event, event.choices.single())

        assertEquals(100, result.afterStats.health)
        assertEquals(0, result.afterStats.social)
        assertEquals(2, result.appliedDelta.health)
        assertEquals(-2, result.appliedDelta.social)
    }

    @Test
    fun applyChoice_updatesAllSevenStats() {
        val event = event(
            choice(
                statDelta = StatDelta(
                    health = 1,
                    fitness = 2,
                    intelligence = 3,
                    social = 4,
                    wealth = 5,
                    happiness = 6,
                    luck = 7,
                ),
            ),
        )

        val result = engine.applyChoice(progress(), event, event.choices.single())

        assertEquals(
            Stats(
                health = 51,
                fitness = 52,
                intelligence = 53,
                social = 54,
                wealth = 55,
                happiness = 56,
                luck = 57,
            ),
            result.afterStats,
        )
    }

    @Test
    fun applyChoice_extremeDeltaDoesNotOverflow() {
        val event = event(
            choice(
                statDelta = StatDelta(
                    health = Int.MAX_VALUE,
                    happiness = Int.MIN_VALUE,
                ),
            ),
        )

        val result = engine.applyChoice(progress(), event, event.choices.single())

        assertEquals(100, result.afterStats.health)
        assertEquals(0, result.afterStats.happiness)
    }

    @Test
    fun applyChoice_addsExperienceFlagsAndCompletesEvent() {
        val event = event(choice(addFlags = setOf("reading_interest")))

        val result = engine.applyChoice(progress(), event, event.choices.single())

        assertTrue("reading_interest" in result.progress.flags)
        assertTrue(event.eventId in result.progress.completedEventIds)
    }

    @Test
    fun applyChoice_sameOccurrenceIsAppliedOnlyOnce() {
        val event = event(choice(statDelta = StatDelta(intelligence = 6)))
        val first = engine.applyChoice(progress(), event, event.choices.single())

        val second = engine.applyChoice(first.progress, event, event.choices.single())

        assertFalse(second.wasApplied)
        assertEquals(56, second.afterStats.intelligence)
        assertEquals(1, second.progress.choiceHistory.size)
    }

    @Test
    fun applyChoice_rejectsChoiceFromAnotherEvent() {
        val event = event(choice(choiceId = "A"))

        try {
            engine.applyChoice(progress(), event, choice(choiceId = "B"))
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected: an unrelated choice must never mutate progress.
        }
    }

    @Test
    fun canShowEvent_checksStageStatsAndRequiredFlags() {
        val qualified = event(
            conditions = EventConditions(
                stats = StatConditions(minSocial = 60),
                requiredFlags = setOf("club_joined"),
            ),
        )
        val progress = progress(
            stats = Stats(social = 70),
            flags = setOf("club_joined"),
        )

        assertTrue(engine.canShowEvent(progress, qualified))
        assertFalse(engine.canShowEvent(progress.copy(flags = emptySet()), qualified))
        assertFalse(engine.canShowEvent(progress.copy(stage = LifeStage.TEEN), qualified))
    }

    @Test
    fun canShowEvent_rejectsBlockedOrCompletedEvent() {
        val blocked = event(
            conditions = EventConditions(blockedFlags = setOf("injured")),
        )

        assertFalse(engine.canShowEvent(progress(flags = setOf("injured")), blocked))
        assertFalse(
            engine.canShowEvent(
                progress(completedEventIds = setOf(blocked.eventId)),
                blocked,
            ),
        )
    }

    @Test
    fun pickNextEvent_usesHighestPriorityThenEventId() {
        val laterId = event(eventId = "child_020", priority = 10)
        val earlierId = event(eventId = "child_010", priority = 10)
        val lowerPriority = event(eventId = "child_001", priority = 5)

        val selected = engine.pickNextEvent(
            progress(),
            listOf(laterId, lowerPriority, earlierId),
        )

        assertEquals("child_010", selected?.eventId)
    }

    @Test
    fun pickNextEvent_selectsSocialRequirementWhenSatisfied() {
        val socialEvent = event(
            eventId = "child_social",
            conditions = EventConditions(stats = StatConditions(minSocial = 60)),
            priority = 20,
        )
        val commonEvent = event(eventId = "child_common", isFallback = true)

        val selected = engine.pickNextEvent(
            progress(stats = Stats(social = 60)),
            listOf(commonEvent, socialEvent),
        )

        assertSame(socialEvent, selected)
    }

    @Test
    fun pickNextEvent_usesFallbackOnlyWhenNoConditionalEventMatches() {
        val conditional = event(
            eventId = "child_social",
            conditions = EventConditions(stats = StatConditions(minSocial = 90)),
        )
        val fallback = event(eventId = "child_common", isFallback = true)

        val selected = engine.pickNextEvent(
            progress(stats = Stats(social = 50)),
            listOf(conditional, fallback),
        )

        assertSame(fallback, selected)
    }

    @Test
    fun pickNextEvent_returnsNullWhenNoEventCanBeShown() {
        val teenEvent = event(stage = LifeStage.TEEN)

        assertNull(engine.pickNextEvent(progress(), listOf(teenEvent)))
    }

    @Test
    fun advanceAfterResult_excludesCompletedEvent() {
        val completed = event(eventId = "child_done", priority = 100)
        val remaining = event(eventId = "child_next", priority = 10)

        val advanced = engine.advanceAfterResult(
            progress(completedEventIds = setOf(completed.eventId)),
            listOf(completed, remaining),
        )

        assertTrue(advanced.currentEventOccurrenceId.contains(remaining.eventId))
        assertEquals(com.example.lifegame.domain.model.GameScreenState.EVENT, advanced.screenState)
    }

    @Test
    fun advanceAfterResult_movesToEndingWhenNoCandidatesRemain() {
        val advanced = engine.advanceAfterResult(
            progress(stage = LifeStage.ADULT),
            emptyList(),
        )

        assertEquals(com.example.lifegame.domain.model.GameScreenState.ENDING, advanced.screenState)
    }

    @Test
    fun pickSpecialEvent_stopsAtPerRunLimit() {
        val special = event(eventId = "special_001", type = EventType.SPECIAL)
        val limited = GameEngine(rollPercent = { 1 }, maxSpecialEventsPerRun = 2)

        val selected = limited.pickSpecialEvent(
            progress(specialEventCount = 2),
            listOf(special),
            chancePercent = 100,
        )

        assertNull(selected)
    }

    @Test
    fun pickSpecialEvent_usesHighestPriorityThenEventId() {
        val eventB = event(eventId = "special_b", type = EventType.SPECIAL, priority = 50)
        val eventA = event(eventId = "special_a", type = EventType.SPECIAL, priority = 50)
        val low = event(eventId = "special_low", type = EventType.SPECIAL, priority = 1)

        val selected = engine.pickSpecialEvent(
            progress(),
            listOf(eventB, low, eventA),
            chancePercent = 100,
        )

        assertSame(eventA, selected)
    }

    @Test
    fun shouldMoveNextStage_afterFiveEvents() {
        assertFalse(engine.shouldMoveNextStage(progress(stageEventCount = 4)))
        assertTrue(engine.shouldMoveNextStage(progress(stageEventCount = 5)))
    }

    @Test
    fun moveNextStage_keepsAdultAndAdvancesOtherStages() {
        val child = engine.moveNextStage(
            progress(stage = LifeStage.INFANT, stageEventCount = 5),
        )
        val adult = progress(stage = LifeStage.ADULT, stageEventCount = 5)

        assertEquals(LifeStage.CHILD, child.stage)
        assertEquals(0, child.stageEventCount)
        assertSame(adult, engine.moveNextStage(adult))
    }

    @Test
    fun resolveEnding_usesHighestPriorityThenEndingId() {
        val endingB = ending("ending_b", priority = 80)
        val endingA = ending("ending_a", priority = 80)
        val low = ending("ending_low", priority = 10)
        val default = ending("ending_default", isDefault = true)

        val selected = engine.resolveEnding(
            progress(stats = Stats(intelligence = 90)),
            listOf(endingB, default, low, endingA),
        )

        assertEquals("ending_a", selected.endingId)
    }

    @Test
    fun resolveEndingDecision_listsAllEligibleEndingsInDecisionOrder() {
        val high = ending("ending_high", priority = 100)
        val tiedB = ending("ending_tied_b", priority = 80)
        val tiedA = ending("ending_tied_a", priority = 80)
        val default = ending("ending_default", isDefault = true)

        val decision = engine.resolveEndingDecision(
            progress(),
            listOf(tiedB, default, high, tiedA),
        )

        assertEquals("ending_high", decision.selected.endingId)
        assertEquals(
            listOf("ending_high", "ending_tied_a", "ending_tied_b"),
            decision.eligibleEndingIds,
        )
        assertEquals(EndingSelectionReason.HIGHEST_PRIORITY, decision.reason)
    }

    @Test
    fun resolveEndingDecision_sameConditionsAlwaysSelectExactlyOneEnding() {
        val endings = (1..25).map { index ->
            ending("ending_${index.toString().padStart(2, '0')}", priority = index)
        } + ending("ending_default", isDefault = true)

        val selectedIds = (1..30).map {
            engine.resolveEndingDecision(progress(), endings).selected.endingId
        }.toSet()

        assertEquals(setOf("ending_25"), selectedIds)
    }

    @Test
    fun resolveEndingDecision_excludesBlockedAndMissingRequiredFlags() {
        val blocked = ending(
            "ending_blocked",
            priority = 100,
            conditions = EndingConditions(blockedFlags = setOf("injured")),
        )
        val missingFlag = ending(
            "ending_missing_flag",
            priority = 90,
            conditions = EndingConditions(requiredFlags = setOf("career_experience")),
        )
        val eligible = ending("ending_eligible", priority = 10)
        val default = ending("ending_default", isDefault = true)

        val decision = engine.resolveEndingDecision(
            progress(flags = setOf("injured")),
            listOf(blocked, missingFlag, eligible, default),
        )

        assertEquals("ending_eligible", decision.selected.endingId)
        assertEquals(listOf("ending_eligible"), decision.eligibleEndingIds)
    }

    @Test
    fun resolveEnding_usesDefaultWhenNoConditionMatches() {
        val specialist = ending(
            "ending_specialist",
            conditions = EndingConditions(
                stats = StatConditions(minIntelligence = 90),
                requiredFlags = setOf("career_experience"),
            ),
        )
        val default = ending("ending_default", isDefault = true)

        val selected = engine.resolveEnding(progress(), listOf(specialist, default))

        assertSame(default, selected)
    }

    @Test
    fun resolveEndingDecision_reportsDefaultFallbackWhenNoConditionMatches() {
        val specialist = ending(
            "ending_specialist",
            priority = 100,
            conditions = EndingConditions(stats = StatConditions(minIntelligence = 90)),
        )
        val default = ending("ending_default", isDefault = true)

        val decision = engine.resolveEndingDecision(
            progress(stats = Stats(intelligence = 50)),
            listOf(specialist, default),
        )

        assertSame(default, decision.selected)
        assertTrue(decision.eligibleEndingIds.isEmpty())
        assertEquals(EndingSelectionReason.DEFAULT_FALLBACK, decision.reason)
    }

    @Test
    fun checkEnding_matchesResolveEndingForNotionCompatibility() {
        val highPriority = ending("ending_high", priority = 100)
        val default = ending("ending_default", isDefault = true)
        val endings = listOf(default, highPriority)

        assertEquals(
            engine.resolveEnding(progress(), endings),
            engine.checkEnding(progress(), endings),
        )
    }

    @Test
    fun shouldTriggerSpecialEvent_obeysChanceBoundary() {
        assertFalse(engine.shouldTriggerSpecialEvent(progress(), 0))
        assertTrue(engine.shouldTriggerSpecialEvent(progress(), 25))
        assertFalse(engine.shouldTriggerSpecialEvent(progress(), 24))
        assertTrue(engine.shouldTriggerSpecialEvent(progress(), 100))
    }

    @Test
    fun shouldTriggerSpecialEvent_stopsAtPerRunLimit() {
        val limitedEngine = GameEngine(
            rollPercent = { 1 },
            maxSpecialEventsPerRun = 2,
        )

        assertFalse(
            limitedEngine.shouldTriggerSpecialEvent(
                progress(specialEventCount = 2),
                100,
            ),
        )
    }

    @Test
    fun applyChoice_incrementsSpecialEventCount() {
        val special = event(
            type = EventType.SPECIAL,
            choice = choice(),
        )

        val result = engine.applyChoice(progress(), special, special.choices.single())

        assertEquals(1, result.progress.specialEventCount)
    }

    private fun progress(
        stats: Stats = Stats(),
        flags: Set<String> = emptySet(),
        completedEventIds: Set<String> = emptySet(),
        stage: LifeStage = LifeStage.CHILD,
        specialEventCount: Int = 0,
        stageEventCount: Int = 0,
    ) = GameProgress(
        runId = "run-001",
        stage = stage,
        currentEventOccurrenceId = "run-001:child_001:1",
        stats = stats,
        flags = flags,
        completedEventIds = completedEventIds,
        specialEventCount = specialEventCount,
        stageEventCount = stageEventCount,
    )

    private fun event(
        choice: Choice = choice(),
        eventId: String = "child_001",
        stage: LifeStage = LifeStage.CHILD,
        type: EventType = EventType.NORMAL,
        conditions: EventConditions = EventConditions(),
        priority: Int = 0,
        isFallback: Boolean = false,
    ) = GameEvent(
        eventId = eventId,
        stage = stage,
        type = type,
        title = "방과 후 선택",
        text = "학교가 끝났다. 오늘 남은 시간을 어떻게 보낼까?",
        imageId = "img_child_school_001",
        conditions = conditions,
        choices = listOf(choice),
        priority = priority,
        isFallback = isFallback,
    )

    private fun choice(
        choiceId: String = "A",
        statDelta: StatDelta = StatDelta(),
        addFlags: Set<String> = emptySet(),
    ) = Choice(
        choiceId = choiceId,
        label = "도서관에서 책을 읽는다.",
        statDelta = statDelta,
        resultText = "새로운 지식을 얻었다.",
        addFlags = addFlags,
    )

    private fun ending(
        id: String,
        priority: Int = 0,
        conditions: EndingConditions = EndingConditions(),
        isDefault: Boolean = false,
    ) = Ending(
        endingId = id,
        title = id,
        summary = "테스트 결말",
        priority = priority,
        conditions = conditions,
        isDefault = isDefault,
    )
}
