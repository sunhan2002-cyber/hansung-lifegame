# 7주차 백엔드 제출 보고서 — 김우영

## 작업 기준

- 기준 브랜치: `origin/main`
- 최초 기준 커밋: `b8abc5bd8d6912d382c20566f5946ac905d1960a`
- 최신화한 `main` 커밋: `86cb6b728c90bce5de9a2fe24325ba1e8f8cb62e`
- 작업 브랜치: `feat/week7-backend-wooyoung`
- 목표: 저장 후 앱을 다시 실행해도 사건, 선택 결과, 결말 화면과 진행 상태를 복원한다.

## 구현 내용

### ProgressStore 저장 형식 보강

- 캐릭터 ID, 플레이어 이름과 `GameProgress`를 하나의 버전 있는 JSON 스냅샷으로 저장한다.
- 지표 7개, 플래그, 완료 사건, 처리한 occurrence ID, 선택 이력, 돌발 사건 횟수와 단계 진행 횟수를 빠짐없이 저장한다.
- 기존 코드에서 누락됐던 `stageEventCount`를 저장·복원한다.
- 손상된 JSON, 알 수 없는 enum, 지원하지 않는 저장 버전은 예외를 앱 밖으로 전파하지 않고 복원 실패로 처리한다.
- 플레이어 이름이 없던 기존 스냅샷은 `플레이어`를 기본값으로 읽어 하위 호환한다.

### GameSession 복원

- 사건 화면은 occurrence ID로 현재 사건을 다시 연결한다.
- 선택 결과 화면은 마지막 선택 이력으로 사건, 선택지, 변화량과 결과 문장을 재구성한다.
- 결말 화면은 저장된 진행 상태로 결말을 다시 판정한다.
- 현재 콘텐츠에서 사건이나 선택지를 찾지 못하면 잘못된 저장으로 판단한다.

### ViewModel과 화면 흐름 연결

- 새 게임 시작, 선택 적용, 다음 사건 이동, 결말 이동 시 자동 저장한다.
- 시작 화면에서 저장 데이터 유무에 따라 이어하기 버튼을 활성화한다.
- 이어하기 시 `EVENT`, `CHOICE_RESULT`, `ENDING`에 맞는 화면으로 이동한다.
- 최신 UI에서 선택 결과는 별도 화면이 아니라 게임 화면 안에 표시되므로 `CHOICE_RESULT`도 게임 화면으로 복원한다.
- 이어하기 때 저장된 이름을 사건·선택지·결말에 다시 치환하고 기록 및 이미지 매핑도 함께 복원한다.
- 손상된 저장은 삭제하고 새 게임 안내 상태로 복구한다.
- 다시 시작하면 저장 데이터를 삭제한다.

## 변경 파일

- `app/src/main/java/com/example/lifegame/data/ProgressStore.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameApp.kt`
- `app/src/test/java/com/example/lifegame/data/ProgressSnapshotCodecTest.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameSessionTest.kt`
- `app/build.gradle.kts`
- `.gitignore`
- `docs/week7_backend_submission.md`

## 추가 테스트

1. 전체 진행 상태 JSON 왕복 시 모든 필드 보존
2. 손상 또는 지원하지 않는 저장 버전 안전 처리
3. 저장된 사건 화면 복원
4. 저장된 선택 결과와 지표 변화량 복원
5. 저장된 결말 화면 복원
6. 플레이어 이름 JSON 왕복 보존
7. 이름 필드가 없는 기존 스냅샷의 기본 이름 복원

## 검증 결과

- `gradlew.bat test`: BUILD SUCCESSFUL
- Debug 단위 테스트: 57개, 실패 0
- Release 단위 테스트: 57개, 실패 0
- `gradlew.bat assembleDebug`: BUILD SUCCESSFUL
- `python scripts/validate_content.py`: PASS
- 콘텐츠 검증: 사건 204개, 돌발 사건 19개, 결말 31개, 이미지 286개로 PASS

## 확인 필요한 부분

- 실제 Android 기기에서 앱 강제 종료 후 사건/결과/결말 각 화면의 복원 동선을 팀장이 최종 확인해야 한다.
- 저장 포맷 버전은 현재 1만 지원하며, 이후 모델 변경 시 마이그레이션 규칙을 추가해야 한다.

---

## v4 자동 진행 엔진 추가 제출

### 작업 범위

- 시기/해/초점 모델과 24턴 개발용 시기 정의를 추가했다.
- 시기 시작 시 초점 선택을 요구하고, 선택한 초점은 해당 시기가 끝날 때까지 변경하지 못하게 했다.
- 해마다 초점 효과와 기록을 적용하고, 결정이 없는 해는 자동 진행 가능한 `TurnStep`을 반환한다.
- 예약 마일스톤을 큰 돌발·일반 돌발보다 먼저 판정한다.
- 큰 돌발과 일반 돌발의 고정 seed 추첨 및 가중치 선택 뼈대를 추가했다.
- 선택 결과를 `outcomes` 가중치로 결정하며 능력치는 선택지를 잠그지 않고 결과 가중치만 바꾼다.
- 같은 `runSeed + eventId + choiceId`는 동일한 결과를 반환한다.
- hidden 결과는 `PendingDecision` preview에 포함하지 않고, 사망 결과는 즉시 `ENDING` 단계로 전환한다.
- `sceneImageId`, `povImageId`, `momentImageId`, `TransitionCue`를 엔진 출력에 연결했다.
- 기존 사건 나열형 API는 삭제하지 않고 v4 `PeriodGameSession`을 병행 추가했다.

### v4 변경 파일

- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/V4GameEngineTest.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/V4PeriodGameSessionTest.kt`
- `docs/week7_backend_submission.md`

### v4 신규 테스트 13개

1. 초점이 없으면 장 표지 단계 반환
2. 시기 중간 초점 변경 거부
3. 해 진행 시 초점 효과와 이미지 ID 반영
4. 마일스톤 우선 처리
5. 결정 pending 시 자동 진행 정지
6. 같은 seed와 선택의 결과 재현
7. 능력치에 따른 결과 가중치 변화
8. hidden death preview 비노출
9. 사망 결과 즉시 결말 전환
10. 고정 seed 24턴 완주
11. 큰 돌발의 POV 이미지와 왜곡 전환
12. 세션의 장 표지 → 결정 → 다음 해 연결
13. pending 결정 복원 시 자동 진행 정지 유지

### 최신 검증 결과

- `gradlew.bat test`: BUILD SUCCESSFUL
- Debug 70개 + Release 70개 = 140개 실행, 실패/오류/건너뜀 0
- `gradlew.bat assembleDebug`: BUILD SUCCESSFUL
- `python scripts/validate_content.py`: PASS
- 콘텐츠: 사건 204개, 돌발 19개, 결말 31개, 이미지 ID 286개

### 후속 담당자 참고

- 프론트는 `PeriodGameSession.start/selectFocus/advance/choose`와 `TurnStep`을 연결하면 된다.
- 시나리오·DB 담당자가 `periods.json`, `focuses.json`, v4 사건 로더를 추가하면 현재 임시 코드 정의를 같은 모델 목록으로 교체할 수 있다.
- 7주차 범위에서는 기존 JSON 204개를 v4로 변환하지 않았고, 서버나 실시간 이미지 생성도 연결하지 않았다.

---

## 협업 후속 보완 — PR #26 v4 콘텐츠 로더 연결

### 반영 내용

- `ContentRepository`가 `periods.sample.json`, `focuses.sample.json`, `events.sample.json`을 직접 읽는 v4 로더를 제공한다.
- `V4ContentParser`를 Android `Context`와 분리해 JVM 단위 테스트에서 JSON 스키마를 검증할 수 있게 했다.
- 시기 표시명·장 제목·시작/종료 문구, 초점의 연간 지표 변화·기록 문장·이미지 ID를 모델에 보존한다.
- 사건의 `kind`, 조건, 선택지별 `outcomes`, `chanceWeights`, hidden/death, 플래그를 엔진 모델로 변환한다.
- 기존 사건 데이터에 `outcomes`가 없을 때는 기존 `resultText/statDelta/addFlags`로 100 가중치 결과를 만들어 하위 호환한다.
- 기존 `ADULT` 사건은 v4의 `YOUNG_ADULT`, `MIDDLE` 후보로 연결하고, 추후 `periods` 배열이 들어오면 명시 시기를 우선한다.
- `BIG_INCIDENT`는 실제 데이터가 추가되는 즉시 낮은 확률 추첨, 자동 진행 정지, `DISTORT` 전환, POV 및 moment 이미지 전달 경로를 그대로 사용한다.
- 연도가 지정되지 않은 기존 `MILESTONE`도 누락되지 않고 일반·큰 돌발보다 먼저 처리한다.
- 초점의 `yearlyLogTemplates`는 seed와 연도에 따라 재현 가능하게 선택된다.

### 추가 변경 파일

- `app/src/main/java/com/example/lifegame/data/ContentRepository.kt`
- `app/src/main/java/com/example/lifegame/data/V4ContentParser.kt`
- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/test/java/com/example/lifegame/data/V4ContentParserTest.kt`

### 추가 검증

- 시기 JSON 정렬 및 표시 메타데이터 파싱
- 초점 지표·기록 문장·scene/moment 이미지 ID 파싱
- outcomes 확률·가중치·hidden/death·조건 파싱
- 기존 선택지의 100 가중치 outcome 하위 호환
- 연도 없는 milestone 우선 처리
- BIG_INCIDENT 자동 진행 정지와 POV/moment 이미지 전달
- `gradlew.bat test`: Debug 76개 + Release 76개 = 152개, 실패 0
- `gradlew.bat assembleDebug`: BUILD SUCCESSFUL

### 남은 협업 의존성

- PR #26이 main에 merge되어 실제 `periods.sample.json`, `focuses.sample.json`, v4 `events.outcomes`가 앱 자산에 포함되어야 한다.
- 현재 PR #26 데이터에는 `BIG_INCIDENT`가 0개이므로 신기훈의 실제 사건 데이터가 추가된 뒤 콘텐츠 기반 시뮬레이션을 다시 실행해야 한다.
- 실제 Compose 화면에서 scene/pov/moment 이미지를 그리는 작업은 UI/UX·프론트 화면 연결 범위이며, 백엔드는 세 ID를 `TurnStep`과 `PendingDecision`으로 제공한다.
