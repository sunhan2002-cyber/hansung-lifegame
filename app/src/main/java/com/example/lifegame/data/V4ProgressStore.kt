package com.example.lifegame.data

import android.content.Context
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.Stats
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
            pendingDecisionId = obj.optString("pending").takeIf { it.isNotBlank() && it != "null" },
            moments = List(moments.length()) { index -> moments.getJSONObject(index).let { LifeMoment(LifePeriod.valueOf(it.getString("period")), it.getInt("year"), it.getString("text"), it.optString("image").takeIf(String::isNotBlank)) } },
            isEnding = obj.optBoolean("ending", false),
            isDead = obj.optBoolean("dead", false),
            endingPeriod = obj.optString("endingPeriod").takeIf { it.isNotBlank() && it != "null" }?.let { LifePeriod.valueOf(it) },
        )
        V4SavedRun(root.optString("playerName", "플레이어"), state)
    }.getOrNull()

    private companion object { const val KEY = "savedRun" }
}
