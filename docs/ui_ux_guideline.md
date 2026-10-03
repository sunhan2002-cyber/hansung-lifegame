# 6주차 실제 게임 화면 UI/UX 가이드

담당자: 정원률

작성 기준: 2026-10-03, 6주차 UI/UX 작업 브랜치

브랜치: `feat/week6-uiux-wonryul`

## 1. 작업 범위

실제 `GameEvent`를 표시하는 플레이 화면과 선택 결과 화면의 가독성·스크롤·접근성을 개선한다. 긴 본문과 선택지, 작은 화면, 확대 글꼴에서도 마지막 선택지와 다음 사건 버튼에 접근할 수 있도록 구성한다.

| 구분 | 파일 | 역할 |
| --- | --- | --- |
| 플레이 화면 | `app/src/main/java/com/example/lifegame/ui/screens/GameScreen.kt` | 사건·현재 지표·선택지를 하나의 스크롤 목록으로 배치 |
| 결과 화면 | `app/src/main/java/com/example/lifegame/ui/screens/ChoiceResultScreen.kt` | 선택 문구·결과 본문·실제 지표 변화·다음 사건 버튼 표시 |
| 선택지 | `app/src/main/java/com/example/lifegame/ui/components/ChoiceButton.kt` | 긴 선택지의 줄바꿈과 버튼 높이 확장 |
| 사건 카드 | `app/src/main/java/com/example/lifegame/ui/components/EventCard.kt` | 성장 단계·돌발 배지·제목·이미지 대체 영역·본문 구분 |
| 이미지 대체 영역 | `app/src/main/java/com/example/lifegame/ui/components/EventImageBox.kt` | 이미지 준비 안내, 개발용 이미지 ID 비노출 |
| 지표 | `app/src/main/java/com/example/lifegame/ui/components/StatBar.kt` | 지표 이름·수치·막대와 접근성 정보 표시 |
| UI 회귀 검증 | `app/src/androidTest/java/com/example/lifegame/ui/Week6UiTest.kt` | 제한된 화면과 큰 글씨에서 화면 계약 검증 |
| 실제 게임 흐름 검증 | `app/src/androidTest/java/com/example/lifegame/ui/Week6GameplaySmokeTest.kt` | 실제 Activity·assets로 두 사건을 진행하고 검토용 화면 저장 |
| 가이드 | `docs/ui_ux_guideline.md` | 현재 구현·검증 범위·제출 절차 설명 |

테마, 게임 엔진, 사건 JSON과 데이터 모델의 동작은 이번 UI/UX 수정 범위에 포함하지 않는다.

## 2. 화면과 데이터 연결 계약

### GameScreen

```kotlin
GameScreen(
    event = currentGameEvent, // GameEvent
    stats = currentStats,    // Stats
    onChoose = onChoose,     // (Choice) -> Unit
    onShowEndingForTest = onShowEndingForTest,
)
```

- 사건 정보는 `GameEvent.title`, `text`, `imageId`, `stage`, `type`, `choices`에서 읽는다.
- 성장 단계는 `INFANT` → 영아기, `CHILD` → 유년기, `TEEN` → 청소년기, `ADULT` → 성인기로 표시한다.
- `event.type == EventType.SPECIAL`인 사건에 돌발 배지를 표시한다.
- 선택지는 `event.choices` 순서대로 표시한다. 문구 앞에 A, B, C 등의 표식을 붙이며, 전달되지 않은 빈 선택지를 만들지 않는다.
- 클릭 시 해당 목록의 **원본 `Choice` 객체**를 `onChoose(choice)`에 전달한다. 표시용 문구 변경을 위해 객체를 재생성하거나 `choiceId`, `statDelta`, `addFlags` 등을 변경하지 않는다.
- `onShowEndingForTest`는 기존 프론트엔드 호출과의 호환을 위해 인자만 유지한다. 플레이 화면에는 임시 결말 바로가기 버튼이 없으며 이 콜백을 호출하지 않는다.
- `event.eventId`가 바뀌면 목록을 맨 위로 이동해 새 사건의 제목부터 읽도록 한다.

화면 구조:

```text
시스템 안전 영역을 적용한 전체 화면
└─ LazyColumn (최대 폭 720dp, 가로·세로 여백 16dp)
   ├─ EventCard
   ├─ 현재 지표 제목 + StatGrid
   ├─ 어떻게 할까요? 제목
   ├─ 선택지 A
   ├─ 선택지 B
   └─ 실제 데이터에 있는 나머지 선택지
```

본문과 선택지를 같은 `LazyColumn`에 넣는다. 선택지를 화면 하단에 고정하거나 스크롤 밖에 두지 않는다. 각 선택지가 개별 목록 항목이므로 긴 선택지 여러 개가 화면 높이를 넘더라도 끝까지 스크롤할 수 있다. 카드·버튼·지표 내부에는 중첩 스크롤을 추가하지 않는다.

### StatGrid

- `Stats`의 건강, 운동능력, 지력, 사회성, 경제력, 행복, 운을 정해진 순서로 표시한다.
- **지표 영역의 가용 폭이 360dp 미만이거나 `fontScale >= 1.5f`이면 1열**, 그 외에는 2열이다. 기준은 기기 전체 폭이 아니라 화면 여백을 제외한 지표 영역의 폭이다.
- 2열일 때 마지막 지표 오른쪽에 같은 폭의 빈 영역을 두어 다른 지표와 너비를 맞춘다.
- 세로 간격은 12dp, 2열의 가로 간격은 16dp다.

### ChoiceResultScreen

- `UiChoiceResult`와 `onNext: () -> Unit`을 받는다.
- “선택 결과” → “내가 한 선택”과 `result.choice.label` → `result.engineResult.resultText` → “변화한 지표” → “다음 사건” 순서로 한 `LazyColumn`에 표시한다.
- 지표 변화는 선택지의 명목상 `statDelta`가 아니라 엔진에서 실제로 적용한 **`engineResult.appliedDelta`**를 사용한다. 예를 들어 건강이 98일 때 명목 변화가 +10이고 실제 적용량이 +2라면 화면에는 +2를 표시한다.
- 변화량 0인 지표는 생략한다. 모든 변화량이 0이면 “변화 없음”을 표시한다.
- 양수는 `+`, 음수는 `-` 부호를 표시해 색에 의존하지 않고 방향을 알 수 있게 한다.
- “다음 사건”은 전달받은 `onNext`를 호출한다. 진행 규칙과 다음 사건 결정은 화면에서 계산하지 않는다.
- `result.eventId` 또는 `result.choice.choiceId`가 바뀌면 결과 목록을 맨 위로 이동한다.
- 플레이 화면과 동일하게 안전 영역, 최대 폭 720dp, 16dp 여백을 적용한다.

## 3. 공통 컴포넌트 계약

### ChoiceButton / ChoiceButtonGroup

- `ChoiceButton(text, onClick, modifier)`의 기존 인자와 기본값을 유지한다.
- 최소 높이 56dp, 모서리 12dp, 가로·세로 내부 여백 16dp를 사용한다.
- 문구는 `bodyLarge`, 왼쪽 정렬, 줄바꿈을 사용한다. 최대 줄 수나 고정 높이를 지정하지 않아 큰 글씨와 긴 선택지에 따라 버튼이 늘어난다.
- `ChoiceButtonGroup<T>(choices, choiceText, onChoose, modifier)`도 호환용으로 유지한다. 항목 간격은 10dp이며 원본 항목을 콜백에 전달한다.
- 실제 `GameScreen`은 스크롤 목록의 각 항목에 `ChoiceButton`을 직접 배치한다. 그룹 자체는 스크롤이나 하단 고정을 처리하지 않는다.

### EventCard

- 인자: `modifier`, `title`, `description`, `imageId`, `stage`, `isSpecial`. 기존 기본값을 유지한다.
- 성장 단계 → 돌발 배지(해당 시) → 제목 → 이미지 대체 영역 → 본문 순서다. 배지와 제목을 별도 행으로 배치하여 긴 제목의 폭을 확보한다.
- 빈 단계·제목·본문은 생략한다. 제목과 본문은 줄바꿈하며 카드 높이가 내용에 따라 늘어난다.
- 제목은 접근성 제목(`heading`)으로 제공한다.
- 모서리 16dp, 내부 여백 16dp, 요소 간격 12dp다.
- 돌발 사건은 “돌발 이벤트” 문구와 함께 `tertiaryContainer`/`onTertiaryContainer` 배지, `tertiary` 1dp 테두리를 사용한다. 카드 배경은 `surface`에 `tertiaryContainer`를 25% 섞는다. 문구와 테두리도 제공하므로 색만으로 사건을 구분하지 않는다.

### EventImageBox

- 인자: 필수 `imageId`, 선택적인 `modifier`, `fallbackText`. 기존 호출 계약을 유지한다.
- 현재는 **실제 이미지를 로딩하지 않는 대체 영역**이다. `imageId`는 전달받되 사용자 화면에 표시하지 않는다. 빈 ID도 “이미지 ID 없음” 같은 개발용 문구로 노출하지 않는다.
- 기본 안내는 “이미지를 준비 중입니다”이며 빈 `fallbackText`도 이 문구로 대체한다.
- 폭의 9/16을 계산한 뒤 112~160dp 범위로 제한한 값을 최소 높이로 사용한다. 폭이 제한되지 않은 경우 최소 높이는 112dp다. 고정 높이가 아니므로 큰 글씨로 안내가 길어지면 영역이 더 늘어날 수 있다.
- 모서리와 여백은 16dp, 색상은 `surfaceVariant`/`onSurfaceVariant`다.
- 실제 이미지 리소스 매칭과 로딩은 별도 담당 작업이며 이 컴포넌트의 구현 완료 항목으로 기록하지 않는다.

### StatBar

- `StatBar(name, value, modifier)`의 기존 인자를 유지한다.
- 표시 수치와 진행률 모두 `value.coerceIn(0, 100)`을 사용한다. 원본 게임 상태를 수정하지 않는다.
- 이름에 남은 가로 폭을 배정하고 줄바꿈을 허용해 숫자와 겹치지 않게 한다. 숫자는 굵게 표시한다.
- 이름·수치 아래에 높이 6dp의 진행 막대를 표시한다.
- 접근성에는 이름, `값 / 100` 상태 설명, 0~100 진행률을 하나의 노드로 제공해 같은 지표의 중복 읽기를 줄인다.

## 4. 다른 담당자와의 경계 및 후속 확인

| 항목 | 이번 UI/UX 변경의 범위 | 별도 담당 또는 후속 확인 |
| --- | --- | --- |
| 이름 입력과 호칭 | 전달받은 문자열을 표시 | 이름 입력, `{name}` / `{nameCall}` 치환은 프론트엔드 담당 |
| 사건·성장 단계·결말 | 전달받은 사건과 엔진 결과를 표시 | 랜덤 진행, 돌발 조건, 성장 단계 전환, 결말 우선순위는 엔진 담당 |
| 선택 처리 | 원본 `Choice`와 기존 콜백 유지 | 선택 중복 방지·상태 전환 등 진행 로직은 기존 프론트엔드·엔진 계약을 따름 |
| 이미지 | 안내 영역과 ID 비노출 | 이미지 ID 매칭·실물 리소스 로딩은 데이터·연결 담당과 확인 |
| 데이터 분량 | 임의의 긴 텍스트도 표시 가능한 구조 | 사건·돌발 이벤트·결말 추가와 데이터 검증은 각 데이터 담당 |
| 테마 | 기존 `LifeGameTheme`의 의미별 색상·글꼴 사용 | 이번 작업에서 테마 파일은 변경하지 않음 |

`GameScreen.kt`와 `ChoiceResultScreen.kt`는 프론트엔드 작업과 겹칠 수 있다. 병합 시 입력 인자, 원본 `Choice` 콜백, 실제 적용 변화량, 사건 변경 시 스크롤 초기화가 함께 유지되는지 확인한다.

## 5. 검증 기준과 기록

### 자동 UI 회귀 검증

`Week6UiTest`의 8개 검증은 Compose 화면을 **320×400dp, 글꼴 배율 2.0**으로 제한해 실행한다. 에뮬레이터 창을 축소하는 방식이 아니라 테스트 콘텐츠의 크기와 `LocalDensity.fontScale`을 지정한다. 텍스트는 행별 글자 폭·높이, 말줄임 여부, 마지막 글자까지 배치됐는지와 긴 문구의 줄바꿈을 검사한다. Compose 1.7의 단순 문자열 Text는 접근성 측정값의 문단 너비를 부모 너비로 재구성하므로 `hasVisualOverflow`만으로 판정하지 않는다. 행별 폭 검사는 소수점 반올림에 한해 1px을 허용한다.

`Week6GameplaySmokeTest`는 기본 기기 화면에서 실제 `MainActivity`와 assets를 사용한다. 새 게임 → 아기 1 선택 → 게임 시작 → A 선택 → 결과 → 다음 사건을 두 번 검증한다. 검토용 `game.png`, `choices.png`, `result.png`는 테스트 앱의 외부 파일 영역 `Android/data/com.example.lifegame/files/week6-uiux/`에 저장하며 Git에는 포함하지 않는다.

2026-10-03 Pixel 9 / Android 16(API 36) 에뮬레이터에서 AndroidJUnitRunner로 실행했다. 아래 8개와 실제 게임 흐름 1개를 합해 `OK (9 tests)`를 확인했다.

| 번호 | 테스트 함수 | 확인 기준 | 최종 결과 |
| --- | --- | --- | --- |
| 1 | `threeLongChoicesCanBeScrolledToAndReturnTheirOriginalObjects` | 긴 선택지 3개에 모두 스크롤·터치 가능, 각 문구 줄바꿈·넘침 없음, 선택마다 해당 원본 `Choice`가 한 번 전달됨 | 통과 |
| 2 | `changingTheEventResetsTheScrollToItsHeading` | 마지막 선택지까지 내려간 후 사건 ID가 바뀌면 추가 테스트 스크롤 없이 새 제목·성장 단계가 보임 | 통과 |
| 3 | `allSevenStatsRemainReachableAndExposeClampedValues` | 지표 7개에 모두 접근 가능, 건강 -10은 0·행복 130은 100으로 표시하는 접근성 상태와 진행률 제공 | 통과 |
| 4 | `specialEventKeepsLongHeadingBodyAndMissingImageMessageReadable` | 돌발 배지·긴 제목·긴 본문·빈 이미지 ID의 안내 문구에 접근 가능, 텍스트 넘침 없음 | 통과 |
| 5 | `resultUsesAppliedDeltaInsteadOfNominalChoiceDeltaAndOmitsZeroStats` | 명목 +10 대신 실제 건강 +2·경제력 -3 표시, 변화 없는 지표와 불필요한 “변화 없음” 문구 생략 | 통과 |
| 6 | `resultWithNoAppliedChangesSaysThereIsNoChange` | 모든 실제 변화량이 0이면 “변화 없음” 표시, 명목 변화량과 개별 지표 생략 | 통과 |
| 7 | `longResultAndNextButtonRemainReachableWithLargeText` | 긴 결과 본문 줄바꿈·넘침 없음, 다음 사건 버튼까지 접근·터치 시 콜백 한 번 호출 | 통과 |
| 8 | `gameDoesNotExposeTheTemporaryEndingShortcut` | 전체 스크롤 목록을 탐색해도 “결말 화면 확인 (임시)” 동작이 없음 | 통과 |

### 최종 실행 기록

| 검사 | 실행 명령 | 결과·환경·확인한 로그 |
| --- | --- | --- |
| 단위 테스트 | `.\gradlew.bat test` | 통과. Debug·Release 각각 35개, 실패 0개 |
| 디버그 빌드 | `.\gradlew.bat assembleDebug` | 통과. JDK 21.0.11 / Gradle 8.9 / compileSdk 35 |
| Gradle 기기 테스트 연결 | `.\gradlew.bat connectedDebugAndroidTest` | UTP 호스트가 테스트 시작 전 종료해 실행 실패. 통과로 기록하지 않음 |
| 기기 UI 테스트 직접 실행 | `adb shell am instrument -w -r com.example.lifegame.test/androidx.test.runner.AndroidJUnitRunner` | Pixel 9 / API 36, 9개 통과·실패 0개 (`OK (9 tests)`) |
| 정적 검사 | `.\gradlew.bat lintDebug` | 오류 0개, 기존 파일의 경고 7개(라이브러리 업데이트 6개·앱 아이콘 1개) |

한 차례 실행한 빌드나 테스트 결과를 이후 변경까지 포함한 최종 검증으로 재사용하지 않는다. 팀장 검증과 자동 검사 결과도 구분한다.

### 수동 확인이 필요한 항목

- 실제 게임의 두 사건 진행은 자동 검증했고, 기본 글꼴의 돌발 카드·2열 지표·선택지와 결과 화면 캡처를 검토했다. 결말까지의 전체 회차와 장시간 진행은 팀장 검증에서 추가 확인한다.
- 기본 글꼴에서 충분한 가용 폭의 2열 지표, 360dp 미만 폭 및 글꼴 배율 1.5 이상에서 1열 지표를 각각 확인한다. 위 자동 테스트는 작은 폭과 큰 글꼴을 동시에 적용하므로 경계값 각각을 독립 검증하지는 않는다.
- 실제 기기의 시스템 상태 표시줄·내비게이션 영역, 가로 화면, 기본/큰 글씨에서 콘텐츠에 접근 가능한지 확인한다.
- 돌발 배지·테두리·배경의 색 대비와, 일반 사건과의 시각적 구분을 확인한다.
- 비어 있거나 긴 이미지 ID가 사용자 화면에 노출되지 않는지 확인한다. 자동 이미지 안내 검증은 빈 ID 사례를 사용한다.
- TalkBack에서 제목 이동과 지표 이름·값·진행률 읽기를 확인한다. 접근성 속성 자동 검증과 실제 읽기 경험은 별도다.
- 실물 이미지 로딩, 이름 치환, 데이터 분량, 엔진 분기·결말 검증은 이번 UI 테스트로 완료 처리하지 않는다.

확인하지 못한 항목은 PR과 Notion에 “미확인”으로 기록한다. 캡처나 실행 로그를 확인하지 않고 시각적 검증 완료로 표시하지 않는다.

## 6. 커밋·PR·Notion 제출 절차

1. 작업 시작 전 GitHub `main`을 최신화하고 기존 로컬 변경을 보존한다.
2. 개인 브랜치 `feat/week6-uiux-wonryul`에서 작업한다. `main`에 직접 push하지 않는다.
3. 실제 작업 파일만 검토·커밋한다. 불필요한 ZIP, `build` 폴더, 캡처 파일, 로컬 설정은 올리지 않는다.
4. 개인 브랜치에 push한 뒤 GitHub PR을 생성한다. 제목은 **`[6주차] UI/UX - 정원률`**로 작성한다.
5. PR 설명에는 작업 파일, 구현 내용, 실제 테스트 결과, 아직 부족하거나 확인이 필요한 부분을 적는다.
6. Notion 과제 제출 칸반보드의 정원률 카드에 PR 링크, 작업 요약, 테스트 결과, 확인 필요한 부분을 등록한다. PR 생성만으로 제출을 완료 처리하지 않는다.
7. 팀장이 코드와 Android Studio 실행·빌드·테스트를 검증한다. 통과 후 팀장이 PR에 “검증 완료”를 남기고 merge한다. 수정 요청이 있으면 개인 브랜치에서 수정하고 PR·Notion 기록을 갱신한다.

제출 순서는 **개인 브랜치 push → GitHub PR → Notion 카드 제출 → 팀장 검증 → merge**다.
