package com.example.lifegame.domain.interaction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrativeInputGateTest {
    @Test
    fun tapWhileTypingCompletesTextAndIsConsumed() {
        val result = NarrativeInputState(displayedChars = 4, totalChars = 20).consumeTap()

        assertEquals(20, result.state.displayedChars)
        assertTrue(result.state.isComplete)
        assertSame(NarrativeTapAction.COMPLETE_TEXT, result.action)
        assertFalse(result.action == NarrativeTapAction.FORWARD)
    }

    @Test
    fun tapAfterCompletionMayBeForwardedByUi() {
        val complete = NarrativeInputState(displayedChars = 20, totalChars = 20)
        val result = complete.consumeTap()

        assertEquals(complete, result.state)
        assertSame(NarrativeTapAction.FORWARD, result.action)
    }

    @Test
    fun emptyNarrativeIsAlreadyComplete() {
        val result = NarrativeInputState(displayedChars = 0, totalChars = 0).consumeTap()

        assertTrue(result.state.isComplete)
        assertSame(NarrativeTapAction.FORWARD, result.action)
    }
}
