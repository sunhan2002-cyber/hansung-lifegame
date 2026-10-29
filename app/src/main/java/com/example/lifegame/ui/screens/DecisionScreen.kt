package com.example.lifegame.ui.screens

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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.DecisionUi
import com.example.lifegame.ui.OutcomeUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.avatar.PlayerAvatar
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

/**
 * 결정 · 돌발 화면 (10주차 최종 시안).
 * 위: 투명 배경의 캐릭터와 두꺼운 건강 게이지. 가운데: 사건 분류 글자 효과 · 제목 · 본문(가운데 정렬).
 * 아래: 선택지(어두운 유리 카드)와 -O-O-O- 시기선. 배경 그림만 흐리게 하고 글자는 선명하게 둔다.
 * 장면 보기는 배경 블러를 풀고 모든 UI를 숨긴 뒤, 한 번 누르면 그대로 돌아온다.
 */
@Composable
fun DecisionScreen(
    state: PeriodPlayUiState,
    onChoose: (choiceId: String) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val decision = state.decision ?: return
    val outcome = state.outcome
    val introKey = "${decision.eventId}:${state.yearKey}"
    var introDone by rememberSaveable(introKey) { mutableStateOf(outcome != null) }
    var sceneOnly by rememberSaveable(introKey) { mutableStateOf(false) }
    var typedChars by rememberSaveable(introKey) { mutableStateOf(if (outcome != null) decision.text.length else 0) }
    var detailsOpen by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf(FilmMotion()) }
    var popupOpen by remember { mutableStateOf(false) }
    val popupProgress = remember { Animatable(0f) }
    val popupClosed = remember { Channel<Unit>(Channel.CONFLATED) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()

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
        if (typedChars >= decision.text.length) return@LaunchedEffect
        if (reduceMotion) {
            typedChars = decision.text.length
        } else {
            for (index in typedChars until decision.text.length) {
                typedChars = index + 1
                delay(18)
            }
        }
    }

    BackHandler(enabled = popupOpen) { popupClosed.trySend(Unit) }
    BackHandler(enabled = sceneOnly && !popupOpen) { sceneOnly = false }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LifeGameInk),
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
                .blur(if (sceneOnly) 0.dp else 8.dp)
                .testTag(if (sceneOnly) "scene_clear" else "scene_blurred"),
            fitWholeImage = sceneOnly,
        )
        if (sceneOnly) {
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
                    avatar = { avatarModifier -> PlayerAvatar(state.avatar, avatarModifier) },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                        .verticalScroll(rememberScrollState())
                        .testTag("decision_content"),
                ) {
                    if (outcome == null) {
                        NarrativeReveal(
                            decision = decision,
                            typedText = decision.text.take(typedChars),
                            ready = introDone && typedChars >= decision.text.length,
                            animateMark = !reduceMotion,
                            onReveal = { sceneOnly = true },
                        )
                    } else {
                        DecisionText(decision = decision, onReveal = { sceneOnly = true })
                    }
                    DecisionSheet(
                        decision = decision,
                        outcome = outcome,
                        enabled = introDone,
                        onChoose = onChoose,
                        onContinue = onContinue,
                    )
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
                modifier = Modifier.testTag("decision_body"),
            )
        }
        SceneViewButton(onClick = onReveal, enabled = ready)
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
    decision: DecisionUi,
    outcome: OutcomeUi?,
    enabled: Boolean,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (outcome == null) {
            decision.choices.forEach { choice ->
                DecisionChoiceCard(
                    choice = choice,
                    enabled = enabled && choice.available,
                    onClick = { onChoose(choice.choiceId) },
                )
            }
        } else {
            OutcomeBlock(outcome)
            InkButton(
                text = if (outcome.endsLife) "인생 정리하기" else "계속하기",
                onClick = onContinue,
            )
        }
    }
}

/**
 * 어두운 유리 카드. ABC 표식은 카드 왼쪽 위 모서리에 딱 붙은 한 조각이다(따로 떠 있는 동그라미가 아니다).
 * 숨은 결과의 문장 · 확률은 쓰지 않고, 잠긴 선택지는 실제 조건만 보인다.
 */
@Composable
private fun DecisionChoiceCard(
    choice: DecisionChoiceUi,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val textAlpha = if (choice.available) 1f else 0.72f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(ChoiceGlass)
            .border(1.dp, LifeGamePaper.copy(alpha = 0.30f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                if (!choice.available) stateDescription = "잠김"
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = choice.label,
                    color = LifeGamePaper.copy(alpha = textAlpha),
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (choice.hint.isNotBlank()) {
                    Text(choice.hint, color = LifeGamePaper.copy(alpha = 0.82f * textAlpha), fontSize = 14.sp, lineHeight = 19.sp)
                }
                // 잠긴 선택지는 아래 잠금 이유(필요 값 · 지금 값)에 조건이 이미 나오므로 겹쳐 쓰지 않는다.
                if (choice.statConditionText != null && choice.available) {
                    Text("조건: ${choice.statConditionText}", color = LifeGamePaper.copy(alpha = 0.82f), fontSize = 14.sp, lineHeight = 19.sp)
                }
                if (choice.healthEffectText != null) {
                    Text(
                        text = choice.healthEffectText,
                        color = if (choice.healthEffectText.contains("감소")) Color(0xFFFF9B8F) else Color(0xFF9FD8B0),
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Medium,
                    )
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
                .background(LifeGamePaper.copy(alpha = 0.24f))
                .semantics { contentDescription = "선택지 ${choice.key}" }
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

private val ChoiceGlass = Color(0xFF14110F).copy(alpha = 0.62f)

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
private fun OutcomeBlock(outcome: OutcomeUi) {
    val shape = RoundedCornerShape(12.dp)
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
        } else {
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
}

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
