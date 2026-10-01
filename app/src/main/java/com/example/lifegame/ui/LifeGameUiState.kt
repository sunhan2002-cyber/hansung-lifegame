package com.example.lifegame.ui

import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.ChoiceResult
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.Stats

val STAT_NAMES = listOf("건강", "운동능력", "지력", "사회성", "경제력", "행복", "운")
const val STAT_MAX = 100
const val STAT_START = 50

data class UiChoiceResult(
    val eventId: String,
    val choice: Choice,
    val engineResult: ChoiceResult,
)

data class LifeGameUiState(
    val characterId: Int? = null,
    val hasSaveData: Boolean = false,
    val progress: GameProgress? = null,
    val currentEvent: GameEvent? = null,
    val lastResult: UiChoiceResult? = null,
    val ending: Ending? = null,
    val contentError: String? = null,
) {
    val stats: Stats get() = progress?.stats ?: Stats()
}
