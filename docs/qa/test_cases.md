# 4주차 QA 테스트 케이스

테스트는 팀원이 Notion에 제출한 파일을 팀장이 검사할 때 사용하는 기준이다. GitHub push 전에는 Notion 제출물 기준으로 확인하고, Android 프로젝트가 합쳐진 뒤에는 `./gradlew test`, `./gradlew assembleDebug`로 다시 확인한다.

## 일반 사례

| 테스트 ID | 구분 | 확인 대상 파일 | 조건·입력 | 기대 결과 |
| --- | --- | --- | --- | --- |
| TC-W4-001 | 일반 | `app/src/main/assets/endings.sample.json` | JSON 파싱 | 결말이 20개 이상이다. |
| TC-W4-002 | 일반 | `app/src/main/assets/image_ids.sample.json` | JSON 파싱 | 이미지 ID가 80개 이상이다. |
| TC-W4-003 | 일반 | `GameEngine.kt` | 지력 50에서 +6 선택 | 지력이 56이 된다. |
| TC-W4-004 | 일반 | `GameEngine.kt` | 사회성 60 조건 사건 | 사회성 조건 사건이 선택된다. |
| TC-W4-005 | 일반 | `ContentRepository.kt` | 정상 결말 JSON 로딩 | `loadEndings()`가 빈 목록이 아닌 결말 목록을 반환한다. |
| TC-W4-006 | 일반 | `ProgressStore.kt` | 진행 상태 저장 후 불러오기 | `runId`, 현재 사건 ID, 지표가 유지된다. |
| TC-W4-007 | 일반 | `events.sample.json` | 사건 80개 제출 | 단계별 사건 수가 편중되지 않는다. |
| TC-W4-008 | 일반 | `image_ids.sample.json` | `imageId`와 `fileName` 확인 | 같은 ID와 같은 파일명이 중복되지 않는다. |
| TC-W4-009 | 일반 | `endings.sample.json` | 기본 결말 확인 | `isDefault: true` 결말이 1개 이상 있다. |
| TC-W4-010 | 일반 | Android 화면 | 사건 이미지와 설명 표시 | 같은 사건의 `imageId`와 상황 설명이 함께 보인다. |

## 위험 사례

| 테스트 ID | 구분 | 확인 대상 파일 | 조건·입력 | 기대 결과 |
| --- | --- | --- | --- | --- |
| TC-W4-011 | 위험 | `GameEngine.kt` | 건강 98에서 +5 | 건강이 100을 넘지 않는다. |
| TC-W4-012 | 위험 | `GameEngine.kt` | 같은 사건 발생 ID로 두 번 선택 | 지표와 선택 이력이 한 번만 반영된다. |
| TC-W4-013 | 위험 | `ContentRepository.kt` | 깨진 JSON 파일 | 앱이 강제 종료되지 않고 빈 목록 또는 오류 상태로 처리한다. |
| TC-W4-014 | 위험 | `ProgressStore.kt` | 저장된 현재 사건 ID가 실제 사건 목록에 없음 | 새 게임 안내 또는 복구 불가 안내로 처리한다. |
| TC-W4-015 | 위험 | `image_ids.sample.json` | 실제 이미지 파일이 없음 | `fallbackText`나 설명으로 대체 표시한다. |
| TC-W4-016 | 위험 | `endings.sample.json` | 기본 결말이 없음 | 결말 검증에서 실패 처리한다. |
| TC-W4-017 | 위험 | `events.sample.json` | A/B 선택지 중 하나가 없음 | 사건 데이터 검증에서 실패 처리한다. |
| TC-W4-018 | 위험 | `events.sample.json` | 선택 변화량이 -1000 또는 +1000 | 게임 로직에서 0~100 범위로 제한한다. |
| TC-W4-019 | 위험 | Android 앱 | 선택 버튼 연속 탭 | 동일 선택이 중복 반영되지 않는다. |
| TC-W4-020 | 위험 | 저장 데이터 | 앱 종료 직후 재실행 | 마지막 정상 상태를 복원하거나 새 게임 안내를 표시한다. |

## 애매한 사례

| 테스트 ID | 구분 | 확인 대상 파일 | 조건·입력 | 기대 결과 |
| --- | --- | --- | --- | --- |
| TC-W4-021 | 애매 | `GameEngine.kt` | 여러 결말 조건을 동시에 만족 | priority가 높은 결말을 선택한다. |
| TC-W4-022 | 애매 | `GameEngine.kt` | priority가 같은 결말 2개 | `endingId` 오름차순으로 하나만 선택한다. |
| TC-W4-023 | 애매 | `GameEngine.kt` | 조건부 사건 후보 없음 | 공통 사건 또는 단계 종료 기준을 따른다. |
| TC-W4-024 | 애매 | `events.sample.json` | 지표는 같고 경험 플래그만 다름 | 서로 다른 조건 사건이 등장할 수 있다. |
| TC-W4-025 | 애매 | `endings.sample.json` | 경제력만 매우 높고 행복이 낮음 | 성공 결말이 아니라 외로운 부자 등 조건에 맞는 결말이 나온다. |
| TC-W4-026 | 애매 | `image_ids.sample.json` | 같은 설명의 다른 이미지 ID | ID가 다르고 사용처가 명확하면 허용한다. |
| TC-W4-027 | 애매 | `ContentRepository.kt` | 알 수 없는 stage 문자열 | 기본 단계 처리 또는 검증 실패로 기록한다. |
| TC-W4-028 | 애매 | `ProgressStore.kt` | 저장된 플래그는 있지만 완료 사건 목록이 비어 있음 | 진행 복원 후 사건 중복 여부를 추가 확인한다. |
| TC-W4-029 | 애매 | Android 화면 | 긴 사건 문장 | 스크롤로 읽을 수 있고 선택 버튼이 가려지지 않는다. |
| TC-W4-030 | 애매 | 통합 시연 | 3개 결말 경로 시도 | 최소 3가지 결말에 도달할 수 있다. |
