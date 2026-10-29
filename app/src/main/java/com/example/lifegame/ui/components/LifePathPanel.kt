package com.example.lifegame.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.FactLineUi
import com.example.lifegame.ui.FocusOptionUi
import com.example.lifegame.ui.LifeFactsUi
import com.example.lifegame.ui.StatValueUi
import com.example.lifegame.ui.educationCareerLines
import com.example.lifegame.ui.familyLifeLines
import com.example.lifegame.ui.screens.StatGrid
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameSerif

/**
 * 플레이 중 "지금 내 인생" 요약 (8주차).
 * 1줄: 이번 시기 초점 · 대표 능력치, 2줄: 교육 · 직업 · 관계 · 가족 · 주거 · 경제력 · 업적.
 * 누르면 전체 사실 시트가 열린다.
 */
@Composable
fun LifePathSummary(
    focus: FocusOptionUi?,
    topStat: StatValueUi?,
    facts: LifeFactsUi,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameInk.copy(alpha = 0.62f))
            .border(1.dp, LifeGamePaper.copy(alpha = 0.22f), shape)
            .clickable(role = Role.Button, onClickLabel = "지금까지의 인생 자세히 보기", onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("life_path_summary"),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = LifeGamePaper.copy(alpha = 0.86f))) {
                    append(if (focus != null) "이번 시기 초점 " else "초점 없음")
                }
                if (focus != null) {
                    withStyle(SpanStyle(color = LifeGamePaper, fontWeight = FontWeight.Bold, fontSize = 13.sp)) {
                        append(focus.label)
                    }
                }
                if (topStat != null) {
                    withStyle(SpanStyle(color = LifeGamePaper.copy(alpha = 0.86f))) {
                        append("  ·  대표 ${topStat.name} ${topStat.value}")
                    }
                }
            },
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.testTag("life_focus_line"),
        )
        Text(
            text = facts.shortLine.ifBlank { "아직 기록된 인생 사실이 없다" },
            color = LifeGamePaper,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.testTag("life_facts_line"),
        )
    }
}

/**
 * 지금까지 쌓인 사실 전체. 값이 없는 항목도 기본 문구로 보인다.
 * 10주차부터 플레이 화면의 초점 요약 박스를 없앴으므로, 이번 시기 초점도 이 창에서 확인한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeFactsSheet(
    facts: LifeFactsUi,
    stats: Stats,
    onDismiss: () -> Unit,
    place: String? = null,
    focus: FocusOptionUi? = null,
    showFocus: Boolean = false,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LifeGamePaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("life_facts_sheet")
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("지금까지의 나", style = MaterialTheme.typography.headlineMedium)
            if (place != null) {
                Text(place, color = LifeGameInkTertiary, fontSize = 14.sp)
            }
            if (showFocus) {
                SectionLabel("이번 시기 초점")
                Column(Modifier.fillMaxWidth().testTag("details_focus"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (focus == null) {
                        Text("이번 시기는 초점 없이 흘러간다", color = LifeGameInkTertiary, fontSize = 15.sp)
                    } else {
                        Text(focus.label, color = LifeGameInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("시기가 끝날 때까지 바꿀 수 없다", color = LifeGameInkTertiary, fontSize = 13.sp)
                        if (focus.effects.isNotEmpty()) EffectText(focus.effects, fontSize = 14.sp)
                    }
                }
            }
            SectionLabel("교육 · 직업")
            FactLines(facts.educationCareerLines())
            SectionLabel("가족 · 생활")
            FactLines(facts.familyLifeLines())
            SectionLabel("업적", trailing = facts.achievements.size.takeIf { it > 0 }?.let { "${it}개" })
            Achievements(facts.achievements)
            SectionLabel("능력치")
            StatGrid(stats)
            Text(
                "건강 상태: ${healthLevelOf(stats.health).label}",
                color = LifeGameInkTertiary,
                fontSize = 13.sp,
                modifier = Modifier.testTag("details_health_text"),
            )
        }
    }
}

/** "교육  대학 진학"처럼 이름표와 값. 값이 기본 문구면 흐리게. */
@Composable
fun FactLines(
    lines: List<FactLineUi>,
    modifier: Modifier = Modifier,
    labelColor: Color = LifeGameInkTertiary,
    valueColor: Color = LifeGameInk,
    unknownColor: Color = LifeGameInkTertiary,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = line.label,
                    color = labelColor,
                    fontSize = 13.sp,
                    modifier = Modifier.widthIn(min = 56.dp),
                )
                Text(
                    text = line.value,
                    color = if (line.known) valueColor else unknownColor,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = if (line.known) FontWeight.Medium else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
fun Achievements(achievements: List<String>, modifier: Modifier = Modifier) {
    if (achievements.isEmpty()) {
        Text("아직 남긴 업적이 없다", color = LifeGameInkTertiary, fontSize = 14.sp, modifier = modifier)
        return
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        achievements.forEach { name ->
            Text(
                text = "· $name",
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }
    }
}
