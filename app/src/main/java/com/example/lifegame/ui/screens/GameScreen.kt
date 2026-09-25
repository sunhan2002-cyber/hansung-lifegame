package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.FALLBACK_EVENTS
import com.example.lifegame.ui.STAT_MAX
import com.example.lifegame.ui.StatLine
import com.example.lifegame.ui.label
import com.example.lifegame.ui.toLines

@Composable
fun GameScreen(
    event: GameEvent,
    stats: Stats,
    onChoose: (Choice) -> Unit,
    onShowEndingForTest: () -> Unit,
    modifier: Modifier = Modifier,
    usingFallbackEvents: Boolean = false,
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
        // 상황·지표 영역은 스크롤되고, 선택 버튼은 항상 하단에 고정된다.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = event.stage.label(),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                )
                if (isSpecial) {
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
            Spacer(Modifier.height(4.dp))
            Text(event.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))

            // 실제 이미지가 연결되기 전에는 imageId를 대체 문구로 보여 준다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("상황 이미지", color = colors.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = event.imageId,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Text(event.text, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))

            StatGrid(stats.toLines())

            if (usingFallbackEvents) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "임시 사건 데이터입니다. events.sample.json이 연결되면 실제 사건으로 바뀝니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }

            TextButton(
                onClick = onShowEndingForTest,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text("결말 화면 확인 (임시)")
            }
        }

        Spacer(Modifier.height(12.dp))
        event.choices.forEachIndexed { index, choice ->
            if (index > 0) Spacer(Modifier.height(10.dp))
            ChoiceButtonPlaceholder(label = choiceLabel(index), choice = choice, onChoose = onChoose)
        }
    }
}

/** 선택지 앞에 붙는 A, B, C… 표시 */
private fun choiceLabel(index: Int): String = ('A' + index).toString()

// 정원률 담당 ui/components/ChoiceButton.kt가 준비되면 교체한다.
@Composable
private fun ChoiceButtonPlaceholder(
    label: String,
    choice: Choice,
    onChoose: (Choice) -> Unit,
) {
    Button(
        onClick = { onChoose(choice) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text = "$label. ${choice.label}",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp),
        )
    }
}

/** 7개 지표를 2열로 보여준다. 정원률 담당 StatBar.kt가 준비되면 교체한다. */
@Composable
fun StatGrid(lines: List<StatLine>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { line ->
                    StatItem(line, Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatItem(line: StatLine, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row {
            Text(line.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text("${line.value}", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { line.value / STAT_MAX.toFloat() },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun GameScreenPreview() {
    MaterialTheme {
        GameScreen(
            event = FALLBACK_EVENTS[2],
            stats = Stats(),
            onChoose = {},
            onShowEndingForTest = {},
            usingFallbackEvents = true,
        )
    }
}
