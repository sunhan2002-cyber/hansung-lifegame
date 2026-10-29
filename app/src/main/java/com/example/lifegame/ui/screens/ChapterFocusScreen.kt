package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.size
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.PlayerAvatar
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.components.EffectText
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.PixelAssetImage
import com.example.lifegame.ui.images.ImageCandidate
import com.example.lifegame.ui.components.SectionLabel
import com.example.lifegame.ui.components.ThinRule
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.withRo

/**
 * 시안 8-1 장 표지 · 초점 고르기. 시기가 시작될 때 한 번만 고르고, 그 시기 동안은 바꿀 수 없다 (R-10, R-11).
 */
@Composable
fun ChapterFocusScreen(
    state: PeriodPlayUiState,
    onSelectFocus: (focusId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedId by rememberSaveable(state.runKey, state.periodName) { mutableStateOf<String?>(null) }
    val selected = state.focusOptions.firstOrNull { it.focusId == selectedId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("chapter_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.chapterLabel.isNotBlank()) {
                Text(state.chapterLabel, color = LifeGameInkSecondary, fontSize = 13.sp)
            }
            Text(
                text = state.periodName,
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 42.sp,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = state.periodIntro,
                color = LifeGameInkSecondary,
                fontFamily = LifeGameSerif,
                fontSize = 15.sp,
                lineHeight = 23.sp,
            )

            GrowthStage(state)

            SectionLabel("이번 시기에 집중할 것", trailing = "시기가 끝날 때까지 바꿀 수 없어요")
            state.focusOptions.forEach { option ->
                SelectCard(
                    selected = option.focusId == selectedId,
                    onSelect = { selectedId = option.focusId },
                    modifier = Modifier.testTag("focus_option"),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(option.label, color = LifeGameInk, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                        EffectText(option.effects)
                        if (option.description.isNotBlank()) {
                            Text(
                                text = option.description,
                                color = LifeGameInkTertiary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        InkButton(
            text = selected?.let { "${state.periodName} 시작하기" } ?: "초점을 골라 주세요",
            enabled = selected != null,
            onClick = { selected?.let { onSelectFocus(it.focusId) } },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

/**
 * 지난 시기 끝 모습 → 이번 시기 시작 모습. 같은 외형 · 같은 체형 규칙으로 조립한 한 인물이다.
 * 두 그림을 같은 높이의 칸에 그려 실제로 자란 키 차이가 보인다.
 */
@Composable
private fun GrowthStage(state: PeriodPlayUiState) {
    val previousPeriod = LifePeriod.entries.getOrNull(state.avatar.period.ordinal - 1)
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("growth_stage"), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
            if (state.previousPeriodName != null && previousPeriod != null) {
                GrowthFigure(state.avatar.copy(period = previousPeriod), "${state.previousPeriodName} 끝", false)
                Box(Modifier.padding(horizontal = 22.dp, vertical = 48.dp)) {
                    Text("→", color = LifeGameInkTertiary, fontSize = 18.sp)
                }
            }
            GrowthFigure(state.avatar, "${state.periodName} 시작", true)
        }
        Spacer(Modifier.height(4.dp))
        ThinRule(Modifier.width(240.dp))
    }
}

@Composable
private fun GrowthFigure(look: AvatarLook, label: String, emphasized: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlayerAvatar(look, Modifier.size(width = 84.dp, height = 132.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            color = if (emphasized) LifeGameInk else LifeGameInkTertiary,
            fontSize = 13.sp,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

