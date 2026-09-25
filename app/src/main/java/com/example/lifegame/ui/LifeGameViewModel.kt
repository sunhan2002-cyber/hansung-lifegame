package com.example.lifegame.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.lifegame.data.ContentRepository
import com.example.lifegame.domain.engine.GameEngine
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.EndingConditions
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.GameScreenState
import com.example.lifegame.domain.model.LifeStage
import java.util.UUID

/**
 * 화면과 게임 상태를 연결한다.
 * 지표 계산·사건 선택·결말 판정은 GameEngine이 하고, 사건·결말 데이터는 ContentRepository가 읽는다.
 * 이 클래스는 둘을 이어 화면이 쓸 상태로만 정리한다.
 */
class LifeGameViewModel(
    private val repository: ContentRepository,
    private val engine: GameEngine = GameEngine(),
) : ViewModel() {

    var uiState by mutableStateOf(LifeGameUiState())
        private set

    private var events: List<GameEvent> = emptyList()
    private var endings: List<Ending> = emptyList()

    fun startNewGame(characterId: Int) {
        val loadedEvents = repository.loadEvents()
        val usingFallback = loadedEvents.isEmpty()
        events = loadedEvents.ifEmpty { FALLBACK_EVENTS }
        endings = repository.loadEndings()

        val progress = GameProgress(
            runId = UUID.randomUUID().toString(),
            currentEventOccurrenceId = newOccurrenceId(),
            selectedImageRef = "character_$characterId",
        )
        val firstEvent = engine.pickNextEvent(progress, events)

        uiState = LifeGameUiState(
            hasSaveData = uiState.hasSaveData,
            characterId = characterId,
            progress = progress,
            currentEvent = firstEvent,
            isLastEvent = firstEvent == null,
            usingFallbackEvents = usingFallback,
        )
        if (firstEvent == null) finish()
    }

    /**
     * 선택을 반영한다.
     * 같은 사건에서 이미 결과가 나왔으면(버튼 연타 포함) false를 돌려주고 화면을 움직이지 않는다.
     */
    fun choose(choice: Choice): Boolean {
        val state = uiState
        val progress = state.progress ?: return false
        val event = state.currentEvent ?: return false
        if (state.lastChoice != null) return false

        val result = engine.applyChoice(progress, event, choice)
        if (!result.wasApplied) return false

        uiState = state.copy(
            progress = result.progress,
            lastChoice = choice,
            lastChanges = result.appliedDelta.toChanges(),
            lastResultText = result.resultText,
            isLastEvent = nextEventFor(result.progress) == null,
        )
        return true
    }

    /**
     * 다음 사건으로 넘어간다. 남은 사건이 없으면 결말을 판정하고 true를 돌려준다.
     * 현재 단계에 남은 사건이 없으면 다음 성장 단계로 넘어간다.
     */
    fun proceed(): Boolean {
        val state = uiState
        val progress = state.progress ?: return false
        if (state.lastChoice == null) return state.ending != null

        val advanced = advanceStageIfNeeded(progress)
        val nextEvent = engine.pickNextEvent(advanced, events)
        if (nextEvent == null) {
            finish()
            return true
        }

        uiState = state.copy(
            progress = advanced.copy(
                screenState = GameScreenState.EVENT,
                currentEventOccurrenceId = newOccurrenceId(),
            ),
            currentEvent = nextEvent,
            lastChoice = null,
            lastChanges = emptyList(),
            lastResultText = "",
        )
        return false
    }

    /** 임시 버튼용: 현재 지표로 바로 결말을 판정한다. */
    fun finish() {
        val state = uiState
        val progress = state.progress ?: return
        val ending = resolveEndingOrFallback(progress)
        uiState = state.copy(
            progress = progress.copy(screenState = GameScreenState.ENDING),
            lastChoice = null,
            lastChanges = emptyList(),
            lastResultText = "",
            ending = ending,
        )
    }

    fun reset() {
        uiState = LifeGameUiState(hasSaveData = uiState.hasSaveData)
    }

    /** 현재 진행 상태에서 (필요하면 단계를 넘겨) 다음 사건을 미리 본다. */
    private fun nextEventFor(progress: GameProgress): GameEvent? =
        engine.pickNextEvent(advanceStageIfNeeded(progress), events)

    /**
     * 현재 단계에 보여 줄 사건이 없으면 다음 성장 단계로 넘긴다.
     * 단계 전환 규칙은 아직 정해지지 않아, 5주차에서는 "남은 사건이 없으면 다음 단계"로 둔다.
     */
    private fun advanceStageIfNeeded(progress: GameProgress): GameProgress {
        var current = progress
        while (engine.pickNextEvent(current, events) == null) {
            val nextStage = current.stage.next() ?: return current
            current = current.copy(stage = nextStage)
        }
        return current
    }

    private fun resolveEndingOrFallback(progress: GameProgress): Ending =
        runCatching { engine.resolveEnding(progress, endings) }
            .getOrElse {
                Ending(
                    endingId = "ending_placeholder",
                    title = "기록되지 않은 인생",
                    summary = "결말 데이터를 불러오지 못해 임시 결말을 표시합니다. " +
                        "endings.sample.json이 연결되면 실제 결말로 바뀝니다.",
                    priority = 0,
                    conditions = EndingConditions(),
                    isDefault = true,
                )
            }

    private fun newOccurrenceId(): String = UUID.randomUUID().toString()

    private fun LifeStage.next(): LifeStage? = when (this) {
        LifeStage.INFANT -> LifeStage.CHILD
        LifeStage.CHILD -> LifeStage.TEEN
        LifeStage.TEEN -> LifeStage.ADULT
        LifeStage.ADULT -> null
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer { LifeGameViewModel(ContentRepository(appContext)) }
            }
        }
    }
}
