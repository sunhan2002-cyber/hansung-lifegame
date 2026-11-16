package com.example.lifegame.data

import com.example.lifegame.domain.model.AutomaticEvent
import com.example.lifegame.domain.model.AutomaticEventTrigger
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatConditions
import com.example.lifegame.domain.model.StatDelta
import org.json.JSONArray
import org.json.JSONObject

/**
 * `auto_events.sample.json`(신기훈, schemaVersion 2)을 앱 모델로 읽는다.
 *
 * - 각 항목은 엔진 모델 [AutomaticEvent]로 바뀐다(eventId·시기·본문·트리거·스탯·플래그·이미지·반복 여부).
 * - 엔진 모델에 없는 표시 조건(플래그·능력치·생애 사실·진행 단계·중복 방지 키·시기당 상한)은
 *   [AutomaticEventContent]에 남기고, [AutomaticEventCatalog.candidatesFor]로 지금 상태에서 열 수 있는 후보만 고른다.
 * - 진행 화면 연결(세션에 후보를 넘기는 일)은 김우영 담당이다. 이 파일은 엔진·화면 코드를 바꾸지 않는다.
 */
data class AutomaticEventTriggerRule(
    /** PERIOD_START · PERIOD_MID · PERIOD_END · AFTER_EVENT (PROGRESS_STAGE일 때) */
    val stage: String? = null,
    val minYearInPeriod: Int? = null,
    val afterEventId: String? = null,
    /** ELAPSED_TIME: 이번 시기에 쌓인 기록(LifeMoment) 수가 이 값 이상일 때. 매년 카운터가 아니다. */
    val minMomentsInPeriod: Int? = null,
)

data class AutomaticEventContent(
    val event: AutomaticEvent,
    val triggerRule: AutomaticEventTriggerRule,
    val requiredFlags: Set<String>,
    val blockedFlags: Set<String>,
    val statConditions: StatConditions,
    val lifeFactConditions: LifeFactConditions,
    val dedupeKey: String,
    val appliesStats: Boolean,
    /** NONE · REUSE_CANDIDATE · NEW_CANDIDATE. 승인 이미지가 없으면 event.momentImageId는 null이다. */
    val imageUse: String,
) {
    /** 진행 단계·경과 조건. 시기 연수는 [periodYears]로 받는다(PERIOD_END 판정). */
    fun triggerMatches(state: PeriodRunState, periodYears: Int): Boolean {
        val rule = triggerRule
        if (rule.minYearInPeriod != null && state.yearInPeriod < rule.minYearInPeriod) return false
        return when (event.trigger) {
            AutomaticEventTrigger.ELAPSED_TIME ->
                state.moments.count { it.period == state.period } >= (rule.minMomentsInPeriod ?: 1)
            AutomaticEventTrigger.PROGRESS_STAGE -> when (rule.stage) {
                "PERIOD_START" -> state.yearInPeriod == 1
                "PERIOD_MID" -> state.yearInPeriod >= (rule.minYearInPeriod ?: 2)
                "PERIOD_END" -> state.yearInPeriod >= periodYears
                "AFTER_EVENT" -> rule.afterEventId != null && "v4:event:${rule.afterEventId}" in state.flags
                else -> false
            }
        }
    }

    fun conditionsMatch(state: PeriodRunState): Boolean =
        state.flags.containsAll(requiredFlags) &&
            state.flags.none(blockedFlags::contains) &&
            statConditions.matches(state.stats) &&
            lifeFactConditions.matches(state.lifeFacts)
}

data class AutomaticEventCatalog(
    val events: List<AutomaticEventContent>,
    val maxPerPeriod: Map<LifePeriod, Int>,
) {
    private val dedupeKeyById = events.associate { it.event.eventId to it.dedupeKey }

    /** 엔진의 [AutomaticEvent] 목록(조건 판정 없이 전체). */
    val automaticEvents: List<AutomaticEvent> get() = events.map { it.event }

    /**
     * 지금 상태에서 표시해도 되는 자동 사건 후보. 시기·조건·진행 단계·중복 방지 키·시기당 상한을 모두 통과한 것만.
     * 대기 중 선택이 있거나 결말이면 빈 목록(선택·돌발 사건을 가로채지 않는다).
     */
    fun candidatesFor(state: PeriodRunState, periodYears: Int): List<AutomaticEvent> {
        if (state.isEnding || state.pendingDecisionId != null) return emptyList()
        val shownThisPeriod = state.automaticEventRecords.count { it.period == state.period }
        if (shownThisPeriod >= (maxPerPeriod[state.period] ?: DEFAULT_MAX_PER_PERIOD)) return emptyList()
        val usedKeys = state.automaticEventRecords.mapNotNull { dedupeKeyById[it.eventId] }.toSet()
        return events.filter { content ->
            state.period in content.event.periods &&
                (content.event.repeatable || content.dedupeKey !in usedKeys) &&
                content.conditionsMatch(state) &&
                content.triggerMatches(state, periodYears)
        }.map { it.event }
    }

    companion object {
        const val DEFAULT_MAX_PER_PERIOD = 2

        fun parse(json: String): AutomaticEventCatalog {
            val root = JSONObject(json)
            val limits = root.optJSONObject("rules")?.optJSONObject("maxPerPeriod")
            val maxPerPeriod = LifePeriod.values().mapNotNull { period ->
                limits?.takeIf { it.has(period.name) }?.let { period to it.getInt(period.name) }
            }.toMap()
            val events = root.getJSONArray("events").objects().map { item -> item.toContent() }
            return AutomaticEventCatalog(events, maxPerPeriod)
        }

        private fun JSONObject.toContent(): AutomaticEventContent {
            val conditions = optJSONObject("conditions") ?: JSONObject()
            val rule = optJSONObject("triggerRule") ?: JSONObject()
            val event = AutomaticEvent(
                eventId = getString("eventId"),
                periods = getJSONArray("periods").strings().map { LifePeriod.valueOf(it) }.toSet(),
                text = getString("text"),
                trigger = AutomaticEventTrigger.valueOf(getString("trigger")),
                statDelta = optJSONObject("statDelta").toStatDelta(),
                addFlags = optJSONArray("addFlags").strings().toSet(),
                removeFlags = optJSONArray("removeFlags").strings().toSet(),
                momentImageId = if (isNull("momentImageId")) null else optString("momentImageId").ifBlank { null },
                repeatable = optBoolean("repeatable", false),
            )
            return AutomaticEventContent(
                event = event,
                triggerRule = AutomaticEventTriggerRule(
                    stage = rule.optString("stage").ifBlank { null },
                    minYearInPeriod = rule.optIntOrNull("minYearInPeriod"),
                    afterEventId = rule.optString("afterEventId").ifBlank { null },
                    minMomentsInPeriod = rule.optIntOrNull("minMomentsInPeriod"),
                ),
                requiredFlags = conditions.optJSONArray("requiredFlags").strings().toSet(),
                blockedFlags = conditions.optJSONArray("blockedFlags").strings().toSet(),
                statConditions = conditions.optJSONObject("stats").toMinimumStats(),
                lifeFactConditions = conditions.optJSONObject("lifeFacts").toLifeFacts(),
                dedupeKey = optString("dedupeKey").ifBlank { event.eventId },
                appliesStats = optString("statEffect") == "STAT",
                imageUse = optString("imageUse", "NONE"),
            )
        }

        private fun JSONObject?.toStatDelta(): StatDelta = StatDelta(
            health = stat("건강"), fitness = stat("운동능력"), intelligence = stat("지력"), social = stat("사회성"),
            wealth = stat("경제력"), happiness = stat("행복"), luck = stat("운"),
        )

        private fun JSONObject?.stat(key: String): Int = this?.optInt(key, 0) ?: 0

        private fun JSONObject?.toMinimumStats(): StatConditions = StatConditions(
            minHealth = min("건강"), minFitness = min("운동능력"), minIntelligence = min("지력"), minSocial = min("사회성"),
            minWealth = min("경제력"), minHappiness = min("행복"), minLuck = min("운"),
        )

        private fun JSONObject?.min(key: String): Int? = if (this != null && has(key)) getInt(key) else null

        private fun JSONObject?.toLifeFacts(): LifeFactConditions = LifeFactConditions(
            educationLevel = text("educationLevel"), fieldOfStudy = text("fieldOfStudy"), careerTrack = text("careerTrack"),
            careerLevel = text("careerLevel"), employmentStatus = text("employmentStatus"), maritalStatus = text("maritalStatus"),
            minChildrenCount = this?.optIntOrNull("minChildrenCount"), housingType = text("housingType"),
            wealthBand = text("wealthBand"), requiredAchievements = this?.optJSONArray("requiredAchievements").strings().toSet(),
        )

        private fun JSONObject?.text(key: String): String? = this?.optString(key)?.ifBlank { null }

        private fun JSONObject.optIntOrNull(key: String): Int? = if (has(key) && !isNull(key)) getInt(key) else null

        private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }

        private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }
    }
}
