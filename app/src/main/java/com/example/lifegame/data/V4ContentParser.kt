package com.example.lifegame.data

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.json.JSONObject

data class V4Content(
    val periods: List<PeriodDefinition>,
    val focuses: List<Focus>,
    val decisions: List<DecisionEvent>,
)

/** PR #26의 v4 JSON을 Android Context 없이도 검증할 수 있는 순수 파서다. */
object V4ContentParser {
    fun parsePeriods(json: String): List<PeriodDefinition> = JSONArray(json).mapObjects { item ->
        PeriodDefinition(
            period = item.getString("periodId").toLifePeriod(),
            order = item.getInt("order"),
            years = item.getInt("years"),
            displayName = item.getString("displayName"),
            chapterTitle = item.getString("chapterTitle"),
            startLabel = item.optString("startLabel"),
            endLabel = item.optString("endLabel"),
        )
    }.sortedBy { it.order }

    fun parseFocuses(json: String): List<Focus> = JSONArray(json).mapObjects { item ->
        Focus(
            focusId = item.getString("focusId"),
            period = item.getString("period").toLifePeriod(),
            label = item.getString("name"),
            statEffects = item.optJSONObject("annualStatDelta").toStatDelta(),
            description = item.getString("description"),
            sceneImageId = item.optNullableString("sceneImageId"),
            momentImageId = item.optNullableString("momentImageId"),
            yearlyLogTemplates = item.optJSONArray("yearlyLogTemplates").toStringList(),
        )
    }

    fun parseDecisionEvents(json: String): List<DecisionEvent> = JSONArray(json).mapObjects { item ->
        val conditions = item.optJSONObject("conditions")
        DecisionEvent(
            eventId = item.getString("eventId"),
            periods = item.toPeriods(),
            kind = item.toDecisionKind(),
            title = item.getString("title"),
            text = item.getString("text"),
            choices = item.getJSONArray("choices").mapObjects { choice -> choice.toDecisionOption() },
            weight = item.optInt("weight", 1).coerceAtLeast(1),
            yearInPeriod = item.optPositiveInt("yearInPeriod"),
            requiredFlags = conditions?.optJSONArray("requiredFlags").toStringSet(),
            blockedFlags = conditions?.optJSONArray("blockedFlags").toStringSet(),
            statConditions = conditions?.optJSONObject("stats").toStatConditions(),
            povImageId = item.optNullableString("povImageId")
                ?: item.optNullableString("imageId"),
            momentImageId = item.optNullableString("momentImageId"),
        )
    }

    private fun JSONObject.toDecisionOption(): DecisionOption {
        val fallbackDelta = optJSONObject("statDelta").toStatDelta()
        val fallbackText = optString("resultText")
        val fallbackFlags = optJSONArray("addFlags").toStringSet()
        val parsedOutcomes = optJSONArray("outcomes")?.mapObjects { outcome ->
            Outcome(
                outcomeId = outcome.getString("outcomeId"),
                resultText = outcome.optString("text", fallbackText),
                statDelta = outcome.optJSONObject("statDelta")?.toStatDelta() ?: fallbackDelta,
                addFlags = outcome.optJSONArray("addFlags")?.toStringSet() ?: fallbackFlags,
                chance = outcome.getInt("chance"),
                primary = outcome.optBoolean("primary", false),
                chanceWeights = outcome.optJSONObject("chanceWeights").toChanceWeights(),
                hidden = outcome.optBoolean("hidden", false),
                death = outcome.optBoolean("death", false),
            )
        }.orEmpty()
        val outcomes = parsedOutcomes.ifEmpty {
            listOf(
                Outcome(
                    outcomeId = "${getString("choiceId").lowercase()}_legacy",
                    resultText = fallbackText,
                    statDelta = fallbackDelta,
                    addFlags = fallbackFlags,
                    chance = 100,
                ),
            )
        }
        return DecisionOption(
            choiceId = getString("choiceId"),
            label = getString("label"),
            outcomes = outcomes,
            requiredFlags = optJSONArray("requiredFlags").toStringSet(),
            blockedFlags = optJSONArray("blockedFlags").toStringSet(),
        )
    }

    private fun JSONObject.toPeriods(): Set<LifePeriod> {
        val explicit = optJSONArray("periods")?.mapStrings { it.toLifePeriod() }.orEmpty()
        if (explicit.isNotEmpty()) return explicit.toSet()
        return when (getString("stage").uppercase()) {
            "INFANT" -> setOf(LifePeriod.INFANT)
            "CHILD" -> setOf(LifePeriod.CHILD)
            "TEEN" -> setOf(LifePeriod.TEEN)
            "ADULT", "YOUNG_ADULT", "MIDDLE" ->
                setOf(LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE)
            else -> error("Unknown event stage: ${getString("stage")}")
        }
    }

    private fun JSONObject.toDecisionKind(): DecisionKind = when {
        has("kind") -> DecisionKind.valueOf(getString("kind").uppercase())
        optBoolean("isMilestone", false) -> DecisionKind.MILESTONE
        optString("type").uppercase() == "SPECIAL" -> DecisionKind.BIG_INCIDENT
        else -> DecisionKind.INCIDENT
    }

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

    private fun JSONObject?.toChanceWeights(): ChanceWeights = ChanceWeights(
        health = this?.optNullableInt("건강") ?: this?.optNullableInt("health") ?: 0,
        fitness = this?.optNullableInt("운동능력") ?: this?.optNullableInt("fitness") ?: 0,
        intelligence = this?.optNullableInt("지력") ?: this?.optNullableInt("intelligence") ?: 0,
        social = this?.optNullableInt("사회성") ?: this?.optNullableInt("social") ?: 0,
        wealth = this?.optNullableInt("경제력") ?: this?.optNullableInt("wealth") ?: 0,
        happiness = this?.optNullableInt("행복") ?: this?.optNullableInt("happiness") ?: 0,
        luck = this?.optNullableInt("운") ?: this?.optNullableInt("luck") ?: 0,
    )

    private fun String.toLifePeriod(): LifePeriod = LifePeriod.valueOf(uppercase())

    private fun JSONObject.optNullableString(name: String): String? =
        if (!has(name) || isNull(name)) null else optString(name).takeIf { it.isNotBlank() }

    private fun JSONObject.optPositiveInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name).takeIf { it > 0 } else null

    private fun JSONObject.optNullableInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else mapStrings { it }

    private fun JSONArray?.toStringSet(): Set<String> = toStringList().toSet()

    private fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
        List(length()) { index -> transform(getJSONObject(index)) }

    private fun <T> JSONArray.mapStrings(transform: (String) -> T): List<T> =
        List(length()) { index -> transform(getString(index)) }
}
