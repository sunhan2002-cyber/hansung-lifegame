package com.example.lifegame.ui.components

import androidx.compose.foundation.background
import com.example.lifegame.ui.theme.Week7DesignTokens as Design
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
        // 실물 이미지와 동일한 4:3 자리. 큰 대체 문구에는 최소 높이보다 더 많은 공간을 허용한다.
        // 최소 높이만 지정하므로 큰 글씨의 대체 문구도 잘리지 않는다.
        val minimumHeight = if (constraints.hasBoundedWidth) {
            maxWidth / Design.ImageAspectRatio
        } else {
            112.dp
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minimumHeight)
                .clip(RoundedCornerShape(Design.CardCorner))
                .background(colors.surfaceVariant)
                .padding(Design.CardPadding),
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
