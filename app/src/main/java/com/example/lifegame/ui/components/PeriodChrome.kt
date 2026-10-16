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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.Dialog
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
import com.example.lifegame.ui.theme.LifeGameSpecial

/** 캐릭터 화면과 사건 화면에서 공통으로 보여 주는 0~100 건강 게이지. */
@Composable
fun LifeGauge(health: Int, modifier: Modifier = Modifier) {
    val value = health.coerceIn(0, 100)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("건강", color = LifeGameInkSecondary, fontSize = 11.5.sp, modifier = Modifier.weight(1f))
            Text("$value / 100", color = LifeGameStatDown, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LifeGameRule),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(value / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LifeGameStatDown),
            )
        }
    }
}

/** 시안 8 헤더: 플레이어 모습·생명력과 시기 진행 막대. 캐릭터를 누르면 능력치를 연다. */
@Composable
fun PeriodHeader(
    periodName: String,
    yearLabel: String,
    marks: List<PeriodMarkUi>,
    stats: Stats,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(shape)
            .background(LifeGamePaper.copy(alpha = 0.94f))
            .border(1.dp, LifeGameRule, shape)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PixelFigurePlaceholder(
                    height = 42.dp,
                    grown = periodName != "영아기",
                    modifier = Modifier
                        .clickable(role = Role.Button, onClick = onStatsClick)
                        .semantics { contentDescription = "플레이어 현재 모습, 능력치 보기" },
                )
                LifeGauge(health = stats.health, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
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
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(43.dp)
            .semantics {
                contentDescription = marks.firstOrNull { it.isCurrent }?.let { "${it.label} 시기" }.orEmpty()
            },
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(22.dp)
                .padding(horizontal = 10.dp),
        ) {
            if (marks.isEmpty()) return@Canvas
            val y = size.height / 2f
            val step = if (marks.size == 1) 0f else size.width / (marks.size - 1)
            drawLine(track, Offset(0f, y), Offset(size.width, y), strokeWidth = 2.dp.toPx())
            marks.forEachIndexed { index, mark ->
                val x = step * index
                val isComplete = mark.fill >= 1f || mark.isCurrent
                drawCircle(
                    color = if (isComplete) fill else LifeGamePaper,
                    radius = if (mark.isCurrent) 6.dp.toPx() else 5.dp.toPx(),
                    center = Offset(x, y),
                )
                drawCircle(
                    color = if (mark.isCurrent) LifeGameSpecial else track,
                    radius = if (mark.isCurrent) 6.dp.toPx() else 5.dp.toPx(),
                    center = Offset(x, y),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            marks.forEach { mark ->
                Text(
                    text = mark.label,
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
            .clip(RoundedCornerShape(10.dp))
            .background(LifeGameInk.copy(alpha = 0.62f))
            .border(1.dp, LifeGamePaper.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("focus_lock_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("이번 시기 초점", color = LifeGamePaper.copy(alpha = 0.86f), fontSize = 12.sp)
            Text(focus.label, color = LifeGamePaper, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
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
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (compact) 44.dp else 56.dp)
            .clip(shape)
            .background(if (enabled) LifeGameInk else LifeGameRule)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (compact) 10.dp else 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) LifeGameCard else LifeGameInkTertiary,
            fontSize = if (compact) 14.sp else 15.5.sp,
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

/** 능력치 자세히 보기 중앙 대화상자. */
@Composable
fun StatsSheet(stats: Stats, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .widthIn(max = 360.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(LifeGamePaper)
                .border(1.dp, LifeGameRule, RoundedCornerShape(18.dp))
                .verticalScroll(rememberScrollState())
                .testTag("stats_content")
                .padding(24.dp),
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
