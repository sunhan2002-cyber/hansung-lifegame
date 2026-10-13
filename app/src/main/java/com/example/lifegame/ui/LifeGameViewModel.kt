package com.example.lifegame.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.data.ProgressStore
import com.example.lifegame.data.SavedGame
import com.example.lifegame.domain.engine.GameSession
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.GameScreenState

class LifeGameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ContentRepository(application)
    private val progressStore = ProgressStore(application)
    private var session: GameSession? = null

    var uiState by mutableStateOf(LifeGameUiState(hasSaveData = progressStore.hasSaveData()))
        private set

    fun startNewGame(characterId: Int, playerName: String) {
        val name = normalizePlayerName(playerName)
        val nameCall = nameCallOf(name)
        // 세션에 넘기기 전에 {name}, {nameCall}을 바꿔 두면 사건 · 선택지 · 결과 · 기록 · 결말 어디에도 남지 않는다.
        val events = repository.loadEvents().map { it.withPlayerName(name, nameCall) }
        val endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) }
        if (events.isEmpty() || endings.none { it.isDefault }) {
            session = null
            uiState = LifeGameUiState(
                characterId = characterId,
                playerName = name,
                nameCall = nameCall,
                contentError = "사건 또는 기본 결말 데이터를 불러오지 못했습니다.",
            )
            return
        }

        session = GameSession(events = events, endings = endings)
        val started = session!!.start()
        uiState = LifeGameUiState(
            characterId = characterId,
            hasSaveData = true,
            playerName = name,
            nameCall = nameCall,
            progress = started.progress,
            currentEvent = started.currentEvent,
            ending = started.ending,
            eventsById = events.associateBy { it.eventId },
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
        persistSession()
    }

    fun continueGame(): GameScreenState? {
        val savedGame = progressStore.load() ?: return clearInvalidSave()
        val name = normalizePlayerName(savedGame.playerName)
        val nameCall = nameCallOf(name)
        val events = repository.loadEvents().map { it.withPlayerName(name, nameCall) }
        val endings = repository.loadEndings().map { it.withPlayerName(name, nameCall) }
        val restoredSession = runCatching {
            GameSession(events = events, endings = endings).also {
                it.restore(savedGame.progress)
            }
        }.getOrElse { return clearInvalidSave() }

        session = restoredSession
        uiState = LifeGameUiState(
            characterId = savedGame.characterId,
            playerName = name,
            nameCall = nameCall,
            hasSaveData = true,
            eventsById = events.associateBy { it.eventId },
            imageFiles = repository.loadImageAssets()
                .filter { it.fileName.isNotBlank() }
                .associate { it.imageId to it.fileName },
        )
        syncFromSession()
        return uiState.progress?.screenState
    }

    fun choose(choice: Choice): Boolean {
        val event = uiState.currentEvent ?: return false
        val result = session?.choose(choice) ?: return false
        uiState = uiState.copy(
            progress = result.progress,
            lastResult = UiChoiceResult(event.eventId, choice, result),
        )
        persistSession()
        return true
    }

    fun proceed(): Boolean {
        val hasNextEvent = session?.proceed() ?: return false
        syncFromSession()
        persistSession()
        return !hasNextEvent
    }

    fun finish() {
        session?.finish()
        syncFromSession()
        persistSession()
    }

    fun reset() {
        session = null
        progressStore.clear()
        uiState = LifeGameUiState(hasSaveData = false)
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

    private fun persistSession() {
        val characterId = uiState.characterId ?: return
        val progress = session?.state?.progress ?: return
        progressStore.save(
            SavedGame(
                characterId = characterId,
                playerName = uiState.playerName,
                progress = progress,
            ),
        )
        uiState = uiState.copy(hasSaveData = true)
    }

    private fun clearInvalidSave(): GameScreenState? {
        progressStore.clear()
        session = null
        uiState = LifeGameUiState(
            hasSaveData = false,
            contentError = "저장된 진행 상태를 복원하지 못했습니다. 새 게임을 시작해 주세요.",
        )
        return null
    }
}
