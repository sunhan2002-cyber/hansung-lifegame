package com.example.lifegame.data

import com.example.lifegame.domain.model.Stats
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class Week10ContentScaleTest {
    @Test fun oneThousandEventsLoadWithoutChangingProductionAssets() {
        val sample = JSONArray(File("src/main/assets/events.sample.json").readText()).getJSONObject(0)
        val fixture = JSONArray()
        repeat(1000) { i -> fixture.put(JSONObject(sample.toString()).put("eventId", "qa_scale_$i")) }
        val parsed = V4ContentParser.parseDecisionEvents(fixture.toString())
        assertEquals(1000, parsed.size)
        assertEquals(1000, parsed.map { it.eventId }.toSet().size)
        assertTrue(parsed.all { it.choices.size == sample.getJSONArray("choices").length() })
    }

    @Test fun everyAuthoredMinimumStatGateHonorsItsExactBoundary() {
        val events = V4ContentParser.parseDecisionEvents(File("src/main/assets/events.sample.json").readText())
        var checked = 0
        events.forEach { event ->
            event.choices.forEach { choice ->
                val c = choice.statConditions
                val minima = listOf(c.minHealth, c.minFitness, c.minIntelligence, c.minSocial, c.minWealth, c.minHappiness, c.minLuck)
                minima.forEachIndexed { index, threshold ->
                    if (threshold != null && threshold > 0) {
                        fun stats(value: Int): Stats {
                            val v = MutableList(7) { 100 }.apply { this[index] = value }
                            return Stats(v[0], v[1], v[2], v[3], v[4], v[5], v[6])
                        }
                        assertTrue("${event.eventId}:${choice.choiceId}:$index exact threshold", c.matches(stats(threshold)))
                        assertFalse("${event.eventId}:${choice.choiceId}:$index below threshold", c.matches(stats(threshold - 1)))
                        checked++
                    }
                }
            }
        }
        assertTrue("Actual content must exercise at least one stat gate", checked > 0)
    }
}
