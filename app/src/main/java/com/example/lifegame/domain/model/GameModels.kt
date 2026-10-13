package com.example.lifegame.domain.model

enum class LifeStage {
    INFANT,
    CHILD,
    TEEN,
    ADULT,
}

enum class EventType {
    NORMAL,
    SPECIAL,
}

enum class GameScreenState {
    EVENT,
    CHOICE_RESULT,
    ENDING,
}

data class Stats(
    val health: Int = DEFAULT_STAT,
    val fitness: Int = DEFAULT_STAT,
    val intelligence: Int = DEFAULT_STAT,
    val social: Int = DEFAULT_STAT,
    val wealth: Int = DEFAULT_STAT,
    val happiness: Int = DEFAULT_STAT,
    val luck: Int = DEFAULT_STAT,
) {
    companion object {
        const val DEFAULT_STAT = 50
    }
}

data class StatDelta(
    val health: Int = 0,
    val fitness: Int = 0,
    val intelligence: Int = 0,
    val social: Int = 0,
    val wealth: Int = 0,
    val happiness: Int = 0,
    val luck: Int = 0,
)

data class StatConditions(
    val minHealth: Int? = null,
    val minFitness: Int? = null,
    val minIntelligence: Int? = null,
    val minSocial: Int? = null,
    val minWealth: Int? = null,
    val minHappiness: Int? = null,
    val minLuck: Int? = null,
    val maxHealth: Int? = null,
    val maxFitness: Int? = null,
    val maxIntelligence: Int? = null,
    val maxSocial: Int? = null,
    val maxWealth: Int? = null,
    val maxHappiness: Int? = null,
    val maxLuck: Int? = null,
) {
    fun matches(stats: Stats): Boolean =
        stats.health.isWithin(minHealth, maxHealth) &&
            stats.fitness.isWithin(minFitness, maxFitness) &&
            stats.intelligence.isWithin(minIntelligence, maxIntelligence) &&
            stats.social.isWithin(minSocial, maxSocial) &&
            stats.wealth.isWithin(minWealth, maxWealth) &&
            stats.happiness.isWithin(minHappiness, maxHappiness) &&
            stats.luck.isWithin(minLuck, maxLuck)

    private fun Int.isWithin(min: Int?, max: Int?): Boolean =
        (min == null || this >= min) && (max == null || this <= max)
}

data class EventConditions(
    val stats: StatConditions = StatConditions(),
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
)

data class Choice(
    val choiceId: String,
    val label: String,
    val statDelta: StatDelta = StatDelta(),
    val resultText: String,
    val addFlags: Set<String> = emptySet(),
)

data class GameEvent(
    val eventId: String,
    val stage: LifeStage,
    val type: EventType = EventType.NORMAL,
    val title: String,
    val text: String,
    val imageId: String,
    val conditions: EventConditions = EventConditions(),
    val choices: List<Choice>,
    val priority: Int = 0,
    val isFallback: Boolean = false,
)

data class ImageAsset(
    val imageId: String,
    val usage: String,
    val stage: String,
    val description: String,
    val fileName: String,
    val fallbackText: String,
)


data class EndingConditions(
    val stats: StatConditions = StatConditions(),
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
)

data class Ending(
    val endingId: String,
    val title: String,
    val summary: String,
    val priority: Int,
    val conditions: EndingConditions = EndingConditions(),
    val isDefault: Boolean = false,
)

data class ChoiceHistory(
    val eventOccurrenceId: String,
    val eventId: String,
    val choiceId: String,
    val beforeStats: Stats,
    val afterStats: Stats,
    val resultText: String,
)

data class GameProgress(
    val runId: String,
    val contentVersion: String = "1",
    val saveFormatVersion: Int = 1,
    val selectedImageRef: String? = null,
    val stage: LifeStage = LifeStage.INFANT,
    val currentEventOccurrenceId: String,
    val screenState: GameScreenState = GameScreenState.EVENT,
    val stats: Stats = Stats(),
    val flags: Set<String> = emptySet(),
    val completedEventIds: Set<String> = emptySet(),
    val handledOccurrenceIds: Set<String> = emptySet(),
    val choiceHistory: List<ChoiceHistory> = emptyList(),
    val specialEventCount: Int = 0,
    val stageEventCount: Int = 0,
)

data class ChoiceResult(
    val progress: GameProgress,
    val beforeStats: Stats,
    val afterStats: Stats,
    val appliedDelta: StatDelta,
    val resultText: String,
    val wasApplied: Boolean,
)

/** v4에서 한 장(chapter)을 구성하는 생애 시기다. */
enum class LifePeriod {
    INFANT,
    CHILD,
    TEEN,
    YOUNG_ADULT,
    MIDDLE,
}

data class PeriodDefinition(
    val period: LifePeriod,
    val order: Int,
    val years: Int,
    val displayName: String = period.name,
    val chapterTitle: String = displayName,
    val startLabel: String = "",
    val endLabel: String = "",
) {
    init {
        require(order >= 0) { "period order must be zero or greater" }
        require(years > 0) { "period years must be greater than zero" }
    }
}

data class Focus(
    val focusId: String,
    val period: LifePeriod,
    val label: String,
    val statEffects: StatDelta,
    val description: String,
    val sceneImageId: String? = null,
    val momentImageId: String? = null,
    val yearlyLogTemplates: List<String> = emptyList(),
)

enum class TransitionCue {
    NOISE,
    DISTORT,
    CHAPTER,
}

enum class DecisionKind {
    MILESTONE,
    INCIDENT,
    BIG_INCIDENT,
}

/**
 * 능력치 50을 기준으로 10점 차이마다 결과 가중치에 더할 값이다.
 * 양수는 해당 능력치가 높을수록, 음수는 낮을수록 결과 가능성을 높인다.
 */
data class ChanceWeights(
    val health: Int = 0,
    val fitness: Int = 0,
    val intelligence: Int = 0,
    val social: Int = 0,
    val wealth: Int = 0,
    val happiness: Int = 0,
    val luck: Int = 0,
)

data class Outcome(
    val outcomeId: String,
    val resultText: String,
    val statDelta: StatDelta = StatDelta(),
    val addFlags: Set<String> = emptySet(),
    val chance: Int,
    val primary: Boolean = false,
    val chanceWeights: ChanceWeights = ChanceWeights(),
    val hidden: Boolean = false,
    val death: Boolean = false,
) {
    init {
        require(chance >= 0) { "outcome chance must be zero or greater" }
    }
}

data class DecisionOption(
    val choiceId: String,
    val label: String,
    val outcomes: List<Outcome>,
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
) {
    init {
        require(outcomes.isNotEmpty()) { "decision option must have at least one outcome" }
        require(outcomes.sumOf { it.chance } > 0) { "decision option must have a positive outcome chance" }
    }
}

data class DecisionEvent(
    val eventId: String,
    val periods: Set<LifePeriod>,
    val kind: DecisionKind,
    val title: String,
    val text: String,
    val choices: List<DecisionOption>,
    val weight: Int = 1,
    val yearInPeriod: Int? = null,
    val requiredFlags: Set<String> = emptySet(),
    val blockedFlags: Set<String> = emptySet(),
    val statConditions: StatConditions = StatConditions(),
    val povImageId: String? = null,
    val momentImageId: String? = null,
) {
    init {
        require(periods.isNotEmpty()) { "decision event must target at least one period" }
        require(choices.isNotEmpty()) { "decision event must have at least one choice" }
        require(weight > 0) { "decision event weight must be greater than zero" }
        require(yearInPeriod == null || yearInPeriod > 0) { "milestone year must be greater than zero" }
    }
}

data class DecisionChoicePreview(
    val choiceId: String,
    val label: String,
    val likelihood: OutcomeLikelihood? = null,
    val keyStats: List<String> = emptyList(),
)

/** UI가 엔진의 가중치 계산을 복제하지 않고 선택지 가능성을 표시하기 위한 질적 구간이다. */
enum class OutcomeLikelihood(val label: String) {
    LOW("낮음"),
    HALF("반반쯤"),
    HIGH("높음"),
    VERY_HIGH("매우 높음"),
}

/** UI에는 결과 목록을 포함하지 않아 hidden 결과가 preview로 새지 않는다. */
data class PendingDecision(
    val eventId: String,
    val kind: DecisionKind,
    val title: String,
    val text: String,
    val choices: List<DecisionChoicePreview>,
    val povImageId: String? = null,
)

data class LifeMoment(
    val period: LifePeriod,
    val yearInPeriod: Int,
    val text: String,
    val momentImageId: String? = null,
)

data class PeriodRunState(
    val runId: String,
    val runSeed: Long,
    val period: LifePeriod,
    val yearInPeriod: Int = 1,
    val selectedFocusId: String? = null,
    val stats: Stats = Stats(),
    val flags: Set<String> = emptySet(),
    val pendingDecisionId: String? = null,
    val moments: List<LifeMoment> = emptyList(),
    val isEnding: Boolean = false,
)

enum class TurnPhase {
    CHAPTER,
    AUTO_PROGRESS,
    DECISION,
    ENDING,
}

data class TurnStep(
    val state: PeriodRunState,
    val phase: TurnPhase,
    val recordText: String? = null,
    val transitionCue: TransitionCue? = null,
    val pendingDecision: PendingDecision? = null,
    val sceneImageId: String? = null,
    val momentImageId: String? = null,
    val canAutoAdvance: Boolean = false,
    val needsFocusSelection: Boolean = false,
)

data class OutcomeResolution(
    val outcome: Outcome,
    val step: TurnStep,
)

// Notion 카드에서 사용하는 초기 명칭과 GitHub 명세의 확정 명칭을 함께 지원한다.
typealias PlayerStats = Stats
typealias Event = GameEvent
