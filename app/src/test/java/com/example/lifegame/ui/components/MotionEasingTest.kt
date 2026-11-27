package com.example.lifegame.ui.components

import androidx.compose.animation.core.CubicBezierEasing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Compose 1.7's CubicBezierEasing throws for some curves when a tween's fraction lands a hair below 1, which crashed
 * the app while the 연출 비교 lobby preview replayed (2026-11-27). Every curve we define goes through
 * [safeCubicBezierEasing]; these sweeps keep it that way.
 */
class MotionEasingTest {
    /** Every float in the last stretch below 1 (where the solver fails), plus an even grid over the whole range. */
    private val fractions: List<Float> = buildList {
        for (i in 1 until 100_000) add(i / 100_000f)
        var f = 1f
        repeat(5_000) {
            f = Math.nextDown(f)
            add(f)
        }
    }

    /** The parameters of every curve the motion code defines (MotionKit, play, time, lobby2, bookends). */
    private val curves = listOf(
        floatArrayOf(0.18f, 0.9f, 0.24f, 1f), // RiseEasing — threw at 0.99999994
        floatArrayOf(0.34f, 1.4f, 0.64f, 1f), // title-unlock flip — threw at 0.99999994
        floatArrayOf(0.2f, 1.25f, 0.4f, 1f), // profile unfold
        floatArrayOf(0.25f, 0.1f, 0.25f, 1f), // choice ink
        floatArrayOf(0.55f, 0.085f, 0.68f, 0.53f), // title-unlock fall
        floatArrayOf(0.15f, 0.6f, 0.25f, 1f), // title-unlock spin
        floatArrayOf(0.6f, 0f, 0.9f, 0.4f), // title-unlock stamp down
        floatArrayOf(0.5f, 0f, 0.25f, 1f), // page turn
    )

    @Test fun riseEasingNeverThrowsAndEndsExactly() {
        fractions.forEach { assertTrue(RiseEasing.transform(it).isFinite()) }
        assertEquals(0f, RiseEasing.transform(0f), 0f)
        assertEquals(1f, RiseEasing.transform(1f), 0f)
    }

    @Test fun everyCurveWeDefineIsSafeAndMatchesComposeWhereComposeSolves() {
        for ((a, b, c, d) in curves) {
            val safe = safeCubicBezierEasing(a, b, c, d)
            val compose = CubicBezierEasing(a, b, c, d)
            for (f in fractions) {
                val value = safe.transform(f)
                assertTrue("($a, $b, $c, $d) at $f", value.isFinite())
                val expected = runCatching { compose.transform(f) }.getOrNull() ?: continue
                assertEquals("($a, $b, $c, $d) at $f", expected, value, 0f)
            }
            assertEquals(0f, safe.transform(0f), 0f)
            assertEquals(1f, safe.transform(1f), 0f)
        }
    }

    @Test fun aFailedSolveFallsBackToTheFractionItself() {
        // RiseEasing's raw curve cannot be solved here; the safe one returns the fraction (the curve is at ~1 there).
        val f = 0.99999994f
        assertTrue(runCatching { CubicBezierEasing(0.18f, 0.9f, 0.24f, 1f).transform(f) }.isFailure)
        assertEquals(f, RiseEasing.transform(f), 0f)
    }
}
