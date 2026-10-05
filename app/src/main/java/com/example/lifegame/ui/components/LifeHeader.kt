package com.example.lifegame.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.StatDelta
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.LIFELINE_MAX_AGE
import com.example.lifegame.ui.STAGE_START_AGES
import com.example.lifegame.ui.STAT_NAMES
import com.example.lifegame.ui.STAT_START
import com.example.lifegame.ui.displayName
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGameMuted
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatUp
import com.example.lifegame.ui.toDisplayMap
import kotlin.math.roundToInt

/**
 * 게임 화면 맨 위: 나이 · 단계, 능력치 미니 차트 버튼, 0~80세 인생선.
 * [changes]가 있으면 방금 바뀐 능력치 막대를 파랑(상승) · 빨강(하락)으로 칠한다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LifeHeader(
    age: Int,
    stage: LifeStage,
    stats: Stats,
    changes: StatDelta?,
    viewingHistory: Boolean,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row {
                Text(
                    text = "$age",
                    modifier = Modifier.alignByBaseline(),
                    color = LifeGameInk,
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "세",
                    modifier = Modifier.alignByBaseline().padding(start = 4.dp),
                    color = LifeGameInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stage.displayName(),
                    modifier = Modifier.alignByBaseline().padding(start = 12.dp),
                    color = LifeGameInkSecondary,
                    fontSize = 13.sp,
                )
            }
            if (viewingHistory) {
                Text(
                    text = "기록 보는 중",
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(LifeGameMuted)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    color = LifeGameInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            } else {
                StatsButton(stats = stats, changes = changes, onClick = onStatsClick)
            }
        }
        Lifeline(age = age, modifier = Modifier.fillMaxWidth().height((16.dp * LocalDensity.current.fontScale).coerceAtLeast(24.dp)))
    }
}

@Composable
private fun StatsButton(stats: Stats, changes: StatDelta?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("stats_button")
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .clickable(role = Role.Button, onClickLabel = "능력치 자세히 보기", onClick = onClick)
            .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        MiniChart(stats = stats, changes = changes)
        Text(text = "능력치", color = LifeGameInkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 능력치 7개를 막대 7개로. 50을 가운데(11dp)로 두고 4~22dp 사이에서 오르내린다. */
@Composable
private fun MiniChart(stats: Stats, changes: StatDelta?) {
    val values = stats.toDisplayMap()
    val deltas = changes?.toDisplayMap().orEmpty()
    Row(
        modifier = Modifier
            .height(22.dp)
            .drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(LifeGameInkTertiary, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            .padding(horizontal = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        STAT_NAMES.forEach { name ->
            val value = values[name] ?: STAT_START
            val barHeight by animateDpAsState((11 + (value - 50) * 0.6f).roundToInt().coerceIn(4, 22).dp, label = "bar")
            val delta = deltas[name] ?: 0
            val color by animateColorAsState(
                when {
                    delta > 0 -> LifeGameStatUp
                    delta < 0 -> LifeGameStatDown
                    else -> LifeGameInk
                },
                label = "barColor",
            )
            Box(
                Modifier
                    .width(4.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.dp))
                    .background(color),
            )
        }
    }
}

/** 0~80세 인생선. 단계가 바뀌는 나이에 눈금, 지금 나이에 점. */
@Composable
private fun Lifeline(age: Int, modifier: Modifier = Modifier) {
    val position by animateFloatAsState(age.coerceIn(0, LIFELINE_MAX_AGE) / LIFELINE_MAX_AGE.toFloat(), label = "lifeline")
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val trackY = 7.dp.toPx()
            val trackH = 2.dp.toPx()
            drawRect(LifeGameRule, Offset(0f, trackY), Size(size.width, trackH))
            drawRect(LifeGameInk, Offset(0f, trackY), Size(size.width * position, trackH))
            STAGE_START_AGES.forEach { start ->
                val x = size.width * start / LIFELINE_MAX_AGE
                drawRect(LifeGameInkTertiary, Offset(x, 4.dp.toPx()), Size(1.dp.toPx(), 8.dp.toPx()))
            }
            val dot = Offset(size.width * position, trackY + trackH / 2)
            drawCircle(LifeGamePaper, radius = 8.dp.toPx(), center = dot)
            drawCircle(LifeGameInk, radius = 5.dp.toPx(), center = dot)
        }
        Text(
            text = "$LIFELINE_MAX_AGE",
            modifier = Modifier.align(Alignment.TopEnd).background(LifeGamePaper),
            color = LifeGameInkTertiary,
            fontSize = 10.sp,
            lineHeight = 10.sp,
        )
    }
}

/** 필름 위 손잡이와 "지난 기록" 안내. */
@Composable
fun PullHint(recordCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier
                .size(width = 32.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(LifeGameRule),
        )
        Text(
            text = if (recordCount > 0) {
                "지난 기록 ${recordCount}개 · 아래로 당겨서 보기"
            } else {
                "고른 일은 여기 위에 기록으로 쌓여요"
            },
            color = LifeGameInkTertiary,
            fontSize = 11.5.sp,
        )
    }
}
