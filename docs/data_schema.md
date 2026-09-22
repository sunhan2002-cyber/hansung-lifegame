# 인생게임 데이터 스키마

이 문서는 4주차 DB·콘텐츠 데이터 담당 산출물 기준으로 작성한다. 실제 앱에서는 사건, 선택지, 결말, 이미지, 저장 상태를 화면 코드와 분리해 관리한다.

## 공통 규칙

- 모든 ID는 영문 소문자, 숫자, 밑줄만 사용한다.
- `eventId`, `choiceId`, `endingId`, `imageId`는 각 데이터 안에서 중복되면 안 된다.
- 지표명은 문서에서는 한국어를 사용하고, Kotlin 모델에서는 영어 필드로 매핑한다.

| 문서 지표명 | Kotlin 필드 |
| --- | --- |
| 건강 | health |
| 운동능력 | fitness |
| 지력 | intelligence |
| 사회성 | social |
| 경제력 | wealth |
| 행복 | happiness |
| 운 | luck |

## 사건 JSON

파일 위치 예정: `app/src/main/assets/events.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| eventId | string | O | 사건 고유 ID |
| stage | string | O | `INFANT`, `CHILD`, `TEEN`, `ADULT` 또는 한국어 단계명 |
| type | string | O | `NORMAL` 또는 `SPECIAL` |
| title | string | O | 사건 제목 |
| text | string | O | 사용자에게 보여줄 상황 설명 |
| imageId | string | O | `image_ids.sample.json`의 이미지 ID |
| conditions | object | O | 등장 조건 |
| choices | array | O | A/B 선택지 배열 |
| priority | number | O | 후보가 여러 개일 때 높은 값 우선 |
| isFallback | boolean | O | 조건부 사건이 없을 때 쓰는 공통 사건 여부 |

## 선택지 JSON

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| choiceId | string | O | 한 사건 안의 선택지 ID. 예: `A`, `B` |
| label | string | O | 버튼에 표시할 행동 문장 |
| statDelta | object | O | 선택 후 지표 변화량 |
| resultText | string | O | 선택 결과 문장 |
| addFlags | array | O | 이후 사건·결말 조건에 사용할 경험 플래그 |

## 결말 JSON

파일 위치: `app/src/main/assets/endings.sample.json`

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

## 이미지 ID JSON

파일 위치: `app/src/main/assets/image_ids.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| imageId | string | O | 이미지 고유 ID |
| usage | string | O | `event`, `character`, `ending` 중 하나 |
| stage | string | O | 이미지가 쓰이는 성장 단계 |
| description | string | O | 이미지 준비 전 대체 설명 |
| fileName | string | O | 실제 이미지 파일명 |
| fallbackText | string | O | 파일이 없을 때 화면에 표시할 대체 문장 |

## 저장 상태

저장 담당 파일: `app/src/main/java/com/example/lifegame/data/ProgressStore.kt`

| 항목 | 설명 |
| --- | --- |
| runId | 회차 ID |
| currentEventOccurrenceId | 현재 사건 발생 ID |
| stage | 현재 성장 단계 |
| stats | 7개 지표 값 |
| flags | 획득 경험 플래그 |
| completedEventIds | 이미 완료한 사건 ID |
| specialEventCount | 이번 회차 돌발 이벤트 등장 횟수 |

## 검증 기준

- 결말은 20개 이상이어야 한다.
- 이미지 ID는 80개 이상이어야 한다.
- 기본 결말은 최소 1개 있어야 한다.
- 조건에 쓰는 지표명은 7개 지표 중 하나여야 한다.
- 이미지 파일이 아직 없어도 `fallbackText`로 화면 대체가 가능해야 한다.
