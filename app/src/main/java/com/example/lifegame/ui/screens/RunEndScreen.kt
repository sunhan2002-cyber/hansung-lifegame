package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.ChoiceRecordUi
import com.example.lifegame.ui.EndingSummaryUi
import com.example.lifegame.ui.FactLineUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.components.Achievements
import com.example.lifegame.ui.components.FactLines
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.PixelAssetImage
import com.example.lifegame.ui.components.SectionLabel
import com.example.lifegame.ui.components.ThinRule
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial

/**
 * 인생 요약 (8주차).
 * 결말 제목 또는 사망 문장 → 한 줄 인생 요약 → 마지막(시기 · 원인) → 교육 · 직업 → 가족 · 관계 · 주거 · 경제력
 * → 업적 → 많이 키운 능력치 → 주요 선택 → 지나온 해.
 * 기록이 없는 항목도 숨기지 않고 "…기록 없음" 같은 기본 문구로 보여 준다.
 */
@Composable
fun RunEndScreen(
    state: PeriodPlayUiState,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ending = state.ending
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("인생 요약", color = LifeGameInkSecondary, fontSize = 13.sp)
            PixelAssetImage(
                state.portraitImages, "${state.periodName} 마지막 모습",
                Modifier.fillMaxWidth().aspectRatio(4f / 3f).testTag("ending_portrait"),
            )
            Text(
                text = state.playerName,
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 38.sp,
                modifier = Modifier.semantics { heading() },
            )
            if (ending != null) {
                EndingBody(ending)
            } else {
                Text(
                    text = if (state.isDead) "${state.periodName}에서 인생을 마쳤다." else "${state.periodName}까지의 선택을 돌아본다.",
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontSize = 17.sp,
                )
            }

            ThinRule(Modifier.padding(vertical = 2.dp))
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

@Composable
private fun EndingBody(ending: EndingSummaryUi) {
    Text(
        text = ending.headline,
        color = if (ending.death?.early == true) LifeGameSpecial else LifeGameInk,
        fontFamily = LifeGameSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        modifier = Modifier.testTag("ending_headline"),
    )
    ending.endingSummary?.let {
        Text(it, color = LifeGameInkSecondary, fontFamily = LifeGameSerif, fontSize = 15.sp, lineHeight = 23.sp)
    }

    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(14.dp)
            .testTag("life_line"),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("한 줄 인생 요약", color = LifeGameInkTertiary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(ending.lifeLine, color = LifeGameInk, fontFamily = LifeGameSerif, fontSize = 15.5.sp, lineHeight = 24.sp)
        Text(ending.pathLine, color = LifeGameInkSecondary, fontSize = 12.5.sp, lineHeight = 18.sp)
    }

    SectionLabel("마지막")
    val death = ending.death
    FactLines(
        if (death != null) {
            listOf(
                FactLineUi("시기", "${death.periodName} ${death.yearInPeriod}번째 해", true),
                FactLineUi("원인", death.cause, true),
            )
        } else {
            listOf(FactLineUi("시기", "${ending.endedAt}까지 살았다", true))
        },
        modifier = Modifier.testTag("ending_death"),
    )
    death?.detail?.let {
        Text(it, color = LifeGameInkSecondary, fontFamily = LifeGameSerif, fontSize = 14.sp, lineHeight = 21.sp)
    }

    SectionLabel("교육 · 직업")
    FactLines(ending.educationCareer, modifier = Modifier.testTag("ending_education_career"))

    SectionLabel("가족 · 관계 · 주거 · 경제력")
    FactLines(ending.familyLife, modifier = Modifier.testTag("ending_family_life"))

    SectionLabel("주요 업적", trailing = ending.achievements.size.takeIf { it > 0 }?.let { "${it}개" })
    Achievements(ending.achievements, modifier = Modifier.testTag("ending_achievements"))

    SectionLabel("많이 키운 능력치")
    Text(
        text = ending.topStats.joinToString("  ·  ") { "${it.name} ${it.value}" },
        color = LifeGameInk,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
    )

    SectionLabel("주요 선택", trailing = ending.choices.size.takeIf { it > 0 }?.let { "${it}번" })
    if (ending.choices.isEmpty()) {
        Text("기록된 선택이 없다", color = LifeGameInkTertiary, fontSize = 14.sp)
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("ending_choices")) {
            ending.choices.forEach { ChoiceRecord(it) }
        }
    }
}

@Composable
private fun ChoiceRecord(record: ChoiceRecordUi) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = listOf(record.periodName, record.yearInPeriod.takeIf { it > 0 }?.let { "${it}번째 해" })
                .filterNot { it.isNullOrBlank() }.joinToString(" · "),
            color = LifeGameInkTertiary,
            fontSize = 11.sp,
        )
        Text(
            text = record.choiceLabel?.let { "${record.title} → $it" } ?: record.title,
            color = LifeGameInk,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}
