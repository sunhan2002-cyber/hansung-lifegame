# 8주차 일곱 스탯 인생 경로

- 담당: 신기훈 (시나리오·콘텐츠)
- 기준: `8주차_숙제_신기훈_시나리오.md`, `8주차_숙제_통합지시서.md`, 선택지 조건 정책(`docs/qa/week8_event_stat_classification.md`)
- 수정 파일: `events.sample.json`(+61), `endings.sample.json`(+34), `image_ids.sample.json`(+71), `focuses.sample.json`(노년기 기록 문장), `life_facts.sample.json`(신규)
- 기존 사건 ID·문장·조건은 하나도 바꾸지 않았다. 새 사건은 `path_<경로>_<번호>` ID로 파일 끝에 붙였다.

## 1. 완료 기준

| 기준 (숙제 문서) | 결과 |
| --- | --- |
| 일곱 스탯 독립 경로 | 7개 (지력·운동능력·사회성·경제력·행복·운·건강) |
| 경로별 핵심 사건 8개 이상 | 지력 10 / 운동능력 9 / 사회성 9 / 경제력 9 / 행복 8 / 운 8 / 건강 8 |
| 경로별 분기 선택지 3개 이상 | 모든 경로 7개 이상 (검증 스크립트 표 참고) |
| 경로별 중간 성과 2개 이상 | 업적 플래그(`achv_*`) 경로별 5~10개 |
| 경로별 전용 결말 2개 이상 | 지력 5 / 운동능력 5 / 사회성 4 / 경제력 6 / 행복 4 / 운 6 / 건강 4 |
| 전체 신규 사건 40개 이상 | 61개 (선택지 154개) |
| 전체 신규 결말 10개 이상 | 34개 |
| 다른 스탯과의 상호작용 사건 | 경로마다 3개 이상 (다른 스탯이 결과 가능성이나 선택지 잠금을 바꿈) |
| 실패도 경로 | 재수·자격시험 실패·부상 은퇴·이혼·실직·가게 폐업·빚·전세 손실·번아웃·만성 질환이 다음 사건 조건이 됨 |
| 모든 신규 선택지에 조건과 결과 사실 | 선택지 154개 모두 `requiredStats/requiredFlags/blockedFlags` 또는 `alwaysAvailable`+사유 |
| 한 사건의 모든 선택지가 잠기는 경우 | 0건 |

## 2. 숙제 필드와 실제 JSON 필드

다른 담당자의 필드 이름을 바꾸지 않으려고 기존 형식을 그대로 썼다. 숙제 문서의 이름은 아래처럼 대응한다.

| 숙제 문서 | 실제 JSON | 비고 |
| --- | --- | --- |
| `id` | `eventId` | `path_int_01` 형식 |
| `period` | `periods` (+ 기존 앱용 `stage`) | 노년기는 `OLD_AGE`, stage는 `ADULT` |
| `kind` | `kind` | 전환점 `MILESTONE`, 나머지 `INCIDENT`. `BIG_INCIDENT`는 쓰지 않음 (4장) |
| `body` | `text` | |
| `requires` | 사건 `conditions.stats` / 선택지 `requiredStats` | 선택지 잠금은 MILESTONE만 |
| `requiresFlags` | 사건 `conditions.requiredFlags` / 선택지 `requiredFlags` | 제외는 `blockedFlags` |
| `outcomes` | `choices[].outcomes[]` | `chance`, `chanceWeights`, `primary`, `hidden`, `death` |
| `addFlags` / `removeFlags` | `outcomes[].addFlags` / `outcomes[].removeFlags` | `removeFlags`는 엔진 반영 필요 (8장) |
| 후속 사건 ID | `followUps` | 이 사건 다음에 이어질 수 있는 경로 사건 |
| 경로 주석 | `lifePath`, `pathRole` | 예: `INTELLIGENCE`, `전환점` |
| 결과로 추가되는 사실 | 사실 플래그 + `life_facts.sample.json` | 5장 |

## 3. 설계 규칙

1. **경로 입구는 스탯 기준선**: 다른 스탯에 집중한 플레이의 상위 10%보다 높게 잡았다 (엔진 시뮬레이션 기준, 6장).

| 스탯 | 유년기 | 청소년기 | 청년기 | 중년기 |
| --- | ---: | ---: | ---: | ---: |
| 지력 | 62 | 72 | 82 | 88 |
| 운동능력 | 63 | 70 | 82 | 85 |
| 사회성 | 64 | — | 82 | 88 |
| 경제력 | — | 56 | 62 | 75 |
| 행복 | 74 | — | — | 80 (공연 선택지) |
| 운 | 59 | — | 64 | 72 |
| 건강 | — (선택으로 입장) | — (선택으로 입장) | — | 70 |

2. **건강은 수치로 가를 수 없다**: 기존 결과에 남은 `lifeDelta: +10`이 건강에 합산되어 거의 모든 플레이의 건강이 100 근처다. 그래서 건강 경로는 "브로콜리를 다 먹는다", "불을 끄고 잔다"처럼 **건강을 고른 선택**으로 들어오고, 악화 쪽은 `health_warning`(과로·몸살·쓰러짐 결과)으로 들어온다.
3. **전환점은 조건부 MILESTONE**: 청년기는 대학/취업·입영·결혼 정지 칸이 해를 거의 다 쓴다. 그래서 일반 사건이 1~2번밖에 안 나온다. 그래서 경로의 뼈대(영재교육원, 특기생, 첫 직장, 내 집 마련 등)는 해를 정하지 않은 MILESTONE으로 두어 조건이 되는 해에 바로 나오게 했다. 고정 해 정지 칸이 항상 먼저 나온다.
4. **나머지는 가중치 20의 일반 사건**: 조건(경로 플래그)이 맞을 때만 후보에 들어가므로 다른 플레이의 사건을 밀어내지 않는다.
5. **선택지 잠금은 전환점에서만** (팀장 정책): 일반 사건 선택지는 `alwaysAvailable` + 사유를 달고, 스탯은 `chanceWeights`로 결과 가능성만 바꾼다. 전환점에는 항상 기본 선택지를 하나 남겼다.
6. **조건에는 사라지지 않는 플래그만**: 엔진이 아직 `removeFlags`를 처리하지 않아도 경로가 꼬이지 않게, 조건은 "한 번 붙으면 계속 참인" 플래그로만 걸었다 (예: 실직 뒤 재취업은 `job_unemployed`를 지우지 않고 `comeback`으로 표시).
7. **lifeDelta·BIG_INCIDENT 미사용**: 인수인계 문서 규칙대로 건강 변화는 `statDelta.건강`에만 적었다. BIG_INCIDENT는 파서가 결과마다 건강 -25를 자동으로 넣으므로 경로 사건에는 쓰지 않았다.
8. **기존 사건과 이어 붙이기**: 대학 진학(`university`), 취업(`early_job`), 쉬는 해(`gap_year`), 결혼(`married`), 과로(`overwork`), 건강 경고(`health_warning`)를 기존 정지 칸·사건에서 받아 다음 경로 사건의 조건으로 썼다. 반대로 새 경로에서 결혼하면 기존 "첫 아이", "닫힌 방문", "빈 방"이 이어진다.
9. **기존 결말 플래그 연결**: 아무도 붙이지 않던 결말 조건 플래그 6개를 뜻이 맞는 경로 결과에 붙였다. 이제 도달할 수 없는 결말은 0개다.
   - `sports_training`: 육상부 입단
   - `club_joined`: 축제 준비위원회
   - `money_habit`, `part_time_job`: 첫 알바와 적금
   - `family_memory`: 가족과 보낸 결과
   - `special_survived`: 위기를 피한 결과

## 4. 경로별 연결표

### 4.1 지력 경로

흐름: 시험 → 영재교육원 → 수능 → 전공·장학금 / 재수 / 야간대학 → 대학원·연구소·변리사 → 연구 성과·특허·강단 → 마지막 강의

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_int_01 | 유년기 | INCIDENT | 입구 | 교내 수학 경시대회 | 지력 62+ | A. 대회에 나간다. (항상) → `int_path` `achv_math_award` <sub>결과 2갈래</sub><br>B. 친구 생일 파티에 간다. (항상) → -<br>C. 대회 문제를 직접 만들어서 선생님께 낸다. (항상) → `int_path` `achv_problem_setter` `achv_math_award` <sub>결과 2갈래</sub> | path_int_02 |
| path_int_02 | 청소년기 | MILESTONE | 전환점 | 영재교육원 선발 시험 | 지력 72+ | A. 추천을 받아 시험을 본다. (지력 75+) → `int_path` `int_gifted` `achv_gifted_program` `int_gifted_fail` <sub>결과 2갈래</sub><br>B. 혼자 공부하는 게 더 맞다. (항상) → `int_path` `int_self_study`<br>C. 추천을 친구에게 양보한다. (사회성 60+) → `int_path` `helped_friend` | path_int_03 |
| path_int_03 | 청소년기 | INCIDENT | 시험·상호작용(건강) | 수능 D-30 | `int_path` | A. 하루 네 시간만 자고 문제를 더 푼다. (항상) → `int_csat_top` `overwork` `int_csat_fail` <sub>결과 2갈래</sub><br>B. 평소처럼 자고 평소처럼 푼다. (항상) → `int_csat_top` `int_csat_fail` <sub>결과 2갈래</sub><br>C. 사물함에 '수능 대박' 엿을 붙이고 인증샷을 올린다. (항상) → `int_csat_top` `int_csat_fail` <sub>결과 2갈래</sub> | path_int_04, path_int_05, path_int_06 |
| path_int_04 | 청년기 | INCIDENT | 진학·장학금·상호작용(경제력) | 3.98과 장학금 커트라인 | `int_path`, `university` | A. 교수님께 성적 확인 메일을 보낸다. (항상) → `scholarship` `int_major_strong` <sub>결과 2갈래</sub><br>B. 연구실 인턴에 지원한다. (항상) → `int_lab` <sub>결과 2갈래</sub><br>C. 과외 세 개로 등록금을 직접 번다. (항상) → `int_tutor` `student_loan` <sub>결과 2갈래</sub> | path_int_07 |
| path_int_05 | 청년기 | INCIDENT | 실패 후 재도전 | 쉬어 가는 1년, 다시 수능 | `int_path`, `gap_year` | A. 다시 수능을 본다. (항상) → `university` `field_science` `int_retake_success` `int_retake_fail` <sub>결과 2갈래</sub><br>B. 혼자 코딩을 배워 바로 일을 구한다. (항상) → `int_self_taught` `career_engineer` `job_employed` | path_int_07 |
| path_int_06 | 청년기·중년기 | INCIDENT | 취업한 지력형 — 다른 길 | 퇴근 후 야간대학 | `int_path`, `early_job` | A. 야간대학에 등록한다. (항상) → `university` `achv_night_degree` `int_night_dropout` <sub>결과 2갈래</sub><br>B. 사내 기술 자격증부터 딴다. (항상) → `career_engineer` `level_mid` | path_int_08 |
| path_int_07 | 청년기 | MILESTONE | 전환점 | 졸업 앞의 세 갈래 | 지력 82+, `int_path`, `university` | A. 대학원에 진학한다. (지력 85+) → `edu_graduate` `career_researcher` `job_employed`<br>B. 대기업 연구소에 입사한다. (항상) → `career_engineer` `job_employed`<br>C. 변리사 시험에 도전한다. (지력 90+) → `career_patent_attorney` `job_employed` `achv_license` `career_engineer` `int_exam_fail` <sub>결과 2갈래</sub> | path_int_08, path_int_09 |
| path_int_08 | 중년기 | INCIDENT | 성과 vs 가족·상호작용(건강·사회성) | 마감 D-1과 가족 모임 | `int_path`, `job_employed` | A. 주말 내내 결과를 마무리한다. (항상) → `achv_major_paper` `int_burnout` `health_warning` `overwork` `int_project_delayed` <sub>결과 3갈래·숨김</sub><br>B. 가족 모임에 간다. (항상) → `family_bond` `family_memory`<br>C. 노트북을 들고 가족 모임에 간다. (항상) → `achv_major_paper` `int_project_delayed` <sub>결과 2갈래</sub> | path_int_09, path_int_10 |
| path_int_09 | 중년기 | MILESTONE | 전환점·중년 성과 | 평생 연구의 마지막 퍼즐 | 지력 88+, `int_path`, `job_employed` | A. 연구를 끝까지 밀어붙인다. (`career_researcher`) → `achv_breakthrough` `level_senior` `int_project_cancelled` <sub>결과 2갈래</sub><br>B. 결과를 특허로 낸다. (`career_engineer`) → `patent_holder` `achv_patent` `level_senior`<br>C. 은사님 강의를 맡아 후배를 가르친다. (항상) → `achv_mentor` `helped_friend` `career_professor` | path_int_10 |
| path_int_10 | 노년기 | INCIDENT | 노년 결과 | 마지막 강의 | `int_path` | A. 평생의 연구 이야기를 한다. (`achv_major_paper`) → `achv_last_lecture`<br>B. 실패한 이야기만 골라서 한다. (항상) → `achv_last_lecture`<br>C. 손주 숙제를 대신 풀어 주고 강연은 미룬다. (항상) → `family_bond` `family_memory` | - |

지력 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_int_researcher | 97 | 질문 하나로 평생을 산 연구자 | `int_path`, `edu_graduate`, `achv_breakthrough`, 지력 85+ |
| ending_path_int_engineer | 96 | 특허 한 줄에 이름을 남긴 엔지니어 | `int_path`, `achv_patent` |
| ending_path_int_teacher | 95 | 질문을 물려준 선생님 | `int_path`, `achv_mentor` |
| ending_path_int_late | 94 | 퇴근길에 딴 졸업장 | `int_path`, `achv_night_degree` |
| ending_path_int_burnout | 93 | 책상 앞에서 놓친 것들 | `int_path`, `int_burnout` |

### 4.2 운동능력 경로

흐름: 육상부 → 첫 대회 → 체육 특기생 → 전국체전·부상 → 프로·체육교사·트레이너 → 주전 경쟁 → 수술·은퇴 → 체육관·지도자 → 시니어 마라톤

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_fit_01 | 유년기 | INCIDENT | 입구 | 체육관 관장님의 명함 | 운동능력 63+ | A. 육상부에 들어간다. (항상) → `fit_path` `sports_club` `sports_training`<br>B. 운동은 취미로만 한다. (항상) → -<br>C. 명함을 받아 들고 운동장 한 바퀴를 더 돈다. (항상) → `fit_path` `sports_club` `sports_training` | path_fit_02 |
| path_fit_02 | 유년기·청소년기 | INCIDENT | 첫 경기·상호작용(운) | 첫 대회 결승선 | `fit_path` | A. 끈을 묶고 다시 자세를 잡는다. (항상) → `achv_youth_medal` `fit_rival` <sub>결과 2갈래</sub><br>B. 그냥 달린다. (항상) → `achv_youth_medal` `fit_first_loss` <sub>결과 2갈래</sub> | path_fit_03 |
| path_fit_03 | 청소년기 | MILESTONE | 전환점 | 체육 특기생 제안 | 운동능력 70+, `fit_path` | A. 체육 특기생으로 간다. (운동능력 74+) → `fit_path` `fit_elite`<br>B. 일반고에 가서 공부와 병행한다. (항상) → `fit_path` `fit_dual`<br>C. 운동을 그만두고 공부에 집중한다. (항상) → `fit_quit` | path_fit_04, path_fit_05 |
| path_fit_04 | 청소년기 | INCIDENT | 경기·부상·상호작용(건강) | 전국체전 결승, 무릎의 소리 | `fit_path`, 제외 `fit_quit` | A. 테이핑하고 결승에 뛴다. (항상) → `achv_national_champion` `fit_injury` `health_injured` `achv_youth_medal` <sub>결과 3갈래</sub><br>B. 기권하고 병원에 간다. (항상) → `healthy_routine` | path_fit_05 |
| path_fit_05 | 청년기 | MILESTONE | 전환점·직업 | 프로 입단 테스트 | `fit_path`, 제외 `fit_quit` | A. 프로 테스트를 본다. (운동능력 82+) → `career_athlete` `pro_athlete` `job_employed` `career_coach` `fit_pro_fail` <sub>결과 2갈래</sub><br>B. 체육교육과에 진학한다. (항상) → `university` `field_sports` `career_pe_teacher` `job_employed`<br>C. 재활 트레이너가 된다. (`fit_injury`) → `career_trainer` `job_employed` `helped_friend` | path_fit_06, path_fit_07, path_fit_08 |
| path_fit_06 | 청년기·중년기 | INCIDENT | 직업 성과·상호작용(사회성) | 주전 경쟁 | `career_athlete` | A. 훈련량을 두 배로 늘린다. (항상) → `achv_league_best` `fit_bench` `health_injured` <sub>결과 2갈래</sub><br>B. 후배에게 밥을 사며 비결을 묻는다. (항상) → `achv_league_best` `lifelong_friend` `fit_bench` <sub>결과 2갈래</sub> | path_fit_07 |
| path_fit_07 | 청년기·중년기 | INCIDENT | 부상·은퇴 갈림길(실패) | 세 번째 수술 상담 | `fit_path`, `health_injured` | A. 수술하고 복귀를 노린다. (항상) → `achv_comeback` `fit_retired` `health_limited` <sub>결과 2갈래</sub><br>B. 은퇴하고 유소년 감독이 된다. (항상) → `career_coach` `coach_path` `fit_retired`<br>C. 해설위원 제안을 받는다. (항상) → `career_caster` `fit_retired` | path_fit_08 |
| path_fit_08 | 중년기 | MILESTONE | 전환점·중년 결과 | 내 이름을 건 체육관 | 운동능력 85+, `fit_path`, 제외 `fit_quit` | A. 내 이름을 건 체육관을 연다. (경제력 60+) → `career_gym_owner` `own_shop` `job_self_employed` `achv_own_gym` `fit_gym_closed` `in_debt` <sub>결과 2갈래</sub><br>B. 후배들을 길러 내는 지도자로 남는다. (항상) → `career_coach` `achv_trained_champion` | path_fit_09 |
| path_fit_09 | 노년기 | INCIDENT | 노년 결과·상호작용(건강) | 시니어 마라톤 출발선 | `fit_path` | A. 풀코스에 도전한다. (항상) → `achv_senior_marathon` <sub>결과 2갈래</sub><br>B. 무릎을 생각해 응원석에 선다. (`health_limited`) → -<br>C. 5km 걷기 코스로 바꾼다. (항상) → `family_bond` `family_memory` | - |

운동능력 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_fit_legend | 97 | 경기장에 이름을 남긴 선수 | `career_athlete`, `achv_league_best`, 운동능력 85+ |
| ending_path_fit_coach | 95 | 누군가의 첫 메달을 만든 지도자 | `fit_path`, `career_coach` |
| ending_path_fit_gym | 94 | 내 이름을 건 체육관 관장 | `achv_own_gym` |
| ending_path_fit_injury | 93 | 부상으로 멈췄지만 다시 걸은 사람 | `fit_path`, `fit_retired`, `health_limited` |
| ending_path_fit_teacher | 92 | 운동장을 지킨 체육 선생님 | `career_pe_teacher` |

### 4.3 사회성 경로

흐름: 반장 선거 → 축제 준비위원회 → 첫 연애 → 사람으로 들어온 일자리 → 상견례·신혼집 → 부부 위기·이혼 → 주민자치·연결 모임 → 칠순 잔치

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_soc_01 | 유년기 | INCIDENT | 입구 | 반장 선거 연설 | 사회성 64+ | A. 친구들 이름을 하나씩 부르며 즉석 연설을 한다. (항상) → `soc_path` `class_leader` `achv_class_president` <sub>결과 2갈래</sub><br>B. 다른 후보를 지지한다고 말하고 내려온다. (항상) → `soc_path` `leader_experience` | path_soc_02 |
| path_soc_02 | 청소년기 | INCIDENT | 협업·상호작용(행복) | 축제 준비위원회의 밤 | `soc_path` | A. 양쪽을 모아 밤새 예산표를 다시 짠다. (항상) → `soc_team_lead` `achv_festival` `club_joined` `soc_conflict` <sub>결과 2갈래</sub><br>B. 투표로 정하자고 한다. (항상) → `soc_team_lead` `club_joined`<br>C. 내 용돈으로 모자란 예산을 채운다. (항상) → `soc_team_lead` `secret_payer` `club_joined` | path_soc_03 |
| path_soc_03 | 청소년기·청년기 | INCIDENT | 연애 | 집에 가는 길이 같은 사람 | `soc_path` | A. "같이 내려서 걸을래?"라고 묻는다. (항상) → `dating` `first_love` `best_friend` <sub>결과 2갈래</sub><br>B. 모르는 척 다음 정류장에서 내린다. (항상) → `missed_chance` | path_soc_04, path_soc_05 |
| path_soc_04 | 청년기 | MILESTONE | 전환점·직업 | 사람으로 들어온 일자리 | 사회성 82+, `soc_path` | A. 선배 회사 영업팀에 간다. (사회성 85+) → `career_sales` `job_employed` `achv_top_sales` `career_office` <sub>결과 2갈래</sub><br>B. 상담·복지 일을 배운다. (항상) → `career_counselor` `job_employed`<br>C. 친구와 같이 창업한다. (경제력 60+) → `career_founder` `startup_founder` `job_self_employed` `soc_friend_lost` `job_unemployed` <sub>결과 2갈래</sub> | path_soc_06 |
| path_soc_05 | 청년기·중년기 | INCIDENT | 결혼 후·상호작용(경제력) | 상견례 대참사 | `soc_path`, `married`, 제외 `divorced` | A. 그날 축구 영상을 찾아서 같이 본다. (항상) → `family_bond` `achv_family_peace` `family_memory` `family_conflict` <sub>결과 2갈래</sub><br>B. 신혼집 이야기로 화제를 돌린다. (항상) → `housing_jeonse` `housing_monthly_rent` <sub>결과 2갈래</sub> | path_soc_07 |
| path_soc_06 | 중년기 | MILESTONE | 전환점·네트워크 | 동네가 나를 부른다 | 사회성 88+, `soc_path` | A. 선거에 나간다. (사회성 90+) → `achv_community_leader` `volunteer` <sub>결과 2갈래</sub><br>B. 사람들을 이어 주는 모임을 만든다. (항상) → `achv_connector` `best_friend` | path_soc_08 |
| path_soc_07 | 중년기 | INCIDENT | 가족 위기(실패)·상호작용(행복·경제력) | 식탁 위의 침묵 | 사회성 60+, `married`, 제외 `divorced` | A. 이번 주말 둘만의 여행을 예약한다. (항상) → `family_bond` `achv_family_peace` `family_memory` `divorced` `soc_divorce` <sub>결과 2갈래</sub><br>B. 부부 상담을 받아 보자고 한다. (항상) → `family_talk` | path_soc_08 |
| path_soc_08 | 중년기·노년기 | INCIDENT | 이혼 후·실패에서 회복 | 혼자 차린 생일상 | `divorced` | A. 친구들을 불러 남은 미역국을 같이 먹는다. (항상) → `lifelong_friend` `achv_found_family`<br>B. 혼자 조용히 하루를 보낸다. (항상) → `single_life` | path_soc_09 |
| path_soc_09 | 노년기 | INCIDENT | 노년 결과 | 칠순 잔치의 방명록 | `soc_path` | A. 한 사람 한 사람에게 인사하러 돈다. (항상) → `achv_full_guestbook`<br>B. 가족 테이블에 앉아 손주들과 사진을 찍는다. (`married`) → `family_bond` `family_memory`<br>C. 마이크를 잡고 노래방 18번을 부른다. (항상) → - | - |

사회성 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_soc_leader | 97 | 동네가 기억하는 이름 | `achv_community_leader` |
| ending_path_soc_family | 96 | 큰 식탁의 중심 | `soc_path`, `married`, `family_bond`, 제외 `divorced` |
| ending_path_soc_connector | 95 | 사람과 사람을 이어 준 사람 | `soc_path`, `achv_connector` |
| ending_path_soc_after_divorce | 93 | 헤어진 뒤에도 사람을 잃지 않은 사람 | `soc_path`, `divorced` |

### 4.4 경제력 경로

흐름: 첫 알바 월급 → 중고 거래 사업 → 첫 직장 계약서 → 전셋집 계약 → 내 집 마련 → 학원비 → 가게 위기 → 회사 인수 제안 → 재산 정리

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_wea_01 | 청소년기 | MILESTONE | 입구·전환점 | 첫 월급 37만 원 | 경제력 56+ | A. 적금 통장을 만든다. (항상) → `wea_path` `saving_habit` `money_habit` `part_time_job`<br>B. 주식 계좌를 열고 첫 주식을 산다. (항상) → `wea_path` `wea_investor` `part_time_job` <sub>결과 2갈래</sub><br>C. 친구들에게 떡볶이를 쏜다. (항상) → `secret_payer` `part_time_job` | path_wea_02, path_wea_03 |
| path_wea_02 | 청소년기·청년기 | INCIDENT | 작은 사업·상호작용(사회성) | 중고 거래 사장님 | `wea_path` | A. 온라인 가게를 정식으로 연다. (항상) → `wea_seller` `achv_first_business` <sub>결과 2갈래</sub><br>B. 번 돈으로 학원비를 낸다. (항상) → `family_bond` `family_memory` | path_wea_03 |
| path_wea_03 | 청년기 | MILESTONE | 전환점·직업 | 첫 직장, 세 개의 계약서 | 경제력 62+ | A. 대기업에 입사한다. (항상) → `wea_path` `career_office` `corporate_job` `job_employed` `housing_monthly_rent`<br>B. 스타트업에 들어간다. (경제력 65+) → `wea_path` `career_startup` `job_employed` `achv_stock_option` `job_unemployed` <sub>결과 2갈래</sub><br>C. 내 가게를 연다. (`wea_seller`) → `wea_path` `career_founder` `startup_founder` `own_shop` `job_self_employed` `wea_struggle` <sub>결과 2갈래</sub> | path_wea_04, path_wea_06 |
| path_wea_04 | 청년기·중년기 | INCIDENT | 주거·상호작용(지력·운) | 전셋집 계약 전날 | `wea_path`, `job_employed` | A. 등기부등본부터 떼어 본다. (항상) → `housing_jeonse` <sub>결과 2갈래</sub><br>B. 좋은 집은 빨리 잡아야 한다. 계약금을 넣는다. (항상) → `housing_jeonse` `wea_jeonse_loss` `in_debt` <sub>결과 2갈래·숨김</sub><br>C. 부모님 집에 조금 더 산다. (항상) → `housing_with_parents` | path_wea_05 |
| path_wea_05 | 중년기 | MILESTONE | 전환점·주거 | 내 집 마련 상담 | 경제력 75+, `wea_path` | A. 대출을 끼고 아파트를 산다. (경제력 80+) → `home_owner` `housing_own_apartment` `in_debt` `achv_home`<br>B. 전세를 유지하고 남은 돈을 투자한다. (항상) → `wea_investor` `housing_jeonse` `wea_market_loss` <sub>결과 2갈래</sub><br>C. 도시를 떠나 마당 있는 집을 산다. (행복 70+) → `home_owner` `housing_own_house` | path_wea_06, path_wea_08 |
| path_wea_06 | 중년기 | INCIDENT | 가족 지출·상호작용(행복·사회성) | 학원비 영수증 | `wea_path`, `married`, 제외 `divorced` | A. 내 용돈을 줄이고 학원을 보낸다. (항상) → `first_child` `family_bond` `devoted_parent` `family_memory`<br>B. 학원 대신 주말에 같이 그림을 그린다. (항상) → `first_child` `family_bond` `family_memory` <sub>결과 2갈래</sub><br>C. 부업을 하나 더 시작한다. (항상) → `first_child` `overwork` | path_wea_08 |
| path_wea_07 | 청년기·중년기 | INCIDENT | 위기(실패) | 월세 날의 통장 잔고 | `wea_struggle` | A. 대출을 받아 버틴다. (항상) → `in_debt` `comeback` `wea_survived_crisis` `wea_bankrupt` `job_unemployed` <sub>결과 2갈래</sub><br>B. 가게를 접고 다시 회사에 들어간다. (항상) → `career_office` `job_employed` `comeback` | path_wea_08 |
| path_wea_08 | 중년기 | INCIDENT | 중년 성과 | 회사를 사겠다는 전화 | `career_founder`, 제외 `wea_bankrupt` | A. 회사를 판다. (항상) → `achv_exit` `job_retired` `debt_free`<br>B. 거절하고 회사를 더 키운다. (항상) → `achv_company_100` `level_executive` <sub>결과 2갈래</sub> | path_wea_09 |
| path_wea_09 | 노년기 | INCIDENT | 노년 결과 | 통장 정리하는 날 | `wea_path` | A. 장학 재단에 일부를 기부한다. (`achv_exit`) → `achv_donation`<br>B. 남은 빚부터 정리한다. (`in_debt`) → `debt_free`<br>C. 가족에게 나누고 남은 돈으로 여행을 간다. (항상) → `family_bond` `family_memory` | - |

경제력 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_wea_exit | 97 | 작은 가게를 큰 회사로 키운 창업가 | `achv_exit` |
| ending_path_wea_ceo | 96 | 직원 백 명의 대표 | `achv_company_100` |
| ending_path_wea_home | 95 | 대출을 다 갚은 날 | `wea_path`, `achv_home`, `debt_free` |
| ending_path_wea_stock | 94 | 스톡옵션 숫자에 0을 붙인 사람 | `wea_path`, `achv_stock_option` |
| ending_path_wea_debt | 92 | 빚과 함께 늙은 사장님 | `wea_path`, `in_debt`, 제외 `debt_free` |
| ending_path_wea_saver | 91 | 적금 통장 하나로 시작한 자산가 | `wea_path`, `money_habit`, 경제력 85+ |

### 4.5 행복 경로

흐름: 주말 취미 → 진로 갈등 → 연봉이냐 저녁이냐 → 번아웃 → 취미 모임의 인연 → 첫 개인 공연 → 통장과 웃음 → 복지관 선생님

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_hap_01 | 유년기 | INCIDENT | 입구 | 주말의 비밀 취미 | 행복 74+ | A. 없는 줄 하나 빼고 다섯 줄로 연습한다. (항상) → `hap_path` `creative_hobby` `music_path`<br>B. 기타 대신 그림을 그린다. (항상) → `hap_path` `creative_hobby` `art_interest`<br>C. 창고를 다시 닫고 게임을 한다. (항상) → `gamer` | path_hap_02 |
| path_hap_02 | 청소년기 | INCIDENT | 진로 갈등·상호작용(지력) | 좋아하는 것과 잘하는 것 | `hap_path` | A. 좋아하는 걸 전공하겠다고 말한다. (항상) → `hap_art_major` `arts_track` `family_conflict` <sub>결과 2갈래</sub><br>B. 성적에 맞춰 가고 취미는 남겨 둔다. (항상) → `hap_hobby_side` | path_hap_03 |
| path_hap_03 | 청년기 | MILESTONE | 전환점·직업 | 연봉이냐 저녁이냐 | `hap_path` | A. 6시 퇴근 회사에 간다. (항상) → `work_life_balance` `career_office` `job_employed`<br>B. 연봉 높은 회사에 간다. (경제력 55+) → `career_office` `job_employed` `overwork`<br>C. 친구와 작업실을 연다. (`hap_art_major`) → `career_creator` `job_self_employed` `achv_first_commission` `overwork` <sub>결과 2갈래</sub> | path_hap_04, path_hap_05 |
| path_hap_04 | 청년기·중년기 | INCIDENT | 번아웃(실패)·상호작용(건강) | 아무것도 재미없는 일요일 | `hap_path`, `overwork` | A. 휴직계를 낸다. (항상) → `burnout_recovered` `hap_rest` `burnout` <sub>결과 2갈래</sub><br>B. 상담을 받아 본다. (항상) → `burnout_recovered`<br>C. 그냥 버틴다. (항상) → `burnout` `hap_lost_joy` `health_warning` <sub>결과 2갈래·숨김</sub> | path_hap_06 |
| path_hap_05 | 청년기·중년기 | INCIDENT | 관계 만족·상호작용(사회성) | 취미 모임에서 만난 사람 | `hap_path`, 제외 `married` | A. 다음 주에도 간다. (항상) → `married` `achv_band_wedding` `lifelong_friend` <sub>결과 2갈래</sub><br>B. 혼자 하는 취미가 더 편하다. (항상) → `single_life` `solo_musician` | path_hap_06 |
| path_hap_06 | 중년기 | MILESTONE | 전환점·중년 성과 | 첫 개인 공연 제안 | `hap_path`, 제외 `hap_lost_joy` | A. 공연을 연다. (행복 80+) → `achv_solo_show` <sub>결과 2갈래</sub><br>B. 가족 앞에서만 작게 연다. (항상) → `family_bond` `content_life` `family_memory` | path_hap_07, path_hap_08 |
| path_hap_07 | 중년기 | INCIDENT | 행복의 대가·상호작용(경제력) | 통장과 웃음 사이 | `hap_path` | A. 취미로 돈을 버는 방법을 찾는다. (항상) → `achv_hobby_income` `content_life` <sub>결과 2갈래</sub><br>B. 지금 행복한 게 노후다. (항상) → `content_life` | path_hap_08 |
| path_hap_08 | 노년기 | INCIDENT | 노년 결과 | 문화센터 할머니 할아버지 선생님 | `hap_path` | A. 첫 곡으로 다 같이 아는 동요를 고른다. (항상) → `achv_senior_teacher`<br>B. 손주에게 먼저 가르쳐 본다. (`first_child`) → `family_bond` `family_memory`<br>C. 번아웃 때 쓴 일기를 꺼내 읽는다. (`burnout`) → `burnout_recovered` | - |

행복 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_hap_artist | 96 | 쉰 석짜리 무대의 주인공 | `hap_path`, `achv_solo_show` |
| ending_path_hap_teacher | 95 | 복지관 기타 선생님 | `hap_path`, `achv_senior_teacher` |
| ending_path_hap_content | 94 | 천천히 오래 웃은 사람 | `hap_path`, `content_life`, 제외 `hap_lost_joy` |
| ending_path_hap_burnout | 93 | 웃는 법을 잊었던 사람 | `hap_path`, `hap_lost_joy` |

### 4.6 운 경로

흐름: 뽑기 3연속 → 추첨 해외 연수 → 가짜 당첨 문자 → 엘리베이터 30초 → 놓친 비행기 → 주운 지갑 → 동창의 투자 제안 → 운으로 산 인생

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_luk_01 | 유년기 | INCIDENT | 입구 | 문구점 뽑기 3연속 1등 | 운 59+ | A. 상품 하나를 뒤에 선 아이에게 준다. (항상) → `luk_path` `sharing_habit`<br>B. 한 번 더 뽑는다. (항상) → `luk_path` `achv_lucky_streak` <sub>결과 2갈래</sub> | path_luk_02 |
| path_luk_02 | 청소년기 | INCIDENT | 뜻밖의 기회 | 추첨으로 뽑힌 해외 연수 | `luk_path` | A. 여권을 만들고 연수를 간다. (항상) → `luk_exchange` `achv_exchange`<br>B. 꼭 가고 싶어 하던 친구에게 양보한다. (항상) → `helped_friend` | path_luk_03 |
| path_luk_03 | 청소년기·청년기 | INCIDENT | 위기 회피·상호작용(지력) | 당첨 축하 문자 | `luk_path` | A. 링크를 누른다. (항상) → `achv_lucky_streak` `luk_scammed` <sub>결과 2갈래</sub><br>B. 번호를 검색해 본다. (항상) → `luk_avoided` `special_survived` | path_luk_04 |
| path_luk_04 | 청년기 | MILESTONE | 전환점·직업 | 엘리베이터 안의 30초 | 운 64+, `luk_path` | A. 동네 이야기로 30초를 꽉 채운다. (운 66+) → `career_office` `job_employed` `luk_lucky_hire` `luk_missed` <sub>결과 2갈래</sub><br>B. 조용히 면접 답변을 외운다. (항상) → `career_office` `job_employed`<br>C. 면접 대신 동전 던지기로 진로를 정한다. (항상) → `career_freelancer` `job_self_employed` `luk_wanderer` | path_luk_05, path_luk_06 |
| path_luk_05 | 중년기 | MILESTONE | 전환점·뜻밖의 성공 또는 손실·상호작용(경제력) | 동창의 투자 제안 | 운 72+, `luk_path` | A. 모아 둔 돈을 넣는다. (경제력 35+) → `luk_windfall` `achv_windfall` `luk_loss` `in_debt` <sub>결과 2갈래</sub><br>B. 조금만 넣는다. (항상) → `luk_small_win` `luk_small_loss` <sub>결과 2갈래</sub><br>C. 술값만 내고 먼저 일어난다. (항상) → `luk_avoided` `special_survived` | path_luk_06, path_luk_07 |
| path_luk_06 | 청년기·중년기 | INCIDENT | 뜻밖의 인연 | 주운 지갑의 주인 | `luk_path` | A. 바로 근처 파출소에 맡긴다. (항상) → `luk_patron` `achv_good_deed` <sub>결과 2갈래</sub><br>B. 직접 회사로 찾아간다. (항상) → - | path_luk_07 |
| path_luk_08 | 청년기·중년기 | INCIDENT | 위기 회피·상호작용(건강) | 놓친 비행기 | `luk_path` | A. 공항 근처에서 하룻밤 자고 내일 간다. (항상) → `luk_airport_contact` <sub>결과 2갈래</sub><br>B. 다른 항공사 표를 비싸게 산다. (항상) → `luk_airport_contact` `luk_doubt` <sub>결과 2갈래</sub><br>C. 출장을 취소하고 집에 간다. (항상) → `luk_avoided` `special_survived` | path_luk_05 |
| path_luk_07 | 노년기 | INCIDENT | 노년 결과 | 운으로 산 인생 | `luk_path` | A. "운이 좋았지. 근데 그 운을 줍는 법을 배웠어." (`luk_windfall`) → `achv_lucky_life`<br>B. "운이 안 좋을 때가 더 많았어. 그래도 여기까지 왔지." (`luk_loss`) → `luk_made_peace`<br>C. "운은 몰라도 사람은 좋았지." (항상) → `family_bond` `family_memory` | - |

운 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_luk_jackpot | 97 | 될 사람은 됐다 | `luk_windfall` |
| ending_path_luk_patron | 95 | 지갑 하나로 바뀐 인생 | `luk_patron` |
| ending_path_luk_wanderer | 94 | 동전 한 닢으로 떠난 사람 | `luk_wanderer` |
| ending_path_luk_loss | 93 | 운이 등을 돌렸던 날들 | `luk_path`, `luk_loss` |
| ending_path_luk_small_win | 92 | 적당히 운 좋았던 사람 | `luk_path`, `luk_small_win` |
| ending_path_luk_dodger | 91 | 아슬아슬 피해 간 인생 | `luk_path`, `luk_avoided` |

### 4.7 건강 경로

흐름: 급식 브로콜리 → 새벽 2시 → 첫 종합검진 → 등산 모임 대장 → 진단서 → 건강 나이 측정 / 계단 앞에서 → 병원 대기실

| ID | 시기 | 종류 | 역할 | 제목 | 등장 조건 | 선택지 (조건) → 결과로 붙는 사실 | 후속 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| path_hea_01 | 유년기 | INCIDENT | 입구 | 급식 시간의 브로콜리 | 없음 (선택으로 입장) | A. 다섯 송이를 다 먹는다. (항상) → `hea_path` `good_eater`<br>B. 짝꿍 것만 돌려준다. (항상) → -<br>C. 브로콜리로 나무를 만들어 급식판 숲을 꾸민다. (항상) → `storyteller` | path_hea_02 |
| path_hea_02 | 청소년기 | INCIDENT | 입구·습관·상호작용(지력) | 새벽 2시의 책상 | 없음 (선택으로 입장) | A. 불을 끄고 잔다. (항상) → `hea_path` `healthy_routine` `hea_sleep_habit`<br>B. 에너지 음료를 따고 밤을 새운다. (항상) → `hea_sleep_debt` `health_warning` <sub>결과 2갈래·숨김</sub> | path_hea_03 |
| path_hea_03 | 청년기·중년기 | INCIDENT | 검진 | 첫 종합검진 결과지 | `hea_path` | A. 그날부터 매일 30분 걷는다. (항상) → `healthy_routine` `hea_managed`<br>B. 경계면 아직 괜찮은 거다. (항상) → `health_warning` <sub>결과 2갈래</sub> | path_hea_04, path_hea_05 |
| path_hea_04 | 청년기·중년기 | INCIDENT | 관리 성과·상호작용(운동능력) | 주말 등산 모임 대장 | `hea_managed` | A. 대장을 맡는다. (항상) → `achv_hundred_peaks` `healthy_routine` `health_injured` <sub>결과 2갈래</sub><br>B. 혼자 걷는 게 더 좋다. (항상) → `healthy_routine` | path_hea_06 |
| path_hea_05 | 중년기 | MILESTONE | 전환점·질병 | 진단서 한 장 | `health_warning` | A. 일을 줄이고 생활을 다 바꾼다. (항상) → `hea_recovered` `healthy_routine` `health_recovered` `hea_chronic` `health_chronic` <sub>결과 2갈래</sub><br>B. 전문 재활 프로그램에 등록한다. (경제력 65+) → `hea_recovered` `health_recovered` `achv_full_recovery`<br>C. 지금 일이 더 급하다. 약만 먹는다. (항상) → `hea_chronic` `health_chronic` `overwork` | path_hea_06, path_hea_07 |
| path_hea_06 | 중년기·노년기 | INCIDENT | 건강 성과 | 건강 나이 측정 | 건강 70+, `healthy_routine`, 제외 `hea_chronic` | A. 결과지를 액자에 넣는다. (항상) → `achv_young_body`<br>B. 보건소 걷기 교실 강사를 자원한다. (항상) → `achv_young_body` `volunteer` `helped_friend` | path_hea_08 |
| path_hea_07 | 중년기·노년기 | INCIDENT | 활동 제한(실패)·상호작용(사회성) | 계단 앞에서 | `hea_chronic` | A. 솔직하게 말하고 둘레길 코스로 바꾸자고 한다. (항상) → `health_limited` `lifelong_friend` <sub>결과 2갈래</sub><br>B. 무리해서 따라간다. (항상) → `health_limited` `health_injured` `health_warning` <sub>결과 2갈래·숨김</sub> | path_hea_08 |
| path_hea_08 | 노년기 | INCIDENT | 수명 결과 | 병원 대기실의 단골 | `hea_path` | A. 검사 끝나고 대기실 사람들과 국밥을 먹으러 간다. (항상) → `achv_clinic_friends` `health_chronic` `fatal_risk` <sub>결과 3갈래·숨김·사망</sub><br>B. 오늘도 걸어서 집에 간다. (제외 `health_limited`) → `achv_walk_home` | - |

건강 경로 결말:

| 결말 ID | 우선순위 | 제목 | 조건 |
| --- | ---: | --- | --- |
| ending_path_hea_peaks | 96 | 백 개의 정상을 밟은 사람 | `achv_hundred_peaks` |
| ending_path_hea_young | 95 | 열두 살 젊은 몸으로 산 사람 | `hea_path`, `achv_young_body` |
| ending_path_hea_recovered | 94 | 아프고 나서 더 단단해진 사람 | `hea_recovered` |
| ending_path_hea_chronic | 93 | 몸과 타협하며 산 사람 | `hea_chronic` |

## 5. 결과로 붙는 사실 — `life_facts.sample.json`

선택 결과는 플래그로 저장하고, 플래그가 어떤 인생 사실인지는 `life_facts.sample.json`에 적었다. 항목은 `{flag, fact, value, label}`이다. 우영 담당 상태 모델의 필드 이름(`educationLevel`, `careerTrack`, `maritalStatus`, `housingType` …)에 맞췄다. 기존 사건의 플래그(`university`, `married`, `home_owner` 등)도 함께 넣어 두어 예전 사건으로 쌓인 사실도 결말 화면에 나올 수 있다. 결과가 적용될 때 붙은 순서대로 사실을 덮어쓰면 마지막 상태가 남는다.

| 사실 | 플래그 수 | 예시 |
| --- | ---: | --- |
| 교육 (`educationLevel`) | 4 | `university`=대학 진학, `edu_graduate`=대학원 진학, `vocational_high`=특성화고, `arts_high`=예술고 |
| 전공 (`fieldOfStudy`) | 3 | `field_science`=자연과학 전공, `field_sports`=체육 전공, `arts_track`=예술 전공 |
| 직업 (`careerTrack`) | 20 | `career_researcher`=연구원, `career_engineer`=엔지니어, `career_patent_attorney`=변리사, `career_professor`=강단에 선 연구자 |
| 직급 (`careerLevel`) | 3 | `level_mid`=중간 관리자, `level_senior`=책임자, `level_executive`=임원·대표 |
| 고용 상태 (`employmentStatus`) | 7 | `job_employed`=재직, `job_self_employed`=자영업, `job_unemployed`=실직, `job_retired`=은퇴 |
| 결혼 (`maritalStatus`) | 4 | `dating`=연애 중, `married`=결혼, `divorced`=이혼, `single_life`=혼자의 삶 |
| 자녀 (`childrenAtLeast`) | 2 | `first_child`=자녀, `v4:event:adult_039`=첫 아이 |
| 주거 (`housingType`) | 6 | `housing_with_parents`=부모님 집, `housing_monthly_rent`=월세, `housing_jeonse`=전세, `housing_own_apartment`=내 집(아파트) |
| 경제 상태 (`wealthNote`) | 3 | `in_debt`=빚 있음, `debt_free`=빚 청산, `student_loan`=학자금 대출 |
| 건강 상태 (`healthStatus`) | 6 | `health_injured`=부상, `health_limited`=활동 제한, `health_chronic`=만성 질환 관리, `health_recovered`=회복 |
| 업적 (`achievement`) | 49 | `achv_band_wedding`=합주 모임 결혼식, `achv_breakthrough`=연구 돌파구, `achv_class_president`=반장 당선, `achv_clinic_friends`=대기실 친구들 |

## 6. 실제 엔진으로 돌려 본 결과

방법은 이렇다. 현재 브랜치의 `V4ContentParser`와 `GameEngine`을 앱과 같은 설정(`skipInfantFocus`, `guaranteeYearlyIncident`)으로 썼다. 임시 테스트로 스탯마다 300판씩 돌렸다. 이 테스트는 커밋하지 않았다.

- 집중 플레이: 시기마다 그 스탯을 가장 많이 올리는 초점과 선택지를 고른다. 열린 선택지만 고를 수 있다.
- 성별: 절반은 남성 시작이다.
- 결말 판정: 엔진의 `resolveEndingDecision`과 같은 규칙이다(우선순위 높은 순, 없으면 기본 결말).

| 집중 스탯 | 이번 주 전 1위 결말 | 이번 주 후 1위 결말 | 경로 사건 (중앙값) | 경로 결말 도달 | 다른 스탯 집중 판의 경로 결말 |
| --- | --- | --- | ---: | ---: | ---: |
| 지력 | 조용한 학자 80% | 질문 하나로 평생을 산 연구자 56% | 6 / 10 | 57% | 1% |
| 운동능력 | 평범한 인생 74% | 누군가의 첫 메달을 만든 지도자 49% | 7 / 9 | 85% | 1% |
| 사회성 | 평범한 인생 90% | 큰 식탁의 중심 49% | 7 / 9 | 84% | 1% |
| 경제력 | 무난한 어른 42% | 대출을 다 갚은 날 45% | 5 / 9 | 89% | 0% |
| 행복 | 평범한 인생 98% | 쉰 석짜리 무대의 주인공 84% | 5 / 8 | 84% | 0% |
| 운 | 조용한 학자 54% | 지갑 하나로 바뀐 인생 21% | 6 / 8 | 61% | 0% |
| 건강 | 평범한 인생 72% | 열두 살 젊은 몸으로 산 사람 38% | 5 / 8 | 75% | 9% |

무작위로 고른 300판에서 나온 결말 종류: 31개 (이번 주 전 10개)

읽는 법: 한 스탯에 집중하면 그 경로 사건을 중앙값 5~7개 거치고 그 경로 결말로 끝난다. 다른 스탯에 집중하면 그 경로 사건을 거의 만나지 않는다. 건강만 다른 판에서도 조금 나오는데, 건강 경로는 수치가 아니라 "건강한 선택"으로 들어오기 때문이다(3장 2번).

## 7. 시연 경로 (시뮬레이션에서 실제로 그 결말에 도달한 판)

앱의 시드는 매번 달라서 사건이 똑같은 순서로 나오지는 않는다. 그 스탯을 올리는 초점을 고르고 아래 선택을 따라가면 같은 경로 사건이 열린다. (그 경로 사건과 고정 정지 칸만 적었다. 실제 플레이에서는 사이사이 일반 사건이 섞인다.)

**지력 집중** — 엔진 시드 8002, 결말 「질문 하나로 평생을 산 연구자」

영아기 돌잡이 → 판사봉을 잡는다. → 유년기 학원 고르기 → 피아노. → 유년기 교내 수학 경시대회 → 대회에 나간다. → 청소년기 영재교육원 선발 시험 → 추천을 받아 시험을 본다. → 청소년기 고등학교 원서 → 인문계. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 대학. → 청년기 입영 통지서 → 하던 공부와 일을 마무리하고 1년 뒤에 간다. → 청년기 졸업 앞의 세 갈래 → 대학원에 진학한다. → 청년기 결혼 이야기 → "같이 파견 가자." → 중년기 평생 연구의 마지막 퍼즐 → 연구를 끝까지 밀어붙인다. → 중년기 쉰 번째 생일 → 소원: "앞으로 50년 더!" → 노년기 마지막 강의 → 실패한 이야기만 골라서 한다.

**운동능력 집중** — 엔진 시드 8037, 결말 「누군가의 첫 메달을 만든 지도자」

영아기 돌잡이 → 5만 원권을 잡는다. → 유년기 학원 고르기 → 태권도. → 유년기 체육관 관장님의 명함 → 육상부에 들어간다. → 유년기 첫 대회 결승선 → 끈을 묶고 다시 자세를 잡는다. → 청소년기 체육 특기생 제안 → 체육 특기생으로 간다. → 청소년기 고등학교 원서 → 예술고 실기 시험에 도전한다. → 청소년기 전국체전 결승, 무릎의 소리 → 테이핑하고 결승에 뛴다. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 1년 쉬면서 생각해 본다. → 청년기 프로 입단 테스트 → 프로 테스트를 본다. → 청년기 주전 경쟁 → 훈련량을 두 배로 늘린다. → 청년기 결혼 이야기 → "같이 파견 가자." → 청년기 세 번째 수술 상담 → 수술하고 복귀를 노린다. → 중년기 내 이름을 건 체육관 → 후배들을 길러 내는 지도자로 남는다. → 중년기 쉰 번째 생일 → 소원: "앞으로 50년 더!" → 노년기 시니어 마라톤 출발선 → 풀코스에 도전한다.

**사회성 집중** — 엔진 시드 8299, 결말 「큰 식탁의 중심」

영아기 돌잡이 → 5만 원권을 잡는다. → 유년기 학원 고르기 → "나 그냥 놀래." → 유년기 반장 선거 연설 → 친구들 이름을 하나씩 부르며 즉석 연설을 한다. → 청소년기 축제 준비위원회의 밤 → 양쪽을 모아 밤새 예산표를 다시 짠다. → 청소년기 고등학교 원서 → 예술고 실기 시험에 도전한다. → 청소년기 집에 가는 길이 같은 사람 → "같이 내려서 걸을래?"라고 묻는다. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 1년 쉬면서 생각해 본다. → 청년기 사람으로 들어온 일자리 → 상담·복지 일을 배운다. → 청년기 결혼 이야기 → 결혼한다. → 청년기 상견례 대참사 → 그날 축구 영상을 찾아서 같이 본다. → 중년기 동네가 나를 부른다 → 사람들을 이어 주는 모임을 만든다. → 중년기 쉰 번째 생일 → 가족과 사진을 찍는다. → 노년기 칠순 잔치의 방명록 → 한 사람 한 사람에게 인사하러 돈다.

**경제력 집중** — 엔진 시드 8107, 결말 「대출을 다 갚은 날」

영아기 돌잡이 → 판사봉을 잡는다. → 유년기 학원 고르기 → "나 그냥 놀래." → 청소년기 첫 월급 37만 원 → 주식 계좌를 열고 첫 주식을 산다. → 청소년기 고등학교 원서 → 특성화고. → 청소년기 졸업식 날 → 친구들한테 간다. → 청년기 대학이냐 취업이냐 → 취업. → 청년기 첫 직장, 세 개의 계약서 → 스타트업에 들어간다. → 청년기 결혼 이야기 → 파견부터 다녀온다. → 청년기 전셋집 계약 전날 → 부모님 집에 조금 더 산다. → 중년기 내 집 마련 상담 → 대출을 끼고 아파트를 산다. → 중년기 쉰 번째 생일 → 소원: "앞으로 50년 더!" → 노년기 통장 정리하는 날 → 남은 빚부터 정리한다.

**행복 집중** — 엔진 시드 8018, 결말 「쉰 석짜리 무대의 주인공」

영아기 돌잡이 → 피아노 장난감을 잡는다. → 유년기 학원 고르기 → "나 그냥 놀래." → 유년기 주말의 비밀 취미 → 기타 대신 그림을 그린다. → 청소년기 고등학교 원서 → 예술고 실기 시험에 도전한다. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 1년 쉬면서 생각해 본다. → 청년기 입영 통지서 → 입대 전까지 송별회를 일곱 번 한다. → 청년기 결혼 이야기 → 결혼한다. → 청년기 연봉이냐 저녁이냐 → 6시 퇴근 회사에 간다. → 중년기 첫 개인 공연 제안 → 공연을 연다. → 중년기 쉰 번째 생일 → 가족과 사진을 찍는다. → 노년기 문화센터 할머니 할아버지 선생님 → 첫 곡으로 다 같이 아는 동요를 고른다.

**운 집중** — 엔진 시드 8235, 결말 「지갑 하나로 바뀐 인생」

영아기 돌잡이 → 5만 원권을 잡는다. → 유년기 학원 고르기 → 피아노. → 유년기 문구점 뽑기 3연속 1등 → 한 번 더 뽑는다. → 청소년기 당첨 축하 문자 → 링크를 누른다. → 청소년기 고등학교 원서 → 인문계. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 1년 쉬면서 생각해 본다. → 청년기 엘리베이터 안의 30초 → 동네 이야기로 30초를 꽉 채운다. → 청년기 놓친 비행기 → 출장을 취소하고 집에 간다. → 청년기 결혼 이야기 → "같이 파견 가자." → 청년기 주운 지갑의 주인 → 바로 근처 파출소에 맡긴다. → 중년기 동창의 투자 제안 → 술값만 내고 먼저 일어난다. → 중년기 쉰 번째 생일 → "앞으로 하고 싶은 것 50개"를 적는다. → 노년기 운으로 산 인생 → "운은 몰라도 사람은 좋았지."

**건강 집중** — 엔진 시드 8099, 결말 「백 개의 정상을 밟은 사람」

영아기 돌잡이 → 판사봉을 잡는다. → 유년기 학원 고르기 → "나 그냥 놀래." → 유년기 급식 시간의 브로콜리 → 다섯 송이를 다 먹는다. → 청소년기 새벽 2시의 책상 → 불을 끄고 잔다. → 청소년기 고등학교 원서 → 예술고 실기 시험에 도전한다. → 청소년기 졸업식 날 → 친구들을 전부 고깃집에 데려간다. → 청년기 대학이냐 취업이냐 → 1년 쉬면서 생각해 본다. → 청년기 첫 종합검진 결과지 → 그날부터 매일 30분 걷는다. → 청년기 결혼 이야기 → "같이 파견 가자." → 중년기 건강 나이 측정 → 결과지를 액자에 넣는다. → 중년기 주말 등산 모임 대장 → 대장을 맡는다. → 중년기 쉰 번째 생일 → 소원: "앞으로 50년 더!" → 노년기 병원 대기실의 단골 → 오늘도 걸어서 집에 간다.

## 8. 다른 담당자 확인 필요

| 담당 | 내용 |
| --- | --- |
| 김우영 (엔진) | ① `outcomes[].removeFlags` 처리. 결혼할 때 `dating`, 이혼할 때 `married`를 지우는 데 쓴다. 조건은 지우지 않아도 되게 짜 두었다. ② `life_facts.sample.json`을 읽어 결과가 적용될 때 사실을 누적한다(제안). ③ 기존 결과의 `lifeDelta: +10` 때문에 건강이 거의 항상 100이라, 건강 집중과 다른 집중이 수치로 구분되지 않는다. ④ 경로 사건의 `lifePath`와 현재 초점의 주 스탯이 같을 때 가중치를 올리면 경로가 더 잘 이어진다(선택) |
| 김선한 (DB·QA) | ① `scripts/validate_content.py`의 시기 목록에 `OLD_AGE`가 없어서 오류 1건이 난다(이번 PR 전부터 있던 문제). 노년기 초점 쪽 오류 15건은 이번 PR에서 채웠다. ② 새 선택지 154개에 `conditionReview`·`conditionBasis`(`week8_life_path`)를 달았으니 `audit_choice_conditions.py`를 다시 돌려 보고서를 갱신해야 한다. ③ 결말 우선순위: 경로 결말을 91~97로 두어 기존 일반 결말(90 이하)보다 먼저 나온다 |
| 김재겸 (프론트) | 선택지 잠금 이유는 엔진이 주는 `lockReason`을 쓰면 된다. 결말 화면의 교육·직업·가족·주거·업적은 `life_facts.sample.json`의 `label`로 보여 줄 수 있다 |
| 정원률 (이미지) | 경로 사건 61개마다 1인칭 이미지 ID를 등록했다(`img_path_*_001`, usage `pov`). 장면 설명은 `image_ids.sample.json`의 description과 아래 9장에 있다. 노년기 초점 이미지 10개도 등록했다 |

## 9. 이미지 요청 목록 (경로 사건 1인칭)

| imageId | 경로 | 시기 | 사건 | 장면 |
| --- | --- | --- | --- | --- |
| `img_path_int_01_001` | 지력 | 유년기 | 교내 수학 경시대회 | 교실 게시판의 경시대회 공고와 생일 초대장을 번갈아 보는 1인칭 |
| `img_path_int_02_001` | 지력 | 청소년기 | 영재교육원 선발 시험 | 복도 게시판의 선발 공문을 올려다보는 1인칭 |
| `img_path_int_03_001` | 지력 | 청소년기 | 수능 D-30 | 문제집 탑과 달력의 D-30 표시가 보이는 책상 1인칭 |
| `img_path_int_04_001` | 지력 | 청년기 | 3.98과 장학금 커트라인 | 성적 조회 화면 3.98과 연구실 공지를 함께 보는 1인칭 |
| `img_path_int_05_001` | 지력 | 청년기 | 쉬어 가는 1년, 다시 수능 | 독서실 책상 위 문제집과 코딩 책을 내려다보는 1인칭 |
| `img_path_int_06_001` | 지력 | 청년기·중년기 | 퇴근 후 야간대학 | 퇴근길 회사 앞 야간대학 현수막을 올려다보는 1인칭 |
| `img_path_int_07_001` | 지력 | 청년기 | 졸업 앞의 세 갈래 | 노트북 화면의 합격 메일과 시험 접수 페이지를 보는 1인칭 |
| `img_path_int_08_001` | 지력 | 중년기 | 마감 D-1과 가족 모임 | 모니터의 미완성 보고서와 휴대폰 단톡 알림을 함께 보는 1인칭 |
| `img_path_int_09_001` | 지력 | 중년기 | 평생 연구의 마지막 퍼즐 | 화이트보드 가득한 수식 앞 1인칭 |
| `img_path_int_10_001` | 지력 | 노년기 | 마지막 강의 | 도서관 강연대에서 객석을 바라보는 1인칭 |
| `img_path_fit_01_001` | 운동능력 | 유년기 | 체육관 관장님의 명함 | 운동회 결승선 옆에서 명함을 받아 드는 1인칭 |
| `img_path_fit_02_001` | 운동능력 | 유년기·청소년기 | 첫 대회 결승선 | 출발선에 엎드린 채 풀린 신발 끈을 내려다보는 1인칭 |
| `img_path_fit_03_001` | 운동능력 | 청소년기 | 체육 특기생 제안 | 교무실에서 감독님 명함과 성적표를 번갈아 보는 1인칭 |
| `img_path_fit_04_001` | 운동능력 | 청소년기 | 전국체전 결승, 무릎의 소리 | 경기장 트랙 위에서 자신의 무릎을 내려다보는 1인칭 |
| `img_path_fit_05_001` | 운동능력 | 청년기 | 프로 입단 테스트 | 라커룸 벤치 위에 통지서와 합격 문자가 뜬 휴대폰을 놓고 보는 1인칭 |
| `img_path_fit_06_001` | 운동능력 | 청년기·중년기 | 주전 경쟁 | 훈련장 전광판의 명단을 올려다보는 1인칭 |
| `img_path_fit_07_001` | 운동능력 | 청년기·중년기 | 세 번째 수술 상담 | 진료실 엑스레이 사진 앞 1인칭 |
| `img_path_fit_08_001` | 운동능력 | 중년기 | 내 이름을 건 체육관 | 빈 체육관 바닥에 서서 거울 벽을 보는 1인칭 |
| `img_path_fit_09_001` | 운동능력 | 노년기 | 시니어 마라톤 출발선 | 시니어 마라톤 출발선에서 운동화 끝을 내려다보는 1인칭 |
| `img_path_soc_01_001` | 사회성 | 유년기 | 반장 선거 연설 | 교탁 앞에 서서 반 친구들을 바라보는 1인칭 |
| `img_path_soc_02_001` | 사회성 | 청소년기 | 축제 준비위원회의 밤 | 어두운 교실에서 두 무리 사이에 선 1인칭 |
| `img_path_soc_03_001` | 사회성 | 청소년기·청년기 | 집에 가는 길이 같은 사람 | 저녁 버스 창가에 비친 두 사람의 그림자를 보는 1인칭 |
| `img_path_soc_04_001` | 사회성 | 청년기 | 사람으로 들어온 일자리 | 휴대폰에 온 세 개의 메시지를 나란히 보는 1인칭 |
| `img_path_soc_05_001` | 사회성 | 청년기·중년기 | 상견례 대참사 | 한정식 상 위로 오가는 두 아버지의 손짓을 보는 1인칭 |
| `img_path_soc_06_001` | 사회성 | 중년기 | 동네가 나를 부른다 | 꽉 찬 연락처 목록을 스크롤하는 1인칭 |
| `img_path_soc_07_001` | 사회성 | 중년기 | 식탁 위의 침묵 | 식탁 맞은편 빈 의자와 식은 찌개를 보는 1인칭 |
| `img_path_soc_08_001` | 사회성 | 중년기·노년기 | 혼자 차린 생일상 | 식탁 위 미역국 두 그릇과 울리는 휴대폰을 보는 1인칭 |
| `img_path_soc_09_001` | 사회성 | 노년기 | 칠순 잔치의 방명록 | 빼곡한 방명록 페이지를 넘기는 1인칭 |
| `img_path_wea_01_001` | 경제력 | 청소년기 | 첫 월급 37만 원 | 은행 앱 잔액 37만 원을 보는 1인칭 |
| `img_path_wea_02_001` | 경제력 | 청소년기·청년기 | 중고 거래 사장님 | 방구석에 쌓인 택배 박스와 거래 앱 알림을 보는 1인칭 |
| `img_path_wea_03_001` | 경제력 | 청년기 | 첫 직장, 세 개의 계약서 | 책상 위 계약서 세 장과 볼펜을 내려다보는 1인칭 |
| `img_path_wea_04_001` | 경제력 | 청년기·중년기 | 전셋집 계약 전날 | 빈 거실 창밖을 보며 계약서를 쥔 1인칭 |
| `img_path_wea_05_001` | 경제력 | 중년기 | 내 집 마련 상담 | 은행 창구 너머 대출 상담 서류를 보는 1인칭 |
| `img_path_wea_06_001` | 경제력 | 중년기 | 학원비 영수증 | 식탁 위 카드 명세서와 아이 그림을 함께 보는 1인칭 |
| `img_path_wea_07_001` | 경제력 | 청년기·중년기 | 월세 날의 통장 잔고 | 텅 빈 가게 계산대와 통장 잔고 화면을 보는 1인칭 |
| `img_path_wea_08_001` | 경제력 | 중년기 | 회사를 사겠다는 전화 | 사장실 창밖으로 일하는 직원들을 내려다보는 1인칭 |
| `img_path_wea_09_001` | 경제력 | 노년기 | 통장 정리하는 날 | 변호사 책상 위 재산 목록 서류를 내려다보는 1인칭 |
| `img_path_hap_01_001` | 행복 | 유년기 | 주말의 비밀 취미 | 창고 먼지 속에서 낡은 기타를 꺼내 드는 1인칭 |
| `img_path_hap_02_001` | 행복 | 청소년기 | 좋아하는 것과 잘하는 것 | 진로 상담지 위에 낙서가 가득한 노트를 겹쳐 보는 1인칭 |
| `img_path_hap_03_001` | 행복 | 청년기 | 연봉이냐 저녁이냐 | 저녁 6시와 밤 11시를 가리키는 두 시계를 보는 1인칭 |
| `img_path_hap_04_001` | 행복 | 청년기·중년기 | 아무것도 재미없는 일요일 | 소파에 누워 천장을 올려다보는 1인칭 |
| `img_path_hap_05_001` | 행복 | 청년기·중년기 | 취미 모임에서 만난 사람 | 합주실 앰프 너머로 웃는 사람을 보는 1인칭 |
| `img_path_hap_06_001` | 행복 | 중년기 | 첫 개인 공연 제안 | 빈 소극장 객석 쉰 석을 무대에서 내려다보는 1인칭 |
| `img_path_hap_07_001` | 행복 | 중년기 | 통장과 웃음 사이 | 악기 영수증과 적금 통장을 나란히 펼친 1인칭 |
| `img_path_hap_08_001` | 행복 | 노년기 | 문화센터 할머니 할아버지 선생님 | 복지관 교실에서 기타를 든 노인들을 바라보는 1인칭 |
| `img_path_luk_01_001` | 운 | 유년기 | 문구점 뽑기 3연속 1등 | 뽑기 기계에서 나온 1등 쪽지 세 장을 쥔 1인칭 |
| `img_path_luk_02_001` | 운 | 청소년기 | 추첨으로 뽑힌 해외 연수 | 강당 무대 위 추첨 상자와 교감 선생님 손을 보는 1인칭 |
| `img_path_luk_03_001` | 운 | 청소년기·청년기 | 당첨 축하 문자 | 휴대폰 화면의 당첨 문자와 링크를 보는 1인칭 |
| `img_path_luk_04_001` | 운 | 청년기 | 엘리베이터 안의 30초 | 엘리베이터 층수 표시와 옆 사람 손에 든 서류를 보는 1인칭 |
| `img_path_luk_05_001` | 운 | 중년기 | 동창의 투자 제안 | 술잔 너머로 내밀어진 휴대폰 속 투자 자료를 보는 1인칭 |
| `img_path_luk_06_001` | 운 | 청년기·중년기 | 주운 지갑의 주인 | 길바닥에서 주운 지갑 속 신분증을 보는 1인칭 |
| `img_path_luk_08_001` | 운 | 청년기·중년기 | 놓친 비행기 | 닫힌 탑승 게이트 앞에서 캐리어 손잡이를 쥔 1인칭 |
| `img_path_luk_07_001` | 운 | 노년기 | 운으로 산 인생 | 손주의 눈을 마주 보는 1인칭 |
| `img_path_hea_01_001` | 건강 | 유년기 | 급식 시간의 브로콜리 | 급식판 위 브로콜리 다섯 송이를 내려다보는 1인칭 |
| `img_path_hea_02_001` | 건강 | 청소년기 | 새벽 2시의 책상 | 새벽 2시를 가리키는 탁상시계와 켜진 스탠드를 보는 1인칭 |
| `img_path_hea_03_001` | 건강 | 청년기·중년기 | 첫 종합검진 결과지 | 검진 결과지의 노란 형광펜 줄을 보는 1인칭 |
| `img_path_hea_04_001` | 건강 | 청년기·중년기 | 주말 등산 모임 대장 | 산 정상 표지석 옆에서 회원들을 내려다보는 1인칭 |
| `img_path_hea_05_001` | 건강 | 중년기 | 진단서 한 장 | 진료실 책상 위 진단서와 안내문을 보는 1인칭 |
| `img_path_hea_06_001` | 건강 | 중년기·노년기 | 건강 나이 측정 | 측정기 화면의 건강 나이 숫자를 보는 1인칭 |
| `img_path_hea_07_001` | 건강 | 중년기·노년기 | 계단 앞에서 | 높은 계단을 올려다보는 1인칭 |
| `img_path_hea_08_001` | 건강 | 노년기 | 병원 대기실의 단골 | 대기실 번호표 47번과 전광판을 보는 1인칭 |
