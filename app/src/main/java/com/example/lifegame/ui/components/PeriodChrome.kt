package com.example.lifegame.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.FocusOptionUi
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.StatChangeUi
import com.example.lifegame.ui.screens.StatGrid
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatUp

/** 시안 8 헤더: 시기 이름 + N번째 해 + 능력치 버튼 + 시기 진행 막대 5칸. 나이는 보여 주지 않는다. */
@Composable
fun PeriodHeader(
    periodName: String,
    yearLabel: String,
    marks: List<PeriodMarkUi>,
    stats: Stats,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = periodName,
                    color = LifeGameInk,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = yearLabel,
                    color = LifeGameInkSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            StatsButton(stats = stats, changes = null, onClick = onStatsClick)
        }
        Spacer(Modifier.height(14.dp))
        PeriodProgress(marks)
    }
}

@Composable
fun PeriodProgress(
    marks: List<PeriodMarkUi>,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    val track = if (dark) Color(0xFF3A3631) else LifeGameRule
    val fill = if (dark) LifePaperOnDark else LifeGameInk
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = marks.firstOrNull { it.isCurrent }?.let { "${it.label} 시기" }.orEmpty()
            },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        marks.forEach { mark ->
            Column(Modifier.weight(1f)) {
                Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(track, cornerRadius = radius)
                    if (mark.fill > 0f) {
                        drawRoundRect(
                            fill,
                            size = Size(maxOf(size.height, size.width * mark.fill), size.height),
                            cornerRadius = radius,
                        )
                    }
                }
                Text(
                    text = mark.label,
                    modifier = Modifier.padding(top = 5.dp),
                    color = when {
                        mark.isCurrent && dark -> LifePaperOnDark
                        mark.isCurrent -> LifeGameInk
                        dark -> LifeMutedOnDark
                        else -> LifeGameInkTertiary
                    },
                    fontSize = 10.sp,
                    fontWeight = if (mark.isCurrent) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** "이번 시기 초점 학업 [자물쇠] 5해 고정 … 지력 ↑ 행복 ↓" — 시기 동안 바꿀 수 없다는 표시 (R-11). */
@Composable
fun FocusLockRow(
    focus: FocusOptionUi,
    years: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("focus_lock_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("이번 시기 초점", color = LifeGameInkTertiary, fontSize = 12.sp)
            Text(focus.label, color = LifeGameInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            LockIcon()
            Text("${years}해 고정", color = LifeGameInkTertiary, fontSize = 11.5.sp)
        }
        EffectText(focus.effects, fontSize = 12.sp)
    }
}

/** 능력치 효과를 "지력 ↑ · 행복 ↓"처럼 쓴다. 오르면 파랑, 내리면 빨강. */
@Composable
fun EffectText(
    effects: List<StatChangeUi>,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp,
    showAmount: Boolean = false,
) {
    if (effects.isEmpty()) return
    val text = buildAnnotatedString {
        effects.forEachIndexed { index, effect ->
            if (index > 0) withStyle(SpanStyle(color = LifeGameInkTertiary)) { append("  ·  ") }
            val up = effect.delta > 0
            withStyle(SpanStyle(color = if (up) LifeGameStatUp else LifeGameStatDown, fontWeight = FontWeight.Bold)) {
                append(effect.name)
                append(' ')
                if (showAmount) {
                    append(if (up) "+${effect.delta}" else "−${-effect.delta}")
                } else {
                    append(if (up) "↑" else "↓")
                }
            }
        }
    }
    Text(text = text, modifier = modifier, fontSize = fontSize, lineHeight = fontSize * 1.4f)
}

/** 초점 고정 표시용 작은 자물쇠. 이모지 대신 직접 그린다. */
@Composable
fun LockIcon(modifier: Modifier = Modifier, color: Color = LifeGameInkTertiary) {
    Canvas(modifier.size(width = 10.dp, height = 12.dp)) {
        val stroke = 1.4.dp.toPx()
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.2f, stroke / 2),
            size = Size(size.width * 0.6f, size.height * 0.62f),
            cornerRadius = CornerRadius(size.width * 0.3f),
            style = Stroke(stroke),
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, size.height * 0.42f),
            size = Size(size.width, size.height * 0.58f),
            cornerRadius = CornerRadius(1.5.dp.toPx()),
        )
    }
}

/** 주 버튼 (잉크 바탕). */
@Composable
fun InkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(if (enabled) LifeGameInk else LifeGameRule)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) LifeGameCard else LifeGameInkTertiary,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

/** 보조 버튼 (종이 바탕 + 1px 선). */
@Composable
fun LineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, if (enabled) LifeGameInk else LifeGameRule, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) LifeGameInk else LifeGameInkTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}

/** 능력치 자세히 보기 시트. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(stats: Stats, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = LifeGamePaper,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .testTag("stats_content")
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("능력치", style = MaterialTheme.typography.headlineMedium)
            StatGrid(stats)
        }
    }
}

// 도트 캐릭터 그림이 들어오기 전까지 쓰는 자리 표시 실루엣 (X = 칠함)
private val ADULT_FIGURE = listOf(
    "..XXXX..", ".XXXXXX.", ".XXXXXX.", "..XXXX..", "...XX...", ".XXXXXX.", "XXXXXXXX", "XXXXXXXX",
    "X.XXXX.X", "X.XXXX.X", "..XXXX..", "..X..X..", "..X..X..", "..X..X..", "..X..X..", ".XX..XX.",
)
private val CHILD_FIGURE = listOf(
    "..XXXX..", ".XXXXXX.", ".XXXXXX.", "..XXXX..", ".XXXXXX.", "X.XXXX.X",
    "X.XXXX.X", "..XXXX..", "..X..X..", "..X..X..", "..X..X..", ".XX..XX.",
)

/**
 * 도트 캐릭터 자리. 실제 스프라이트(얼굴 시트 + 몸 부품)가 들어오면 이 자리를 바꾼다.
 * [grown]이 false면 한 시기 전의 작은 모습.
 */
@Composable
fun PixelFigurePlaceholder(
    height: Dp,
    modifier: Modifier = Modifier,
    grown: Boolean = true,
    color: Color = LifeGameInkTertiary.copy(alpha = 0.55f),
) {
    val pattern = if (grown) ADULT_FIGURE else CHILD_FIGURE
    val columns = pattern.first().length
    val cell = height / pattern.size
    Canvas(modifier.size(width = cell * columns, height = height)) {
        val px = size.height / pattern.size
        pattern.forEachIndexed { row, line ->
            line.forEachIndexed { column, mark ->
                if (mark == 'X') {
                    drawRect(color, topLeft = Offset(column * px, row * px), size = Size(px + 0.5f, px + 0.5f))
                }
            }
        }
    }
}

/** 어두운 주마등 · 끝 화면에서 쓰는 글자색 */
val LifePaperOnDark = Color(0xFFF6F4EF)
val LifeMutedOnDark = Color(0xFF8C867C)

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = text,
            color = LifeGameInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (trailing != null) {
            Text(trailing, color = LifeGameInkTertiary, fontSize = 11.5.sp)
        }
    }
}

@Composable
fun ThinRule(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(LifeGameRule))
}
