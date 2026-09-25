package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.StatChange

@Composable
fun ChoiceResultScreen(
    choiceLabel: String,
    resultText: String,
    changes: List<StatChange>,
    isLastEvent: Boolean,
    onNext: () -> Unit,
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
            Text("선택 결과", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = choiceLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(resultText, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("변화한 지표", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    // 변화량이 0인 지표는 아예 넘어오지 않는다.
                    if (changes.isEmpty()) {
                        Text("변화한 지표가 없습니다.", style = MaterialTheme.typography.bodyMedium)
                    }
                    changes.forEach { change ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(
                                text = change.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = if (change.delta > 0) "+${change.delta}" else "${change.delta}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (change.delta > 0) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text(
                text = if (isLastEvent) "결말 보기" else "다음 사건",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChoiceResultScreenPreview() {
    MaterialTheme {
        ChoiceResultScreen(
            choiceLabel = "A. 강아지를 향해 용감하게 걸어간다",
            resultText = "세 걸음 만에 넘어졌지만 강아지가 얼굴을 핥아 주었다.",
            changes = listOf(StatChange("운동능력", 5), StatChange("행복", 3)),
            isLastEvent = false,
            onNext = {},
        )
    }
}
