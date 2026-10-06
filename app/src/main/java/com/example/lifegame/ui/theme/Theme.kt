package com.example.lifegame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeGameColorScheme = lightColorScheme(
    // 주 버튼은 필름과 같은 먹색
    primary = LifeGameInk,
    onPrimary = Color.White,
    primaryContainer = LifeGameMuted,
    onPrimaryContainer = LifeGameInk,

    secondary = LifeGameInkSecondary,
    onSecondary = Color.White,
    // 진행 막대(StatBar)의 빈 부분 등. 기본 보라색이 섞이지 않게 선 색으로 맞춘다.
    secondaryContainer = LifeGameRule,
    onSecondaryContainer = LifeGameInk,

    background = LifeGamePaper,
    onBackground = LifeGameInk,

    surface = LifeGamePaper,
    onSurface = LifeGameInk,
    surfaceVariant = LifeGameMuted,
    onSurfaceVariant = LifeGameInkSecondary,
    surfaceContainerLowest = LifeGameCard,
    surfaceContainerLow = LifeGamePaper,
    surfaceContainer = LifeGamePaper,
    surfaceContainerHigh = LifeGamePaper,
    surfaceContainerHighest = LifeGameMuted,

    // 능력치 하락 등 부정적인 변화
    error = LifeGameStatDown,
    onError = Color.White,
    errorContainer = LifeGameStatDownSoft,
    onErrorContainer = LifeGameInk,

    outline = LifeGameRule,
    outlineVariant = LifeGameRule,
)

@Composable
fun LifeGameTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LifeGameColorScheme,
        typography = LifeGameTypography,
        content = content
    )
}
