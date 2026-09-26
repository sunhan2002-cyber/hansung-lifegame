package com.example.lifegame.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.lifegame.ui.screens.CharacterSelectScreen
import com.example.lifegame.ui.screens.ChoiceResultScreen
import com.example.lifegame.ui.screens.EndingScreen
import com.example.lifegame.ui.screens.GameScreen
import com.example.lifegame.ui.screens.StartScreen
import com.example.lifegame.ui.theme.LifeGameTheme

object Routes {
    const val START = "start"
    const val CHARACTER_SELECT = "character_select"
    const val GAME = "game"
    const val CHOICE_RESULT = "choice_result"
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
                    GameScreen(
                        event = gameViewModel.currentEvent,
                        stats = state.stats,
                        onChoose = { choice ->
                            if (gameViewModel.choose(choice)) {
                                navController.navigateOnce(Routes.CHOICE_RESULT) {
                                    popUpTo(Routes.GAME) { inclusive = true }
                                }
                            }
                        },
                        onShowEndingForTest = {
                            gameViewModel.finish()
                            navController.navigateOnce(Routes.ENDING) {
                                popUpTo(Routes.START)
                            }
                        },
                    )
                }
                composable(Routes.CHOICE_RESULT) {
                    val result = state.lastResult
                    if (result != null) {
                        ChoiceResultScreen(
                            result = result,
                            onNext = {
                                val finished = gameViewModel.proceed()
                                navController.navigateOnce(if (finished) Routes.ENDING else Routes.GAME) {
                                    popUpTo(Routes.CHOICE_RESULT) { inclusive = true }
                                }
                            },
                        )
                    }
                }
                composable(Routes.ENDING) {
                    val ending = state.ending
                    if (ending != null) {
                        EndingScreen(
                            ending = ending,
                            stats = state.stats,
                            choiceHistory = state.choiceHistory,
                            onRestart = {
                                gameViewModel.reset()
                                navController.popBackStack(Routes.START, inclusive = false)
                            },
                        )
                    }
                }
            }
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
