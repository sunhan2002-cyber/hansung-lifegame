package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats

enum class EndingSelectionReason {
    HIGHEST_PRIORITY,
    DEFAULT_FALLBACK,
}

/**
 * 결말 판정 결과와 판정 근거를 함께 제공한다.
 * [eligibleEndingIds]는 우선순위 내림차순, 같은 우선순위는 ID 오름차순으로 정렬된다.
 */
data class EndingDecision(
    val selected: Ending,
    val eligibleEndingIds: List<String>,
    val reason: EndingSelectionReason,
)

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

    fun createInitialProgress(runId: String): GameProgress {
        require(runId.isNotBlank()) { "runId must not be blank" }
        return GameProgress(
            runId = runId,
            stage = LifeStage.INFANT,
            currentEventOccurrenceId = "$runId:pending:0",
            stats = Stats(),
        )
    }

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
            stageEventCount = progress.stageEventCount + 1,
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
        val candidates = events.filter {
            it.type == EventType.NORMAL && canShowEvent(progress, it)
        }
        val conditional = candidates.filterNot { it.isFallback }
        val selectable = conditional.ifEmpty { candidates.filter { it.isFallback } }
        return selectable.sortedWith(
            compareByDescending<GameEvent> { it.priority }
                .thenBy { it.eventId },
        ).firstOrNull()
    }

    fun pickSpecialEvent(
        progress: GameProgress,
        events: List<GameEvent>,
        chancePercent: Int,
    ): GameEvent? {
        if (!shouldTriggerSpecialEvent(progress, chancePercent)) return null
        return events
            .asSequence()
            .filter { it.type == EventType.SPECIAL }
            .filter { canShowEvent(progress, it) }
            .sortedWith(
                compareByDescending<GameEvent> { it.priority }
                    .thenBy { it.eventId },
            )
            .firstOrNull()
    }

    fun shouldMoveNextStage(progress: GameProgress): Boolean =
        progress.stage != LifeStage.ADULT && progress.stageEventCount >= EVENTS_PER_STAGE

    fun moveNextStage(progress: GameProgress): GameProgress {
        val nextStage = when (progress.stage) {
            LifeStage.INFANT -> LifeStage.CHILD
            LifeStage.CHILD -> LifeStage.TEEN
            LifeStage.TEEN -> LifeStage.ADULT
            LifeStage.ADULT -> LifeStage.ADULT
        }
        if (nextStage == progress.stage) return progress
        return progress.copy(
            stage = nextStage,
            stageEventCount = 0,
            screenState = GameScreenState.EVENT,
            currentEventOccurrenceId = "${progress.runId}:pending:${progress.choiceHistory.size}",
        )
    }

    /**
     * 선택 결과 화면 이후에 다음 사건을 정하고 화면에서 바로 사용할 진행 상태를 반환한다.
     * 현재 단계에 후보가 없으면 다음 단계도 확인하며, 성인기까지 후보가 없으면 결말로 이동한다.
     */
    fun advanceAfterResult(
        progress: GameProgress,
        events: List<GameEvent>,
    ): GameProgress {
        var candidateProgress = if (shouldMoveNextStage(progress)) {
            moveNextStage(progress)
        } else {
            progress
        }

        while (true) {
            val nextEvent = pickSpecialEvent(
                progress = candidateProgress,
                events = events,
                chancePercent = DEFAULT_SPECIAL_EVENT_CHANCE_PERCENT,
            ) ?: pickNextEvent(candidateProgress, events)

            if (nextEvent != null) {
                return candidateProgress.copy(
                    screenState = GameScreenState.EVENT,
                    currentEventOccurrenceId =
                        "${candidateProgress.runId}:${nextEvent.eventId}:${candidateProgress.choiceHistory.size + 1}",
                )
            }

            if (candidateProgress.stage == LifeStage.ADULT) {
                return candidateProgress.copy(screenState = GameScreenState.ENDING)
            }
            candidateProgress = moveNextStage(candidateProgress)
        }
    }

    fun resolveEndingDecision(
        progress: GameProgress,
        endings: List<Ending>,
    ): EndingDecision {
        val matching = endings
            .filterNot { it.isDefault }
            .filter { it.conditions.stats.matches(progress.stats) }
            .filter { progress.flags.containsAll(it.conditions.requiredFlags) }
            .filterNot { ending -> progress.flags.any(ending.conditions.blockedFlags::contains) }
            .sortedWith(
                compareByDescending<Ending> { it.priority }
                    .thenBy { it.endingId },
            )
        if (matching.isNotEmpty()) {
            return EndingDecision(
                selected = matching.first(),
                eligibleEndingIds = matching.map { it.endingId },
                reason = EndingSelectionReason.HIGHEST_PRIORITY,
            )
        }

        val defaultEnding = endings
            .filter { it.isDefault }
            .minByOrNull { it.endingId }
            ?: error("At least one default ending is required")

        return EndingDecision(
            selected = defaultEnding,
            eligibleEndingIds = emptyList(),
            reason = EndingSelectionReason.DEFAULT_FALLBACK,
        )
    }

    fun resolveEnding(progress: GameProgress, endings: List<Ending>): Ending =
        resolveEndingDecision(progress, endings).selected

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
        const val EVENTS_PER_STAGE = 5
        const val DEFAULT_SPECIAL_EVENT_CHANCE_PERCENT = 10
    }
}
