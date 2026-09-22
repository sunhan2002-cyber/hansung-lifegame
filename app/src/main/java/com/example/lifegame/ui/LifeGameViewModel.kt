package com.example.lifegame.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * 화면과 게임 상태를 연결한다.
 * 지표 계산과 결말 판정은 GameEngine이 준비되기 전까지 임시로 여기서 처리한다.
 */
class LifeGameViewModel : ViewModel() {

    var uiState by mutableStateOf(LifeGameUiState())
        private set

    val currentEvent: UiEvent
        get() = SAMPLE_EVENTS[uiState.eventIndex.coerceIn(SAMPLE_EVENTS.indices)]

    fun startNewGame(characterId: Int) {
        uiState = LifeGameUiState(characterId = characterId, hasSaveData = uiState.hasSaveData)
    }

    /** 선택을 반영한다. 이미 결과가 나온 사건에 대한 중복 탭이면 false를 반환한다. */
    fun choose(choice: UiChoice): Boolean {
        val state = uiState
        if (state.lastResult != null) return false

        val event = currentEvent
        val newStats = state.stats.toMutableMap()
        val applied = mutableMapOf<String, Int>()
        choice.statChanges.forEach { (name, delta) ->
            val before = newStats[name] ?: STAT_START
            val after = (before + delta).coerceIn(STAT_MIN, STAT_MAX)
            newStats[name] = after
            applied[name] = after - before
        }

        uiState = state.copy(
            stats = newStats,
            lastResult = UiChoiceResult(
                eventId = event.id,
                choice = choice,
                appliedChanges = applied,
                isLastEvent = state.eventIndex >= SAMPLE_EVENTS.lastIndex,
            ),
            choiceHistory = state.choiceHistory + "[${event.stage}] ${choice.text}",
        )
        return true
    }

    /** 다음 사건으로 넘어간다. 마지막 사건이었다면 결말을 판정하고 true를 반환한다. */
    fun proceed(): Boolean {
        val state = uiState
        val result = state.lastResult ?: return state.ending != null
        return if (result.isLastEvent) {
            finish()
            true
        } else {
            uiState = state.copy(eventIndex = state.eventIndex + 1, lastResult = null)
            false
        }
    }

    /** 임시 버튼용: 현재 지표로 바로 결말을 판정한다. */
    fun finish() {
        val state = uiState
        uiState = state.copy(lastResult = null, ending = judgeEnding(state.stats))
    }

    fun reset() {
        uiState = LifeGameUiState(hasSaveData = uiState.hasSaveData)
    }

    private fun judgeEnding(stats: Map<String, Int>): UiEnding {
        val (topStat, _) = stats.maxBy { it.value }
        return UiEnding(
            title = "'$topStat' 지표가 빛난 인생",
            description = "임시 결말입니다. 가장 높은 지표인 '$topStat'을(를) 기준으로 판정했습니다. " +
                "GameEngine의 결말 판정이 연결되면 실제 결말로 바뀝니다.",
        )
    }
}
