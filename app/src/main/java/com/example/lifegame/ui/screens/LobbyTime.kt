/**
 * Area: lobby2 (시간대 로비). Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — LifeGameApp.kt (integrator) and HomeScreen.kt call these, so the signatures stay as they are
 * (bodies change freely; overloads and new declarations may be added):
 * - PUBLIC `fun interface LobbyClock { fun hourOfDay(): Float }` with `companion object { const val NEUTRAL_HOUR = 13f;
 *   val System: LobbyClock; fun fixed(hour: Float): LobbyClock }` — public because the public LifeGameApp takes
 *   `lobbyClock: LobbyClock = LobbyClock.System`. Hours run 0f until 24f, device local time.
 * - `@Composable internal fun rememberLobbyHour(clock: LobbyClock): Float` — LifeGameApp passes its result to
 *   HomeScreen(hourOfDay = …). Stub: always [LobbyClock.NEUTRAL_HOUR], i.e. today's daytime lobby.
 *
 * HomeScreen's default `hourOfDay = LobbyClock.NEUTRAL_HOUR` (13 h) must keep reproducing today's lobby exactly, so
 * direct HomeScreen tests stay deterministic.
 */
package com.example.lifegame.ui.screens

import androidx.compose.runtime.Composable
import java.time.LocalTime

/** Where the lobby reads the time of day from. Tests and the comparison screen inject [fixed]. */
fun interface LobbyClock {
    /** Local time of day in hours, 0f until 24f (18.5f = 18:30). */
    fun hourOfDay(): Float

    companion object {
        /** Early afternoon: the lobby exactly as it looked before time-of-day skies. */
        const val NEUTRAL_HOUR = 13f

        /** The device's local time. */
        val System: LobbyClock = LobbyClock { LocalTime.now().let { it.hour + it.minute / 60f } }

        fun fixed(hour: Float): LobbyClock = LobbyClock { hour }
    }
}

/** The hour the lobby shows. Stub: always the neutral hour (today's lobby). */
@Composable
internal fun rememberLobbyHour(clock: LobbyClock): Float = LobbyClock.NEUTRAL_HOUR
