package com.example.lifegame.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

private val DEFAULT_CHARACTERS = listOf(1 to "아기 1", 2 to "아기 2", 3 to "아기 3")

@Composable
fun CharacterSelectScreen(
    onStartGame: (characterId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable { mutableStateOf<Int?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
    ) {
        Text("캐릭터 설정", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            text = "인생을 시작할 아기를 골라 주세요.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DEFAULT_CHARACTERS.forEach { (id, name) ->
                val selected = selectedId == id
                OutlinedCard(
                    onClick = { selectedId = id },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(
                        width = if (selected) 3.dp else 1.dp,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // 기본 아기 이미지가 준비되기 전 자리 표시 영역
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("👶", style = MaterialTheme.typography.displayMedium)
                        }
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = { selectedId?.let(onStartGame) },
            enabled = selectedId != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("게임 시작", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CharacterSelectScreenPreview() {
    MaterialTheme { CharacterSelectScreen(onStartGame = {}) }
}
