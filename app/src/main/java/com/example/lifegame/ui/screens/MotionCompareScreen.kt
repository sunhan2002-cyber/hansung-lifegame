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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameSerif

private val CompareNight = Color(0xFF15242C)
private val ComparePanel = Color(0xFF1E323C)
private val CompareGold = Color(0xFFE9CD94)
private val ComparePaper = Color(0xFFF6F1E5)
private val CompareInk = Color(0xFF253631)

/**
 * Debug-only playground: every variant of each lobby effect is shown live, side by side.
 * "적용" stores the choice so the real lobby (and every button in the app) uses it.
 */
@Composable
fun MotionCompareScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember(context) { MotionStyleStore(context) }
    val applied = LocalMotionStyle.current
    fun apply(style: MotionStyle) = store.write(style)

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
    }
}

@Composable
private fun SectionTitle(title: String, hint: String) {
    Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = ComparePaper, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(hint, color = ComparePaper.copy(alpha = 0.65f), fontSize = 12.sp)
    }
}

@Composable
private fun VariantCard(
    label: String,
    detail: String,
    selected: Boolean,
    tag: String,
    onApply: () -> Unit,
    sample: @Composable () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ComparePanel)
            .border(if (selected) 2.dp else 1.dp, if (selected) CompareGold else ComparePaper.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(14.dp).testTag(tag),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, color = ComparePaper, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(detail, color = ComparePaper.copy(alpha = 0.7f), fontSize = 12.sp, lineHeight = 17.sp)
            }
            ApplyChip(selected, onApply, "${tag}_apply")
        }
        sample()
    }
}

@Composable
private fun ApplyChip(selected: Boolean, onApply: () -> Unit, tag: String) {
    Text(
        if (selected) "적용됨" else "적용",
        color = if (selected) CompareInk else CompareGold,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (selected) CompareGold else Color.Transparent)
            .border(1.dp, CompareGold, RoundedCornerShape(50))
            .clickable(enabled = !selected, role = Role.Button, onClick = onApply)
            .padding(horizontal = 14.dp, vertical = 8.dp).testTag(tag),
    )
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

private val MiniTitleStyle = TextStyle(color = ComparePaper, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold,
    fontSize = 28.sp, letterSpacing = 2.sp)

/** A scaled-down lobby that replays the chosen entrance with the same building blocks as the real one. */
@Composable
private fun MiniLobby(intro: LobbyIntroState) {
    val scene = rememberHomeScene(null, null)
    val phase = rememberAmbientPhase()
    Box(Modifier.fillMaxWidth().height(400.dp).clip(RoundedCornerShape(12.dp)).clipToBounds().background(CompareNight)) {
        HomeSceneBackground(scene, phase, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to CompareNight.copy(alpha = 0.6f), 0.5f to CompareNight.copy(alpha = 0.1f), 0.8f to CompareNight.copy(alpha = 0.85f), 1f to CompareNight,
        )))
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("선택으로 만드는 한 사람의 일생", color = ComparePaper.copy(alpha = 0.9f), fontSize = 11.sp,
                modifier = Modifier.lobbyEntrance(intro, 0))
            StampedTitle("인생게임", MiniTitleStyle, CompareGold, intro, Modifier.lobbyEntrance(intro, LobbyIntroState.TITLE_INDEX))
            HomeSceneAvatar(scene, phase, Modifier.fillMaxWidth().weight(1f).lobbyEntrance(intro, 2, groupOpacity = true))
            Column(Modifier.lobbyEntrance(intro, 3), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("아기부터 노년까지", color = CompareGold, fontSize = 11.sp)
                Text("이번엔, 어떤 삶을 살게 될까?", color = ComparePaper, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            Box(Modifier.lobbyEntrance(intro, 4, rise = false)) {
                InkButton("새 인생 살기", {}, compact = true, containerColor = CompareGold, contentColor = CompareInk, highlight = true)
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp).lobbyEntrance(intro, 6, rise = false), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("지난 인생", "칭호 수집", "설정").forEach {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(4.dp).clip(CircleShape).background(ComparePaper.copy(alpha = 0.4f)))
                        Spacer(Modifier.width(5.dp))
                        Text(it, color = ComparePaper.copy(alpha = 0.85f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
