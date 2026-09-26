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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.SAMPLE_EVENTS
import com.example.lifegame.ui.STAT_NAMES
import com.example.lifegame.ui.STAT_START
import com.example.lifegame.ui.UiChoice
import com.example.lifegame.ui.UiEvent
import com.example.lifegame.ui.components.ChoiceButton
import com.example.lifegame.ui.components.EventCard
import com.example.lifegame.ui.components.StatBar

@Composable
fun GameScreen(
    event: UiEvent,
    stats: Map<String, Int>,
    onChoose: (UiChoice) -> Unit,
    onShowEndingForTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(if (event.isSpecial) colors.errorContainer.copy(alpha = 0.35f) else colors.surface)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // 상황·지표 영역은 스크롤되고, 선택 버튼은 항상 하단에 고정된다.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(event.stage, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                if (event.isSpecial) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "돌발 이벤트",
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onError,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(colors.error)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // 상황 이미지가 준비되기 전 자리 표시 영역
            EventCard()

            Spacer(Modifier.height(16.dp))

            Text(event.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))

            StatGrid(stats)

            TextButton(
                onClick = onShowEndingForTest,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("결말 화면 확인 (임시)")
            }
        }

        Spacer(Modifier.height(12.dp))

        ChoiceButton(
            text = "${event.choiceA.label}. ${event.choiceA.text}",
            onClick = { onChoose(event.choiceA) }
        )

        Spacer(Modifier.height(10.dp))

        ChoiceButton(
            text = "${event.choiceB.label}. ${event.choiceB.text}",
            onClick = { onChoose(event.choiceB) }
        )
    }
}

/** 7개 지표를 2열로 보여준다. 정원률 담당 StatBar.kt가 준비되면 교체한다. */
@Composable
fun StatGrid(stats: Map<String, Int>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        STAT_NAMES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { name ->
                    StatBar(
                        name = name,
                        value = stats[name] ?: STAT_START,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun GameScreenPreview() {
    MaterialTheme {
        GameScreen(
            event = SAMPLE_EVENTS[2],
            stats = STAT_NAMES.associateWith { STAT_START },
            onChoose = {},
            onShowEndingForTest = {},
        )
    }
}
