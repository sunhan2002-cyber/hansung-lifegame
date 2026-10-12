package com.example.lifegame.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifegame.ui.components.InkButton
import com.example.lifegame.ui.screens.AutoProgressScreen
import com.example.lifegame.ui.screens.ChapterFocusScreen
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.DecisionScreen
import com.example.lifegame.ui.screens.HomeScreen
import com.example.lifegame.ui.screens.RunEndScreen
import com.example.lifegame.ui.theme.LifeGameInkSecondary
import com.example.lifegame.ui.theme.LifeGamePaper
import com.example.lifegame.ui.theme.LifeGameTheme

/** v4 화면 흐름: 홈 → 프로필 만들기 → 플레이(장 표지 · 자동 진행 · 결정 · 끝). */
object Routes {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PLAY = "play"
}

@Composable
fun LifeGameApp(
    navController: NavHostController = rememberNavController(),
    gameViewModel: LifeGameViewModel = viewModel(),
) {
    LifeGameTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = LifeGamePaper) {
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(
                        lifeCard = gameViewModel.lifeCard,
                        finishedLives = gameViewModel.finishedLives,
                        message = gameViewModel.playError,
                        onContinue = { navController.navigateOnce(Routes.PLAY) },
                        onNewLife = { navController.navigateOnce(Routes.PROFILE) },
                    )
                }
                composable(Routes.PROFILE) {
                    CharacterSelectScreen(
                        starters = gameViewModel.starters,
                        onStart = { profile ->
                            gameViewModel.startNewLife(profile)
                            navController.navigateOnce(Routes.PLAY) {
                                popUpTo(Routes.HOME)
                            }
                        },
                    )
                }
                composable(Routes.PLAY) {
                    // 뒤로 가기는 홈으로 간다. 진행 중인 인생은 홈의 "이어하기"로 돌아온다.
                    PlayHost(
                        gameViewModel = gameViewModel,
                        onGoHome = {
                            navController.navigateOnce(Routes.HOME) {
                                popUpTo(Routes.HOME) { inclusive = true }
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayHost(
    gameViewModel: LifeGameViewModel,
    onGoHome: () -> Unit,
) {
    val play = gameViewModel.play
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
        PlayPhase.DECISION -> DecisionScreen(
            state = play,
            onChoose = gameViewModel::chooseDecision,
            onContinue = gameViewModel::continueAfterOutcome,
        )
        PlayPhase.ENDED -> RunEndScreen(
            state = play,
            onDone = {
                gameViewModel.finishLife()
                onGoHome()
            },
        )
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

/**
 * 현재 화면이 RESUMED 상태일 때만 이동한다.
 * 버튼을 빠르게 여러 번 눌러 같은 화면이 중복으로 쌓이는 것을 막는다.
 */
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
