package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EventConditions
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.json.JSONObject

class ContentRepository(
    private val context: Context,
) {
    fun loadEvents(): List<GameEvent> = runCatching {
        val array = JSONArray(readAsset(EVENTS_FILE))
        List(array.length()) { index -> array.getJSONObject(index).toGameEvent() }
    }.getOrElse { emptyList() }

    fun loadEndings(): List<Ending> = runCatching {
        val array = JSONArray(readAsset(ENDINGS_FILE))
        List(array.length()) { index -> array.getJSONObject(index).toEnding() }
    }.getOrElse { emptyList() }

    private fun readAsset(fileName: String): String =
        context.assets.open(fileName).bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun JSONObject.toGameEvent(): GameEvent = GameEvent(
        eventId = getString("eventId"),
        stage = getString("stage").toLifeStage(),
        type = optString("type", "NORMAL").toEventType(),
        title = getString("title"),
        text = getString("text"),
        imageId = getString("imageId"),
        conditions = optJSONObject("conditions").toEventConditions(),
        choices = getJSONArray("choices").toChoices(),
        priority = optInt("priority", 0),
        isFallback = optBoolean("isFallback", false),
    )

    private fun JSONObject.toEnding(): Ending = Ending(
        endingId = getString("endingId"),
        title = getString("title"),
        summary = getString("summary"),
        priority = optInt("priority", 0),
        conditions = optJSONObject("conditions").toEndingConditions(),
        isDefault = optBoolean("isDefault", false),
    )

    private fun JSONArray.toChoices(): List<Choice> = List(length()) { index ->
        val item = getJSONObject(index)
        Choice(
            choiceId = item.getString("choiceId"),
            label = item.getString("label"),
            statDelta = item.optJSONObject("statDelta").toStatDelta(),
            resultText = item.getString("resultText"),
            addFlags = item.optJSONArray("addFlags").toStringSet(),
        )
    }

    private fun JSONObject?.toEventConditions(): EventConditions = EventConditions(
        stats = this?.optJSONObject("stats").toStatConditions(),
        requiredFlags = this?.optJSONArray("requiredFlags").toStringSet(),
        blockedFlags = this?.optJSONArray("blockedFlags").toStringSet(),
    )

    private fun JSONObject?.toEndingConditions(): EndingConditions = EndingConditions(
        stats = this?.optJSONObject("minStats").toStatConditions(),
        requiredFlags = this?.optJSONArray("requiredFlags").toStringSet(),
        blockedFlags = this?.optJSONArray("blockedFlags").toStringSet(),
    )

    private fun JSONObject?.toStatConditions(): StatConditions = StatConditions(
        minHealth = this?.optNullableInt("건강") ?: this?.optNullableInt("health"),
        minFitness = this?.optNullableInt("운동능력") ?: this?.optNullableInt("fitness"),
        minIntelligence = this?.optNullableInt("지력") ?: this?.optNullableInt("intelligence"),
        minSocial = this?.optNullableInt("사회성") ?: this?.optNullableInt("social"),
        minWealth = this?.optNullableInt("경제력") ?: this?.optNullableInt("wealth"),
        minHappiness = this?.optNullableInt("행복") ?: this?.optNullableInt("happiness"),
        minLuck = this?.optNullableInt("운") ?: this?.optNullableInt("luck"),
    )

    private fun JSONObject?.toStatDelta(): StatDelta = StatDelta(
        health = this?.optInt("건강", 0) ?: this?.optInt("health", 0) ?: 0,
        fitness = this?.optInt("운동능력", 0) ?: this?.optInt("fitness", 0) ?: 0,
        intelligence = this?.optInt("지력", 0) ?: this?.optInt("intelligence", 0) ?: 0,
        social = this?.optInt("사회성", 0) ?: this?.optInt("social", 0) ?: 0,
        wealth = this?.optInt("경제력", 0) ?: this?.optInt("wealth", 0) ?: 0,
        happiness = this?.optInt("행복", 0) ?: this?.optInt("happiness", 0) ?: 0,
        luck = this?.optInt("운", 0) ?: this?.optInt("luck", 0) ?: 0,
    )

    private fun JSONArray?.toStringSet(): Set<String> =
        if (this == null) emptySet() else List(length()) { index -> getString(index) }.toSet()

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun String.toLifeStage(): LifeStage = when (uppercase()) {
        "INFANT", "유아기" -> LifeStage.INFANT
        "CHILD", "아동기" -> LifeStage.CHILD
        "TEEN", "청소년기" -> LifeStage.TEEN
        "ADULT", "성인기" -> LifeStage.ADULT
        else -> LifeStage.CHILD
    }

    private fun String.toEventType(): EventType = when (uppercase()) {
        "SPECIAL", "돌발" -> EventType.SPECIAL
        else -> EventType.NORMAL
    }

    private companion object {
        const val EVENTS_FILE = "events.sample.json"
        const val ENDINGS_FILE = "endings.sample.json"
    }
}
