package com.example.lifegame.ui

import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.RunChoiceRecord
import com.example.lifegame.domain.engine.RunExtensionApi
import org.json.JSONArray
import org.json.JSONObject

/**
 * 승인된 확장 기능의 화면 표시용 조회(2026-11-13 통합지시서 "승인된 확장 기능", 김재겸 과제의 표시 부분).
 *
 * 콘텐츠는 신기훈 #76 `scenario_extensions.sample.json`(회상 슬롯 · 결말 계기 카드 · 칭호 목표 · NPC 재등장)을 읽고,
 * 판정은 이번 인생의 실제 선택 기록([RunChoiceRecord])과 지금 플래그로만 한다. 기록이 없으면 아무것도 만들지 않는다.
 * 다른 인생 기록과 섞지 않으며, 지난 인생 비교 문구는 만들지 않는다.
 *
 * 김우영 담당 조회 API(#74 후속)가 들어오면 이 파일의 조회 함수를 그 API로 바꾸면 된다. 화면은 [RecallLine] 등 결과 모델만 쓴다.
 */
data class ScenarioExtensions(
    val recallSlots: List<RecallSlot> = emptyList(),
    val turningPoints: Map<String, List<TurningPointCard>> = emptyMap(),
    val titleGoals: Map<String, TitleGoal> = emptyMap(),
    val npcs: List<NpcReappearance> = emptyList(),
) {
    companion object {
        val EMPTY = ScenarioExtensions()
        const val ENDING = "ENDING"
        const val ENDING_RECALL_LIMIT = 3
        const val TURNING_POINT_MIN = 2
        const val TURNING_POINT_MAX = 3
        const val APPEARANCE_EVENT = "EVENT"
        const val APPEARANCE_AUTO = "AUTO"

        /** 읽지 못하면 빈 확장(아무것도 표시하지 않음). */
        fun parse(json: String): ScenarioExtensions = runCatching {
            val root = JSONObject(json)
            ScenarioExtensions(
                recallSlots = root.optJSONArray("recallSlots").objects().map { o ->
                    RecallSlot(
                        slotId = o.getString("slotId"),
                        sourceEventId = o.getString("sourceEventId"),
                        sourceChoiceIds = o.optJSONArray("sourceChoiceIds").strings().toSet(),
                        requiredFlags = o.optJSONArray("requiredFlags").strings().toSet(),
                        blockedFlags = o.optJSONArray("blockedFlags").strings().toSet(),
                        line = o.getString("line"),
                        showIn = o.optJSONArray("showIn").strings(),
                    )
                },
                turningPoints = root.optJSONObject("turningPointCards")?.let { fields ->
                    fields.keys().asSequence().associateWith { field ->
                        fields.optJSONArray(field).objects().map { o ->
                            TurningPointCard(
                                field = field,
                                cardId = o.getString("cardId"),
                                eventId = o.getString("eventId"),
                                priority = o.optInt("priority", 0),
                                requiredFlags = o.optJSONArray("requiredFlags").strings().toSet(),
                                title = o.optString("title"),
                                line = o.getString("line"),
                            )
                        }
                    }
                }.orEmpty(),
                titleGoals = root.optJSONArray("titleGoals").objects().associate { o ->
                    o.getString("titleId") to TitleGoal(o.getString("titleId"), o.optString("goal"), o.optString("hint"))
                },
                npcs = root.optJSONArray("npcReappearances").objects().map { o ->
                    val origin = o.getJSONObject("origin")
                    NpcReappearance(
                        npcId = o.getString("npcId"),
                        label = o.getString("label"),
                        originEventIds = origin.getString("eventId").split("|").map(String::trim).filter(String::isNotEmpty),
                        originChoiceIds = origin.optJSONArray("choiceIds").strings().toSet(),
                        relationFlags = origin.optJSONArray("relationFlags").strings().toSet(),
                        appearances = o.optJSONArray("appearances").objects().map { a ->
                            NpcAppearance(
                                type = a.getString("type"),
                                id = a.getString("id"),
                                requiredFlags = a.optJSONArray("requiredFlags").strings().toSet(),
                                blockedFlags = a.optJSONArray("blockedFlags").strings().toSet(),
                            )
                        },
                    )
                },
            )
        }.getOrElse { EMPTY }

        private fun JSONArray?.objects(): List<JSONObject> =
            if (this == null) emptyList() else List(length()) { getJSONObject(it) }

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else List(length()) { getString(it) }
    }
}

data class RecallSlot(
    val slotId: String,
    val sourceEventId: String,
    val sourceChoiceIds: Set<String>,
    val requiredFlags: Set<String>,
    val blockedFlags: Set<String>,
    val line: String,
    val showIn: List<String>,
)

data class TurningPointCard(
    val field: String,
    val cardId: String,
    val eventId: String,
    val priority: Int,
    val requiredFlags: Set<String>,
    val title: String,
    val line: String,
)

data class TitleGoal(val titleId: String, val goal: String, val hint: String)

data class NpcReappearance(
    val npcId: String,
    val label: String,
    val originEventIds: List<String>,
    val originChoiceIds: Set<String>,
    val relationFlags: Set<String>,
    val appearances: List<NpcAppearance>,
)

data class NpcAppearance(val type: String, val id: String, val requiredFlags: Set<String>, val blockedFlags: Set<String>)

/** 실제로 지나간 자동 사건 한 건(재등장 판정용). [title]은 기록 화면에 쓸 이름(주제). */
data class AutoAppearance(val eventId: String, val title: String, val period: LifePeriod)

/** 화면에 보일 회상 한 줄 */
data class RecallLine(val slotId: String, val text: String)

/** 결말의 "내 인생의 계기" 카드. 실제로 고른 선택 문구를 그대로 넣는다. */
data class TurningPointUi(
    val cardId: String,
    val fieldLabel: String,
    val eventId: String,
    val title: String,
    val periodName: String,
    val yearInPeriod: Int,
    val choiceLabel: String,
    val text: String,
)

/** 관계가 이어진 사람의 재등장 정보 */
data class NpcReturnUi(
    val npcId: String,
    val label: String,
    /** 처음 만난 사건(실제 기록) */
    val originTitle: String,
    val originPeriodName: String,
    /** 다시 만난 사건 제목(기록 화면용). 사건 화면에서는 지금 사건이라 비운다. */
    val metAgainTitles: List<String> = emptyList(),
)

private fun ScenarioExtensions.flagsOk(flags: Set<String>, required: Set<String>, blocked: Set<String>) =
    flags.containsAll(required) && blocked.none { it in flags }

private fun RecallSlot.sourceIn(choices: List<RunChoiceRecord>): Boolean =
    RunExtensionApi.recallEvidence(sourceEventId, sourceChoiceIds, choices) != null

/**
 * 지금 사건에 붙일 회상 한 줄. 근거 선택이 이 인생에 실제로 있고, 같은 회상을 앞선 사건에서 이미 보여 주지 않았을 때만.
 * [priorChoices]는 지금 사건 전까지의 선택 기록이다.
 */
fun ScenarioExtensions.recallForEvent(
    eventId: String,
    priorChoices: List<RunChoiceRecord>,
    flags: Set<String>,
    /** 이미 지나간 자동 사건 id. 회상이 자동 사건 카드에서 먼저 나왔으면 다시 보이지 않게 한다. */
    priorAutoEventIds: Set<String> = emptySet(),
): RecallLine? =
    recallSlots.firstOrNull { slot ->
        eventId in slot.showIn &&
            slot.sourceIn(priorChoices) &&
            flagsOk(flags, slot.requiredFlags, slot.blockedFlags) &&
            // 사건 회상은 한 인생에 한 번: 앞서 지나온 다른 표시 사건(자동 사건 포함)에서 이미 나왔으면 다시 보이지 않는다.
            priorChoices.none { it.eventId != eventId && it.eventId in slot.showIn } &&
            priorAutoEventIds.none { it != eventId && it in slot.showIn }
    }?.let { RecallLine(it.slotId, it.line) }

/** 결말 회상(최대 3줄). 근거 선택이 실제 기록에 있는 슬롯만, 콘텐츠 순서대로. */
fun ScenarioExtensions.recallsForEnding(choices: List<RunChoiceRecord>, flags: Set<String>): List<RecallLine> =
    recallSlots.asSequence()
        .filter { ScenarioExtensions.ENDING in it.showIn && it.sourceIn(choices) && flagsOk(flags, it.requiredFlags, it.blockedFlags) }
        .distinctBy { it.line }
        .take(ScenarioExtensions.ENDING_RECALL_LIMIT)
        .map { RecallLine(it.slotId, it.line) }
        .toList()

private val FIELD_LABELS = mapOf("EDUCATION" to "교육", "CAREER" to "직업", "FAMILY" to "가족")

/**
 * 결말의 "내 인생의 계기" 카드 2~3장(콘텐츠 규칙: 실제 선택 기록이 있는 카드만, priority 높은 순으로 전체 2~3장).
 * 교육 · 직업 · 가족이 고루 보이도록 분야마다 가장 높은 카드를 먼저 놓고, 3장이 안 되면 남은 카드 중 priority 순으로 채운다.
 * 실제 기록이 있는 카드가 한 장뿐이면 한 장만, 없으면 빈 목록이다(카드를 지어내지 않는다).
 */
fun ScenarioExtensions.turningPointsFor(
    choices: List<RunChoiceRecord>,
    flags: Set<String>,
    periods: List<PeriodDefinition>,
): List<TurningPointUi> {
    val qualified = turningPoints.values.flatten()
        .filter { card -> flags.containsAll(card.requiredFlags) && choices.any { it.eventId == card.eventId } }
        .distinctBy { it.eventId }
    val bestPerField = qualified.groupBy { it.field }.values
        .mapNotNull { cards -> cards.maxByOrNull { it.priority } }
        .sortedByDescending { it.priority }
    val picked = (bestPerField + (qualified - bestPerField.toSet()).sortedByDescending { it.priority })
        .take(ScenarioExtensions.TURNING_POINT_MAX)
    return picked.mapNotNull { card ->
        val record = choices.lastOrNull { it.eventId == card.eventId } ?: return@mapNotNull null
        val label = displayChoiceLabel(record.choiceLabel)
        TurningPointUi(
            cardId = card.cardId,
            fieldLabel = FIELD_LABELS[card.field] ?: card.field,
            eventId = card.eventId,
            title = card.title.ifBlank { record.eventTitle },
            periodName = periods.firstOrNull { it.period == record.period }?.displayName ?: record.period.name,
            yearInPeriod = record.yearInPeriod,
            choiceLabel = label,
            text = card.line.replace("{choiceLabel}", label),
        )
    }
}

/**
 * 관계 플래그가 실제로 있는지. 콘텐츠는 "그 밖의 연인"처럼 여러 관계 중 하나면 되는 사람도 한 목록에 적으므로,
 * 플래그가 하나뿐이면 그것이 있어야 하고, 여럿이면 그중 하나라도 있으면 관계가 생긴 것으로 본다.
 */
private fun NpcReappearance.related(flags: Set<String>): Boolean =
    relationFlags.isEmpty() || relationFlags.any(flags::contains)

private data class NpcOrigin(val title: String, val period: LifePeriod)

/** 처음 만난 실제 기록. 선택 기록에서 먼저 찾고, 자동 사건에서 시작한 관계(예: 다시 만난 인연)는 자동 사건 기록에서 찾는다. */
private fun NpcReappearance.origin(choices: List<RunChoiceRecord>, autos: List<AutoAppearance>): NpcOrigin? =
    choices.firstOrNull { record ->
        record.eventId in originEventIds && (originChoiceIds.isEmpty() || record.choiceId in originChoiceIds)
    }?.let { NpcOrigin(it.eventTitle, it.period) }
        ?: autos.firstOrNull { it.eventId in originEventIds }?.let { NpcOrigin(it.title, it.period) }

/**
 * 지금 사건(또는 자동 사건)에 다시 나오는 사람. 관계 플래그와 처음 만난 실제 기록이 모두 있을 때만.
 * [appearanceType]은 콘텐츠의 등장 종류("EVENT" 결정 사건, "AUTO" 자동 사건 = 연도 이야기).
 */
fun ScenarioExtensions.npcForEvent(
    eventId: String,
    priorChoices: List<RunChoiceRecord>,
    flags: Set<String>,
    periods: List<PeriodDefinition>,
    appearanceType: String = ScenarioExtensions.APPEARANCE_EVENT,
    /** 이미 지나간 자동 사건. 자동 사건에서 시작한 관계의 처음 만난 기록으로 쓴다. */
    priorAutoAppearances: List<AutoAppearance> = emptyList(),
): NpcReturnUi? = npcs.firstNotNullOfOrNull { npc ->
    if (!RunExtensionApi.canNpcReturn(npc.originEventIds, npc.originChoiceIds, npc.relationFlags, priorChoices, flags)) {
        return@firstNotNullOfOrNull null
    }
    val appearance = npc.appearances.firstOrNull { it.type == "EVENT" && it.id == eventId } ?: return@firstNotNullOfOrNull null
    if (appearance.type != appearanceType) return@firstNotNullOfOrNull null
    if (!flagsOk(flags, appearance.requiredFlags, appearance.blockedFlags)) return@firstNotNullOfOrNull null
    val origin = npc.origin(priorChoices, priorAutoAppearances) ?: return@firstNotNullOfOrNull null
    NpcReturnUi(
        npcId = npc.npcId,
        label = npc.label,
        originTitle = origin.title,
        originPeriodName = periods.firstOrNull { it.period == origin.period }?.displayName ?: origin.period.name,
    )
}

/**
 * 기록 화면용: 이번 인생에서 실제로 다시 만난 사람들(처음 만난 기록과 다시 만난 사건 · 자동 사건 기록이 모두 있는 경우만).
 * [autoAppearances]는 실제로 지나간 자동 사건 (id, 기록 화면에 쓸 이름) 목록이다.
 */
fun ScenarioExtensions.npcsMetAgain(
    choices: List<RunChoiceRecord>,
    flags: Set<String>,
    periods: List<PeriodDefinition>,
    autoAppearances: List<AutoAppearance> = emptyList(),
): List<NpcReturnUi> = npcs.mapNotNull { npc ->
    if (!RunExtensionApi.canNpcReturn(npc.originEventIds, npc.originChoiceIds, npc.relationFlags, choices, flags)) {
        return@mapNotNull null
    }
    val origin = npc.origin(choices, autoAppearances) ?: return@mapNotNull null
    val eventIds = npc.appearances.filter { it.type == ScenarioExtensions.APPEARANCE_EVENT }.map { it.id }.toSet()
    val autoIds = npc.appearances.filter { it.type == ScenarioExtensions.APPEARANCE_AUTO }.map { it.id }.toSet()
    val again = choices.filter { it.eventId in eventIds && it.eventId !in npc.originEventIds }.map { it.eventTitle } +
        autoAppearances.filter { it.eventId in autoIds && it.eventId !in npc.originEventIds }.map { it.title }
    if (again.isEmpty()) return@mapNotNull null
    NpcReturnUi(
        npcId = npc.npcId,
        label = npc.label,
        originTitle = origin.title,
        originPeriodName = periods.firstOrNull { it.period == origin.period }?.displayName ?: origin.period.name,
        metAgainTitles = again.distinct(),
    )
}
