# 8주차 게임 로직 제출 보고서 — 김우영

## 목표

플레이어가 집중한 건강·운동능력·지력·사회성·경제력·행복·운이 다음 사건 후보와 결과 확률에 영향을 주고, 선택으로 생긴 교육·직업·관계·가족·주거·경제력·업적·사망 기록이 결말까지 이어지도록 한다.

## 구현 내용

### 1. 인생 사실 상태와 저장 호환

- `PeriodRunState.lifeFacts`에 학력, 전공, 직업 경로, 직급, 고용 상태, 결혼 상태, 자녀 수, 주거, 경제력 구간, 업적, 사망 시기·원인을 저장한다.
- `life_facts.sample.json`의 플래그→사실 규칙을 `LifeFactRule`로 읽는다.
- 결과 플래그가 추가되는 즉시 인생 사실도 누적한다.
- v4 저장 형식을 2로 올리고 인생 사실, 대표 초점 스탯, 최근 선택을 함께 저장한다.
- 예전처럼 플래그만 있는 저장은 `synchronizeLifeFacts`로 새 상태에 승격한다.

### 2. 일곱 스탯 경로 전이

| 집중 스탯 | 사건 경로 ID | 대표 누적 사실 | 테스트 결말 |
|---|---|---|---|
| 건강 | `path_hea_*` | 건강 관리 직업·건강 상태 | `ending_hea` |
| 운동능력 | `path_fit_*` | 선수·코치 경력 | `ending_fit` |
| 지력 | `path_int_*` | 진학·전공·연구 경력 | `ending_int` |
| 사회성 | `path_soc_*` | 관계·결혼·상담 경력 | `ending_soc` |
| 경제력 | `path_wea_*` | 취업·창업·주거·자산 | `ending_wea` |
| 행복 | `path_hap_*` | 취미·창작·가족 안정 | `ending_hap` |
| 운 | `path_luk_*` | 기회·위기 회피·행운 업적 | `ending_luk` |

- 초점의 가장 큰 양수 효과를 `dominantFocusStat`으로 기록한다.
- 사건·선택지는 스탯, 플래그, 인생 사실 조건을 모두 만족해야 후보가 된다.
- `path_hea_`부터 `path_luk_`까지의 경로 사건은 대응 초점과 일치할 때 추첨 가중치를 받는다.
- 결과 확률은 기존 `chanceWeights`의 7개 스탯 보정을 그대로 사용한다.
- 최근 사건·선택은 `recentChoiceIds`와 완료 사건 플래그로 다음 후보에 사용할 수 있다.

### 3. 결말과 사망 기록

- 결말 판정은 `특수 사망·업적 > 교육·직업·가족 > 경제력·주거·관계 > 일반 인생 > 기본값` 순서의 `EndingTier`를 먼저 비교하고, 같은 단계에서 기존 priority를 비교한다.
- 결말 조건에 인생 사실 조합(`LifeFactConditions`)을 사용할 수 있다.
- 결과 사망, 건강 0 사망, 확률 사망, 노년 자연사 모두 `deathPeriod`와 `deathCause`를 기록한다.
- 결말 요약에 교육, 전공, 직업, 관계, 자녀, 주거, 경제력, 업적, 실제 사망 시기·원인을 조합한다.
- 기본 결말을 선택해도 단순한 “노년기에서 인생을 마쳤다” 한 문장으로 끝나지 않는다.

## 주요 변경 파일

- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/main/java/com/example/lifegame/data/V4ContentParser.kt`
- `app/src/main/java/com/example/lifegame/data/ContentRepository.kt`
- `app/src/main/java/com/example/lifegame/data/V4ProgressStore.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/Week8LifePathEngineTest.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/V4FullLifeEndingPathTest.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/V4GameEngineTest.kt`

## 자동 테스트

- 7개 스탯 집중 플레이를 각각 독립 테스트로 실행한다.
- 같은 seed와 같은 선택이 같은 상태·사실·결말을 재현하는지 검사한다.
- 7개 경로의 완료 사건, 직업 사실, 결말 ID가 서로 다른지 검사한다.
- 청소년기 건강이 0이 되면 `deathPeriod=TEEN`, `deathCause=급성 질환`이고 결말 요약도 청소년기로 표시되는지 검사한다.
- 구형 플래그 저장이 학력·직업·결혼·자녀·경제력 사실로 승격되는지 검사한다.

## 실행 명령과 결과

```powershell
.\gradlew.bat test assembleDebug --no-daemon
python scripts\validate_content.py
```

- Gradle Debug 테스트: 108개 통과, 실패 0, 오류 0
- Gradle Release 테스트: 108개 통과, 실패 0, 오류 0
- 총 216개 테스트 통과
- `assembleDebug`: BUILD SUCCESSFUL
- 콘텐츠 검증기는 실행했으나 최신 main의 8주차 노년기 데이터가 검증 스크립트보다 먼저 병합되어 오류를 보고한다.
  - `OLD_AGE` 미등록
  - 노년기 초점 이미지 ID 10개 미등록
  - 노년기 초점 로그 템플릿 3개 미만
  - 이 항목은 백엔드 코드 변경과 무관한 DB·QA 통합 선행 조건이며 숨기지 않고 PR에 기록한다.

## 통합 확인 사항

- 시나리오 PR의 `life_facts.sample.json`이 병합되면 별도 코드 변경 없이 실제 플래그를 인생 사실로 변환한다.
- 프론트는 `PeriodRunState.lifeFacts`와 `PeriodGameSession.ending.summary`를 그대로 표시할 수 있다.
- DB·QA의 8주차 조건 데이터와 검증 스크립트가 병합된 뒤 `python scripts/validate_content.py`를 다시 실행해야 한다.
- 팀장 Android Studio 실행 검증 후 main에 병합한다.
