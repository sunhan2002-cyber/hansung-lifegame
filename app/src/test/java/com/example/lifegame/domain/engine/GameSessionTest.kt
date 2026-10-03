package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    @Test
    fun choose_reflectsEngineResultInSessionStats() {
        val session = session(events = listOf(event(LifeStage.INFANT, healthDelta = 7)))
        val started = session.start()

        val result = session.choose(started.currentEvent!!.choices.single())

        assertTrue(result!!.wasApplied)
        assertEquals(50, result.beforeStats.health)
        assertEquals(57, session.state.progress!!.stats.health)
        assertEquals(GameScreenState.CHOICE_RESULT, session.state.progress!!.screenState)
    }

    @Test
    fun proceed_movesToNextLifeStageWhenCurrentStageIsComplete() {
        val infant = event(LifeStage.INFANT)
        val child = event(LifeStage.CHILD)
        val session = session(events = listOf(infant, child))
        session.start()
        session.choose(infant.choices.single())

        val hasNext = session.proceed()

        assertTrue(hasNext)
        assertEquals(LifeStage.CHILD, session.state.progress!!.stage)
        assertEquals(child.eventId, session.state.currentEvent!!.eventId)
        assertNull(session.state.lastResult)
    }

    @Test
    fun proceed_resolvesDefaultEndingAfterLastEvent() {
        val onlyEvent = event(LifeStage.INFANT)
        val session = session(events = listOf(onlyEvent))
        session.start()
        session.choose(onlyEvent.choices.single())

        val hasNext = session.proceed()

        assertFalse(hasNext)
        assertEquals("ending_default", session.state.ending!!.endingId)
        assertEquals(GameScreenState.ENDING, session.state.progress!!.screenState)
    }

    @Test
    fun choose_cannotApplyTwiceBeforeProceeding() {
        val onlyEvent = event(LifeStage.INFANT, healthDelta = 7)
        val session = session(events = listOf(onlyEvent))
        val choice = session.start().currentEvent!!.choices.single()

        assertTrue(session.choose(choice)!!.wasApplied)
        assertNull(session.choose(choice))
        assertEquals(57, session.state.progress!!.stats.health)
    }

    @Test
    fun restore_reopensSavedEventScreen() {
        val infant = event(LifeStage.INFANT)
        val session = session(events = listOf(infant))
        val progress = GameProgress(
            runId = "saved-run",
            currentEventOccurrenceId = "saved-run:${infant.eventId}:1",
            stage = LifeStage.INFANT,
            stageEventCount = 3,
        )

        val restored = session.restore(progress)

        assertEquals(progress, restored.progress)
        assertEquals(infant.eventId, restored.currentEvent!!.eventId)
        assertNull(restored.lastResult)
    }

    @Test
    fun restore_rebuildsSavedChoiceResult() {
        val infant = event(LifeStage.INFANT, healthDelta = 7)
        val before = Stats()
        val after = before.copy(health = 57)
        val progress = GameProgress(
            runId = "saved-run",
            currentEventOccurrenceId = "saved-run:${infant.eventId}:1",
            screenState = GameScreenState.CHOICE_RESULT,
            stats = after,
            completedEventIds = setOf(infant.eventId),
            handledOccurrenceIds = setOf("saved-run:${infant.eventId}:1"),
            choiceHistory = listOf(
                ChoiceHistory(
                    eventOccurrenceId = "saved-run:${infant.eventId}:1",
                    eventId = infant.eventId,
                    choiceId = "A",
                    beforeStats = before,
                    afterStats = after,
                    resultText = "선택이 반영되었다.",
                ),
            ),
        )

        val restored = session(events = listOf(infant)).restore(progress)

        assertEquals("A", restored.selectedChoice!!.choiceId)
        assertEquals(7, restored.lastResult!!.appliedDelta.health)
        assertEquals(after, restored.lastResult!!.afterStats)
    }

    @Test
    fun restore_resolvesEndingForSavedEndingScreen() {
        val progress = GameProgress(
            runId = "saved-run",
            currentEventOccurrenceId = "saved-run:adult_event:1",
            stage = LifeStage.ADULT,
            screenState = GameScreenState.ENDING,
        )

        val restored = session(events = listOf(event(LifeStage.ADULT))).restore(progress)

        assertEquals("ending_default", restored.ending!!.endingId)
        assertNull(restored.currentEvent)
    }

    private fun session(events: List<GameEvent>) = GameSession(
        events = events,
        endings = listOf(
            Ending(
                endingId = "ending_default",
                title = "기본 결말",
                summary = "테스트 결말",
                priority = 0,
                isDefault = true,
            ),
        ),
        runIdFactory = { "test-run" },
    )

    private fun event(stage: LifeStage, healthDelta: Int = 0): GameEvent {
        val stageName = stage.name.lowercase()
        return GameEvent(
            eventId = "${stageName}_event",
            stage = stage,
            title = "테스트 사건",
            text = "선택 결과를 검증한다.",
            imageId = "img_${stageName}_test",
            choices = listOf(
                Choice(
                    choiceId = "A",
                    label = "선택한다.",
                    statDelta = StatDelta(health = healthDelta),
                    resultText = "선택이 반영되었다.",
                ),
            ),
            isFallback = true,
        )
    }
}
