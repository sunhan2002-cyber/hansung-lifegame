package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifegame.domain.model.ChoiceHistory
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.toLines

/** 결말 화면에 보여 줄 최근 선택 기록 개수 */
private const val RECENT_HISTORY_COUNT = 5

@Composable
fun EndingScreen(
    ending: Ending,
    stats: Stats,
    choiceHistory: List<ChoiceHistory>,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = "최종 결말",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(ending.title, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(ending.summary, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))
            Text("최종 지표", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            StatGrid(stats.toLines())

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))
            Text("최근 선택 기록", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val recent = choiceHistory.takeLast(RECENT_HISTORY_COUNT)
            if (recent.isEmpty()) {
                Text("기록된 선택이 없습니다.", style = MaterialTheme.typography.bodyMedium)
            }
            recent.forEachIndexed { index, record ->
                Text(
                    text = "${index + 1}. ${record.resultText}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 3.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("다시 시작", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun EndingScreenPreview() {
    MaterialTheme {
        EndingScreen(
            ending = Ending(
                endingId = "ending_ordinary_path",
                title = "평범하지만 따뜻한 인생",
                summary = "큰 사건은 없었지만, 곁에 남은 사람들이 많은 인생이었다.",
                priority = 0,
                isDefault = true,
            ),
            stats = Stats(),
            choiceHistory = emptyList(),
            onRestart = {},
        )
    }
}
