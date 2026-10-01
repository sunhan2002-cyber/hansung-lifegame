package com.example.lifegame.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

/**
 * 사건 표시만 담당한다. 스크롤과 StatGrid 배치는 호출 화면에서 처리한다.
 * 기본값은 기존 EventCard() 호출과의 호환을 위해 제공한다.
 */
@Composable
fun EventCard(
    modifier: Modifier = Modifier,
    title: String = "",
    description: String = "",
    imageId: String = "",
    stage: String = "",
    isSpecial: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isSpecial) lerp(colors.surface, colors.errorContainer, 0.25f) else colors.surface,
        contentColor = colors.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (stage.isNotBlank()) {
                Text(
                    text = stage,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primary,
                )
            }

            if (title.isNotBlank() || isSpecial) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    if (isSpecial) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colors.errorContainer,
                            contentColor = colors.onErrorContainer,
                        ) {
                            Text(
                                text = "돌발 이벤트",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            EventImageBox(imageId = imageId)

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
