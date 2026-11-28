package com.example.lifegame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.ui.GenerationStatus
import com.example.lifegame.ui.GenerationUi
import com.example.lifegame.ui.LifeRecordUi
import com.example.lifegame.ui.RecapMomentUi
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.images.rememberPixelAsset
import androidx.compose.ui.platform.LocalDensity
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.CompletionSealHeader
import com.example.lifegame.ui.components.LocalMotionStyle
import com.example.lifegame.ui.components.rememberCompletionReveal
import com.example.lifegame.ui.components.rememberEntranceMotionAllowed
import com.example.lifegame.ui.components.LifeMutedOnDark
import com.example.lifegame.ui.components.LifePaperOnDark
import com.example.lifegame.ui.components.LineButton
import com.example.lifegame.ui.components.PixelAssetImage
import com.example.lifegame.ui.components.SceneBackdrop
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import kotlinx.coroutines.delay

/** 종료 흐름 단계: 주마등 → 미래 모습 → 인생기록증 */
enum class EndingStep { RECAP, FUTURE, CERTIFICATE }

/** 기록증 칭호 줄의 머리말. 칭호 공개(SPIN_STAMP)는 이 뒤의 글자만 찍는다. */
private const val CERTIFICATE_TITLE_PREFIX = "칭호 · "

/**
 * 뒤로가기로 갈 단계. 바로 앞 단계로 가되, 화면을 연 단계보다 앞으로는 가지 않는다.
 * 지난 인생 기록은 인생기록증에서 열리므로, 거기서 "주마등 다시 보기"로 들어간 주마등 · 미래 모습의 뒤로가기는 인생기록증으로 돌아간다.
 * 주마등은 첫 단계라 앞 단계가 없다. 예전에는 `entries[-1]`을 읽어 앱이 꺼졌다(2026-11-12 에뮬레이터에서 재현).
 */
internal fun previousEndingStep(step: EndingStep, initialStep: EndingStep): EndingStep =
    EndingStep.entries.getOrNull(step.ordinal - 1)?.takeIf { it.ordinal >= initialStep.ordinal } ?: initialStep

/** 주마등 한 장면이 머무는 시간. 연출 비교 화면의 주마등 미리보기도 같은 시간으로 넘어간다. */
internal const val RECAP_MILLIS = 2_800L

/**
 * 인생이 끝난 뒤의 흐름. 플레이 직후에는 주마등부터, 홈의 지난 인생에서 열면 기록증부터 보여 준다.
 * 모든 내용은 저장된 완료 기록에서 만든 [LifeRecordUi]다. 이 화면은 기록을 새로 만들거나 고치지 않는다.
 * [titleUnlock]은 플레이 직후의 흐름만 넘긴다(칭호 획득 팝업, lobby2 영역). 지난 기록을 열 때는 null이다.
 */
@Composable
fun EndingFlowScreen(
    record: LifeRecordUi,
    futureLook: GenerationUi?,
    onRequestFuture: () -> Unit,
    onCancelFuture: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    doneLabel: String = "홈으로",
    initialStep: EndingStep = EndingStep.RECAP,
    titleUnlock: TitleUnlockUi? = null,
) {
    var step by rememberSaveable(record.runId) { mutableStateOf(initialStep) }
    // Presentation-only consumption survives replay and Activity recreation. Archived records open quietly.
    var completionPresented by rememberSaveable(record.runId) { mutableStateOf(initialStep == EndingStep.CERTIFICATE) }
    val look = futureLook?.takeIf { it.key == record.runId }

    BackHandler(enabled = step != initialStep) {
        if (step == EndingStep.FUTURE && look?.status == GenerationStatus.LOADING) onCancelFuture()
        step = previousEndingStep(step, initialStep)
    }

    when (step) {
        EndingStep.RECAP -> RecapView(
            record = record,
            onFinish = { step = EndingStep.FUTURE },
            modifier = modifier,
        )
        EndingStep.FUTURE -> FutureView(
            record = record,
            look = look,
            onRequest = onRequestFuture,
            onCancel = onCancelFuture,
            onNext = {
                if (look?.status == GenerationStatus.LOADING) onCancelFuture()
                step = EndingStep.CERTIFICATE
            },
            modifier = modifier,
        )
        EndingStep.CERTIFICATE -> CertificateView(
            record = record,
            onReplay = { step = EndingStep.RECAP },
            onDone = onDone,
            doneLabel = doneLabel,
            celebrate = !completionPresented,
            onCelebrationConsumed = { completionPresented = true },
            modifier = modifier,
            titleUnlock = titleUnlock,
        )
    }
}

/** 주마등: 실제로 지나온 장면을 시간순으로. 누르면 다음 장면, 건너뛰기로 바로 다음 단계. */
@Composable
private fun RecapView(record: LifeRecordUi, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val moments = record.recap
    var index by rememberSaveable(record.runId) { mutableIntStateOf(0) }

    LaunchedEffect(record.runId, index) {
        if (moments.isEmpty()) return@LaunchedEffect
        delay(RECAP_MILLIS)
        if (index < moments.lastIndex) index += 1 else onFinish()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(LifeGameInk)
            .safeDrawingPadding()
            .testTag("recap_screen"),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "주마등",
                color = LifePaperOnDark,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                text = "건너뛰기",
                color = LifePaperOnDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(role = Role.Button, onClick = onFinish)
                    .padding(horizontal = 14.dp, vertical = 13.dp)
                    .testTag("recap_skip"),
            )
        }
        if (moments.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("돌아볼 장면이 기록되지 않았다.", color = LifeMutedOnDark, fontSize = 15.sp)
            }
            LineButton(
                text = "다음",
                onClick = onFinish,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }
        val safeIndex = index.coerceIn(0, moments.lastIndex)
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clickable(role = Role.Button) { if (safeIndex < moments.lastIndex) index = safeIndex + 1 else onFinish() }
                .semantics { contentDescription = "다음 장면" },
        ) {
            RecapStage(moments, safeIndex)
        }
        Text(
            text = "${safeIndex + 1} / ${moments.size}",
            color = LifeMutedOnDark,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp).testTag("recap_counter"),
        )
    }
}

/**
 * 주마등의 무대: 지금 장면과 장면 사이 전환(연출: LocalMotionStyle.recapMoment). 넘기기 · 타이머는 부르는 쪽이 정한다.
 * 연출 비교 화면도 이 무대를 그대로 쓴다.
 */
@Composable
internal fun RecapStage(moments: List<RecapMomentUi>, index: Int, modifier: Modifier = Modifier) {
    if (moments.isEmpty()) return
    val reduceMotion = rememberReduceMotion()
    val entrancesMove = rememberEntranceMotionAllowed()
    val style = LocalMotionStyle.current.recapMoment
    AnimatedContent(
        targetState = moments[index.coerceIn(0, moments.lastIndex)],
        transitionSpec = { recapTransition(style, reduceMotion, entrancesMove) },
        modifier = modifier,
        label = "recap",
    ) { moment -> RecapMoment(moment) }
}

@Composable
internal fun RecapMoment(moment: RecapMomentUi) {
    // 장면마다 제 시계: 그림이 자리 잡고 글줄이 차례로 나타난다. 모두 1.1초 안에 끝나고 반복하지 않는다.
    val motion = rememberRecapMomentMotion(moment.key)
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag("recap_moment"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .testTag("recap_picture")
                .recapPicture(motion),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SceneBackdrop(moment.images, Modifier.fillMaxSize())
            // 실제로 불러온 그림을 보고 정한다: 1인칭 사건 그림 위에는 얼굴을 억지로 올리지 않는다.
            val density = LocalDensity.current
            val shown = if (moment.images.isEmpty()) {
                null
            } else {
                rememberPixelAsset(
                    moment.images,
                    with(density) { maxWidth.roundToPx() }.coerceAtLeast(1),
                    with(density) { maxHeight.roundToPx() }.coerceAtLeast(1),
                )?.candidate
            }
            moment.avatar?.takeUnless { moment.isFirstPerson(shown) }?.let { look ->
                PlayerAvatar(
                    look = look,
                    modifier = Modifier
                        .padding(bottom = maxHeight * 0.05f)
                        .height(maxHeight * 0.7f)
                        .aspectRatio(0.62f),
                )
            }
        }
        Text(moment.place, color = LifeMutedOnDark, fontSize = 14.sp, modifier = Modifier.recapLine(motion, 0))
        Text(
            text = moment.title,
            color = LifePaperOnDark,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 29.sp,
            modifier = Modifier.recapLine(motion, 1),
        )
        Text(
            "→ ${moment.choiceLabel}",
            color = LifePaperOnDark.copy(alpha = 0.86f),
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.recapLine(motion, 2),
        )
        Text(
            text = moment.resultText,
            color = LifePaperOnDark,
            fontFamily = LifeGameSerif,
            fontSize = 16.sp,
            lineHeight = 25.sp,
            modifier = Modifier.recapLine(motion, 3),
        )
    }
}

/**
 * 미래 모습. 마지막 시기 · 체형 · 외형으로 조립한 캐릭터를 가장 많이 걸어온 경로의 장면 위에 세운다.
 * 생성(Fake)은 30초 제한 · 취소 가능이며, 실패해도 같은 기기 안 그림으로 이어진다.
 */
@Composable
private fun FutureView(
    record: LifeRecordUi,
    look: GenerationUi?,
    onRequest: () -> Unit,
    onCancel: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(record.runId) { onRequest() }
    val loading = look == null || look.status == GenerationStatus.LOADING
    val early = record.summary.death?.early == true
    Column(
        modifier
            .fillMaxSize()
            .background(LifeGameInk)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("future_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = if (early) "마지막 모습" else "미래 모습",
            color = LifePaperOnDark,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = "${record.finalPeriodName}의 ${record.playerName} · ${record.pathTitle}",
            color = LifeMutedOnDark,
            fontSize = 14.sp,
        )
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(12.dp))
                .testTag("future_stage"),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SceneBackdrop(record.backdrop, Modifier.fillMaxSize())
            if (!loading) {
                PlayerAvatar(
                    look = record.avatar,
                    modifier = Modifier
                        .padding(bottom = maxHeight * 0.05f)
                        .height(maxHeight * 0.78f)
                        .aspectRatio(0.62f),
                )
            } else {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = LifePaperOnDark, modifier = Modifier.size(36.dp))
                        Text("모습을 그리는 중", color = LifePaperOnDark, fontSize = 15.sp, modifier = Modifier.testTag("future_loading"))
                    }
                }
            }
        }
        look?.message?.takeIf { !loading }?.let { message ->
            Text(
                text = message,
                color = if (look.status == GenerationStatus.FALLBACK) Color(0xFFFFC2A8) else LifeMutedOnDark,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.testTag("future_message").semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        if (loading) {
            Text(
                text = "취소",
                color = LifePaperOnDark,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, LifePaperOnDark.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(vertical = 13.dp)
                    .testTag("future_cancel"),
            )
        }
        LineButton(text = "인생기록증 보기", onClick = onNext, modifier = Modifier.testTag("future_next"))
    }
}

/**
 * 인생기록증: 이름 · 칭호 · 사망 시기와 원인 · 교육 · 직업 · 가족 · 주거 · 경제력 · 업적 · 생애 요약.
 * 막 끝난 인생을 처음 열 때만 한 번 펼쳐진다(카드 · 본문 · 도장 · 칭호 공개, 모두 1초 안). 지난 기록 · 주마등 다시 보기 ·
 * 화면 재생성 · 백그라운드에서 돌아올 때는 처음부터 다 찍힌 모습이다. 연출 비교 화면이 그대로 미리 보여 준다.
 */
@Composable
internal fun CertificateView(
    record: LifeRecordUi,
    onReplay: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    celebrate: Boolean,
    onCelebrationConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    titleUnlock: TitleUnlockUi? = null,
) {
    // 칭호 획득 팝업(lobby2 영역)의 상태. 팝업이 칭호를 축하하는 동안(unlock.celebratesTitle) 기록증 안의 칭호 공개는 쉰다.
    val unlock = rememberTitleUnlockState(record, titleUnlock, celebrate)
    val reveal = rememberCompletionReveal(record.runId, celebrate, onCelebrationConsumed)
    val motion = LocalMotionStyle.current
    val seal = motion.certificateSeal
    val titleReveal = certificateTitleRevealFor(motion.titleReveal, hasTitle = record.title != null, popupCelebrates = unlock.celebratesTitle)
    val sealArt = rememberCertificateSealArt(record.runId)
    CertificateSealHaptic(reveal, seal)
    Column(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding(),
    ) {
        // 팝업은 스크롤 영역만 덮고 아래 "주마등 다시 보기 · 홈으로" 줄은 덮지 않는다. 배치 · 의미는 이전과 같다.
        Box(Modifier.weight(1f).fillMaxWidth().titleUnlockOutsideTouch(unlock)) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .testTag("certificate"),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CompletionSealHeader(
                    reveal = reveal,
                    titleEarned = record.title != null,
                    badgeModifier = Modifier.titleUnlockAnchor(unlock),
                    titleReveal = titleReveal,
                )
                val shape = RoundedCornerShape(12.dp)
                Row(
                    Modifier
                        // 카드가 떠오르고 도장이 사진 모서리에 찍힌다. 도장은 카드의 clip 바깥에서 그려 잘리지 않는다.
                        .certificateCardEntrance(seal, reveal.clockMs)
                        .certificateSeal(seal, reveal.clockMs, sealArt)
                        .fillMaxWidth()
                        .clip(shape)
                        .background(LifeGameCard)
                        .border(1.dp, LifeGameRule, shape)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlayerAvatar(record.avatar, Modifier.size(width = 64.dp, height = 96.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = record.playerName,
                            color = LifeGameInk,
                            fontFamily = LifeGameSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            modifier = Modifier.testTag("certificate_name"),
                        )
                        val title = record.title
                        if (title != null) {
                            val titleLayout = remember { mutableStateOf<TextLayoutResult?>(null) }
                            Text(
                                text = "$CERTIFICATE_TITLE_PREFIX${title.text}",
                                color = LifeGameSpecial,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                onTextLayout = { titleLayout.value = it },
                                modifier = Modifier
                                    .testTag("certificate_title")
                                    .certificateTitleReveal(titleReveal, reveal.clockMs, titleLayout, CERTIFICATE_TITLE_PREFIX.length),
                            )
                            Text(title.description, color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                        } else {
                            Text("칭호 기록 없음", color = LifeGameInkTertiary, fontSize = 14.sp, modifier = Modifier.testTag("certificate_title"))
                        }
                        if (record.avatar.photoFaceSheet != null) {
                            Text("사진으로 만든 얼굴", color = LifeGameInkTertiary, fontSize = 13.sp, modifier = Modifier.testTag("certificate_photo_face"))
                        } else if (record.usesPhotoFace) {
                            Text("사진 얼굴: 예시 응답(생성 키 없는 빌드)", color = LifeGameInkTertiary, fontSize = 13.sp)
                        }
                    }
                }
                // 본문은 카드를 뒤따라 들어온다. 묶음 Column의 간격이 바깥과 같아 배치 · 의미는 이전과 같다.
                Column(
                    Modifier.fillMaxWidth().certificateBodyEntrance(seal, reveal.clockMs),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    EndingBody(record.summary)
                    LifeExtensionSections(record)
                }
            }
            TitleUnlockOverlay(unlock, record, Modifier.matchParentSize())
        }
        Column(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LineButton(text = "주마등 다시 보기", onClick = onReplay, modifier = Modifier.testTag("certificate_replay"))
            InkButton(text = doneLabel, onClick = onDone, modifier = Modifier.testTag("certificate_done"))
        }
    }
}

/**
 * 인생기록증의 승인된 확장 기능(2026-11-13): "내 인생의 계기" 카드(2~3장), "그때 그 선택" 회상(최대 3줄), 다시 만난 사람.
 * 모두 이번 인생의 실제 선택 기록에서만 나온다. 기록이 없으면 그 칸 자체를 그리지 않는다.
 */
@Composable
private fun LifeExtensionSections(record: LifeRecordUi) {
    if (record.turningPoints.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().testTag("certificate_turning_points"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CertificateSectionTitle("내 인생의 계기")
            record.turningPoints.forEach { card ->
                val shape = RoundedCornerShape(10.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(LifeGameCard)
                        .border(1.dp, LifeGameRule, shape)
                        .padding(12.dp)
                        // 카드 한 장을 화면 낭독에서 한 덩어리로 읽는다.
                        .semantics(mergeDescendants = true) {}
                        .testTag("turning_point_${card.cardId}"),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "${card.fieldLabel} · ${card.periodName} ${card.yearInPeriod}번째 해 · ${card.title}",
                        color = LifeGameInkTertiary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Text(card.text, color = LifeGameInk, fontSize = 15.sp, lineHeight = 22.sp)
                    Text("고른 선택: ${card.choiceLabel}", color = LifeGameInkSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }
    }
    if (record.recalls.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().testTag("certificate_recalls"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CertificateSectionTitle("그때 그 선택")
            record.recalls.forEach { recall ->
                Text(
                    text = recall.text,
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("certificate_recall_${recall.slotId}"),
                )
            }
        }
    }
    if (record.npcsMetAgain.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().testTag("certificate_npcs"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CertificateSectionTitle("다시 만난 사람")
            record.npcsMetAgain.forEach { npc ->
                Text(
                    text = "${npc.label}: ${npc.originPeriodName} 「${npc.originTitle}」에서 만나 " +
                        npc.metAgainTitles.joinToString(" · ") { "「$it」" } + "에서 다시 만났다",
                    color = LifeGameInkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("certificate_npc_${npc.npcId}"),
                )
            }
        }
    }
    record.partnerStory?.let { story ->
        // 2026-11-20: 연애 사건과 같은 상대, 함께한 연애 사건, 실제 결혼식 결과를 한 칸에. 기록에 있는 것만 보여 준다.
        Column(Modifier.fillMaxWidth().testTag("certificate_partner"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CertificateSectionTitle("인연")
            val headline = listOfNotNull(
                story.partner?.let { "${it.label}(${it.originPeriodName} ${it.originYear}번째 해)" },
                story.status.ifBlank { null },
            ).joinToString(" · ")
            if (headline.isNotBlank()) {
                Text(
                    text = headline,
                    color = LifeGameInk,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("certificate_partner_name"),
                )
            }
            story.moments.takeLast(4).forEach { moment ->
                Text(
                    text = "${moment.periodName} ${moment.year}번째 해 「${moment.title}」 · ${moment.choiceLabel}",
                    color = LifeGameInkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("certificate_partner_moment_${moment.eventId}"),
                )
            }
            story.wedding?.let { wedding ->
                Text(
                    text = "결혼식 · ${wedding.periodName} ${wedding.year}번째 해 「${wedding.title}」 · ${wedding.choiceLabel}",
                    color = LifeGameInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("certificate_wedding"),
                )
            }
        }
    }
    if (record.automaticEvents.isNotEmpty()) {
        // 자동 사건은 선택 기록(내 인생의 계기 · 그때 그 선택)과 섞지 않고 따로, "자동" 표시와 함께 남긴다.
        Column(Modifier.fillMaxWidth().testTag("certificate_auto_events"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CertificateSectionTitle("저절로 지나간 일")
            record.automaticEvents.forEach { line ->
                val changes = line.changes.joinToString(" ") { "${it.name} ${if (it.delta > 0) "+" else ""}${it.delta}" }
                Text(
                    text = "[자동] ${line.label}${if (line.topic.isBlank()) "" else " · ${line.topic}"}: ${line.text}" +
                        if (changes.isBlank()) " (기록만)" else " ($changes)",
                    color = LifeGameInkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("certificate_auto_${line.key.substringAfter(":auto:")}"),
                )
            }
        }
    }
}

@Composable
private fun CertificateSectionTitle(text: String) {
    Text(
        text = text,
        color = LifeGameInkSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() },
    )
}
