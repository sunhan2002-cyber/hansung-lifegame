package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.StatKey
import org.json.JSONArray
import org.json.JSONObject

data class V4SavedRun(val playerName: String, val state: PeriodRunState)

class V4ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("lifegame_v4_progress", Context.MODE_PRIVATE)
    fun hasSave(): Boolean = prefs.contains(KEY)
    fun save(run: V4SavedRun) = prefs.edit().putString(KEY, encode(run)).apply()
    fun load(): V4SavedRun? = prefs.getString(KEY, null)?.let(::decode)
    fun clear() = prefs.edit().remove(KEY).apply()

    private fun encode(run: V4SavedRun): String = JSONObject().apply {
        put("saveFormatVersion", 2)
        put("playerName", run.playerName)
        put("state", JSONObject().apply {
            put("runId", run.state.runId)
            put("runSeed", run.state.runSeed)
            put("period", run.state.period.name)
            put("year", run.state.yearInPeriod)
            put("focus", run.state.selectedFocusId)
            put("stats", JSONObject().apply {
                put("health", run.state.stats.health); put("fitness", run.state.stats.fitness)
                put("intelligence", run.state.stats.intelligence); put("social", run.state.stats.social)
                put("wealth", run.state.stats.wealth); put("happiness", run.state.stats.happiness)
                put("luck", run.state.stats.luck)
            })
            put("flags", JSONArray(run.state.flags.toList()))
            put("dominantFocusStat", run.state.dominantFocusStat?.name)
            put("recentChoiceIds", JSONArray(run.state.recentChoiceIds))
            put("lifeFacts", run.state.lifeFacts.toJson())
            put("pending", run.state.pendingDecisionId)
            put("ending", run.state.isEnding)
            put("dead", run.state.isDead)
            put("endingPeriod", run.state.endingPeriod?.name)
            put("moments", JSONArray().apply {
                run.state.moments.forEach { moment -> put(JSONObject().apply {
                    put("period", moment.period.name); put("year", moment.yearInPeriod)
                    put("text", moment.text); put("image", moment.momentImageId)
                }) }
            })
        })
    }.toString()

    private fun decode(raw: String): V4SavedRun? = runCatching {
        val root = JSONObject(raw); val obj = root.getJSONObject("state")
        val stats = obj.getJSONObject("stats")
        val moments = obj.optJSONArray("moments") ?: JSONArray()
        val state = PeriodRunState(
            runId = obj.getString("runId"), runSeed = obj.getLong("runSeed"),
            period = LifePeriod.valueOf(obj.getString("period")), yearInPeriod = obj.getInt("year"),
            selectedFocusId = obj.optString("focus").takeIf { it.isNotBlank() && it != "null" },
            stats = Stats(stats.optInt("health", stats.optInt("life", Stats.DEFAULT_STAT)), stats.getInt("fitness"), stats.getInt("intelligence"), stats.getInt("social"), stats.getInt("wealth"), stats.getInt("happiness"), stats.getInt("luck")),
            flags = buildSet { repeat(obj.optJSONArray("flags")?.length() ?: 0) { add(obj.getJSONArray("flags").getString(it)) } },
            lifeFacts = obj.optJSONObject("lifeFacts")?.toLifeFacts() ?: LifeFacts(
                wealthBand = wealthBand(stats.optInt("wealth", Stats.DEFAULT_STAT)),
            ),
            dominantFocusStat = obj.optString("dominantFocusStat")
                .takeIf { it.isNotBlank() && it != "null" }
                ?.let { runCatching { StatKey.valueOf(it) }.getOrNull() },
            recentChoiceIds = obj.optJSONArray("recentChoiceIds").toStringList(),
            pendingDecisionId = obj.optString("pending").takeIf { it.isNotBlank() && it != "null" },
            moments = List(moments.length()) { index -> moments.getJSONObject(index).let { LifeMoment(LifePeriod.valueOf(it.getString("period")), it.getInt("year"), it.getString("text"), it.optString("image").takeIf(String::isNotBlank)) } },
            isEnding = obj.optBoolean("ending", false),
            isDead = obj.optBoolean("dead", false),
            endingPeriod = obj.optString("endingPeriod").takeIf { it.isNotBlank() && it != "null" }?.let { LifePeriod.valueOf(it) },
        )
        V4SavedRun(root.optString("playerName", "플레이어"), state)
    }.getOrNull()

    private fun LifeFacts.toJson(): JSONObject = JSONObject().apply {
        put("educationLevel", educationLevel); put("fieldOfStudy", fieldOfStudy)
        put("careerTrack", careerTrack); put("careerLevel", careerLevel)
        put("employmentStatus", employmentStatus); put("maritalStatus", maritalStatus)
        put("childrenCount", childrenCount); put("housingType", housingType)
        put("wealthBand", wealthBand); put("achievementFlags", JSONArray(achievementFlags.toList()))
        put("deathPeriod", deathPeriod?.name); put("deathCause", deathCause)
    }

    private fun JSONObject.toLifeFacts(): LifeFacts = LifeFacts(
        educationLevel = nullableString("educationLevel"),
        fieldOfStudy = nullableString("fieldOfStudy"),
        careerTrack = nullableString("careerTrack"),
        careerLevel = nullableString("careerLevel"),
        employmentStatus = nullableString("employmentStatus"),
        maritalStatus = nullableString("maritalStatus"),
        childrenCount = optInt("childrenCount", 0),
        housingType = nullableString("housingType"),
        wealthBand = optString("wealthBand", "MIDDLE"),
        achievementFlags = optJSONArray("achievementFlags").toStringList().toSet(),
        deathPeriod = nullableString("deathPeriod")?.let { runCatching { LifePeriod.valueOf(it) }.getOrNull() },
        deathCause = nullableString("deathCause"),
    )

    private fun JSONObject.nullableString(name: String): String? =
        optString(name).takeIf { it.isNotBlank() && it != "null" }

    private fun JSONArray?.toStringList(): List<String> =
        if (this == null) emptyList() else List(length()) { getString(it) }

    private fun wealthBand(value: Int): String = when {
        value < 25 -> "LOW"
        value < 50 -> "LOWER_MIDDLE"
        value < 75 -> "MIDDLE"
        value < 90 -> "HIGH"
        else -> "VERY_HIGH"
    }

    private companion object { const val KEY = "savedRun" }
}
