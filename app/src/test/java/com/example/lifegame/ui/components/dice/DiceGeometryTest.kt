package com.example.lifegame.ui.components.dice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiceGeometryTest {
    @Test fun everyRequestedFaceEndsFacingTheViewer() {
        (1..6).forEach { value ->
            val normal = DiceGeometry.rotatedFrontNormal(value)
            assertEquals("face $value x", 0f, normal[0], .0001f)
            assertEquals("face $value y", 0f, normal[1], .0001f)
            assertEquals("face $value z", 1f, normal[2], .0001f)
        }
    }

    @Test fun animationEndsAtExactlyTheSuppliedValueAndNeverOvershootsItsLifetime() {
        (1..6).forEach { value ->
            listOf(false, true).forEach { second ->
                val expected = DiceGeometry.Pose(DiceGeometry.finalRotation(value), 0f, 1f)
                assertEquals(expected, DiceGeometry.pose(value, 490, true, second))
                assertEquals(expected, DiceGeometry.pose(value, 20_000, true, second))
                assertEquals(expected, DiceGeometry.pose(value, 0, false, second))
                assertTrue(DiceGeometry.pose(value, 245, true, second).lift > 0f)
            }
        }
    }

    @Test fun oppositeFacesSumToSeven() {
        (1..3).forEach { value ->
            val first = DiceGeometry.frontNormal(value)
            val opposite = DiceGeometry.frontNormal(7 - value)
            (0..2).forEach { axis -> assertEquals(-first[axis], opposite[axis], 0f) }
        }
    }
}
