package com.example.lifegame.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class Week11StateContractsTest {

    @Test fun imageExposureAcknowledgementDoesNotChangeEngineState() {
        val engineState = state()
        val pending = PresentationState().requestEventImage("adult_012")
        val shown = pending.markEventImageShown("adult_012")

        assertEquals(EventImageExposureStatus.SHOWN, shown.eventImageExposure)
        assertSame(engineState, engineState)
    }

    @Test fun acknowledgementForAnotherEventIsIgnored() {
        val pending = PresentationState().requestEventImage("adult_012")
        assertSame(pending, pending.markEventImageShown("adult_017"))
    }

    @Test fun sameEventAlreadyShownDoesNotBecomePendingAfterRestore() {
        val shown = PresentationState().requestEventImage("adult_012").markEventImageShown("adult_012")
        assertSame(shown, shown.requestEventImage("adult_012"))
    }

    @Test fun resultAnimationIsIdempotentAndPresentationOnly() {
        val first = PresentationState().requestResultAnimation("run:event:choice:outcome")
        val duplicate = first.requestResultAnimation("run:event:choice:outcome")
        val consumed = duplicate.consumeResultAnimation("run:event:choice:outcome")

        assertSame(first, duplicate)
        assertEquals(ResultAnimationStatus.CONSUMED, consumed.resultAnimation)
    }

    @Test fun repeatedNarrativeRecordIsStoredOnce() {
        val record = yearly("run:year:1")
        val records = emptyList<RunNarrativeRecord>().appendOnce(record).appendOnce(record)
        assertEquals(listOf(record), records)
    }

    @Test fun automaticStoryAndChoiceResultRemainDifferentKinds() {
        val yearly = yearly("year")
        val choice = RunNarrativeRecord(
            recordId = "choice",
            kind = RunNarrativeKind.CHOICE_RESULT,
            period = LifePeriod.YOUNG_ADULT,
            yearInPeriod = 2,
            text = "제안을 거절했다.",
            eventId = "adult_002",
            choiceId = "B",
        )
        val records = emptyList<RunNarrativeRecord>().appendOnce(yearly).appendOnce(choice)

        assertEquals(listOf(RunNarrativeKind.YEARLY_STORY, RunNarrativeKind.CHOICE_RESULT), records.map { it.kind })
        assertEquals("adult_002", records.last().eventId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun choiceResultCannotInventMissingSourceIds() {
        RunNarrativeRecord(
            recordId = "bad",
            kind = RunNarrativeKind.CHOICE_RESULT,
            period = LifePeriod.YOUNG_ADULT,
            yearInPeriod = 1,
            text = "출처 없는 결과",
        )
    }

    private fun yearly(id: String) = RunNarrativeRecord(
        recordId = id,
        kind = RunNarrativeKind.YEARLY_STORY,
        period = LifePeriod.YOUNG_ADULT,
        yearInPeriod = 1,
        text = "익숙한 길을 천천히 걸었다.",
    )

    private fun state() = PeriodRunState("run", 11L, LifePeriod.YOUNG_ADULT)
}
