package com.example.lifegame.ui.screens

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.lifegame.domain.model.Choice
import com.example.lifegame.domain.model.EventType
import com.example.lifegame.domain.model.GameEvent
import com.example.lifegame.domain.model.GameProgress
import com.example.lifegame.domain.model.LifeStage
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.LifeGameUiState
import com.example.lifegame.ui.STAT_NAMES
import com.example.lifegame.ui.STAT_START
import com.example.lifegame.ui.UiChoiceResult
import com.example.lifegame.ui.affectedStatNames
import com.example.lifegame.ui.buildLifeRecords
import com.example.lifegame.ui.components.DISTORT_MAX_DP
import com.example.lifegame.ui.components.FilmMotion
import com.example.lifegame.ui.components.FilmShot
import com.example.lifegame.ui.components.FilmStrip
import com.example.lifegame.ui.components.LifeHeader
import com.example.lifegame.ui.components.PerforationBand
import com.example.lifegame.ui.components.PullHint
import com.example.lifegame.ui.components.StatBar
import com.example.lifegame.ui.displayAge
import com.example.lifegame.ui.replacePlayerPlaceholders
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
import com.example.lifegame.ui.theme.LifeGameStatDown
import com.example.lifegame.ui.theme.LifeGameStatDownSoft
import com.example.lifegame.ui.theme.LifeGameStatUp
import com.example.lifegame.ui.theme.LifeGameStatUpSoft
import com.example.lifegame.ui.theme.MarkLabelStyle
import com.example.lifegame.ui.theme.ResultStoryStyle
import com.example.lifegame.ui.theme.StoryStyle
import com.example.lifegame.ui.toDisplayMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow
import kotlin.random.Random

/** 화면에 걸린 한 해. 전환이 끝날 때까지는 이전 해를 계속 보여 준다. */
@Immutable
private data class YearFrame(
    val key: String,
    val event: GameEvent,
    val age: Int,
    val stage: LifeStage,
    val specialOrdinal: Int,
    val shot: FilmShot,
) {
    val isSpecial: Boolean get() = event.type == EventType.SPECIAL
}

// 평소 전환: 넘어가다 40% 근처에서 덜컥 걸린 뒤 마저 넘어간다 (시간 비율 → 넘어간 비율)
private val NormalAdvanceKeys = listOf(
    0f to 0f, 0.36f to 0.4167f, 0.42f to 0.3972f, 0.47f to 0.4333f,
    0.52f to 0.4083f, 0.58f to 0.4167f, 1f to 1f,
)
private val PopupEasing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

private fun easeInOut(t: Float): Float = if (t < 0.5f) 2 * t * t else 1 - (-2 * t + 2).pow(2) / 2
private fun easeOut(t: Float): Float = 1 - (1 - t).pow(3)

private fun normalAdvance(p: Float): Float {
    for (i in 0 until NormalAdvanceKeys.lastIndex) {
        val (p0, v0) = NormalAdvanceKeys[i]
        val (p1, v1) = NormalAdvanceKeys[i + 1]
        if (p <= p1) {
            val local = (p - p0) / (p1 - p0)
            val eased = if (i == 0 || i == NormalAdvanceKeys.lastIndex - 1) easeInOut(local) else local
            return v0 + (v1 - v0) * eased
        }
    }
    return 1f
}

/**
 * 시안 7 · C안 빈티지 게임 화면.
 * 한 해 = 한 화면. 사건 그림은 필름 한 컷으로, 고른 결과는 같은 화면에 이어 붙는다.
 * "다음 해로"를 누르면 평소에는 필름이 지지직거리며 넘어가고,
 * 돌발 사건이면 들어온 컷이 물결처럼 일그러진 뒤 돌발 사건 팝업이 뜬다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: LifeGameUiState,
    onChoose: (Choice) -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event = state.currentEvent ?: return
    val progress = state.progress ?: return
    val key = progress.currentEventOccurrenceId
    val target = remember(key) {
        val chosen = state.lastResult != null
        YearFrame(
            key = key,
            event = event,
            age = displayAge(progress.stage, progress.stageEventCount - if (chosen) 1 else 0),
            stage = progress.stage,
            specialOrdinal = progress.specialEventCount + if (chosen) 0 else 1,
            shot = FilmShot(
                key = key,
                imageId = event.imageId,
                imageFile = state.imageFiles[event.imageId],
                isSpecial = event.type == EventType.SPECIAL,
            ),
        )
    }

    var shown by remember { mutableStateOf(target) }
    var incoming by remember { mutableStateOf<YearFrame?>(null) }
    var motion by remember { mutableStateOf(FilmMotion()) }
    var frozenResult by remember { mutableStateOf<UiChoiceResult?>(null) }
    var transitioning by remember { mutableStateOf(false) }
    val textAlpha = remember { Animatable(1f) }
    val sheetAlpha = remember { Animatable(1f) }
    var popup by remember { mutableStateOf<YearFrame?>(null) }
    val popupProgress = remember { Animatable(0f) }
    val popupClosed = remember { Channel<Unit>(Channel.CONFLATED) }
    var historyOpen by rememberSaveable { mutableStateOf(false) }
    var statsOpen by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val reduceMotion = rememberReduceMotion()
    val listState = rememberLazyListState()
    LaunchedEffect(shown.key) { listState.scrollToItem(0) }

    // 전환이 끝나기 전에는 이전 해의 결과를 그대로 보여 준다
    val result = if (shown.key == key) state.lastResult else frozenResult
    val busy = transitioning || shown.key != key
    val records = remember(progress.choiceHistory, state.eventsById) {
        buildLifeRecords(progress.choiceHistory, state.eventsById)
    }

    suspend fun showSpecialPopup(frame: YearFrame) {
        popupClosed.tryReceive()
        popup = frame
        popupProgress.snapTo(0f)
        popupProgress.animateTo(1f, tween(if (reduceMotion) 150 else 240, easing = PopupEasing))
        popupClosed.receive()
        popupProgress.animateTo(0f, tween(160))
        popup = null
    }

    LaunchedEffect(target.key) {
        if (target.key == shown.key) {
            // 새 게임의 첫 사건도 돌발 추첨 대상이므로 진입 팝업을 생략하지 않는다.
            if (target.isSpecial && state.lastResult == null) {
                transitioning = true
                showSpecialPopup(target)
                transitioning = false
            }
            return@LaunchedEffect
        }
        transitioning = true
        historyOpen = false
        if (reduceMotion) {
            textAlpha.animateTo(0f, tween(150))
            sheetAlpha.snapTo(0f)
            shown = target
            frozenResult = null
        } else {
            coroutineScope {
                launch { textAlpha.animateTo(0f, tween(120)) }
                launch { sheetAlpha.animateTo(0f, tween(120)) }
                incoming = target
                val noise = launch {
                    while (isActive) {
                        motion = motion.copy(noiseSeed = Random.nextInt(1, Int.MAX_VALUE))
                        delay(60)
                    }
                }
                if (target.isSpecial) {
                    animate(0f, 1f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { v, _ ->
                        motion = motion.copy(advance = v)
                    }
                } else {
                    animate(0f, 1f, animationSpec = tween(340, easing = LinearEasing)) { p, _ ->
                        motion = motion.copy(advance = normalAdvance(p))
                    }
                }
                noise.cancel()
            }
            shown = target
            incoming = null
            frozenResult = null
            motion = FilmMotion()

            if (target.isSpecial) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val jitter = Random(5)
                animate(0f, 1f, animationSpec = tween(680, easing = LinearEasing)) { p, _ ->
                    // 빠르게 최대로 올라가 잠깐 버티다 가라앉는다
                    val envelope = when {
                        p < 0.18f -> easeOut(p / 0.18f)
                        p < 0.42f -> 1f
                        else -> 1f - easeInOut((p - 0.42f) / 0.58f)
                    }
                    motion = FilmMotion(
                        distortion = DISTORT_MAX_DP * envelope,
                        phase = p * 680f * 0.014f,
                        jitterY = (jitter.nextFloat() - 0.5f) * 5f * envelope,
                    )
                }
                motion = FilmMotion()
            }
        }

        if (target.isSpecial) showSpecialPopup(target)

        coroutineScope {
            launch { textAlpha.animateTo(1f, tween(120)) }
            delay(80)
            sheetAlpha.animateTo(1f, tween(120))
        }
        transitioning = false
    }

    BackHandler(enabled = historyOpen) { historyOpen = false }
    BackHandler(enabled = popup != null) { popupClosed.trySend(Unit) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(LifeGamePaper),
    ) {
        val gateHeight = min((maxWidth - 20.dp) * 0.62f, maxHeight * 0.3f)

        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            LifeHeader(
                age = shown.age,
                stage = shown.stage,
                stats = state.stats,
                changes = result?.engineResult?.appliedDelta,
                viewingHistory = historyOpen,
                onStatsClick = { statsOpen = true },
            )

            if (historyOpen) {
                HistoryPanel(
                    records = records,
                    nowAge = shown.age,
                    nowTitle = shown.event.title.replacePlayerPlaceholders(),
                    nowIsSpecial = shown.isSpecial,
                    onBackToNow = { historyOpen = false },
                    modifier = Modifier.weight(1f),
                )
            } else {
                // 필름, 본문, 결과와 선택지를 한 스크롤에 넣어 큰 글씨에서도 끝까지 읽는다.
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag("game_content"),
                ) {
                    item(key = "film") {
                        val canOpenHistory = !busy && records.isNotEmpty()
                        val pullThreshold = with(LocalDensity.current) { 56.dp.toPx() }
                        var pulled by remember { mutableFloatStateOf(0f) }
                        Column(
                            Modifier.draggable(
                                orientation = Orientation.Vertical,
                                enabled = canOpenHistory,
                                state = rememberDraggableState { delta -> pulled += delta },
                                onDragStopped = {
                                    if (pulled > pullThreshold) historyOpen = true
                                    pulled = 0f
                                },
                            ),
                        ) {
                            PullHint(
                                recordCount = records.size,
                                modifier = Modifier.clickable(
                                    enabled = canOpenHistory,
                                    role = Role.Button,
                                    onClickLabel = "지난 기록 보기",
                                ) { historyOpen = true },
                            )
                            FilmStrip(
                                current = shown.shot,
                                incoming = incoming?.shot,
                                gateHeight = gateHeight,
                                motion = { motion },
                            )
                        }
                    }
                    item(key = "event") {
                        EventText(
                            frame = shown,
                            result = result,
                            modifier = Modifier.graphicsLayer { alpha = textAlpha.value },
                        )
                    }
                    item(key = "choices") {
                        ChoiceSheet(
                            event = shown.event,
                            result = result,
                            nextAge = nextYearAge(progress),
                            enabled = !busy,
                            onChoose = onChoose,
                            onNextYear = {
                                frozenResult = state.lastResult
                                onNextYear()
                            },
                            modifier = Modifier.graphicsLayer { alpha = sheetAlpha.value },
                        )
                    }
                }
            }
        }

        popup?.let { frame ->
            SpecialEventPopup(
                frame = frame,
                progress = { popupProgress.value },
                onOpen = { popupClosed.trySend(Unit) },
            )
        }
    }

    if (statsOpen) {
        ModalBottomSheet(
            onDismissRequest = { statsOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = LifeGamePaper,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("stats_content")
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("능력치", style = MaterialTheme.typography.headlineMedium)
                StatGrid(state.stats)

            }
        }
    }
}

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** "다음 해로" 버튼에 적을 나이. 단계당 사건 수는 GameEngine과 같은 5개로 본다. */
private fun nextYearAge(progress: GameProgress): Int {
    val stageFull = progress.stage != LifeStage.ADULT && progress.stageEventCount >= 5
    return if (stageFull) {
        displayAge(LifeStage.entries[progress.stage.ordinal + 1], 0)
    } else {
        displayAge(progress.stage, progress.stageEventCount)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventText(frame: YearFrame, result: UiChoiceResult?, modifier: Modifier = Modifier) {
    val chosen = result != null
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (frame.isSpecial) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("돌발 사건", style = MarkLabelStyle, color = LifeGameSpecial)
                Text("이번 생 ${frame.specialOrdinal}번째 돌발 사건", color = LifeGameInkSecondary, fontSize = 11.5.sp)
            }
        }
        Text(
            text = frame.event.title.replacePlayerPlaceholders(),
            style = if (chosen) EventTitleStyle.copy(fontSize = 20.sp, lineHeight = 26.sp) else EventTitleStyle,
            color = LifeGameInk,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = frame.event.text.replacePlayerPlaceholders(),
            style = if (chosen) StoryStyle.copy(fontSize = 15.sp, lineHeight = 25.5.sp) else StoryStyle,
            color = if (chosen) LifeGameInkTertiary else LifeGameInk,
        )
        if (result != null) {
            val appear = remember(result) { Animatable(0f) }
            LaunchedEffect(result) { appear.animateTo(1f, tween(200)) }
            ResultBlock(result, Modifier.graphicsLayer { alpha = appear.value })
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultBlock(result: UiChoiceResult, modifier: Modifier = Modifier) {
    val changes = result.engineResult.appliedDelta.toDisplayMap().filterValues { it != 0 }
    Column(
        modifier = modifier
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
            .padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "→ ${result.choice.label.replacePlayerPlaceholders()}",
            color = LifeGameInk,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = result.engineResult.resultText.replacePlayerPlaceholders(),
            style = ResultStoryStyle,
            color = LifeGameInk,
        )
        Text("선택 결과", color = LifeGameInkSecondary, modifier = Modifier.semantics { heading() })
        if (changes.isEmpty()) {
            Text("변화 없음", color = LifeGameInkTertiary)
        } else {
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                changes.forEach { (name, delta) ->
                    val up = delta > 0
                    Text(
                        text = "$name ${if (up) "+" else "-"}${abs(delta)}",
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (up) LifeGameStatUpSoft else LifeGameStatDownSoft)
                            .padding(horizontal = 9.dp, vertical = 4.dp),
                        color = if (up) LifeGameStatUp else LifeGameStatDown,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChoiceSheet(
    event: GameEvent,
    result: UiChoiceResult?,
    nextAge: Int,
    enabled: Boolean,
    onChoose: (Choice) -> Unit,
    onNextYear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(LifeGameRule, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (result == null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("어떻게 할까", color = LifeGameInkTertiary, fontSize = 12.sp)
                Text("영향을 받는 능력치", color = LifeGameInkTertiary, fontSize = 12.sp)
            }
            event.choices.forEachIndexed { index, choice ->
                ChoiceCard(
                    key = ('A' + index).toString(),
                    label = choice.label.replacePlayerPlaceholders(),
                    affected = choice.affectedStatNames(),
                    enabled = enabled,
                    onClick = { onChoose(choice) },
                )
            }
        } else {
            InkButton(text = "다음 해로 · ${nextAge}세", enabled = enabled, onClick = onNextYear)
        }
    }
}

@Composable
private fun ChoiceCard(
    key: String,
    label: String,
    affected: List<String>,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(LifeGameCard)
            .border(1.dp, LifeGameRule, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .sizeIn(minWidth = 22.dp, minHeight = 22.dp)
                .padding(4.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LifeGameMuted),
            contentAlignment = Alignment.Center,
        ) {
            Text(key, color = LifeGameInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = label,
                color = LifeGameInk,
                fontSize = 15.5.sp,
                lineHeight = 21.5.sp,
                fontWeight = FontWeight.Medium,
            )
            if (affected.isNotEmpty()) {
                Text(
                    text = affected.joinToString(" · "),
                    color = LifeGameInkTertiary,
                    fontSize = 11.5.sp,
                    lineHeight = 15.5.sp,
                )
            }
        }
    }
}

@Composable
private fun InkButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(LifeGameInk)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = LifeGameCard, fontSize = 15.5.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

/** 돌발 사건 팝업. 머리에 필름 구멍 띠를 두른 카드가 어두워진 화면 위로 올라온다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialEventPopup(frame: YearFrame, progress: () -> Float, onOpen: () -> Unit) {
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
                .testTag("special_popup"),
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
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("돌발 사건", style = MarkLabelStyle, color = LifeGameSpecial)
                    Text("이번 생 ${frame.specialOrdinal}번째", color = LifeGameInkSecondary, fontSize = 11.5.sp)
                }
                Text(
                    text = frame.event.title.replacePlayerPlaceholders(),
                    color = LifeGameInk,
                    fontFamily = LifeGameSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                )
                Text(
                    text = frame.event.text.replacePlayerPlaceholders().teaser(),
                    color = LifeGameBodyInk,
                    fontFamily = LifeGameSerif,
                    fontSize = 15.5.sp,
                    lineHeight = 25.5.sp,
                )
                Spacer(Modifier.height(6.dp))
                InkButton(text = "사건 보기", enabled = true, onClick = onOpen)
            }
        }
    }
}

@Composable
fun StatGrid(stats: Stats, modifier: Modifier = Modifier) {
    val values = stats.toDisplayMap()
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 360.dp || fontScale >= 1.5f) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            STAT_NAMES.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { name ->
                        StatBar(
                            name = name,
                            value = values[name] ?: STAT_START,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (columns == 2 && row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
