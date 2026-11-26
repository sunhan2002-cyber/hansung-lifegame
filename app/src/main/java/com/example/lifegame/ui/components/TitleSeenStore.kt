/**
 * Area: lobby2 (새 칭호 알림 점). Integrator stub written before the stage-2 branches.
 *
 * Frozen contract — LifeGameApp.kt (integrator) calls these, so the signatures stay as they are (bodies change
 * freely; overloads and new declarations may be added):
 * - `@Stable internal class TitleSeenState`
 * - `@Composable internal fun rememberTitleSeenState(): TitleSeenState` — once, inside LifeGameApp's Box, above the
 *   NavHost, so HOME and TITLES share one state.
 * - `@Composable internal fun rememberHasNewTitles(state, lives): Boolean` — HOME passes it to
 *   HomeScreen(hasNewTitles = …). Stub: false.
 * - `@Composable internal fun rememberNewTitleIds(state, lives): Set<String>` — TITLES passes it to
 *   TitleCollectionScreen(newTitleIds = …). Stub: empty.
 * `lives` is `gameViewModel.completedLives` (read only). Seeding, the snapshot of new ids and markSeen run inside these
 * functions, backed by the UI-only SharedPreferences file 'lifegame_title_seen'; nothing under domain/ or data/ and
 * nothing in the ViewModel changes.
 *
 * Stub: nothing is stored or read; no title is ever new.
 */
package com.example.lifegame.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.example.lifegame.domain.model.CompletedLifeRecord

/** Which earned titles the player has already seen in the title collection. Stub: empty. */
@Stable
internal class TitleSeenState

@Composable
internal fun rememberTitleSeenState(): TitleSeenState = remember { TitleSeenState() }

/** Whether the lobby's 칭호 수집 button shows the new-title dot. Stub: false. */
@Composable
internal fun rememberHasNewTitles(state: TitleSeenState, lives: List<CompletedLifeRecord>): Boolean = false

/** Titles shown as '새로 받음' in this visit of the title collection. Stub: none. */
@Composable
internal fun rememberNewTitleIds(state: TitleSeenState, lives: List<CompletedLifeRecord>): Set<String> = emptySet()
