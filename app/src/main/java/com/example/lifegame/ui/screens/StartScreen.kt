package com.example.lifegame.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.lifegame.ui.theme.LifeGameTheme
import com.example.lifegame.ui.theme.Week7DesignTokens as Design
import androidx.compose.ui.unit.dp

@Composable
fun StartScreen(
    canContinue: Boolean,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onDesignPreview: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(Design.ScreenPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "인생게임",
            style = MaterialTheme.typography.displaySmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "아기로 시작해 선택으로 만드는 나만의 인생",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNewGame,
            shape = RoundedCornerShape(Design.CardCorner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Design.CardPadding),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Design.ButtonMinHeight),
        ) {
            Text("새 게임", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(Design.SectionGap))
        OutlinedButton(
            onClick = onContinue,
            shape = RoundedCornerShape(Design.CardCorner),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Design.CardPadding),
            enabled = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Design.ButtonMinHeight),
        ) {
            Text("이어하기", style = MaterialTheme.typography.titleMedium)
        }
        onDesignPreview?.let { openPreview ->
            Spacer(Modifier.height(Design.SectionGap))
            TextButton(onClick = openPreview) { Text("디자인 미리보기") }
        }
        if (!canContinue) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "저장된 게임이 없습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StartScreenPreview() {
    LifeGameTheme { StartScreen(canContinue = false, onNewGame = {}, onContinue = {}) }
}
