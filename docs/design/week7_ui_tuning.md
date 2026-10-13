# Week7 확정 UI 수치와 적용 범위

- 디자인 수치 확정: 2026-10-12 / 설명 정리: 2026-10-13
- 사용자 결정: 값과 Tokens/Color/Type/Theme 구조는 유지한다. 중복 기능 구현은 김재겸 담당으로 넘긴다.
- [레이아웃 가이드](week7_screen_layout_guide.md) · [인계 자료](week7_handoff.md) · [중복 구현 정리](week7_frontend_alignment.md)

## 1. 이 파일에서 조정되는 것

`Week7DesignTokens.kt`는 화면 모양의 확정 기준을 보관한다. Figma 원본 측정값이 아니다. **현재 코드가 참조하는 값만 실행 화면에 영향을 준다.** 진행 기능을 제거하면서 참조가 없어졌지만 디자인 인계를 위해 보관한 값도 있다.

현재 로컬에는 이미지 검수 화면과 기존 HEAD의 게임이 있다. 김재겸 PR의 v4 헤더·장 표지·자동 진행·결정은 이 브랜치에 없으며, 실제 기능은 해당 PR 구현을 기준으로 한다. Tokens를 변경했다고 그 PR 화면까지 자동으로 바뀌지 않는다.

## 2. Tokens 기준값과 현재 참조

아래의 `현재 참조`는 코드에서 해당 Tokens 값을 직접 읽는 위치다. 색상·타이포그래피를 통해 영향을 받는 화면은 더 넓지만, 화면 안의 모든 상수를 일괄 변경하지는 않는다.

| 변수 | 확정 값 | 현재 참조·변경 영향 |
| --- | --- | --- |
| Paper | #F7F4EE | Color.kt → 종이 바탕·필름 천공 |
| Ink | #191919 | Color.kt → 주요 글자·필름·버튼·스탯바 색 |
| SecondaryInk | #6F6A63 | Color.kt, 이미지 검수 → 보조 글자 |
| Card | #FFFFFC | Color.kt → 카드·능력치 버튼 바탕 |
| Border | #DBD5CC | Color.kt → 테두리·스탯바 빈 부분 |
| ScreenPadding | 12dp | StartScreen·CharacterSelectScreen·ImageSamplePreviewScreen의 여백 |
| SectionGap | 16dp | EventCard·StartScreen·ImageSamplePreviewScreen의 영역 간격 |
| CardPadding | 14dp | ChoiceButton·EventCard·EventImageBox·StartScreen의 내부 여백 |
| ChoiceGap | 10dp | ChoiceButtonGroup·ImageSamplePreviewScreen의 항목/조작 간격 |
| CardCorner | 8dp | ChoiceButton·EventCard·EventImageBox·StartScreen·ImageSamplePreviewScreen의 모서리 |
| ButtonMinHeight | 56dp | ChoiceButton·StartScreen·ImageSamplePreviewScreen의 주요 조작 최소 높이. 현재 배속 버튼에는 연결되지 않음 |
| TitleSize / TitleLineHeight | 24sp / 32sp | Type.kt의 headlineMedium·titleLarge·EventTitleStyle |
| BodySize / BodyLineHeight | 16sp / 26sp | Type.kt의 bodyLarge·StoryStyle·ResultStoryStyle. titleSmall·labelLarge는 크기만 참조 |
| FilmBandHeight | 14dp | FilmStrip의 위·아래 띠 두께 |
| FilmSideBorder | 10dp | FilmStrip의 좌우 테두리, 이미지 검수의 그림 크기 계산 |
| FilmHoleWidth / FilmHoleHeight / FilmHolePitch | 8dp / 6dp / 16dp | FilmStrip의 천공 폭·높이·간격 |
| ImageAspectRatio | 4:3 | EventImageBox·ImageSamplePreviewScreen의 그림 영역 높이 계산. 파일 해상도를 변경하지 않음 |

아래 값은 **현재 참조 없음 / 디자인 인계 기준으로 보관** 상태다. 값을 바꿔도 현재 실행 화면은 바뀌지 않는다. 프론트가 실제 화면에 연결할 때 적용한다.

| 변수 | 확정 값 | 후속 디자인 용도 |
| --- | --- | --- |
| YearLabelSize / YearLabelLineHeight | 13sp / 18sp | 시기명 옆 N번째 해 |
| FocusLabelSize / FocusLabelLineHeight | 14sp / 20sp | 초점 안내·이름 |
| DecisionContentGap | 12dp | 사건 유형·제목·본문 사이 |
| DecisionSectionPadding | 12dp | 선택지 영역 위아래 |
| ResultPadding | 16dp | 결과 카드 내부 |
| ResultContentGap | 10dp | 결과 항목 사이 |
| ChoiceBadgeGap | 12dp | A/B 표식과 선택 문장 사이 |
| DecisionMetaSize / DecisionMetaLineHeight | 13sp / 20sp | 가능성·변화 수치·돌발 보조 안내 |
| IncidentOuterPadding | 24dp | 돌발 팝업 바깥 여백 |
| IncidentContentPadding | 20dp | 돌발 팝업 내부 여백 |

## 3. Tokens 이외의 설정

| 파일 | 역할 |
| --- | --- |
| [Color.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Color.kt) | 기본 색상은 Tokens에서 받고 상승·하락·돌발·옅은 면·대체 그림 색상도 정의 |
| [Type.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Type.kt) | 명조/고딕·굵기·행간·자간·텍스트 스타일 |
| [Theme.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Theme.kt) | 색상·글자 스타일을 Material 역할에 연결 |

구조와 값은 그대로 유지한다. 별도 값은 앱 로고 32sp/40sp, titleMedium 18sp/24sp, bodyMedium 14sp/20sp, bodySmall 12sp/18sp, MarkLabelStyle 12.5sp·굵게·자간 0.04em이다. titleSmall·labelLarge의 행간은 별도 22sp이므로 BodyLineHeight 변경으로 모든 행간이 바뀌지는 않는다.

필름은 호출 화면에서 전달한 높이를 사용한다. ImageAspectRatio를 변경해도 기존 GameScreen의 별도 높이 계산이나 모든 이미지 영역이 자동으로 바뀌지 않는다. 실제 300×225 PNG 제작은 화면 표시와 별도다.

## 4. 프론트 기준과 디자인 적용 차이

비교 기준은 김재겸 PR #28 커밋 `9c4818d`다. 해당 구현은 이 브랜치에 가져오지 않았다.

- 능력치 버튼·시트·스탯바 공유는 PR의 공통 구현을 따른다. 정원률의 신규 StatsSheet와 헤더 기능 변경은 제거/철회했다.
- 문구는 PR의 `N번째 해`, `이번 시기 초점`, `N해 고정`, 결과 뒤 `계속하기`/`인생 정리하기`를 따른다.
- 능력치 버튼 최소 48dp·모서리 8dp·글자 12sp, 패널 좌우 24dp·하단 32dp, 작은 폭·큰 글씨의 1열 처리는 PR 코드 기준이다. Tokens에서 이 값들을 변경하는 구조는 아니다.
- 확정 디자인과 차이: 헤더 좌우 여백은 디자인 12dp / PR 20dp, 필름 띠는 14dp / 21dp, 장 표지 제목은 24sp/32sp / 34sp/42sp, 이야기 스타일은 16sp/26sp / 16.5sp/29sp다.
- 디자인의 전체 장면 표시·4:3 기준과 PR의 가운데 잘라 채우기·화면 높이 제한도 적용 협의 대상이다.

이 차이를 김재겸이 실제 화면에 적용하고 정원률이 모양을 확인한다. 진행 시간·초점 효과·판정·저장은 프론트·엔진 담당이다. 검토용 PreviewYearDurationMs와 독립 진행 미리보기는 제거했다.

## 5. 이미지 검수 실행

1. [run-preview.cmd](../../run-preview.cmd) 또는 Android Studio의 app/debug를 실행한다.
2. 첫 화면 **디자인 미리보기**를 누르면 **예시 이미지 검수**로 바로 들어간다.
3. **이전 이미지 / 다음 이미지 / 장면 고르기**로 8장을 확인한다.
4. **다시 보기**는 첫 장으로, **돌아가기**는 첫 화면으로 이동한다.

예전 화면 고르기·장 표지·자동 진행·결정 미리보기는 없다. 검수 화면은 실제 게임 상태·엔진·저장과 연결하지 않는다. 변경된 직접 진입·조작과 큰 글씨 표시는 사용자 확인 대상이다.

이 PC의 Gradle JDK는 `C:\Users\bestw\.jdks\jbr-21.0.11`이다. 기존 JDK 25 실행 실패 기록이 있어 JDK 21을 유지한다.

## 6. 검증·보류 상태

- 정리 후 앱·검수 APK 빌드·Release Kotlin 컴파일 통과, 단위 테스트 Debug 51개 + Release 51개 통과. [상세 기록](week7_handoff.md).
- 제거 전 장 표지·자동 진행·결정 화면 테스트는 과거 구현 이력이다. 현재 기능 통과 근거로 사용하지 않는다.
- 기존 이미지 8장 로딩·전환·선택 검사 통과. 큰 글씨에서 제목 overflow를 수정하고 빌드는 통과했으나 해당 자동 화면 검사는 재실행하지 않았다.
- 사용자 2026-10-13 이미지 8장 육안 검수 완료 보고 유지. 현재 설명 수정에서는 추가 빌드·에뮬레이터 검사를 실행하지 않는다.
- 원본은 1448×1086 PNG·4:3, 300×225 처리 보류. 최종 imageId/fileName·규격 검증은 김선한 담당, 실제 연결은 김재겸 담당이다.
