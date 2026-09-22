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
import com.example.lifegame.ui.SAMPLE_EVENTS
import com.example.lifegame.ui.UiChoiceResult

@Composable
fun ChoiceResultScreen(
    result: UiChoiceResult,
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
                text = "${result.choice.label}. ${result.choice.text}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(result.choice.resultText, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("변화한 지표", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    if (result.appliedChanges.isEmpty()) {
                        Text("변화 없음", style = MaterialTheme.typography.bodyMedium)
                    }
                    result.appliedChanges.forEach { (name, delta) ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
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
                text = if (result.isLastEvent) "결말 보기" else "다음 사건",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChoiceResultScreenPreview() {
    val choice = SAMPLE_EVENTS[0].choiceA
    MaterialTheme {
        ChoiceResultScreen(
            result = UiChoiceResult(SAMPLE_EVENTS[0].id, choice, choice.statChanges, isLastEvent = false),
            onNext = {},
        )
    }
}
