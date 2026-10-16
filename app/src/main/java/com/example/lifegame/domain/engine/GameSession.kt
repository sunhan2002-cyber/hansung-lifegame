package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.DecisionEvent
import com.example.lifegame.domain.model.Focus
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
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
    private val engine: GameEngine = GameEngine(),
    private val runIdFactory: () -> String = { "run-${System.currentTimeMillis()}" },
    private val runSeedFactory: () -> Long = { System.currentTimeMillis() },
) {
    var state: PeriodRunState? = null
        private set

    var lastStep: TurnStep? = null
        private set

    fun start(): TurnStep {
        val initial = engine.createPeriodRun(
            runId = runIdFactory(),
            runSeed = runSeedFactory(),
            periods = periods,
        )
        val step = engine.advancePeriodYear(initial, periods, focuses, decisions)
        state = step.state
        lastStep = step
        return step
    }

    fun restore(savedState: PeriodRunState): TurnStep {
        val step = engine.advancePeriodYear(savedState, periods, focuses, decisions)
        state = step.state
        lastStep = step
        return step
    }

    /** 이미 한 해를 처리한 상태를 중복 처리하지 않고 화면에 복원한다. */
    fun restoreSaved(savedState: PeriodRunState): TurnStep {
        state = savedState
        val step = when {
            savedState.isEnding -> TurnStep(savedState, TurnPhase.ENDING)
            savedState.pendingDecisionId != null -> engine.advancePeriodYear(savedState, periods, focuses, decisions)
            savedState.selectedFocusId == null -> TurnStep(savedState, TurnPhase.CHAPTER, needsFocusSelection = true)
            else -> TurnStep(savedState, TurnPhase.AUTO_PROGRESS, canAutoAdvance = true)
        }
        lastStep = step
        return step
    }

    fun selectFocus(focusId: String): TurnStep {
        val current = requireNotNull(state) { "Session has not started" }
        val focus = requireNotNull(focuses.firstOrNull { it.focusId == focusId }) {
            "Focus is not available in the current content"
        }
        val selected = engine.selectFocus(current, focus)
        state = selected
        return engine.advancePeriodYear(selected, periods, focuses, decisions)
            .also {
                state = it.state
                lastStep = it
            }
    }

    fun advance(): TurnStep {
        val current = requireNotNull(state) { "Session has not started" }
        val step = engine.advancePeriodYear(current, periods, focuses, decisions)
        state = step.state
        lastStep = step
        return step
    }

    fun choose(choiceId: String): OutcomeResolution {
        val current = requireNotNull(state) { "Session has not started" }
        val eventId = requireNotNull(current.pendingDecisionId) { "There is no pending decision" }
        val event = requireNotNull(decisions.firstOrNull { it.eventId == eventId }) {
            "Pending decision is not available in the current content"
        }
        val resolution = engine.resolvePeriodDecision(current, event, choiceId, periods)
        state = resolution.step.state
        lastStep = resolution.step
        return resolution
    }
}
