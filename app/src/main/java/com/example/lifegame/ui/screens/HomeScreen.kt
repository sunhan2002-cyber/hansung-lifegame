package com.example.lifegame.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.BuildConfig
import com.example.lifegame.ui.FakeScenario
import com.example.lifegame.ui.FinishedLifeUi
import com.example.lifegame.ui.LifeCardUi
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.components.ExperienceSettingsDialog
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.ThinRule
import com.example.lifegame.ui.components.rememberAmbientPhase
import com.example.lifegame.ui.components.experiencePress
import com.example.lifegame.ui.components.rememberExperienceClick
import com.example.lifegame.ui.theme.*

private val LobbyGold = Color(0xFFE9CD94)
private val LobbyNight = Color(0xFF15242C)
private val LobbyPaper = Color(0xFFF6F1E5)

/** Artwork fills the viewport; the reserved scene area keeps the avatar clear of the text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    lifeCard: LifeCardUi?,
    finishedLives: List<FinishedLifeUi>,
    onContinue: () -> Unit,
    onNewLife: () -> Unit,
    modifier: Modifier = Modifier,
    onDesignPreview: (() -> Unit)? = null,
    message: String? = null,
    onOpenRecord: (runId: String) -> Unit = {},
    onOpenTitles: () -> Unit = {},
    generationScenario: FakeScenario? = null,
    onScenarioChange: (FakeScenario) -> Unit = {},
    latestRecord: LifeRecordUi? = null,
    hasRecoverableSave: Boolean = false,
) {
    HomeSystemBarAppearance()
    var confirmOverwrite by rememberSaveable { mutableStateOf(false) }
    var showPastLives by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val scene = rememberHomeScene(lifeCard, latestRecord)
    val phase = rememberAmbientPhase(enabled = !confirmOverwrite && !showPastLives && !showSettings)
    val startNew = { if (lifeCard != null || hasRecoverableSave) confirmOverwrite = true else onNewLife() }

    BoxWithConstraints(modifier.fillMaxSize().background(LobbyNight).testTag("home_screen")) {
        HomeSceneBackground(scene, phase, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to LobbyNight.copy(alpha = 0.64f), 0.22f to LobbyNight.copy(alpha = 0.12f),
            0.56f to LobbyNight.copy(alpha = 0.12f), 0.78f to LobbyNight.copy(alpha = 0.82f), 1f to LobbyNight,
        )))
        val largeText = LocalDensity.current.fontScale > 1.5f
        val sceneHeight = (maxHeight * if (largeText) 0.30f else 0.34f).coerceIn(176.dp, 340.dp)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp).testTag("home_content"),
        ) {
            Text("선택으로 만드는 한 사람의 일생", color = LobbyPaper.copy(alpha = 0.9f), fontSize = 12.sp, letterSpacing = 1.sp)
            Text("인생게임", color = LobbyPaper, fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold,
                fontSize = 36.sp, letterSpacing = 3.sp, modifier = Modifier.semantics { heading() })
            HomeSceneAvatar(scene, phase, Modifier.fillMaxWidth().height(sceneHeight))
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(
                    LobbyNight.copy(alpha = 0.24f), LobbyNight.copy(alpha = 0.94f),
                )), RoundedCornerShape(16.dp)).padding(top = 12.dp, bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when {
                    lifeCard != null -> Column(Modifier.testTag("life_card"), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        LobbyEyebrow("지금 사는 인생 · ${lifeCard.place}")
                        LobbyHeading(lifeCard.playerName)
                        lifeCard.focusLabel?.let { LobbyDescription("초점 · $it") }
                        lifeCard.lastLine?.let { LobbyDescription(it) }
                    }
                    latestRecord != null -> {
                        LobbyEyebrow("지난 인생에서 이어지는 한 장면")
                        LobbyHeading(latestRecord.title?.text ?: latestRecord.pathTitle)
                        LobbyDescription("${latestRecord.playerName} · ${scene.moment?.place ?: latestRecord.finalPeriodName}")
                        scene.moment?.let { LobbyDescription(it.title) }
                    }
                    else -> {
                        LobbyEyebrow("아기부터 노년까지")
                        LobbyHeading("이번엔, 어떤 삶을 살게 될까?")
                        LobbyDescription("작은 선택 하나로 달라지는 나의 이야기.")
                    }
                }
                if (message != null) Text(message, color = LobbyPaper, fontSize = 13.sp, lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth().background(LobbyNight.copy(alpha = 0.86f), RoundedCornerShape(8.dp))
                        .padding(10.dp).testTag("home_message"))
                if (lifeCard == null && hasRecoverableSave) LobbyDescription("불러오지 못한 저장이 있어요. 새로 시작하기 전에 확인해 주세요.")
                Spacer(Modifier.height(2.dp))
                InkButton(if (lifeCard != null) "이어하기" else "새 인생 살기", if (lifeCard != null) onContinue else startNew,
                    Modifier.testTag(if (lifeCard != null) "home_continue" else "home_new_life"),
                    containerColor = LobbyGold, contentColor = Color(0xFF253631))
                if (lifeCard != null) LineButton("새 인생 살기", startNew, Modifier.testTag("home_new_life"),
                    containerColor = LobbyNight.copy(alpha = 0.55f), contentColor = LobbyPaper, borderColor = LobbyPaper.copy(alpha = 0.35f))
                if (largeText) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        LobbyFooter("지난 인생", "home_past_lives", Modifier.fillMaxWidth()) { showPastLives = true }
                        LobbyFooter("칭호 수집", "home_titles", Modifier.fillMaxWidth(), onOpenTitles)
                        LobbyFooter("설정", "home_settings", Modifier.fillMaxWidth()) { showSettings = true }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        LobbyFooter("지난 인생", "home_past_lives", Modifier.weight(1f)) { showPastLives = true }
                        LobbyFooter("칭호 수집", "home_titles", Modifier.weight(1f), onOpenTitles)
                        LobbyFooter("설정", "home_settings", Modifier.weight(1f)) { showSettings = true }
                    }
                }
                if (BuildConfig.DEBUG) {
                    onDesignPreview?.let { openPreview ->
                        LineButton("디자인 미리보기", openPreview, Modifier.testTag("home_design_preview"),
                            containerColor = LobbyNight, contentColor = LobbyPaper, borderColor = LobbyPaper.copy(alpha = 0.25f))
                    }
                    if (generationScenario != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.testTag("debug_generation")) {
                            Text("개발용 생성 응답 (Fake)", color = LobbyPaper.copy(alpha = 0.65f), fontSize = 12.sp)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FakeScenario.entries.forEach { option ->
                                    val selected = option == generationScenario
                                    Text(option.label, color = if (selected) LobbyNight else LobbyPaper, fontSize = 13.sp,
                                        modifier = Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) LobbyGold else LobbyNight)
                                            .border(1.dp, LobbyPaper.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .selectable(selected, role = Role.RadioButton) { onScenarioChange(option) }
                                            .padding(horizontal = 10.dp, vertical = 12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showPastLives) PastLivesDialog(finishedLives, onDismiss = { showPastLives = false }) { runId -> showPastLives = false; onOpenRecord(runId) }
    if (showSettings) ExperienceSettingsDialog { showSettings = false }
    if (confirmOverwrite) AlertDialog(
        onDismissRequest = { confirmOverwrite = false }, modifier = Modifier.testTag("home_overwrite_confirmation"), containerColor = LifeGamePaper,
        title = { Text("새 인생을 시작할까요?", fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold) },
        text = { Text(if (lifeCard != null) "지금 사는 인생은 이어서 할 수 없게 됩니다."
            else "불러오지 못한 저장 대신 새 인생을 시작합니다. 손상된 원본은 따로 보관됩니다.", color = LifeGameInkSecondary) },
        confirmButton = { TextButton(onClick = { confirmOverwrite = false; onNewLife() }, modifier = Modifier.testTag("home_confirm_new_life")) {
            Text("새로 시작", color = LifeGameInk, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { confirmOverwrite = false }) { Text("취소", color = LifeGameInkSecondary) } },
    )
}

@Composable private fun LobbyEyebrow(text: String) { Text(text, color = LobbyGold, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
@Composable private fun LobbyHeading(text: String) {
    Text(text, color = LobbyPaper, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 29.sp, modifier = Modifier.semantics { heading() })
}
@Composable private fun LobbyDescription(text: String) { Text(text, color = LobbyPaper.copy(alpha = 0.86f), fontSize = 13.sp, lineHeight = 20.sp) }

@Composable private fun LobbyFooter(label: String, tag: String, modifier: Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val click = rememberExperienceClick(onClick)
    Box(modifier.heightIn(min = 48.dp).experiencePress(interactions).clip(RoundedCornerShape(8.dp))
        .clickable(interactionSource = interactions, indication = LocalIndication.current, role = Role.Button, onClick = click)
        .padding(horizontal = 4.dp, vertical = 12.dp).testTag(tag), contentAlignment = Alignment.Center) {
        Text(label, color = LobbyPaper.copy(alpha = 0.9f), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable private fun PastLivesDialog(lives: List<FinishedLifeUi>, onDismiss: () -> Unit, onOpenRecord: (String) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, modifier = Modifier.testTag("home_past_lives_dialog"), containerColor = LifeGamePaper,
        title = { Text("지난 인생", fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold) },
        text = {
            if (lives.isEmpty()) Text("아직 끝까지 산 인생이 없다.", color = LifeGameInkTertiary)
            else Column(Modifier.verticalScroll(rememberScrollState()).testTag("finished_lives"), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                lives.forEachIndexed { index, life ->
                    if (index > 0) ThinRule()
                    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button) { onOpenRecord(life.key) }.padding(vertical = 12.dp, horizontal = 4.dp).testTag("finished_life_$index"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(life.playerName, color = LifeGameInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        life.titleText?.let { Text(it, color = LifeGameSpecial, fontSize = 14.sp) }
                        Text(life.summary, color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 21.sp)
                        life.endedAt?.let { Text(it, color = LifeGameInkTertiary, fontSize = 12.sp) }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}
