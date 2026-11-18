package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.AutomaticEvent
import com.example.lifegame.domain.model.AutomaticEventStage
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.LifeFactRule
import com.example.lifegame.domain.model.OutcomeResolution
import com.example.lifegame.domain.model.PeriodDefinition
import com.example.lifegame.domain.model.PeriodRunState
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.domain.model.TurnStep
import com.example.lifegame.domain.model.TurnPhase

data class GameSessionState(
    val progress: GameProgress? = null,
    val currentEvent: GameEvent? = null,
    val selectedChoice: Choice? = null,
    val lastResult: ChoiceResult? = null,
    val ending: Ending? = null,
)

/**
 * JSON 콘텐츠, GameEngine, 화면 상태 사이를 연결하는 순수 Kotlin 세션이다.
 * Android UI와 분리되어 있어 선택 반영부터 다음 사건/결말까지 단위 테스트할 수 있다.
 */
class GameSession(
    private val events: List<GameEvent>,
    private val endings: List<Ending>,
    private val engine: GameEngine = GameEngine(),
    private val runIdFactory: () -> String = { "run-${System.currentTimeMillis()}" },
) {
    var state: GameSessionState = GameSessionState()
        private set

    fun start(): GameSessionState {
        require(events.isNotEmpty()) { "At least one event is required" }
        require(endings.any { it.isDefault }) { "At least one default ending is required" }

        val runId = runIdFactory()
        val initial = engine.createInitialProgress(runId)
        state = moveToNextEvent(initial)
        return state
    }

    fun restore(progress: GameProgress): GameSessionState {
        require(events.isNotEmpty()) { "At least one event is required" }
        require(endings.any { it.isDefault }) { "At least one default ending is required" }

        state = when (progress.screenState) {
            GameScreenState.EVENT -> GameSessionState(
                progress = progress,
                currentEvent = requireNotNull(progress.findCurrentEvent()) {
                    "Saved event is not available in the current content"
                },
            )

            GameScreenState.CHOICE_RESULT -> restoreChoiceResult(progress)
            GameScreenState.ENDING -> GameSessionState(
                progress = progress,
                ending = engine.checkEnding(progress, endings),
            )
        }
        return state
    }

    fun choose(choice: Choice): ChoiceResult? {
        val progress = state.progress ?: return null
        val event = state.currentEvent ?: return null
        if (state.lastResult != null || state.ending != null) return null

        val result = engine.applyChoice(progress, event, choice)
        state = state.copy(
            progress = result.progress,
            selectedChoice = choice,
            lastResult = result,
        )
        return result
    }

    /** 다음 사건이 있으면 true, 모든 사건이 끝나 결말로 이동하면 false를 반환한다. */
    fun proceed(): Boolean {
        val progress = state.progress ?: return false
        if (state.lastResult == null) return state.currentEvent != null
        state = moveToNextEvent(progress)
        return state.currentEvent != null
    }

    fun finish(): Ending {
        val progress = requireNotNull(state.progress) { "Game has not started" }
        val ending = engine.checkEnding(progress, endings)
        state = state.copy(
            progress = progress.copy(screenState = GameScreenState.ENDING),
            currentEvent = null,
            selectedChoice = null,
            lastResult = null,
            ending = ending,
        )
        return ending
    }

    private fun moveToNextEvent(progress: GameProgress): GameSessionState {
        val advanced = engine.advanceAfterResult(progress, events)
        if (advanced.screenState != GameScreenState.ENDING) {
            val event = events.firstOrNull { event ->
                advanced.currentEventOccurrenceId.startsWith(
                    "${advanced.runId}:${event.eventId}:",
                )
            }
            if (event != null) {
                return GameSessionState(progress = advanced, currentEvent = event)
            }
        }

        val ending = engine.checkEnding(advanced, endings)
        return GameSessionState(
            progress = advanced.copy(screenState = GameScreenState.ENDING),
            ending = ending,
        )
    }

    private fun restoreChoiceResult(progress: GameProgress): GameSessionState {
        val history = requireNotNull(progress.choiceHistory.lastOrNull()) {
            "Choice result save has no choice history"
        }
        val event = requireNotNull(events.firstOrNull { it.eventId == history.eventId }) {
            "Saved event is not available in the current content"
        }
        val choice = requireNotNull(event.choices.firstOrNull { it.choiceId == history.choiceId }) {
            "Saved choice is not available in the current content"
        }
        val result = ChoiceResult(
            progress = progress,
            beforeStats = history.beforeStats,
            afterStats = history.afterStats,
            appliedDelta = history.beforeStats.deltaTo(history.afterStats),
            resultText = history.resultText,
            wasApplied = true,
        )
        return GameSessionState(
            progress = progress,
            currentEvent = event,
            selectedChoice = choice,
            lastResult = result,
        )
    }

    private fun GameProgress.findCurrentEvent(): GameEvent? = events.firstOrNull { event ->
        currentEventOccurrenceId.startsWith("$runId:${event.eventId}:")
    }

    private fun Stats.deltaTo(after: Stats): StatDelta = StatDelta(
        health = after.health - health,
        fitness = after.fitness - fitness,
        intelligence = after.intelligence - intelligence,
        social = after.social - social,
        wealth = after.wealth - wealth,
        happiness = after.happiness - happiness,
        luck = after.luck - luck,
    )
}

/**
 * v4 시기 단위 자동 진행을 화면과 연결하기 위한 순수 Kotlin 세션.
 * 기존 사건 나열형 [GameSession]은 이전 화면 호환을 위해 그대로 유지한다.
 */
class PeriodGameSession(
    private val periods: List<PeriodDefinition>,
    private val focuses: List<Focus>,
    private val decisions: List<DecisionEvent>,
    private val endings: List<Ending> = emptyList(),
    private val lifeFactRules: List<LifeFactRule> = emptyList(),
    private val automaticEvents: List<AutomaticEvent> = emptyList(),
    private val engine: GameEngine = GameEngine(),
    private val runIdFactory: () -> String = { "run-${System.currentTimeMillis()}" },
    private val runSeedFactory: () -> Long = { System.currentTimeMillis() },
    /**
     * 콘텐츠 자동 사건(auto_events.sample.json)을 고르는 규칙. 있으면 한 해의 결정을 끝내고 다음 해로 넘어가기 전에
     * 한 단계를 자동 사건에 쓴다. 결정 · 결말 · 초점 선택 중에는 고르지 않는다(2026-11-16 프론트 마무리 과제 2).
     */
    private val automaticEventPicker: AutomaticEventPicker? = null,
) {
    var state: PeriodRunState? = null
        private set

    var lastStep: TurnStep? = null
        private set

    /** v4 진행이 끝났을 때 기존 결말 우선순위 규칙으로 선택된 단 하나의 결말. */
    var ending: Ending? = null
        private set

    fun start(): TurnStep {
        ending = null
        val initial = engine.createPeriodRun(
            runId = runIdFactory(),
            runSeed = runSeedFactory(),
            periods = periods,
        )
        val step = advanceWithAutomaticEvent(initial)
        return record(step)
    }

    fun restore(savedState: PeriodRunState): TurnStep {
        val migrated = engine.synchronizeLifeFacts(savedState, lifeFactRules)
        val step = advanceWithAutomaticEvent(migrated)
        return record(step)
    }

    /** 이미 한 해를 처리한 상태를 중복 처리하지 않고 화면에 복원한다. */
    fun restoreSaved(savedState: PeriodRunState): TurnStep {
        val migrated = engine.synchronizeLifeFacts(savedState, lifeFactRules)
        state = migrated
        val step = when {
            migrated.isEnding -> TurnStep(migrated, TurnPhase.ENDING)
            migrated.pendingDecisionId != null -> engine.advancePeriodYear(migrated, periods, focuses, decisions)
            engine.requiresFocusSelection(migrated) -> TurnStep(migrated, TurnPhase.CHAPTER, needsFocusSelection = true)
            else -> TurnStep(migrated, TurnPhase.AUTO_PROGRESS, canAutoAdvance = true)
        }
        return record(step)
    }

    fun selectFocus(focusId: String): TurnStep {
        val current = requireNotNull(state) { "Session has not started" }
        val focus = requireNotNull(focuses.firstOrNull { it.focusId == focusId }) {
            "Focus is not available in the current content"
        }
        val selected = engine.selectFocus(current, focus)
        state = selected
        return advanceWithAutomaticEvent(selected)
            .let(::record)
    }

    fun advance(): TurnStep {
        val current = requireNotNull(state) { "Session has not started" }
        val step = advanceWithAutomaticEvent(current)
        return record(step)
    }

    fun choose(choiceId: String): OutcomeResolution {
        val current = requireNotNull(state) { "Session has not started" }
        val eventId = requireNotNull(current.pendingDecisionId) { "There is no pending decision" }
        val event = requireNotNull(decisions.firstOrNull { it.eventId == eventId }) {
            "Pending decision is not available in the current content"
        }
        val resolution = engine.resolvePeriodDecision(
            current,
            event,
            choiceId,
            periods,
            lifeFactRules,
        )
        record(resolution.step)
        return resolution
    }

    private fun advanceWithAutomaticEvent(current: PeriodRunState): TurnStep {
        automaticEventBeforeYear(current)?.let { return it }
        val primary = engine.advancePeriodYear(current, periods, focuses, decisions)
        if (primary.phase != TurnPhase.AUTO_PROGRESS || primary.state.isEnding || automaticEvents.isEmpty()) {
            return primary
        }
        val currentPeriod = periods.firstOrNull { it.period == primary.state.period }
        val progressStep = primary.state.moments.count { it.period == primary.state.period } -
            primary.state.automaticEventRecords.count { it.period == primary.state.period }
        val afterEventId = primary.state.recentChoiceIds.lastOrNull()?.substringBefore(':')
        val hasAfterEventCandidate = automaticEvents.any {
            it.periods.contains(primary.state.period) &&
                it.triggerRule.stage == AutomaticEventStage.AFTER_EVENT &&
                it.triggerRule.afterEventId == afterEventId
        }
        val stage = when {
            hasAfterEventCandidate -> AutomaticEventStage.AFTER_EVENT
            current.period != primary.state.period || current.yearInPeriod == 1 -> AutomaticEventStage.PERIOD_START
            currentPeriod != null && current.yearInPeriod >= currentPeriod.years -> AutomaticEventStage.PERIOD_END
            currentPeriod != null && primary.state.yearInPeriod >= (currentPeriod.years + 1) / 2 -> AutomaticEventStage.PERIOD_MID
            else -> null
        }
        val triggerToken = buildString {
            append(primary.state.runId)
            append(":progress:")
            append(progressStep)
            append(':')
            append(primary.state.period.name)
            append(':')
            append(stage?.name ?: "STEP")
            afterEventId?.let { append(':').append(it) }
        }
        return engine.applyAutomaticEvent(
            primary.state,
            automaticEvents,
            triggerToken,
            lifeFactRules,
            progressStep,
            stage,
            afterEventId,
        ) ?: primary
    }

    /**
     * 다음 해를 처리하기 전 한 번, 이 진행 단계에 맞는 자동 사건이 있으면 그것만 적용한다.
     * 해 · 시기는 넘기지 않으므로 다음 [advance]에서 그해 결정이 그대로 나온다(자동 사건이 결정을 대신하지 않는다).
     */
    private fun automaticEventBeforeYear(current: PeriodRunState): TurnStep? {
        val picker = automaticEventPicker ?: return null
        if (current.isEnding || current.pendingDecisionId != null || engine.requiresFocusSelection(current)) return null
        val event = picker.pick(current) ?: return null
        return engine.applyAutomaticEvent(current, listOf(event), AutomaticEventPicker.stageToken(current), lifeFactRules)
    }

    private fun record(step: TurnStep): TurnStep {
        state = step.state
        lastStep = step
        ending = if (step.state.isEnding && endings.isNotEmpty()) {
            engine.resolveEnding(step.state, endings)
        } else {
            null
        }
        return step
    }
}
