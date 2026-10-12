package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.SectionLabel
import com.example.lifegame.ui.components.ThinRule
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameSerif

/**
 * 인생이 끝났을 때의 임시 기록 화면.
 * 8주차에 주마등 → 예상 미래 모습 → 인생 기록 서류 + 칭호(R-81)로 바꾼다.
 */
@Composable
fun RunEndScreen(
    state: PeriodPlayUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                .testTag("run_end_content"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("인생 기록", color = LifeGameInkSecondary, fontSize = 13.sp)
            Text(
                text = state.playerName,
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = if (state.isDead) "${state.periodName}에 생을 마쳤다." else "${state.periodName}까지 살았다.",
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontSize = 17.sp,
            )
            if (state.isDead) {
                state.log.lastOrNull()?.let { last ->
                    Text(
                        text = last.text,
                        color = LifeGameInkSecondary,
                        fontFamily = LifeGameSerif,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                    )
                }
            }
            ThinRule(Modifier.padding(vertical = 4.dp))
            SectionLabel("지나온 해", trailing = "${state.log.size}줄")
            state.log.forEach { line ->
                Column(Modifier.fillMaxWidth()) {
                    Text(line.label, color = LifeGameInkTertiary, fontSize = 11.sp)
                    Text(
                        text = line.text,
                        color = LifeGameInk,
                        fontFamily = LifeGameSerif,
                        fontSize = 14.5.sp,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
        InkButton(
            text = "홈으로",
            onClick = onDone,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}
