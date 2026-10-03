package com.example.lifegame.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

@Composable
fun LifeGameApp(
    navController: NavHostController = rememberNavController(),
    gameViewModel: LifeGameViewModel = viewModel(),
) {
    LifeGameTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val state = gameViewModel.uiState
            val restart = {
                gameViewModel.reset()
                navController.navigate(Routes.START) {
                    popUpTo(Routes.START) { inclusive = true }
                    launchSingleTop = true
                }
            }

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
                        onStartGame = { characterId, playerName ->
                            gameViewModel.startNewGame(characterId, playerName)
                            navController.navigateOnce(Routes.GAME) {
                                popUpTo(Routes.START)
                            }
                        },
                    )
                }
                composable(Routes.GAME) {
                    val event = state.currentEvent
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
                        // 결말이 이미 정해졌는데 아직 게임 화면에 있으면 결말로 보낸다 (빈 화면 방지).
                        state.ending != null -> LaunchedEffect(Unit) {
                            navController.navigate(Routes.ENDING) {
                                popUpTo(Routes.START)
                                launchSingleTop = true
                            }
                        }
                        else -> RestartNotice(
                            message = state.contentError
                                ?: "진행 중인 게임을 찾지 못했어요. 처음부터 다시 시작해 주세요.",
                            onRestart = restart,
                        )
                    }
                }
                composable(Routes.ENDING) {
                    val ending = state.ending
                    if (ending != null) {
                        val records = buildLifeRecords(
                            state.progress?.choiceHistory.orEmpty(),
                            state.eventsById,
                        )
                        EndingScreen(
                            ending = ending,
                            stats = state.stats,
                            choiceHistory = records.map { "${it.age}세 · ${it.title} → ${it.choiceLabel}" },
                            onRestart = restart,
                        )
                    } else {
                        RestartNotice(
                            message = "결말을 불러오지 못했어요. 처음부터 다시 시작해 주세요.",
                            onRestart = restart,
                        )
                    }
                }
            }
        }
    }
}

/** 진행 상태가 사라졌을 때 (앱 재시작, 콘텐츠 오류 등) 흰 화면 대신 보여 준다. */
@Composable
private fun RestartNotice(message: String, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text("이어서 보여 줄 장면이 없어요", style = MaterialTheme.typography.headlineMedium)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = onRestart,
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("처음으로", style = MaterialTheme.typography.titleMedium)
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
