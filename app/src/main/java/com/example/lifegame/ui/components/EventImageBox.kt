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

/** 이미지 연결 전에도 사건의 이미지 ID와 대체 문구를 보여주는 영역. */
@Composable
fun EventImageBox(
    imageId: String,
    modifier: Modifier = Modifier,
    fallbackText: String = "이미지를 준비 중입니다",
) {
    val colors = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // 기본 비율은 4:3. 큰 글꼴이나 긴 ID는 영역 높이를 늘려 표시한다.
        val minimumHeight = if (constraints.hasBoundedWidth) maxWidth * 3f / 4f else 180.dp

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
            Text(
                text = if (imageId.isBlank()) "이미지 ID 없음" else "이미지 ID: $imageId",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
