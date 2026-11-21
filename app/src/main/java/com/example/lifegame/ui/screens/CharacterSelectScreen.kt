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
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.example.lifegame.ui.StartingStatRowUi
import com.example.lifegame.ui.startingStatRows
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.liveRegion
import com.example.lifegame.ui.ChallengeCode
import com.example.lifegame.ui.ChallengeCodeResult
import com.example.lifegame.ui.ChallengeStart
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
import com.example.lifegame.ui.components.dice.Dice3DStage
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatDownSoft
import com.example.lifegame.ui.theme.LifeGameStatUp
import com.example.lifegame.ui.theme.LifeGameStatUpSoft
import com.example.lifegame.ui.theme.LifeGameMuted
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
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
 * [aiFace]는 온라인 생성 연결 상태다. 현재 배포는 연결 전이며 사진을 전송하지 않고 기본 외형을 쓴다.
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
    /** 칭호 목록에서 "다음 판에서 노리기"로 고른, 아직 받지 못한 칭호 */
    titleTarget: com.example.lifegame.ui.TitleEntryUi? = null,
) {
    var nameInput by rememberSaveable { mutableStateOf("") }
    var gender by rememberSaveable { mutableStateOf<Gender?>(null) }
    var rollRaw by rememberSaveable { mutableStateOf<String?>(null) }
    var rerollsLeft by rememberSaveable { mutableStateOf(StartingDice.REROLLS) }
    // 이 출발선의 실행 시드. 도전 코드에 담기고, 코드로 시작하면 코드의 시드로 바뀐다.
    var runSeed by rememberSaveable { mutableStateOf(kotlin.random.Random.nextLong() and Long.MAX_VALUE) }
    var codeInput by rememberSaveable { mutableStateOf("") }
    var codeMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var codeError by rememberSaveable { mutableStateOf(false) }
    var rollNonce by remember { mutableStateOf(0) }
    var skinId by rememberSaveable { mutableStateOf(AvatarOptions.skins.first().id) }
    var hairId by rememberSaveable { mutableStateOf(AvatarOptions.hairs.first().id) }
    var eyesId by rememberSaveable { mutableStateOf(AvatarOptions.eyes.first().id) }
    var hairColorId by rememberSaveable { mutableStateOf(AvatarOptions.hairColors.first().id) }
    var outfitColorId by rememberSaveable { mutableStateOf(AvatarOptions.outfitColors.first().id) }
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
            titleTarget?.let { target ->
                val shape = RoundedCornerShape(10.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(LifeGameCard)
                        .border(1.dp, LifeGameRule, shape)
                        .padding(12.dp)
                        .testTag("profile_title_target"),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text("이번 판에서 노리는 칭호 · ${target.text}", color = LifeGameInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    target.goal?.let { Text("목표 · $it", color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp) }
                }
            }

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

            SectionLabel("같은 출발선 도전 코드", trailing = "선택")
            ChallengeCodeSection(
                code = if (gender != null && rollRaw != null) {
                    ChallengeCode.encode(ChallengeStart(gender!!, rollRaw!!, runSeed))
                } else {
                    null
                },
                input = codeInput,
                onInputChange = { codeInput = it.take(48) },
                message = codeMessage,
                isError = codeError,
                onApply = {
                    when (val result = ChallengeCode.decode(codeInput)) {
                        is ChallengeCodeResult.Valid -> {
                            gender = result.start.gender
                            rollRaw = result.start.rollRaw
                            runSeed = result.start.seed
                            rerollsLeft = 0 // 친구와 같은 출발선이라 다시 굴리지 않는다
                            // 도전 코드는 이미 확정된 주사위 값을 공유하므로 셔플 애니메이션을 재생하지 않는다.
                            codeError = false
                            codeMessage = "도전 코드의 출발선(성별 · 타고난 능력치 · 시드)으로 맞췄다. 이제 선택만 다르다."
                        }
                        is ChallengeCodeResult.Invalid -> {
                            codeError = true
                            codeMessage = result.message
                        }
                    }
                    focusManager.clearFocus()
                },
            )

            SectionLabel("모습", trailing = "사진 없이도 정할 수 있다")
            AppearancePreview(
                skinId = skinId,
                hairId = hairId,
                eyesId = eyesId,
                hairColorId = hairColorId,
                outfitColorId = outfitColorId,
                photoFaceSheet = PhotoFaceSheet.pathOf(sheet?.takeIf { it.status == GenerationStatus.READY }?.value),
            )
            OptionChips("피부", AvatarOptions.skins, skinId) { skinId = it }
            OptionChips("머리", AvatarOptions.hairs, hairId) { hairId = it }
            OptionChips(
                "머리 색",
                AvatarOptions.hairColors.map { AvatarOption(it.id, it.label) },
                hairColorId,
                note = "노년기에는 흰머리로 바뀐다",
            ) { hairColorId = it }
            OptionChips("눈", AvatarOptions.eyes, eyesId) { eyesId = it }
            OptionChips(
                "옷 색",
                AvatarOptions.outfitColors.map { AvatarOption(it.id, it.label) },
                outfitColorId,
                note = "시기마다 옷 모양은 달라지고, 윗옷 색이 바뀐다",
            ) { outfitColorId = it }

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
                    photoMessage = "사진 참조를 지웠습니다. 사진 없이 위에서 고른 모습으로 진행합니다."
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
                        runSeed = runSeed,
                        avatar = AvatarConfig(
                            skinToneId = skinId,
                            hairId = hairId,
                            eyesId = eyesId,
                            hairColorId = hairColorId,
                            outfitColorId = outfitColorId,
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
                        "온라인 얼굴 생성은 아직 연결되지 않았습니다. 고른 사진은 기기 밖으로 보내지 않고 참조만 저장하며, 캐릭터는 고른 기본 외형으로 표시됩니다. " +
                            "사진과 사진 참조가 포함된 인생 기록은 자동 백업하지 않습니다. '사진 지우기'로 시작 전 사진 참조를 지울 수 있습니다."
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
                    photoMessage = "사진 없이 위에서 고른 모습으로 진행합니다."
                    onPhotoFlowChanged(PhotoFlowStatus.FALLBACK, photoMessage)
                }, modifier = Modifier.testTag("photo_consent_no")) { Text("동의하지 않음", color = LifeGameInkSecondary) }
            },
        )
    }
}

/**
 * 타고난 능력치 정하기(2026-11-20 과제: 3D 눈과 능력치 줄 1:1).
 * 3D 주사위 위에 지금 보이는 눈이 어느 능력치인지 이름을 붙이고, 여섯 줄은 각각 "1 + 4 = 5 · 50 + 5 = 55"처럼
 * 그 능력치의 두 눈 · 합 · 실제 증가량 · 최종값을 보여 준다. 줄을 누르면 위 3D(정지 그림 대체 포함)가 그 능력치의 눈으로 바뀐다.
 * 숫자는 로직 결과([startingStatRows])를 그대로 쓰고 화면에서 규칙을 계산하지 않는다. 다시 굴리기 · 연타 잠금은 그대로다.
 * 굴린 값과 가장 닮은 성향을 아래에 한 줄로 보여 준다.
 */
@Composable
internal fun DicePanel(
    roll: StartingRoll?,
    rollNonce: Int,
    tendency: StarterUi?,
    rerollsLeft: Int,
    onRoll: () -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    var rolling by remember { mutableStateOf(false) }
    LaunchedEffect(rollNonce, reduceMotion) {
        if (rollNonce > 0 && !reduceMotion) delay(490L)
        rolling = false
    }
    val rollOnce = {
        // Synchronous lock covers two taps before the parent has recomposed.
        if (!rolling) { rolling = true; onRoll() }
    }
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
        if (roll == null) {
            Text(
                text = "주사위를 굴려 운동능력 · 지력 · 사회성 · 경제력 · 행복 · 운을 정한다. 다시 굴리기는 ${StartingDice.REROLLS}번까지.",
                color = LifeGameInkSecondary,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        } else {
            val rows = remember(roll) { startingStatRows(roll) }
            // 회전 · 복원 뒤에도 보던 능력치를 그대로 보여 준다. 굴림 결과나 재굴림 횟수에는 영향이 없다.
            var shownIndex by rememberSaveable { mutableStateOf(0) }
            val shown = rows.getOrElse(shownIndex) { rows.first() }
            Text(
                text = "${shown.label}의 두 눈",
                color = LifeGameInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("dice_3d_label"),
            )
            Dice3DStage(first = shown.first, second = shown.second, rollNonce = rollNonce, label = shown.label)
            Text(
                text = "능력치마다 주사위 두 개를 굴려요. 줄을 누르면 그 능력치의 두 눈을 위에서 볼 수 있어요.",
                color = LifeGameInkSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.testTag("dice_rule"),
            )
            rows.forEachIndexed { index, row -> DiceRow(row, shown = index == shownIndex, onShow = { shownIndex = index }) }
            if (tendency != null) {
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
            roll == null -> InkButton(text = "주사위 굴리기", onClick = rollOnce, enabled = !rolling, modifier = Modifier.testTag("dice_roll"))
            rerollsLeft > 0 -> LineButton(
                text = "다시 굴리기 (${rerollsLeft}번 남음)",
                onClick = rollOnce,
                enabled = !rolling,
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

/**
 * 능력치 한 줄: "경제력 · 1 + 4 = 5 · 50 + 5 = 55". 증가량은 오르면 파랑, 내리면 빨강, 그대로면 회색, 최종값은 굵게.
 * 누르면 위 3D 주사위가 이 능력치의 두 눈을 보여 준다(다시 굴리지 않는다). 낭독도 같은 값을 읽는다.
 */
@Composable
private fun DiceRow(row: StartingStatRowUi, shown: Boolean, onShow: () -> Unit) {
    val increaseColor = when {
        row.increase > 0 -> LifeGameStatUp
        row.increase < 0 -> LifeGameStatDown
        else -> LifeGameInkTertiary
    }
    val formula = buildAnnotatedString {
        append(row.diceText)
        append(" · ")
        append("${row.base}\u00A0")
        withStyle(SpanStyle(color = increaseColor, fontWeight = FontWeight.Bold)) { append(row.increaseText) }
        append("\u00A0=\u00A0")
        withStyle(SpanStyle(color = LifeGameInk, fontWeight = FontWeight.Bold)) { append("${row.finalValue}") }
    }
    val shape = RoundedCornerShape(8.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(shape)
            .background(if (shown) LifeGameMuted else LifeGameCard)
            .selectable(selected = shown, role = Role.RadioButton, onClick = onShow)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .clearAndSetSemantics {
                contentDescription = row.speech
                stateDescription = if (shown) "위 주사위에 보이는 중" else "누르면 위 주사위에 보여요"
                selected = shown
                role = Role.RadioButton
                onClick(label = "${row.label}의 두 눈 보기") { onShow(); true }
                testTag = "dice_row_${row.label}"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            row.label,
            color = LifeGameInk,
            fontSize = 15.sp,
            fontWeight = if (shown) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.widthIn(min = 64.dp).testTag("dice_label_${row.label}"),
        )
        Text(
            text = formula,
            color = LifeGameInkSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).testTag("dice_formula_${row.label}"),
        )
    }
}

@Composable
private fun AppearancePreview(
    skinId: String,
    hairId: String,
    eyesId: String,
    hairColorId: String = AvatarOptions.hairColors.first().id,
    outfitColorId: String = AvatarOptions.outfitColors.first().id,
    photoFaceSheet: String? = null,
) {
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
                        hairColorId = hairColorId,
                        outfitColorId = outfitColorId,
                    ),
                    modifier = Modifier.size(width = 60.dp, height = 96.dp),
                )
                Text(label, color = LifeGameInkTertiary, fontSize = 13.sp)
            }
        }
    }
}

/**
 * 같은 출발선 도전 코드(승인된 확장 기능 4): 지금 고른 성별 · 주사위 · 시드로 만든 코드를 보여 주고 복사하게 하며,
 * 친구가 준 코드를 넣으면 그 출발선으로 맞춘다. 인생 기록은 담지 않고 비교 화면도 없다.
 */
@Composable
private fun ChallengeCodeSection(
    code: String?,
    input: String,
    onInputChange: (String) -> Unit,
    message: String?,
    isError: Boolean,
    onApply: () -> Unit,
) {
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("challenge_section"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "같은 성별 · 같은 주사위 · 같은 시드에서 친구와 다른 선택을 해 보는 코드다.",
            color = LifeGameInkSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        if (code == null) {
            Text("성별을 고르고 주사위를 굴리면 내 출발선 코드가 생긴다.", color = LifeGameInkTertiary, fontSize = 13.sp, lineHeight = 18.sp)
        } else {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = code,
                    color = LifeGameInk,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("challenge_code"),
                )
            }
            LineButton(
                text = if (copied) "복사했다" else "코드 복사",
                onClick = {
                    clipboard.setText(androidx.compose.ui.text.AnnotatedString(code))
                    copied = true
                },
                modifier = Modifier.testTag("challenge_copy"),
            )
        }
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.fillMaxWidth().testTag("challenge_input"),
            label = { Text("친구의 도전 코드") },
            placeholder = { Text("LG1-…") },
            singleLine = true,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = LifeGamePaper,
                unfocusedContainerColor = LifeGamePaper,
                focusedBorderColor = LifeGameInk,
                unfocusedBorderColor = LifeGameRule,
                cursorColor = LifeGameInk,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onApply() }),
        )
        LineButton(text = "이 코드로 출발선 맞추기", onClick = onApply, modifier = Modifier.testTag("challenge_apply"))
        message?.let {
            Text(
                text = it,
                color = if (isError) LifeGameStatDown else LifeGameInkSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier
                    .semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite }
                    .testTag(if (isError) "challenge_error" else "challenge_message"),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionChips(
    label: String,
    options: List<AvatarOption>,
    selectedId: String,
    note: String? = null,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = LifeGameInkSecondary, fontSize = 14.sp)
        note?.let { Text(it, color = LifeGameInkTertiary, fontSize = 13.sp, lineHeight = 18.sp) }
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
                "온라인 얼굴 생성은 아직 연결되지 않았다. 고른 사진은 전송하지 않으며, 위에서 고른 기본 외형으로 시작한다."
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
                        if (aiFace) "AI가 얼굴을 그리는 중 (30초~1분)" else "기본 모습 준비 중",
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
