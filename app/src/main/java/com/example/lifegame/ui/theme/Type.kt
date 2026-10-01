package com.example.lifegame.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// 제목과 이야기 문장은 명조(기기 기본 serif, 한글은 Noto Serif CJK), 화면 UI는 기본 고딕
val LifeGameSerif = FontFamily.Serif

val LifeGameTypography = Typography(

    // 앱 시작 화면의 큰 제목
    displaySmall = TextStyle(
        fontFamily = LifeGameSerif,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 40.sp
    ),

    // 캐릭터 이모지 등 매우 큰 요소
    displayMedium = TextStyle(
        fontSize = 40.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 48.sp
    ),

    // 화면 제목
    headlineMedium = TextStyle(
        fontFamily = LifeGameSerif,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 32.sp
    ),

    // 사건 제목
    titleLarge = TextStyle(
        fontFamily = LifeGameSerif,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 28.sp
    ),

    // 게임 단계, 주요 버튼, 섹션 제목
    titleMedium = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp
    ),

    // 작은 섹션 제목
    titleSmall = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp
    ),

    // 사건 설명 등 일반 본문
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),

    // 지표 이름, 수치 등
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    ),

    // 작은 안내 문구
    bodySmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 18.sp
    ),

    // A/B 선택 버튼
    labelLarge = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 22.sp
    )
)

// 게임 화면 전용 글자 스타일 (시안 7)

// 사건 제목
val EventTitleStyle = TextStyle(
    fontFamily = LifeGameSerif,
    fontWeight = FontWeight.Bold,
    fontSize = 23.sp,
    lineHeight = 30.sp,
)

// 사건 문장
val StoryStyle = TextStyle(
    fontFamily = LifeGameSerif,
    fontSize = 16.5.sp,
    lineHeight = 29.sp,
)

// 결과 문장
val ResultStoryStyle = TextStyle(
    fontFamily = LifeGameSerif,
    fontSize = 15.5.sp,
    lineHeight = 26.5.sp,
)

// "돌발 사건" 같은 작은 표시
val MarkLabelStyle = TextStyle(
    fontSize = 12.5.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.04.em,
)
