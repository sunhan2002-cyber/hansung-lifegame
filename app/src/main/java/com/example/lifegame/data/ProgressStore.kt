package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats
import org.json.JSONArray
import org.json.JSONObject

class ProgressStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveProgress(progress: GameProgress) {
        preferences.edit()
            .putString(KEY_RUN_ID, progress.runId)
            .putString(KEY_CONTENT_VERSION, progress.contentVersion)
            .putInt(KEY_SAVE_FORMAT_VERSION, progress.saveFormatVersion)
            .putString(KEY_SELECTED_IMAGE_REF, progress.selectedImageRef)
            .putString(KEY_CURRENT_EVENT_OCCURRENCE_ID, progress.currentEventOccurrenceId)
            .putString(KEY_SCREEN_STATE, progress.screenState.name)
            .putString(KEY_STAGE, progress.stage.name)
            .putStringSet(KEY_FLAGS, progress.flags)
            .putStringSet(KEY_COMPLETED_EVENT_IDS, progress.completedEventIds)
            .putStringSet(KEY_HANDLED_OCCURRENCE_IDS, progress.handledOccurrenceIds)
            .putString(KEY_CHOICE_HISTORY, progress.choiceHistory.toJsonString())
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

    fun loadProgress(): GameProgress? = runCatching {
        val runId = preferences.getString(KEY_RUN_ID, null) ?: return null
        val currentEventOccurrenceId = preferences.getString(KEY_CURRENT_EVENT_OCCURRENCE_ID, null) ?: return null
        val stageName = preferences.getString(KEY_STAGE, LifeStage.INFANT.name) ?: LifeStage.INFANT.name
        val screenStateName = preferences.getString(KEY_SCREEN_STATE, GameScreenState.EVENT.name) ?: GameScreenState.EVENT.name
        val flags = preferences.getStringSet(KEY_FLAGS, emptySet())?.toSet() ?: emptySet()
        val completedEventIds = preferences.getStringSet(KEY_COMPLETED_EVENT_IDS, emptySet())?.toSet() ?: emptySet()
        val handledOccurrenceIds = preferences.getStringSet(KEY_HANDLED_OCCURRENCE_IDS, emptySet())?.toSet() ?: emptySet()
        val stats = Stats(
            health = preferences.getInt(KEY_HEALTH, Stats.DEFAULT_STAT),
            fitness = preferences.getInt(KEY_FITNESS, Stats.DEFAULT_STAT),
            intelligence = preferences.getInt(KEY_INTELLIGENCE, Stats.DEFAULT_STAT),
            social = preferences.getInt(KEY_SOCIAL, Stats.DEFAULT_STAT),
            wealth = preferences.getInt(KEY_WEALTH, Stats.DEFAULT_STAT),
            happiness = preferences.getInt(KEY_HAPPINESS, Stats.DEFAULT_STAT),
            luck = preferences.getInt(KEY_LUCK, Stats.DEFAULT_STAT),
        )

        GameProgress(
            runId = runId,
            contentVersion = preferences.getString(KEY_CONTENT_VERSION, "1") ?: "1",
            saveFormatVersion = preferences.getInt(KEY_SAVE_FORMAT_VERSION, 1),
            selectedImageRef = preferences.getString(KEY_SELECTED_IMAGE_REF, null),
            stage = stageName.toLifeStageOrDefault(),
            currentEventOccurrenceId = currentEventOccurrenceId,
            screenState = screenStateName.toGameScreenStateOrDefault(),
            stats = stats,
            flags = flags,
            completedEventIds = completedEventIds,
            handledOccurrenceIds = handledOccurrenceIds,
            choiceHistory = preferences.getString(KEY_CHOICE_HISTORY, null).toChoiceHistoryList(),
            specialEventCount = preferences.getInt(KEY_SPECIAL_EVENT_COUNT, 0),
        )
    }.getOrNull()

    fun clearProgress() {
        preferences.edit().clear().apply()
    }

    private fun String.toLifeStageOrDefault(): LifeStage =
        runCatching { LifeStage.valueOf(this) }.getOrDefault(LifeStage.INFANT)

    private fun String.toGameScreenStateOrDefault(): GameScreenState =
        runCatching { GameScreenState.valueOf(this) }.getOrDefault(GameScreenState.EVENT)

    private fun List<ChoiceHistory>.toJsonString(): String {
        val array = JSONArray()
        forEach { history ->
            array.put(
                JSONObject()
                    .put("eventOccurrenceId", history.eventOccurrenceId)
                    .put("eventId", history.eventId)
                    .put("choiceId", history.choiceId)
                    .put("beforeStats", history.beforeStats.toJson())
                    .put("afterStats", history.afterStats.toJson())
                    .put("resultText", history.resultText),
            )
        }
        return array.toString()
    }

    private fun String?.toChoiceHistoryList(): List<ChoiceHistory> = runCatching {
        if (isNullOrBlank()) return emptyList()
        val array = JSONArray(this)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ChoiceHistory(
                eventOccurrenceId = item.getString("eventOccurrenceId"),
                eventId = item.getString("eventId"),
                choiceId = item.getString("choiceId"),
                beforeStats = item.getJSONObject("beforeStats").toStats(),
                afterStats = item.getJSONObject("afterStats").toStats(),
                resultText = item.getString("resultText"),
            )
        }
    }.getOrDefault(emptyList())

    private fun Stats.toJson(): JSONObject = JSONObject()
        .put("health", health)
        .put("fitness", fitness)
        .put("intelligence", intelligence)
        .put("social", social)
        .put("wealth", wealth)
        .put("happiness", happiness)
        .put("luck", luck)

    private fun JSONObject.toStats(): Stats = Stats(
        health = optInt("health", Stats.DEFAULT_STAT),
        fitness = optInt("fitness", Stats.DEFAULT_STAT),
        intelligence = optInt("intelligence", Stats.DEFAULT_STAT),
        social = optInt("social", Stats.DEFAULT_STAT),
        wealth = optInt("wealth", Stats.DEFAULT_STAT),
        happiness = optInt("happiness", Stats.DEFAULT_STAT),
        luck = optInt("luck", Stats.DEFAULT_STAT),
    )

    private companion object {
        const val PREFERENCES_NAME = "lifegame_progress"
        const val KEY_RUN_ID = "runId"
        const val KEY_CONTENT_VERSION = "contentVersion"
        const val KEY_SAVE_FORMAT_VERSION = "saveFormatVersion"
        const val KEY_SELECTED_IMAGE_REF = "selectedImageRef"
        const val KEY_CURRENT_EVENT_OCCURRENCE_ID = "currentEventOccurrenceId"
        const val KEY_SCREEN_STATE = "screenState"
        const val KEY_STAGE = "stage"
        const val KEY_FLAGS = "flags"
        const val KEY_COMPLETED_EVENT_IDS = "completedEventIds"
        const val KEY_HANDLED_OCCURRENCE_IDS = "handledOccurrenceIds"
        const val KEY_CHOICE_HISTORY = "choiceHistory"
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
