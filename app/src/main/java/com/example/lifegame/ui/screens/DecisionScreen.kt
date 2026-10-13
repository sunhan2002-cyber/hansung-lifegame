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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
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
import com.example.lifegame.ui.components.PerforationBand
import com.example.lifegame.ui.components.PeriodHeader
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

    BackHandler(enabled = popupOpen) { popupClosed.trySend(Unit) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper),
    ) {
        // 선택지가 한 화면에 더 많이 보이도록 자동 진행보다 조금 낮게 잡는다 (넘치면 가운데를 잘라 보여 준다).
        val gateHeight = min((maxWidth - 20.dp) * 0.75f, maxHeight * 0.26f)
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
                    .verticalScroll(rememberScrollState())
                    .testTag("decision_content"),
            ) {
                state.focus?.let { FocusLockRow(focus = it, years = state.periodYears) }
                FilmStrip(
                    current = FilmShot(
                        key = introKey,
                        imageId = decision.imageId,
                        imageFile = decision.imageFile,
                        isSpecial = decision.kind == DecisionKind.BIG_INCIDENT,
                    ),
                    incoming = null,
                    gateHeight = gateHeight,
                    motion = { motion },
                    modifier = Modifier.padding(top = 6.dp),
                )
                Column(Modifier.graphicsLayer { alpha = contentAlpha.value }) {
                    DecisionText(decision = decision, dimmed = outcome != null)
                    DecisionSheet(
                        decision = decision,
                        outcome = outcome,
                        enabled = introDone,
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
    DecisionKind.MILESTONE -> "갈림길"
    DecisionKind.INCIDENT -> "돌발 사건"
    DecisionKind.BIG_INCIDENT -> "큰 돌발"
}

@Composable
private fun DecisionText(decision: DecisionUi, dimmed: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = decision.kind.markLabel(),
            style = MarkLabelStyle,
            color = if (decision.isIncident) LifeGameSpecial else LifeGameInkSecondary,
        )
        Text(
            text = decision.title,
            style = EventTitleStyle,
            color = LifeGameInk,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = decision.text,
            style = StoryStyle,
            color = if (dimmed) LifeGameInkTertiary else LifeGameInk,
        )
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
            .drawBehind {
                drawLine(LifeGameRule, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (outcome == null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                Text("어떻게 할까", color = LifeGameInkTertiary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("결과 가능성", color = LifeGameInkTertiary, fontSize = 12.sp)
            }
            decision.choices.forEach { choice ->
                DecisionChoiceCard(choice = choice, enabled = enabled, onClick = { onChoose(choice.choiceId) })
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
private fun DecisionChoiceCard(choice: DecisionChoiceUi, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("decision_choice_${choice.key}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LifeGameMuted)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(choice.key, color = LifeGameInkSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = choice.label,
                color = LifeGameInk,
                fontSize = 15.5.sp,
                lineHeight = 21.5.sp,
                fontWeight = FontWeight.Medium,
            )
            if (choice.hint.isNotBlank()) {
                Text(
                    text = choice.hint,
                    color = LifeGameInkSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                )
            }
        }
    }
}

@Composable
private fun OutcomeBlock(outcome: OutcomeUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val dash = 4.dp.toPx()
                drawLine(
                    color = LifeGameRule,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.75f)),
                )
            }
            .padding(start = 4.dp, end = 4.dp, top = 10.dp)
            .testTag("decision_outcome"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "→ ${outcome.choiceLabel}",
            color = LifeGameInk,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(text = outcome.text, style = ResultStoryStyle, color = LifeGameInk)
        if (outcome.changes.isEmpty()) {
            Text("변화 없음", color = LifeGameInkTertiary, fontSize = 12.5.sp)
        } else {
            EffectText(outcome.changes, fontSize = 12.5.sp, showAmount = true)
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
