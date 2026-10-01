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
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.UiChoiceResult
import com.example.lifegame.ui.replacePlayerPlaceholders

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
                text = "${result.choice.choiceId}. ${result.choice.label.replacePlayerPlaceholders()}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(result.engineResult.resultText.replacePlayerPlaceholders(), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("변화한 지표", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    val appliedChanges = result.engineResult.appliedDelta.toDisplayMap()
                        .filterValues { it != 0 }
                    if (appliedChanges.isEmpty()) {
                        Text("변화 없음", style = MaterialTheme.typography.bodyMedium)
                    }
                    appliedChanges.forEach { (name, delta) ->
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
                text = "다음 사건",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

private fun com.example.lifegame.domain.model.StatDelta.toDisplayMap(): Map<String, Int> = mapOf(
    "건강" to health,
    "운동능력" to fitness,
    "지력" to intelligence,
    "사회성" to social,
    "경제력" to wealth,
    "행복" to happiness,
    "운" to luck,
)

