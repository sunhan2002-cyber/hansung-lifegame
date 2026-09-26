package com.example.lifegame.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeGameColorScheme = lightColorScheme(
    primary = LifeGamePrimary,
    onPrimary = Color.White,

    background = LifeGameBackground,
    onBackground = LifeGameTextPrimary,

    surface = LifeGameSurface,
    onSurface = LifeGameTextPrimary,

    // 돌발 이벤트 강조색과 그 위의 글자색
    error = LifeGameSpecial,
    onError = LifeGameTextPrimary,

    // 돌발 이벤트 배경색과 그 위의 글자색
    errorContainer = LifeGameSpecialBackground,
    onErrorContainer = LifeGameTextPrimary,

    outline = LifeGameBorder
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