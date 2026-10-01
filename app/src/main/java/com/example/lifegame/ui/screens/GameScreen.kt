package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.STAT_NAMES
import com.example.lifegame.ui.STAT_START
import com.example.lifegame.ui.components.ChoiceButton
import com.example.lifegame.ui.components.EventCard
import com.example.lifegame.ui.components.StatBar
import com.example.lifegame.ui.replacePlayerPlaceholders

@Composable
fun GameScreen(
    event: GameEvent,
    stats: Stats,
    onChoose: (Choice) -> Unit,
    onShowEndingForTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val isSpecial = event.type == EventType.SPECIAL

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (isSpecial) colors.errorContainer.copy(alpha = 0.35f) else colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            EventCard(
                title = event.title.replacePlayerPlaceholders(),
                description = event.text.replacePlayerPlaceholders(),
                imageId = event.imageId,
                stage = event.stage.displayName(),
                isSpecial = isSpecial,
            )
            Spacer(Modifier.height(20.dp))
            StatGrid(stats)

            TextButton(
                onClick = onShowEndingForTest,
                modifier = Modifier.align(androidx.compose.ui.Alignment.End),
            ) {
                Text("결말 화면 확인 (임시)")
            }
        }

        Spacer(Modifier.height(12.dp))
        event.choices.forEachIndexed { index, choice ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            ChoiceButton(
                text = "${choiceLabel(index)}. ${choice.label.replacePlayerPlaceholders()}",
                onClick = { onChoose(choice) },
            )
        }
    }
}

private fun choiceLabel(index: Int): String = ('A' + index).toString()

@Composable
fun StatGrid(stats: Stats, modifier: Modifier = Modifier) {
    val values = stats.toDisplayMap()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        STAT_NAMES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { name ->
                    StatBar(
                        name = name,
                        value = values[name] ?: STAT_START,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private fun LifeStage.displayName(): String = when (this) {
    LifeStage.INFANT -> "영아기"
    LifeStage.CHILD -> "유년기"
    LifeStage.TEEN -> "청소년기"
    LifeStage.ADULT -> "성인기"
}

private fun Stats.toDisplayMap(): Map<String, Int> = mapOf(
    "건강" to health,
    "운동능력" to fitness,
    "지력" to intelligence,
    "사회성" to social,
    "경제력" to wealth,
    "행복" to happiness,
    "운" to luck,
)

