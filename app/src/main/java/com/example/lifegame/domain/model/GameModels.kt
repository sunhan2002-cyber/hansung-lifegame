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
)

data class ChoiceResult(
    val progress: GameProgress,
    val beforeStats: Stats,
    val afterStats: Stats,
    val appliedDelta: StatDelta,
    val resultText: String,
    val wasApplied: Boolean,
)

// Notion 카드에서 사용하는 초기 명칭과 GitHub 명세의 확정 명칭을 함께 지원한다.
typealias PlayerStats = Stats
typealias Event = GameEvent
