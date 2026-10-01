# 5주차 백엔드 코드 검증 재제출 — 김우영

## 재제출 사유

기존 Notion 제출에는 작업 설명과 테스트 결과만 있고 실제 수정 소스 ZIP 또는 PR/브랜치가 없어 팀장이 코드를 검증할 수 없었다. 팀장 검사 전 GitHub push 금지 조건을 지키면서 코드 검증이 가능하도록 실제 수정 파일 전체를 ZIP으로 묶어 재제출한다.

## 포함한 두 과제

1. 진행 로직 보강
   - JSON 사건·결말 로딩
   - 선택 반영과 중복 반영 방지
   - 다음 사건 선택, 성장 단계 이동, 돌발 사건, 결말 화면 연결
2. 결말 우선순위 규칙
   - 조건을 통과한 결말을 `priority` 내림차순으로 정렬
   - 동점은 `endingId` 오름차순으로 결정
   - 일반 후보가 없을 때 기본 결말 하나 선택
   - 동일 조건에서 항상 하나의 결말만 선택

## ZIP 파일

- 파일명: `week5_backend_kimwooyoung_code.zip`
- 크기: 약 27KB(5MB 이하)
- 빌드 산출물과 APK는 제외
- 소스 경로를 유지해 압축 해제 후 프로젝트에 그대로 비교·반영 가능

## ZIP 포함 파일

### 팀장 지정 최소 파일

- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameEngineTest.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameSessionTest.kt`
- `app/src/main/java/com/example/lifegame/data/ContentRepository.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt`

### 화면 연결 및 도메인 의존 파일

- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameApp.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameUiState.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/GameScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/ChoiceResultScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/EndingScreen.kt`

### 검증 문서

- `docs/week5_backend_submission.md`
- `docs/week5_backend_resubmission.md`
- `docs/week5_ending_priority_examples.md`
- `docs/evidence/week5_ending_priority_test.txt`

## 실행 방법

프로젝트 루트에서 다음 명령을 실행한다.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

## 검증 결과

- 전체 단위 테스트 35개
- 실패 0개, 오류 0개, 건너뜀 0개
- Android 디버그 APK 빌드 성공
- 동일 조건 결말 25개를 30회 판정해도 `ending_25` 하나만 선택
- 원격 `main` 기준 커밋: `6516936`
- 원격 `main`과 로컬 HEAD 차이: 0 커밋

## 데이터 현황 및 담당 경계

- 사건 138개: 전체 목표 충족
- 이미지 ID 217개: 전체 목표 충족
- 결말 21개: 전체 25개 목표까지 4개 부족하며 DB·콘텐츠 담당 잔여 작업

김우영 담당인 진행 로직과 결말 우선순위 판정은 코드와 테스트로 완료했다. 팀장 검사 통과 전에는 GitHub에 push하지 않으며, 통과 후 커밋 링크를 Notion 카드에 남긴다.
