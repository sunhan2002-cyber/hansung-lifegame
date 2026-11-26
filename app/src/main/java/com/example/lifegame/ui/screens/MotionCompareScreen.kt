package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.components.EntranceStyle
import com.example.lifegame.ui.components.HighlightStyle
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.MotionStyle
import com.example.lifegame.ui.components.MotionStyleStore
import com.example.lifegame.ui.components.PressStyle
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameSerif

/**
 * Debug-only playground: every variant of each lobby effect is shown live, side by side.
 * "적용" stores the choice so the real lobby (and every button in the app) uses it.
 * Stage 2 adds five collapsible groups (4–8). Each group's sections live in the file of the area that owns
 * them (LobbyDetailsCompare, CreationEndingCompare, PlayMotionCompare, TimeMotionCompare); this shell only
 * numbers and opens them. All groups start closed, at most one is open, and a group composes only while open.
 */
@Composable
fun MotionCompareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MotionStyleStore(context) }
    val applied = LocalMotionStyle.current
    fun apply(style: MotionStyle) = store.write(style)
    var openGroup by rememberSaveable { mutableStateOf<String?>(null) }
    fun toggle(group: String) { openGroup = if (openGroup == group) null else group }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().background(CompareNight)
            .verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("motion_compare"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("돌아가기", color = ComparePaper) }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = { store.reset() }, modifier = Modifier.testTag("motion_reset")) {
                Text("기본값으로", color = CompareGold)
            }
        }
        Text("연출 비교", color = ComparePaper, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Text(
            "버전마다 직접 눌러 보고, 마음에 드는 것을 '적용'하세요. 적용하면 실제 로비와 앱 전체 버튼이 그 버전으로 바뀌어요.",
            color = ComparePaper.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp,
        )
        AppliedSummary(applied)

        SectionTitle("1. 버튼 누르는 느낌", "버튼을 꾹 눌렀다가 떼 보세요.")
        PressStyle.entries.forEach { press ->
            VariantCard(press.label, press.detail, selected = applied.press == press,
                tag = "press_${press.name}", onApply = { apply(applied.copy(press = press)) }) {
                CompositionLocalProvider(LocalMotionStyle provides applied.copy(press = press)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InkButton("새 인생 살기", {}, containerColor = CompareGold, contentColor = CompareInk)
                        LineButton("지난 인생", {}, containerColor = CompareNight, contentColor = ComparePaper,
                            borderColor = ComparePaper.copy(alpha = 0.35f))
                    }
                }
            }
        }

        SectionTitle("2. 메인 버튼 강조", "로비의 '새 인생 살기 / 이어하기' 버튼에만 쓰여요.")
        HighlightStyle.entries.forEach { highlight ->
            VariantCard(highlight.label, highlight.detail, selected = applied.highlight == highlight,
                tag = "highlight_${highlight.name}", onApply = { apply(applied.copy(highlight = highlight)) }) {
                CompositionLocalProvider(LocalMotionStyle provides applied.copy(highlight = highlight)) {
                    Box(Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                        InkButton("이어하기", {}, containerColor = CompareGold, contentColor = CompareInk, highlight = true)
                    }
                }
            }
        }

        SectionTitle("3. 로비 등장", "버전을 고르면 아래 미리보기가 다시 재생돼요.")
        EntranceComparison(applied) { apply(applied.copy(entrance = it)) }

        CompareGroup("4. 로비 디테일", "아래 아이콘 · 새 칭호 점 · 시간대 하늘", "motion_group_lobby",
            expanded = openGroup == "motion_group_lobby", onToggle = { toggle("motion_group_lobby") }) {
            LobbyDetailsSections(applied) { apply(it) }
        }
        CompareGroup("5. 새 인생 만들기", "새 인생 화면 · 모습 바꾸기 · 주사위 숫자", "motion_group_creation",
            expanded = openGroup == "motion_group_creation", onToggle = { toggle("motion_group_creation") }) {
            CreationSections(applied) { apply(it) }
        }
        CompareGroup("6. 플레이 화면", "선택지 · 고른 카드 · 능력치 변화 · 큰 결과", "motion_group_play",
            expanded = openGroup == "motion_group_play", onToggle = { toggle("motion_group_play") }) {
            PlayMotionSections(applied) { apply(it) }
        }
        CompareGroup("7. 해 · 시기 전환", "해 도장 · 시기 넘김", "motion_group_time",
            expanded = openGroup == "motion_group_time", onToggle = { toggle("motion_group_time") }) {
            TimeMotionSections(applied) { apply(it) }
        }
        CompareGroup("8. 인생의 끝", "주마등 · 기록증 도장 · 칭호", "motion_group_ending",
            expanded = openGroup == "motion_group_ending", onToggle = { toggle("motion_group_ending") }) {
            CompareNote("칭호 팝업이 켜져 있으면 실제 앱에서는 기록증의 칭호 공개가 쉬고, 팝업은 기록증 도장이 끝난 뒤 떠요.")
            EndingSections(applied) { apply(it) }
            TitleUnlockSection(applied) { apply(it) }
        }

        LineButton("로비에서 처음 들어가는 느낌으로 보기", {
            LobbyIntroMemory.playedThisProcess = false
            onBack()
        }, Modifier.testTag("motion_view_lobby"), containerColor = CompareNight, contentColor = CompareGold, borderColor = CompareGold)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AppliedSummary(style: MotionStyle) {
    val shipped = style == MotionStyle.Default
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(ComparePanel).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(if (shipped) "지금 적용된 조합 · 기본값" else "지금 적용된 조합", color = CompareGold, fontSize = 12.sp)
        Text("누름 ${style.press.label} · 강조 ${style.highlight.label} · 등장 ${style.entrance.label}",
            color = ComparePaper, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.testTag("motion_applied"))
        AppliedLine("로비 · 아이콘 ${style.footerIcon.label} · 알림 점 ${style.newBadge.label} · 시간대 ${style.lobbyTime.label}",
            "motion_applied_lobby")
        AppliedLine("새 인생 · 등장 ${style.profileEntrance.label} · 모습 ${style.appearancePop.label} · 숫자 ${style.statCountUp.label}",
            "motion_applied_creation")
        AppliedLine("플레이 · 선택지 ${style.choiceReveal.label} · 고른 카드 ${style.choicePick.label} · " +
            "변화 ${style.statChange.label} · 큰 결과 ${style.bigResult.label}", "motion_applied_play")
        AppliedLine("해 ${style.ageStamp.label} · 시기 ${style.periodTransition.label}", "motion_applied_time")
        AppliedLine("끝 · 주마등 ${style.recapMoment.label} · 도장 ${style.certificateSeal.label} · " +
            "칭호 ${style.titleReveal.label} · 팝업 ${style.titleUnlock.label}", "motion_applied_ending")
    }
}

/** A stage-2 line of the summary; labels repeat across effects, so each line names its effects. */
@Composable
private fun AppliedLine(text: String, tag: String) {
    Text(text, color = ComparePaper.copy(alpha = 0.85f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.testTag(tag))
}

@Composable
private fun EntranceComparison(applied: MotionStyle, onApply: (EntranceStyle) -> Unit) {
    var previewing by remember { mutableStateOf(applied.entrance) }
    var quick by remember { mutableStateOf(false) }
    var replay by remember { mutableIntStateOf(0) }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComparePanel).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            EntranceStyle.entries.forEach { entrance ->
                val on = entrance == previewing
                Text(entrance.label, color = if (on) CompareInk else ComparePaper, fontSize = 12.sp,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) CompareGold else CompareNight)
                        .selectable(selected = on, role = Role.RadioButton) { previewing = entrance; replay++ }
                        .padding(vertical = 10.dp, horizontal = 6.dp).testTag("entrance_${entrance.name}"))
            }
        }
        Text(previewing.detail, color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp)
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(false to "처음 켰을 때", true to "다시 돌아올 때").forEach { (value, label) ->
                val on = quick == value
                Text(label, color = if (on) ComparePaper else ComparePaper.copy(alpha = 0.55f), fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .border(1.dp, ComparePaper.copy(alpha = if (on) 0.6f else 0.15f), RoundedCornerShape(8.dp))
                        .selectable(selected = on, role = Role.RadioButton) { quick = value; replay++ }
                        .padding(horizontal = 10.dp, vertical = 8.dp))
            }
            Spacer(Modifier.weight(1f))
            Text("▶ 다시 재생", color = CompareGold, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { replay++ }
                    .padding(horizontal = 8.dp, vertical = 8.dp).testTag("entrance_replay"))
        }
        val reduceMotion = rememberReduceMotion()
        val intro = LobbyIntroState(previewing, active = previewing != EntranceStyle.NONE && !reduceMotion, quick = quick, key = replay)
        MiniLobby(intro)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (applied.entrance == previewing) "이 버전이 적용돼 있어요" else "미리보는 중: ${previewing.label}",
                color = ComparePaper.copy(alpha = 0.75f), fontSize = 12.sp, modifier = Modifier.weight(1f))
            ApplyChip(applied.entrance == previewing, { onApply(previewing) }, "entrance_apply")
        }
    }
}
