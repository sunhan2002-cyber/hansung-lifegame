# 10주차 실제 콘텐츠 10,000회 시뮬레이션

검증 소스: `6b3730e`. 실행 10,000회, 소요 20.6초.

기본 시작 능력치는 7종 모두 50, 성별/시작 캐릭터 보정 플래그 없음이다. LOW_STATS는 전부 10, BOUNDARY는 1/34/35/59/60/69/70/79/80/99/100으로 변경한다. UI 기본 캐릭터 보정·남성 군복무 경로와 동일한 표본으로 주장하지 않는다.

RANDOM은 목표 스탯 초점을 택하고 가능한 선택지를 시드·사건·시기·해 해시로 고른다. TARGET은 같은 초점과 기본 확률 가중 평균 스탯 보상이 최대인 선택을 고른다. 건강 보상은 lifeDelta를 합산한다. 실제 보정 확률을 미리 아는 최적 정책은 아니다. LOW_HEALTH는 반대로 건강 기대 변화가 최소인 초점·선택을 고른다. LOW_STATS는 연간 합계가 최소인 초점과 해시 선택, BOUNDARY는 경계 시작값과 RANDOM 선택이다. LOW_HEALTH/LOW_STATS의 경로 열은 결말 분류용 라벨이며 해당 경로 초점을 선택했다는 뜻이 아니다.

| 정책 | 경로 | 실행 | 전용 결말 | 비율 | 결말 종류 | 노년 이전 사망 | 평균 돌발 |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |
| BOUNDARY | FITNESS | 143 | 37 | 25.9% | 27 | 8 | 0.65 |
| BOUNDARY | HAPPINESS | 143 | 41 | 28.7% | 24 | 3 | 0.64 |
| BOUNDARY | HEALTH | 143 | 32 | 22.4% | 30 | 0 | 0.75 |
| BOUNDARY | INTELLIGENCE | 143 | 75 | 52.4% | 17 | 6 | 0.64 |
| BOUNDARY | LUCK | 142 | 15 | 10.6% | 20 | 8 | 0.59 |
| BOUNDARY | SOCIAL | 143 | 56 | 39.2% | 25 | 6 | 0.75 |
| BOUNDARY | WEALTH | 143 | 19 | 13.3% | 27 | 2 | 0.63 |
| LOW_HEALTH | FITNESS | 143 | 0 | 0.0% | 7 | 52 | 0.64 |
| LOW_HEALTH | HAPPINESS | 143 | 0 | 0.0% | 7 | 51 | 0.64 |
| LOW_HEALTH | HEALTH | 143 | 6 | 4.2% | 7 | 58 | 0.76 |
| LOW_HEALTH | INTELLIGENCE | 143 | 86 | 60.1% | 7 | 55 | 0.68 |
| LOW_HEALTH | LUCK | 142 | 0 | 0.0% | 7 | 49 | 0.63 |
| LOW_HEALTH | SOCIAL | 143 | 0 | 0.0% | 7 | 57 | 0.73 |
| LOW_HEALTH | WEALTH | 143 | 0 | 0.0% | 7 | 48 | 0.69 |
| LOW_STATS | FITNESS | 143 | 0 | 0.0% | 5 | 24 | 0.80 |
| LOW_STATS | HAPPINESS | 143 | 0 | 0.0% | 5 | 12 | 0.73 |
| LOW_STATS | HEALTH | 143 | 31 | 21.7% | 6 | 19 | 0.85 |
| LOW_STATS | INTELLIGENCE | 143 | 0 | 0.0% | 5 | 18 | 0.80 |
| LOW_STATS | LUCK | 142 | 0 | 0.0% | 5 | 17 | 0.79 |
| LOW_STATS | SOCIAL | 143 | 0 | 0.0% | 5 | 18 | 0.85 |
| LOW_STATS | WEALTH | 143 | 0 | 0.0% | 5 | 15 | 0.76 |
| RANDOM | FITNESS | 500 | 194 | 38.8% | 17 | 12 | 0.71 |
| RANDOM | HAPPINESS | 500 | 24 | 4.8% | 22 | 5 | 0.75 |
| RANDOM | HEALTH | 500 | 242 | 48.4% | 11 | 2 | 0.76 |
| RANDOM | INTELLIGENCE | 500 | 441 | 88.2% | 10 | 25 | 0.68 |
| RANDOM | LUCK | 500 | 82 | 16.4% | 27 | 11 | 0.74 |
| RANDOM | SOCIAL | 500 | 186 | 37.2% | 19 | 13 | 0.75 |
| RANDOM | WEALTH | 500 | 242 | 48.4% | 22 | 11 | 0.70 |
| TARGET | FITNESS | 500 | 438 | 87.6% | 5 | 9 | 0.67 |
| TARGET | HAPPINESS | 500 | 452 | 90.4% | 7 | 6 | 0.73 |
| TARGET | HEALTH | 500 | 454 | 90.8% | 6 | 0 | 0.78 |
| TARGET | INTELLIGENCE | 500 | 287 | 57.4% | 9 | 10 | 0.68 |
| TARGET | LUCK | 500 | 264 | 52.8% | 11 | 8 | 0.73 |
| TARGET | SOCIAL | 500 | 460 | 92.0% | 8 | 7 | 0.73 |
| TARGET | WEALTH | 500 | 476 | 95.2% | 4 | 3 | 0.71 |

## 인수 판정

**FAIL**. 치명 불변식 위반 판 수: 0.

- F10-02/13: HAPPINESS random path rate=4.8%, target>=20%
- F10-02/13: LUCK random path rate=16.4%, target>=20%

관측 사건 246개, 결말 53개. 미관측 결말 12개:

`ending_ai_storyteller`, `ending_calm_recovery`, `ending_fragile_but_alive`, `ending_health_guardian`, `ending_late_blooming_developer`, `ending_lucky_adventurer`, `ending_rescue_survivor`, `ending_star_athlete`, `ending_tiny_startup_survivor`, `ending_wandering_freelancer`, `ending_warm_teacher`, `ending_wealth_builder`

## 증거와 제한

동일 정책·시드 재실행 20회 비교. 매 250번째 실행의 각 phase에서 엔진 상태 복원을 검사했다. 이는 Android 저장소 또는 실제 프로세스 종료 시험을 대신하지 않는다.

summary.json에는 경로별 교육·직업·가족·주거·경제력 분포가 있다. coverage.json은 실제 사건/선택/결과/잠금 및 시기별 돌발 노출 수, demo_seeds.json은 재현 선택 순서다. raw runs.jsonl은 별도 빌드 산출물이며 저장소에 대용량 원본을 넣지 않는다.

칭호 분포는 QA 독립 기대값이며 앱 칭호 기능의 실행 결과가 아니다. ending_witnesses.json은 합성 상태에서 조건·우선순위를 확인한 자료로, 정상 플레이로 도달했다는 증거가 아니다. 미관측/증인 불일치는 담당자 검토 대상으로 남긴다.
