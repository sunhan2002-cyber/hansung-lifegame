package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.AutomaticEvent
import com.example.lifegame.domain.model.LifeFactConditions
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatConditions

/** 자동 사건이 걸리는 진행 단계. 신기훈 `auto_events.sample.json`의 trigger · triggerRule.stage를 옮긴 것이다. */
enum class AutomaticEventStage {
    /** trigger = ELAPSED_TIME. 같은 시기에서 지나간 자동 진행 단계 수가 [AutomaticEventRule.afterAutoSteps] 이상 */
    ELAPSED,
    /** 시기 첫해(장 표지 · 초점을 고른 직후, 또는 영아기 · 노년기 진입) */
    PERIOD_START,
    /** [AutomaticEventRule.minYearInPeriod]번째 해 이상 */
    PERIOD_MID,
    /** 시기 마지막 해 */
    PERIOD_END,
    /** [AutomaticEventRule.afterEventId] 사건을 고른 바로 다음 단계 */
    AFTER_EVENT,
}

/**
 * 김우영 [AutomaticEvent]에 콘텐츠의 표시 조건을 붙인 한 항목.
 * 엔진 적용([GameEngine.applyAutomaticEvent])은 [event]만 받고, 언제 · 무엇을 고를지는 [AutomaticEventPicker]가 정한다.
 */
data class AutomaticEventRule(
    val event: AutomaticEvent,
    /** "감기"처럼 짧은 주제. 화면 카드 제목과 기록 화면에 쓴다. */
    val topic: String = "",
    val stage: AutomaticEventStage = AutomaticEventStage.ELAPSED,
    val afterAutoSteps: Int = 0,
    val minYearInPeriod: Int = 1,
    val afterEventId: String? = null,
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
    val statConditions: StatConditions = StatConditions(),
    val lifeFactConditions: LifeFactConditions = LifeFactConditions(),
    /** 같은 키 묶음은 한 인생에 하나만(예: 돌잡이 사진 세 갈래). */
    val dedupeKey: String = event.eventId,
    /** RECORD_ONLY: 능력치는 바꾸지 않고 기록에만 남긴다. */
    val recordOnly: Boolean = false,
)

/**
 * 자동 사건을 고르는 규칙(2026-11-16 프론트 마무리 과제 2). 순수 Kotlin이라 기기 없이 검증한다.
 *
 * - 결정 대기 · 결말 · 초점 선택 중에는 고르지 않는다. 선택 · 돌발 · 결말보다 앞서지 않는다.
 * - 한 진행 단계(시기 · 해)에 최대 1건, 한 시기에 최대 2건(영아기 · 노년기 3건).
 * - 조건(플래그 · 능력치 · 생애 사실)을 사건과 같은 방식으로 검사하고, 완료한 eventId와 dedupe 키는 다시 고르지 않는다.
 * - 바로 앞 선택에 이어지는 AFTER_EVENT 후보가 있으면 먼저 고른다. 나머지는 runSeed · 시기 · 진행 단계로 결정적으로 뽑되,
 *   남은 자리를 시기의 남은 단계에 고르게 나눠(남은 자리 / 남은 단계 확률) 시기 마지막 해 사건도 나올 수 있게 한다.
 * - 자동 사건으로는 죽지 않는다: 건강을 0 이하로 만드는 후보는 고르지 않는다.
 *
 * 이 게임은 해마다 돌발이 보장되어(guaranteeYearlyIncident) "결정 없이 지나간 해"가 거의 없다. 그래서 자동 진행 단계는
 * "한 해의 결정을 끝내고 다음 해로 넘어가기 전" 한 번으로 세고, 지나간 단계 수는 시기 안에서 끝낸 해의 수(yearInPeriod − 1)로 본다.
 */
class AutomaticEventPicker(
    val rules: List<AutomaticEventRule>,
    periods: List<PeriodDefinition>,
) {
    private val byId = rules.associateBy { it.event.eventId }
    private val lastYears = periods.associate { it.period to it.years }

    val isEmpty: Boolean get() = rules.isEmpty()

    fun rule(eventId: String): AutomaticEventRule? = byId[eventId]

    fun pick(state: PeriodRunState): AutomaticEvent? {
        if (state.isEnding || state.pendingDecisionId != null) return null
        val token = stageToken(state)
        if (state.automaticEventRecords.any { it.triggerToken == token }) return null
        if (state.automaticEventRecords.count { it.period == state.period } >= periodLimit(state.period)) return null
        val usedKeys = state.automaticEventRecords.map { byId[it.eventId]?.dedupeKey ?: it.eventId }.toSet()
        val lastChoiceEventId = state.recentChoiceIds.lastOrNull()?.substringBefore(':')
        val eligible = rules.filter { eligible(it, state, usedKeys, lastChoiceEventId) }
        val stageKey = "${state.runSeed}:${state.period.name}:${elapsedAutoSteps(state)}:auto"
        // 바로 앞 선택에 이어지는 사건은 이 단계가 아니면 의미가 없으므로 남은 자리가 있으면 바로 고른다.
        val afterEvent = eligible.filter { it.stage == AutomaticEventStage.AFTER_EVENT }
        if (afterEvent.isNotEmpty()) return afterEvent.sortedBy { it.event.eventId }.let { it[seededIndex(stageKey, it.size)].event }
        if (eligible.isEmpty()) return null
        // 남은 자리를 시기의 남은 단계에 고르게 나눈다(남은 자리 / 남은 단계 확률). 매번 채우면 시기 앞쪽 해에서
        // 상한이 차 "마지막 해" 사건이 나올 수 없다.
        val remainingSlots = periodLimit(state.period) - state.automaticEventRecords.count { it.period == state.period }
        val remainingStages = ((lastYears[state.period] ?: state.yearInPeriod) - state.yearInPeriod + 1).coerceAtLeast(1)
        if (remainingSlots < remainingStages && seededIndex("$stageKey:fire", remainingStages) >= remainingSlots) return null
        val ordered = eligible.sortedBy { it.event.eventId }
        return ordered[seededIndex(stageKey, ordered.size)].event
    }

    private fun eligible(
        rule: AutomaticEventRule,
        state: PeriodRunState,
        usedKeys: Set<String>,
        lastChoiceEventId: String?,
    ): Boolean {
        val event = rule.event
        if (state.period !in event.periods) return false
        if (!event.repeatable && (rule.dedupeKey in usedKeys || state.automaticEventRecords.any { it.eventId == event.eventId })) {
            return false
        }
        if (!state.flags.containsAll(rule.requiredFlags) || rule.blockedFlags.any(state.flags::contains)) return false
        if (!rule.statConditions.matches(state.stats) || !rule.lifeFactConditions.matches(state.lifeFacts)) return false
        if (event.statDelta.health < 0 && state.stats.health + event.statDelta.health <= 0) return false
        val year = state.yearInPeriod
        return when (rule.stage) {
            AutomaticEventStage.ELAPSED -> elapsedAutoSteps(state) >= rule.afterAutoSteps
            AutomaticEventStage.PERIOD_START -> year == 1
            AutomaticEventStage.PERIOD_MID -> year >= rule.minYearInPeriod
            AutomaticEventStage.PERIOD_END -> year == (lastYears[state.period] ?: return false)
            AutomaticEventStage.AFTER_EVENT -> rule.afterEventId != null && lastChoiceEventId == rule.afterEventId
        }
    }

    companion object {
        /** 한 진행 단계를 가리키는 키. 같은 키로는 한 번만 적용되고, 저장 · 복원 뒤에도 다시 적용되지 않는다. */
        fun stageToken(state: PeriodRunState): String = "${state.runId}:auto:${state.period.name}:${state.yearInPeriod}"

        fun elapsedAutoSteps(state: PeriodRunState): Int = (state.yearInPeriod - 1).coerceAtLeast(0)

        fun periodLimit(period: LifePeriod): Int =
            if (period == LifePeriod.INFANT || period == LifePeriod.OLD_AGE) 3 else 2

        private fun seededIndex(key: String, size: Int): Int {
            var hash = 1_125_899_906_842_597L
            key.forEach { hash = 31 * hash + it.code }
            hash = hash xor (hash ushr 33)
            hash *= -0x00ae502812aa7333L
            hash = hash xor (hash ushr 33)
            return Math.floorMod(hash, size.toLong()).toInt()
        }
    }
}
