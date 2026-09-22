package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats

class ProgressStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveProgress(progress: GameProgress) {
        preferences.edit()
            .putString(KEY_RUN_ID, progress.runId)
            .putString(KEY_CURRENT_EVENT_OCCURRENCE_ID, progress.currentEventOccurrenceId)
            .putString(KEY_STAGE, progress.stage.name)
            .putStringSet(KEY_FLAGS, progress.flags)
            .putStringSet(KEY_COMPLETED_EVENT_IDS, progress.completedEventIds)
            .putInt(KEY_HEALTH, progress.stats.health)
            .putInt(KEY_FITNESS, progress.stats.fitness)
            .putInt(KEY_INTELLIGENCE, progress.stats.intelligence)
            .putInt(KEY_SOCIAL, progress.stats.social)
            .putInt(KEY_WEALTH, progress.stats.wealth)
            .putInt(KEY_HAPPINESS, progress.stats.happiness)
            .putInt(KEY_LUCK, progress.stats.luck)
            .putInt(KEY_SPECIAL_EVENT_COUNT, progress.specialEventCount)
            .apply()
    }

    fun loadProgress(): GameProgress? {
        val runId = preferences.getString(KEY_RUN_ID, null) ?: return null
        val currentEventOccurrenceId = preferences.getString(KEY_CURRENT_EVENT_OCCURRENCE_ID, null) ?: return null
        val stageName = preferences.getString(KEY_STAGE, LifeStage.INFANT.name) ?: LifeStage.INFANT.name
        val stage = runCatching { LifeStage.valueOf(stageName) }.getOrDefault(LifeStage.INFANT)
        val flags = preferences.getStringSet(KEY_FLAGS, emptySet())?.toSet() ?: emptySet()
        val completedEventIds = preferences.getStringSet(KEY_COMPLETED_EVENT_IDS, emptySet())?.toSet() ?: emptySet()
        val stats = Stats(
            health = preferences.getInt(KEY_HEALTH, Stats.DEFAULT_STAT),
            fitness = preferences.getInt(KEY_FITNESS, Stats.DEFAULT_STAT),
            intelligence = preferences.getInt(KEY_INTELLIGENCE, Stats.DEFAULT_STAT),
            social = preferences.getInt(KEY_SOCIAL, Stats.DEFAULT_STAT),
            wealth = preferences.getInt(KEY_WEALTH, Stats.DEFAULT_STAT),
            happiness = preferences.getInt(KEY_HAPPINESS, Stats.DEFAULT_STAT),
            luck = preferences.getInt(KEY_LUCK, Stats.DEFAULT_STAT),
        )

        return GameProgress(
            runId = runId,
            stage = stage,
            currentEventOccurrenceId = currentEventOccurrenceId,
            stats = stats,
            flags = flags,
            completedEventIds = completedEventIds,
            specialEventCount = preferences.getInt(KEY_SPECIAL_EVENT_COUNT, 0),
        )
    }

    fun clearProgress() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "lifegame_progress"
        const val KEY_RUN_ID = "runId"
        const val KEY_CURRENT_EVENT_OCCURRENCE_ID = "currentEventOccurrenceId"
        const val KEY_STAGE = "stage"
        const val KEY_FLAGS = "flags"
        const val KEY_COMPLETED_EVENT_IDS = "completedEventIds"
        const val KEY_HEALTH = "health"
        const val KEY_FITNESS = "fitness"
        const val KEY_INTELLIGENCE = "intelligence"
        const val KEY_SOCIAL = "social"
        const val KEY_WEALTH = "wealth"
        const val KEY_HAPPINESS = "happiness"
        const val KEY_LUCK = "luck"
        const val KEY_SPECIAL_EVENT_COUNT = "specialEventCount"
    }
}

