package com.example.lifegame.data

import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.AwardedTitle
import com.example.lifegame.domain.model.CompletedLifeRecord
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.model.SavedOutcome
import com.example.lifegame.domain.model.StatKey
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import org.json.JSONArray
import org.json.JSONObject

/** SharedPreferences와 분리한 순수 JSON 코덱. JVM 테스트에서 마이그레이션까지 검증한다. */
internal object Week10ProgressCodec {
    private const val VERSION = 3

    fun encode(run: V4SavedRun): String = JSONObject().apply {
        put("saveFormatVersion", VERSION)
        put("playerName", run.playerName)
        put("state", run.state.toJson())
        put("choices", run.choices.toJsonArray { it.toJson() })
        put("pendingOutcome", run.pendingOutcome?.toJson())
        put("avatar", run.avatar.toJson())
        put("awardedTitle", run.awardedTitle?.toJson())
        put("phase", run.phase?.name)
    }.toString()

    fun decode(raw: String): V4SavedRun? = runCatching {
        val root = JSONObject(raw)
        val version = root.optInt("saveFormatVersion", 1)
        require(version in 1..VERSION) { "unsupported save format: $version" }
        V4SavedRun(
            playerName = root.optString("playerName", "플레이어"),
            state = root.getJSONObject("state").toPeriodRunState(),
            choices = root.optJSONArray("choices").objects { it.toRunChoiceRecord() },
            pendingOutcome = root.optJSONObject("pendingOutcome")?.toSavedOutcome(),
            avatar = root.optJSONObject("avatar")?.toAvatar() ?: AvatarConfig(),
            awardedTitle = root.optJSONObject("awardedTitle")?.toAwardedTitle(),
            phase = root.nullableString("phase")?.let { TurnPhase.valueOf(it) },
        )
    }.getOrNull()

    fun encodeCompletedLives(records: List<CompletedLifeRecord>): String = JSONObject().apply {
        put("saveFormatVersion", VERSION)
        put("lives", records.toJsonArray { it.toJson() })
    }.toString()

    data class CompletedLivesRead(
        val records: List<CompletedLifeRecord> = emptyList(),
        val damagedEntries: Int = 0,
        val writable: Boolean = true,
    )

    fun decodeCompletedLives(raw: String): List<CompletedLifeRecord> = readCompletedLives(raw).records

    fun readCompletedLives(raw: String): CompletedLivesRead = runCatching {
        val root = JSONObject(raw)
        require(root.optInt("saveFormatVersion", 1) in 1..VERSION)
        val lives = root.getJSONArray("lives")
        var damaged = 0
        val records = buildList {
            for (index in 0 until lives.length()) {
                val record = runCatching { lives.getJSONObject(index).toCompletedLifeRecord() }.getOrNull()
                if (record == null) damaged++ else add(record)
            }
        }
        CompletedLivesRead(records, damaged)
    }.getOrElse { CompletedLivesRead(writable = false) }

    private fun PeriodRunState.toJson() = JSONObject().apply {
        put("runId", runId); put("runSeed", runSeed); put("period", period.name)
        put("year", yearInPeriod); put("focus", selectedFocusId); put("stats", stats.toJson())
        put("flags", JSONArray(flags.toList())); put("lifeFacts", lifeFacts.toJson())
        put("dominantFocusStat", dominantFocusStat?.name)
        put("recentChoiceIds", JSONArray(recentChoiceIds)); put("pending", pendingDecisionId)
        put("moments", moments.toJsonArray { it.toJson() })
        put("ending", isEnding); put("dead", isDead); put("endingPeriod", endingPeriod?.name)
    }

    private fun JSONObject.toPeriodRunState() = PeriodRunState(
        runId = getString("runId"),
        runSeed = getLong("runSeed"),
        period = LifePeriod.valueOf(getString("period")),
        yearInPeriod = getInt("year"),
        selectedFocusId = nullableString("focus"),
        stats = getJSONObject("stats").toStats(),
        flags = optJSONArray("flags").strings().toSet(),
        lifeFacts = optJSONObject("lifeFacts")?.toLifeFacts()
            ?: LifeFacts(wealthBand = wealthBand(getJSONObject("stats").optInt("wealth", Stats.DEFAULT_STAT))),
        dominantFocusStat = nullableString("dominantFocusStat")?.let { StatKey.valueOf(it) },
        recentChoiceIds = optJSONArray("recentChoiceIds").strings(),
        pendingDecisionId = nullableString("pending"),
        moments = optJSONArray("moments").objects { it.toLifeMoment() },
        isEnding = optBoolean("ending", false),
        isDead = optBoolean("dead", false),
        endingPeriod = nullableString("endingPeriod")?.let { LifePeriod.valueOf(it) },
    )

    private fun Stats.toJson() = JSONObject().apply {
        put("health", health); put("fitness", fitness); put("intelligence", intelligence)
        put("social", social); put("wealth", wealth); put("happiness", happiness); put("luck", luck)
    }

    private fun JSONObject.toStats() = Stats(
        health = optInt("health", optInt("life", Stats.DEFAULT_STAT)),
        fitness = optInt("fitness", Stats.DEFAULT_STAT),
        intelligence = optInt("intelligence", Stats.DEFAULT_STAT),
        social = optInt("social", Stats.DEFAULT_STAT),
        wealth = optInt("wealth", Stats.DEFAULT_STAT),
        happiness = optInt("happiness", Stats.DEFAULT_STAT),
        luck = optInt("luck", Stats.DEFAULT_STAT),
    )

    private fun LifeFacts.toJson() = JSONObject().apply {
        put("educationLevel", educationLevel); put("fieldOfStudy", fieldOfStudy)
        put("careerTrack", careerTrack); put("careerLevel", careerLevel)
        put("employmentStatus", employmentStatus); put("maritalStatus", maritalStatus)
        put("childrenCount", childrenCount); put("housingType", housingType); put("wealthBand", wealthBand)
        put("achievementFlags", JSONArray(achievementFlags.toList()))
        put("deathPeriod", deathPeriod?.name); put("deathCause", deathCause)
    }

    private fun JSONObject.toLifeFacts() = LifeFacts(
        educationLevel = nullableString("educationLevel"), fieldOfStudy = nullableString("fieldOfStudy"),
        careerTrack = nullableString("careerTrack"), careerLevel = nullableString("careerLevel"),
        employmentStatus = nullableString("employmentStatus"), maritalStatus = nullableString("maritalStatus"),
        childrenCount = optInt("childrenCount", 0), housingType = nullableString("housingType"),
        wealthBand = optString("wealthBand", "MIDDLE"),
        achievementFlags = optJSONArray("achievementFlags").strings().toSet(),
        deathPeriod = nullableString("deathPeriod")?.let { LifePeriod.valueOf(it) },
        deathCause = nullableString("deathCause"),
    )

    private fun LifeMoment.toJson() = JSONObject().apply {
        put("period", period.name); put("year", yearInPeriod); put("text", text); put("image", momentImageId)
    }

    private fun JSONObject.toLifeMoment() = LifeMoment(
        period = LifePeriod.valueOf(getString("period")), yearInPeriod = getInt("year"),
        text = getString("text"), momentImageId = nullableString("image"),
    )

    private fun RunChoiceRecord.toJson() = JSONObject().apply {
        put("eventId", eventId); put("choiceId", choiceId); put("outcomeId", outcomeId)
        put("period", period.name); put("year", yearInPeriod); put("eventTitle", eventTitle)
        put("choiceLabel", choiceLabel); put("resultText", resultText); put("momentImageId", momentImageId)
    }

    private fun JSONObject.toRunChoiceRecord() = RunChoiceRecord(
        eventId = getString("eventId"), choiceId = getString("choiceId"), outcomeId = getString("outcomeId"),
        period = LifePeriod.valueOf(getString("period")), yearInPeriod = getInt("year"),
        eventTitle = getString("eventTitle"), choiceLabel = getString("choiceLabel"),
        resultText = getString("resultText"), momentImageId = nullableString("momentImageId"),
    )

    private fun SavedOutcome.toJson() = JSONObject().apply {
        put("eventId", eventId); put("choiceId", choiceId); put("outcomeId", outcomeId)
        put("choiceLabel", choiceLabel); put("resultText", resultText)
        put("beforeStats", beforeStats.toJson()); put("afterStats", afterStats.toJson())
        put("beforeFacts", beforeFacts.toJson()); put("afterFacts", afterFacts.toJson()); put("endsLife", endsLife)
        put("openedEvents", JSONArray(openedEvents))
    }

    private fun JSONObject.toSavedOutcome() = SavedOutcome(
        eventId = getString("eventId"), choiceId = getString("choiceId"), outcomeId = getString("outcomeId"),
        choiceLabel = getString("choiceLabel"), resultText = getString("resultText"),
        beforeStats = getJSONObject("beforeStats").toStats(), afterStats = getJSONObject("afterStats").toStats(),
        beforeFacts = getJSONObject("beforeFacts").toLifeFacts(), afterFacts = getJSONObject("afterFacts").toLifeFacts(),
        endsLife = optBoolean("endsLife", false),
        openedEvents = optJSONArray("openedEvents").strings(),
    )

    private fun AvatarConfig.toJson() = JSONObject().apply {
        put("skinToneId", skinToneId); put("hairId", hairId); put("eyesId", eyesId)
        put("portraitRef", portraitRef); put("faceSheetRef", faceSheetRef); put("photoConsent", photoConsent)
    }

    private fun JSONObject.toAvatar() = AvatarConfig(
        skinToneId = optString("skinToneId", "skin_default"), hairId = optString("hairId", "hair_default"),
        eyesId = optString("eyesId", "eyes_default"), portraitRef = nullableString("portraitRef"),
        faceSheetRef = nullableString("faceSheetRef"), photoConsent = optBoolean("photoConsent", false),
    )

    private fun AwardedTitle.toJson() = JSONObject().apply {
        put("titleId", titleId); put("text", text); put("description", description)
    }

    private fun JSONObject.toAwardedTitle() = AwardedTitle(
        titleId = getString("titleId"), text = getString("text"), description = optString("description"),
    )

    private fun CompletedLifeRecord.toJson() = JSONObject().apply {
        put("runId", runId); put("playerName", playerName); put("summary", summary)
        put("endingId", endingId); put("endingTitle", endingTitle); put("endingSummary", endingSummary)
        put("awardedTitle", awardedTitle?.toJson()); put("finalState", finalState.toJson())
        put("choices", choices.toJsonArray { it.toJson() }); put("avatar", avatar.toJson())
    }

    private fun JSONObject.toCompletedLifeRecord() = CompletedLifeRecord(
        runId = getString("runId"), playerName = getString("playerName"), summary = getString("summary"),
        endingId = nullableString("endingId"), endingTitle = nullableString("endingTitle"),
        endingSummary = nullableString("endingSummary"), awardedTitle = optJSONObject("awardedTitle")?.toAwardedTitle(),
        finalState = getJSONObject("finalState").toPeriodRunState(),
        choices = optJSONArray("choices").objects { it.toRunChoiceRecord() },
        avatar = optJSONObject("avatar")?.toAvatar() ?: AvatarConfig(),
    )

    private fun JSONObject.nullableString(name: String): String? =
        optString(name).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else List(length()) { getString(it) }

    private fun <T> JSONArray?.objects(transform: (JSONObject) -> T): List<T> =
        if (this == null) emptyList() else List(length()) { transform(getJSONObject(it)) }

    private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject) =
        JSONArray().also { array -> forEach { array.put(transform(it)) } }

    private fun wealthBand(value: Int): String = when {
        value < 25 -> "LOW"; value < 50 -> "LOWER_MIDDLE"; value < 75 -> "MIDDLE"
        value < 90 -> "HIGH"; else -> "VERY_HIGH"
    }
}
