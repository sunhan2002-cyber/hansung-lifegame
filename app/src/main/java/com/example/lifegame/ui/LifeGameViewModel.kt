package com.example.lifegame.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.domain.engine.GameSession
import com.example.lifegame.domain.model.Choice

class LifeGameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(application)
    private var session: GameSession? = null

    var uiState by mutableStateOf(LifeGameUiState())
        private set

    fun startNewGame(characterId: Int) {
        val events = repository.loadEvents()
        val endings = repository.loadEndings()
        if (events.isEmpty() || endings.none { it.isDefault }) {
            uiState = LifeGameUiState(
                characterId = characterId,
                contentError = "사건 또는 기본 결말 데이터를 불러오지 못했습니다.",
            )
            return
        }

        session = GameSession(events = events, endings = endings)
        val started = session!!.start()
        uiState = LifeGameUiState(
            characterId = characterId,
            progress = started.progress,
            currentEvent = started.currentEvent,
            ending = started.ending,
        )
    }

    fun choose(choice: Choice): Boolean {
        val event = uiState.currentEvent ?: return false
        val result = session?.choose(choice) ?: return false
        uiState = uiState.copy(
            progress = result.progress,
            lastResult = UiChoiceResult(event.eventId, choice, result),
        )
        return true
    }

    fun proceed(): Boolean {
        val hasNextEvent = session?.proceed() ?: return false
        syncFromSession()
        return !hasNextEvent
    }

    fun finish() {
        session?.finish()
        syncFromSession()
    }

    fun reset() {
        session = null
        uiState = LifeGameUiState(hasSaveData = uiState.hasSaveData)
    }

    private fun syncFromSession() {
        val sessionState = session?.state ?: return
        uiState = uiState.copy(
            progress = sessionState.progress,
            currentEvent = sessionState.currentEvent,
            lastResult = sessionState.lastResult?.let { result ->
                UiChoiceResult(
                    eventId = result.progress.choiceHistory.lastOrNull()?.eventId.orEmpty(),
                    choice = requireNotNull(sessionState.selectedChoice),
                    engineResult = result,
                )
            },
            ending = sessionState.ending,
        )
    }
}
