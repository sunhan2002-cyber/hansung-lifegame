/**
 * Area: time — its "연출 비교" sections (group "7. 해 · 시기 전환").
 *
 * Frozen contract — MotionCompareScreen (integrator) calls this; only its body changes:
 * - `@Composable internal fun TimeMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit)`.
 *
 * Both sections run the real effects on fake data (no ViewModel, no game random stream, no stored state):
 * - age_stamp: the real PeriodTrack under a demo LifeClock, over a decision-scene or an auto-progress backdrop. Each
 *   preview plays the change from the year shown before to the current one; replay plays it again.
 * - period_transition: the real PeriodTurnOverlay over a fake chapter cover in one PhoneFrame (for 노년기 a fake auto
 *   progress page whose real PeriodTrack then moves and stamps, as in the app). The cover's cards stay tappable during
 *   the turn: touches pass through.
 * The real PeriodTrack keeps its production tags, so each preview sits in `Box(testTag("<key>_preview"))`.
 * Tags: age_stamp_* and period_transition_* (variant chips, _replay, _apply, and the extra controls below).
 * No ages anywhere: the stamp shows the "N번째 해" label.
 */
package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.PeriodMarkUi
import com.example.lifegame.ui.PlayPhase
import com.example.lifegame.ui.components.AgeStampStyle
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LifeMark
import com.example.lifegame.ui.components.LocalLifeClock
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.PeriodTrack
import com.example.lifegame.ui.components.PeriodTransitionStyle
import com.example.lifegame.ui.components.PeriodTurnOverlay
import com.example.lifegame.ui.components.rememberLifeClockFor
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameNight
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif

/** Group 7 · 해 · 시기 전환 (ageStamp · periodTransition). */
@Composable
internal fun TimeMotionSections(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    LifeClockStampSection(applied, onApply)
    PeriodTurnSection(applied, onApply)
}

// ---------------------------------------------------------------------------------------------------------
// Fake lifeline: six periods of five years, as the content ships today
// ---------------------------------------------------------------------------------------------------------

private val LifeClockDemoPeriods = listOf("영아기", "유년기", "청소년기", "청년기", "중년기", "노년기")
private const val LIFE_CLOCK_DEMO_YEARS = 5
private const val LIFE_CLOCK_DEMO_STEPS = 30

/** The demo's place [step] (0 = 영아기 1번째 해 … 29 = 노년기 5번째 해) as a mark on [phase]. */
private fun lifeClockDemoMark(step: Int, phase: PlayPhase, speed: Int = 1): LifeMark {
    val period = (step / LIFE_CLOCK_DEMO_YEARS).coerceIn(0, LifeClockDemoPeriods.lastIndex)
    return LifeMark(
        runKey = "demo",
        periodIndex = period,
        year = step % LIFE_CLOCK_DEMO_YEARS + 1,
        periodName = LifeClockDemoPeriods[period],
        chapterLabel = "제${period + 1}장",
        phase = phase,
        speed = speed,
    )
}

/** The PeriodTrack marks with [current] as the current period (labels like PeriodDefinition.shortName()). */
private fun lifeClockDemoTrack(current: Int): List<PeriodMarkUi> = LifeClockDemoPeriods.mapIndexed { index, name ->
    PeriodMarkUi(label = name.removeSuffix("기"), fill = if (index < current) 1f else 0f, isCurrent = index == current)
}

// ---------------------------------------------------------------------------------------------------------
// 해가 바뀔 때 (age_stamp)
// ---------------------------------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LifeClockStampSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    // The change the preview plays: from the place shown before to the current one (no "from" = a silent first place).
    var fromStep by remember { mutableStateOf<Int?>(6) }
    var toStep by remember { mutableIntStateOf(7) }
    var onScene by remember { mutableStateOf(true) }
    EffectComparison(
        title = "해가 바뀔 때",
        hint = "시기선 위에 'N번째 해'가 찍혀요. 시기가 바뀌면 점이 먼저 다음 칸으로 옮겨 가요.",
        entries = AgeStampStyle.entries,
        applied = applied.ageStamp,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.AGE_STAMP_KEY,
        onApply = { onApply(applied.copy(ageStamp = it)) },
        controls = { replay ->
            ChoiceChips(
                options = listOf(true, false),
                selected = onScene,
                label = { if (it) "사건 화면 위" else "자동 진행 위" },
                tag = { if (it) "age_stamp_scene" else "age_stamp_paper" },
                onSelect = { onScene = it; replay() },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ReplayChip("age_stamp_year", label = "▶ 한 해 지나기", onReplay = {
                    val next = (toStep + 1) % LIFE_CLOCK_DEMO_STEPS
                    fromStep = toStep.takeIf { next != 0 }
                    toStep = next
                    replay()
                })
                ReplayChip("age_stamp_period", label = "▶ 다음 시기", onReplay = {
                    val next = (toStep / LIFE_CLOCK_DEMO_YEARS + 1) * LIFE_CLOCK_DEMO_YEARS % LIFE_CLOCK_DEMO_STEPS
                    fromStep = toStep.takeIf { next != 0 }
                    toStep = next
                    replay()
                })
                ReplayChip("age_stamp_first", label = "처음으로", onReplay = {
                    fromStep = null
                    toStep = 0
                    replay()
                })
            }
        },
    ) { previewing, _ ->
        CompositionLocalProvider(LocalMotionStyle provides applied.copy(ageStamp = previewing)) {
            Box(Modifier.testTag("age_stamp_preview")) { LifeClockStampDemo(fromStep, toStep, onScene) }
        }
    }
}

/** The real PeriodTrack at the bottom of a 260dp frame, playing fromStep → toStep on a demo clock (no period turn). */
@Composable
private fun LifeClockStampDemo(fromStep: Int?, toStep: Int, onScene: Boolean) {
    val phase = if (onScene) PlayPhase.DECISION else PlayPhase.AUTO
    val mark = lifeClockDemoMark(toStep, phase)
    val clock = rememberLifeClockFor(mark, seed = fromStep?.let { lifeClockDemoMark(it, phase) }, periodTurns = false)
    CompositionLocalProvider(LocalLifeClock provides clock) {
        Column(
            Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(12.dp))
                .background(if (onScene) LifeClockDemoScene else SolidColor(LifeGamePaper)),
        ) {
            Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onScene) {
                    Text("새 학기 첫날", color = Color.White, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text("교실 문 앞에서 잠깐 숨을 고른다.", color = Color.White.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 21.sp)
                } else {
                    Text("${mark.periodName} · 지난 기록", color = LifeGameInkSecondary, fontSize = 12.sp)
                    Text("· 친구와 처음으로 먼 동네까지 걸어가 보았다.", color = LifeGameInk, fontSize = 14.sp, lineHeight = 21.sp)
                    Text("· 받아쓰기에서 처음으로 백 점을 받았다.", color = LifeGameInk, fontSize = 14.sp, lineHeight = 21.sp)
                }
            }
            PeriodTrack(marks = lifeClockDemoTrack(mark.periodIndex), yearLabel = "${mark.year}번째 해")
        }
    }
}

/** A dark, blurred-scene stand-in for the decision screen behind the stamp. */
private val LifeClockDemoScene = Brush.verticalGradient(listOf(Color(0xFF34505C), Color(0xFF223640), LifeGameNight))

// ---------------------------------------------------------------------------------------------------------
// 시기가 바뀔 때 (period_transition)
// ---------------------------------------------------------------------------------------------------------

@Composable
private fun PeriodTurnSection(applied: MotionStyle, onApply: (MotionStyle) -> Unit) {
    // The period the turn lands in: 청소년기 · 청년기 · 중년기 get a chapter cover, 노년기 auto progress (index 5).
    var toPeriod by remember { mutableIntStateOf(2) }
    var speed by remember { mutableIntStateOf(1) }
    EffectComparison(
        title = "시기가 바뀔 때",
        hint = "지난 시기의 마지막 장이 넘어가며 새 장 표지가 드러나요. 노년기는 표지 없이 자동 진행으로 이어져요.",
        entries = PeriodTransitionStyle.entries,
        applied = applied.periodTransition,
        label = { it.label },
        detail = { it.detail },
        tagPrefix = MotionStyleStore.PERIOD_TRANSITION_KEY,
        onApply = { onApply(applied.copy(periodTransition = it)) },
        controls = { replay ->
            ReplayChip("period_transition_next", label = "▶ 다음 시기로", onReplay = {
                toPeriod = if (toPeriod >= LifeClockDemoPeriods.lastIndex) 2 else toPeriod + 1
                replay()
            })
            ChoiceChips(
                options = listOf(1, 2, 4),
                selected = speed,
                label = { "${it}×" },
                tag = { "period_transition_speed_$it" },
                onSelect = { speed = it; replay() },
            )
            CompareNote("배속은 노년기처럼 자동 진행으로 넘어갈 때만 영향을 줘요. 넘어가는 동안에도 아래 화면은 눌려요.")
        },
    ) { previewing, _ ->
        CompositionLocalProvider(LocalMotionStyle provides applied.copy(periodTransition = previewing)) {
            Box(Modifier.testTag("period_transition_preview")) { PeriodTurnDemo(toPeriod, speed) }
        }
    }
}

/** One PhoneFrame: the new screen with the real overlay on top, playing the turn from the previous period's last year. */
@Composable
private fun PeriodTurnDemo(toPeriod: Int, speed: Int) {
    val landsOnAuto = toPeriod == LifeClockDemoPeriods.lastIndex
    val start = toPeriod * LIFE_CLOCK_DEMO_YEARS
    val to = lifeClockDemoMark(start, if (landsOnAuto) PlayPhase.AUTO else PlayPhase.CHAPTER, speed)
    val clock = rememberLifeClockFor(to, seed = lifeClockDemoMark(start - 1, PlayPhase.AUTO, speed))
    PhoneFrame {
        CompositionLocalProvider(LocalLifeClock provides clock) {
            Box(Modifier.fillMaxSize()) {
                if (landsOnAuto) PeriodTurnDemoAuto(to) else PeriodTurnDemoCover(to)
                PeriodTurnOverlay(clock, Modifier.fillMaxSize())
            }
        }
    }
}

private val PeriodTurnDemoFocuses = mapOf(
    2 to listOf("학업" to "공부에 시간을 쓴다", "친구 · 동아리" to "함께 어울리며 사회성을 키운다"),
    3 to listOf("진로 준비" to "하고 싶은 일을 찾아 준비한다", "독립" to "혼자 서는 연습을 한다"),
    4 to listOf("일" to "쌓아 온 일을 단단히 다진다", "가족" to "가까운 사람과 시간을 보낸다"),
)

/** A stand-in for ChapterFocusScreen: the header where the old page's header was, two focus cards and the start bar. */
@Composable
private fun PeriodTurnDemoCover(mark: LifeMark) {
    var picked by remember { mutableStateOf<Int?>(null) }
    Column(Modifier.fillMaxSize().background(LifeGamePaper)) {
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(mark.chapterLabel, color = LifeGameInkSecondary, fontSize = 13.sp)
            Text(mark.periodName, color = LifeGameInk, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 42.sp)
            Text("새 시기가 시작됐어요. 이번 시기에 무엇에 마음을 쏟을지 골라 주세요.", color = LifeGameInkSecondary,
                fontFamily = LifeGameSerif, fontSize = 15.sp, lineHeight = 23.sp)
            Spacer(Modifier.height(4.dp))
            Text("이번 시기에 집중할 것", color = LifeGameInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            PeriodTurnDemoFocuses[mark.periodIndex].orEmpty().forEachIndexed { index, (label, description) ->
                val selected = picked == index
                Column(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(10.dp)).background(LifeGameCard)
                        .border(if (selected) 1.5.dp else 1.dp, if (selected) LifeGameInk else LifeGameRule, RoundedCornerShape(10.dp))
                        .selectable(selected = selected, role = Role.RadioButton) { picked = index }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("period_transition_card_$index"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(label, color = LifeGameInk, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                    Text(description, color = LifeGameInkSecondary, fontSize = 12.5.sp)
                }
            }
        }
        InkButton(
            text = if (picked != null) "${mark.periodName} 시작하기" else "초점을 골라 주세요",
            onClick = {},
            enabled = picked != null,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

/** A stand-in for 노년기's auto progress: a year log and the real PeriodTrack, which moves and stamps after the turn. */
@Composable
private fun PeriodTurnDemoAuto(mark: LifeMark) {
    Column(Modifier.fillMaxSize().background(LifeGamePaper)) {
        Box(
            Modifier.padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth().height(10.dp)
                .clip(RoundedCornerShape(5.dp)).background(Color(0xFF4E9A63)),
        )
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(mark.periodName, color = LifeGameInk, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            Text("· 오랜만에 옛 친구에게 편지를 썼다.", color = LifeGameInk, fontSize = 14.sp, lineHeight = 21.sp)
            Text("· 동네 도서관에서 지난 신문을 천천히 읽었다.", color = LifeGameInk, fontSize = 14.sp, lineHeight = 21.sp)
        }
        PeriodTrack(marks = lifeClockDemoTrack(mark.periodIndex), yearLabel = "${mark.year}번째 해")
        Text(
            "▶   1×  2×  4×   ·   다음 결정까지 ›",
            color = LifeGameInkSecondary,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth().background(LifeGamePaper).padding(horizontal = 20.dp, vertical = 14.dp),
        )
    }
}
