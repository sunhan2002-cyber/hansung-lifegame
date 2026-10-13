package com.example.lifegame.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.DEFAULT_PLAYER_NAME
import com.example.lifegame.ui.Gender
import com.example.lifegame.ui.NewLifeProfile
import com.example.lifegame.ui.PLAYER_NAME_MAX_LENGTH
import com.example.lifegame.ui.StarterUi
import com.example.lifegame.ui.components.EffectText
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.SectionLabel
import com.example.lifegame.ui.nameCallOf
import com.example.lifegame.ui.normalizePlayerName
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif

/**
 * 프로필 만들기: 이름 · 성별 · 타고난 기질.
 * 성별은 입영 통지서(남자) 때문에 받는다 (R-70). 기질은 characters.sample.json의 시작 능력치 · 플래그다.
 * 아기 사진으로 얼굴 만들기(R-101)는 8주차 이후에 이 화면에 붙인다.
 */
@Composable
fun CharacterSelectScreen(
    starters: List<StarterUi>,
    onStart: (NewLifeProfile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var starterId by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    val previewName = normalizePlayerName(nameInput)
    val starterChosen = starters.isEmpty() || starterId != null
    val ready = gender != null && starterChosen

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding(),
    ) {
        // 키보드가 올라와도 시작 버튼이 가려지지 않도록 위쪽 내용만 스크롤한다.
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .testTag("profile_content"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "새 인생",
                color = LifeGameInk,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.semantics { heading() },
            )
            Text("태어날 아기를 정해 주세요.", color = LifeGameInkSecondary, fontSize = 14.sp)

            Spacer(Modifier.height(4.dp))
            SectionLabel("이름")
            OutlinedTextField(
                value = nameInput,
                onValueChange = { input ->
                    // 줄바꿈은 받지 않고, 최대 글자 수까지만 입력받는다.
                    nameInput = input.replace("\n", "").take(PLAYER_NAME_MAX_LENGTH)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("비워 두면 '$DEFAULT_PLAYER_NAME'") },
                supportingText = {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("부르는 이름: ${nameCallOf(previewName)}")
                        Text("${nameInput.length}/$PLAYER_NAME_MAX_LENGTH")
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = LifeGameCard,
                    unfocusedContainerColor = LifeGameCard,
                    focusedBorderColor = LifeGameInk,
                    unfocusedBorderColor = LifeGameRule,
                    cursorColor = LifeGameInk,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )

            SectionLabel("성별", trailing = "남자는 청년기에 입영 통지서를 받는다")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Gender.entries.forEach { option ->
                    SelectCard(
                        selected = gender == option,
                        onSelect = { gender = option },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(option.label, color = LifeGameInk, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            if (starters.isNotEmpty()) {
                SectionLabel("타고난 기질")
                starters.forEach { starter ->
                    SelectCard(
                        selected = starterId == starter.starterId,
                        onSelect = { starterId = starter.starterId },
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(starter.name, color = LifeGameInk, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
                            EffectText(starter.effects, showAmount = true)
                            Text(
                                text = starter.description,
                                color = LifeGameInkTertiary,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }

        InkButton(
            text = if (ready) "이 아기로 시작하기" else "성별과 기질을 골라 주세요",
            enabled = ready,
            onClick = {
                val chosen = gender ?: return@InkButton
                onStart(NewLifeProfile(name = previewName, gender = chosen, starterId = starterId))
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }
}

/** 라디오 동그라미가 있는 선택 카드. 고르면 테두리가 잉크색이 된다. */
@Composable
internal fun SelectCard(
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(LifeGameCard)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) LifeGameInk else LifeGameRule, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(18.dp)) {
            Canvas(Modifier.size(18.dp)) {
                val ring = if (selected) 5.dp.toPx() else 1.5.dp.toPx()
                drawCircle(
                    color = if (selected) LifeGameInk else LifeGameInkTertiary,
                    radius = (size.minDimension - ring) / 2,
                    style = Stroke(ring),
                )
            }
        }
        Box(Modifier.weight(1f)) { content() }
    }
}
