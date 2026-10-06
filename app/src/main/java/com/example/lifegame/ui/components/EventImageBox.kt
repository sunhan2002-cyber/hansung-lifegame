package com.example.lifegame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** 이미지 연결 전의 대체 영역. imageId는 기존 호출 계약을 유지하되 화면에 노출하지 않는다. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun EventImageBox(
    imageId: String,
    modifier: Modifier = Modifier,
    fallbackText: String = "이미지를 준비 중입니다",
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 실제 그림이 없는 동안 본문보다 큰 빈 공간을 만들지 않는다.
        // 최소 높이만 지정하므로 큰 글씨의 대체 문구도 잘리지 않는다.
        val minimumHeight = if (constraints.hasBoundedWidth) {
            (maxWidth * 9f / 16f).coerceIn(112.dp, 160.dp)
        } else {
            112.dp
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = fallbackText.ifBlank { "이미지를 준비 중입니다" },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
