package com.example.lifegame.ui

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats

val STAT_NAMES = listOf("건강", "운동능력", "지력", "사회성", "경제력", "행복", "운")
const val STAT_MAX = 100
const val STAT_START = 50

data class UiChoiceResult(
    val eventId: String,
    val choice: Choice,
    val engineResult: ChoiceResult,
)
private const val DEFAULT_PLAYER_NAME = "선한"
private const val DEFAULT_PLAYER_NAME_CALL = "선한아"

fun String.replacePlayerPlaceholders(
    playerName: String = DEFAULT_PLAYER_NAME,
    playerNameCall: String = DEFAULT_PLAYER_NAME_CALL,
): String = this
    .replace("{nameCall}", playerNameCall)
    .replace("{name}", playerName)

data class LifeGameUiState(
    val characterId: Int? = null,
    val hasSaveData: Boolean = false,
    val progress: GameProgress? = null,
    val currentEvent: GameEvent? = null,
    val lastResult: UiChoiceResult? = null,
    val ending: Ending? = null,
    val contentError: String? = null,
    /** 기록 화면에서 지난 사건의 제목과 선택지 문구를 찾을 때 쓴다. */
    val eventsById: Map<String, GameEvent> = emptyMap(),
    /** imageId → 그림 파일 이름. 파일이 아직 없으면 필름 안에 자리 표시 그림을 그린다. */
    val imageFiles: Map<String, String> = emptyMap(),
) {
    val stats: Stats get() = progress?.stats ?: Stats()
}

fun LifeStage.displayName(): String = when (this) {
    LifeStage.INFANT -> "영아기"
    LifeStage.CHILD -> "유년기"
    LifeStage.TEEN -> "청소년기"
    LifeStage.ADULT -> "성인기"
}

fun LifeStage.chapter(): Int = ordinal + 1

/** 인생선(0~80세)에서 단계가 바뀌는 나이. 눈금으로 표시한다. */
val STAGE_START_AGES = listOf(5, 13, 20)
const val LIFELINE_MAX_AGE = 80

/**
 * 진행 모델에는 나이가 없어서, 단계와 그 단계에서 몇 번째 사건인지로 화면에 보일 나이를 정한다.
 * 단계당 사건 수(5개)에 맞춰 나눈 값이고, 성인기는 사건마다 4년씩 지난다.
 */
fun displayAge(stage: LifeStage, indexInStage: Int): Int {
    val i = indexInStage.coerceAtLeast(0)
    return when (stage) {
        LifeStage.INFANT -> listOf(0, 1, 2, 3, 4).getOrElse(i) { 4 }
        LifeStage.CHILD -> listOf(5, 7, 9, 11, 12).getOrElse(i) { 12 }
        LifeStage.TEEN -> listOf(13, 14, 16, 17, 19).getOrElse(i) { 19 }
        LifeStage.ADULT -> (20 + i * 4).coerceAtMost(LIFELINE_MAX_AGE - 1)
    }
}

fun Stats.toDisplayMap(): Map<String, Int> = mapOf(
    "건강" to health,
    "운동능력" to fitness,
    "지력" to intelligence,
    "사회성" to social,
    "경제력" to wealth,
    "행복" to happiness,
    "운" to luck,
)

fun StatDelta.toDisplayMap(): Map<String, Int> = mapOf(
    "건강" to health,
    "운동능력" to fitness,
    "지력" to intelligence,
    "사회성" to social,
    "경제력" to wealth,
    "행복" to happiness,
    "운" to luck,
)

/** 선택지 옆에 보여 줄 "영향을 받는 능력치" 이름 (최대 2개). */
fun Choice.affectedStatNames(): List<String> =
    statDelta.toDisplayMap().filterValues { it != 0 }.keys.take(2)

/** 기록 화면의 한 줄. */
data class LifeRecord(
    val key: String,
    val age: Int,
    val stage: LifeStage,
    val title: String,
    val imageId: String,
    val choiceLabel: String,
    val changes: Map<String, Int>,
    val isSpecial: Boolean,
    val startsStage: Boolean,
)

fun buildLifeRecords(
    history: List<ChoiceHistory>,
    eventsById: Map<String, GameEvent>,
): List<LifeRecord> {
    var stage: LifeStage? = null
    var indexInStage = 0
    return history.mapNotNull { record ->
        val event = eventsById[record.eventId] ?: return@mapNotNull null
        val startsStage = event.stage != stage
        if (startsStage) {
            stage = event.stage
            indexInStage = 0
        }
        val age = displayAge(event.stage, indexInStage)
        indexInStage += 1
        val before = record.beforeStats.toDisplayMap()
        LifeRecord(
            key = record.eventOccurrenceId,
            age = age,
            stage = event.stage,
            title = event.title.replacePlayerPlaceholders(),
            imageId = event.imageId,
            choiceLabel = event.choices.firstOrNull { it.choiceId == record.choiceId }
                ?.label?.replacePlayerPlaceholders().orEmpty(),
            changes = record.afterStats.toDisplayMap()
                .mapValues { (name, value) -> value - (before[name] ?: value) }
                .filterValues { it != 0 },
            isSpecial = event.type == EventType.SPECIAL,
            startsStage = startsStage,
        )
    }
}

/** 팝업에 넣을 사건의 첫 문장. 너무 짧으면 두 번째 문장까지. */
fun String.teaser(): String {
    val sentences = split(Regex("(?<=[.!?…])\\s+")).filter { it.isNotBlank() }
    if (sentences.isEmpty()) return this
    val first = sentences.first()
    return if (first.length < 18 && sentences.size > 1) "$first ${sentences[1]}" else first
}
