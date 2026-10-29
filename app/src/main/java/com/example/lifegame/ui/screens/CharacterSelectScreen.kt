package com.example.lifegame.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import com.example.lifegame.domain.model.AvatarConfig
import com.example.lifegame.domain.model.BodyType
import com.example.lifegame.domain.model.LifePeriod
import com.example.lifegame.ui.GenerationStatus
import com.example.lifegame.ui.GenerationUi
import com.example.lifegame.ui.avatar.AvatarLook
import com.example.lifegame.ui.avatar.AvatarOption
import com.example.lifegame.ui.avatar.AvatarOptions
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.components.LineButton
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
 * 프로필 만들기: 이름 · 성별 · 타고난 기질 · 모습(피부 · 머리 · 눈) · 사진(선택).
 * 성별은 입영 통지서(남자) 때문에 받는다 (R-70). 기질은 characters.sample.json의 시작 능력치 · 플래그다.
 * 사진은 선택 기능이다. 동의 · 취소 · 읽기 실패 · 생성 실패 어느 경우에도 기본 모습으로 바로 시작할 수 있다.
 * 사진은 기기 안 참조(URI)만 저장하고 복사 · 기록하지 않는다.
 */
@Composable
fun CharacterSelectScreen(
    starters: List<StarterUi>,
    onStart: (NewLifeProfile) -> Unit,
    modifier: Modifier = Modifier,
    faceSheet: GenerationUi? = null,
    onRequestFaceSheet: (photoRef: String, consent: Boolean) -> Unit = { _, _ -> },
    onCancelFaceSheet: () -> Unit = {},
    onClearFaceSheet: () -> Unit = {},
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var starterId by rememberSaveable { mutableStateOf<String?>(null) }
    var skinId by rememberSaveable { mutableStateOf(AvatarOptions.skins.first().id) }
    var hairId by rememberSaveable { mutableStateOf(AvatarOptions.hairs.first().id) }
    var eyesId by rememberSaveable { mutableStateOf(AvatarOptions.eyes.first().id) }
    var photoRef by rememberSaveable { mutableStateOf<String?>(null) }
    var photoMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var consentOpen by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sheet = faceSheet?.takeIf { it.key == photoRef }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            photoMessage = "사진 고르기를 취소했습니다. 기본 모습으로 진행합니다."
            return@rememberLauncherForActivityResult
        }
        val readable = runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
        if (!readable) {
            photoMessage = "사진을 읽지 못했습니다. 기본 모습으로 진행합니다."
            return@rememberLauncherForActivityResult
        }
        // 다시 켜도 같은 사진을 쓸 수 있으면 권한을 유지한다. 지원하지 않는 제공자면 이번 실행에서만 쓴다.
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        photoRef = uri.toString()
        photoMessage = null
        onRequestFaceSheet(uri.toString(), true)
    }

    BackHandler(enabled = sheet?.status == GenerationStatus.LOADING) { onCancelFaceSheet() }
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

            SectionLabel("모습", trailing = "사진 없이도 정할 수 있다")
            AppearancePreview(skinId = skinId, hairId = hairId, eyesId = eyesId)
            OptionChips("피부", AvatarOptions.skins, skinId) { skinId = it }
            OptionChips("머리", AvatarOptions.hairs, hairId) { hairId = it }
            OptionChips("눈", AvatarOptions.eyes, eyesId) { eyesId = it }

            SectionLabel("사진으로 얼굴 만들기", trailing = "선택")
            PhotoSection(
                photoRef = photoRef,
                sheet = sheet,
                message = photoMessage,
                onPick = { consentOpen = true },
                onCancel = onCancelFaceSheet,
                onRetry = { photoRef?.let { onRequestFaceSheet(it, true) } },
                onRemove = {
                    photoRef?.let { ref ->
                        runCatching {
                            context.contentResolver.releasePersistableUriPermission(
                                Uri.parse(ref),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION,
                            )
                        }
                    }
                    photoRef = null
                    onClearFaceSheet()
                    photoMessage = "사진 참조를 지웠습니다. 기본 모습으로 진행합니다."
                },
            )
        }

        InkButton(
            text = if (ready) "이 아기로 시작하기" else "성별과 기질을 골라 주세요",
            enabled = ready,
            onClick = {
                val chosen = gender ?: return@InkButton
                if (sheet?.status == GenerationStatus.LOADING) onCancelFaceSheet()
                val readySheet = sheet?.takeIf { it.status == GenerationStatus.READY }?.value
                onStart(
                    NewLifeProfile(
                        name = previewName,
                        gender = chosen,
                        starterId = starterId,
                        avatar = AvatarConfig(
                            skinToneId = skinId,
                            hairId = hairId,
                            eyesId = eyesId,
                            portraitRef = photoRef,
                            faceSheetRef = readySheet,
                            photoConsent = photoRef != null,
                        ),
                    ),
                )
            },
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        )
    }

    if (consentOpen) {
        AlertDialog(
            onDismissRequest = { consentOpen = false },
            containerColor = LifeGamePaper,
            title = { Text("사진 사용에 동의할까요?", fontFamily = LifeGameSerif, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "고른 사진은 기기 밖으로 보내지 않고 참조만 저장합니다. 얼굴 시트 생성 요청에만 쓰며, 지금은 생성 서버가 연결되지 않아 예시 응답을 받습니다. " +
                        "'사진 지우기'로 언제든 참조를 지울 수 있습니다.",
                    color = LifeGameInkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    consentOpen = false
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.testTag("photo_consent_yes")) { Text("동의하고 사진 고르기", color = LifeGameInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    consentOpen = false
                    photoMessage = "동의하지 않아 기본 모습으로 진행합니다."
                }, modifier = Modifier.testTag("photo_consent_no")) { Text("동의하지 않음", color = LifeGameInkSecondary) }
            },
        )
    }
}

@Composable
private fun AppearancePreview(skinId: String, hairId: String, eyesId: String) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(12.dp)
            .testTag("appearance_preview"),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(LifePeriod.INFANT to "지금", LifePeriod.TEEN to "청소년기", LifePeriod.YOUNG_ADULT to "청년기").forEach { (period, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PlayerAvatar(
                    look = AvatarLook(skinId, hairId, eyesId, period, BodyType.STANDARD),
                    modifier = Modifier.size(width = 60.dp, height = 96.dp),
                )
                Text(label, color = LifeGameInkTertiary, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    label: String,
    options: List<AvatarOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = LifeGameInkSecondary, fontSize = 14.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val selected = option.id == selectedId
                Text(
                    text = option.label,
                    color = if (selected) LifeGameCard else LifeGameInk,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) LifeGameInk else LifeGameCard)
                        .border(1.dp, if (selected) LifeGameInk else LifeGameRule, RoundedCornerShape(10.dp))
                        .selectable(selected = selected, role = Role.RadioButton) { onSelect(option.id) }
                        .padding(horizontal = 14.dp, vertical = 13.dp)
                        .testTag("option_${option.id}"),
                )
            }
        }
    }
}

@Composable
private fun PhotoSection(
    photoRef: String?,
    sheet: GenerationUi?,
    message: String?,
    onPick: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().testTag("photo_section"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "아기 사진을 고르면 그 얼굴로 캐릭터 얼굴 시트를 만들도록 요청한다. 사진이 없어도 위에서 고른 모습으로 똑같이 시작한다.",
            color = LifeGameInkSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        if (photoRef == null) {
            LineButton(text = "사진 고르기", onClick = onPick, modifier = Modifier.testTag("photo_pick"))
        } else {
            Text("사진 1장 선택됨 (기기 안 참조만 저장)", color = LifeGameInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            when (sheet?.status) {
                GenerationStatus.LOADING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = LifeGameInk, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Text("얼굴 시트를 만드는 중", color = LifeGameInkSecondary, fontSize = 14.sp, modifier = Modifier.weight(1f).testTag("face_loading"))
                    TextButton(onClick = onCancel) { Text("취소", color = LifeGameInk, fontWeight = FontWeight.Bold) }
                }
                GenerationStatus.FALLBACK -> TextButton(onClick = onRetry) { Text("다시 시도", color = LifeGameInk, fontWeight = FontWeight.Bold) }
                else -> Unit
            }
            LineButton(text = "사진 지우기", onClick = onRemove, modifier = Modifier.testTag("photo_remove"))
        }
        val note = sheet?.message ?: message
        if (note != null) {
            Text(note, color = LifeGameInkTertiary, fontSize = 13.5.sp, lineHeight = 19.sp, modifier = Modifier.testTag("photo_message"))
        }
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
