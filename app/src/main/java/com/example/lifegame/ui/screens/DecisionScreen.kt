package com.example.lifegame.ui.screens

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import com.example.lifegame.ui.StatChangeUi
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatDownSoft
import com.example.lifegame.ui.theme.LifeGameStatUp
import com.example.lifegame.ui.theme.LifeGameStatUpSoft
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.domain.model.EventImageExposureStatus
import com.example.lifegame.domain.model.ResultAnimationStatus
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.DecisionUi
import com.example.lifegame.ui.OutcomeUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.avatar.PlayerAvatar
import com.example.lifegame.ui.avatar.PlayerPortrait
import com.example.lifegame.ui.components.DISTORT_MAX_DP
import com.example.lifegame.ui.components.DISTORT_MILLIS
import com.example.lifegame.ui.components.DecisionKindMark
import com.example.lifegame.ui.components.EffectText
import com.example.lifegame.ui.components.FilmMotion
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LifeFactsSheet
import com.example.lifegame.ui.components.LockIcon
import com.example.lifegame.ui.components.OnSceneShadow
import com.example.lifegame.ui.components.PerforationBand
import com.example.lifegame.ui.components.PeriodTrack
import com.example.lifegame.ui.components.PlayTopBar
import com.example.lifegame.ui.components.SceneImage
import com.example.lifegame.ui.components.SceneViewButton
import com.example.lifegame.ui.components.distortEnvelope
import com.example.lifegame.ui.components.markLabel
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.teaser
import com.example.lifegame.ui.theme.LifeGameBodyInk
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import com.example.lifegame.ui.theme.MarkLabelStyle
import com.example.lifegame.ui.theme.ResultStoryStyle
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlin.random.Random

private val PopupEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/** 본문 한 글자가 나오는 간격 */
internal const val TYPE_MILLIS = 18L

/** 완성 터치 뒤 연타를 무시하는 시간 */
internal const val TAP_GUARD_MILLIS = 300L

/** 본문이 다 나온 뒤 사건 그림을 먼저 보여 주기까지의 짧은 틈 (2026-11-06 요구 8) */
internal const val PREVIEW_DELAY_MILLIS = 600L

/** 사건 그림 선노출 시간. 그 전에 한 번 누르면 바로 선택지로 넘어간다. */
internal const val PREVIEW_HOLD_MILLIS = 2_500L

/** 선택 결과 변화 문구가 떠 있는 시간 (2026-11-06 요구 9) */
internal const val CHANGE_HOLD_MILLIS = 1_600L

/**
 * 켜져 있는 동안 화면의 모든 탭을 가로채 [onTap]만 부른다.
 * 부모가 Initial 단계에서 손을 뗀 이벤트를 먼저 소비하므로 아래 선택지 · 장면 보기 버튼의 클릭은 취소된다.
 * 누른 이벤트는 소비하지 않아 스크롤은 그대로 된다.
 */
private fun Modifier.completeTextOnTap(enabled: Boolean, onTap: () -> Unit): Modifier = composed {
    val currentTap by rememberUpdatedState(onTap)
    if (!enabled) {
        this
    } else {
        pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                if (up != null) {
                    up.consume()
                    currentTap()
                }
            }
        }
    }
}

/**
 * 결정 · 돌발 화면 (10주차 최종 시안).
 * 위: 투명 배경의 캐릭터와 두꺼운 건강 게이지. 가운데: 사건 분류 글자 효과 · 제목 · 본문(가운데 정렬).
 * 아래: 선택지(어두운 유리 카드)와 -O-O-O- 시기선. 배경 그림만 흐리게 하고 글자는 선명하게 둔다.
 * 장면 보기는 배경 블러를 풀고 모든 UI를 숨긴 뒤, 한 번 누르면 그대로 돌아온다.
 *
 * 순서 (2026-11-06 요구 8): 제목 · 본문 타이핑 → 사건 그림 선노출(블러 없이, 짧게 또는 한 번 터치까지) → 선택지.
 * 타이핑 중 터치는 본문 완성만 하고, 그림 선노출을 끝내는 터치도 바로 아래 선택지로 이어지지 않는다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DecisionScreen(
    state: PeriodPlayUiState,
    onChoose: (choiceId: String) -> Unit,
    onContinue: () -> Unit,
    onEventImageShown: (eventId: String) -> Unit = {},
    onResultAnimationConsumed: (displayId: String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val decision = state.decision ?: return
    val outcome = state.outcome
    val introKey = "${decision.eventId}:${state.yearKey}"
    // 앱을 완전히 닫았다 연 경우(화면 저장 상태가 없음)에도, 저장된 연출 상태(#63)상 이미 선노출까지 본 사건이면
    // 팝업 · 타이핑 · 선노출을 다시 하지 않고 바로 선택지를 보여 준다.
    val previewShownBefore = state.presentation.exposedEventId == decision.eventId &&
        state.presentation.eventImageExposure == EventImageExposureStatus.SHOWN
    var introDone by rememberSaveable(introKey) { mutableStateOf(outcome != null || previewShownBefore) }
    var sceneOnly by rememberSaveable(introKey) { mutableStateOf(false) }
    // 그림 선노출은 사건마다 한 번. 복원돼도 다시 보여 주지 않는다.
    var previewDone by rememberSaveable(introKey) { mutableStateOf(outcome != null || previewShownBefore) }
    var previewing by rememberSaveable(introKey) { mutableStateOf(false) }
    var showChoiceDetails by rememberSaveable { mutableStateOf(false) }
    var chosenId by remember(introKey) { mutableStateOf<String?>(null) }
    var typedChars by rememberSaveable(introKey) {
        mutableStateOf(if (outcome != null || previewShownBefore) decision.text.length else 0)
    }
    var detailsOpen by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf(FilmMotion()) }
    var popupOpen by remember { mutableStateOf(false) }
    val popupProgress = remember { Animatable(0f) }
    val popupClosed = remember { Channel<Unit>(Channel.CONFLATED) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val resultDisplayId = state.presentation.resultDisplayId
    val textLength = decision.text.length
    // 완성 터치 직후 아주 짧게 이어지는 연타도 삼켜, 본문을 보려던 손가락이 선택을 확정하지 않게 한다.
    var swallowTaps by remember(introKey) { mutableStateOf(false) }
    val typing = introDone && outcome == null && !sceneOnly && typedChars < textLength
    val completeText: () -> Unit = {
        if (typedChars < textLength) {
            typedChars = textLength
            swallowTaps = true
        }
    }
    val textComplete = introDone && typedChars >= textLength
    // 선노출이 끝나는 순간의 터치는 선택지로 넘기지 않는다. 눌러서 끝냈든 시간이 다 돼 저절로 끝났든 같다.
    // 저절로 끝날 때 보호가 없으면 "누르면 선택지로"를 누르려던 손가락이 막 나타난 선택지 카드에 떨어져,
    // 보지도 못한 선택지가 골라진다(2026-11-12 에뮬레이터 실제 플레이에서 확인).
    val endPreview: () -> Unit = {
        if (previewing) {
            previewing = false
            previewDone = true
            swallowTaps = true
        }
    }

    // 화면 연출 상태만 소비한다. 엔진 진행, 보상, 기록, 사망 판정은 다시 호출하지 않는다.
    // 그림 선노출을 실제로 보여 주고 넘어간 뒤에만 "표시함"으로 저장한다. 결정 화면이 뜨자마자 저장하면
    // 타이핑 중에 앱이 꺼진 경우도 표시한 것으로 남아, 다시 열었을 때 그림을 못 보고 지나간다.
    LaunchedEffect(introKey, previewDone) {
        if (previewDone && outcome == null) onEventImageShown(decision.eventId)
    }

    LaunchedEffect(outcome, resultDisplayId, state.presentation.resultAnimation, reduceMotion) {
        if (
            outcome != null &&
            resultDisplayId != null &&
            state.presentation.resultAnimation == ResultAnimationStatus.PENDING
        ) {
            if (!reduceMotion && outcome.changes.isNotEmpty()) {
                delay(CHANGE_HOLD_MILLIS)
            }
            onResultAnimationConsumed(resultDisplayId)
        }
    }

    LaunchedEffect(introKey) {
        if (introDone) return@LaunchedEffect
        if (decision.isIncident) {
            if (!reduceMotion) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val jitter = Random(5)
                animate(0f, 1f, animationSpec = tween(DISTORT_MILLIS, easing = LinearEasing)) { p, _ ->
                    val envelope = distortEnvelope(p)
                    motion = FilmMotion(
                        distortion = DISTORT_MAX_DP * envelope,
                        phase = p * DISTORT_MILLIS * 0.014f,
                        jitterY = (jitter.nextFloat() - 0.5f) * 5f * envelope,
                    )
                }
                motion = FilmMotion()
            }
            popupClosed.tryReceive()
            popupOpen = true
            popupProgress.snapTo(0f)
            popupProgress.animateTo(1f, tween(if (reduceMotion) 150 else 240, easing = PopupEasing))
            popupClosed.receive()
            popupProgress.animateTo(0f, tween(160))
            popupOpen = false
        }
        introDone = true
    }

    LaunchedEffect(introKey, introDone) {
        if (!introDone || outcome != null) return@LaunchedEffect
        if (reduceMotion) {
            typedChars = textLength
            return@LaunchedEffect
        }
        // 매번 지금 값을 다시 읽는다. 터치로 전문을 채우면 바로 끝나고, 작은 값으로 다시 덮어쓰지 않는다.
        while (typedChars < textLength) {
            // 이모지처럼 두 칸을 쓰는 글자는 한 번에 넘겨 반쪽 글자가 보이지 않게 한다.
            typedChars = (typedChars + Character.charCount(decision.text.codePointAt(typedChars))).coerceAtMost(textLength)
            delay(TYPE_MILLIS)
        }
    }

    LaunchedEffect(introKey, textComplete, previewDone) {
        if (!textComplete || previewDone || previewing || outcome != null) return@LaunchedEffect
        delay(PREVIEW_DELAY_MILLIS)
        previewing = true
    }

    LaunchedEffect(introKey, previewing) {
        if (!previewing) return@LaunchedEffect
        delay(PREVIEW_HOLD_MILLIS)
        endPreview()
    }

    LaunchedEffect(swallowTaps) {
        if (swallowTaps) {
            delay(TAP_GUARD_MILLIS)
            swallowTaps = false
        }
    }

    // 본문 스크롤은 사건마다 새로 시작하고, 장면 보기에서 돌아와도 위치를 지킨다.
    val contentScroll = rememberSaveable(introKey, saver = ScrollState.Saver) { ScrollState(0) }
    // 선노출 뒤 선택지가 처음 나타나면 첫 카드가 다 보일 만큼만 한 번 스크롤한다(이미 보이면 움직이지 않는다).
    // 작은 화면 · 큰 글씨에서 선택지가 첫 화면 아래에 있어 카드 A 윗부분만 보이던 문제(2026-11-09).
    // 선택지 묶음 전체를 맞추면 긴 본문에서 "장면 보기" 버튼이 위로 밀려나므로 첫 카드까지만 맞춘다.
    // 복원 때는 다시 스크롤하지 않는다(스크롤 위치도 복원된다).
    val choicesInView = remember(introKey) { BringIntoViewRequester() }
    var choicesRevealed by rememberSaveable(introKey) { mutableStateOf(previewDone) }
    LaunchedEffect(introKey, previewDone, outcome == null) {
        if (!previewDone || outcome != null || choicesRevealed) return@LaunchedEffect
        choicesRevealed = true
        withFrameNanos { } // 선택지가 한 번 배치된 뒤에 잰다.
        choicesInView.bringIntoView()
    }

    BackHandler(enabled = popupOpen) { popupClosed.trySend(Unit) }
    BackHandler(enabled = sceneOnly && !popupOpen) { sceneOnly = false }
    BackHandler(enabled = previewing && !popupOpen) { endPreview() }
    val sceneClear = sceneOnly || previewing
    // 그림 선노출에 들어갈 때만 블러가 서서히 풀린다. 장면 보기 · 복귀는 이전처럼 바로 바뀐다.
    val blur by animateDpAsState(
        targetValue = if (sceneClear) 0.dp else 8.dp,
        animationSpec = tween(if (previewing && !reduceMotion) 350 else 0),
        label = "sceneBlur",
    )

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LifeGameInk)
            // 본문이 나오는 동안의 터치는 어디를 눌러도 본문 완성만 한다(선택 · 장면 보기 · 진행으로 가지 않는다).
            .completeTextOnTap(enabled = typing || swallowTaps, onTap = completeText),
    ) {
        SceneImage(
            shot = FilmShot(
                key = introKey,
                imageId = decision.imageId,
                imageFile = decision.imageFile,
                candidates = decision.images,
                isSpecial = decision.kind == DecisionKind.BIG_INCIDENT,
            ),
            modifier = Modifier
                .fillMaxSize()
                // 배경만 흐리게 한다. 사건 글자와 선택지는 그 위에 선명하게 얹힌다.
                .blur(blur)
                .testTag(if (sceneClear) "scene_clear" else "scene_blurred"),
            // 장면 보기에서도 화면 전체를 채운다(위아래 여백 없음). 블러만 풀린다.
            fitWholeImage = false,
        )
        if (previewing) {
            ScenePreview(title = decision.title, onDone = endPreview)
        } else if (sceneOnly) {
            // 장면 보기: 그림만 남기고 모든 UI를 숨긴다. 한 번 누르면 그대로 돌아온다.
            Box(
                Modifier
                    .fillMaxSize()
                    .testTag("scene_only")
                    .semantics { contentDescription = "장면 보기 중. 누르면 사건 화면으로 돌아갑니다" }
                    .pointerInput(Unit) { detectTapGestures { sceneOnly = false } },
            )
        } else {
            // 밝은 그림에서도 흰 글자가 읽히도록 전체를 한 겹 어둡게 덮는다.
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.52f),
                            0.45f to Color.Black.copy(alpha = 0.38f),
                            1f to Color.Black.copy(alpha = 0.48f),
                        ),
                    ),
            )
            Column(Modifier.fillMaxSize()) {
                PlayTopBar(
                    health = state.stats.health,
                    onOpenDetails = { detailsOpen = true },
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
                    avatar = { avatarModifier -> PlayerPortrait(state.avatar, avatarModifier) },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .verticalScroll(contentScroll)
                        .testTag("decision_content"),
                ) {
                    if (outcome == null) {
                        NarrativeReveal(
                            decision = decision,
                            typedText = decision.text.take(typedChars),
                            ready = textComplete && previewDone,
                            animateMark = !reduceMotion,
                            onReveal = { sceneOnly = true },
                            onCompleteText = completeText.takeIf { typing },
                        )
                    } else {
                        DecisionText(decision = decision, onReveal = { sceneOnly = true })
                    }
                    // 선택지는 사건 그림을 먼저 본 뒤에 나온다. 결과 화면은 그대로.
                    if (outcome != null || previewDone) {
                        DecisionSheet(
                            firstChoiceModifier = Modifier.bringIntoViewRequester(choicesInView),
                            decision = decision,
                            outcome = outcome,
                            enabled = introDone && chosenId == null,
                            chosenId = chosenId,
                            showChoiceDetails = showChoiceDetails,
                            onToggleDetails = { showChoiceDetails = !showChoiceDetails },
                            onChoose = { id ->
                                // 같은 화면에서 두 번 눌러도 한 번만 넘긴다(VM도 결과가 있으면 무시한다).
                                if (chosenId == null) {
                                    chosenId = id
                                    onChoose(id)
                                }
                            },
                            onContinue = onContinue,
                            changeKey = introKey,
                            // 저장된 연출 상태상 이미 끝난 결과 칩은 앱을 다시 열어도 다시 띄우지 않는다.
                            resultChangesShownBefore = resultDisplayId != null &&
                                state.presentation.resultAnimation == ResultAnimationStatus.CONSUMED,
                            reduceMotion = reduceMotion,
                        )
                    }
                }
                PeriodTrack(
                    marks = state.periodMarks,
                    yearLabel = "${state.yearInPeriod}번째 해",
                    modifier = Modifier
                        .background(LifeGamePaper)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                        ),
                )
            }
        }

        if (popupOpen) {
            IncidentPopup(
                decision = decision,
                progress = { popupProgress.value },
                onOpen = { popupClosed.trySend(Unit) },
            )
        }
    }

    if (detailsOpen) {
        LifeFactsSheet(
            facts = state.facts,
            stats = state.stats,
            onDismiss = { detailsOpen = false },
            place = state.place,
            focus = state.focus,
            showFocus = true,
        )
    }
}

@Composable
private fun NarrativeReveal(
    decision: DecisionUi,
    typedText: String,
    ready: Boolean,
    animateMark: Boolean,
    onReveal: () -> Unit,
    onCompleteText: (() -> Unit)? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DecisionKindMark(kind = decision.kind, animate = animateMark)
        Text(
            text = decision.title,
            color = LifeGamePaper,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 35.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = OnSceneShadow),
            modifier = Modifier.semantics { heading() }.testTag("decision_title"),
        )
        // 글자가 다 나온 뒤의 높이를 미리 잡아 두어 선택지가 위아래로 흔들리지 않게 한다.
        Box(contentAlignment = Alignment.TopCenter) {
            Text(
                text = decision.text,
                fontFamily = LifeGameSerif,
                fontSize = 17.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Center,
                color = Color.Transparent,
                modifier = Modifier.clearAndSetSemantics { },
            )
            Text(
                text = typedText,
                color = LifeGamePaper,
                fontFamily = LifeGameSerif,
                fontSize = 17.sp,
                lineHeight = 27.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = OnSceneShadow),
                modifier = Modifier
                    .testTag("decision_body")
                    .semantics {
                        // 스크린리더 사용자도 본문을 바로 끝까지 볼 수 있다.
                        if (onCompleteText != null) {
                            onClick(label = "본문 전체 보기") { onCompleteText(); true }
                        }
                    },
            )
        }
        if (ready) SceneViewButton(onClick = onReveal, enabled = true)
    }
}

/**
 * 사건 그림 선노출. 블러를 풀고 UI를 숨긴 채 아래에 제목과 안내만 둔다.
 * 시간이 지나거나 한 번 누르면 선택지로 넘어간다.
 */
@Composable
private fun ScenePreview(title: String, onDone: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .testTag("scene_preview")
            .semantics { contentDescription = "$title 장면. 누르면 선택지로 넘어갑니다" }
            .pointerInput(Unit) { detectTapGestures { onDone() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(start = 22.dp, end = 22.dp, top = 28.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                color = LifeGamePaper,
                fontFamily = LifeGameSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                style = TextStyle(shadow = OnSceneShadow),
            )
            Text(
                text = "누르면 선택지로",
                color = LifeGamePaper.copy(alpha = 0.85f),
                fontSize = 14.sp,
                style = TextStyle(shadow = OnSceneShadow),
            )
        }
    }
}

/** 결과를 보는 동안의 사건 머리. 분류 · 제목 · 본문을 가운데 정렬로 선명하게 둔다. */
@Composable
private fun DecisionText(decision: DecisionUi, onReveal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DecisionKindMark(kind = decision.kind, animate = false)
        Text(
            text = decision.title,
            color = LifeGamePaper,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 31.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = OnSceneShadow),
            modifier = Modifier.semantics { heading() }.testTag("decision_title"),
        )
        Text(
            text = decision.text,
            color = LifeGamePaper,
            fontFamily = LifeGameSerif,
            fontSize = 16.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center,
            style = TextStyle(shadow = OnSceneShadow),
            modifier = Modifier.testTag("decision_body"),
        )
        SceneViewButton(onClick = onReveal)
    }
}

@Composable
private fun DecisionSheet(
    firstChoiceModifier: Modifier = Modifier,
    decision: DecisionUi,
    outcome: OutcomeUi?,
    enabled: Boolean,
    chosenId: String?,
    showChoiceDetails: Boolean,
    onToggleDetails: () -> Unit,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit,
    changeKey: String,
    resultChangesShownBefore: Boolean = false,
    reduceMotion: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (outcome == null) {
            val hasDetails = decision.choices.any { it.statTags.isNotEmpty() }
            if (hasDetails) {
                ChoiceDetailsToggle(expanded = showChoiceDetails, onToggle = onToggleDetails)
            }
            decision.choices.forEachIndexed { index, choice ->
                DecisionChoiceCard(
                    modifier = if (index == 0) firstChoiceModifier else Modifier,
                    choice = choice,
                    enabled = enabled && choice.available,
                    showDetails = showChoiceDetails,
                    chosen = chosenId == choice.choiceId,
                    dimmed = chosenId != null && chosenId != choice.choiceId,
                    onClick = { onChoose(choice.choiceId) },
                )
            }
        } else {
            OutcomeBlock(outcome, changeKey = changeKey, reduceMotion = reduceMotion, changesShownBefore = resultChangesShownBefore)
            InkButton(
                text = if (outcome.endsLife) "인생 정리하기" else "계속하기",
                onClick = onContinue,
            )
        }
    }
}

/** 능력치 영향(사회성 · 행복 같은 보조 태그)은 기본으로 접어 두고 필요할 때만 펼친다 (2026-11-06 요구 7). */
@Composable
private fun ColumnScope.ChoiceDetailsToggle(expanded: Boolean, onToggle: () -> Unit) {
    Text(
        text = if (expanded) "능력치 영향 접기" else "능력치 영향 보기",
        color = LifeGamePaper.copy(alpha = 0.78f),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .align(Alignment.End)
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClickLabel = if (expanded) "능력치 영향 접기" else "능력치 영향 펼치기", onClick = onToggle)
            .heightIn(min = 40.dp)
            .padding(horizontal = 8.dp, vertical = 10.dp)
            .testTag("choice_details_toggle"),
    )
}

/**
 * 어두운 유리 카드. ABC 표식은 카드 왼쪽 위 모서리에 딱 붙은 한 조각이다(따로 떠 있는 동그라미가 아니다).
 * 숨은 결과의 문장 · 확률은 쓰지 않고, 잠긴 선택지는 실제 조건만 보인다.
 * 제목(행동)이 가장 크고, 가능성 · 건강 변화는 한 줄 보조 정보로 약하게 둔다. 능력치 태그는 펼쳤을 때만 보인다.
 * 누르는 동안 카드가 밝아지고, 고른 카드는 테두리가 진해지며 나머지는 흐려진다.
 */
@Composable
private fun DecisionChoiceCard(
    modifier: Modifier = Modifier,
    choice: DecisionChoiceUi,
    enabled: Boolean,
    showDetails: Boolean,
    chosen: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val textAlpha = when {
        !choice.available -> 0.72f
        dimmed -> 0.55f
        else -> 1f
    }
    val summary = listOfNotNull(choice.likelihood, choice.healthEffectText?.let(::quietHealthText))
        .joinToString(" · ")
    val fullDescription = buildList {
        add("선택지 ${choice.key}. ${choice.label}")
        choice.likelihood?.let(::add)
        if (choice.statTags.isNotEmpty()) add("능력치 영향 ${choice.statTags.joinToString(", ")}")
        choice.healthEffectText?.let(::add)
    }.map { it.trimEnd().removeSuffix(".") }.joinToString(". ") // "잡는다.. 능력치"처럼 마침표가 겹치지 않게
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .graphicsLayer {
                val scale = if (pressed && enabled) 0.985f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(if (pressed && enabled) ChoiceGlassPressed else ChoiceGlass)
            .border(
                width = if (chosen) 2.dp else 1.dp,
                color = LifeGamePaper.copy(alpha = if (chosen || (pressed && enabled)) 0.85f else 0.30f),
                shape = shape,
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = "이 선택지로 결정",
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = fullDescription
                if (!choice.available) stateDescription = "잠김"
                if (chosen) stateDescription = "선택함"
            }
            .testTag("decision_choice_${choice.key}"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 46.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = choice.label,
                    color = LifeGamePaper.copy(alpha = textAlpha),
                    fontSize = 17.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("choice_label_${choice.key}"),
                )
                if (summary.isNotBlank() && choice.available) {
                    Text(
                        text = summary,
                        color = LifeGamePaper.copy(alpha = 0.68f * textAlpha),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("choice_summary_${choice.key}"),
                    )
                }
                if (showDetails && choice.statTags.isNotEmpty()) {
                    Text(
                        text = "영향: ${choice.statTags.joinToString(" · ")}",
                        color = LifeGamePaper.copy(alpha = 0.6f * textAlpha),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.testTag("choice_tags_${choice.key}"),
                    )
                }
                // 잠긴 선택지는 아래 잠금 이유(필요 값 · 지금 값)에 조건이 이미 나오므로 겹쳐 쓰지 않는다.
                if (choice.statConditionText != null && choice.available) {
                    Text("조건: ${choice.statConditionText}", color = LifeGamePaper.copy(alpha = 0.72f * textAlpha), fontSize = 13.sp, lineHeight = 18.sp)
                }
                if (!choice.available) {
                    val reasons = choice.lockReasons.ifEmpty { listOfNotNull(choice.lockReason) }
                    reasons.forEach { reason ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("lock_reason"),
                        ) {
                            LockIcon(color = LifeGamePaper.copy(alpha = 0.86f))
                            Text(
                                text = reason,
                                color = LifeGamePaper.copy(alpha = 0.92f),
                                fontSize = 14.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
            ChoiceArrow(color = LifeGamePaper.copy(alpha = 0.9f * textAlpha))
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(width = 36.dp, height = 32.dp)
                .clip(RoundedCornerShape(bottomEnd = 10.dp))
                .background(LifeGamePaper.copy(alpha = if (chosen) 0.5f else 0.24f))
                .testTag("choice_badge_${choice.key}"),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = choice.key,
                color = LifeGamePaper,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** "건강 +10 회복" → "건강 +10". 색을 빼고 글자만 남겨 선택지 본문보다 약하게 둔다. */
private fun quietHealthText(text: String): String =
    text.removeSuffix(" 회복").removeSuffix(" 감소").replace("-", "−")

private val ChoiceGlass = Color(0xFF14110F).copy(alpha = 0.62f)
private val ChoiceGlassPressed = Color(0xFF2A241F).copy(alpha = 0.78f)

/** 오른쪽 화살표. 글꼴 크기에 따라 흔들리지 않게 직접 그린다. */
@Composable
private fun ChoiceArrow(color: Color) {
    Canvas(Modifier.size(width = 12.dp, height = 20.dp)) {
        val stroke = 2.4.dp.toPx()
        drawLine(color, Offset(size.width * 0.2f, size.height * 0.15f), Offset(size.width * 0.85f, size.height * 0.5f), stroke)
        drawLine(color, Offset(size.width * 0.85f, size.height * 0.5f), Offset(size.width * 0.2f, size.height * 0.85f), stroke)
    }
}

@Composable
private fun OutcomeBlock(outcome: OutcomeUi, changeKey: String, reduceMotion: Boolean, changesShownBefore: Boolean = false) {
    val shape = RoundedCornerShape(12.dp)
    Box(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(LifeGamePaper.copy(alpha = 0.97f))
                .border(1.dp, LifeGameRule, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag("decision_outcome"),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = outcome.choiceLabel,
                color = LifeGameInkSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = outcome.text,
                style = ResultStoryStyle,
                color = LifeGameInk,
                fontSize = 16.5.sp,
                lineHeight = 25.sp,
                modifier = Modifier.testTag("outcome_text"),
            )
            if (outcome.changes.isEmpty()) {
                Text("능력치 변화 없음", color = LifeGameInkTertiary, fontSize = 14.sp)
            } else if (reduceMotion) {
                // 동작 줄이기: 움직이지 않고 바로 읽을 수 있는 고정 문구로 대신한다.
                EffectText(outcome.changes, fontSize = 14.sp, showAmount = true, modifier = Modifier.testTag("outcome_stat_changes"))
            }
            if (outcome.factChanges.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().testTag("outcome_fact_changes"),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text("새로 생긴 사실", color = LifeGameInkSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    outcome.factChanges.forEach { change ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(change.category, color = LifeGameInkTertiary, fontSize = 14.sp, modifier = Modifier.widthIn(min = 44.dp))
                            Text(
                                text = change.text,
                                color = LifeGameInk,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            if (outcome.openedEvents.isNotEmpty()) {
                Text(
                    text = "새로 열린 길: " + outcome.openedEvents.joinToString(", ") { "「$it」" },
                    color = LifeGameInkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.testTag("outcome_opened_events"),
                )
            }
        }
        if (outcome.changes.isNotEmpty() && !reduceMotion) {
            FloatingChanges(
                changes = outcome.changes,
                key = changeKey,
                shownBefore = changesShownBefore,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
}

/**
 * 선택 직후 결과 카드 위에 능력치 변화가 잠깐 떠올랐다가 사라진다 (2026-11-06 요구 9).
 * 순수 표시용이라 반영은 VM에서 이미 한 번 끝났다. 같은 결과에서는 다시 재생하지 않는다(복원 포함).
 * 스크린리더에는 나타날 때 한 번 읽힌다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingChanges(changes: List<StatChangeUi>, key: String, shownBefore: Boolean = false, modifier: Modifier = Modifier) {
    var played by rememberSaveable(key) { mutableStateOf(shownBefore) }
    val visible = remember(key) { MutableTransitionState(false) }
    LaunchedEffect(key) {
        if (played) return@LaunchedEffect
        // 시작하자마자 재생함으로 남긴다: 떠 있는 도중 화면이 다시 만들어져도(회전 등) 처음부터 다시 띄우지 않는다.
        played = true
        visible.targetState = true
        delay(CHANGE_HOLD_MILLIS)
        visible.targetState = false
    }
    val spoken = "능력치 변화: " + changes.joinToString(", ") { "${it.name} ${signed(it.delta)}" }
    AnimatedVisibility(
        visibleState = visible,
        // 칩 묶음의 아래쪽 12dp만 결과 카드 위에 걸치고 나머지는 카드 위로 띄운다. 작은 화면 · 큰 글씨에서
        // 칩이 두 줄이 돼도 카드의 선택 문구를 덮지 않는다(한 줄이면 예전처럼 24dp쯤 위).
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val overlap = 12.dp.roundToPx()
                layout(placeable.width, placeable.height) { placeable.place(0, -(placeable.height - overlap)) }
            },
        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { it / 2 },
        exit = fadeOut(tween(320)) + slideOutVertically(tween(320)) { -it / 2 },
    ) {
        // 칩이 한 줄에 다 들어가지 않으면 다음 줄로 넘긴다(한 칩을 좁혀 글자가 세로로 꺾이지 않게).
        FlowRow(
            Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = spoken
                }
                .testTag("outcome_floating_changes"),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            changes.forEach { change ->
                val up = change.delta > 0
                Text(
                    text = "${change.name} ${signed(change.delta)}",
                    color = if (up) LifeGameStatUp else LifeGameStatDown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (up) LifeGameStatUpSoft else LifeGameStatDownSoft)
                        .border(1.dp, (if (up) LifeGameStatUp else LifeGameStatDown).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        // 칩 하나하나는 읽지 않는다(묶음 설명으로 한 번). 테스트 표시만 남긴다.
                        .clearAndSetSemantics { testTag = "outcome_change_chip" },
                )
            }
        }
    }
}

private fun signed(delta: Int): String = if (delta > 0) "+$delta" else "−${-delta}"

/** 돌발 팝업. 머리에 필름 구멍 띠를 두른 카드가 어두워진 화면 위로 올라온다. */
@Composable
private fun IncidentPopup(decision: DecisionUi, progress: () -> Float, onOpen: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind { drawRect(LifeGameInk.copy(alpha = 0.45f * progress())) }
                .pointerInput(Unit) { detectTapGestures { } },
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 28.dp, vertical = 24.dp)
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = progress()
                    alpha = p
                    translationY = (1f - p) * 12.dp.toPx()
                }
                .clip(RoundedCornerShape(10.dp))
                .background(LifeGamePaper)
                .verticalScroll(rememberScrollState())
                .testTag("incident_popup"),
        ) {
            PerforationBand(
                modifier = Modifier.fillMaxWidth().height(17.dp),
                holeWidth = 8.dp,
                holeHeight = 5.dp,
                pitch = 15.dp,
            )
            Column(
                Modifier.padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(decision.kind.markLabel(), style = MarkLabelStyle, color = LifeGameSpecial)
                Text(
                    text = decision.title,
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                )
                Text(
                    text = decision.text.teaser(),
                    color = LifeGameBodyInk,
                    fontFamily = LifeGameSerif,
                    fontSize = 15.5.sp,
                    lineHeight = 25.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                InkButton(text = "사건 보기", onClick = onOpen)
            }
        }
    }
}
