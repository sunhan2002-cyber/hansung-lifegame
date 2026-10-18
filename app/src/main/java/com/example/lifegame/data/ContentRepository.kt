package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EndingTier
import com.example.lifegame.domain.model.EventConditions
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.ImageAsset
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.json.JSONObject

class ContentRepository(
    private val context: Context,
) {
    fun loadPeriodDefinitions(): List<PeriodDefinition> = runCatching {
        V4ContentParser.parsePeriods(readAsset(PERIODS_FILE))
    }.getOrElse { emptyList() }

    fun loadFocuses(): List<Focus> = runCatching {
        V4ContentParser.parseFocuses(readAsset(FOCUSES_FILE))
    }.getOrElse { emptyList() }

    fun loadDecisionEvents(): List<DecisionEvent> = runCatching {
        V4ContentParser.parseDecisionEvents(readAsset(EVENTS_FILE))
    }.getOrElse { emptyList() }

    /** 시나리오 PR의 플래그→인생 사실 카탈로그. 병합 전에는 빈 목록으로 안전하게 동작한다. */
    fun loadLifeFactRules() = runCatching {
        V4ContentParser.parseLifeFactRules(readAsset(LIFE_FACTS_FILE))
    }.getOrElse { emptyList() }

    fun loadV4Content(): V4Content = V4Content(
        periods = loadPeriodDefinitions(),
        focuses = loadFocuses(),
        decisions = loadDecisionEvents(),
        lifeFactRules = loadLifeFactRules(),
    )

    fun loadEvents(): List<GameEvent> = runCatching {
        val array = JSONArray(readAsset(EVENTS_FILE))
        List(array.length()) { index -> array.getJSONObject(index).toGameEvent() }
    }.getOrElse { emptyList() }

    fun loadEndings(): List<Ending> = runCatching {
        V4ContentParser.parseEndings(readAsset(ENDINGS_FILE))
    }.getOrElse { emptyList() }

    fun loadImageAssets(): List<ImageAsset> = runCatching {
        val array = JSONArray(readAsset(IMAGE_IDS_FILE))
        List(array.length()) { index -> array.getJSONObject(index).toImageAsset() }
    }.getOrElse { emptyList() }

    fun findImageAsset(imageId: String): ImageAsset? =
        loadImageAssets().firstOrNull { it.imageId == imageId }

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
        tier = optString("tier").takeIf { it.isNotBlank() }
            ?.let { runCatching { EndingTier.valueOf(it.uppercase()) }.getOrNull() }
            ?: if (optBoolean("isDefault", false)) EndingTier.DEFAULT else EndingTier.GENERAL_LIFE,
    )

    private fun JSONObject.toImageAsset(): ImageAsset = ImageAsset(
        imageId = getString("imageId"),
        usage = optString("usage", "event"),
        stage = optString("stage", ""),
        description = optString("description", ""),
        fileName = optString("fileName", ""),
        fallbackText = optString("fallbackText", optString("description", "")),
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
        lifeFacts = this?.optJSONObject("lifeFacts").toLifeFactConditions(),
    )

    private fun JSONObject?.toLifeFactConditions(): LifeFactConditions = LifeFactConditions(
        educationLevel = this?.optString("educationLevel")?.takeIf { it.isNotBlank() },
        fieldOfStudy = this?.optString("fieldOfStudy")?.takeIf { it.isNotBlank() },
        careerTrack = this?.optString("careerTrack")?.takeIf { it.isNotBlank() },
        careerLevel = this?.optString("careerLevel")?.takeIf { it.isNotBlank() },
        employmentStatus = this?.optString("employmentStatus")?.takeIf { it.isNotBlank() },
        maritalStatus = this?.optString("maritalStatus")?.takeIf { it.isNotBlank() },
        minChildrenCount = this?.optNullableInt("minChildrenCount"),
        housingType = this?.optString("housingType")?.takeIf { it.isNotBlank() },
        wealthBand = this?.optString("wealthBand")?.takeIf { it.isNotBlank() },
        requiredAchievements = this?.optJSONArray("requiredAchievements").toStringSet(),
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
        health = this?.optNullableInt("건강") ?: this?.optNullableInt("health") ?: 0,
        fitness = this?.optNullableInt("운동능력") ?: this?.optNullableInt("fitness") ?: 0,
        intelligence = this?.optNullableInt("지력") ?: this?.optNullableInt("intelligence") ?: 0,
        social = this?.optNullableInt("사회성") ?: this?.optNullableInt("social") ?: 0,
        wealth = this?.optNullableInt("경제력") ?: this?.optNullableInt("wealth") ?: 0,
        happiness = this?.optNullableInt("행복") ?: this?.optNullableInt("happiness") ?: 0,
        luck = this?.optNullableInt("운") ?: this?.optNullableInt("luck") ?: 0,
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
        const val PERIODS_FILE = "periods.sample.json"
        const val FOCUSES_FILE = "focuses.sample.json"
        const val EVENTS_FILE = "events.sample.json"
        const val ENDINGS_FILE = "endings.sample.json"
        const val IMAGE_IDS_FILE = "image_ids.sample.json"
        const val LIFE_FACTS_FILE = "life_facts.sample.json"
    }
}
