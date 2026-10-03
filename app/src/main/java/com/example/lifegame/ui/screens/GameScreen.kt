package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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

@Suppress("UNUSED_PARAMETER") // 기존 FE 호출과 호환하되 임시 결말 버튼은 플레이 화면에서 숨긴다.
@Composable
fun GameScreen(
    event: GameEvent,
    stats: Stats,
    onChoose: (Choice) -> Unit,
    onShowEndingForTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(event.eventId) { listState.scrollToItem(0) }

    Box(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        // 선택지도 같은 스크롤에 포함해 작은 화면이나 큰 글씨에서 가려지지 않게 한다.
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 720.dp).fillMaxSize().testTag("game_content"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                EventCard(
                    title = event.title,
                    description = event.text,
                    imageId = event.imageId,
                    stage = event.stage.displayName(),
                    isSpecial = event.type == EventType.SPECIAL,
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "현재 지표",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    StatGrid(stats)
                }
            }
            item {
                Text(
                    "어떻게 할까요?",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                )
            }
            itemsIndexed(event.choices) { index, choice ->
                ChoiceButton(
                    text = "${choiceLabel(index)}. ${choice.label}",
                    onClick = { onChoose(choice) },
                )
            }
        }
    }
}

private fun choiceLabel(index: Int): String = ('A' + index).toString()

@Composable
fun StatGrid(stats: Stats, modifier: Modifier = Modifier) {
    val values = stats.toDisplayMap()
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 360.dp || fontScale >= 1.5f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            STAT_NAMES.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { name ->
                        StatBar(
                            name = name,
                            value = values[name] ?: STAT_START,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (columns == 2 && row.size == 1) Spacer(Modifier.weight(1f))
                }
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
