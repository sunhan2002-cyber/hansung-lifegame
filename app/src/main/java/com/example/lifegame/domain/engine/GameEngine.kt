package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.ChanceWeights
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.DecisionChoicePreview
import com.example.lifegame.domain.model.DecisionOption
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeMoment
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.OutcomeLikelihood
import com.example.lifegame.domain.model.OutcomeResolution
import com.example.lifegame.domain.model.PendingDecision
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TransitionCue
import com.example.lifegame.domain.model.TurnPhase
import com.example.lifegame.domain.model.TurnStep

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
    private val minNormalEventsBeforeSpecial: Int = 2,
) {
    init {
        require(maxSpecialEventsPerRun >= 0) {
            "maxSpecialEventsPerRun must be zero or greater"
        }
        require(minNormalEventsBeforeSpecial >= 0) {
            "minNormalEventsBeforeSpecial must be zero or greater"
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
        if (completedNormalEventCount(progress, events) < minNormalEventsBeforeSpecial) {
            return null
        }
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

    /** v4 개발용 24턴 시기 정의. JSON 로더가 준비되면 같은 모델 목록으로 교체할 수 있다. */
    fun defaultPeriodDefinitions(): List<PeriodDefinition> = listOf(
        PeriodDefinition(LifePeriod.INFANT, order = 0, years = 4),
        PeriodDefinition(LifePeriod.CHILD, order = 1, years = 5),
        PeriodDefinition(LifePeriod.TEEN, order = 2, years = 5),
        PeriodDefinition(LifePeriod.YOUNG_ADULT, order = 3, years = 5),
        PeriodDefinition(LifePeriod.MIDDLE, order = 4, years = 5),
    )

    fun createPeriodRun(
        runId: String,
        runSeed: Long,
        periods: List<PeriodDefinition> = defaultPeriodDefinitions(),
    ): PeriodRunState {
        require(runId.isNotBlank()) { "runId must not be blank" }
        val ordered = validatePeriods(periods)
        return PeriodRunState(
            runId = runId,
            runSeed = runSeed,
            period = ordered.first().period,
        )
    }

    /** 초점은 새 시기의 장 표지에서 한 번만 고를 수 있다. */
    fun selectFocus(
        state: PeriodRunState,
        focus: Focus,
    ): PeriodRunState {
        require(!state.isEnding) { "ending state cannot select a focus" }
        require(state.pendingDecisionId == null) { "pending decision must be resolved first" }
        require(state.selectedFocusId == null) { "focus cannot change during a period" }
        require(focus.period == state.period) { "focus must belong to the current period" }
        return state.copy(selectedFocusId = focus.focusId)
    }

    /**
     * 한 해를 처리한다. 초점 효과를 먼저 반영하고 마일스톤, 큰 돌발, 일반 돌발 순서로 확인한다.
     * 결정이 생기면 [canAutoAdvance]가 false인 step을 반환해 자동 진행을 멈춘다.
     */
    fun advancePeriodYear(
        state: PeriodRunState,
        periods: List<PeriodDefinition>,
        focuses: List<Focus>,
        decisions: List<DecisionEvent>,
        bigIncidentChancePercent: Int = DEFAULT_BIG_INCIDENT_CHANCE_PERCENT,
        incidentChancePercent: Int = DEFAULT_INCIDENT_CHANCE_PERCENT,
    ): TurnStep {
        val orderedPeriods = validatePeriods(periods)
        if (state.isEnding) {
            return TurnStep(state = state, phase = TurnPhase.ENDING)
        }

        state.pendingDecisionId?.let { eventId ->
            val event = requireNotNull(decisions.firstOrNull { it.eventId == eventId }) {
                "pending decision is not available in current content"
            }
            return decisionStep(state, event)
        }

        val focusId = state.selectedFocusId
            ?: return TurnStep(
                state = state,
                phase = TurnPhase.CHAPTER,
                transitionCue = TransitionCue.CHAPTER,
                needsFocusSelection = true,
            )
        val focus = requireNotNull(focuses.firstOrNull { it.focusId == focusId }) {
            "selected focus is not available in current content"
        }
        require(focus.period == state.period) { "selected focus belongs to another period" }

        val recordText = focus.yearlyLogTemplates.takeIf { it.isNotEmpty() }
            ?.let { templates ->
                templates[deterministicIndex(
                    "${state.runSeed}:${focus.focusId}:${state.yearInPeriod}:yearly-log",
                    templates.size,
                )]
            }
            ?: "${state.period.name} ${state.yearInPeriod}년차, ${focus.label}에 집중했다."
        val stateAfterFocus = state.copy(
            stats = state.stats.apply(focus.statEffects),
            moments = state.moments + LifeMoment(
                period = state.period,
                yearInPeriod = state.yearInPeriod,
                text = recordText,
                momentImageId = focus.momentImageId,
            ),
        )
        val decision = pickPeriodDecision(
            state = stateAfterFocus,
            decisions = decisions,
            bigIncidentChancePercent = bigIncidentChancePercent,
            incidentChancePercent = incidentChancePercent,
        )
        if (decision != null) {
            return decisionStep(
                state = stateAfterFocus.copy(pendingDecisionId = decision.eventId),
                event = decision,
                recordText = recordText,
                sceneImageId = focus.sceneImageId,
                momentImageId = focus.momentImageId,
            )
        }

        val nextState = movePeriodCalendar(stateAfterFocus, orderedPeriods)
        val needsFocus = !nextState.isEnding && nextState.selectedFocusId == null
        return TurnStep(
            state = nextState,
            phase = when {
                nextState.isEnding -> TurnPhase.ENDING
                needsFocus -> TurnPhase.CHAPTER
                else -> TurnPhase.AUTO_PROGRESS
            },
            recordText = recordText,
            transitionCue = if (needsFocus) TransitionCue.CHAPTER else null,
            sceneImageId = focus.sceneImageId,
            momentImageId = focus.momentImageId,
            canAutoAdvance = !nextState.isEnding && !needsFocus,
            needsFocusSelection = needsFocus,
        )
    }

    fun resolvePeriodDecision(
        state: PeriodRunState,
        event: DecisionEvent,
        choiceId: String,
        periods: List<PeriodDefinition>,
    ): OutcomeResolution {
        val orderedPeriods = validatePeriods(periods)
        require(!state.isEnding) { "ending state cannot resolve a decision" }
        require(state.pendingDecisionId == event.eventId) { "event is not the pending decision" }
        val choice = requireNotNull(event.choices.firstOrNull { it.choiceId == choiceId }) {
            "choice is not available in the pending decision"
        }
        require(state.flags.containsAll(choice.requiredFlags)) { "choice requirements are not met" }
        require(state.flags.none(choice.blockedFlags::contains)) { "choice is blocked by current flags" }

        val outcome = pickOutcome(state, event, choice.choiceId, choice.outcomes)
        val resolved = state.copy(
            stats = state.stats.apply(outcome.statDelta),
            flags = state.flags + outcome.addFlags + completedEventFlag(event.eventId),
            pendingDecisionId = null,
            moments = state.moments + LifeMoment(
                period = state.period,
                yearInPeriod = state.yearInPeriod,
                text = outcome.resultText,
                momentImageId = event.momentImageId,
            ),
            isEnding = outcome.death,
        )
        if (outcome.death) {
            return OutcomeResolution(
                outcome = outcome,
                step = TurnStep(
                    state = resolved,
                    phase = TurnPhase.ENDING,
                    recordText = outcome.resultText,
                    transitionCue = TransitionCue.DISTORT,
                    momentImageId = event.momentImageId,
                ),
            )
        }

        val nextState = movePeriodCalendar(resolved, orderedPeriods)
        val needsFocus = !nextState.isEnding && nextState.selectedFocusId == null
        return OutcomeResolution(
            outcome = outcome,
            step = TurnStep(
                state = nextState,
                phase = when {
                    nextState.isEnding -> TurnPhase.ENDING
                    needsFocus -> TurnPhase.CHAPTER
                    else -> TurnPhase.AUTO_PROGRESS
                },
                recordText = outcome.resultText,
                transitionCue = if (needsFocus) TransitionCue.CHAPTER else null,
                momentImageId = event.momentImageId,
                canAutoAdvance = !nextState.isEnding && !needsFocus,
                needsFocusSelection = needsFocus,
            ),
        )
    }

    private fun pickPeriodDecision(
        state: PeriodRunState,
        decisions: List<DecisionEvent>,
        bigIncidentChancePercent: Int,
        incidentChancePercent: Int,
    ): DecisionEvent? {
        val candidates = decisions.filter { canShowPeriodDecision(state, it) }
        val milestone = candidates
            .filter {
                it.kind == DecisionKind.MILESTONE &&
                    (it.yearInPeriod == null || it.yearInPeriod == state.yearInPeriod)
            }
            .sortedWith(
                compareBy<DecisionEvent> { it.yearInPeriod == null }
                    .thenBy { it.eventId },
            )
            .firstOrNull()
        if (milestone != null) return milestone

        val bigIncidents = candidates.filter { it.kind == DecisionKind.BIG_INCIDENT }
        if (deterministicPercent(state, "big-incident") <= bigIncidentChancePercent.coerceIn(0, 100)) {
            pickWeightedEvent(state, bigIncidents, "big-incident-pick")?.let { return it }
        }

        val incidents = candidates.filter { it.kind == DecisionKind.INCIDENT }
        if (deterministicPercent(state, "incident") <= incidentChancePercent.coerceIn(0, 100)) {
            return pickWeightedEvent(state, incidents, "incident-pick")
        }
        return null
    }

    private fun canShowPeriodDecision(state: PeriodRunState, event: DecisionEvent): Boolean =
        state.period in event.periods &&
            completedEventFlag(event.eventId) !in state.flags &&
            event.statConditions.matches(state.stats) &&
            state.flags.containsAll(event.requiredFlags) &&
            state.flags.none(event.blockedFlags::contains) &&
            event.choices.any { choice ->
                state.flags.containsAll(choice.requiredFlags) &&
                    state.flags.none(choice.blockedFlags::contains)
            }

    private fun decisionStep(
        state: PeriodRunState,
        event: DecisionEvent,
        recordText: String? = null,
        sceneImageId: String? = null,
        momentImageId: String? = null,
    ): TurnStep {
        val visibleChoices = event.choices
            .filter { state.flags.containsAll(it.requiredFlags) }
            .filter { state.flags.none(it.blockedFlags::contains) }
            .map { choice -> buildChoicePreview(choice, state.stats) }
        return TurnStep(
            state = state,
            phase = TurnPhase.DECISION,
            recordText = recordText,
            transitionCue = if (event.kind == DecisionKind.BIG_INCIDENT) {
                TransitionCue.DISTORT
            } else {
                TransitionCue.NOISE
            },
            pendingDecision = PendingDecision(
                eventId = event.eventId,
                kind = event.kind,
                title = event.title,
                text = event.text,
                choices = visibleChoices,
                povImageId = event.povImageId,
            ),
            sceneImageId = sceneImageId,
            momentImageId = momentImageId,
            canAutoAdvance = false,
        )
    }

    /**
     * 결과 확률과 능력치 가중치 계산은 실제 결과 추첨과 같은 함수를 사용한다.
     * hidden 결과는 분모에는 포함하지만 대표 결과나 핵심 지표로 노출하지 않는다.
     */
    private fun buildChoicePreview(
        choice: DecisionOption,
        stats: Stats,
    ): DecisionChoicePreview {
        val primary = choice.outcomes.firstOrNull { it.primary && !it.hidden }
            ?: choice.outcomes.firstOrNull { !it.hidden }
            ?: return DecisionChoicePreview(choice.choiceId, choice.label)
        val weighted = choice.outcomes.map { effectiveOutcomeChance(it, stats) }
        val usable = if (weighted.sum() > 0) weighted else choice.outcomes.map { it.chance }
        val total = usable.sum()
        val primaryIndex = choice.outcomes.indexOf(primary)
        val percent = if (total <= 0 || primaryIndex < 0) {
            0
        } else {
            (usable[primaryIndex] * 100 + total / 2) / total
        }
        val likelihood = if (choice.outcomes.size <= 1) null else when {
            percent < 35 -> OutcomeLikelihood.LOW
            percent < 60 -> OutcomeLikelihood.HALF
            percent < 80 -> OutcomeLikelihood.HIGH
            else -> OutcomeLikelihood.VERY_HIGH
        }
        val weightedStats = primary.chanceWeights.toPreviewStats()
        val keyStats = (weightedStats.ifEmpty { primary.statDelta.toPreviewStats() }).take(2)
        return DecisionChoicePreview(
            choiceId = choice.choiceId,
            label = choice.label,
            likelihood = likelihood,
            keyStats = keyStats,
        )
    }

    private fun ChanceWeights.toPreviewStats(): List<String> =
        listOfNotNull(
            "건강".takeIf { health != 0 },
            "운동능력".takeIf { fitness != 0 },
            "지력".takeIf { intelligence != 0 },
            "사회성".takeIf { social != 0 },
            "경제력".takeIf { wealth != 0 },
            "행복".takeIf { happiness != 0 },
            "운".takeIf { luck != 0 },
        )

    private fun StatDelta.toPreviewStats(): List<String> = listOfNotNull(
        "건강".takeIf { health != 0 },
        "운동능력".takeIf { fitness != 0 },
        "지력".takeIf { intelligence != 0 },
        "사회성".takeIf { social != 0 },
        "경제력".takeIf { wealth != 0 },
        "행복".takeIf { happiness != 0 },
        "운".takeIf { luck != 0 },
    )

    private fun pickOutcome(
        state: PeriodRunState,
        event: DecisionEvent,
        choiceId: String,
        outcomes: List<Outcome>,
    ): Outcome {
        val weighted = outcomes.map { outcome ->
            outcome to effectiveOutcomeChance(outcome, state.stats)
        }
        val usable = if (weighted.sumOf { it.second } > 0) {
            weighted
        } else {
            outcomes.map { it to it.chance }
        }
        val total = usable.sumOf { it.second }
        require(total > 0) { "decision outcomes must have a positive effective chance" }
        var roll = deterministicIndex(
            "${state.runSeed}:${event.eventId}:$choiceId",
            total,
        ) + 1
        usable.forEach { (outcome, weight) ->
            roll -= weight
            if (roll <= 0) return outcome
        }
        return usable.last().first
    }

    private fun effectiveOutcomeChance(outcome: Outcome, stats: Stats): Int {
        val weights = outcome.chanceWeights
        val adjustment =
            ((stats.health - Stats.DEFAULT_STAT) * weights.health) / 10 +
                ((stats.fitness - Stats.DEFAULT_STAT) * weights.fitness) / 10 +
                ((stats.intelligence - Stats.DEFAULT_STAT) * weights.intelligence) / 10 +
                ((stats.social - Stats.DEFAULT_STAT) * weights.social) / 10 +
                ((stats.wealth - Stats.DEFAULT_STAT) * weights.wealth) / 10 +
                ((stats.happiness - Stats.DEFAULT_STAT) * weights.happiness) / 10 +
                ((stats.luck - Stats.DEFAULT_STAT) * weights.luck) / 10
        return (outcome.chance + adjustment).coerceAtLeast(0)
    }

    private fun pickWeightedEvent(
        state: PeriodRunState,
        events: List<DecisionEvent>,
        salt: String,
    ): DecisionEvent? {
        if (events.isEmpty()) return null
        val sorted = events.sortedBy { it.eventId }
        var roll = deterministicIndex(
            "${state.runSeed}:${state.period}:${state.yearInPeriod}:$salt",
            sorted.sumOf { it.weight },
        ) + 1
        sorted.forEach { event ->
            roll -= event.weight
            if (roll <= 0) return event
        }
        return sorted.last()
    }

    private fun deterministicPercent(state: PeriodRunState, salt: String): Int =
        deterministicIndex(
            "${state.runSeed}:${state.period}:${state.yearInPeriod}:$salt",
            100,
        ) + 1

    private fun deterministicIndex(key: String, bound: Int): Int {
        require(bound > 0) { "bound must be greater than zero" }
        var hash = FNV_OFFSET_BASIS
        key.encodeToByteArray().forEach { byte ->
            hash = hash xor (byte.toLong() and 0xff)
            hash *= FNV_PRIME
        }
        return ((hash ushr 1) % bound.toLong()).toInt()
    }

    private fun movePeriodCalendar(
        state: PeriodRunState,
        orderedPeriods: List<PeriodDefinition>,
    ): PeriodRunState {
        val currentIndex = orderedPeriods.indexOfFirst { it.period == state.period }
        require(currentIndex >= 0) { "current period is missing from definitions" }
        val current = orderedPeriods[currentIndex]
        if (state.yearInPeriod < current.years) {
            return state.copy(yearInPeriod = state.yearInPeriod + 1)
        }
        val next = orderedPeriods.getOrNull(currentIndex + 1)
            ?: return state.copy(isEnding = true, pendingDecisionId = null)
        return state.copy(
            period = next.period,
            yearInPeriod = 1,
            selectedFocusId = null,
            pendingDecisionId = null,
        )
    }

    private fun validatePeriods(periods: List<PeriodDefinition>): List<PeriodDefinition> {
        require(periods.isNotEmpty()) { "at least one period definition is required" }
        require(periods.map { it.period }.distinct().size == periods.size) {
            "period definitions must not contain duplicate periods"
        }
        require(periods.map { it.order }.distinct().size == periods.size) {
            "period definitions must not contain duplicate order values"
        }
        return periods.sortedBy { it.order }
    }

    private fun completedEventFlag(eventId: String): String = "$COMPLETED_EVENT_FLAG_PREFIX$eventId"

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

    private fun completedNormalEventCount(
        progress: GameProgress,
        events: List<GameEvent>,
    ): Int {
        val normalEventIds = events
            .asSequence()
            .filter { it.type == EventType.NORMAL }
            .map { it.eventId }
            .toSet()
        return progress.completedEventIds.count(normalEventIds::contains)
    }

    private companion object {
        const val MIN_STAT = 0
        const val MAX_STAT = 100
        const val EVENTS_PER_STAGE = 5
        const val DEFAULT_SPECIAL_EVENT_CHANCE_PERCENT = 10
        const val DEFAULT_BIG_INCIDENT_CHANCE_PERCENT = 5
        const val DEFAULT_INCIDENT_CHANCE_PERCENT = 20
        const val COMPLETED_EVENT_FLAG_PREFIX = "v4:event:"
        const val FNV_OFFSET_BASIS = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
