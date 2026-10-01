# 5주차 UI/UX 컴포넌트 적용 가이드

담당자: 정원률  
마감일: 2026-09-30  
작성 기준: 2026-09-26 로컬 UI 컴포넌트

## 1. 작업 범위와 제출물

UI/UX 담당은 공통 컴포넌트와 테마를 제공한다. 이번 제출 기준에서는 기존 화면, 화면용 데이터 모델, 게임 엔진과 ViewModel을 유지하며, 실제 사건 데이터와 컴포넌트의 연결은 FE가 담당한다.

제출 ZIP에는 다음 8개 파일만 프로젝트 상대 경로를 유지해 포함한다. 이번에 수정하지 않은 테마 파일도 포함된다.

```text
docs/ui_ux_guideline.md
app/src/main/java/com/example/lifegame/ui/theme/Color.kt
app/src/main/java/com/example/lifegame/ui/theme/Theme.kt
app/src/main/java/com/example/lifegame/ui/theme/Type.kt
app/src/main/java/com/example/lifegame/ui/components/ChoiceButton.kt
app/src/main/java/com/example/lifegame/ui/components/EventCard.kt
app/src/main/java/com/example/lifegame/ui/components/EventImageBox.kt
app/src/main/java/com/example/lifegame/ui/components/StatBar.kt
```

`MainActivity.kt`, `GameEngine.kt`, `GameModels.kt`, `LifeGameViewModel.kt`, `LifeGameUiState.kt`, `ui/screens/*.kt`는 이 제출물에 포함하지 않는다. 로컬 화면 테스트를 위해 임시 수정한 부분은 제출·커밋 전에 복원한다. `GameScreen.kt`의 `TEMP_EVENT_CARD_TEST` 연결은 이러한 테스트용 변경이며 UI/UX 제출 범위에 속하지 않는다.

결과물과 검증 자료를 Notion 카드에 먼저 제출하고, 팀장 검증 통과 후 GitHub에 push한다.

## 2. 컴포넌트 사용 방법

### ChoiceButton / ChoiceButtonGroup

`ChoiceButton.kt` 안에 단일 버튼과 선택지 묶음을 함께 제공한다.

```kotlin
ChoiceButton(text = "A. 친구를 도와준다", onClick = { /* 선택 처리 */ })
```

- `ChoiceButton`은 `text`, `onClick`, 선택적인 `modifier`를 받는다.
- 최소 높이는 56dp, 모서리는 12dp, 내부 여백은 가로 16dp·세로 14dp다.
- 최대 줄 수와 고정 높이를 지정하지 않아 긴 선택지는 줄바꿈되고 버튼 높이가 늘어난다.
- `ChoiceButtonGroup<T>`는 `choices: List<T>`, `choiceText: (T) -> String`, `onChoose: (T) -> Unit`, 선택적인 `modifier`를 받는다.
- 목록 순서대로 필요한 개수만 표시한다. 2개면 2개, 3개면 3개이며 빈 C 자리를 만들지 않는다. 버튼 간격은 10dp다.
- A/B/C 표시는 `choiceText`에서 포함한다. 클릭하면 원본 선택지 객체를 `onChoose`에 전달한다.
- 버튼 묶음 자체는 하단 고정이나 스크롤을 처리하지 않는다. 호출 화면에서 배치한다.

### EventCard

```kotlin
EventCard(
    stage = "유년기",
    title = "친구가 대사를 잊었다",
    description = "발표회 도중 친구가 대사를 잊어버렸다. 어떻게 도와줄까?",
    imageId = "example_child_event", // 사용법 설명을 위한 예시 ID
    isSpecial = false,
)
```

- 입력: `modifier`, `title`, `description`, `imageId`, `stage`, `isSpecial`.
- 모든 인자에 기본값이 있어 기존 `EventCard()` 호출도 컴파일된다. 기본 호출에서는 빈 이미지 ID의 대체 영역만 표시된다.
- 단계 → 제목과 돌발 라벨 → `EventImageBox` → 본문 순서로 구성한다. 단계는 사건 제목과 별개다.
- 비어 있는 단계·제목·본문은 표시하지 않는다. 제목과 본문은 줄바꿈되며 내용에 따라 카드 높이가 늘어난다.
- 모서리는 16dp, 내부 여백은 16dp, 세로 요소 간격은 12dp다.
- `isSpecial = true`이면 “돌발 이벤트” 라벨을 표시한다. 카드 배경에는 `surface`에 `errorContainer`를 25% 섞은 색을 사용한다.
- `StatGrid`와 선택지는 카드에 포함하지 않는다. 카드 내부에도 별도 스크롤을 넣지 않는다.

### EventImageBox

```kotlin
EventImageBox(
    imageId = "example_child_event",
    fallbackText = "이미지를 준비 중입니다",
)
```

- 입력: 필수 `imageId`, 선택적인 `modifier`, `fallbackText`.
- 현재 구현은 실제 이미지를 로딩하지 않는다. 이미지 연결 전 대체 영역으로 이미지 ID와 안내 문구를 표시한다.
- 기본 안내 문구는 “이미지를 준비 중입니다”다. 빈 안내 문구도 이 기본값으로 대체한다.
- 이미지 ID가 비어 있으면 “이미지 ID 없음”을 표시한다.
- 가로 폭 기준 4:3을 최소 높이로 사용한다. 긴 ID나 큰 글꼴로 내용이 많아지면 높이가 늘어나므로 항상 고정된 4:3 비율은 아니다.
- 모서리는 16dp, 내부 여백은 16dp이며 테마의 `surfaceVariant`와 `onSurfaceVariant`를 사용한다.
- 실제 이미지 리소스와 ID의 대응 및 로딩 방식은 후속 연결 작업에서 정한다.

### StatBar

```kotlin
StatBar(name = "건강", value = -10) // 표시: 0, 진행률: 0
StatBar(name = "행복", value = 130) // 표시: 100, 진행률: 1
```

- 입력: `name`, `value`, 선택적인 `modifier`.
- `value.coerceIn(0, 100)`을 숫자와 진행률에 함께 적용한다. 게임 데이터 자체를 변경하지 않는다.
- 지표 이름과 수치를 한 줄에 표시하고, 아래에 높이 5dp의 막대를 표시한다.
- 지표 7개의 순서와 2열 배치는 기존 화면의 `StatGrid`가 담당한다. `StatGrid`는 이번 제출 파일이 아니다.

## 3. 테마 적용

- `Color.kt`: 공통 색상 정의. 주요 색은 청록색 `#009688`, 돌발 강조색은 `#E76F51`, 돌발 배경색은 `#FFC8C8`이다.
- `Theme.kt`: `LifeGameTheme`에서 색상과 글꼴 스타일을 `MaterialTheme`에 연결한다. 돌발 색상은 `error`와 `errorContainer`에 연결된다.
- `Type.kt`: 제목·본문·버튼 등 공통 글꼴 크기, 굵기, 줄 높이를 정의한다.
- 호출 화면은 `LifeGameTheme` 안에서 컴포넌트를 사용한다. 다른 테마로 감싸면 해당 테마의 색상과 글꼴이 적용된다.
- 이미지 대체 영역의 `surfaceVariant`는 현재 커스텀 테마에서 별도로 지정하지 않은 Material 기본 색상이다.

## 4. FE 연결 기준

아래는 FE가 적용할 배치 구조다. UI/UX 제출물만 교체한다고 실제 화면의 데이터 연결까지 자동으로 바뀌지는 않는다.

```text
전체 화면 Column
├─ 남은 높이를 사용하는 스크롤 Column: weight(1f), verticalScroll
│   ├─ EventCard
│   └─ StatGrid
└─ ChoiceButtonGroup: 콘텐츠에 필요한 높이만 사용
```

1. `EventCard`에는 실제 사건의 단계, 제목, 본문, 이미지 ID, 돌발 여부를 전달한다.
2. 카드가 단계·본문·돌발 라벨을 표시하게 되면 기존 화면의 동일한 표시를 제거해 중복을 방지한다. 카드와 화면 전체의 강조 배경도 함께 검토한다.
3. 사건 카드와 지표를 같은 스크롤 부모 아래 배치한다. 각 컴포넌트 내부에 스크롤을 중복해서 넣지 않는다.
4. 선택지 묶음은 스크롤 영역 밖에 두고 고정 높이·고정 비율을 주지 않는다. 선택지가 늘어나면 상단 스크롤 영역의 높이가 줄어든다.
5. 시스템 상태 표시줄과 하단 내비게이션 영역의 여백은 화면에서 처리한다.

현재 `UiEvent`에는 `choiceA`, `choiceB`가 있고 사건 제목·이미지 ID·선택지 목록은 없다. 기존 모델에 없는 `event.title`, `event.imageId`, `event.choices`를 그대로 참조하면 안 된다. 실제 `GameEvent` 데이터를 화면 모델에 연결하는 작업은 FE가 담당한다.

현재 A/B 화면 모델을 유지할 때의 선택지 연결 예시:

```kotlin
ChoiceButtonGroup(
    choices = listOf(event.choiceA, event.choiceB),
    choiceText = { "${it.label}. ${it.text}" },
    onChoose = onChoose,
)
```

위 예시는 2개만 전달하므로 C가 생기지 않는다. 3개를 표시하려면 FE가 3개 선택지가 담긴 목록을 전달해야 한다. 실제 `GameEvent.choices`의 항목은 `UiChoice`와 필드 구성이 다르므로 표시 문구와 클릭 처리를 해당 모델에 맞춰 연결한다.

## 5. 검증 결과와 제출할 자료

검증일: 2026-09-26  
수동 검증자: 정원률  
기록 근거: 담당자가 임시 테스트 화면에서 직접 검증을 완료했다고 확인한 결과와 제공한 앱 화면. 빌드·lint 결과는 실행 로그로 별도 확인했다.

검증 환경과 입력:

- 앱 실행 환경: Pixel 9 API 36.0 에뮬레이터.
- 작은 화면: `GameScreen.kt`에 구성한 320×568dp Preview의 선택지 2개·3개 사례. 에뮬레이터 창 확대/축소 비율과는 별개다.
- 긴 문장: 60자 이상 선택지와 번호가 붙은 긴 본문 5개 문단.
- 지표: 기존 7개 지표 중 건강에 -10, 행복에 130을 화면 입력값으로 전달. 실제 게임 상태는 변경하지 않았다.
- 글꼴 배율 변경 테스트는 별도로 기록되지 않았으며, 아래 결과는 확인한 테스트 환경에 한정한다.

| 항목 | 입력·확인 기준 | 결과 |
| --- | --- | --- |
| 긴 선택지 줄바꿈 | 60자 이상 문구, 선택지 2개·3개 | 담당자 직접 확인 완료: 버튼 안에서 줄바꿈되고 문구가 잘리지 않음 |
| 하단 선택지와 스크롤 | 선택지는 하단에 두고 사건·지표만 함께 스크롤 | 담당자 직접 확인 완료: 선택지 고정, 본문 끝과 지표까지 접근 가능 |
| 긴 본문 | 긴 문단 5개, 이미지·제목·본문 배치 | 담당자 직접 확인 완료: 본문 줄바꿈과 스크롤 정상 |
| 작은 화면·지표 7개 | 320×568dp에서 지표까지 스크롤 | 담당자 직접 확인 완료: 지표 7개가 겹치지 않고 표시됨 |
| 음수 지표 | 건강 -10 | 담당자 직접 확인 완료: 숫자 0, 진행 막대 채움 없음 |
| 상한 초과 지표 | 행복 130 | 담당자 직접 확인 완료: 숫자 100, 진행 막대 가득 참 |
| 이미지 대체 표시 | 실제 이미지 없이 테스트용 imageId 전달 | 제공된 앱 화면에서 이미지 ID와 “이미지를 준비 중입니다” 문구 표시 확인 |
| 돌발 이벤트 구분 | `isSpecial = true` | 제공된 앱 화면에서 돌발 라벨과 옅은 카드 배경 표시 확인 |

아주 긴 제목, 빈/아주 긴 이미지 ID, 확대된 시스템 글꼴은 별도 확장 검증 항목이다. 코드에 대응 구조는 있지만 이번 기록에서 화면 검증 완료로 표시하지 않는다. 실제 이미지 로딩과 게임 데이터 통합도 이번 UI 테스트 범위에 포함하지 않는다.

빌드·검사 이력:

- 두 사건 UI 컴포넌트 추가 후 기존 `EventCard()` 호출 상태에서 `assembleDebug` 성공.
- 같은 컴포넌트 코드에 대해 `lintDebug` 성공. `UnusedBoxWithConstraintsScope` 오류는 Gradle lint에서 보고되지 않았다.
- 카드 임시 연결, 긴 본문·선택지 연결, 작은 화면 Preview·지표 경계값 연결을 각각 추가한 상태에서 `assembleDebug` 성공.
- 최종 임시 화면 변경 복원 후의 빌드 및 팀장 통합 빌드는 별도로 확인한다. 위 테스트 빌드 성공을 최종 통합 검증 완료로 간주하지 않는다.

제출 시 작은 화면·긴 본문·긴 선택지의 스크린샷 또는 스크롤 영상과, 건강 0·행복 100의 지표 화면을 Notion에 첨부한다. 본 문서 갱신은 테스트 완료 사실을 기록한 것이며, 증빙 파일이 첨부되었다는 뜻은 아니다.

검증용 Preview는 `GameScreen.kt`에 임시로 추가했으며 UI 컴포넌트 제출 파일에는 포함하지 않는다. `TEMP_EVENT_CARD_TEST`, `TEMP_LONG_CONTENT_TEST`, `TEMP_STAT_BOUNDARY_TEST`, `TEMP_SMALL_SCREEN_TEST` 변경은 정식 제출·PR에서 제외한다.

## 6. 제출 전 확인

- 위 목록의 8개 파일만 ZIP에 포함한다. 기존 4주차 화면 변경이나 임시 테스트 파일은 포함하지 않는다.
- 컴포넌트 설명은 이 문서를 첨부한다. 작은 화면, 긴 문장/선택지, 지표 제한, 돌발 구분의 실제 검증 결과와 스크린샷을 별도로 첨부한다.
- 현재 앱의 단계·본문·선택지 연결을 UI/UX 완료 기능으로 혼동하지 않도록 FE 연결 사항을 전달한다.
- Notion 제출과 팀장 확인 후 push한다.
