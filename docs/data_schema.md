# 인생게임 데이터 스키마 v4

이 문서는 7주차 v4 개편 기준의 콘텐츠 데이터 구조와 검증 규칙을 정의한다. 기존 Android 앱이 읽는 6주차 사건 구조는 유지하되, v4 엔진과 UI가 사용할 수 있도록 `periods`, `focuses`, `kind`, `outcomes`, 이미지 규격, 저장 버전 2 기준을 함께 둔다.

## 1. 현재 데이터 현황

| 항목 | 파일 | 7주차 기준 |
| --- | --- | ---: |
| 시기 | `app/src/main/assets/periods.sample.json` | 5개 |
| 시기별 초점 | `app/src/main/assets/focuses.sample.json` | 25개 |
| 사건 | `app/src/main/assets/events.sample.json` | 204개 |
| 결말 | `app/src/main/assets/endings.sample.json` | 31개 |
| 이미지 ID | `app/src/main/assets/image_ids.sample.json` | 336개 |
| 캐릭터 | `app/src/main/assets/characters.sample.json` | 3개 |

기존 사건은 `stage/type/choices/statDelta/resultText` 구조를 유지한다. 7주차부터는 각 사건에 v4용 `kind`를 추가하고, 각 선택지에 `outcomes` 배열을 추가한다. 기존 앱은 모르는 필드를 무시하므로 현재 화면 흐름과 호환된다.

## 2. 공통 ID 규칙

- 모든 ID는 영문 소문자, 숫자, 밑줄만 사용한다.
- `eventId`, `endingId`, `imageId`, `periodId`, `focusId`, `outcomeId`는 각 파일 또는 부모 범위 안에서 중복되면 안 된다.
- 사용자 이름 치환은 `{name}`, `{nameCall}`만 허용한다.
- 잘못된 `{namecall}`, `{NameCall}`은 검증 오류로 처리한다.
- JSON 지표명은 한국어 명칭을 기준으로 한다.

| JSON 지표명 | Kotlin 의미 | 설명 |
| --- | --- | --- |
| 건강 | health | 체력, 회복력, 질병 저항 |
| 운동능력 | fitness | 운동, 체육, 신체 수행 능력 |
| 지력 | intelligence | 학습, 문제 해결, 판단력 |
| 사회성 | social | 친구, 가족, 협업, 소통 |
| 경제력 | wealth | 돈 관리, 직업, 기회 포착 |
| 행복 | happiness | 만족도, 정서 안정, 재미 |
| 운 | luck | 우연, 돌발 상황 회피, 기회 |

## 3. 시기 데이터: periods

파일 위치: `app/src/main/assets/periods.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| periodId | string | O | `INFANT`, `CHILD`, `TEEN`, `YOUNG_ADULT`, `MIDDLE` |
| displayName | string | O | 화면 표시명. 예: 청소년기 |
| chapterTitle | string | O | 장 표지 제목. 예: 제3장 청소년기 |
| order | number | O | 시기 진행 순서 |
| years | number | O | 해당 시기에서 자동 진행할 해 수 |
| startLabel | string | 선택 | 시기 설명 |
| endLabel | string | 선택 | 다음 시기로 넘어가기 전 설명 |

현재 개발용 시기 길이는 `4 + 5 + 5 + 5 + 5 = 24턴`이다.

## 4. 시기별 초점 데이터: focuses

파일 위치: `app/src/main/assets/focuses.sample.json`

시기별 초점은 장 표지 화면에서 사용자가 고르는 장기 방향이다. 초점은 한 시기 동안 고정되며, 매년 자동으로 능력치에 영향을 준다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| focusId | string | O | 초점 고유 ID |
| period | string | O | `periods.sample.json`의 periodId |
| name | string | O | 화면에 표시할 초점명 |
| description | string | O | 초점 설명 |
| annualStatDelta | object | O | 해마다 자동 적용할 지표 변화 |
| sceneImageId | string | O | 자동 진행 화면의 3인칭 도트 장면 이미지 |
| momentImageId | string | O | 주마등/기록 썸네일 이미지 |
| yearlyLogTemplates | array | O | 자동 진행 중 출력할 기록 문장 3개 이상 |

검증 기준은 시기마다 초점 5개다. 현재 샘플은 `INFANT`, `CHILD`, `TEEN`, `YOUNG_ADULT`, `MIDDLE` 각각 5개씩 총 25개를 둔다.

## 5. 사건 데이터: events

파일 위치: `app/src/main/assets/events.sample.json`

### 5.1 기존 호환 필드

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| eventId | string | O | 사건 고유 ID |
| stage | string | O | 기존 앱용 단계. `INFANT`, `CHILD`, `TEEN`, `ADULT` |
| type | string | O | 기존 앱용 타입. `NORMAL`, `SPECIAL` |
| title | string | O | 사건 제목 |
| text | string | O | 상황 설명 |
| imageId | string | O | `image_ids.sample.json`에 존재해야 하는 이미지 ID |
| conditions | object | O | 등장 조건 |
| choices | array | O | 선택지 배열 |
| priority | number | O | 후보 정렬 우선순위 |
| isFallback | boolean | O | 대체 사건 여부 |

### 5.2 v4 추가 필드

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| kind | string | O | `MILESTONE`, `INCIDENT`, `BIG_INCIDENT` |

`kind`의 의미는 다음과 같다.

| kind | 의미 | 처리 규칙 |
| --- | --- | --- |
| MILESTONE | 시기 또는 해에 고정된 주요 사건 | 일반 돌발보다 먼저 등장 |
| INCIDENT | 일반 돌발 또는 생활 사건 | 확률 추첨으로 등장 |
| BIG_INCIDENT | 큰 돌발 사건 | 일반 돌발보다 낮은 확률로 등장, 연출 강도 높음 |

현재 기존 사건은 호환을 위해 대부분 `INCIDENT`로 분류하고, 기존 `isMilestone: true` 사건은 `MILESTONE`으로 분류한다. `BIG_INCIDENT`는 신기훈 시나리오/우영 엔진 연결 후 별도 사건으로 확장한다.

## 6. 선택지와 outcomes

기존 선택지 필드는 유지한다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| choiceId | string | O | 한 사건 안의 선택지 ID |
| label | string | O | 버튼 문구 |
| statDelta | object | O | 기존 앱용 즉시 지표 변화 |
| resultText | string | O | 기존 앱용 결과 문장 |
| addFlags | array | O | 추가 플래그 |
| outcomes | array | O | v4 확률 결과 목록 |

`outcomes` 구조는 다음과 같다.

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| outcomeId | string | O | 선택지 안의 결과 ID |
| text | string | O | 실제 결과 문장 |
| chance | number | O | 기본 가중치. 숫자 확률을 UI에 그대로 보여주면 안 됨 |
| primary | boolean | O | 대표 결과 여부. 선택지당 정확히 1개 |
| hidden | boolean | O | UI preview에 노출하지 않을 숨은 결과 여부 |
| death | boolean | O | 즉시 결말로 넘어가는 사망/치명 결과 여부 |
| statDelta | object | 선택 | v4 결과별 지표 변화 |
| addFlags | array | 선택 | v4 결과별 추가 플래그 |
| chanceWeights | object | 선택 | 능력치에 따른 가중치 보정 |

v4 핵심 규칙은 다음과 같다.

- 능력치로 선택지를 잠그지 않는다.
- 능력치는 `outcomes` 중 어떤 결과가 나올지에만 영향을 준다.
- UI는 숫자 확률을 보여주지 않고 `낮음`, `반반쯤`, `높음`, `매우 높음` 같은 질적 표현만 사용한다.
- `hidden: true` 결과는 선택지 preview나 가능성 문구에 노출하면 안 된다.
- `hidden: true`와 `primary: true`를 동시에 사용할 수 없다.
- `INFANT`, `CHILD` 단계에서는 `death: true` 결과를 금지한다.

## 7. 결말 데이터: endings

파일 위치: `app/src/main/assets/endings.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| endingId | string | O | 결말 고유 ID |
| title | string | O | 결과 화면 제목 |
| priority | number | O | 여러 결말 충족 시 높은 값 우선 |
| conditions | object | O | 결말 조건 |
| conditions.minStats | object | O | 최소 지표 조건 |
| conditions.maxStats | object | 선택 | 최대 지표 조건. 예: 건강이 너무 낮은 결말 |
| conditions.requiredFlags | array | O | 필요한 경험 플래그 |
| conditions.blockedFlags | array | 선택 | 있으면 안 되는 플래그 |
| summary | string | O | 결과 설명 |
| isDefault | boolean | O | 조건 불충족 시 사용할 기본 결말 여부 |

`maxStats`는 7주차부터 검증 스크립트에서 파싱 가능해야 한다. 값은 `minStats`와 동일하게 한국어 지표명과 정수 값을 사용한다.

## 8. 이미지 ID 데이터

파일 위치: `app/src/main/assets/image_ids.sample.json`

| 항목 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| imageId | string | O | 이미지 고유 ID |
| usage | string | O | `event`, `character`, `ending`, `scene`, `pov`, `moment`, `face_sheet`, `body_part`, `placeholder` |
| stage | string | O | 연결 시기 또는 용도 |
| description | string | O | 이미지 설명 |
| fileName | string | O | 실제 이미지 파일명. `imageId + .png`와 일치해야 함 |
| fallbackText | string | O | 파일이 없을 때 표시할 대체 문구 |
| width | number | 권장 | 실제 생성 이미지 너비. 300 |
| height | number | 권장 | 실제 생성 이미지 높이. 225 |
| style | string | 권장 | `pixel_art` 권장 |
| perspective | string | 권장 | `third_person`, `first_person`, `flashback` 등 |

이미지 생성 검증 기준은 다음과 같다.

- PNG 파일이어야 한다.
- 기준 크기는 300×225 px이다.
- 비율은 4:3이다.
- 도트풍이어야 한다.
- 텍스트, 워터마크, 로고가 들어가면 안 된다.
- `imageId`와 `fileName`은 일치해야 한다.
- 실제 파일이 아직 없어도 앱은 placeholder로 대체 가능해야 한다.

## 9. 저장 데이터 버전 2

저장 담당 파일: `app/src/main/java/com/example/lifegame/data/ProgressStore.kt`

기존 저장 형식 버전 1은 사건 단위 진행 상태를 저장한다. v4 저장 형식 버전 2는 시기·초점·자동 진행 상태를 저장해야 한다.

| 항목 | 설명 |
| --- | --- |
| saveFormatVersion | 저장 형식 버전. v4는 2 |
| runId | 회차 ID |
| runSeed | 확률 재현용 seed |
| playerName | 플레이어 이름 |
| selectedCharacterId | 선택한 캐릭터 ID |
| currentPeriod | 현재 시기 |
| yearInPeriod | 현재 시기 안에서 몇 번째 해인지 |
| selectedFocusId | 현재 시기에서 고정된 초점 |
| stats | 7개 지표 |
| flags | 경험 플래그 |
| moments | 주마등/기록 목록 |
| pendingDecisionId | 결정/돌발 대기 중이면 해당 사건 ID |
| phase | `CHAPTER`, `AUTO`, `DECISION`, `RESULT`, `ENDING` |

복구 기준은 다음과 같다.

- `saveFormatVersion`이 알 수 없는 값이면 새 회차로 초기화한다.
- `runSeed`가 없으면 기존 진행 결과를 재현할 수 없으므로 새 회차로 초기화한다.
- `pendingDecisionId`가 있는데 해당 사건이 없으면 장 표지 또는 자동 진행으로 복구한다.
- `selectedFocusId`가 현재 시기에 속하지 않으면 해당 시기 장 표지로 되돌린다.

## 10. 검증 스크립트

실행 명령:

```bash
python scripts/validate_content.py
```

검증 항목은 다음과 같다.

- JSON 파싱 가능 여부
- ID 중복 여부
- period ID 중복 및 누락 여부
- focus ID 중복 여부
- 시기마다 초점 5개인지 여부
- event kind 값 검증
- outcome 없는 선택지 검출
- primary outcome 중복 또는 누락 검출
- hidden death가 UI preview로 나가지 않게 표시 필드 검증
- 영아·유년기 death 결과 금지
- imageId가 `image_ids.sample.json`에 존재하는지 확인
- image fileName 규격 확인
- 실제 PNG 파일이 있으면 300×225 크기 확인
- `maxStats` 파싱 가능 여부
- `{name}`, `{nameCall}` 치환 누락 가능 문장 검출

## 11. 7주차 통과 기준

- `python scripts/validate_content.py` PASS
- `./gradlew.bat test` PASS
- `./gradlew.bat assembleDebug` PASS
- 새로 추가한 `periods.sample.json`, `focuses.sample.json`이 검증 대상에 포함됨
- 이미지 생성 산출물 검증 기준이 문서화됨
- v4 규칙 위반 여부를 QA 문서로 확인 가능함
