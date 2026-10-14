# 8주차 백엔드 제출 보고서 — 김우영

## 목표

v4 시기 진행을 처음부터 끝까지 완주했을 때 기존 결말 우선순위 규칙으로 실제 결말 하나를 선택하고, 서로 다른 최소 3개 경로를 자동 검증한다.

## 구현 내용

- `GameEngine.resolveEndingDecision(PeriodRunState, endings)` 추가
- 기존 `GameProgress`와 v4 `PeriodRunState`가 동일한 결말 조건·우선순위·기본 결말 규칙을 사용
- `PeriodGameSession`에 선택된 `ending` 상태 추가
- 자연 완주와 사망 종료 모두 마지막 `TurnStep` 기록 시 결말 판정
- 기존 생성자 호출을 깨지 않도록 `endings` 기본값은 빈 목록으로 유지

## 전체 플레이 결말 경로

| 경로 | 전체 진행 | 기대 결말 |
|---|---|---|
| 공부 초점 | 5개 시기 완주, `study_focus`, 지력 85 이상 | `ending_genius_researcher` |
| 운동 초점 | 5개 시기 완주, `sports_training`, 건강·운동능력 조건 | `ending_star_athlete` |
| 평범한 초점 | 5개 시기 완주, 전문 결말 조건 불충족 | `ending_ordinary_path` |

각 경로는 고정 seed, 시기별 장 표지, 초점 선택, 마일스톤 결정, 결과 반영, 다음 시기 전환, 마지막 결말까지 한 번에 실행한다.

## 수정 파일

- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/V4FullLifeEndingPathTest.kt`
- `docs/week8_backend_submission.md`

## 검증 명령

```powershell
.\gradlew.bat test
.\gradlew.bat assembleDebug
python scripts\validate_content.py
```

## 검증 결과

- 전체 단위 테스트: Debug 94개 + Release 94개 = 188개
- 실패 0, 오류 0, 건너뜀 0
- 신규 전체 생애 결말 경로 테스트: Debug·Release 각각 3개 통과
- `assembleDebug`: BUILD SUCCESSFUL
- 콘텐츠 검증: PASS
  - 시기 5개, 전체 24년
  - 초점 25개
  - 사건 209개
  - BIG_INCIDENT 20개
  - outcomes 선택지 609개
  - 결말 31개, 기본 결말 1개
  - 이미지 ID 346개

## 협업 확인 사항

- 프론트는 `PeriodGameSession.ending`의 제목과 요약을 결말 화면에 표시할 수 있다.
- 저장·이어하기에서 v4 `PeriodRunState`와 pending 결정을 영구 복원하는 작업은 별도 8주차 저장 연동 범위다.
- 팀장 검증 후에만 main에 병합한다.
