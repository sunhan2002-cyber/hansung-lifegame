package com.example.lifegame.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Week7 확정 UI 기준 (2026-10-12). 현재 값과 파일 구조를 유지합니다.
 * Figma 원본 측정값이 아니라, 실행하며 검토한 값을 사용자 결정으로 확정한 기준입니다.
 * dp = 화면 배치 단위, sp = 시스템 글자 크기를 반영하는 단위.
 * 화면 모양의 인계 기준입니다. 실제 진행 기능은 프론트·백엔드가 담당합니다.
 * 적용 범위: docs/design/week7_ui_tuning.md, 화면/도트 규칙: docs/design의 각 가이드.
 */
object Week7DesignTokens {
    // 화면 전체 종이색 / 제목·본문 / 보조 문구 / 선택 카드 / 테두리
    val Paper = Color(0xFFF7F4EE)
    val Ink = Color(0xFF191919)
    val SecondaryInk = Color(0xFF6F6A63)
    val Card = Color(0xFFFFFFFC)
    val Border = Color(0xFFDBD5CC)

    // 커질수록 내용 폭이 좁아지고 글자의 줄바꿈과 이미지 높이가 달라집니다.
    val ScreenPadding = 12.dp
    // 이미지·사건·선택지 영역 사이 간격. 커질수록 아래 영역이 더 내려갑니다.
    val SectionGap = 16.dp
    // 카드 테두리와 글자 사이 거리 / 선택 카드 사이 거리 / 카드의 둥근 정도
    val CardPadding = 14.dp
    val ChoiceGap = 10.dp
    val CardCorner = 8.dp
    // 최소 높이이므로 긴 글자는 버튼을 더 높게 만듭니다.
    val ButtonMinHeight = 56.dp

    // 시기·사건 제목 크기와 행간. 글자 크기보다 행간을 크게 유지하세요.
    val TitleSize = 24.sp
    val TitleLineHeight = 32.sp
    // 시기 제목 옆 작은 회색 연도 / 진행도 아래 중점 문구의 크기·행간
    val YearLabelSize = 13.sp
    val YearLabelLineHeight = 18.sp
    val FocusLabelSize = 14.sp
    val FocusLabelLineHeight = 20.sp

    // 본문과 선택지 크기 / 본문 행간. 행간을 키우면 본문 아래 요소가 내려갑니다.
    val BodySize = 16.sp
    val BodyLineHeight = 26.sp

    // 결정 제목·본문 사이 / 선택지 영역 위아래 / 결과 카드 안쪽과 요소 사이
    val DecisionContentGap = 12.dp
    val DecisionSectionPadding = 12.dp
    val ResultPadding = 16.dp
    val ResultContentGap = 10.dp
    // A/B 글자와 선택 문장 사이 / 가능성·영향·변화 수치 보조 글자
    val ChoiceBadgeGap = 12.dp
    val DecisionMetaSize = 13.sp
    val DecisionMetaLineHeight = 20.sp
    // 돌발 팝업: 화면 가장자리부터 거리 / 카드 안쪽 여백
    val IncidentOuterPadding = 24.dp
    val IncidentContentPadding = 20.dp

    // 필름 위·아래 검은 띠 두께. 장면 자체의 4:3 비율에는 영향을 주지 않습니다.
    val FilmBandHeight = 14.dp
    // 필름 좌우 검은 테두리. 커질수록 장면 폭과 높이가 함께 줄어듭니다.
    val FilmSideBorder = 10.dp
    // 필름 천공(구멍)의 폭·높이·간격
    val FilmHoleWidth = 8.dp
    val FilmHoleHeight = 6.dp
    val FilmHolePitch = 16.dp

    // 필수 규격: 300×225px의 4:3. 크기 조정은 ScreenPadding으로 합니다.
    const val ImageAspectRatio = 4f / 3f
}
