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
    ) = GameProgress(
        runId = "run-001",
        stage = stage,
        currentEventOccurrenceId = "run-001:child_001:1",
        stats = stats,
        flags = flags,
        completedEventIds = completedEventIds,
        specialEventCount = specialEventCount,
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
