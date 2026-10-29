package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.AwardedTitle
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
import com.example.lifegame.domain.model.LifeTitle
import com.example.lifegame.domain.model.LifeFacts
import com.example.lifegame.domain.model.LifeFactRule
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Outcome
import com.example.lifegame.domain.model.OutcomeLikelihood
import com.example.lifegame.domain.model.OutcomeResolution
import com.example.lifegame.domain.model.PendingDecision
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.StatKey
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
    private val skipInfantFocus: Boolean = false,
    private val guaranteeYearlyIncident: Boolean = false,
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
            // 제거 후 추가한다. 같은 플래그가 양쪽에 있으면 addFlags가 최종 승리한다.
            flags = (progress.flags - choice.removeFlags) + choice.addFlags,
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

    /** v1 저장본처럼 플래그만 있는 상태를 v2 인생 사실 상태로 안전하게 승격한다. */
    fun synchronizeLifeFacts(
        state: PeriodRunState,
        lifeFactRules: List<LifeFactRule>,
    ): PeriodRunState = state.copy(
        lifeFacts = state.lifeFacts
            .applyRules(state.flags, lifeFactRules)
            .copy(wealthBand = wealthBand(state.stats.wealth)),
    )

    internal fun requiresFocusSelection(state: PeriodRunState): Boolean =
        state.selectedFocusId == null &&
            !(skipInfantFocus && state.period in setOf(LifePeriod.INFANT, LifePeriod.OLD_AGE))

    /** 초점은 새 시기의 장 표지에서 한 번만 고를 수 있다. */
    fun selectFocus(
        state: PeriodRunState,
        focus: Focus,
    ): PeriodRunState {
        require(!state.isEnding) { "ending state cannot select a focus" }
        require(state.pendingDecisionId == null) { "pending decision must be resolved first" }
        require(state.selectedFocusId == null) { "focus cannot change during a period" }
        require(focus.period == state.period) { "focus must belong to the current period" }
        return state.copy(
            selectedFocusId = focus.focusId,
            dominantFocusStat = focus.statEffects.dominantPositiveStat() ?: state.dominantFocusStat,
        )
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
        incidentChancePercent: Int = if (guaranteeYearlyIncident) 100 else DEFAULT_INCIDENT_CHANCE_PERCENT,
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

        // Restore a pending decision before any yearly work, including focus-free periods.
        if (skipInfantFocus && (state.period == LifePeriod.INFANT || state.period == LifePeriod.OLD_AGE) && state.selectedFocusId == null) {
            return advanceInfantYear(state, orderedPeriods, focuses, decisions, bigIncidentChancePercent, incidentChancePercent)
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
        val focusStats = state.stats.apply(focus.statEffects, minHealth = healthFloor(state.period))
        val stateAfterFocus = state.copy(
            stats = focusStats,
            lifeFacts = state.lifeFacts.copy(wealthBand = wealthBand(focusStats.wealth)),
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

        val stateAfterYear = stateAfterFocus.copy(
            stats = stateAfterFocus.stats.apply(
                StatDelta(health = annualHealthDelta(state.period)),
                minHealth = healthFloor(state.period),
            ),
        )
        healthDeathStep(stateAfterYear)?.let { return it }
        val nextState = movePeriodCalendar(stateAfterYear, orderedPeriods)
        val needsFocus = !nextState.isEnding && requiresFocusSelection(nextState)
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
        lifeFactRules: List<LifeFactRule> = emptyList(),
    ): OutcomeResolution {
        val orderedPeriods = validatePeriods(periods)
        require(!state.isEnding) { "ending state cannot resolve a decision" }
        require(state.pendingDecisionId == event.eventId) { "event is not the pending decision" }
        val choice = requireNotNull(event.choices.firstOrNull { it.choiceId == choiceId }) {
            "choice is not available in the pending decision"
        }
        require(choice.statConditions.matches(state.stats)) { "choice stat requirements are not met" }
        require(choice.lifeFactConditions.matches(state.lifeFacts)) { "choice life fact requirements are not met" }
        require(state.flags.containsAll(choice.requiredFlags)) { "choice requirements are not met" }
        require(state.flags.none(choice.blockedFlags::contains)) { "choice is blocked by current flags" }

        val outcome = pickOutcome(state, event, choice.choiceId, choice.outcomes)
        val combinedDelta = resolveOutcomeStatDelta(state.period, outcome)
        val afterStats = state.stats.apply(combinedDelta, minHealth = healthFloor(state.period))
        val healthDepleted = afterStats.health <= 0
        // Reacquired facts must replay after older facts when restoring a saved run.
        val nextFlags = (state.flags - outcome.removeFlags - outcome.addFlags) + outcome.addFlags + completedEventFlag(event.eventId)
        val nextFacts = state.lifeFacts
            .removeRules(outcome.removeFlags, lifeFactRules)
            .applyRules(outcome.addFlags + completedEventFlag(event.eventId), lifeFactRules)
            .copy(wealthBand = wealthBand(afterStats.wealth))
        val resolved = state.copy(
            stats = afterStats,
            flags = nextFlags,
            lifeFacts = if (outcome.death || healthDepleted) nextFacts.copy(
                deathPeriod = state.period,
                deathCause = outcome.deathCause ?: if (healthDepleted) "건강 악화" else event.title,
            ) else nextFacts,
            recentChoiceIds = (state.recentChoiceIds + "${event.eventId}:$choiceId").takeLast(5),
            pendingDecisionId = null,
            moments = state.moments + LifeMoment(
                period = state.period,
                yearInPeriod = state.yearInPeriod,
                text = outcome.resultText,
                momentImageId = event.momentImageId,
            ),
            isEnding = outcome.death || healthDepleted,
            isDead = outcome.death || healthDepleted,
            endingPeriod = if (outcome.death || healthDepleted) state.period else state.endingPeriod,
        )
        if (outcome.death || healthDepleted) {
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
        val needsFocus = !nextState.isEnding && requiresFocusSelection(nextState)
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

    /** 한 선택 결과에 적용되는 건강 정책: 명시 statDelta + legacy lifeDelta + 연간 감소를 정확히 한 번 합산한다. */
    fun resolveOutcomeStatDelta(period: LifePeriod, outcome: Outcome): StatDelta = outcome.statDelta.copy(
        health = outcome.statDelta.health + outcome.lifeDelta + annualHealthDelta(period),
    )

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

    private fun advanceInfantYear(
        state: PeriodRunState,
        periods: List<PeriodDefinition>,
        focuses: List<Focus>,
        decisions: List<DecisionEvent>,
        bigIncidentChancePercent: Int,
        incidentChancePercent: Int,
    ): TurnStep {
        val record = if (state.period == LifePeriod.INFANT) {
            "영아기 ${state.yearInPeriod}번째 해, 세상을 처음 배웠다."
        } else {
            "노년기 ${state.yearInPeriod}번째 해, 지나온 선택을 돌아보았다."
        }
        val afterRecord = state.copy(
            stats = state.stats.apply(
                StatDelta(health = annualHealthDelta(state.period)),
                minHealth = healthFloor(state.period),
            ),
            moments = state.moments + LifeMoment(state.period, state.yearInPeriod, record),
        )
        healthDeathStep(afterRecord)?.let { return it }
        val decision = pickPeriodDecision(afterRecord, decisions, bigIncidentChancePercent, incidentChancePercent)
        if (decision != null) {
            return decisionStep(afterRecord.copy(pendingDecisionId = decision.eventId), decision)
        }
        val next = movePeriodCalendar(afterRecord, periods)
        val chapter = next.period != LifePeriod.INFANT && next.period != LifePeriod.OLD_AGE && next.selectedFocusId == null
        return TurnStep(
            state = next,
            phase = when {
                next.isEnding -> TurnPhase.ENDING
                chapter -> TurnPhase.CHAPTER
                else -> TurnPhase.AUTO_PROGRESS
            },
            recordText = record,
            canAutoAdvance = !next.isEnding && !chapter,
            needsFocusSelection = chapter,
        )
    }

    private fun canShowPeriodDecision(state: PeriodRunState, event: DecisionEvent): Boolean =
        state.period in event.periods &&
            completedEventFlag(event.eventId) !in state.flags &&
            event.statConditions.matches(state.stats) &&
            event.lifeFactConditions.matches(state.lifeFacts) &&
            state.flags.containsAll(event.requiredFlags) &&
            state.flags.none(event.blockedFlags::contains) &&
            event.choices.any { choice ->
                choice.statConditions.matches(state.stats) &&
                choice.lifeFactConditions.matches(state.lifeFacts) &&
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
        // 잠긴 선택지도 화면에 남겨 요구 스탯을 안내한다. 실제 선택 시에는
        // resolvePeriodDecision에서 다시 검사하므로 UI 우회가 불가능하다.
        val visibleChoices = event.choices.map { choice ->
            val statOk = choice.statConditions.matches(state.stats)
            val flagsOk = state.flags.containsAll(choice.requiredFlags) &&
                state.flags.none(choice.blockedFlags::contains)
            val factsOk = choice.lifeFactConditions.matches(state.lifeFacts)
            buildChoicePreview(
                choice = choice,
                stats = state.stats,
                available = statOk && flagsOk && factsOk,
                lockReason = if (!statOk) "${choice.statConditions.description()} 필요" else
                    if (!flagsOk) "이전 선택과 조건이 맞지 않아요" else
                        if (!factsOk) "현재 인생 경로와 조건이 맞지 않아요" else null,
            )
        }.sortedByDescending { it.available }
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
        available: Boolean = true,
        lockReason: String? = null,
    ): DecisionChoicePreview {
        val primary = choice.outcomes.firstOrNull { it.primary && !it.hidden }
            ?: choice.outcomes.firstOrNull { !it.hidden }
            ?: return DecisionChoicePreview(
                choiceId = choice.choiceId,
                label = choice.label,
                statConditionText = choice.statConditions.description().takeIf { it.isNotBlank() },
                available = available,
                lockReason = lockReason,
            )
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
            statConditionText = choice.statConditions.description().takeIf { it.isNotBlank() },
            available = available,
            lockReason = lockReason,
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
        val weightedEvents = sorted.map { event ->
            event to eventSelectionWeight(event, state)
        }
        var roll = deterministicIndex(
            "${state.runSeed}:${state.period}:${state.yearInPeriod}:$salt",
            weightedEvents.sumOf { it.second },
        ) + 1
        weightedEvents.forEach { (event, weight) ->
            roll -= weight
            if (roll <= 0) return event
        }
        return sorted.last()
    }

    /** 사건의 기본 weight에 현재 능력치와 사건 결과의 chanceWeights를 함께 반영한다. */
    private fun eventSelectionWeight(event: DecisionEvent, state: PeriodRunState): Int {
        val stats = state.stats
        val outcomes = event.choices.flatMap { it.outcomes }.filterNot { it.hidden }
        val baseChance = outcomes.sumOf { it.chance }
        if (baseChance <= 0) return event.weight
        val effectiveChance = outcomes.sumOf { effectiveOutcomeChance(it, stats) }
        val pathBonus = if (event.pathStat != null && event.pathStat == state.dominantFocusStat) 4 else 1
        return (event.weight * effectiveChance / baseChance * pathBonus).coerceAtLeast(1)
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
            ?: return if (state.period == LifePeriod.OLD_AGE) {
                state.copy(
                    isEnding = true,
                    isDead = true,
                    endingPeriod = state.period,
                    lifeFacts = state.lifeFacts.copy(
                        deathPeriod = state.period,
                        deathCause = "자연사",
                    ),
                    pendingDecisionId = null,
                    moments = state.moments + LifeMoment(
                        period = state.period,
                        yearInPeriod = state.yearInPeriod,
                        text = "노년기를 마치고 자연스럽게 생을 마쳤다.",
                    ),
                )
            } else {
                state.copy(isEnding = true, endingPeriod = state.period, pendingDecisionId = null)
            }
        return state.copy(
            period = next.period,
            yearInPeriod = 1,
            selectedFocusId = null,
            pendingDecisionId = null,
        )
    }

    /** 건강이 낮을 때만 발생하는 연간 돌발 사망. 보통 건강 수치에서는 확률을 0으로 둔다. */
    private fun healthDeathStep(state: PeriodRunState): TurnStep? {
        if (state.period !in setOf(LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE, LifePeriod.OLD_AGE)) return null
        val chance = when {
            state.stats.health <= 15 -> 8
            state.stats.health <= 25 -> 4
            state.stats.health <= 35 -> 1
            else -> 0
        }
        if (chance == 0 || deterministicPercent(state, "health-death") > chance) return null
        val text = "건강이 급격히 나빠져 예기치 않게 생을 마쳤다."
        val ended = state.copy(
            isEnding = true,
            isDead = true,
            endingPeriod = state.period,
            lifeFacts = state.lifeFacts.copy(
                deathPeriod = state.period,
                deathCause = "건강 악화",
            ),
            pendingDecisionId = null,
            moments = state.moments + LifeMoment(state.period, state.yearInPeriod, text),
        )
        return TurnStep(
            state = ended,
            phase = TurnPhase.ENDING,
            recordText = text,
            transitionCue = TransitionCue.DISTORT,
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
    ): EndingDecision = resolveEndingDecision(progress.stats, progress.flags, endings)

    /** v4 시기 진행 상태를 기존 결말 우선순위 규칙에 그대로 연결한다. */
    fun resolveEndingDecision(
        state: PeriodRunState,
        endings: List<Ending>,
    ): EndingDecision = resolveEndingDecision(state.stats, state.flags, endings, state.lifeFacts)

    private fun resolveEndingDecision(
        stats: Stats,
        flags: Set<String>,
        endings: List<Ending>,
        lifeFacts: LifeFacts = LifeFacts(),
    ): EndingDecision {
        val matching = endings
            .filterNot { it.isDefault }
            .filter { it.conditions.stats.matches(stats) }
            .filter { flags.containsAll(it.conditions.requiredFlags) }
            .filterNot { ending -> flags.any(ending.conditions.blockedFlags::contains) }
            .filter { it.conditions.lifeFacts.matches(lifeFacts) }
            .sortedWith(
                compareByDescending<Ending> { it.tier.rank }
                    .thenByDescending { it.priority }
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

    fun resolveEnding(state: PeriodRunState, endings: List<Ending>): Ending {
        val selected = resolveEndingDecision(state, endings).selected
        return selected.copy(summary = buildLifeSummary(state, selected.summary))
    }

    /** 우선순위 내림차순, 동률 titleId 오름차순. 조건 불일치 시 유일한 기본 칭호를 반환한다. */
    fun resolveTitle(state: PeriodRunState, titles: List<LifeTitle>): AwardedTitle {
        require(titles.isNotEmpty()) { "At least one title is required" }
        val defaults = titles.filter { it.isDefault }
        require(defaults.size == 1) { "Exactly one default title is required" }
        val selected = titles.asSequence()
            .filterNot { it.isDefault }
            .filter { it.conditions.stats.matches(state.stats) }
            .filter { state.flags.containsAll(it.conditions.requiredFlags) }
            .filterNot { title -> state.flags.any(title.conditions.blockedFlags::contains) }
            .filter { it.conditions.lifeFacts.matches(state.lifeFacts) }
            .sortedWith(compareByDescending<LifeTitle> { it.priority }.thenBy { it.titleId })
            .firstOrNull()
            ?: defaults.single()
        return AwardedTitle(selected.titleId, selected.text, selected.description)
    }

    /** 프론트가 별도 판정 없이 그대로 표시할 수 있는 결말용 인생 요약. */
    fun buildLifeSummary(state: PeriodRunState, baseSummary: String = ""): String {
        val facts = state.lifeFacts
        val details = listOfNotNull(
            facts.educationLevel?.let { "교육 $it" },
            facts.fieldOfStudy?.let { "전공 $it" },
            facts.careerTrack?.let { career ->
                listOfNotNull(career, facts.careerLevel).joinToString(" · ").let { "직업 $it" }
            },
            facts.maritalStatus?.let { "관계 $it" },
            "자녀 ${facts.childrenCount}명".takeIf { facts.childrenCount > 0 },
            facts.housingType?.let { "주거 $it" },
            "경제력 ${facts.wealthBand}",
            "업적 ${facts.achievementFlags.size}개".takeIf { facts.achievementFlags.isNotEmpty() },
            facts.deathPeriod?.let { period ->
                "${period.koreanName()}에 ${facts.deathCause ?: "알 수 없는 원인"}로 생을 마침"
            },
        )
        return (listOf(baseSummary.trim().trimEnd('.')) + details)
            .filter { it.isNotBlank() }
            .joinToString(". ") + "."
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

    private fun Stats.apply(delta: StatDelta, minHealth: Int = 0): Stats = copy(
        health = addAndClamp(health, delta.health, minHealth, MAX_STAT),
        fitness = addAndClamp(fitness, delta.fitness),
        intelligence = addAndClamp(intelligence, delta.intelligence),
        social = addAndClamp(social, delta.social),
        wealth = addAndClamp(wealth, delta.wealth),
        happiness = addAndClamp(happiness, delta.happiness),
        luck = addAndClamp(luck, delta.luck),
    )

    private fun healthFloor(period: LifePeriod): Int = when (period) {
        LifePeriod.INFANT, LifePeriod.CHILD -> 1
        else -> MIN_STAT
    }

    private fun annualHealthDelta(period: LifePeriod): Int = when (period) {
        LifePeriod.INFANT, LifePeriod.CHILD, LifePeriod.TEEN -> 0
        LifePeriod.YOUNG_ADULT, LifePeriod.MIDDLE, LifePeriod.OLD_AGE -> -LIFE_DECAY_PER_YEAR
    }

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

    private fun addAndClamp(current: Int, delta: Int, min: Int, max: Int): Int =
        (current.toLong() + delta.toLong())
            .coerceIn(min.toLong(), max.toLong())
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

    private fun StatDelta.dominantPositiveStat(): StatKey? = listOf(
        StatKey.HEALTH to health,
        StatKey.FITNESS to fitness,
        StatKey.INTELLIGENCE to intelligence,
        StatKey.SOCIAL to social,
        StatKey.WEALTH to wealth,
        StatKey.HAPPINESS to happiness,
        StatKey.LUCK to luck,
    ).filter { it.second > 0 }.maxByOrNull { it.second }?.first

    private fun LifeFacts.applyRules(flags: Set<String>, rules: List<LifeFactRule>): LifeFacts {
        var current = this
        // flags는 LinkedHashSet으로 누적되므로 실제 선택 연대기를 유지한다. 같은 사실은 나중 선택이 이긴다.
        flags.flatMap { flag -> rules.filter { it.flag == flag } }.forEach { rule ->
            current = when (rule.fact) {
                "educationLevel" -> current.copy(educationLevel = rule.value)
                "fieldOfStudy" -> current.copy(fieldOfStudy = rule.value)
                "careerTrack" -> current.copy(careerTrack = rule.value)
                "careerLevel" -> current.copy(careerLevel = rule.value)
                "employmentStatus" -> current.copy(employmentStatus = rule.value)
                "maritalStatus" -> current.copy(maritalStatus = rule.value)
                "childrenAtLeast" -> current.copy(
                    childrenCount = maxOf(current.childrenCount, rule.value.toIntOrNull() ?: 1),
                )
                "housingType" -> current.copy(housingType = rule.value)
                "achievement" -> current.copy(achievementFlags = current.achievementFlags + rule.flag)
                else -> current
            }
        }
        return current
    }

    private fun LifeFacts.removeRules(flags: Set<String>, rules: List<LifeFactRule>): LifeFacts {
        var current = this
        rules.filter { it.flag in flags }.forEach { rule ->
            current = when (rule.fact) {
                "educationLevel" -> if (current.educationLevel == rule.value) current.copy(educationLevel = null) else current
                "fieldOfStudy" -> if (current.fieldOfStudy == rule.value) current.copy(fieldOfStudy = null) else current
                "careerTrack" -> if (current.careerTrack == rule.value) current.copy(careerTrack = null) else current
                "careerLevel" -> if (current.careerLevel == rule.value) current.copy(careerLevel = null) else current
                "employmentStatus" -> if (current.employmentStatus == rule.value) current.copy(employmentStatus = null) else current
                "maritalStatus" -> if (current.maritalStatus == rule.value) current.copy(maritalStatus = null) else current
                "housingType" -> if (current.housingType == rule.value) current.copy(housingType = null) else current
                // 자녀 수와 과거 업적은 현재 상태 플래그 제거만으로 되돌리지 않는다.
                else -> current
            }
        }
        return current
    }

    private fun wealthBand(wealth: Int): String = when {
        wealth < 25 -> "LOW"
        wealth < 50 -> "LOWER_MIDDLE"
        wealth < 75 -> "MIDDLE"
        wealth < 90 -> "HIGH"
        else -> "VERY_HIGH"
    }

    private fun LifePeriod.koreanName(): String = when (this) {
        LifePeriod.INFANT -> "영아기"
        LifePeriod.CHILD -> "유년기"
        LifePeriod.TEEN -> "청소년기"
        LifePeriod.YOUNG_ADULT -> "청년기"
        LifePeriod.MIDDLE -> "중년기"
        LifePeriod.OLD_AGE -> "노년기"
    }

    private companion object {
        const val MIN_STAT = 0
        const val MAX_STAT = 100
        const val EVENTS_PER_STAGE = 5
        const val DEFAULT_SPECIAL_EVENT_CHANCE_PERCENT = 10
        const val DEFAULT_BIG_INCIDENT_CHANCE_PERCENT = 5
        const val DEFAULT_INCIDENT_CHANCE_PERCENT = 20
        const val LIFE_DECAY_PER_YEAR = 5
        const val COMPLETED_EVENT_FLAG_PREFIX = "v4:event:"
        const val FNV_OFFSET_BASIS = -3750763034362895579L
        const val FNV_PRIME = 1099511628211L
    }
}
