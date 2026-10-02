# 인생게임 데이터 스키마

이 문서는 6주차 DB·QA 기준으로 앱 콘텐츠 데이터 구조를 설명한다. 앱은 사건, 결말, 이미지 목록, 캐릭터, 저장 상태를 코드와 분리해 관리한다.

## 1. 현재 데이터 현황

| 항목 | 파일 | 6주차 현재 기준 |
| --- | --- | --- |
| 사건 | `app/src/main/assets/events.sample.json` | 150개 |
| 일반 사건 | `events.sample.json` | 140개 |
| 돌발 사건 | `events.sample.json` | 10개 |
| 결말 | `app/src/main/assets/endings.sample.json` | 31개 |
| 이미지 ID | `app/src/main/assets/image_ids.sample.json` | 232개 |
| 캐릭터 | `app/src/main/assets/characters.sample.json` | 3개 |

성장 단계별 사건 수는 다음과 같다.

| 단계 | 사건 수 |
| --- | ---: |
| INFANT | 24 |
| CHILD | 43 |
| TEEN | 33 |
| ADULT | 50 |

현재 `events.sample.json`에서 사용하는 모든 `imageId`는 `image_ids.sample.json`에 존재한다. 사용되지 않는 이미지 ID는 이후 사건 확장과 실제 이미지 연결을 위한 예비 항목으로 유지한다.

## 2. 공통 규칙

- 모든 고유 ID는 영문 소문자, 숫자, 밑줄을 사용한다.
- `eventId`, `endingId`, `imageId`는 각 파일 안에서 중복되면 안 된다.
- 사건의 `imageId`는 반드시 `image_ids.sample.json`에 존재해야 한다.
- 선택지는 최소 2개 이상이어야 한다.
- JSON 지표명은 한국어 명칭을 기본으로 사용하고, Kotlin에서는 영어 필드로 매핑한다.
- `{name}`, `{nameCall}`은 플레이어 이름 치환용 플레이스홀더로만 사용한다.
- JSON 파싱 실패 시 앱이 강제 종료되지 않도록 `ContentRepository`는 빈 목록을 반환한다.

| JSON 지표명 | Kotlin 필드 | 의미 |
| --- | --- | --- |
| 건강 | health | 체력, 회복력, 질병 저항 |
| 운동능력 | fitness | 운동, 체육, 신체 수행 능력 |
| 지력 | intelligence | 학습, 문제 해결, 판단력 |
| 사회성 | social | 친구, 가족, 협업, 소통 |
| 경제력 | wealth | 돈 관리, 직업, 기회 포착 |
| 행복 | happiness | 만족도, 정서 안정, 재미 |
| 운 | luck | 우연, 돌발 상황 회피, 기회 |

## 3. 사건 데이터

파일 위치: `app/src/main/assets/events.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| eventId | string | O | 사건 고유 ID |
| stage | string | O | `INFANT`, `CHILD`, `TEEN`, `ADULT` |
| type | string | O | `NORMAL` 또는 `SPECIAL` |
| title | string | O | 사건 제목 |
| text | string | O | 사용자에게 보여줄 상황 설명 |
| imageId | string | O | `image_ids.sample.json`에 존재해야 하는 이미지 ID |
| conditions | object | O | 등장 조건 |
| conditions.stats | object | O | 지표 조건. 없으면 `{}` |
| conditions.requiredFlags | array | O | 필요한 경험 플래그 |
| conditions.blockedFlags | array | O | 있으면 안 되는 경험 플래그 |
| choices | array | O | 선택지 배열. 최소 2개 이상 |
| priority | number | O | 후보가 여러 개일 때 높은 값 우선 |
| isFallback | boolean | O | 조건부 사건이 없을 때 사용할 공통 사건 여부 |

선택지 구조는 다음과 같다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| choiceId | string | O | 한 사건 안의 선택지 ID. 예: `A`, `B`, `C` |
| label | string | O | 버튼에 표시할 행동 문장 |
| statDelta | object | O | 선택 후 지표 변화량 |
| resultText | string | O | 선택 결과 문장 |
| addFlags | array | O | 이후 사건·결말 조건에 사용할 경험 플래그 |

## 4. 결말 데이터

파일 위치: `app/src/main/assets/endings.sample.json`

6주차 기준 결말은 31개이며, 기본 결말은 최소 1개 이상 유지한다. 여러 결말 조건을 동시에 만족하면 `priority`가 높은 결말을 우선 선택한다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| endingId | string | O | 결말 고유 ID |
| title | string | O | 결과 화면 제목 |
| priority | number | O | 여러 결말 조건 충족 시 높은 값 우선 |
| conditions | object | O | 결말 조건 |
| conditions.minStats | object | O | 결말 도달에 필요한 최소 지표 |
| conditions.requiredFlags | array | O | 필요한 경험 플래그 |
| conditions.blockedFlags | array | 선택 | 있으면 안 되는 경험 플래그 |
| summary | string | O | 결과 설명 |
| isDefault | boolean | O | 조건 불충족 시 사용할 기본 결말 여부 |

6주차에 추가한 결말은 다음과 같다.

| endingId | 제목 | 주요 조건 |
| --- | --- | --- |
| `ending_late_blooming_developer` | 늦게 피었지만 오래 가는 개발자가 되었다 | 지력, 경제력, 행복, `study_hard` |
| `ending_everyday_athlete` | 매일 조금씩 강해지는 생활 체육인이 되었다 | 건강, 운동능력, 행복, `gym_habit` |
| `ending_people_connector` | 사람들을 자연스럽게 이어 주는 사람이 되었다 | 사회성, 행복, 운, `best_friend` |
| `ending_small_stage_big_memory` | 작은 무대를 큰 추억으로 만드는 사람이 되었다 | 사회성, 행복, 운, `wedding_mc` |
| `ending_tiny_startup_survivor` | 작게 시작해 끝까지 버티는 창업자가 되었다 | 지력, 경제력, 운, `startup_founder` |

## 5. 이미지 ID 데이터

파일 위치: `app/src/main/assets/image_ids.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| imageId | string | O | 이미지 고유 ID |
| usage | string | O | `event`, `character`, `ending` 중 하나 |
| stage | string | O | 이미지가 쓰이는 성장 단계 또는 용도 |
| description | string | O | 이미지 준비 전 설명 |
| fileName | string | O | 실제 이미지 파일명 |
| fallbackText | string | O | 파일이 없을 때 화면에 표시할 대체 문장 |

현재 앱은 실제 이미지 파일이 없어도 `EventImageBox`에서 대체 박스와 이미지 ID를 표시한다. 신기훈 시나리오 PR에서 새 imageId가 추가로 필요하면 DB·QA 담당이 `image_ids.sample.json`에 설명과 fallbackText를 추가한다.

## 6. 캐릭터 데이터

파일 위치: `app/src/main/assets/characters.sample.json`

캐릭터 데이터는 시작 화면에서 선택 가능한 기본 아기 캐릭터 후보를 관리한다. 현재 UI는 3개의 기본 캐릭터를 사용한다.

## 7. 저장 상태

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
| stageEventCount | 현재 성장 단계에서 진행한 사건 수 |

저장 복구 기준은 다음과 같다.

- 필수값인 `runId` 또는 `currentEventOccurrenceId`가 없으면 저장 데이터가 없는 것으로 처리한다.
- enum 값이 깨져 있으면 기본값으로 복구한다.
- 선택 이력 JSON이 깨져 있으면 빈 목록으로 복구한다.

## 8. Repository 메서드

파일 위치: `app/src/main/java/com/example/lifegame/data/ContentRepository.kt`

| 메서드 | 역할 |
| --- | --- |
| `loadEvents()` | 사건 JSON 로딩 |
| `loadEndings()` | 결말 JSON 로딩 |
| `loadImageAssets()` | 이미지 ID JSON 로딩 |
| `findImageAsset(imageId: String)` | 특정 이미지 ID 조회 |

## 9. 6주차 데이터 검증 기준

6주차부터는 `scripts/validate_content.py`로 콘텐츠 JSON을 검사한다.

검증 항목은 다음과 같다.

- JSON 파싱 가능 여부
- `eventId`, `endingId`, `imageId` 중복 여부
- 사건 필수 필드 누락 여부
- 선택지 필수 필드 누락 여부
- 모든 사건 선택지 2개 이상 여부
- 사건 imageId가 이미지 목록에 존재하는지 여부
- 결말 30개 이상 여부
- 기본 결말 1개 이상 여부
- 지표 키 오타 여부

실행 명령:

```bash
python scripts/validate_content.py
```

6주차 통합 완료 기준은 다음과 같다.

- 콘텐츠 검증 스크립트 PASS
- `./gradlew test` PASS
- `./gradlew assembleDebug` PASS
- Android Studio에서 새 게임 후 최소 10개 사건 진행 중 강제 종료 없음
