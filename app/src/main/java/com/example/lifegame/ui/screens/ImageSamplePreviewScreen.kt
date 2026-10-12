package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.lifegame.ui.components.FilmMotion
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.FilmStrip
import com.example.lifegame.ui.preview.Week7ImageSamples
import com.example.lifegame.ui.theme.Week7DesignTokens as Design

/** 생성 원본을 게임과 같은 필름 틀에 표시한다. 정지 상태로 육안 검수한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageSamplePreviewScreen(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val index = selectedIndex.coerceIn(Week7ImageSamples.indices)
    val sample = Week7ImageSamples[index]
    var menuOpen by remember { mutableStateOf(false) }

    LazyColumn(
        modifier.fillMaxSize().safeDrawingPadding().testTag("image_sample_content"),
        contentPadding = PaddingValues(Design.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Design.SectionGap),
    ) {
        item {
            Text("예시 이미지 ${index + 1} / ${Week7ImageSamples.size}",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.fillMaxWidth().testTag("image_sample_position").semantics { heading() })
        }
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(sample.title, style = MaterialTheme.typography.titleMedium, softWrap = true,
                    modifier = Modifier.fillMaxWidth().testTag("image_sample_title"))
                Text(sample.viewpoint, style = MaterialTheme.typography.bodyMedium, color = Design.SecondaryInk)
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().testTag("image_sample_film")
                .semantics { contentDescription = "예시 이미지 · ${sample.title}" }) {
                val gateHeight = (maxWidth - Design.FilmSideBorder * 2).coerceAtLeast(1.dp) / Design.ImageAspectRatio
                FilmStrip(
                    current = FilmShot("image-sample:$index", "", sample.imageFile, false),
                    incoming = null,
                    gateHeight = gateHeight,
                    motion = { FilmMotion() },
                )
            }
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Design.ChoiceGap),
                verticalArrangement = Arrangement.spacedBy(Design.ChoiceGap)) {
                OutlinedButton(onClick = {
                    onSelect((index - 1 + Week7ImageSamples.size) % Week7ImageSamples.size)
                }, modifier = Modifier.heightIn(min = Design.ButtonMinHeight).testTag("image_sample_previous"),
                    shape = RoundedCornerShape(Design.CardCorner)) { Text("이전 이미지") }
                OutlinedButton(onClick = { onSelect((index + 1) % Week7ImageSamples.size) },
                    modifier = Modifier.heightIn(min = Design.ButtonMinHeight).testTag("image_sample_next"),
                    shape = RoundedCornerShape(Design.CardCorner)) { Text("다음 이미지") }
            }
        }
        item {
            Box {
                OutlinedButton(onClick = { menuOpen = true },
                    modifier = Modifier.fillMaxWidth().heightIn(min = Design.ButtonMinHeight)
                        .testTag("image_sample_menu"),
                    shape = RoundedCornerShape(Design.CardCorner)) { Text("장면 고르기") }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    Week7ImageSamples.forEachIndexed { sampleIndex, item ->
                        DropdownMenuItem(text = { Text("${sampleIndex + 1}. ${item.title}") },
                            modifier = Modifier.testTag("image_sample_select_$sampleIndex"),
                            onClick = { onSelect(sampleIndex); menuOpen = false })
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("원본 1448×1086 · 4:3", style = MaterialTheme.typography.bodyMedium, color = Design.SecondaryInk)
                Text("필름 안에서 잘림·색감·시점을 확인해 주세요. 검토용 후보이며 300×225 규격 처리는 보류 중입니다.",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium, color = Design.SecondaryInk)
            }
        }
    }
}
