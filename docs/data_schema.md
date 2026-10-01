# 인생게임 데이터 스키마

이 문서는 5주차 main 기준 데이터 구조를 설명한다. 앱은 사건, 결말, 이미지 목록, 저장 상태를 코드와 분리해 관리한다.

## 1. 공통 규칙

- 모든 고유 ID는 영문 소문자, 숫자, 밑줄을 사용한다.
- `eventId`, `endingId`, `imageId`는 각 파일 안에서 중복되면 안 된다.
- 지표명은 JSON에서는 한국어 명칭을 기본으로 사용하고, Kotlin에서는 영어 필드로 매핑한다.
- JSON 파싱 실패 시 앱이 강제 종료되지 않도록 `ContentRepository`는 빈 목록을 반환한다.

| JSON 지표명 | Kotlin 필드 |
| --- | --- |
| 건강 | health |
| 운동능력 | fitness |
| 지력 | intelligence |
| 사회성 | social |
| 경제력 | wealth |
| 행복 | happiness |
| 운 | luck |

## 2. 사건 데이터

파일 위치: `app/src/main/assets/events.sample.json`

현재 기준:

- 사건 수: 138개
- `NORMAL`: 128개
- `SPECIAL`: 10개
- 성장 단계: `INFANT`, `CHILD`, `TEEN`, `ADULT`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| eventId | string | O | 사건 고유 ID |
| stage | string | O | `INFANT`, `CHILD`, `TEEN`, `ADULT` |
| type | string | O | `NORMAL` 또는 `SPECIAL` |
| title | string | O | 사건 제목 |
| text | string | O | 사용자에게 보여줄 상황 설명 |
| imageId | string | O | `image_ids.sample.json`에 존재해야 하는 이미지 ID |
| conditions | object | O | 등장 조건 |
| conditions.stats | object | O | 최소 지표 조건. 없으면 `{}` |
| conditions.requiredFlags | array | O | 필요한 경험 플래그 |
| conditions.blockedFlags | array | O | 있으면 안 되는 경험 플래그 |
| choices | array | O | 선택지 배열. 최소 2개 이상 |
| priority | number | O | 후보가 여러 개일 때 높은 값 우선 |
| isFallback | boolean | O | 조건부 사건이 없을 때 사용할 공통 사건 여부 |

선택지 구조:

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| choiceId | string | O | 한 사건 안의 선택지 ID. 예: `A`, `B`, `C` |
| label | string | O | 버튼에 표시할 행동 문장 |
| statDelta | object | O | 선택 후 지표 변화량 |
| resultText | string | O | 선택 결과 문장 |
| addFlags | array | O | 이후 사건·결말 조건에 사용할 경험 플래그 |

예시:

```json
{
  "eventId": "child_001",
  "stage": "CHILD",
  "type": "NORMAL",
  "title": "방과 후 선택",
  "text": "학교가 끝났다. 오늘 남은 시간을 어떻게 보낼까?",
  "imageId": "img_child_school_001",
  "conditions": {
    "stats": {},
    "requiredFlags": [],
    "blockedFlags": []
  },
  "choices": [
    {
      "choiceId": "A",
      "label": "도서관에서 책을 읽는다.",
      "statDelta": { "지력": 6, "행복": 1 },
      "resultText": "새로운 지식을 얻고 독서에 관심이 생긴다.",
      "addFlags": ["reading_interest"]
    }
  ],
  "priority": 10,
  "isFallback": false
}
```

## 3. 결말 데이터

파일 위치: `app/src/main/assets/endings.sample.json`

현재 기준:

- 결말 수: 26개
- 기본 결말: 1개 이상 유지
- 여러 결말 조건을 동시에 만족하면 `priority`가 높은 결말을 우선 선택한다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| endingId | string | O | 결말 고유 ID |
| title | string | O | 결과 화면 제목 |
| priority | number | O | 여러 결말 조건 충족 시 높은 값 우선 |
| conditions.minStats | object | O | 결말 도달에 필요한 최소 지표 |
| conditions.requiredFlags | array | O | 필요한 경험 플래그 |
| conditions.blockedFlags | array | 선택 | 있으면 안 되는 경험 플래그 |
| summary | string | O | 결과 설명 |
| isDefault | boolean | O | 조건 불충족 시 사용할 기본 결말 여부 |

## 4. 이미지 ID 데이터

파일 위치: `app/src/main/assets/image_ids.sample.json`

현재 기준:

- 이미지 ID 수: 217개
- 실제 이미지 파일이 없어도 `fallbackText`로 화면 표시가 가능해야 한다.
- `events.sample.json`의 모든 `imageId`는 이 파일에 존재해야 한다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| imageId | string | O | 이미지 고유 ID |
| usage | string | O | `event`, `character`, `ending` 중 하나 |
| stage | string | O | 이미지가 쓰이는 성장 단계 또는 용도 |
| description | string | O | 이미지 준비 전 설명 |
| fileName | string | O | 실제 이미지 파일명 |
| fallbackText | string | O | 파일이 없을 때 화면에 표시할 대체 문장 |

Kotlin 모델:

```kotlin
data class ImageAsset(
    val imageId: String,
    val usage: String,
    val stage: String,
    val description: String,
    val fileName: String,
    val fallbackText: String,
)
```

Repository 메서드:

- `loadEvents()`
- `loadEndings()`
- `loadImageAssets()`
- `findImageAsset(imageId: String)`

## 5. 저장 상태

저장 담당 파일: `app/src/main/java/com/example/lifegame/data/ProgressStore.kt`

저장 대상 모델: `GameProgress`

| 항목 | 설명 |
| --- | --- |
| runId | 회차 ID |
| contentVersion | 콘텐츠 버전 |
| saveFormatVersion | 저장 형식 버전 |
| selectedImageRef | 선택한 캐릭터 이미지 참조 |
| currentEventOccurrenceId | 현재 사건 발생 ID |
| screenState | 현재 화면 상태: `EVENT`, `CHOICE_RESULT`, `ENDING` |
| stage | 현재 성장 단계 |
| stats | 7개 지표 값 |
| flags | 획득 경험 플래그 |
| completedEventIds | 이미 완료한 사건 ID |
| handledOccurrenceIds | 이미 처리한 사건 발생 ID. 중복 선택 방지용 |
| choiceHistory | 선택 기록 목록 |
| specialEventCount | 이번 회차 돌발 이벤트 등장 횟수 |

저장 복구 기준:

- 필수값인 `runId` 또는 `currentEventOccurrenceId`가 없으면 저장 데이터가 없는 것으로 처리한다.
- enum 값이 깨져 있으면 기본값으로 복구한다.
- 선택 이력 JSON이 깨져 있으면 빈 목록으로 복구한다.

## 6. 5주차 데이터 검증 기준

- `events.sample.json`은 JSON 파싱이 가능해야 한다.
- 사건은 120개 이상이어야 한다.
- 모든 사건은 선택지 2개 이상을 가져야 한다.
- `eventId`, `endingId`, `imageId`는 중복되면 안 된다.
- 모든 사건의 `imageId`는 `image_ids.sample.json`에 존재해야 한다.
- 결말은 25개 이상이어야 한다.
- 기본 결말은 최소 1개 이상이어야 한다.
- `./gradlew test`와 `./gradlew assembleDebug`가 성공해야 한다.
