/**
 * Area: lobby2 (새 칭호 알림 점).
 *
 * Which earned titles the player has already seen in the title collection. The lobby's 칭호 수집 button shows a gold
 * dot while an earned title is unseen; opening the collection shows those titles as '새로 받음' once and marks them
 * seen. UI-only: backed by its own SharedPreferences file 'lifegame_title_seen' (tests delete it), reading
 * `completedLives` without changing anything under domain/ or data/ or in the ViewModel.
 *
 * Frozen contract — LifeGameApp.kt (integrator) calls these, so the signatures stay as they are:
 * - `@Stable internal class TitleSeenState`
 * - `@Composable internal fun rememberTitleSeenState(): TitleSeenState` — once, inside LifeGameApp's Box, above the
 *   NavHost, so HOME and TITLES share one state.
 * - `@Composable internal fun rememberHasNewTitles(state, lives): Boolean` — HOME passes it to HomeScreen(hasNewTitles = …).
 *   The first lobby after this feature arrived seeds the store with every title earned so far, so an existing player
 *   never sees a spurious dot.
 * - `@Composable internal fun rememberNewTitleIds(state, lives): Set<String>` — TITLES passes it to
 *   TitleCollectionScreen(newTitleIds = …): the unseen titles are snapshotted for this visit (they survive rotation),
 *   then marked seen.
 */
package com.example.lifegame.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.lifegame.domain.model.CompletedLifeRecord

/** The ids of [earned] titles not yet [seen]; nothing is new before the store was ever seeded (null). */
internal fun titleSeenUnseenIds(earned: Set<String>, seen: Set<String>?): Set<String> =
    if (seen == null) emptySet() else earned - seen

/** Every title id the completed lives earned. */
internal fun titleSeenEarnedIds(lives: List<CompletedLifeRecord>): Set<String> =
    lives.mapNotNullTo(mutableSetOf()) { it.awardedTitle?.titleId }

/** The seen-title ids, in their own preference file so no other preference listener fires. */
internal class TitleSeenStore(context: Context) {
    val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    /** Null until the store was first seeded (or if its value is unreadable: then it is simply seeded again). */
    fun read(): Set<String>? = runCatching {
        if (preferences.contains(SEEN_KEY)) preferences.getStringSet(SEEN_KEY, emptySet()).orEmpty().toSet() else null
    }.getOrNull()

    fun write(seen: Set<String>) {
        // A fresh copy: SharedPreferences must never hold a set the caller may change.
        preferences.edit().putStringSet(SEEN_KEY, HashSet(seen)).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "lifegame_title_seen"
        const val SEEN_KEY = "seen_title_ids"
    }
}

/** Which earned titles the player has already seen in the title collection (shared by HOME and TITLES). */
@Stable
internal class TitleSeenState(private val store: TitleSeenStore?) {
    /** Null until seeded: then no title counts as new. */
    private var seen by mutableStateOf(store?.read())

    fun unseen(earned: Set<String>): Set<String> = titleSeenUnseenIds(earned, seen)

    /** The first lobby seeds the store with everything earned so far (existing players get no spurious dot). */
    fun ensureSeeded(earned: Set<String>) {
        if (seen == null) commit(earned)
    }

    /** The collection was opened: every earned title is seen now. */
    fun markSeen(earned: Set<String>) {
        val current = seen
        if (current == null || !current.containsAll(earned)) commit(current.orEmpty() + earned)
    }

    private fun commit(value: Set<String>) {
        seen = value
        store?.write(value)
    }
}

@Composable
internal fun rememberTitleSeenState(): TitleSeenState {
    val context = LocalContext.current
    return remember(context) { TitleSeenState(TitleSeenStore(context)) }
}

/** Whether the lobby's 칭호 수집 button shows the new-title dot. Seeds the store on the first lobby. */
@Composable
internal fun rememberHasNewTitles(state: TitleSeenState, lives: List<CompletedLifeRecord>): Boolean {
    val earned = remember(lives) { titleSeenEarnedIds(lives) }
    LaunchedEffect(state, earned) { state.ensureSeeded(earned) }
    return state.unseen(earned).isNotEmpty()
}

/**
 * Titles shown as '새로 받음' in this visit of the title collection: the unseen ones when the collection opened
 * (kept through rotation), after which every earned title is marked seen and the lobby dot goes out.
 */
@Composable
internal fun rememberNewTitleIds(state: TitleSeenState, lives: List<CompletedLifeRecord>): Set<String> {
    val earned = remember(lives) { titleSeenEarnedIds(lives) }
    val snapshot = rememberSaveable(saver = TitleSeenIdsSaver) { state.unseen(earned) }
    LaunchedEffect(state, earned) { state.markSeen(earned) }
    return snapshot
}

private val TitleSeenIdsSaver = listSaver<Set<String>, String>(save = { it.toList() }, restore = { it.toSet() })
