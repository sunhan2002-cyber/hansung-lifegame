package com.example.lifegame.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.lifegame.domain.model.DecisionKind
import com.example.lifegame.ui.DecisionChoiceUi
import com.example.lifegame.ui.DecisionUi
import com.example.lifegame.ui.OutcomeUi
import com.example.lifegame.ui.PeriodPlayUiState
import com.example.lifegame.ui.components.DISTORT_MAX_DP
import com.example.lifegame.ui.components.DISTORT_MILLIS
import com.example.lifegame.ui.components.EffectText
import com.example.lifegame.ui.components.FilmMotion
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.FilmStrip
import com.example.lifegame.ui.components.FocusLockRow
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.components.LifeGauge
import com.example.lifegame.ui.components.PerforationBand
import com.example.lifegame.ui.components.PeriodHeader
import com.example.lifegame.ui.components.PixelFigurePlaceholder
import com.example.lifegame.ui.components.SceneImage
import com.example.lifegame.ui.components.StatsSheet
import com.example.lifegame.ui.components.distortEnvelope
import com.example.lifegame.ui.components.rememberReduceMotion
import com.example.lifegame.ui.teaser
import com.example.lifegame.ui.theme.EventTitleStyle
import com.example.lifegame.ui.theme.LifeGameBodyInk
import com.example.lifegame.ui.theme.LifeGameCard
import com.example.lifegame.ui.theme.LifeGameInk
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGameInkTertiary
import com.example.lifegame.ui.theme.LifeGameMuted
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameRule
import com.example.lifegame.ui.theme.LifeGameSerif
import com.example.lifegame.ui.theme.LifeGameSpecial
import com.example.lifegame.ui.theme.LifeGameStatUp
import com.example.lifegame.ui.theme.MarkLabelStyle
import com.example.lifegame.ui.theme.ResultStoryStyle
import com.example.lifegame.ui.theme.StoryStyle
import kotlinx.coroutines.channels.Channel
import kotlin.random.Random

private val PopupEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

/**
 * 시안 8-3 결정 · 돌발. 자동 진행이 멈추고 1인칭 도트 장면이 필름에 걸린다.
 * 돌발이면 필름이 왜곡된 뒤 팝업이 뜬다 (R-23). 선택지에는 숫자 대신 가능성 문구만 보인다 (R-43).
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
    var statsOpen by remember { mutableStateOf(false) }
    var motion by remember { mutableStateOf(FilmMotion()) }
    var popupOpen by remember { mutableStateOf(false) }
    val popupProgress = remember { Animatable(0f) }
    val popupClosed = remember { Channel<Unit>(Channel.CONFLATED) }
    val contentAlpha = remember { Animatable(if (introDone) 1f else 0f) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(introKey) {
        if (introDone) {
            contentAlpha.snapTo(1f)
            return@LaunchedEffect
        }
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
        contentAlpha.animateTo(1f, tween(if (reduceMotion) 0 else 180))
    }

    LaunchedEffect(introKey, introDone) {
        if (!introDone || outcome != null) return@LaunchedEffect
        typedChars = 0
        if (reduceMotion) {
            typedChars = decision.text.length
        } else {
            decision.text.forEachIndexed { index, _ ->
                typedChars = index + 1
                delay(18)
            }
        }
    }

    BackHandler(enabled = popupOpen) { popupClosed.trySend(Unit) }

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
                isSpecial = decision.kind == DecisionKind.BIG_INCIDENT,
            ),
            modifier = Modifier
                .fillMaxSize()
                // 장면 보기 전에는 배경만 살짝 흐리게 한다. 텍스트는 별도 패널에서 선명하게 읽힌다.
                .blur(if (sceneOnly) 0.dp else 8.dp),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(LifeGameInk.copy(alpha = if (sceneOnly) 0.04f else 0.30f)),
        )
        if (sceneOnly) {
            Box(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { sceneOnly = false } },
            )
        } else {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                PeriodHeader(
                    periodName = state.periodName,
                    yearLabel = "${state.yearInPeriod}번째 해",
                    marks = state.periodMarks,
                    stats = state.stats,
                    onStatsClick = { statsOpen = true },
                )
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .testTag("decision_content"),
                ) {
                    state.focus?.let { FocusLockRow(focus = it, years = state.periodYears) }
                    if (outcome == null) {
                        NarrativeReveal(
                            decision = decision,
                            typedText = decision.text.take(typedChars),
                            ready = introDone && typedChars >= decision.text.length,
                            onReveal = { sceneOnly = true },
                        )
                    } else {
                        DecisionText(decision = decision, dimmed = true)
                    }
                    DecisionSheet(
                        decision = decision,
                        outcome = outcome,
                        enabled = introDone,
                        choicesBlurred = false,
                        onChoose = onChoose,
                        onContinue = onContinue,
                    )
                }
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

    if (statsOpen) StatsSheet(stats = state.stats, onDismiss = { statsOpen = false })
}

private fun DecisionKind.markLabel(): String = when (this) {
    DecisionKind.MILESTONE -> "인생의 전환점"
    DecisionKind.INCIDENT -> "돌발사건"
    DecisionKind.BIG_INCIDENT -> "돌발선택"
}

@Composable
private fun NarrativeReveal(
    decision: DecisionUi,
    typedText: String,
    ready: Boolean,
    onReveal: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = decision.kind.markLabel(),
            color = LifeGameSpecial,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = decision.title,
            color = LifeGamePaper,
            fontFamily = LifeGameSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 29.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Center,
        )
        Text(
            text = typedText,
            color = LifeGamePaper,
            fontFamily = LifeGameSerif,
            fontSize = 17.sp,
            lineHeight = 27.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        InkButton(
            text = "장면 보기",
            onClick = onReveal,
            enabled = ready,
            modifier = Modifier.fillMaxWidth(0.72f),
            compact = true,
        )
    }
}

@Composable
private fun DecisionText(decision: DecisionUi, dimmed: Boolean, showBody: Boolean = true) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = decision.kind.markLabel(),
            style = MarkLabelStyle,
            color = if (decision.isIncident) LifeGameSpecial else LifeGamePaper,
        )
        Text(
            text = decision.title,
            style = EventTitleStyle,
            color = LifeGamePaper,
            modifier = Modifier.semantics { heading() },
        )
        if (showBody) {
            Text(
                text = decision.text,
                style = StoryStyle,
                color = LifeGamePaper,
            )
        }
    }
}

@Composable
private fun DecisionSheet(
    decision: DecisionUi,
    outcome: OutcomeUi?,
    enabled: Boolean,
    choicesBlurred: Boolean = false,
    onChoose: (String) -> Unit,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (outcome == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("어떻게 할까", color = LifeGameInkTertiary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("결과 가능성", color = LifeGameInkTertiary, fontSize = 12.sp)
            }
            decision.choices.forEach { choice ->
                DecisionChoiceCard(
                    choice = choice,
                    enabled = enabled && choice.available,
                    blurred = choicesBlurred,
                    onClick = { onChoose(choice.choiceId) },
                )
            }
        } else {
            OutcomeBlock(outcome)
            Spacer(Modifier.height(4.dp))
            InkButton(
                text = if (outcome.endsLife) "인생 정리하기" else "계속하기",
                onClick = onContinue,
            )
        }
    }
}

@Composable
private fun DecisionChoiceCard(
    choice: DecisionChoiceUi,
    enabled: Boolean,
    blurred: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(shape)
            .background(LifeGameInk.copy(alpha = 0.52f))
            .border(1.dp, LifeGamePaper.copy(alpha = 0.28f), shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .testTag("decision_choice_${choice.key}"),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 46.dp, end = 14.dp, top = 13.dp, bottom = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = choice.label,
                    color = LifeGamePaper,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
                if (choice.hint.isNotBlank()) {
                    Text(choice.hint, color = LifeGamePaper.copy(alpha = 0.78f), fontSize = 11.5.sp, lineHeight = 15.5.sp)
                }
                if (choice.statConditionText != null) {
                    Text("조건: ${choice.statConditionText}", color = if (choice.available) LifeGamePaper.copy(alpha = 0.78f) else LifeGamePaper.copy(alpha = 0.5f), fontSize = 11.5.sp, lineHeight = 15.5.sp)
                }
                if (choice.healthEffectText != null) {
                    Text(choice.healthEffectText, color = if (choice.healthEffectText.contains("감소")) LifeGameSpecial else LifeGameStatUp, fontSize = 11.5.sp, lineHeight = 15.5.sp, fontWeight = FontWeight.Medium)
                }
                if (!choice.available && choice.lockReason != null) {
                    Text("🔒 ${choice.lockReason}", color = LifeGamePaper.copy(alpha = 0.6f), fontSize = 11.5.sp, lineHeight = 15.5.sp)
                }
            }
            Text("›", color = LifeGamePaper.copy(alpha = 0.86f), fontSize = 32.sp, lineHeight = 32.sp, fontWeight = FontWeight.Light)
        }
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(width = 34.dp, height = 30.dp)
                .clip(RoundedCornerShape(bottomEnd = 7.dp))
                .background(LifeGameInk),
            contentAlignment = Alignment.Center,
        ) {
            Text(choice.key, color = LifeGamePaper, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun OutcomeBlock(outcome: OutcomeUi) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LifeGamePaper.copy(alpha = 0.94f))
            .border(1.dp, LifeGameRule, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("decision_outcome"),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = "→ ${outcome.choiceLabel}",
            color = LifeGameInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = outcome.text,
            style = ResultStoryStyle,
            color = LifeGameInk,
            lineHeight = 24.sp,
        )
        // 세부 증감 수치는 능력치 시트에서 확인하고 결과 화면은 서사에 집중한다.
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
