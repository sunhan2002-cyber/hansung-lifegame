package com.example.lifegame.ui.theme

import androidx.compose.ui.graphics.Color

// Week7 확정 UI 팔레트 (2026-10-12). 주요 색상의 원본은 Week7DesignTokens에 유지한다.

// 화면 바탕 (종이색). 필름 구멍도 바탕이 비쳐 보이도록 같은 색을 쓴다.
val LifeGamePaper = Week7DesignTokens.Paper

// 선택지 카드, 능력치 버튼
val LifeGameCard = Week7DesignTokens.Card

// 글자, 필름, 주 버튼 (먹색)
val LifeGameInk = Week7DesignTokens.Ink

// 보조 글자
val LifeGameInkSecondary = Week7DesignTokens.SecondaryInk
val LifeGameInkTertiary = Week7DesignTokens.SecondaryInk

// 팝업 첫 문장 등 본문 보조
val LifeGameBodyInk = Week7DesignTokens.Ink

// 선, 테두리
val LifeGameRule = Week7DesignTokens.Border

// 옅은 면 (A/B 키, 상태 표시)
val LifeGameMuted = Color(0xFFF2F3F5)

// 능력치 상승 / 하락
val LifeGameStatUp = Color(0xFF1E5BC6)
val LifeGameStatUpSoft = Color(0xFFE9F0FB)
val LifeGameStatDown = Color(0xFFC9423A)
val LifeGameStatDownSoft = Color(0xFFFBEAE8)

// 돌발 사건 표시 글자 (버건디). 돌발 사건이어도 바탕과 필름 색은 바꾸지 않는다.
val LifeGameSpecial = Color(0xFF8A2433)

// 그림이 준비되기 전 필름 안 자리
val LifeGamePicture = Color(0xFFECEEF1)
val LifeGameSpecialPicture = Color(0xFFEFE2E5)
