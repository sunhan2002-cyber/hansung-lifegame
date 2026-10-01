package com.example.lifegame.domain.engine

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage

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
}
