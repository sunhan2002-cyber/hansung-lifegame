# 5주차 백엔드 과제 제출 — 김우영

## 작업한 내용 요약

- `ContentRepository`가 `events.sample.json`과 `endings.sample.json`을 읽어 실제 도메인 모델로 변환하도록 연결했다.
- `GameSession`을 추가해 `JSON 사건 → GameEngine 선택 반영 → 다음 사건/성장 단계 → 결말 판정` 흐름을 한 곳에서 관리한다.
- 카드에 지정된 `createInitialProgress`, `advanceAfterResult`, `pickSpecialEvent`, `shouldMoveNextStage`, `moveNextStage`를 `GameEngine`에 구현했다.
- `LifeGameViewModel`의 임시 Map 계산과 하드코딩 사건을 제거하고 `GameSession` 및 `GameEngine` 결과를 Compose 화면에 반영했다.
- 초기에는 성장 단계 4종의 최소 샘플 사건으로 연동을 검증했고, 현재는 원격 `main`에 통합된 팀 사건 데이터 138개를 같은 로더와 진행 로직으로 사용한다.
- 선택 전후 지표를 실제 에뮬레이터에서 검증했다. 선택 A 적용 전에는 모든 지표가 50이었고, 적용 후 건강 53·운동능력 55·행복 52가 다음 사건 화면까지 유지됐다.

## 만든 파일 또는 수정한 파일명

### 새 파일

- `app/src/main/assets/events.sample.json`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameSessionTest.kt`
- `docs/evidence/week5_01_start.png`
- `docs/evidence/week5_02_event_before.png`
- `docs/evidence/week5_03_choice_result.png`
- `docs/evidence/week5_04_event_after.png`

### 수정 파일

- `app/src/main/java/com/example/lifegame/data/ContentRepository.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameApp.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameUiState.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/GameScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/ChoiceResultScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/EndingScreen.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameEngineTest.kt`

## 실행 방법 또는 확인 방법

1. Android Studio에서 프로젝트를 연다.
2. Gradle Sync 후 에뮬레이터 또는 Android 기기에서 `app`을 실행한다.
3. `새 게임 → 아기 1 → 게임 시작`을 누른다.
4. 첫 사건에서 A를 선택한다.
5. 결과 화면에서 `건강 +3`, `운동능력 +5`, `행복 +2`를 확인한다.
6. `다음 사건`을 누르고 지표가 `건강 53`, `운동능력 55`, `행복 52`로 유지되는지 확인한다.

명령행 검증:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

검증 결과:

- Android 디버그 APK 빌드 성공
- 단위 테스트 35개 실행, 실패 0개, 오류 0개, 건너뜀 0개
- 카드 지정 신규 테스트 7개 추가(요구사항 6개 이상 충족)
- 사건 JSON 138개, 성장 단계 4종
- 사건이 참조한 이미지 ID 누락 0개
- 실제 에뮬레이터에서 JSON 사건 로딩과 선택 후 지표 유지 확인

## 결과 캡처

- `week5_02_event_before.png`: 첫 사건과 선택 전 지표 50 확인
- `week5_03_choice_result.png`: 선택 결과와 변화량 +3/+5/+2 확인
- `week5_04_event_after.png`: 다음 사건에서 건강 53·운동능력 55·행복 52 유지 확인

각 PNG는 5MB 이하이므로 Notion 카드에 직접 첨부할 수 있다. 생성된 APK는 약 9.9MB이므로 요금제 첨부 제한에 맞춰 Notion에는 첨부하지 않는다.

## 변경 전후 비교

| 구분 | 변경 전 | 변경 후 |
| --- | --- | --- |
| 사건 데이터 | UI 파일의 하드코딩 샘플 | `events.sample.json`을 Repository가 로드 |
| 지표 계산 | ViewModel에서 Map을 직접 계산 | `GameEngine.applyChoice()` 결과를 사용 |
| 게임 진행 | 단일 임시 사건과 테스트 결말 | 사건·단계 진행 후 조건 기반 결말 판정 |
| 중복 선택 방지 | 화면 이동에 의존 | `GameSession`과 occurrence ID로 이중 반영 방지 |
| 검증 | 화면 뼈대 중심 | 단위 테스트 35개 + 실제 에뮬레이터 캡처 |

## GitHub push 조건

Notion 제출 후 팀장 검사를 통과하기 전에는 GitHub에 push하지 않는다. 검사 통과 후 변경 파일만 커밋·push하고 이 카드에 commit 링크를 남긴다.
