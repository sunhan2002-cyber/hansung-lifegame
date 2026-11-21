package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.StatKey
import com.example.lifegame.ui.StartingRoll
import org.junit.Assert.*
import org.junit.Test

class SimulationParityTest {
    @Test fun seededProfileContainsTheActualDiceStarterAndGenderInputs() {
        val male = Week10SimulationRunner.profileFor(20261000L)
        val female = Week10SimulationRunner.profileFor(20261001L)
        assertEquals(male, Week10SimulationRunner.profileFor(20261000L))
        assertEquals(male.rolledDelta, StartingRoll.decode(male.startingRollRaw)?.delta)
        assertNotNull(male.starterId)
        assertTrue(male.starterFlags.isNotEmpty())
        assertTrue("military_service_required" in male.initialFlags)
        assertFalse("military_service_required" in female.initialFlags)
    }

    @Test fun completeAuthoredLifeReplaysAutomaticEventsAndChoicesWithoutRestoreViolations() {
        val first = Week10SimulationRunner.simulate("TARGET", StatKey.INTELLIGENCE, 20261000L)
        val replay = Week10SimulationRunner.simulate("TARGET", StatKey.INTELLIGENCE, 20261000L)
        assertEquals(first.toString(), replay.toString())
        assertEquals(first.getJSONArray("violations").toString(), 0, first.getJSONArray("violations").length())
        assertTrue(first.getJSONArray("automaticTrace").length() > 0)
        assertEquals("gender_male", first.getJSONObject("startProfile").getString("genderFlag"))
        assertTrue(first.getJSONObject("targetPathDiagnostics").has("ending_path_int_researcher"))
    }

    @Test fun boundaryRunIsExplicitlyMarkedAsSynthetic() {
        val run = Week10SimulationRunner.simulate("BOUNDARY", StatKey.HEALTH, 20262000L)
        assertFalse(run.getJSONObject("startProfile").isNull("syntheticStatsOverride"))
        assertEquals(run.getJSONArray("violations").toString(), 0, run.getJSONArray("violations").length())
    }
}
