package com.example.lifegame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ChoiceButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 14.dp
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Start
        )
    }
}

/**
 * 전달받은 선택지만 순서대로 표시한다. 빈 선택지 자리는 만들지 않는다.
 * 화면 하단 고정과 위쪽 콘텐츠의 스크롤은 호출하는 화면에서 배치한다.
 */
@Composable
fun <T> ChoiceButtonGroup(
    choices: List<T>,
    choiceText: (T) -> String,
    onChoose: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        choices.forEach { choice ->
            ChoiceButton(
                text = choiceText(choice),
                onClick = { onChoose(choice) },
            )
        }
    }
}