package com.example.lifegame.ui

import com.example.lifegame.domain.engine.AutomaticEventRule
import com.example.lifegame.domain.engine.MarriageContract
import com.example.lifegame.domain.model.AutomaticEventRecord
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.RunChoiceRecord

/**
 * 연애 · 결혼 화면에서 "같은 상대"를 이어 보여 주는 조회(2026-11-20 과제, 김재겸 프론트).
 *
 * 현재 상대의 안정 ID와 실제 만남 기록을 연결한다. 안정 ID가 없는 이전 저장은 경로 플래그로 만남을 찾는다.
 * "「모닥불 옆자리」에서 만난 사람"처럼 처음 만난 사건으로 부른다. 결혼식은 실제로 고른 결혼식 사건 결과만 보여 준다.
 */
object PartnerStory {
    /** 현재 콘텐츠의 상대 플래그. 사건 JSON의 addFlags에서 가져온 목록이다. */
    val PARTNER_FLAGS = listOf(
        "partner_first_love", "partner_ideal", "partner_music", "partner_book", "partner_letter",
        "partner_app", "partner_closing", "partner_gym", "partner_blind_date", "partner_second_chance",
    )

    fun isWeddingEvent(eventId: String): Boolean = MarriageContract.isCeremony(eventId)

    /** 연애 맥락의 사건(만남 이후의 데이트 · 갈등 · 결혼 이야기 · 양가 인사 · 결혼식 등). */
    fun isLoveEvent(eventId: String): Boolean = eventId.startsWith("love_") || eventId == "adult_032"
}

/** 이번 인생의 상대. [label]은 처음 만난 실제 사건 제목으로 만든다. */
data class PartnerUi(
    val flag: String,
    val originEventId: String,
    val originTitle: String,
    val originPeriodName: String,
    val originYear: Int,
    val status: String,
) {
    val label: String get() = "「$originTitle」에서 만난 사람"
}

/** 상대와 함께한 사건 한 줄(실제로 고른 선택). */
data class PartnerMomentUi(val eventId: String, val title: String, val choiceLabel: String, val periodName: String, val year: Int)

/** 기록 화면의 "인연". 결혼식은 실제로 고른 결혼식 사건이 있을 때만 있다. */
data class PartnerStoryUi(
    val partner: PartnerUi?,
    val moments: List<PartnerMomentUi>,
    val wedding: PartnerMomentUi?,
    val status: String,
)

private fun periodName(periods: List<PeriodDefinition>, period: LifePeriod) =
    periods.firstOrNull { it.period == period }?.displayName ?: period.name

/** 지금 관계 단계. 같은 상대 정보가 없어도 혼인 상태는 플래그로 정확히 보여 준다. */
fun relationshipStatus(flags: Set<String>): String = when {
    "divorced" in flags -> "이혼"
    "married" in flags -> "결혼"
    "engaged" in flags -> "약혼"
    "dating" in flags -> "교제 중"
    "breakup_once" in flags -> "헤어짐"
    "single_life" in flags -> "혼자의 삶"
    else -> ""
}

/**
 * 배우자 또는 현재 상대 ID가 있으면 그 사람의 만남 기록을 사용한다. ID가 없는 저장에서는 기존 경로를 찾는다.
 */
fun partnerOf(
    flags: Set<String>,
    choices: List<RunChoiceRecord>,
    autoRecords: List<AutomaticEventRecord>,
    decisions: Map<String, DecisionEvent>,
    automaticRules: Map<String, AutomaticEventRule>,
    periods: List<PeriodDefinition>,
): PartnerUi? {
    val stableId = MarriageContract.spouseId(flags) ?: MarriageContract.currentPartnerId(flags)
    val identity = stableId?.let { id -> MarriageContract.partners.firstOrNull { it.id == id } ?: return null }
    val held = identity?.let { setOf(it.routeFlag) } ?: PartnerStory.PARTNER_FLAGS.filter { it in flags }.toSet()
    if (held.isEmpty()) return null
    data class Origin(val order: Int, val flag: String, val eventId: String, val title: String, val period: LifePeriod, val year: Int)
    val fromChoices = choices.mapIndexedNotNull { index, record ->
        if (identity != null && record.choiceId !in identity.origins[record.eventId].orEmpty()) {
            return@mapIndexedNotNull null
        }
        val added = decisions[record.eventId]?.choices?.firstOrNull { it.choiceId == record.choiceId }
            ?.outcomes?.firstOrNull { it.outcomeId == record.outcomeId }?.addFlags.orEmpty()
        added.firstOrNull { it in held }?.let { Origin(index, it, record.eventId, record.eventTitle, record.period, record.yearInPeriod) }
    }
    // 자동 사건은 선택 기록과 순서를 섞기 어렵다. 같은 상대가 선택에서도 생겼다면 선택 쪽을 쓴다.
    val fromAuto = if (identity != null) emptyList() else autoRecords.mapNotNull { record ->
        val rule = automaticRules[record.eventId] ?: return@mapNotNull null
        rule.event.addFlags.firstOrNull { it in held }?.let {
            Origin(-1, it, record.eventId, rule.topic.ifBlank { record.text }, record.period, record.yearInPeriod)
        }
    }
    val origin = (fromChoices + fromAuto.filter { auto -> fromChoices.none { it.flag == auto.flag } }).maxByOrNull { it.order } ?: return null
    return PartnerUi(
        flag = origin.flag,
        originEventId = origin.eventId,
        originTitle = origin.title,
        originPeriodName = periodName(periods, origin.period),
        originYear = origin.year,
        status = relationshipStatus(flags),
    )
}

/** 기록 화면용 인연: 상대 · 함께한 연애 사건 · 실제 결혼식 결과. 연애 기록이 하나도 없으면 null. */
fun partnerStoryOf(
    flags: Set<String>,
    choices: List<RunChoiceRecord>,
    autoRecords: List<AutomaticEventRecord>,
    decisions: Map<String, DecisionEvent>,
    automaticRules: Map<String, AutomaticEventRule>,
    periods: List<PeriodDefinition>,
): PartnerStoryUi? {
    val partner = partnerOf(flags, choices, autoRecords, decisions, automaticRules, periods)
    val loveRecords = choices.filter { PartnerStory.isLoveEvent(it.eventId) }
    if (partner == null && loveRecords.isEmpty()) return null
    fun moment(record: RunChoiceRecord) = PartnerMomentUi(
        eventId = record.eventId,
        title = record.eventTitle,
        choiceLabel = displayChoiceLabel(record.choiceLabel),
        periodName = periodName(periods, record.period),
        year = record.yearInPeriod,
    )
    val wedding = if ("married" in flags) loveRecords.lastOrNull { PartnerStory.isWeddingEvent(it.eventId) }?.let(::moment) else null
    return PartnerStoryUi(
        partner = partner,
        moments = loveRecords.filterNot { PartnerStory.isWeddingEvent(it.eventId) }.map(::moment),
        wedding = wedding,
        status = relationshipStatus(flags),
    )
}
