package com.example.lifegame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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

/** 주마등 한 장면이 머무는 시간 */
private const val RECAP_MILLIS = 2_800L

/**
 * 인생이 끝난 뒤의 흐름. 플레이 직후에는 주마등부터, 홈의 지난 인생에서 열면 기록증부터 보여 준다.
 * 모든 내용은 저장된 완료 기록에서 만든 [LifeRecordUi]다. 이 화면은 기록을 새로 만들거나 고치지 않는다.
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
) {
    var step by rememberSaveable(record.runId) { mutableStateOf(initialStep) }
    val look = futureLook?.takeIf { it.key == record.runId }

    BackHandler(enabled = step != initialStep) {
        if (step == EndingStep.FUTURE && look?.status == GenerationStatus.LOADING) onCancelFuture()
        step = EndingStep.entries[step.ordinal - 1].takeIf { it.ordinal >= initialStep.ordinal } ?: initialStep
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
            modifier = modifier,
        )
    }
}

/** 주마등: 실제로 지나온 장면을 시간순으로. 누르면 다음 장면, 건너뛰기로 바로 다음 단계. */
@Composable
private fun RecapView(record: LifeRecordUi, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val moments = record.recap
    var index by rememberSaveable(record.runId) { mutableIntStateOf(0) }
    val reduceMotion = rememberReduceMotion()

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
            AnimatedContent(
                targetState = moments[safeIndex],
                transitionSpec = {
                    if (reduceMotion) fadeIn(androidx.compose.animation.core.snap()) togetherWith fadeOut(androidx.compose.animation.core.snap())
                    else fadeIn() togetherWith fadeOut()
                },
                label = "recap",
            ) { moment -> RecapMoment(moment) }
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

@Composable
private fun RecapMoment(moment: RecapMomentUi) {
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
                .testTag("recap_picture"),
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
        Text(moment.place, color = LifeMutedOnDark, fontSize = 14.sp)
        Text(
            text = moment.title,
            color = LifePaperOnDark,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 29.sp,
        )
        Text("→ ${moment.choiceLabel}", color = LifePaperOnDark.copy(alpha = 0.86f), fontSize = 15.sp, lineHeight = 21.sp)
        Text(
            text = moment.resultText,
            color = LifePaperOnDark,
            fontFamily = LifeGameSerif,
            fontSize = 16.sp,
            lineHeight = 25.sp,
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

/** 인생기록증: 이름 · 칭호 · 사망 시기와 원인 · 교육 · 직업 · 가족 · 주거 · 경제력 · 업적 · 생애 요약. */
@Composable
private fun CertificateView(
    record: LifeRecordUi,
    onReplay: () -> Unit,
    onDone: () -> Unit,
    doneLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp)
                .testTag("certificate"),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "인생기록증",
                color = LifeGameInkSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.semantics { heading() },
            )
            val shape = RoundedCornerShape(12.dp)
            Row(
                Modifier
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
                        Text(
                            text = "칭호 · ${title.text}",
                            color = LifeGameSpecial,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.testTag("certificate_title"),
                        )
                        Text(title.description, color = LifeGameInkSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                    } else {
                        Text("칭호 기록 없음", color = LifeGameInkTertiary, fontSize = 14.sp, modifier = Modifier.testTag("certificate_title"))
                    }
                    if (record.usesPhotoFace) {
                        Text("사진 얼굴: 예시 응답(서버 연결 전)", color = LifeGameInkTertiary, fontSize = 12.5.sp)
                    }
                }
            }
            EndingBody(record.summary)
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
