package com.example.lifegame.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.LogLineUi
import com.example.lifegame.ui.PLAY_SPEEDS
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.YEAR_MILLIS_AT_1X
import com.example.lifegame.ui.components.FocusLockRow
import com.example.lifegame.ui.components.LifeGauge
import com.example.lifegame.ui.components.NOISE_ADVANCE_MILLIS
import com.example.lifegame.ui.components.PeriodHeader
import com.example.lifegame.ui.components.PixelFigurePlaceholder
import com.example.lifegame.ui.components.StatsSheet
import com.example.lifegame.ui.components.normalAdvance
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 시안 8-2 자동 진행. 결정할 일이 없는 해는 저절로 넘어간다 (R-20).
 * 필름 안에는 지금 초점을 하는 내 모습(3인칭 도트), 아래에는 한 해 한 줄 기록이 쌓인다.
 * 해가 바뀔 때마다 필름이 지지직거리며 한 칸 넘어간다 (R-23).
 */
@Composable
fun AutoProgressScreen(
    state: PeriodPlayUiState,
    onAdvanceYear: () -> Unit,
    onTogglePlaying: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onSkipToDecision: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var statsOpen by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()
    val advance by rememberUpdatedState(onAdvanceYear)

    val yearProgress = remember { Animatable(0f) }

    // 자동 진행 타이머. 해가 바뀌거나 배속 · 일시정지가 바뀌면 다시 센다.
    LaunchedEffect(state.yearKey, state.autoPlaying, state.speed) {
        yearProgress.snapTo(0f)
        if (!state.autoPlaying) return@LaunchedEffect
        yearProgress.animateTo(1f, tween((YEAR_MILLIS_AT_1X / state.speed).toInt(), easing = LinearEasing))
        advance()
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper),
    ) {
        // 도트 장면은 4:3. 작은 화면에서는 높이를 줄여 기록이 보이게 한다.
        val gateHeight = min((maxWidth - 20.dp) * 0.75f, maxHeight * 0.36f)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            PeriodHeader(
                periodName = state.periodName,
                yearLabel = "${state.yearInPeriod}번째 해",
                marks = state.periodMarks,
                stats = state.stats,
                onStatsClick = { statsOpen = true },
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .testTag("auto_progress_content"),
            ) {
                state.focus?.let { FocusLockRow(focus = it, years = state.periodYears) }
                CharacterMainPanel(state, onStatsClick = { statsOpen = true })
                YearTimer(progress = { yearProgress.value }, playing = state.autoPlaying)
                YearLog(lines = state.log.takeLast(3))
            }
            PlaybackBar(
                playing = state.autoPlaying,
                speed = state.speed,
                onTogglePlaying = onTogglePlaying,
                onSpeedChange = onSpeedChange,
                onSkipToDecision = onSkipToDecision,
            )
        }
    }

    if (statsOpen) StatsSheet(stats = state.stats, onDismiss = { statsOpen = false })
}

/** 사건이 없을 때 계속 유지되는 3인칭 캐릭터 메인 화면. */
@Composable
private fun CharacterMainPanel(state: PeriodPlayUiState, onStatsClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, RoundedCornerShape(14.dp))
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PixelFigurePlaceholder(
            height = 130.dp,
            grown = state.periodName != "영아기",
            color = LifeGameInk.copy(alpha = 0.72f),
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onStatsClick)
                .semantics { contentDescription = "플레이어 현재 모습, 능력치 보기" },
        )
        Text(state.playerName, color = LifeGameInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "${state.periodName} · 다음 사건을 기다리는 중", color = LifeGameInkSecondary, fontFamily = LifeGameSerif, fontSize = 13.sp)
    }
}

@Composable
private fun YearTimer(progress: () -> Float, playing: Boolean) {
    Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
        Canvas(Modifier.fillMaxWidth().height(2.dp)) {
            drawRect(LifeGameRule)
            drawRect(LifeGameInk, size = Size(size.width * progress(), size.height))
        }
        Text(
            text = if (playing) "다음 해까지" else "멈춤",
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
            color = LifeGameInkTertiary,
            fontSize = 11.sp,
        )
    }
}

/** 지난해는 흐리게, 올해는 크게. 맨 아래가 지금이다. */
@Composable
private fun YearLog(lines: List<LogLineUi>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
            .testTag("year_log"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lines.forEachIndexed { index, line ->
            val isNow = index == lines.lastIndex
            val fade = when (lines.size - 1 - index) {
                0 -> 1f
                1 -> 0.65f
                else -> 0.45f
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = line.label,
                    color = (if (isNow) LifeGameInk else LifeGameInkTertiary).copy(alpha = fade),
                    fontSize = 11.sp,
                    fontWeight = if (isNow) FontWeight.Bold else FontWeight.Normal,
                )
                Text(
                    text = line.text,
                    color = LifeGameInk.copy(alpha = fade),
                    fontFamily = LifeGameSerif,
                    fontSize = if (isNow) 16.5.sp else 14.5.sp,
                    lineHeight = if (isNow) 27.sp else 22.sp,
                )
            }
        }
    }
}

/** 일시정지 · 배속 1×/2×/4× · 다음 결정까지 (R-20). */
@Composable
private fun PlaybackBar(
    playing: Boolean,
    speed: Int,
    onTogglePlaying: () -> Unit,
    onSpeedChange: (Int) -> Unit,
    onSkipToDecision: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(LifeGamePaper)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(LifeGameRule))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(LifeGameInk)
                    .clickable(role = Role.Button, onClick = onTogglePlaying)
                    .semantics { contentDescription = if (playing) "일시정지" else "계속 진행" }
                    .testTag("play_pause"),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(16.dp)) {
                    if (playing) {
                        val bar = size.width * 0.28f
                        drawRect(LifeGameCard, topLeft = Offset(size.width * 0.12f, 0f), size = Size(bar, size.height))
                        drawRect(LifeGameCard, topLeft = Offset(size.width * 0.6f, 0f), size = Size(bar, size.height))
                    } else {
                        val path = Path().apply {
                            moveTo(size.width * 0.2f, 0f)
                            lineTo(size.width, size.height / 2)
                            lineTo(size.width * 0.2f, size.height)
                            close()
                        }
                        drawPath(path, LifeGameCard)
                    }
                }
            }
            SpeedToggle(speed = speed, onSpeedChange = onSpeedChange)
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = "다음 결정까지 ›",
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onSkipToDecision)
                        .padding(horizontal = 6.dp, vertical = 13.dp),
                    color = LifeGameInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SpeedToggle(speed: Int, onSpeedChange: (Int) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(3.dp)
            .testTag("speed_toggle"),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        PLAY_SPEEDS.forEach { option ->
            val selected = option == speed
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) LifeGameInk else LifeGameCard)
                    .selectable(selected = selected, role = Role.RadioButton) { onSpeedChange(option) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${option}×",
                    color = if (selected) LifeGameCard else LifeGameInkSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
