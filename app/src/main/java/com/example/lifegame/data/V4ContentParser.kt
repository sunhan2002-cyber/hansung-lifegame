package com.example.lifegame.data

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.EndingTier
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifeFactRule
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.StatKey
import org.json.JSONArray
import org.json.JSONObject

data class V4Content(
    val periods: List<PeriodDefinition>,
    val focuses: List<Focus>,
    val decisions: List<DecisionEvent>,
    val lifeFactRules: List<LifeFactRule> = emptyList(),
    val titles: List<LifeTitle> = emptyList(),
)

/** PR #26의 v4 JSON을 Android Context 없이도 검증할 수 있는 순수 파서다. */
object V4ContentParser {
    fun parseTitles(json: String): List<LifeTitle> = JSONArray(json).mapObjects { item ->
        val conditions = item.optJSONObject("conditions")
        LifeTitle(
            titleId = item.getString("titleId"),
            text = item.getString("text"),
            description = item.getString("description"),
            priority = item.optInt("priority", 0),
            conditions = EndingConditions(
                stats = statConditions(
                    conditions?.optJSONObject("minStats"),
                    conditions?.optJSONObject("maxStats"),
                ),
                requiredFlags = conditions?.optJSONArray("requiredFlags").toStringSet(),
                blockedFlags = conditions?.optJSONArray("blockedFlags").toStringSet(),
                lifeFacts = conditions?.optJSONObject("lifeFacts").toLifeFactConditions(),
            ),
            isDefault = item.optBoolean("isDefault", false),
        )
    }.also { titles ->
        require(titles.map { it.titleId }.distinct().size == titles.size) { "duplicate titleId" }
        require(titles.count { it.isDefault } == 1) { "exactly one default title is required" }
    }

    fun parseEndings(json: String): List<Ending> = JSONArray(json).mapObjects { item ->
        val conditions = item.optJSONObject("conditions")
        Ending(
            endingId = item.getString("endingId"),
            title = item.getString("title"),
            summary = item.getString("summary"),
            priority = item.optInt("priority", 0),
            isDefault = item.optBoolean("isDefault", false),
            tier = item.optNullableString("tier")
                ?.let { runCatching { EndingTier.valueOf(it.uppercase()) }.getOrNull() }
                ?: if (item.optBoolean("isDefault", false)) EndingTier.DEFAULT else EndingTier.GENERAL_LIFE,
            conditions = EndingConditions(
                stats = statConditions(
                    conditions?.optJSONObject("minStats"),
                    conditions?.optJSONObject("maxStats"),
                ),
                requiredFlags = conditions?.optJSONArray("requiredFlags").toStringSet(),
                blockedFlags = conditions?.optJSONArray("blockedFlags").toStringSet(),
                lifeFacts = conditions?.optJSONObject("lifeFacts").toLifeFactConditions(),
            ),
        )
    }

    fun parseLifeFactRules(json: String): List<LifeFactRule> = JSONArray(json).mapObjects { item ->
        LifeFactRule(
            flag = item.getString("flag"),
            fact = item.getString("fact"),
            value = item.opt("value")?.toString().orEmpty(),
            label = item.optString("label", item.opt("value")?.toString().orEmpty()),
        )
    }

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
            choices = item.getJSONArray("choices").mapObjects { choice ->
                choice.toDecisionOption(kind = item.toDecisionKind())
            },
            weight = item.optInt("weight", 1).coerceAtLeast(1),
            yearInPeriod = item.optPositiveInt("yearInPeriod"),
            requiredFlags = conditions?.optJSONArray("requiredFlags").toStringSet(),
            blockedFlags = conditions?.optJSONArray("blockedFlags").toStringSet(),
            statConditions = statConditions(
                conditions?.optJSONObject("stats") ?: conditions?.optJSONObject("minStats"),
                conditions?.optJSONObject("maxStats"),
            ),
            povImageId = item.optNullableString("povImageId")
                ?: item.optNullableString("imageId"),
            momentImageId = item.optNullableString("momentImageId"),
            pathStat = item.getString("eventId").toPathStat(),
            lifeFactConditions = conditions?.optJSONObject("lifeFacts").toLifeFactConditions(),
        )
    }

    private fun JSONObject.toDecisionOption(kind: DecisionKind): DecisionOption {
        val fallbackDelta = optJSONObject("statDelta").toStatDelta()
        val fallbackText = optString("resultText")
        val fallbackFlags = optJSONArray("addFlags").toStringSet()
        val fallbackRemovedFlags = optJSONArray("removeFlags").toStringSet()
        val parsedOutcomes = optJSONArray("outcomes")?.mapObjects { outcome ->
            Outcome(
                outcomeId = outcome.getString("outcomeId"),
                resultText = outcome.optString("text", fallbackText),
                statDelta = outcome.optJSONObject("statDelta")?.toStatDelta() ?: fallbackDelta,
                addFlags = outcome.optJSONArray("addFlags")?.toStringSet() ?: fallbackFlags,
                removeFlags = outcome.optJSONArray("removeFlags")?.toStringSet() ?: fallbackRemovedFlags,
                chance = outcome.getInt("chance"),
                primary = outcome.optBoolean("primary", false),
                chanceWeights = outcome.optJSONObject("chanceWeights").toChanceWeights(),
                hidden = outcome.optBoolean("hidden", false),
                death = outcome.optBoolean("death", false),
                deathCause = outcome.optNullableString("deathCause"),
                // 결과가 명시된 v4 사건은 저자가 적은 피해만 적용한다. BIG_INCIDENT라는 이유만으로
                // 성공/회복 결과에 기본 피해를 주입하지 않는다.
                lifeDelta = outcome.optInt("lifeDelta", 0),
            )
        }.orEmpty()
        val outcomes = parsedOutcomes.ifEmpty {
            listOf(
                Outcome(
                    outcomeId = "${getString("choiceId").lowercase()}_legacy",
                    resultText = fallbackText,
                    statDelta = fallbackDelta,
                    addFlags = fallbackFlags,
                    removeFlags = fallbackRemovedFlags,
                    chance = 100,
                    primary = true,
                    lifeDelta = inferLifeDelta(kind, fallbackDelta),
                ),
            )
        }
        val explicitStats = statConditions(
            optJSONObject("requiredStats") ?: optJSONObject("minStats") ?: optJSONObject("stats"),
            optJSONObject("maxStats"),
        )
        // Gates are authored in JSON. Outcome rewards must never create hidden locks,
        // particularly for alwaysAvailable fallbacks and ordinary incidents.
        return DecisionOption(
            choiceId = getString("choiceId"),
            label = getString("label"),
            outcomes = outcomes,
            requiredFlags = optJSONArray("requiredFlags").toStringSet(),
            blockedFlags = optJSONArray("blockedFlags").toStringSet(),
            statConditions = explicitStats,
            lifeFactConditions = optJSONObject("lifeFacts").toLifeFactConditions(),
        )
    }

    private fun JSONObject?.toLifeFactConditions(): LifeFactConditions = LifeFactConditions(
        educationLevel = this?.optNullableString("educationLevel"),
        fieldOfStudy = this?.optNullableString("fieldOfStudy"),
        careerTrack = this?.optNullableString("careerTrack"),
        careerLevel = this?.optNullableString("careerLevel"),
        employmentStatus = this?.optNullableString("employmentStatus"),
        maritalStatus = this?.optNullableString("maritalStatus"),
        minChildrenCount = this?.optNullableInt("minChildrenCount"),
        housingType = this?.optNullableString("housingType"),
        wealthBand = this?.optNullableString("wealthBand"),
        requiredAchievements = this?.optJSONArray("requiredAchievements").toStringSet(),
    )

    private fun String.toPathStat(): StatKey? = when {
        startsWith("path_hea_") -> StatKey.HEALTH
        startsWith("path_fit_") -> StatKey.FITNESS
        startsWith("path_int_") -> StatKey.INTELLIGENCE
        startsWith("path_soc_") -> StatKey.SOCIAL
        startsWith("path_wea_") -> StatKey.WEALTH
        startsWith("path_hap_") -> StatKey.HAPPINESS
        startsWith("path_luk_") -> StatKey.LUCK
        else -> null
    }

    private fun inferLifeDelta(kind: DecisionKind, delta: StatDelta): Int = when {
        kind == DecisionKind.BIG_INCIDENT -> -25
        else -> 0
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

    private fun statConditions(minimums: JSONObject?, maximums: JSONObject?): StatConditions = StatConditions(
        minHealth = minimums.stat("건강", "health"),
        minFitness = minimums.stat("운동능력", "fitness"),
        minIntelligence = minimums.stat("지력", "intelligence"),
        minSocial = minimums.stat("사회성", "social"),
        minWealth = minimums.stat("경제력", "wealth"),
        minHappiness = minimums.stat("행복", "happiness"),
        minLuck = minimums.stat("운", "luck"),
        maxHealth = maximums.stat("건강", "health"),
        maxFitness = maximums.stat("운동능력", "fitness"),
        maxIntelligence = maximums.stat("지력", "intelligence"),
        maxSocial = maximums.stat("사회성", "social"),
        maxWealth = maximums.stat("경제력", "wealth"),
        maxHappiness = maximums.stat("행복", "happiness"),
        maxLuck = maximums.stat("운", "luck"),
    )

    private fun JSONObject?.stat(korean: String, english: String): Int? =
        this?.optNullableInt(korean) ?: this?.optNullableInt(english)

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
