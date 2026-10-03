# 7주차 백엔드 제출 보고서 — 김우영

## 작업 기준

- 기준 브랜치: `origin/main`
- 기준 커밋: `b8abc5bd8d6912d382c20566f5946ac905d1960a`
- 작업 브랜치: `feat/week7-backend-wooyoung`
- 목표: 저장 후 앱을 다시 실행해도 사건, 선택 결과, 결말 화면과 진행 상태를 복원한다.

## 구현 내용

### ProgressStore 저장 형식 보강

- 캐릭터 ID와 `GameProgress`를 하나의 버전 있는 JSON 스냅샷으로 저장한다.
- 지표 7개, 플래그, 완료 사건, 처리한 occurrence ID, 선택 이력, 돌발 사건 횟수와 단계 진행 횟수를 빠짐없이 저장한다.
- 기존 코드에서 누락됐던 `stageEventCount`를 저장·복원한다.
- 손상된 JSON, 알 수 없는 enum, 지원하지 않는 저장 버전은 예외를 앱 밖으로 전파하지 않고 복원 실패로 처리한다.

### GameSession 복원

- 사건 화면은 occurrence ID로 현재 사건을 다시 연결한다.
- 선택 결과 화면은 마지막 선택 이력으로 사건, 선택지, 변화량과 결과 문장을 재구성한다.
- 결말 화면은 저장된 진행 상태로 결말을 다시 판정한다.
- 현재 콘텐츠에서 사건이나 선택지를 찾지 못하면 잘못된 저장으로 판단한다.

### ViewModel과 화면 흐름 연결

- 새 게임 시작, 선택 적용, 다음 사건 이동, 결말 이동 시 자동 저장한다.
- 시작 화면에서 저장 데이터 유무에 따라 이어하기 버튼을 활성화한다.
- 이어하기 시 `EVENT`, `CHOICE_RESULT`, `ENDING`에 맞는 화면으로 이동한다.
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
- `docs/week7_backend_submission.md`

## 추가 테스트

1. 전체 진행 상태 JSON 왕복 시 모든 필드 보존
2. 손상 또는 지원하지 않는 저장 버전 안전 처리
3. 저장된 사건 화면 복원
4. 저장된 선택 결과와 지표 변화량 복원
5. 저장된 결말 화면 복원

## 검증 결과

- `gradlew.bat test`: BUILD SUCCESSFUL
- Debug 단위 테스트: 40개, 실패 0
- Release 단위 테스트: 40개, 실패 0
- `gradlew.bat assembleDebug`: BUILD SUCCESSFUL
- `python scripts/validate_content.py`: PASS
- 콘텐츠 검증 경고: 사건 수 150개로 6주차 시나리오 목표 200개 미만이며, 백엔드 저장 기능과는 별도 담당 범위이다.

## 확인 필요한 부분

- 실제 Android 기기에서 앱 강제 종료 후 사건/결과/결말 각 화면의 복원 동선을 팀장이 최종 확인해야 한다.
- 저장 포맷 버전은 현재 1만 지원하며, 이후 모델 변경 시 마이그레이션 규칙을 추가해야 한다.
