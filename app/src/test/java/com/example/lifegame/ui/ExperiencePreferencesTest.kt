package com.example.lifegame.ui

import com.example.lifegame.ui.components.ExperiencePreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperiencePreferencesTest {
    @Test fun eitherAppOrSystemCanReduceMotionWithoutTurningItBackOn() {
        assertFalse(ExperiencePreferences().reduceMotion(1f))
        assertTrue(ExperiencePreferences().reduceMotion(0f))
        assertTrue(ExperiencePreferences(motionEnabled = false).reduceMotion(1f))
        assertTrue(ExperiencePreferences(motionEnabled = false).reduceMotion(0f))
    }

    @Test fun soundIsOptInAndIndependentOfMotionAndHaptics() {
        val defaults = ExperiencePreferences()
        assertFalse(defaults.soundEnabled)
        assertTrue(defaults.hapticsEnabled)
        assertTrue(defaults.motionEnabled)
        val soundOnly = ExperiencePreferences(motionEnabled = false, hapticsEnabled = false, soundEnabled = true)
        assertTrue(soundOnly.soundEnabled)
        assertTrue(soundOnly.reduceMotion(1f))
    }
}
