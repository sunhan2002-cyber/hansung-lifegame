package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats
import org.json.JSONArray
import org.json.JSONObject

data class SavedGame(
    val characterId: Int,
    val progress: GameProgress,
)

class ProgressStore(
    context: Context,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun hasSaveData(): Boolean = preferences.contains(KEY_SNAPSHOT)

    fun save(savedGame: SavedGame) {
        preferences.edit()
            .putString(KEY_SNAPSHOT, ProgressSnapshotCodec.encode(savedGame))
            .apply()
    }

    fun load(): SavedGame? = preferences
        .getString(KEY_SNAPSHOT, null)
        ?.let(ProgressSnapshotCodec::decode)

    fun clear() {
        preferences.edit().remove(KEY_SNAPSHOT).apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "lifegame_progress"
        const val KEY_SNAPSHOT = "savedGameSnapshot"
    }
}

internal object ProgressSnapshotCodec {
    private const val SUPPORTED_SAVE_FORMAT_VERSION = 1

    fun encode(savedGame: SavedGame): String = JSONObject()
        .put("characterId", savedGame.characterId)
        .put("progress", savedGame.progress.toJson())
        .toString()

    fun decode(raw: String): SavedGame? = runCatching {
        val root = JSONObject(raw)
        val progress = root.getJSONObject("progress").toGameProgress()
        require(progress.saveFormatVersion == SUPPORTED_SAVE_FORMAT_VERSION) {
            "Unsupported save format: ${progress.saveFormatVersion}"
        }
        SavedGame(
            characterId = root.getInt("characterId"),
            progress = progress,
        )
    }.getOrNull()

    private fun GameProgress.toJson(): JSONObject = JSONObject()
        .put("runId", runId)
        .put("contentVersion", contentVersion)
        .put("saveFormatVersion", saveFormatVersion)
        .put("selectedImageRef", selectedImageRef)
        .put("stage", stage.name)
        .put("currentEventOccurrenceId", currentEventOccurrenceId)
        .put("screenState", screenState.name)
        .put("stats", stats.toJson())
        .put("flags", flags.toJsonArray())
        .put("completedEventIds", completedEventIds.toJsonArray())
        .put("handledOccurrenceIds", handledOccurrenceIds.toJsonArray())
        .put("choiceHistory", choiceHistory.toJsonArray())
        .put("specialEventCount", specialEventCount)
        .put("stageEventCount", stageEventCount)

    private fun JSONObject.toGameProgress(): GameProgress = GameProgress(
        runId = getString("runId"),
        contentVersion = optString("contentVersion", "1"),
        saveFormatVersion = getInt("saveFormatVersion"),
        selectedImageRef = optNullableString("selectedImageRef"),
        stage = LifeStage.valueOf(getString("stage")),
        currentEventOccurrenceId = getString("currentEventOccurrenceId"),
        screenState = GameScreenState.valueOf(getString("screenState")),
        stats = getJSONObject("stats").toStats(),
        flags = getJSONArray("flags").toStringSet(),
        completedEventIds = getJSONArray("completedEventIds").toStringSet(),
        handledOccurrenceIds = getJSONArray("handledOccurrenceIds").toStringSet(),
        choiceHistory = getJSONArray("choiceHistory").toChoiceHistoryList(),
        specialEventCount = optInt("specialEventCount", 0),
        stageEventCount = optInt("stageEventCount", 0),
    )

    private fun Stats.toJson(): JSONObject = JSONObject()
        .put("health", health)
        .put("fitness", fitness)
        .put("intelligence", intelligence)
        .put("social", social)
        .put("wealth", wealth)
        .put("happiness", happiness)
        .put("luck", luck)

    private fun JSONObject.toStats(): Stats = Stats(
        health = getInt("health"),
        fitness = getInt("fitness"),
        intelligence = getInt("intelligence"),
        social = getInt("social"),
        wealth = getInt("wealth"),
        happiness = getInt("happiness"),
        luck = getInt("luck"),
    )

    private fun Set<String>.toJsonArray(): JSONArray = JSONArray().also { array ->
        sorted().forEach(array::put)
    }

    private fun JSONArray.toStringSet(): Set<String> = buildSet {
        repeat(length()) { index -> add(getString(index)) }
    }

    private fun List<ChoiceHistory>.toJsonArray(): JSONArray = JSONArray().also { array ->
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
    }

    private fun JSONArray.toChoiceHistoryList(): List<ChoiceHistory> = List(length()) { index ->
        getJSONObject(index).run {
            ChoiceHistory(
                eventOccurrenceId = getString("eventOccurrenceId"),
                eventId = getString("eventId"),
                choiceId = getString("choiceId"),
                beforeStats = getJSONObject("beforeStats").toStats(),
                afterStats = getJSONObject("afterStats").toStats(),
                resultText = getString("resultText"),
            )
        }
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name) else null
}
