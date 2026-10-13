package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** 이미지의 모양만 검수한다. 게임 상태·타이머·시기 진행·결과 판정은 만들지 않는다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesignPreviewScreen(onBack: () -> Unit) {
    var selectedSampleIndex by rememberSaveable { mutableStateOf(0) }
    Column(
        Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top)),
    ) {
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack, modifier = Modifier.testTag("preview_exit")) {
                Text("돌아가기")
            }
            Text(
                "예시 이미지 검수",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            )
            TextButton(
                onClick = { selectedSampleIndex = 0 },
                modifier = Modifier.testTag("preview_reset"),
            ) {
                Text("다시 보기")
            }
        }
        ImageSamplePreviewScreen(
            selectedIndex = selectedSampleIndex,
            onSelect = { selectedSampleIndex = it },
            modifier = Modifier.weight(1f),
        )
    }
}
