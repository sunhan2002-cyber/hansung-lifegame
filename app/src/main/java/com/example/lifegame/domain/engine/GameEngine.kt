package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats

class GameEngine(
    private val rollPercent: () -> Int = { (1..100).random() },
    private val maxSpecialEventsPerRun: Int = 2,
) {
    init {
        require(maxSpecialEventsPerRun >= 0) {
            "maxSpecialEventsPerRun must be zero or greater"
        }
    }

    fun clampStat(value: Int): Int = value.coerceIn(MIN_STAT, MAX_STAT)

    fun applyChoice(
        progress: GameProgress,
        event: GameEvent,
        choice: Choice,
    ): ChoiceResult {
        require(choice in event.choices) {
            "Choice ${choice.choiceId} does not belong to event ${event.eventId}"
        }

        val before = progress.stats
        val occurrenceId = progress.currentEventOccurrenceId
        if (occurrenceId in progress.handledOccurrenceIds) {
            return ChoiceResult(
                progress = progress,
                beforeStats = before,
                afterStats = before,
                appliedDelta = StatDelta(),
                resultText = choice.resultText,
                wasApplied = false,
            )
        }

        val after = before.apply(choice.statDelta)
        val history = ChoiceHistory(
            eventOccurrenceId = occurrenceId,
            eventId = event.eventId,
            choiceId = choice.choiceId,
            beforeStats = before,
            afterStats = after,
            resultText = choice.resultText,
        )
        val updated = progress.copy(
            screenState = GameScreenState.CHOICE_RESULT,
            stats = after,
            flags = progress.flags + choice.addFlags,
            completedEventIds = progress.completedEventIds + event.eventId,
            handledOccurrenceIds = progress.handledOccurrenceIds + occurrenceId,
            choiceHistory = progress.choiceHistory + history,
            specialEventCount = progress.specialEventCount + if (event.type == EventType.SPECIAL) 1 else 0,
        )

        return ChoiceResult(
            progress = updated,
            beforeStats = before,
            afterStats = after,
            appliedDelta = after - before,
            resultText = choice.resultText,
            wasApplied = true,
        )
    }

    fun canShowEvent(progress: GameProgress, event: GameEvent): Boolean {
        if (event.stage != progress.stage) return false
        if (event.eventId in progress.completedEventIds) return false
        if (!event.conditions.stats.matches(progress.stats)) return false
        if (!progress.flags.containsAll(event.conditions.requiredFlags)) return false
        if (progress.flags.any(event.conditions.blockedFlags::contains)) return false
        return true
    }

    fun pickNextEvent(
        progress: GameProgress,
        events: List<GameEvent>,
    ): GameEvent? {
        val candidates = events.filter { canShowEvent(progress, it) }
        val conditional = candidates.filterNot { it.isFallback }
        val selectable = conditional.ifEmpty { candidates.filter { it.isFallback } }
        return selectable.sortedWith(
            compareByDescending<GameEvent> { it.priority }
                .thenBy { it.eventId },
        ).firstOrNull()
    }

    fun resolveEnding(progress: GameProgress, endings: List<Ending>): Ending {
        val matching = endings
            .asSequence()
            .filterNot { it.isDefault }
            .filter { it.conditions.stats.matches(progress.stats) }
            .filter { progress.flags.containsAll(it.conditions.requiredFlags) }
            .filterNot { ending -> progress.flags.any(ending.conditions.blockedFlags::contains) }
            .sortedWith(
                compareByDescending<Ending> { it.priority }
                    .thenBy { it.endingId },
            )
            .firstOrNull()

        return matching ?: endings
            .filter { it.isDefault }
            .minByOrNull { it.endingId }
            ?: error("At least one default ending is required")
    }

    /**
     * Notion 카드의 초기 메서드명과 호환되는 진입점이다.
     * 실제 판정 규칙은 GitHub 명세의 [resolveEnding] 한 곳에서만 관리한다.
     */
    fun checkEnding(progress: GameProgress, endings: List<Ending>): Ending =
        resolveEnding(progress, endings)

    fun shouldTriggerSpecialEvent(
        progress: GameProgress,
        chancePercent: Int,
    ): Boolean {
        if (progress.specialEventCount >= maxSpecialEventsPerRun) return false
        val normalizedChance = chancePercent.coerceIn(0, 100)
        if (normalizedChance == 0) return false
        if (normalizedChance == 100) return true
        return rollPercent().coerceIn(1, 100) <= normalizedChance
    }

    private fun Stats.apply(delta: StatDelta): Stats = copy(
        health = addAndClamp(health, delta.health),
        fitness = addAndClamp(fitness, delta.fitness),
        intelligence = addAndClamp(intelligence, delta.intelligence),
        social = addAndClamp(social, delta.social),
        wealth = addAndClamp(wealth, delta.wealth),
        happiness = addAndClamp(happiness, delta.happiness),
        luck = addAndClamp(luck, delta.luck),
    )

    private operator fun Stats.minus(before: Stats): StatDelta = StatDelta(
        health = health - before.health,
        fitness = fitness - before.fitness,
        intelligence = intelligence - before.intelligence,
        social = social - before.social,
        wealth = wealth - before.wealth,
        happiness = happiness - before.happiness,
        luck = luck - before.luck,
    )

    private fun addAndClamp(current: Int, delta: Int): Int =
        (current.toLong() + delta.toLong())
            .coerceIn(MIN_STAT.toLong(), MAX_STAT.toLong())
            .toInt()

    private companion object {
        const val MIN_STAT = 0
        const val MAX_STAT = 100
    }
}
