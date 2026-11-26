package com.example.lifegame.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.example.lifegame.ui.components.PlayMenuActions
import com.example.lifegame.ui.components.LocalPlayMenu
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.example.lifegame.data.SaveState
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifegame.BuildConfig
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.screens.AutoProgressScreen
import com.example.lifegame.ui.screens.ChapterFocusScreen
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.DoljabiCutsceneScreen
import com.example.lifegame.ui.screens.doljabiPreviewAlreadyShown
import com.example.lifegame.ui.screens.shouldPlayDoljabiCutscene
import com.example.lifegame.domain.model.CutsceneStatus
import com.example.lifegame.ui.screens.DesignPreviewScreen
import com.example.lifegame.ui.screens.MotionCompareScreen
import com.example.lifegame.ui.components.ProvideMotionStyle
import com.example.lifegame.ui.screens.EndingFlowScreen
import com.example.lifegame.ui.screens.EndingStep
import com.example.lifegame.ui.screens.TitleCollectionScreen
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.screens.RunEndScreen
import com.example.lifegame.ui.screens.TAP_GUARD_MILLIS
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameTheme
import kotlinx.coroutines.delay

/** v4 화면 흐름: 홈 → 프로필 만들기 → 플레이(장 표지 · 자동 진행 · 결정 · 끝). */
object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PLAY = "play"
    const val DESIGN_PREVIEW = "design_preview"
    const val MOTION_COMPARE = "motion_compare"
    const val RECORD = "record/{runId}"
    const val TITLES = "titles"

    fun record(runId: String) = "record/${android.net.Uri.encode(runId)}"
}

@Composable
fun LifeGameApp(
    navController: NavHostController = rememberNavController(),
    gameViewModel: LifeGameViewModel = viewModel(),
) {
    LifeGameTheme {
        ProvideMotionStyle(allowOverride = BuildConfig.DEBUG) {
        Surface(modifier = Modifier.fillMaxSize(), color = LifeGamePaper) {
            val saveState by gameViewModel.saveState.collectAsState()
            Box(Modifier.fillMaxSize()) {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    val latestRecord = remember(gameViewModel.completedLives) {
                        gameViewModel.completedLives.firstOrNull()?.let { gameViewModel.recordFor(it.runId) }
                    }
                    HomeScreen(
                        lifeCard = gameViewModel.lifeCard,
                        finishedLives = gameViewModel.finishedLives,
                        message = gameViewModel.playError ?: gameViewModel.contentWarning,
                        latestRecord = latestRecord,
                        hasRecoverableSave = gameViewModel.hasStoredRun,
                        onContinue = {
                            gameViewModel.continueLife()
                            navController.navigateOnce(Routes.PLAY)
                        },
                        onNewLife = { navController.navigateOnce(Routes.PROFILE) },
                        onDesignPreview = if (BuildConfig.DEBUG) {
                            { navController.navigateOnce(Routes.DESIGN_PREVIEW) }
                        } else null,
                        onMotionCompare = if (BuildConfig.DEBUG) {
                            { navController.navigateOnce(Routes.MOTION_COMPARE) }
                        } else null,
                        onOpenRecord = { runId -> navController.navigateOnce(Routes.record(runId)) },
                        onOpenTitles = { navController.navigateOnce(Routes.TITLES) },
                        generationScenario = if (BuildConfig.DEBUG) gameViewModel.generationScenario else null,
                        onScenarioChange = { gameViewModel.generationScenario = it },
                    )
                }
                composable(Routes.RECORD) { entry ->
                    val runId = entry.arguments?.getString("runId").orEmpty()
                    val record = remember(runId, gameViewModel.completedLives) { gameViewModel.recordFor(runId) }
                    if (record == null) {
                        RecoveryScreen(message = "이 인생 기록을 찾지 못했습니다.", onGoHome = { navController.popBackStack() })
                    } else {
                        EndingFlowScreen(
                            record = record,
                            futureLook = gameViewModel.futureLook,
                            onRequestFuture = { gameViewModel.requestFutureLook(runId) },
                            onCancelFuture = gameViewModel::cancelFutureLook,
                            onDone = { navController.popBackStack() },
                            doneLabel = "목록으로",
                            initialStep = EndingStep.CERTIFICATE,
                        )
                    }
                }
                composable(Routes.TITLES) {
                    val collection = remember(gameViewModel.completedLives) { gameViewModel.titleCollection() }
                    TitleCollectionScreen(
                        collection = collection,
                        onBack = { navController.popBackStack() },
                        targetTitleId = gameViewModel.titleTargetId,
                        onSetTarget = gameViewModel::setTitleTarget,
                    )
                }
                composable(Routes.DESIGN_PREVIEW) {
                    DesignPreviewScreen(onBack = { navController.popBackStack() })
                }
                composable(Routes.MOTION_COMPARE) {
                    MotionCompareScreen(onBack = { navController.popOnce() })
                }
                composable(Routes.PROFILE) {
                    val titleTarget = remember(gameViewModel.titleTargetId, gameViewModel.completedLives) { gameViewModel.titleTarget() }
                    CharacterSelectScreen(
                        starters = gameViewModel.starters,
                        faceSheet = gameViewModel.faceSheet,
                        aiFace = gameViewModel.usesRealFaceGeneration,
                        titleTarget = titleTarget,
                        onRequestFaceSheet = gameViewModel::requestFaceSheet,
                        onCancelFaceSheet = gameViewModel::cancelFaceSheet,
                        onClearFaceSheet = gameViewModel::clearFaceSheet,
                        onPhotoFlowChanged = gameViewModel::setPhotoFlowStatus,
                        onStart = { profile ->
                            gameViewModel.startNewLife(profile)
                            navController.navigateOnce(Routes.PLAY) {
                                popUpTo(Routes.HOME)
                            }
                        },
                    )
                }
                composable(Routes.PLAY) {
                    PlayHost(
                        gameViewModel = gameViewModel,
                        onGoHome = {
                            navController.navigateOnce(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                        // 인생 리셋을 확정하면 새 인생의 프로필 · 주사위 화면으로 바로 간다. 뒤로 가면 홈이다.
                        onStartNewLife = {
                            navController.navigateOnce(Routes.PROFILE) {
                                popUpTo(Routes.HOME)
                            }
                        },
                    )
                }
            }
            // Disk durability is separate from an immediate in-memory save. Fast writes need no flash.
            var showPending by remember { mutableStateOf(false) }
            val pending = saveState is SaveState.Pending
            LaunchedEffect(pending) {
                showPending = false
                if (pending) { delay(350); showPending = true }
            }
            if (showPending) Text(
                "저장 중…", style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.TopEnd).safeDrawingPadding().padding(8.dp)
                    .background(LifeGamePaper).padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("save_pending").semantics { liveRegion = LiveRegionMode.Polite },
            )
            if (saveState is SaveState.Failed) AlertDialog(
                onDismissRequest = {},
                modifier = Modifier.testTag("save_failure"),
                title = { Text("진행 내용을 저장하지 못했어요") },
                text = { Text(gameViewModel.saveWarning ?: "현재 화면의 진행은 유지하고 있어요. 저장 공간을 확인한 뒤 다시 시도해 주세요.") },
                confirmButton = { TextButton(onClick = { gameViewModel.retrySave() }) { Text("저장 다시 시도") } },
            )
            }
        }
        }
    }
}

@Composable
internal fun PlayHost(
    gameViewModel: LifeGameViewModel,
    onGoHome: () -> Unit,
    onStartNewLife: () -> Unit = onGoHome,
) {
    val play = gameViewModel.play
    val saveState by gameViewModel.saveState.collectAsState()
    var finishing by rememberSaveable { mutableStateOf(false) }
    // ---- 진행 중 "인생 리셋"(2026-11-20 과제) ----
    var resetConfirm by rememberSaveable { mutableStateOf(false) }
    var resetting by rememberSaveable { mutableStateOf(false) }
    var resumeAfterMenu by rememberSaveable { mutableStateOf(false) }
    val resumeIfPaused: () -> Unit = {
        if (resumeAfterMenu) {
            resumeAfterMenu = false
            if (gameViewModel.play?.autoPlaying == false) gameViewModel.togglePlaying()
        }
    }
    val playMenu = remember(gameViewModel) {
        PlayMenuActions(
            // 메뉴를 보는 동안 자동 진행이 저절로 다음 해로 넘어가지 않게 멈춘다.
            onOpen = {
                if (gameViewModel.play?.phase == PlayPhase.AUTO && gameViewModel.play?.autoPlaying == true) {
                    gameViewModel.togglePlaying()
                    resumeAfterMenu = true
                }
            },
            onDismiss = { resumeIfPaused() },
            onResetLife = { resetConfirm = true },
        )
    }
    val confirmReset: () -> Unit = {
        resetConfirm = false
        if (!resetting && gameViewModel.startLifeReset()) {
            resumeAfterMenu = false
            resetting = true
        } else if (!resetting) {
            resumeIfPaused()
        }
    }
    BackHandler(enabled = resetting) { /* 지운 것이 디스크에 확인될 때까지 화면을 지킨다. */ }
    LaunchedEffect(resetting, saveState) {
        if (!resetting) return@LaunchedEffect
        if (!gameViewModel.lifeResetPending) {
            // 프로세스가 다시 만들어진 경우: 슬롯이 없으면 리셋이 끝난 것이고, 남아 있으면 그 인생을 그대로 이어 간다.
            resetting = false
            if (!gameViewModel.hasStoredRun) onStartNewLife() else gameViewModel.continueLife()
            return@LaunchedEffect
        }
        // 화면에 모인 저장 상태가 한 프레임 늦을 수 있어, 리셋이 만든 저장 요청의 결과만 본다.
        when (gameViewModel.lifeResetProgress()) {
            LifeResetProgress.DONE -> {
                gameViewModel.completeLifeReset()
                resetting = false
                onStartNewLife()
            }
            LifeResetProgress.FAILED -> {
                gameViewModel.cancelLifeReset()
                resetting = false
            }
            LifeResetProgress.WAITING -> Unit
        }
    }
    BackHandler(enabled = finishing) { /* Keep the final checkpoint visible until disk completion. */ }
    val finishAndGoHome: () -> Unit = {
        if (!finishing) {
            gameViewModel.finishLife()
            if (gameViewModel.play == null) finishing = true
        }
    }
    LaunchedEffect(finishing, saveState) {
        if (finishing && saveState is SaveState.Durable) onGoHome()
        if (finishing && saveState is SaveState.Idle) {
            // A new process has no in-flight writes. Atomic slot removal proves completion;
            // otherwise recover the saved ending and let the player finish it again safely.
            if (!gameViewModel.hasStoredRun) onGoHome() else {
                finishing = false
                gameViewModel.continueLife()
            }
        }
    }
    // 화면이 바뀐 직후 잠깐(연타 보호 시간) 터치를 받지 않는다. "계속하기"를 빠르게 여러 번 누르면 첫 터치로 화면이 바뀌고
    // 남은 터치가 새 화면의 같은 자리 버튼(배속 · 초점 고르기 등)을 눌렀다(2026-11-09 작은 화면 실기 확인).
    // 보호는 화면이 바뀐 그 프레임부터 켠다(효과에서 켜면 한두 프레임 늦어 그 사이 터치가 새 화면에 닿는다).
    val phase = play?.phase
    var settledPhase by remember { mutableStateOf<PlayPhase?>(null) }
    val guardingTaps = settledPhase != phase
    LaunchedEffect(phase) {
        delay(TAP_GUARD_MILLIS)
        settledPhase = phase
    }
    Box(Modifier.fillMaxSize()) {
        if (finishing) {
            Box(Modifier.fillMaxSize().background(LifeGamePaper), contentAlignment = Alignment.Center) {
                Text("인생 기록을 저장하고 있어요…", modifier = Modifier.testTag("ending_save_pending"))
            }
        } else if (resetting) {
            Box(Modifier.fillMaxSize().background(LifeGamePaper), contentAlignment = Alignment.Center) {
                Text("지금 인생을 정리하고 있어요…", modifier = Modifier.testTag("life_reset_pending"))
            }
        } else CompositionLocalProvider(LocalPlayMenu provides playMenu.takeIf { play != null && play.phase != PlayPhase.ENDED }) {
            PlayPhaseScreen(play, gameViewModel, onGoHome, finishAndGoHome)
        }
        if (resetConfirm) {
            AlertDialog(
                onDismissRequest = { resetConfirm = false; resumeIfPaused() },
                modifier = Modifier.testTag("life_reset_dialog"),
                text = { Text("현재 진행 중인 인생은 사라집니다. 새 인생을 시작할까요?") },
                confirmButton = {
                    TextButton(onClick = confirmReset, modifier = Modifier.testTag("life_reset_confirm")) { Text("새 인생 시작") }
                },
                dismissButton = {
                    TextButton(
                        onClick = { resetConfirm = false; resumeIfPaused() },
                        modifier = Modifier.testTag("life_reset_cancel"),
                    ) { Text("취소") }
                },
            )
        }
        gameViewModel.lifeResetError?.let { message ->
            AlertDialog(
                onDismissRequest = gameViewModel::clearLifeResetError,
                modifier = Modifier.testTag("life_reset_error"),
                text = { Text(message) },
                confirmButton = { TextButton(onClick = gameViewModel::clearLifeResetError) { Text("확인") } },
            )
        }
        if (guardingTaps) {
            Box(
                Modifier
                    .matchParentSize()
                    .testTag("screen_change_tap_guard")
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    },
            )
        }
    }
}

@Composable
private fun PlayPhaseScreen(
    play: PeriodPlayUiState?,
    gameViewModel: LifeGameViewModel,
    onGoHome: () -> Unit,
    onFinish: () -> Unit,
) {
    when (play?.phase) {
        PlayPhase.CHAPTER -> ChapterFocusScreen(
            state = play,
            onSelectFocus = gameViewModel::selectFocus,
        )
        PlayPhase.AUTO -> AutoProgressScreen(
            state = play,
            onAdvanceYear = gameViewModel::advanceYear,
            onTogglePlaying = gameViewModel::togglePlaying,
            onSpeedChange = gameViewModel::setSpeed,
            onSkipToDecision = gameViewModel::skipToNextDecision,
        )
        PlayPhase.DECISION -> if (play.decision == null) {
            // 사건 머리를 되살리지 못한 결과 화면은 빈 화면 대신 다음 진행으로 넘긴다.
            LaunchedEffect(play.yearKey) { gameViewModel.continueAfterOutcome() }
        } else if (shouldPlayDoljabiCutscene(play.decision.eventId, play.outcome != null, play.presentation.cutscene)) {
            // 새 인생의 첫 결정(돌잡이) 앞에서 컷신을 한 번 보여 준다. 재생 중에 앱이 꺼졌으면 다시 처음부터.
            DoljabiCutsceneScreen(
                playerName = play.playerName,
                onStart = { gameViewModel.setCutsceneStatus(CutsceneStatus.PLAYING) },
                onFinish = gameViewModel::setCutsceneStatus,
            )
        } else {
            DecisionScreen(
                state = play,
                onChoose = gameViewModel::chooseDecision,
                onContinue = gameViewModel::continueAfterOutcome,
                onEventImageShown = gameViewModel::markEventImageShown,
                onResultAnimationConsumed = gameViewModel::markResultAnimationConsumed,
                skipImagePreview = doljabiPreviewAlreadyShown(play.decision.eventId, play.presentation.cutscene),
            )
        }
        PlayPhase.ENDED -> {
            val record = gameViewModel.currentRecord
            if (record != null) {
                EndingFlowScreen(
                    record = record,
                    futureLook = gameViewModel.futureLook,
                    onRequestFuture = { gameViewModel.requestFutureLook(record.runId) },
                    onCancelFuture = gameViewModel::cancelFutureLook,
                    onDone = onFinish,
                )
            } else {
                RunEndScreen(
                    state = play,
                    onDone = onFinish,
                )
            }
        }
        null -> RecoveryScreen(
            message = gameViewModel.playError ?: "진행 중인 인생이 없습니다.",
            onGoHome = onGoHome,
        )
    }
}

@Composable
private fun RecoveryScreen(
    message: String,
    onGoHome: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LifeGamePaper)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, alignment = androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            text = "진행 상태를 다시 확인해야 합니다",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = message, color = LifeGameInkSecondary, style = MaterialTheme.typography.bodyLarge)
        InkButton(text = "홈으로", onClick = onGoHome)
    }
}

/** Pops only from a settled screen, so a double tap on a back control cannot also pop HOME. */
private fun NavHostController.popOnce() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED && previousBackStackEntry != null) {
        popBackStack()
    }
}

private fun NavHostController.navigateOnce(
    route: String,
    builder: NavOptionsBuilder.() -> Unit = {},
) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route) {
            launchSingleTop = true
            builder()
        }
    }
}
