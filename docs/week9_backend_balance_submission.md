# 9주차 백엔드 밸런스 검증 — 김우영

## 목표

실제 앱 콘텐츠로 다중 seed 플레이를 반복해 확률·사망·경제력·결말이 한 경로로 고정되지 않는지 확인하고, 9주차 이후 구조 변경 없이 회귀 검사가 가능하게 한다.

## 추가한 자동 검증

- 파일: `app/src/test/java/com/example/lifegame/domain/engine/Week9BalanceSimulationTest.kt`
- 건강·운동능력·지력·사회성·경제력·행복·운 7경로를 각각 50개 seed로 실행한다.
- 총 350회 모두 실제 `periods`, `focuses`, `events`, `endings`, `life_facts` 자산을 사용한다.
- 선택지는 seed·사건·시기·연차로 분산해 성공 선택만 고르는 기존 테스트의 편향을 줄였다.
- 다음 조건을 자동 검사한다.
  - 150단계 안에 결말 도달
  - 모든 선택지가 잠기는 사건 0건
  - 사망 시기와 결말 시기 일치
  - 사망 원인 누락 0건
  - 영아·유년 사망 0건
  - 실제 콘텐츠에서 노년기 이전 종료 최소 1건 유지
  - 경로별 결말 종류 최소 3개
  - 경로별 전용 결말 최소 1개 도달

## 350회 측정 결과

| 집중 경로 | 전용 경로 결말 | 결말 종류 | 노년기 이전 종료 | 최종 경제력 분포 |
|---|---:|---:|---:|---|
| 건강 | 13/50 (26%) | 8 | 0 | 낮음 4, 중하 46 |
| 운동능력 | 13/50 (26%) | 8 | 0 | 낮음 4, 중하 46 |
| 지력 | 46/50 (92%) | 6 | 0 | 중하 17, 중간 33 |
| 사회성 | 21/50 (42%) | 11 | 0 | 낮음 20, 중하 30 |
| 경제력 | 18/50 (36%) | 12 | 1 | 중간 5, 높음 31, 매우 높음 14 |
| 행복 | 5/50 (10%) | 14 | 0 | 낮음 40, 중하 10 |
| 운 | 3/50 (6%) | 15 | 1 | 낮음 23, 중하 27 |

- 350회 중 중년기 사망 2회, 노년기 종료 348회다.
- 영아기·유년기 사망은 없다.
- 경제력 집중은 90%가 `HIGH` 또는 `VERY_HIGH`로 끝난다.

## 확인된 협업 필요 항목

### 건강과 운동능력 초점 중복

현재 `focuses.sample.json`에서 건강과 운동능력 집중 테스트가 고르는 초점이 모든 시기에 동일하다.

| 시기 | 건강·운동능력 공통 선택 |
|---|---|
| 유년기 | `focus_child_sports` |
| 청소년기 | `focus_teen_fitness` |
| 청년기 | `focus_young_adult_health` |
| 중년기 | `focus_middle_health` |
| 노년기 | `focus_old_age_health` |

따라서 두 경로는 같은 seed에서 같은 사건·선택·결말을 만든다. 엔진이 임의로 경로를 추정하면 데이터와 화면 설명이 달라지므로 아래 중 하나를 DB·시나리오와 합의해야 한다.

1. 청년기 이후 운동능력 전용 초점을 추가한다.
2. `Focus` JSON에 명시적인 `pathStat`을 추가하고 엔진은 그 값을 사용한다.

### 밸런스 목표값 필요

행복과 운의 전용 결말 도달률이 각각 10%, 6%로 낮고, 조기 사망은 0.57%다. 경제력은 높은 구간으로 치우친다. 수치를 변경하기 전 팀에서 아래 목표 범위를 확정해야 한다.

- 경로 전용 결말 목표 비율
- 청년기·중년기 조기 사망 목표 비율
- 경제력 구간별 목표 분포
- 성공/실패 결말 목표 비율

목표가 확정되면 `eventSelectionWeight`, `healthDeathStep`, 경제력 연간 변화와 결말 조건을 최소 범위로 조정한다.

## 실행 방법

```powershell
.\gradlew.bat testDebugUnitTest --tests com.example.lifegame.domain.engine.Week9BalanceSimulationTest
```

전체 회귀 검증:

```powershell
python scripts\validate_content.py
python scripts\validate_images.py
python scripts\validate_week8_contracts.py
python scripts\audit_choice_conditions.py
.\gradlew.bat test assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2
```

## 현재 판정

- 엔진의 완주·저장 데이터·금지 사망 시기 불변 조건은 통과한다.
- 경로별 결말 다양성은 유지된다.
- 건강/운동능력 분리와 수치 조정은 콘텐츠 필드 또는 목표 비율 합의가 필요한 상태다.
