# 6주차 백엔드 제출 보고서 — 김우영

## 작업 기준

- 기준 브랜치: `main`
- 기준 커밋: `ba9a9ad85ba165eb77fd247cce71b493be7197c7`
- 작업 브랜치: `feat/week6-backend-wooyoung`
- PR 제목: `[6주차] 백엔드 - 김우영`
- 작업일: 2026-10-02

## 수정 전 확인 결과

- `completedEventIds`가 완료한 사건을 다음 후보에서 제외하므로 같은 사건 반복은 이미 방지되고 있었다.
- 돌발 이벤트 회차당 최대 횟수는 `maxSpecialEventsPerRun`으로 제한되고 있었다.
- 성장 단계는 단계당 5개 사건 후 `INFANT → CHILD → TEEN → ADULT` 순서로 이동했다.
- 성인기에 후보 사건이 없으면 결말 화면으로 이동했다.
- 지표는 선택 반영 시 0~100으로 제한되었다.
- 결말은 priority 내림차순, 동점이면 endingId 오름차순으로 하나를 선택했다.
- 일반 사건을 충분히 진행하기 전 돌발 이벤트를 막는 조건은 없었다.

## 구현 내용

`GameEngine`에 `minNormalEventsBeforeSpecial` 설정을 추가했다. 기본값은 2이며, 음수 설정은 거부한다.

돌발 이벤트를 고르기 전에 현재 콘텐츠의 일반 사건 ID와 `completedEventIds`를 대조한다. 완료한 일반 사건이 2개 미만이면 확률이 100%여도 돌발 이벤트를 반환하지 않는다. 완료한 돌발 사건은 일반 사건 진행 횟수에 포함하지 않는다.

기존 동작은 유지된다.

- 완료한 사건 재선택 방지
- 돌발 이벤트 회차당 최대 2회 제한
- 조건에 맞는 돌발 후보 중 priority 내림차순, 동점 시 eventId 오름차순
- 단계당 5개 사건 후 성장 단계 이동
- 성인기 사건 소진 후 결말 이동
- 7개 지표 0~100 제한
- 결말 priority 우선 및 동점 안전 처리

## 변경 파일

- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameEngineTest.kt`
- `docs/week6_backend_submission.md`

## 새로 추가한 테스트 8개

1. `applyChoice_clampsAllSevenStatsAtBothBoundaries`
2. `pickSpecialEvent_isLockedBeforeTwoNormalEvents`
3. `pickSpecialEvent_doesNotCountCompletedSpecialAsNormalProgress`
4. `pickSpecialEvent_isUnlockedAfterTwoNormalEvents`
5. `advanceAfterResult_usesNormalEventWhileSpecialIsLocked`
6. `advanceAfterResult_canUseSpecialAfterTwoNormalEvents`
7. `moveNextStage_advancesThroughEveryLifeStageInOrder`
8. `advanceAfterResult_movesStageAfterFiveHandledEvents`

기존 돌발 이벤트 최대 횟수 및 우선순위 테스트도 일반 사건 2개 완료 조건을 포함하도록 보강했다.

## 검증 결과

### 전체 테스트

명령:

```text
.\gradlew.bat test
```

결과:

- BUILD SUCCESSFUL
- Debug 단위 테스트: 43개
- Release 단위 테스트: 43개
- 실패 0, 오류 0, 건너뜀 0

### Android 디버그 빌드

명령:

```text
.\gradlew.bat assembleDebug
```

결과:

- BUILD SUCCESSFUL

## 항목별 확인 결과

- 사건 반복: 완료 사건은 후보에서 제외됨
- 돌발 이벤트: 일반 사건 0~1개 완료 시 차단, 2개 완료 후 허용
- 돌발 이벤트 최대 횟수: 기본 회차당 2회 유지
- 성장 단계: INFANT, CHILD, TEEN, ADULT 순서 확인
- 성인기 이후: 후보 사건 소진 시 ENDING 이동 확인
- 지표 범위: 7개 지표 모두 0~100 확인
- 결말 우선순위: 높은 priority 선택 확인
- 결말 동점: endingId 기준으로 하나를 안정적으로 선택

## 확인 필요한 부분

- 돌발 이벤트 발생 확률은 현재 기본 10%이며 실제 플레이 밸런스는 통합 후 조정할 수 있다.
- 성장 단계 전환 기준은 현재 단계당 사건 5개이며 콘텐츠 수 증가 후 팀장 검증에서 최종 조정할 수 있다.
- GitHub Actions가 별도로 설정되지 않은 저장소이므로 팀장 환경의 Android Studio 빌드 검증이 필요하다.

## PR 설명문

### 작업한 파일

- `GameEngine.kt`
- `GameEngineTest.kt`
- `docs/week6_backend_submission.md`

### 구현한 내용

- 일반 사건 2개 완료 전 돌발 이벤트 발생 차단
- 완료한 돌발 사건을 일반 사건 진행 횟수에서 제외
- 기존 사건 중복 방지, 돌발 최대 횟수, 단계 전환, 결말 판정 로직 회귀 검증

### 테스트한 내용

- 새 테스트 8개 추가
- 기존 돌발 이벤트 테스트 2개 보강
- `gradlew.bat test` 성공: Debug/Release 각각 43개, 실패 0
- `gradlew.bat assembleDebug` 성공

### 부족하거나 확인 필요한 부분

- 돌발 이벤트 확률 10%와 단계당 사건 5개 설정은 통합 플레이 후 밸런스 확인 필요
- CI 미설정으로 팀장 Android Studio 환경에서 최종 실행 검증 필요
