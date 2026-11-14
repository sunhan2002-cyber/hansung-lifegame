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
import com.example.lifegame.ui.avatar.PhotoFaceSheet
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
import com.example.lifegame.ui.StartingDice
import com.example.lifegame.ui.StartingRoll
import com.example.lifegame.ui.StatRoll
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatUp
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
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
import com.example.lifegame.domain.model.PhotoFlowStatus

/**
 * 프로필 만들기: 이름 · 성별 · 주사위로 정하는 타고난 능력치 · 모습(피부 · 머리 · 눈) · 사진(선택).
 * 성별은 입영 통지서(남자) 때문에 받는다 (R-70).
 * 능력치는 고르지 않고 주사위로 정한다 (2026-11-05 피드백 1번). 굴린 값과 가장 닮은 성향(characters.sample.json)의 플래그가 함께 들어간다.
 * 사진은 선택 기능이다. 동의 · 취소 · 읽기 실패 · 생성 실패 어느 경우에도 기본 모습으로 바로 시작할 수 있다.
 * 사진은 기기 안 참조(URI)만 저장하고 복사 · 기록하지 않는다.
 * [aiFace]면 동의한 사진을 OpenAI로 보내 시기별 얼굴 시트를 받는다(키가 있는 빌드만). 아니면 Fake 응답이다.
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
    onPhotoFlowChanged: (PhotoFlowStatus, String?) -> Unit = { _, _ -> },
    aiFace: Boolean = false,
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var rollRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var rerollsLeft by rememberSaveable { mutableStateOf(StartingDice.REROLLS) }
    var rollNonce by remember { mutableStateOf(0) }
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
            onPhotoFlowChanged(PhotoFlowStatus.FALLBACK, photoMessage)
            return@rememberLauncherForActivityResult
        }
        val readable = runCatching {
            context.contentResolver.openInputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
        if (!readable) {
            photoMessage = "사진을 읽지 못했습니다. 기본 모습으로 진행합니다."
            onPhotoFlowChanged(PhotoFlowStatus.FALLBACK, photoMessage)
            return@rememberLauncherForActivityResult
        }
        // 다시 켜도 같은 사진을 쓸 수 있으면 권한을 유지한다. 지원하지 않는 제공자면 이번 실행에서만 쓴다.
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        photoRef = uri.toString()
        photoMessage = null
        onPhotoFlowChanged(PhotoFlowStatus.PROCESSING, null)
        onRequestFaceSheet(uri.toString(), true)
    }

    LaunchedEffect(sheet?.status, sheet?.message) {
        when (sheet?.status) {
            GenerationStatus.LOADING -> onPhotoFlowChanged(PhotoFlowStatus.PROCESSING, null)
            GenerationStatus.READY -> onPhotoFlowChanged(PhotoFlowStatus.READY, null)
            GenerationStatus.FALLBACK -> onPhotoFlowChanged(PhotoFlowStatus.FALLBACK, sheet.message)
            else -> Unit
        }
    }

    BackHandler(enabled = sheet?.status == GenerationStatus.LOADING) { onCancelFaceSheet() }
    val previewName = normalizePlayerName(nameInput)
    val roll = StartingRoll.decode(rollRaw)
    val tendency = roll?.let { StartingDice.tendencyOf(it.delta, starters) }
    val ready = gender != null && roll != null

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
            Text("이름과 성별을 정하고, 타고난 능력치는 주사위로 정한다.", color = LifeGameInkSecondary, fontSize = 14.sp)

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

            SectionLabel("타고난 능력치", trailing = "주사위로 정한다")
            DicePanel(
                roll = roll,
                rollNonce = rollNonce,
                tendency = tendency,
                rerollsLeft = rerollsLeft,
                onRoll = {
                    if (roll != null) rerollsLeft -= 1
                    rollRaw = StartingDice.roll().encode()
                    rollNonce += 1
                },
            )

            SectionLabel("모습", trailing = "사진 없이도 정할 수 있다")
            AppearancePreview(
                skinId = skinId,
                hairId = hairId,
                eyesId = eyesId,
                photoFaceSheet = PhotoFaceSheet.pathOf(sheet?.takeIf { it.status == GenerationStatus.READY }?.value),
            )
            OptionChips("피부", AvatarOptions.skins, skinId) { skinId = it }
            OptionChips("머리", AvatarOptions.hairs, hairId) { hairId = it }
            OptionChips("눈", AvatarOptions.eyes, eyesId) { eyesId = it }

            SectionLabel("사진으로 얼굴 만들기", trailing = "선택")
            PhotoSection(
                aiFace = aiFace,
                photoRef = photoRef,
                sheet = sheet,
                message = photoMessage,
                onPick = {
                    consentOpen = true
                    onPhotoFlowChanged(PhotoFlowStatus.CONSENT_REQUIRED, null)
                },
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
                    onPhotoFlowChanged(PhotoFlowStatus.NOT_REQUESTED, null)
                },
            )
        }

        InkButton(
            text = when {
                ready -> "이 인생 시작하기"
                gender == null && roll == null -> "성별을 고르고 주사위를 굴려 주세요"
                gender == null -> "성별을 골라 주세요"
                else -> "주사위를 굴려 주세요"
            },
            enabled = ready,
            onClick = {
                val chosen = gender ?: return@InkButton
                val rolled = roll ?: return@InkButton
                if (sheet?.status == GenerationStatus.LOADING) onCancelFaceSheet()
                val readySheet = sheet?.takeIf { it.status == GenerationStatus.READY }?.value
                onStart(
                    NewLifeProfile(
                        name = previewName,
                        gender = chosen,
                        starterId = tendency?.starterId,
                        rolledDelta = rolled.delta,
                        startingRollRaw = rolled.encode(),
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
                    if (aiFace) {
                        "고른 사진을 OpenAI 이미지 생성 서버로 보내, 이 게임 화풍의 도트 얼굴(영아기부터 노년기까지 6장)을 만듭니다. " +
                            "보내는 것은 긴 변 1024px로 줄인 사본 한 장이고, 앱에는 만들어진 얼굴 그림만 저장합니다. " +
                            "보낸 사진은 OpenAI의 데이터 처리 정책에 따라 처리됩니다. '사진 지우기'로 언제든 지울 수 있습니다."
                    } else {
                        "고른 사진은 기기 밖으로 보내지 않고 참조만 저장합니다. 얼굴 시트 생성 요청에만 쓰며, 이 빌드에는 생성 서버 키가 없어 예시 응답을 받습니다. " +
                            "'사진 지우기'로 언제든 참조를 지울 수 있습니다."
                    },
                    color = LifeGameInkSecondary,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    consentOpen = false
                    onPhotoFlowChanged(PhotoFlowStatus.PERMISSION_REQUIRED, null)
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }, modifier = Modifier.testTag("photo_consent_yes")) { Text("동의하고 사진 고르기", color = LifeGameInk, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = {
                    consentOpen = false
                    photoMessage = "동의하지 않아 기본 모습으로 진행합니다."
                    onPhotoFlowChanged(PhotoFlowStatus.FALLBACK, photoMessage)
                }, modifier = Modifier.testTag("photo_consent_no")) { Text("동의하지 않음", color = LifeGameInkSecondary) }
            },
        )
    }
}

/**
 * 능력치마다 주사위 두 개. 굴리는 동안 눈이 몇 번 바뀌다 멈춘다(모션 줄이기 설정이면 바로 멈춘다).
 * 굴린 값과 가장 닮은 성향을 아래에 한 줄로 보여 준다.
 */
@Composable
private fun DicePanel(
    roll: StartingRoll?,
    rollNonce: Int,
    tendency: StarterUi?,
    rerollsLeft: Int,
    onRoll: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    var shuffling by remember { mutableStateOf<StartingRoll?>(null) }
    LaunchedEffect(rollNonce) {
        if (rollNonce == 0 || reduceMotion) return@LaunchedEffect
        repeat(DICE_SHUFFLE_FRAMES) {
            shuffling = StartingDice.roll()
            delay(DICE_SHUFFLE_FRAME_MS)
        }
        shuffling = null
    }
    val shown = shuffling ?: roll
    val settled = shuffling == null
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("dice_panel"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (shown == null) {
            Text(
                text = "운동능력 · 지력 · 사회성 · 경제력 · 행복 · 운을 주사위 두 개씩으로 정한다. 다시 굴리기는 ${StartingDice.REROLLS}번까지.",
                color = LifeGameInkSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        } else {
            shown.rolls.forEach { stat -> DiceRow(stat, settled) }
            if (settled && tendency != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "성향: ${tendency.name}",
                    color = LifeGameInk,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("dice_tendency"),
                )
                Text(tendency.description, color = LifeGameInkTertiary, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        when {
            roll == null -> InkButton(text = "주사위 굴리기", onClick = onRoll, modifier = Modifier.testTag("dice_roll"))
            rerollsLeft > 0 -> LineButton(
                text = "다시 굴리기 (${rerollsLeft}번 남음)",
                onClick = { if (settled) onRoll() },
                modifier = Modifier.testTag("dice_reroll"),
            )
            else -> Text(
                text = "더 굴릴 수 없다. 이 능력치로 태어난다.",
                color = LifeGameInkTertiary,
                fontSize = 13.sp,
                modifier = Modifier.testTag("dice_no_reroll"),
            )
        }
    }
}

private const val DICE_SHUFFLE_FRAMES = 7
private const val DICE_SHUFFLE_FRAME_MS = 70L

@Composable
private fun DiceRow(stat: StatRoll, settled: Boolean) {
    Row(
        Modifier.fillMaxWidth().testTag("dice_row_${stat.label}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stat.label, color = LifeGameInk, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Die(stat.first)
        Die(stat.second)
        val delta = stat.delta
        Text(
            text = when {
                !settled -> ""
                else -> "${stat.first}+${stat.second}=${stat.sum} → ${stat.finalValue}"
            },
            color = when {
                delta > 0 -> LifeGameStatUp
                delta < 0 -> LifeGameStatDown
                else -> LifeGameInkTertiary
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            modifier = Modifier.width(112.dp),
        )
    }
}

/** 주사위 한 개. 눈은 원으로 그린다. */
@Composable
private fun Die(value: Int) {
    val shape = RoundedCornerShape(6.dp)
    Canvas(
        Modifier
            .size(26.dp)
            .clip(shape)
            .background(LifeGamePaper)
            .border(1.dp, LifeGameInk, shape)
            .semantics { contentDescription = "주사위 $value" },
    ) {
        val r = size.minDimension * 0.09f
        val a = size.width * 0.27f
        val c = size.width * 0.5f
        val b = size.width * 0.73f
        val pips = when (value) {
            1 -> listOf(c to c)
            2 -> listOf(a to a, b to b)
            3 -> listOf(a to a, c to c, b to b)
            4 -> listOf(a to a, b to a, a to b, b to b)
            5 -> listOf(a to a, b to a, c to c, a to b, b to b)
            else -> listOf(a to a, b to a, a to c, b to c, a to b, b to b)
        }
        pips.forEach { (x, y) -> drawCircle(LifeGameInk, radius = r, center = Offset(x, y)) }
    }
}

@Composable
private fun AppearancePreview(skinId: String, hairId: String, eyesId: String, photoFaceSheet: String? = null) {
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
        // 나이 드는 모습까지 보이도록 영아 · 청소년 · 중년 · 노년 네 장을 보여 준다.
        listOf(
            LifePeriod.INFANT to "지금",
            LifePeriod.TEEN to "청소년기",
            LifePeriod.MIDDLE to "중년기",
            LifePeriod.OLD_AGE to "노년기",
        ).forEach { (period, label) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                PlayerAvatar(
                    look = AvatarLook(
                        skinId, hairId, eyesId, period, BodyType.STANDARD,
                        usesPhotoFace = photoFaceSheet != null,
                        photoFaceSheet = photoFaceSheet,
                    ),
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
    aiFace: Boolean,
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
            text = if (aiFace) {
                "얼굴이 잘 보이는 정면 사진을 고르면 AI가 그 얼굴로 영아기부터 노년기까지의 도트 얼굴을 만든다. 30초~1분쯤 걸린다. 사진이 없어도 위에서 고른 모습으로 똑같이 시작한다."
            } else {
                "사진을 고르면 그 얼굴로 캐릭터 얼굴 시트를 만들도록 요청한다. 사진이 없어도 위에서 고른 모습으로 똑같이 시작한다."
            },
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
                    Text(
                        if (aiFace) "AI가 얼굴을 그리는 중 (30초~1분)" else "얼굴 시트를 만드는 중",
                        color = LifeGameInkSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f).testTag("face_loading"),
                    )
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
