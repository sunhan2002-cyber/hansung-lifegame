package com.example.lifegame.ui

import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.PendingDecision
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/** v4 화면 단계. 엔진의 [TurnPhase]를 화면 단위로 옮긴 것이다. */
enum class PlayPhase { CHAPTER, AUTO, DECISION, ENDED }

/** 자동 진행 1배속에서 한 해가 걸리는 시간 (R-21). */
const val YEAR_MILLIS_AT_1X = 3_000L
val PLAY_SPEEDS = listOf(1, 2, 4)

/** 남자는 청년기에 입영 통지서(adult_071)가 나오도록 시작 플래그를 받는다. */
const val MILITARY_SERVICE_FLAG = "military_service_required"

enum class Gender(val label: String, val flag: String) {
    MALE("남", "gender_male"),
    FEMALE("여", "gender_female"),
}

/** 결과 가능성 문구 (R-43). 숫자 확률은 화면에 쓰지 않는다. */
enum class Likelihood(val label: String) {
    LOW("낮음"),
    HALF("반반쯤"),
    HIGH("높음"),
    VERY_HIGH("매우 높음"),
}

fun likelihoodOf(percent: Int): Likelihood = when {
    percent < 35 -> Likelihood.LOW
    percent < 60 -> Likelihood.HALF
    percent < 80 -> Likelihood.HIGH
    else -> Likelihood.VERY_HIGH
}

data class StatChangeUi(val name: String, val delta: Int)

/** 시기 진행 막대 한 칸. [fill]은 0~1. */
data class PeriodMarkUi(val label: String, val fill: Float, val isCurrent: Boolean)

data class FocusOptionUi(
    val focusId: String,
    val label: String,
    val description: String,
    val effects: List<StatChangeUi>,
)

data class LogLineUi(val key: String, val label: String, val text: String)

data class DecisionChoiceUi(
    val choiceId: String,
    val key: String,
    val label: String,
    /** "가능성 반반쯤 · 지력" 또는 결과가 하나뿐이면 "건강 · 행복". 숨은 결과는 들어가지 않는다. */
    val hint: String,
    val statConditionText: String? = null,
    val healthEffectText: String? = null,
    val available: Boolean = true,
    val lockReason: String? = null,
    /** 잠긴 이유를 하나씩. 예: "지력 60 필요 (지금 52)", "대학 진학 필요" (8주차) */
    val lockReasons: List<String> = emptyList(),
)

data class DecisionUi(
    val eventId: String,
    val kind: DecisionKind,
    val title: String,
    val text: String,
    val imageId: String,
    val imageFile: String?,
    val choices: List<DecisionChoiceUi>,
) {
    /** 갈림길이 아닌 결정은 모두 돌발이다. 모든 돌발은 왜곡 전환을 쓴다 (R-23). */
    val isIncident: Boolean get() = kind != DecisionKind.MILESTONE
}

data class OutcomeUi(
    val choiceLabel: String,
    val text: String,
    val changes: List<StatChangeUi>,
    val endsLife: Boolean,
    /** 이번 선택으로 새로 생긴 교육 · 직업 · 관계 · 가족 · 주거 · 경제력 · 업적 사실 */
    val factChanges: List<FactChangeUi> = emptyList(),
    /** 이번 선택으로 새로 조건이 맞게 된 사건 제목 */
    val openedEvents: List<String> = emptyList(),
)

data class StarterUi(
    val starterId: String,
    val name: String,
    val description: String,
    val imageId: String?,
    val statDelta: StatDelta,
    val startFlags: Set<String>,
) {
    val effects: List<StatChangeUi> get() = statDelta.toChanges()
}

data class NewLifeProfile(
    val name: String,
    val gender: Gender,
    val starterId: String?,
)

/** 홈 화면의 "지금 사는 인생" 카드. */
data class LifeCardUi(
    val playerName: String,
    val place: String,
    val focusLabel: String?,
    val stats: Stats,
    val lastLine: String?,
)

/** 홈 화면의 "지난 인생" 한 줄. 이번 주에는 앱을 켜 둔 동안만 남는다. */
data class FinishedLifeUi(
    val key: String,
    val playerName: String,
    val summary: String,
)

data class PeriodPlayUiState(
    val runKey: String,
    val playerName: String,
    val characterImageId: String? = null,
    val characterImageFile: String? = null,
    val phase: PlayPhase,
    val periodName: String,
    /** "제3장"처럼 장 번호만 */
    val chapterLabel: String,
    val periodYears: Int,
    val periodIntro: String,
    /** 화면에 보이는 해. 방금 지나간 해 또는 결정이 걸린 해. */
    val yearInPeriod: Int,
    val periodMarks: List<PeriodMarkUi>,
    val previousPeriodName: String?,
    val stats: Stats,
    val focus: FocusOptionUi?,
    val focusOptions: List<FocusOptionUi>,
    val sceneImageId: String,
    val sceneFile: String?,
    val log: List<LogLineUi>,
    /** 해가 바뀔 때마다 달라지는 키. 필름 전환과 자동 진행 타이머가 이 값을 본다. */
    val yearKey: String,
    val decision: DecisionUi?,
    val outcome: OutcomeUi?,
    val isDead: Boolean,
    val autoPlaying: Boolean,
    val speed: Int,
    /** 지금까지 쌓인 인생 사실 (8주차) */
    val facts: LifeFactsUi = LifeFactsUi(),
    /** 지금 가장 높은 능력치 */
    val topStat: StatValueUi? = null,
    /** 끝났을 때만 채운다 */
    val ending: EndingSummaryUi? = null,
) {
    val place: String get() = "$periodName · ${yearInPeriod}번째 해"
}

/** 화면 상태를 만들 때 필요한 콘텐츠 묶음. 이름 치환은 이미 끝난 상태다. */
data class PlayContent(
    val periods: List<PeriodDefinition>,
    val focuses: List<Focus>,
    val decisions: Map<String, DecisionEvent>,
    val imageFiles: Map<String, String>,
    /** 플래그 → 인생 사실 (life_facts.sample.json) */
    val factCatalog: LifeFactCatalog = LifeFactCatalog.EMPTY,
    /** 잠금 이유에 쓸 플래그 이름표 */
    val lookup: FlagLookup = FlagLookup(catalog = factCatalog),
)

fun StatDelta.toChanges(): List<StatChangeUi> =
    toDisplayMap()
        .filterValues { it != 0 }
        .map { (name, delta) -> StatChangeUi(name, delta) }
        .sortedByDescending { it.delta > 0 }

fun ChanceWeights.toDisplayMap(): Map<String, Int> = mapOf(
    "건강" to health,
    "운동능력" to fitness,
    "지력" to intelligence,
    "사회성" to social,
    "경제력" to wealth,
    "행복" to happiness,
    "운" to luck,
)

fun Stats.plusClamped(delta: StatDelta): Stats = Stats(
    health = (health + delta.health).coerceIn(0, STAT_MAX),
    fitness = (fitness + delta.fitness).coerceIn(0, STAT_MAX),
    intelligence = (intelligence + delta.intelligence).coerceIn(0, STAT_MAX),
    social = (social + delta.social).coerceIn(0, STAT_MAX),
    wealth = (wealth + delta.wealth).coerceIn(0, STAT_MAX),
    happiness = (happiness + delta.happiness).coerceIn(0, STAT_MAX),
    luck = (luck + delta.luck).coerceIn(0, STAT_MAX),
)

fun statChanges(before: Stats, after: Stats): List<StatChangeUi> {
    val old = before.toDisplayMap()
    return after.toDisplayMap()
        .map { (name, value) -> StatChangeUi(name, value - (old[name] ?: value)) }
        .filter { it.delta != 0 }
}

fun PeriodDefinition.shortName(): String = displayName.removeSuffix("기")

/** "제3장 청소년기" → "제3장" */
fun PeriodDefinition.chapterLabel(): String =
    chapterTitle.replace(displayName, "").trim()

/** 받침에 맞춰 "으로/로"를 붙인다. 예: 학업 → 학업으로, 놀기 → 놀기로, 일 → 일로 */
fun String.withRo(): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return "$this(으)로"
    val finalConsonant = (last - '가') % 28
    return this + if (finalConsonant == 0 || finalConsonant == 8) "로" else "으로"
}

/**
 * GameEngine이 결과를 뽑을 때 쓰는 가중치와 같은 식이다 (50 기준, 10점마다 가중치만큼).
 * 엔진이 미리보기용 값을 내주게 되면 이 함수는 지운다.
 */
internal fun effectiveChance(outcome: Outcome, stats: Stats): Int {
    val w = outcome.chanceWeights
    val base = Stats.DEFAULT_STAT
    val adjustment =
        ((stats.health - base) * w.health) / 10 +
            ((stats.fitness - base) * w.fitness) / 10 +
            ((stats.intelligence - base) * w.intelligence) / 10 +
            ((stats.social - base) * w.social) / 10 +
            ((stats.wealth - base) * w.wealth) / 10 +
            ((stats.happiness - base) * w.happiness) / 10 +
            ((stats.luck - base) * w.luck) / 10
    return (outcome.chance + adjustment).coerceAtLeast(0)
}

/**
 * 대표 결과가 나올 확률(0~100). 데이터에서 대표 결과(primary)는 항상 첫 번째로 쓰므로
 * 숨지 않은 첫 결과를 대표로 본다. 숨은 결과도 분모에는 들어간다.
 */
fun DecisionOption.primaryChancePercent(stats: Stats): Int {
    val effective = outcomes.map { effectiveChance(it, stats) }
    val weights = if (effective.sum() > 0) effective else outcomes.map { it.chance }
    val total = weights.sum()
    if (total <= 0) return 0
    val primary = outcomes.indexOfFirst { !it.hidden }.takeIf { it >= 0 } ?: 0
    return (weights[primary] * 100f / total).roundToInt()
}

/** 선택지 아래 한 줄. 숨은 결과의 문장 · 확률 · 능력치는 쓰지 않는다 (R-44). */
fun DecisionOption.hint(stats: Stats): String {
    val primary = outcomes.firstOrNull { !it.hidden } ?: return ""
    val keyStats = primary.chanceWeights.toDisplayMap().filterValues { it != 0 }.keys
        .ifEmpty { primary.statDelta.toDisplayMap().filterValues { it != 0 }.keys }
        .take(2)
    if (outcomes.size <= 1) return keyStats.joinToString(" · ")
    val word = "가능성 ${likelihoodOf(primaryChancePercent(stats)).label}"
    return (listOf(word) + keyStats).joinToString(" · ")
}

fun Focus.toOptionUi(): FocusOptionUi = FocusOptionUi(
    focusId = focusId,
    label = label,
    description = description,
    effects = statEffects.toChanges(),
)

fun buildDecisionUi(
    pending: PendingDecision,
    event: DecisionEvent?,
    stats: Stats,
    imageFiles: Map<String, String>,
    flags: Set<String> = emptySet(),
    lookup: FlagLookup = FlagLookup(),
): DecisionUi {
    val options = event?.choices.orEmpty().associateBy { it.choiceId }
    val imageId = pending.povImageId.orEmpty()
    return DecisionUi(
        eventId = pending.eventId,
        kind = pending.kind,
        title = pending.title,
        text = pending.text,
        imageId = imageId,
        imageFile = imageFiles[imageId],
        choices = pending.choices.mapIndexed { index, preview ->
            val option = options[preview.choiceId]
            DecisionChoiceUi(
                choiceId = preview.choiceId,
                key = ('A' + index).toString(),
                label = preview.label,
                hint = options[preview.choiceId]?.hint(stats).orEmpty(),
                statConditionText = preview.statConditionText,
                healthEffectText = option?.healthEffectText(),
                available = preview.available,
                lockReason = preview.lockReason,
                lockReasons = if (preview.available) {
                    emptyList()
                } else {
                    option?.lockReasons(stats, flags, lookup).orEmpty()
                        .ifEmpty { listOfNotNull(preview.lockReason) }
                },
            )
        },
    )
}

private fun DecisionOption.healthEffectText(): String? {
    val changes = outcomes.filterNot { it.hidden }
        .map { it.statDelta.health + it.lifeDelta }
        .distinct()
    val value = when {
        changes.size == 1 -> changes.single()
        else -> outcomes.firstOrNull { it.primary && !it.hidden }?.let { it.statDelta.health + it.lifeDelta }
            ?: outcomes.firstOrNull { !it.hidden }?.let { it.statDelta.health + it.lifeDelta }
            ?: return null
    }
    return when {
        value < 0 -> "건강 ${value} 감소"
        value > 0 -> "건강 +${value} 회복"
        else -> null
    }
}

fun buildLogLines(moments: List<LifeMoment>, periods: List<PeriodDefinition>): List<LogLineUi> {
    val names = periods.associate { it.period to it.displayName }
    return moments.mapIndexed { index, moment ->
        LogLineUi(
            key = "$index",
            label = "${names[moment.period] ?: moment.period.name} · ${moment.yearInPeriod}번째 해",
            text = moment.text,
        )
    }
}

/**
 * 엔진의 한 걸음을 화면 상태로 바꾼다.
 * [decisionYear]는 결정 결과를 보여 주는 동안 그 결정이 걸린 해를 붙잡아 둘 때 쓴다.
 */
fun buildPeriodPlayState(
    step: TurnStep,
    content: PlayContent,
    playerName: String,
    outcome: OutcomeUi? = null,
    decision: DecisionUi? = null,
    decisionYear: Int? = null,
    autoPlaying: Boolean = true,
    speed: Int = 1,
    isDead: Boolean = false,
    characterImageId: String? = null,
    choiceLog: List<ChoiceRecordUi> = emptyList(),
    endingTitle: String? = null,
    endingSummary: String? = null,
): PeriodPlayUiState {
    val state = step.state
    val periods = content.periods.sortedBy { it.order }
    val periodIndex = periods.indexOfFirst { it.period == state.period }.coerceAtLeast(0)
    val period = periods.getOrNull(periodIndex)
    val lastMoment = state.moments.lastOrNull()
    val endingPeriod = state.endingPeriod ?: if (state.isDead) lastMoment?.period else null
    val phase = when {
        decision != null || outcome != null -> PlayPhase.DECISION
        else -> when (step.phase) {
            TurnPhase.CHAPTER -> PlayPhase.CHAPTER
            TurnPhase.AUTO_PROGRESS -> PlayPhase.AUTO
            TurnPhase.DECISION -> PlayPhase.DECISION
            TurnPhase.ENDING -> PlayPhase.ENDED
        }
    }
    val shownYear = when {
        decisionYear != null -> decisionYear
        phase == PlayPhase.CHAPTER -> 1
        lastMoment != null && lastMoment.period == state.period -> lastMoment.yearInPeriod
        else -> state.yearInPeriod
    }
    val focus = content.focuses.firstOrNull { it.focusId == state.selectedFocusId }
    val facts = buildLifeFacts(state.flags, state.stats, content.factCatalog)
    val sceneImageId = step.sceneImageId ?: focus?.sceneImageId.orEmpty()
    val years = period?.years ?: 1
    return PeriodPlayUiState(
        runKey = state.runId,
        playerName = playerName,
        characterImageId = characterImageId,
        characterImageFile = characterImageId?.let { content.imageFiles[it] },
        phase = phase,
        periodName = (periods.firstOrNull { it.period == (endingPeriod ?: state.period) }?.displayName
            ?: (endingPeriod ?: state.period).name),
        chapterLabel = period?.chapterLabel().orEmpty(),
        periodYears = years,
        periodIntro = period?.startLabel.orEmpty(),
        yearInPeriod = shownYear,
        periodMarks = periods.mapIndexed { index, item ->
            PeriodMarkUi(
                label = item.shortName(),
                fill = when {
                    index < periodIndex -> 1f
                    index > periodIndex -> 0f
                    phase == PlayPhase.CHAPTER -> 0f
                    else -> (shownYear.toFloat() / years).coerceIn(0f, 1f)
                },
                isCurrent = index == periodIndex,
            )
        },
        previousPeriodName = periods.getOrNull(periodIndex - 1)?.displayName,
        stats = state.stats,
        focus = focus?.toOptionUi(),
        focusOptions = content.focuses.filter { it.period == state.period }.map { it.toOptionUi() },
        sceneImageId = sceneImageId,
        sceneFile = content.imageFiles[sceneImageId],
        log = buildLogLines(state.moments, periods),
        yearKey = "${state.runId}:${state.period}:$shownYear:${state.moments.size}",
        decision = decision ?: step.pendingDecision?.let { pending ->
            buildDecisionUi(
                pending = pending,
                event = content.decisions[pending.eventId],
                stats = state.stats,
                imageFiles = content.imageFiles,
                flags = state.flags,
                lookup = content.lookup,
            )
        },
        outcome = outcome,
        isDead = isDead || outcome?.endsLife == true,
        autoPlaying = autoPlaying,
        speed = speed,
        facts = facts,
        topStat = state.stats.topStat(),
        ending = if (phase == PlayPhase.ENDED) {
            buildEndingSummary(
                playerName = playerName,
                state = state,
                periods = periods,
                facts = facts,
                endingTitle = endingTitle,
                endingSummary = endingSummary,
                choices = choiceLog,
            )
        } else {
            null
        },
    )
}

/** characters.sample.json → 타고난 기질 목록. 읽지 못하면 빈 목록. */
fun parseStarters(json: String): List<StarterUi> = runCatching {
    val array = JSONArray(json)
    List(array.length()) { index ->
        val item = array.getJSONObject(index)
        StarterUi(
            starterId = item.getString("starterId"),
            name = item.getString("name"),
            description = item.optString("description"),
            imageId = item.optString("imageId").takeIf { it.isNotBlank() },
            statDelta = item.optJSONObject("startStatDelta").toStatDelta(),
            startFlags = item.optJSONArray("startFlags")?.let { flags ->
                List(flags.length()) { flags.getString(it) }.toSet()
            }.orEmpty(),
        )
    }
}.getOrElse { emptyList() }

private fun JSONObject?.toStatDelta(): StatDelta {
    fun value(korean: String, english: String): Int =
        this?.optInt(korean, this.optInt(english, 0)) ?: 0
    return StatDelta(
        health = value("건강", "health"),
        fitness = value("운동능력", "fitness"),
        intelligence = value("지력", "intelligence"),
        social = value("사회성", "social"),
        wealth = value("경제력", "wealth"),
        happiness = value("행복", "happiness"),
        luck = value("운", "luck"),
    )
}
