package com.example.lifegame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import com.example.lifegame.domain.model.Ending
import com.example.lifegame.domain.model.Stats
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.EndingScreen
import com.example.lifegame.ui.screens.GameScreen
import com.example.lifegame.ui.screens.StartScreen
import com.example.lifegame.ui.theme.LifeGameTheme

object Routes {
    const val START = "start"
    const val CHARACTER_SELECT = "character_select"
    const val GAME = "game"
    const val ENDING = "ending"
}

// 정원률 담당 ui/theme의 앱 테마가 준비되면 MaterialTheme을 교체한다.
@Composable
fun LifeGameApp(
    navController: NavHostController = rememberNavController(),
    gameViewModel: LifeGameViewModel = viewModel(),
) {
    LifeGameTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val state = gameViewModel.uiState

            NavHost(navController = navController, startDestination = Routes.START) {
                composable(Routes.START) {
                    StartScreen(
                        canContinue = state.hasSaveData,
                        onNewGame = { navController.navigateOnce(Routes.CHARACTER_SELECT) },
                        onContinue = { /* ProgressStore 연결 후 저장된 화면으로 이동 */ },
                    )
                }
                composable(Routes.CHARACTER_SELECT) {
                    CharacterSelectScreen(
                        onStartGame = { characterId ->
                            gameViewModel.startNewGame(characterId)
                            navController.navigateOnce(Routes.GAME) {
                                popUpTo(Routes.START)
                            }
                        },
                    )
                }
                composable(Routes.GAME) {
                    val event = state.currentEvent
                    val ending = state.ending
                    when {
                        event != null -> {
                            // 선택 결과는 같은 화면에 이어 붙고, 다음 해로 넘어가는 전환도 이 화면 안에서 재생한다.
                            GameScreen(
                                state = state,
                                onChoose = { choice -> gameViewModel.choose(choice) },
                                onNextYear = {
                                    val finished = gameViewModel.proceed()
                                    if (finished) {
                                        navController.navigateOnce(Routes.ENDING) {
                                            popUpTo(Routes.START)
                                        }
                                    }
                                },
                            )
                        }
                        ending != null -> {
                            SafeEndingScreen(
                                ending = ending,
                                stats = state.stats,
                                state = state,
                                gameViewModel = gameViewModel,
                                navController = navController,
                            )
                        }
                        else -> {
                            RecoveryScreen(
                                message = state.contentError ?: "다음 사건을 불러오지 못했습니다.",
                                onRestart = {
                                    gameViewModel.reset()
                                    navController.navigateOnce(Routes.START) {
                                        popUpTo(Routes.START) { inclusive = true }
                                    }
                                },
                            )
                        }
                    }
                }
                composable(Routes.ENDING) {
                    val ending = state.ending
                    if (ending != null) {
                        SafeEndingScreen(
                            ending = ending,
                            stats = state.stats,
                            state = state,
                            gameViewModel = gameViewModel,
                            navController = navController,
                        )
                    } else {
                        RecoveryScreen(
                            message = "결말 데이터를 불러오지 못했습니다.",
                            onRestart = {
                                gameViewModel.reset()
                                navController.navigateOnce(Routes.START) {
                                    popUpTo(Routes.START) { inclusive = true }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SafeEndingScreen(
    ending: Ending,
    stats: Stats,
    state: LifeGameUiState,
    gameViewModel: LifeGameViewModel,
    navController: NavHostController,
) {
    EndingScreen(
        ending = ending,
        stats = stats,
        choiceHistory = state.progress?.choiceHistory.orEmpty().map {
            "${it.eventId}: ${it.choiceId}"
        },
        onRestart = {
            gameViewModel.reset()
            navController.navigateOnce(Routes.START) {
                popUpTo(Routes.START) { inclusive = true }
            }
        },
    )
}

@Composable
private fun RecoveryScreen(
    message: String,
    onRestart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "진행 상태를 다시 확인해야 합니다",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = message,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = onRestart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("처음으로 돌아가기")
        }
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
