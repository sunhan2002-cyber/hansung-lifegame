package com.example.lifegame.data

import com.example.lifegame.domain.engine.AutomaticEventRule
import com.example.lifegame.domain.engine.AutomaticEventStage
import com.example.lifegame.domain.model.AutomaticEvent
import com.example.lifegame.domain.model.AutomaticEventTrigger
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.json.JSONObject

/**
 * 신기훈 `auto_events.sample.json`(#76)을 김우영 [AutomaticEvent]와 표시 조건([AutomaticEventRule])으로 읽는다.
 * Android Context 없이 동작하는 순수 파서다. 읽을 수 없는 항목은 건너뛰고(앱이 멈추지 않게) 나머지만 쓴다.
 */
object AutomaticEventParser {
    fun parse(json: String): List<AutomaticEventRule> =
        JSONObject(json).optJSONArray("events").objects().mapNotNull { item -> runCatching { item.toRule() }.getOrNull() }

    private fun JSONObject.toRule(): AutomaticEventRule {
        val eventId = getString("eventId")
        val trigger = AutomaticEventTrigger.valueOf(getString("trigger").uppercase())
        val rule = optJSONObject("triggerRule") ?: JSONObject()
        val stage = when (trigger) {
            AutomaticEventTrigger.ELAPSED_TIME -> AutomaticEventStage.ELAPSED
            AutomaticEventTrigger.PROGRESS_STAGE -> when (rule.getString("stage").uppercase()) {
                "PERIOD_START" -> AutomaticEventStage.PERIOD_START
                "PERIOD_MID" -> AutomaticEventStage.PERIOD_MID
                "PERIOD_END" -> AutomaticEventStage.PERIOD_END
                "AFTER_EVENT" -> AutomaticEventStage.AFTER_EVENT
                else -> error("unknown stage")
            }
        }
        val recordOnly = optString("statEffect").equals("RECORD_ONLY", ignoreCase = true)
        val conditions = optJSONObject("conditions") ?: JSONObject()
        val stats = conditions.optJSONObject("stats")
        val event = AutomaticEvent(
            eventId = eventId,
            periods = optJSONArray("periods").strings().map { LifePeriod.valueOf(it.uppercase()) }.toSet()
                .also { require(it.isNotEmpty()) { "auto event needs a period" } },
            text = getString("text").also { require(it.isNotBlank()) },
            trigger = trigger,
            // RECORD_ONLY는 콘텐츠에 값이 있어도 능력치를 바꾸지 않는다.
            statDelta = if (recordOnly) StatDelta() else optJSONObject("statDelta").toStatDelta(),
            addFlags = optJSONArray("addFlags").strings().toSet(),
            removeFlags = optJSONArray("removeFlags").strings().toSet(),
            momentImageId = optString("momentImageId").takeIf { it.isNotBlank() && it != "null" },
            repeatable = optBoolean("repeatable", false),
        )
        return AutomaticEventRule(
            event = event,
            topic = optString("topic"),
            stage = stage,
            afterAutoSteps = rule.optInt("afterAutoSteps", 0),
            minYearInPeriod = rule.optInt("minYearInPeriod", 1),
            afterEventId = rule.optString("afterEventId").takeIf { it.isNotBlank() },
            requiredFlags = conditions.optJSONArray("requiredFlags").strings().toSet(),
            blockedFlags = conditions.optJSONArray("blockedFlags").strings().toSet(),
            statConditions = stats.toStatConditions(),
            lifeFactConditions = conditions.optJSONObject("lifeFacts").toLifeFactConditions(),
            dedupeKey = optString("dedupeKey").takeIf { it.isNotBlank() }
                ?: optJSONObject("dedupe")?.optString("key")?.takeIf { it.isNotBlank() }
                ?: eventId,
            recordOnly = recordOnly,
        ).also {
            if (stage == AutomaticEventStage.AFTER_EVENT) require(it.afterEventId != null) { "AFTER_EVENT needs afterEventId" }
        }
    }

    /** 사건과 같은 키 규칙. `{"minStats": {...}, "maxStats": {...}}` 또는 납작한 `{"지력": 40}`(최솟값) 둘 다 읽는다. */
    private fun JSONObject?.toStatConditions(): StatConditions {
        if (this == null || length() == 0) return StatConditions()
        val min = optJSONObject("minStats") ?: if (has("maxStats")) null else this
        val max = optJSONObject("maxStats")
        return StatConditions(
            minHealth = min.stat("건강", "health"),
            minFitness = min.stat("운동능력", "fitness"),
            minIntelligence = min.stat("지력", "intelligence"),
            minSocial = min.stat("사회성", "social"),
            minWealth = min.stat("경제력", "wealth"),
            minHappiness = min.stat("행복", "happiness"),
            minLuck = min.stat("운", "luck"),
            maxHealth = max.stat("건강", "health"),
            maxFitness = max.stat("운동능력", "fitness"),
            maxIntelligence = max.stat("지력", "intelligence"),
            maxSocial = max.stat("사회성", "social"),
            maxWealth = max.stat("경제력", "wealth"),
            maxHappiness = max.stat("행복", "happiness"),
            maxLuck = max.stat("운", "luck"),
        )
    }

    private fun JSONObject?.toLifeFactConditions(): LifeFactConditions = LifeFactConditions(
        educationLevel = this?.optNullableString("educationLevel"),
        fieldOfStudy = this?.optNullableString("fieldOfStudy"),
        careerTrack = this?.optNullableString("careerTrack"),
        careerLevel = this?.optNullableString("careerLevel"),
        employmentStatus = this?.optNullableString("employmentStatus"),
        maritalStatus = this?.optNullableString("maritalStatus"),
        minChildrenCount = this?.stat("minChildrenCount", "minChildrenCount"),
        housingType = this?.optNullableString("housingType"),
        wealthBand = this?.optNullableString("wealthBand"),
        requiredAchievements = this?.optJSONArray("requiredAchievements").strings().toSet(),
    )

    private fun JSONObject?.toStatDelta(): StatDelta = StatDelta(
        health = stat("건강", "health") ?: 0,
        fitness = stat("운동능력", "fitness") ?: 0,
        intelligence = stat("지력", "intelligence") ?: 0,
        social = stat("사회성", "social") ?: 0,
        wealth = stat("경제력", "wealth") ?: 0,
        happiness = stat("행복", "happiness") ?: 0,
        luck = stat("운", "luck") ?: 0,
    )

    private fun JSONObject?.stat(korean: String, english: String): Int? = when {
        this == null -> null
        has(korean) && !isNull(korean) -> getInt(korean)
        has(english) && !isNull(english) -> getInt(english)
        else -> null
    }

    private fun JSONObject.optNullableString(name: String): String? =
        if (has(name) && !isNull(name)) getString(name).takeIf { it.isNotBlank() } else null

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else List(length()) { getJSONObject(it) }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else List(length()) { getString(it) }
}
