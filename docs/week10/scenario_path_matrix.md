# 10주차 7경로 완결표 — 선택 → 사건 → 인생 사실 → 결말·칭호

- 담당: 신기훈 (시나리오·콘텐츠) · 기준: `10주차_숙제_신기훈_시나리오.md` 1장, 최종 명세 F10-02/04~06/12
- 표는 `events/endings/life_facts/titles.sample.json` 에서 그대로 뽑았다. **굵은 ID**는 10주차 신규 사건이다.
- 플래그 옆 괄호는 `life_facts.sample.json` 의 인생 사실 이름이다. 괄호가 없으면 다음 사건·결말 조건에만 쓰는 플래그다.
- 복원 순서 보조 플래그(`<플래그>__<사건ID>`)는 표에서 뺐다 (`scenario_audit.md` 5장).

## 0. 요약

| 경로 | 사건 수 (10주차 신규) | 전용 결말 | 입구 |
| --- | --- | --- | --- |
| 건강 | 16 (8) | 5 | path_hea_01, path_hea_02, **path_hea_16** |
| 운동능력 | 18 (9) | 5 | path_fit_01, path_fit_03, **path_fit_11**, **path_fit_18** |
| 지력 | 18 (8) | 5 | path_int_01, path_int_02 |
| 사회성 | 18 (9) | 4 | path_soc_01, **path_soc_18** |
| 경제력 | 17 (8) | 6 | path_wea_01, path_wea_03 |
| 행복 | 16 (8) | 4 | path_hap_01, **path_hap_09**, **path_hap_10**, **path_hap_11** |
| 운 | 17 (9) | 6 | path_luk_01, **path_luk_09**, **path_luk_10**, **path_luk_17** |
| 교차 사건 | 12 (12) | — | 관계·자녀·고용·건강 상태 조건 |

## 건강 경로

- 입구: 급식 브로콜리(유년, `path_hea_01`), 새벽 2시의 책상(청소년, `path_hea_02`), **10주차** 어릴 때 편식했던 아이의 요리 실습(`picky_eater` → `path_hea_16`).
- 습관: 수면 부족 회복(`path_hea_09`), 입사 첫 주의 야근 문화(일과 충돌, `path_hea_10`), 아버지의 당뇨 진단(가족, `path_hea_11`) → `healthy_routine`·`hea_managed` → 등산 모임 대장(`path_hea_04`)·건강 나이 측정(`path_hea_06`).
- 질병·조정: 건강 경고(`health_warning`) → 진단서 한 장(전환점 `path_hea_05`) → 회복(`hea_recovered`) 또는 만성(`hea_chronic`) → 약봉지와 회의 일정(일 조정, `path_hea_12`)·완치 1년 뒤 재발 위험(`path_hea_13`, 재발하면 `hea_relapse` 로 회복 결말 차단).
- 말년: 동네 건강 체조 강사(`path_hea_14`), 사전연명의료의향서(`path_hea_15`), 병원 대기실(`path_hea_08`, 실제 사망 결과 포함).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_hea_01 급식 시간의 브로콜리 (일반) | 없음 | **A** 다섯 송이를 다 먹는다. → `hea_path` `good_eater`<br>**B** 짝꿍 것만 돌려준다. → 플래그 없음<br>**C** 브로콜리로 나무를 만들어 급식판 숲을 꾸민다. → `storyteller` | path_hea_02 |
| 청소년 | path_hea_02 새벽 2시의 책상 (일반) | 없음 | **A** 불을 끄고 잔다. → `hea_path` `healthy_routine`(건강 관리 습관) `hea_sleep_habit`<br>**B** 에너지 음료를 따고 밤을 새운다. (결과 2갈래) → `hea_sleep_debt` `health_warning`(건강 경고) | path_hea_03 |
| 청소년 | **path_hea_09** 체육 시간의 어지럼증 (일반) | `hea_sleep_debt` | **A** 보건 선생님과 수면표를 만든다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `hea_sleep_habit`<br>**B** 괜찮다고 하고 계속 달린다. → `health_warning`(건강 경고)<br>**C** 친구랑 아침 산책 내기를 건다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) | path_hea_03 |
| 청소년 | **path_hea_16** 가정 시간 요리 실습 (일반) | `picky_eater`, 없음 `hea_path` | **A** 내가 만든 건 먹어 본다. (결과 2갈래) → `hea_path` `good_eater` `healthy_routine`(건강 관리 습관)<br>**B** 몰래 라면 수프를 넣는다. → 플래그 없음<br>**C** 조원들 몫까지 다 만든다. (결과 2갈래) → `hea_path` `cooking_skill` | path_hea_03 |
| 청년·중년 | path_hea_03 종합검진 결과지 (일반) | `hea_path` | **A** 그날부터 매일 30분 걷는다. → `healthy_routine`(건강 관리 습관) `hea_managed`<br>**B** 경계면 아직 괜찮은 거다. (결과 2갈래) → `health_warning`(건강 경고) | path_hea_04, path_hea_05 |
| 청년·중년 | path_hea_04 주말 등산 모임 대장 (일반) | `hea_managed` | **A** 대장을 맡는다. (결과 2갈래) → `achv_hundred_peaks`(백 개 산 등정) `healthy_routine`(건강 관리 습관) `health_injured`(부상)<br>**B** 혼자 걷는 게 더 좋다. → `healthy_routine`(건강 관리 습관) | path_hea_06 |
| 청년 | **path_hea_10** 입사 첫 주의 야근 문화 (일반) | `hea_path`, `job_employed` | **A** 정시에 퇴근하고 헬스장에 간다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `hea_managed`<br>**B** 다들 남으니 같이 남는다. (결과 2갈래) → `overwork` `health_warning`(건강 경고)<br>**C** 점심시간 걷기 모임을 만든다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `club_joined` `hea_managed` | path_hea_03, path_hea_04, path_hea_05 |
| 청년·중년 | **path_hea_11** 아버지의 당뇨 진단 (일반) | `hea_path` | **A** 온 가족 식단을 같이 바꾼다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `family_bond` `family_conflict`<br>**B** 주말마다 아버지와 등산을 간다. → `healthy_routine`(건강 관리 습관) `family_memory` `hea_managed`<br>**C** 잔소리 대신 혈당계를 선물한다. (결과 2갈래) → 플래그 없음 | path_hea_04, path_hea_06 |
| 중년 | path_hea_05 진단서 한 장 (전환점) | `health_warning` | **A** 일을 줄이고 생활을 다 바꾼다. (결과 2갈래) → `hea_recovered` `healthy_routine`(건강 관리 습관) `health_recovered`(회복) `hea_chronic` `health_chronic`(만성 질환 관리)<br>**B** 전문 재활 프로그램에 등록한다. [경제력≥65] → `hea_recovered` `health_recovered`(회복) `achv_full_recovery`(완전 회복)<br>**C** 지금 일이 더 급하다. 약만 먹는다. → `hea_chronic` `health_chronic`(만성 질환 관리) `overwork` | path_hea_06, path_hea_07 |
| 중년·노년 | path_hea_06 건강 나이 측정 (일반) | 건강 ≥ 70, `healthy_routine`, 없음 `hea_chronic` | **A** 결과지를 액자에 넣는다. → `achv_young_body`(건강 나이 12살 젊음)<br>**B** 보건소 걷기 교실 강사를 자원한다. → `achv_young_body`(건강 나이 12살 젊음) `volunteer` `helped_friend` | path_hea_08 |
| 중년·노년 | path_hea_07 계단 앞에서 (일반) | `hea_chronic` | **A** 솔직하게 말하고 둘레길 코스로 바꾸자고 한다. (결과 2갈래) → `health_limited`(활동 제한) `lifelong_friend`<br>**B** 무리해서 따라간다. (결과 2갈래) → `health_limited`(활동 제한) `health_injured`(부상) `health_warning`(건강 경고) | path_hea_08 |
| 중년 | **path_hea_12** 약봉지와 회의 일정 (일반) | `hea_chronic` | **A** 단축 근무를 신청한다. (결과 2갈래) → `health_chronic`(만성 질환 관리) `healthy_routine`(건강 관리 습관)<br>**B** 약을 먹으며 그대로 버틴다. → `health_chronic`(만성 질환 관리) `overwork`<br>**C** 같은 병을 가진 사람들의 모임을 연다. (결과 2갈래) → `health_chronic`(만성 질환 관리) `achv_patient_circle`(환우 모임 운영) | path_hea_07, path_hea_08 |
| 중년 | **path_hea_13** 완치 판정 1년 뒤 (일반) | `hea_recovered` | **A** 생활 습관을 지키며 일을 조절한다. → `healthy_routine`(건강 관리 습관)<br>**B** 예전처럼 밤새 일한다. (결과 2갈래) → `overwork` `health_warning`(건강 경고) `hea_relapse` `hea_chronic` `health_chronic`(만성 질환 관리)<br>**C** 하프 마라톤으로 완치를 기념한다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `achv_recovery_run`(완치 기념 하프 마라톤) | path_hea_06, path_hea_14 |
| 노년 | path_hea_08 병원 대기실의 단골 (일반) | `hea_path` | **A** 검사 끝나고 대기실 사람들과 국밥을 먹으러 간다. (결과 3갈래) → `achv_clinic_friends`(대기실 친구들) `health_chronic`(만성 질환 관리) `fatal_risk`<br>**B** 오늘도 걸어서 집에 간다. → `achv_walk_home`(걸어서 귀가) | — |
| 노년 | **path_hea_14** 동네 건강 체조 강사 (일반) | 건강 ≥ 60, `healthy_routine` | **A** 강사를 맡는다. (결과 2갈래) → `achv_health_teacher`(동네 건강 체조 강사) `volunteer`<br>**B** 혼자 하는 매일 만 보를 지킨다. → `healthy_routine`(건강 관리 습관)<br>**C** 텃밭을 분양받아 몸을 쓴다. (결과 2갈래) → 플래그 없음 | — |
| 노년 | **path_hea_15** 사전연명의료의향서 (일반) | `hea_path` | **A** 가까운 사람들과 이야기한 뒤 작성한다. → `family_memory`<br>**B** 지금은 생각하고 싶지 않다. → 플래그 없음<br>**C** 혼자 조용히 작성하고 여행을 떠난다. (결과 2갈래) → 플래그 없음 | — |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_hea_peaks — 백 개의 정상을 밟은 사람 | 96 | `achv_hundred_peaks` |
| ending_path_hea_young — 열두 살 젊은 몸으로 산 사람 | 95 | `hea_path`, `achv_young_body` |
| ending_path_hea_recovered — 아프고 나서 더 단단해진 사람 | 94 | `hea_recovered`, 없음 `hea_relapse` |
| ending_path_hea_chronic — 몸과 타협하며 산 사람 | 93 | `hea_chronic` |
| ending_path_hea_neighbors — 공원 체조로 동네를 깨운 사람 | 94 | `hea_path`, `achv_health_teacher` |

관련 칭호: 칠각형 인간(`title_seven_sided`), 감기도 비켜 간 사람(`title_stat_health`)

## 운동능력 경로

- 입구: 체육관 관장님의 명함(유년, `path_fit_01`), **10주차** 어릴 때 태권도(`taekwondo` → `path_fit_11`)·초등 운동부(`sports_club` → `path_fit_18`).
- 훈련·경기: 첫 대회 → 특기생 제안(전환점) → 전국체전과 무릎(`path_fit_04`) → 프로 입단 테스트(전환점 `path_fit_05`).
- 실패·전환: 운동부를 그만둔 뒤(`fit_quit` → `path_fit_10`), 프로 낙방 다음 날(`fit_pro_fail` → 트레이너·재도전·체육 교사, `path_fit_12`), 은퇴 경기(선수 → 코치·해설, `path_fit_14`).
- 지도자·노후: 첫 제자(`path_fit_13`), 학교 스포츠클럽(`path_fit_15`), 체육관(전환점 `path_fit_08`), 조기축구회 감독(선수가 아닌 운동형, `path_fit_17`), 부상 후 노년(`path_fit_16`), 시니어 마라톤(`path_fit_09`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_fit_01 체육관 관장님의 명함 (일반) | 운동능력 ≥ 63 | **A** 육상부에 들어간다. → `fit_path` `sports_club` `sports_training`<br>**B** 운동은 취미로만 한다. → 플래그 없음<br>**C** 명함을 받아 들고 운동장 한 바퀴를 더 돈다. → `fit_path` `sports_club` `sports_training` | path_fit_02 |
| 유년·청소년 | path_fit_02 첫 대회 결승선 (일반) | `fit_path` | **A** 끈을 묶고 다시 자세를 잡는다. (결과 2갈래) → `achv_youth_medal`(첫 메달) `fit_rival`<br>**B** 그냥 달린다. (결과 2갈래) → `achv_youth_medal`(첫 메달) `fit_first_loss` | path_fit_03 |
| 청소년 | path_fit_03 운동부 특기생 제안 (전환점) | 운동능력 ≥ 70, `fit_path`, teen_019 겪음 | **A** 운동부 특기생이 된다. [운동능력≥74] → `fit_path` `fit_elite`<br>**B** 일반고에 가서 공부와 병행한다. → `fit_path` `fit_dual`<br>**C** 운동을 그만두고 공부에 집중한다. → `fit_quit` | path_fit_04, path_fit_05 |
| 청소년 | path_fit_04 전국체전 결승, 무릎의 소리 (일반) | `fit_path`, 없음 `fit_quit` | **A** 테이핑하고 결승에 뛴다. (결과 3갈래) → `achv_national_champion`(전국체전 우승) `fit_injury` `health_injured`(부상) `achv_youth_medal`(첫 메달)<br>**B** 기권하고 병원에 간다. → `healthy_routine`(건강 관리 습관) | path_fit_05 |
| 청소년 | **path_fit_10** 운동부를 그만둔 뒤의 오후 (일반) | `fit_quit` | **A** 그 시간에 공부를 한다. (결과 2갈래) → `study_hard`<br>**B** 동네 생활체육 모임에 나간다. → `healthy_routine`(건강 관리 습관) `gym_habit`<br>**C** 운동부 매니저를 자청한다. (결과 2갈래) → `helped_friend` | — |
| 청소년 | **path_fit_11** 도장 관장님의 대회 추천 (일반) | `taekwondo`, 없음 `fit_path` | **A** 겨루기 대회에 나간다. (결과 2갈래) → `fit_path` `achv_youth_medal`(첫 메달) `fit_first_loss`<br>**B** 품새 시범단에 들어간다. → `fit_path` `sports_club`<br>**C** 검은 띠까지만 하고 그만둔다. → 플래그 없음 | path_fit_02, path_fit_03 |
| 청소년 | **path_fit_18** 체육대회 반 대표 (일반) | `sports_club`, 없음 `fit_path` | **A** 계주 마지막 주자를 맡는다. (결과 2갈래) → `fit_path` `relay_hero`<br>**B** 응원단장을 맡는다. → `cheer_leader`<br>**C** 줄다리기 전략을 짠다. → `responsibility` | path_fit_02, path_fit_03 |
| 청년 | path_fit_05 프로 입단 테스트 (전환점) | `fit_path`, 없음 `fit_quit`, 없음 `pro_athlete`, 없음 `coach_path` | **A** 프로 테스트를 본다. [운동능력≥82] (결과 2갈래) → `career_athlete`(프로 선수) `pro_athlete`(프로 선수) `job_employed`(재직) `career_coach`(코치·감독) `fit_pro_fail`<br>**B** 체육교육과에 진학한다. → `university`(대학 진학) `field_sports`(체육 전공) `career_pe_teacher`(체육 교사) `job_employed`(재직)<br>**C** 재활 트레이너가 된다. [fit_injury] → `career_trainer`(재활 트레이너) `job_employed`(재직) `helped_friend` | path_fit_06, path_fit_07, path_fit_08 |
| 청년·중년 | path_fit_06 주전 경쟁 (일반) | `career_athlete` | **A** 훈련량을 두 배로 늘린다. (결과 2갈래) → `achv_league_best`(리그 베스트 11) `fit_bench` `health_injured`(부상)<br>**B** 후배에게 밥을 사며 비결을 묻는다. (결과 2갈래) → `achv_league_best`(리그 베스트 11) `lifelong_friend` `fit_bench` | path_fit_07 |
| 청년·중년 | path_fit_07 세 번째 수술 상담 (일반) | `fit_path`, `health_injured` | **A** 수술하고 복귀를 노린다. (결과 2갈래) → `achv_comeback`(부상 복귀전) `fit_retired` `health_limited`(활동 제한)<br>**B** 은퇴하고 유소년 감독이 된다. → `career_coach`(코치·감독) `coach_path`(코치) `fit_retired`<br>**C** 해설위원 제안을 받는다. → `career_caster`(해설위원) `fit_retired` | path_fit_08 |
| 청년 | **path_fit_12** 프로 낙방 다음 날 (일반) | `fit_pro_fail` | **A** 재활 트레이너 자격증을 딴다. (결과 2갈래) → `career_trainer`(재활 트레이너) `job_employed`(재직) `level_entry`(신입)<br>**B** 1년 더 준비해서 다시 도전한다. (결과 2갈래) → `fit_retired` `career_athlete`(프로 선수) `pro_athlete`(프로 선수) `job_employed`(재직)<br>**C** 체육 교사 임용을 준비한다. (결과 2갈래) → `career_pe_teacher`(체육 교사) `job_employed`(재직) `level_entry`(신입) `career_coach`(코치·감독) | path_fit_07, path_fit_15 |
| 청년·중년 | **path_fit_13** 줄넘기 열 개를 못 넘는 제자 (일반) | `career_coach` | **A** 기본기부터 천천히 가르친다. (결과 2갈래) → `achv_first_pupil`(첫 제자 대표 선발)<br>**B** 대회 성적 위주로 강하게 훈련한다. (결과 2갈래) → `achv_first_pupil`(첫 제자 대표 선발)<br>**C** 아이가 다른 종목을 해 보게 한다. → `helped_friend` | path_fit_08 |
| 중년 | path_fit_08 내 이름을 건 체육관 (전환점) | 운동능력 ≥ 85, `fit_path`, 없음 `fit_quit` | **A** 내 이름을 건 체육관을 연다. [경제력≥40] (결과 2갈래) → `career_gym_owner`(체육관 관장) `own_shop` `job_self_employed`(자영업) `achv_own_gym`(체육관 개관) `fit_gym_closed` `in_debt`(빚 있음) `career_coach`(코치·감독)<br>**B** 후배들을 길러 내는 지도자로 남는다. → `career_coach`(코치·감독) `achv_trained_champion`(국가대표 제자) | path_fit_09 |
| 중년 | **path_fit_14** 은퇴 경기 제안 (일반) | `career_athlete`, 없음 `fit_retired` | **A** 은퇴 경기에서 마지막으로 뛴다. → `fit_retired` `career_coach`(코치·감독)<br>**B** 해외 리그에서 1년 더 뛴다. (결과 2갈래) → `health_injured`(부상) `fit_retired`<br>**C** 해설위원 제안을 받아들인다. (결과 2갈래) → `fit_retired` `career_caster`(해설위원) | path_fit_08, path_fit_09 |
| 중년 | **path_fit_15** 학교 스포츠클럽 창단 (일반) | `career_pe_teacher` | **A** 스포츠클럽을 만든다. (결과 2갈래) → `achv_school_club`(학교 스포츠클럽 창단)<br>**B** 방과후 수업으로 수당을 받는다. → 플래그 없음<br>**C** 주말에 운동장을 동네에 개방한다. (결과 2갈래) → `volunteer` | — |
| 중년·노년 | **path_fit_17** 조기축구회 감독 제안 (일반) | `fit_path`, 없음 `career_athlete`, 없음 `career_coach` | **A** 감독을 맡는다. (결과 2갈래) → `gym_habit` `achv_local_coach`(조기축구회 감독)<br>**B** 선수로 계속 뛴다. → `gym_habit` `healthy_routine`(건강 관리 습관)<br>**C** 회비 관리와 단톡방만 맡는다. → `responsibility` | path_fit_09 |
| 노년 | path_fit_09 시니어 마라톤 출발선 (일반) | `fit_path` | **A** 풀코스에 도전한다. (결과 2갈래) → `achv_senior_marathon`(시니어 마라톤 완주)<br>**B** 무릎을 생각해 응원석에 선다. [health_limited] → 플래그 없음<br>**C** 5km 걷기 코스로 바꾼다. → `lifelong_friend` | — |
| 노년 | **path_fit_16** 비 오는 날 무릎이 먼저 안다 (일반) | `fit_path`, `health_injured` | **A** 수중 운동 강사를 해 본다. (결과 2갈래) → `healthy_routine`(건강 관리 습관) `volunteer`<br>**B** 옛 팀 동료들과 모인다. → `lifelong_friend`<br>**C** 수술을 한 번 더 받는다. (결과 2갈래) → `health_recovered`(회복) `health_limited`(활동 제한) | — |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_fit_legend — 경기장에 이름을 남긴 선수 | 97 | 운동능력 ≥ 85, `career_athlete`, `achv_league_best` |
| ending_path_fit_coach — 누군가의 첫 메달을 만든 지도자 | 95 | `fit_path`, `career_coach` |
| ending_path_fit_gym — 내 이름을 건 체육관 관장 | 94 | `achv_own_gym` |
| ending_path_fit_teacher — 운동장을 지킨 체육 선생님 | 92 | `career_pe_teacher` |
| ending_path_fit_injury — 부상으로 멈췄지만 다시 걸은 사람 | 93 | `fit_path`, `fit_retired`, `health_limited` |

관련 칭호: 칠각형 인간(`title_seven_sided`), 운동회 마지막 주자(`title_relay`)

## 지력 경로

- 입구: 수학 경시대회(유년), 영재교육원(청소년 전환점) → 수능 D-30(청소년 4년 차 전환점, `int_csat_top`/`int_csat_fail`).
- 진학·전공: 장학금 커트라인(`path_int_04`), **10주차** 원하지 않던 학과(수능 실패 후 전과·재수, `path_int_11`), 논문에서 빠진 내 이름(연구실 윤리, `path_int_12`).
- 다른 길: 쉬어 가는 1년(`path_int_05`), 퇴근 후 야간대학(대학에 가지 않고 일하는 지력형, `path_int_06`), 독학 개발자의 사이드 프로젝트(`path_int_18`), 변리사 두 번째 낙방(`path_int_13`).
- 성과·실패: 졸업 앞의 세 갈래(전환점) → 마감과 가족(`path_int_08`, 번아웃) → 번아웃 뒤 다시 연 노트(`path_int_14`)·취소된 프로젝트(`path_int_15`) → 평생 연구의 마지막 퍼즐(전환점). 자녀 숙제(`path_int_16`), 복지관 스마트폰 교실·회고록(`path_int_17`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_int_01 교내 수학 경시대회 (일반) | 지력 ≥ 62 | **A** 대회에 나간다. (결과 2갈래) → `int_path` `achv_math_award`(수학 경시대회 수상)<br>**B** 친구 생일 파티에 간다. → 플래그 없음<br>**C** 대회 문제를 직접 만들어서 선생님께 낸다. (결과 2갈래) → `int_path` `achv_problem_setter`(경시대회 출제) `achv_math_award`(수학 경시대회 수상) | path_int_02 |
| 청소년 | path_int_02 영재교육원 선발 시험 (전환점) | 지력 ≥ 72 | **A** 추천을 받아 시험을 본다. [지력≥75] (결과 2갈래) → `int_path` `int_gifted` `achv_gifted_program`(영재교육원 합격) `int_gifted_fail`<br>**B** 혼자 공부하는 게 더 맞다. → `int_path` `int_self_study`<br>**C** 추천을 친구에게 양보한다. [사회성≥60] → `int_path` `helped_friend` | path_int_03 |
| 청소년 | path_int_03 수능 D-30 (전환점) | `int_path`, teen_019 겪음, 없음 `teen_038 겪음`, 4년 차 고정 | **A** 하루 네 시간만 자고 문제를 더 푼다. (결과 2갈래) → `int_csat_top` `overwork` `int_csat_fail`<br>**B** 평소처럼 자고 평소처럼 푼다. (결과 2갈래) → `int_csat_top` `int_csat_fail`<br>**C** 사물함에 '수능 대박' 엿을 붙이고 인증샷을 올린다. (결과 2갈래) → `int_csat_top` `int_csat_fail` | path_int_04, path_int_05, path_int_06 |
| 청년 | path_int_04 3.98과 장학금 커트라인 (일반) | `int_path`, `university` | **A** 교수님께 성적 확인 메일을 보낸다. (결과 2갈래) → `scholarship`(장학금) `int_major_strong`<br>**B** 연구실 인턴에 지원한다. (결과 2갈래) → `int_lab`<br>**C** 과외 세 개로 등록금을 직접 번다. (결과 2갈래) → `int_tutor` `student_loan`(학자금 대출) | path_int_07 |
| 청년 | path_int_05 쉬어 가는 1년, 다시 수능 (일반) | `int_path`, `gap_year` | **A** 다시 수능을 본다. (결과 2갈래) → `university`(대학 진학) `field_science`(자연과학 전공) `int_retake_success` `int_retake_fail`<br>**B** 혼자 코딩을 배워 바로 일을 구한다. → `int_self_taught` `career_engineer`(엔지니어) `job_employed`(재직) `level_entry`(신입) | path_int_07 |
| 청년·중년 | path_int_06 퇴근 후 야간대학 (일반) | `int_path`, `job_employed`, 없음 `university` | **A** 야간대학에 등록한다. (결과 2갈래) → `university`(대학 진학) `achv_night_degree`(야간대학 졸업) `int_night_dropout`<br>**B** 사내 기술 자격증부터 딴다. → `career_engineer`(엔지니어) `level_mid`(중간 관리자) | path_int_08 |
| 청년 | path_int_07 졸업 앞의 세 갈래 (전환점) | 지력 ≥ 82, `int_path`, `university`, 5년 차 고정 | **A** 대학원에 진학한다. [지력≥85] → `edu_graduate`(대학원 진학) `career_researcher`(연구원) `job_employed`(재직)<br>**B** 대기업 연구소에 입사한다. → `career_engineer`(엔지니어) `job_employed`(재직) `level_entry`(신입)<br>**C** 변리사 시험에 도전한다. [지력≥90] (결과 2갈래) → `career_patent_attorney`(변리사) `job_employed`(재직) `achv_license`(변리사 합격) `level_entry`(신입) `career_engineer`(엔지니어) `int_exam_fail` | path_int_08, path_int_09 |
| 청년 | **path_int_11** 원하지 않던 학과 (일반) | `int_csat_fail`, `university`, 없음 `int_retake_success` | **A** 전과를 준비한다. (결과 2갈래) → `field_science`(자연과학 전공) `int_major_strong`<br>**B** 지금 학과에서 1등을 해 본다. (결과 2갈래) → `scholarship`(장학금) `lifelong_friend`<br>**C** 휴학하고 다시 수능을 본다. (결과 2갈래) → `int_retake_success` `field_science`(자연과학 전공) `int_retake_fail` | path_int_07 |
| 청년 | **path_int_12** 논문에서 빠진 내 이름 (일반) | `int_lab` | **A** 교수님께 정중히 말씀드린다. (결과 2갈래) → `int_major_strong` `achv_first_paper`(첫 논문 저자)<br>**B** 그냥 넘어간다. → 플래그 없음<br>**C** 그 실험을 내 방식으로 다시 해 본다. (결과 2갈래) → `int_lab` `achv_first_paper`(첫 논문 저자) | path_int_07 |
| 청년·중년 | **path_int_13** 변리사 시험 두 번째 낙방 (일반) | `int_exam_fail` | **A** 마지막으로 한 번 더 본다. (결과 2갈래) → `career_patent_attorney`(변리사) `achv_license`(변리사 합격) `job_employed`(재직)<br>**B** 특허사무소 실무자로 취업한다. → `career_office`(회사원) `job_employed`(재직) `level_entry`(신입)<br>**C** 시험 학원 강사가 된다. (결과 2갈래) → `career_instructor`(강사) `job_employed`(재직) | path_int_08 |
| 청년·중년 | **path_int_18** 독학 개발자의 사이드 프로젝트 (일반) | `int_self_taught` | **A** 주말마다 만들어 출시한다. (결과 2갈래) → `responsibility` `achv_app_launch`(앱 출시)<br>**B** 회사 일에 집중해 승진한다. → `level_mid`(중간 관리자)<br>**C** 주말 코딩 강의를 연다. (결과 2갈래) → `helped_friend` | path_int_08, path_int_09 |
| 중년 | path_int_08 마감 D-1과 가족 모임 (일반) | `int_path`, `job_employed` | **A** 주말 내내 결과를 마무리한다. (결과 3갈래) → `achv_major_paper`(대표 논문) `int_burnout` `health_warning`(건강 경고) `overwork` `int_project_delayed`<br>**B** 가족 모임에 간다. → `family_bond` `family_memory`<br>**C** 노트북을 들고 가족 모임에 간다. (결과 2갈래) → `achv_major_paper`(대표 논문) `int_project_delayed` | path_int_09, path_int_10 |
| 중년 | path_int_09 평생 연구의 마지막 퍼즐 (전환점) | 지력 ≥ 88, `int_path`, `job_employed` | **A** 연구를 끝까지 밀어붙인다. [career_researcher] (결과 2갈래) → `achv_breakthrough`(연구 돌파구) `level_senior`(책임자) `int_project_cancelled`<br>**B** 결과를 특허로 낸다. [career_engineer] → `patent_holder`(특허 보유) `achv_patent`(특허 등록) `level_senior`(책임자)<br>**C** 은사님 강의를 맡아 후배를 가르친다. → `achv_mentor`(후배 양성) `helped_friend` `career_professor`(강단에 선 연구자) | path_int_10 |
| 중년 | **path_int_14** 번아웃 뒤 다시 연 연구 노트 (일반) | `int_burnout` | **A** 연구 대신 후배를 가르친다. (결과 2갈래) → `achv_mentor`(후배 양성) `career_professor`(강단에 선 연구자)<br>**B** 1년 안식년을 갖는다. → `health_recovered`(회복) `healthy_routine`(건강 관리 습관)<br>**C** 동네에 작은 공부방을 연다. (결과 2갈래) → `career_instructor`(강사) `job_self_employed`(자영업) | path_int_09, path_int_10 |
| 중년 | **path_int_15** 취소된 프로젝트의 노트 (일반) | `int_project_cancelled` | **A** 개인 시간에 혼자 완성해 본다. (결과 2갈래) → `patent_holder`(특허 보유) `achv_patent`(특허 등록)<br>**B** 다른 팀으로 옮긴다. → `level_mid`(중간 관리자)<br>**C** 실패 기록을 모두 공개한다. (결과 2갈래) → `achv_open_record`(실패 기록 공개) | path_int_10 |
| 중년·노년 | **path_int_16** 아이의 수학 숙제 (일반) | `int_path`, adult_039 겪음 | **A** 같이 머리를 맞대고 푼다. (결과 2갈래) → `family_bond` `family_memory` `family_conflict`<br>**B** 학원에 맡긴다. → 플래그 없음<br>**C** 나도 모르겠다고 솔직히 말한다. → `family_bond` | — |
| 노년 | path_int_10 마지막 강의 (일반) | `int_path` | **A** 평생의 연구 이야기를 한다. [achv_major_paper] → `achv_last_lecture`(마지막 강연)<br>**B** 실패한 이야기만 골라서 한다. → `achv_last_lecture`(마지막 강연)<br>**C** 손주 숙제를 대신 풀어 주고 강연은 미룬다. [v4:event:adult_039] → `family_bond` `family_memory` | — |
| 노년 | **path_int_17** 복지관 스마트폰 교실 (일반) | `int_path`, 없음 `achv_last_lecture` | **A** 강사를 맡는다. (결과 2갈래) → `achv_mentor`(후배 양성) `volunteer`<br>**B** 회고록을 쓴다. (결과 2갈래) → `achv_memoir`(회고록 출간)<br>**C** 바둑 대국으로 머리를 쓴다. → 플래그 없음 | — |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_int_researcher — 질문 하나로 평생을 산 연구자 | 97 | 지력 ≥ 85, `int_path`, `edu_graduate`, `achv_breakthrough` |
| ending_path_int_engineer — 특허 한 줄에 이름을 남긴 엔지니어 | 96 | `int_path`, `achv_patent` |
| ending_path_int_teacher — 질문을 물려준 선생님 | 95 | `int_path`, `achv_mentor` |
| ending_path_int_late — 퇴근길에 딴 졸업장 | 94 | `int_path`, `achv_night_degree` |
| ending_path_int_burnout — 책상 앞에서 놓친 것들 | 93 | `int_path`, `int_burnout` |

관련 칭호: 칠각형 인간(`title_seven_sided`), 액자 속 특허권자(`title_patent`), 장학금 사냥꾼(`title_scholarship`)

## 사회성 경로

- 입구: 반장 선거 연설(유년), **10주차** 어릴 때 전학생 옆에 앉았던 아이(`kind_heart` → `path_soc_18`).
- 친구·갈등: 축제 준비위원회(`path_soc_02`) → 갈라진 무리 중재(`soc_conflict` → `path_soc_10`) → 집에 가는 길이 같은 사람(`path_soc_03`).
- 연애·비혼·결혼: 같이 살 집을 구하는 친구들(비혼 경로 `path_soc_11`), 상견례(`path_soc_05`), 식탁 위의 침묵(이혼 갈래 `path_soc_07`) → 혼자 차린 생일상(`path_soc_08`).
- 동료·공동체: 입사 동기 단톡방 총무(`path_soc_12`), 영업 1등의 슬럼프(`path_soc_13`) → 상담사(`path_soc_14`), 육아 품앗이(자녀, `path_soc_15`), 동네가 나를 부른다(전환점) → 현관에 쌓인 신문(비혼 노년 `path_soc_16`)·오래된 친구의 부고(`path_soc_17`)·칠순 잔치(`path_soc_09`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_soc_01 반장 선거 연설 (일반) | 사회성 ≥ 64, 없음 `child_003 겪음` | **A** 친구들 이름을 하나씩 부르며 즉석 연설을 한다. (결과 2갈래) → `soc_path` `class_leader` `achv_class_president`(반장 당선)<br>**B** 다른 후보를 지지한다고 말하고 내려온다. → `soc_path` `leader_experience` | path_soc_02 |
| 청소년 | path_soc_02 축제 준비위원회의 밤 (일반) | `soc_path` | **A** 양쪽을 모아 밤새 예산표를 다시 짠다. (결과 2갈래) → `soc_team_lead` `achv_festival`(축제 최고 매출) `club_joined` `soc_conflict`<br>**B** 투표로 정하자고 한다. → `soc_team_lead` `club_joined`<br>**C** 내 용돈으로 모자란 예산을 채운다. → `soc_team_lead` `secret_payer` `club_joined` | path_soc_03 |
| 청소년·청년 | path_soc_03 집에 가는 길이 같은 사람 (일반) | `soc_path` | **A** "같이 내려서 걸을래?"라고 묻는다. (결과 2갈래) → `dating`(연애 중) `first_love` `best_friend`<br>**B** 모르는 척 다음 정류장에서 내린다. → `missed_chance` | path_soc_04, path_soc_05 |
| 청소년 | **path_soc_10** 축제가 끝나고 갈라진 무리 (일반) | `soc_conflict` | **A** 양쪽을 한자리에 부른다. (결과 2갈래) → `best_friend` `achv_peacemaker`(친구들 화해 중재)<br>**B** 한쪽 편에 선다. → `best_friend`<br>**C** 둘 다 잠깐 거리를 둔다. → 플래그 없음 | path_soc_03 |
| 청소년 | **path_soc_18** 전학 온 친구의 첫 점심 (일반) | `kind_heart`, 없음 `soc_path` | **A** 이번에도 옆에 앉는다. (결과 2갈래) → `soc_path` `best_friend`<br>**B** 반 친구들 자리로 데려온다. → `soc_path` `club_joined`<br>**C** 이번엔 다른 친구가 챙기게 둔다. → 플래그 없음 | path_soc_02, path_soc_03 |
| 청년 | path_soc_04 사람으로 들어온 일자리 (전환점) | 사회성 ≥ 82, `soc_path`, 5년 차 고정 | **A** 선배 회사 영업팀에 간다. [사회성≥85] (결과 2갈래) → `career_sales`(영업) `job_employed`(재직) `achv_top_sales`(영업 1등) `level_entry`(신입) `career_office`(회사원)<br>**B** 상담·복지 일을 배운다. → `career_counselor`(상담사) `job_employed`(재직) `level_entry`(신입)<br>**C** 친구와 같이 창업한다. [경제력≥60] (결과 2갈래) → `career_founder`(창업가) `startup_founder`(창업 경험) `job_self_employed`(자영업) `soc_friend_lost` `job_unemployed`(실직) | path_soc_06 |
| 청년·중년 | path_soc_05 상견례 대참사 (일반) | `soc_path`, `married`, 없음 `divorced` | **A** 그날 축구 영상을 찾아서 같이 본다. (결과 2갈래) → `family_bond` `achv_family_peace`(집안의 화해) `family_memory` `family_conflict`<br>**B** 신혼집 이야기로 화제를 돌린다. (결과 2갈래) → `housing_jeonse`(전세) `housing_monthly_rent`(월세) | path_soc_07 |
| 청년 | **path_soc_11** 같이 살 집을 구하는 친구들 (일반) | `soc_path`, 없음 `dating`, 없음 `married` | **A** 셋이 함께 산다. (결과 2갈래) → `single_life`(혼자의 삶) `housing_jeonse`(전세) `achv_found_family`(친구라는 가족) `housing_monthly_rent`(월세)<br>**B** 혼자 사는 삶을 고른다. → `single_life`(혼자의 삶) `housing_monthly_rent`(월세)<br>**C** 그래도 소개팅을 한 번 나가 본다. (결과 2갈래) → `lifelong_friend` `dating`(연애 중) `met_ideal` | path_soc_16 |
| 청년·중년 | **path_soc_12** 입사 동기 단톡방의 총무 (일반) | `soc_path`, `job_employed` | **A** 총무를 맡는다. (결과 2갈래) → `responsibility` `club_joined`<br>**B** 퇴사하는 동기의 이직을 돕는다. → `helped_friend` `best_friend`<br>**C** 단톡방을 조용히 나간다. → 플래그 없음 | path_soc_06 |
| 중년 | path_soc_06 동네가 나를 부른다 (전환점) | 사회성 ≥ 88, `soc_path` | **A** 선거에 나간다. [사회성≥90] (결과 2갈래) → `achv_community_leader`(주민자치위원장) `volunteer`<br>**B** 사람들을 이어 주는 모임을 만든다. → `achv_connector`(연결 모임) `best_friend` | path_soc_08 |
| 중년 | path_soc_07 식탁 위의 침묵 (일반) | 사회성 ≥ 60, `married`, 없음 `divorced` | **A** 이번 주말 둘만의 여행을 예약한다. (결과 2갈래) → `family_bond` `achv_family_peace`(집안의 화해) `family_memory` `divorced`(이혼) `soc_divorce`<br>**B** 부부 상담을 받아 보자고 한다. → `family_talk` | path_soc_08 |
| 중년·노년 | path_soc_08 혼자 차린 생일상 (일반) | `divorced` | **A** 친구들을 불러 남은 미역국을 같이 먹는다. → `lifelong_friend` `achv_found_family`(친구라는 가족)<br>**B** 혼자 조용히 하루를 보낸다. → `single_life`(혼자의 삶) | path_soc_09 |
| 중년 | **path_soc_13** 영업 1등의 슬럼프 (일반) | `career_sales` | **A** 오래된 고객들을 한 명씩 찾아간다. (결과 2갈래) → `achv_top_sales`(영업 1등) `helped_friend`<br>**B** 팀장 승진 제안을 받아들인다. → `level_mid`(중간 관리자) `responsibility`<br>**C** 상담 공부를 시작한다. (결과 2갈래) → `career_counselor`(상담사) `volunteer` | path_soc_06 |
| 중년 | **path_soc_14** 상담실 창가의 화분 (일반) | `career_counselor` | **A** 지역 상담 센터를 연다. (결과 2갈래) → `achv_counsel_center`(상담 센터 개소) `job_self_employed`(자영업)<br>**B** 지금처럼 한 사람씩 만난다. → `helped_friend`<br>**C** 나도 상담을 받으러 간다. → 플래그 없음 | path_soc_09 |
| 중년 | **path_soc_15** 육아 품앗이 단톡방 (일반) | `soc_path`, adult_039 겪음 | **A** 품앗이 반장을 맡는다. (결과 2갈래) → `achv_connector`(연결 모임) `family_bond`<br>**B** 우리 아이는 우리가 본다. → `family_bond`<br>**C** 당번 대신 간식비를 낸다. → 플래그 없음 | path_soc_06 |
| 노년 | path_soc_09 칠순 잔치의 방명록 (일반) | `soc_path` | **A** 한 사람 한 사람에게 인사하러 돈다. → `achv_full_guestbook`(방명록 두 권)<br>**B** 가족 테이블에 앉아 손주들과 사진을 찍는다. [v4:event:adult_039] → `family_bond` `family_memory`<br>**C** 마이크를 잡고 노래방 18번을 부른다. → 플래그 없음 | — |
| 노년 | **path_soc_16** 현관에 쌓인 신문 (일반) | `single_life`, 없음 `married` | **A** 층마다 반찬 나눔 모임을 만든다. (결과 2갈래) → `achv_found_family`(친구라는 가족) `volunteer`<br>**B** 그 어르신만 조용히 챙긴다. → `helped_friend`<br>**C** 보호소에서 나이 든 개를 입양한다. → `pet_bond` | — |
| 노년 | **path_soc_17** 오래된 친구의 부고 (일반) | `soc_path` | **A** 장례식장을 끝까지 지킨다. → `lifelong_friend`<br>**B** 조용한 단톡방을 다시 깨운다. (결과 2갈래) → `lifelong_friend` `achv_reunion`(동창회 재결성)<br>**C** 친구 가족을 돕는다. → `helped_friend` | path_soc_09 |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_soc_leader — 동네가 기억하는 이름 | 97 | `achv_community_leader` |
| ending_path_soc_family — 큰 식탁의 중심 | 96 | `soc_path`, `married`, `family_bond`, 없음 `divorced` |
| ending_path_soc_connector — 사람과 사람을 이어 준 사람 | 95 | `soc_path`, `achv_connector` |
| ending_path_soc_after_divorce — 헤어진 뒤에도 사람을 잃지 않은 사람 | 93 | `soc_path`, `divorced` |

관련 칭호: 평생 반장(`title_forever_leader`), 맨땅에서 시작한 창업가(`title_founder`), 첫사랑 보유자(`title_first_love`)

## 경제력 경로

- 입구: 첫 월급 37만 원(청소년 전환점), 세 장의 계약서(청년 5년 차 전환점).
- 고용: 편의점 야간 알바(돈과 건강, `path_wea_10`), 첫 승진 심사(`path_wea_11`) → 임원 제안(정점과 건강 충돌, `path_wea_15`), 실직 석 달째(재취업·창업·직업 훈련, `path_wea_12`), 월세 날의 통장 잔고(`path_wea_07`).
- 주거·자산: 월세에서 전세로(`path_wea_13`), 전셋집 계약 전날(`path_wea_04`), 내 집 마련 상담(전환점 `path_wea_05`), 아이 학원비(자녀, `path_wea_14`), 가족 지출 명세서(`path_wea_06`).
- 노후: 첫 연금 수령일(`path_wea_16`), 빚 정리 상담(`path_wea_17`, 집이 있으면 줄여서 갚기), 통장 정리하는 날(`path_wea_09`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 청소년 | path_wea_01 첫 월급 37만 원 (전환점) | 경제력 ≥ 56, teen_019 겪음 | **A** 적금 통장을 만든다. → `wea_path` `saving_habit` `money_habit` `part_time_job`<br>**B** 주식 계좌를 열고 첫 주식을 산다. (결과 2갈래) → `wea_path` `wea_investor` `part_time_job`<br>**C** 친구들에게 떡볶이를 쏜다. → `secret_payer` `part_time_job` | path_wea_02, path_wea_03 |
| 청소년·청년 | path_wea_02 중고 거래 사장님 (일반) | `wea_path` | **A** 온라인 가게를 정식으로 연다. (결과 2갈래) → `wea_seller` `achv_first_business`(첫 사업자 등록)<br>**B** 번 돈으로 학원비를 낸다. → `family_bond` `family_memory` | path_wea_03 |
| 청소년 | **path_wea_10** 편의점 야간 알바 제안 (일반) | `wea_path` | **A** 야간까지 한다. (결과 2갈래) → `part_time_job` `overwork` `health_warning`(건강 경고)<br>**B** 낮 알바만 계속한다. → `part_time_job` `saving_habit`<br>**C** 사장님께 재고 관리 일을 제안한다. (결과 2갈래) → `part_time_job` `money_habit` | path_wea_02 |
| 청년 | path_wea_03 세 장의 계약서 (전환점) | 경제력 ≥ 62, 5년 차 고정 | **A** 대기업에 입사한다. → `wea_path` `career_office`(회사원) `corporate_job` `job_employed`(재직) `housing_monthly_rent`(월세) `level_entry`(신입)<br>**B** 스타트업에 들어간다. [part_time_job] (결과 2갈래) → `wea_path` `career_startup`(스타트업 직원) `job_employed`(재직) `achv_stock_option`(스톡옵션 대박) `level_entry`(신입) `job_unemployed`(실직)<br>**C** 내 가게를 연다. [wea_seller] (결과 2갈래) → `wea_path` `career_founder`(창업가) `startup_founder`(창업 경험) `own_shop` `job_self_employed`(자영업) `wea_struggle` | path_wea_04, path_wea_06 |
| 청년·중년 | path_wea_04 전셋집 계약 전날 (일반) | `wea_path`, `job_employed` | **A** 등기부등본부터 떼어 본다. (결과 2갈래) → `housing_jeonse`(전세)<br>**B** 좋은 집은 빨리 잡아야 한다. 계약금을 넣는다. (결과 2갈래) → `housing_jeonse`(전세) `wea_jeonse_loss` `in_debt`(빚 있음)<br>**C** 부모님 집에 조금 더 산다. → `housing_with_parents`(부모님 집) | path_wea_05 |
| 청년·중년 | path_wea_07 월세 날의 통장 잔고 (일반) | `wea_struggle` | **A** 대출을 받아 버틴다. (결과 2갈래) → `in_debt`(빚 있음) `comeback`(재취업) `wea_survived_crisis` `wea_bankrupt` `job_unemployed`(실직)<br>**B** 가게를 접고 다시 회사에 들어간다. → `career_office`(회사원) `job_employed`(재직) `comeback`(재취업) `level_entry`(신입) | path_wea_08 |
| 청년·중년 | **path_wea_11** 첫 승진 심사 (일반) | `wea_path`, `job_employed`, 없음 `level_mid`, 없음 `level_senior`, 없음 `level_executive`, 없음 `job_self_employed` | **A** 내 실적만 정직하게 쓴다. (결과 2갈래) → `level_mid`(중간 관리자) `responsibility`<br>**B** 이직 제안을 받아 직급을 올린다. → `level_mid`(중간 관리자) `career_office`(회사원)<br>**C** 승진 대신 노조 활동을 시작한다. → `responsibility` `club_joined` | path_wea_05, path_wea_15 |
| 청년·중년 | **path_wea_12** 실직 석 달째 (일반) | `wea_path`, `job_unemployed`, 없음 `comeback` | **A** 연봉을 낮춰서라도 재취업한다. (결과 2갈래) → `comeback`(재취업) `job_employed`(재직) `career_office`(회사원) `level_entry`(신입)<br>**B** 퇴직금으로 작은 가게를 연다. (결과 2갈래) → `own_shop` `job_self_employed`(자영업) `startup_founder`(창업 경험) `wea_struggle`<br>**C** 직업 훈련으로 새 기술을 배운다. (결과 2갈래) → `comeback`(재취업) `job_employed`(재직) `career_technician`(기술자) `level_entry`(신입) | path_wea_07 |
| 청년·중년 | **path_wea_13** 월세에서 전세로 (일반) | `wea_path`, `housing_monthly_rent`, 없음 `housing_jeonse`, 없음 `home_owner` | **A** 전세 대출을 받아 옮긴다. (결과 2갈래) → `housing_jeonse`(전세)<br>**B** 부모님 집으로 들어가 돈을 모은다. → `housing_with_parents`(부모님 집) `saving_habit`<br>**C** 월세를 올려 주고 그 자리에 산다. → `housing_monthly_rent`(월세) | path_wea_04, path_wea_05 |
| 중년 | path_wea_05 내 집 마련 상담 (전환점) | 경제력 ≥ 75, `wea_path`, 없음 `home_owner` | **A** 대출을 끼고 아파트를 산다. [경제력≥80] → `home_owner`(내 집 마련) `housing_own_apartment`(내 집(아파트)) `in_debt`(빚 있음) `achv_home`(내 집 마련)<br>**B** 전세를 유지하고 남은 돈을 투자한다. (결과 2갈래) → `wea_investor` `housing_jeonse`(전세) `wea_market_loss`<br>**C** 도시를 떠나 마당 있는 집을 산다. [경제력≥60] → `home_owner`(내 집 마련) `housing_own_house`(마당 있는 내 집) `achv_home`(내 집 마련) `in_debt`(빚 있음) | path_wea_06, path_wea_08 |
| 중년 | path_wea_06 가족 지출 명세서 (일반) | `wea_path` | **A** 부모님 병원비는 내가 다 낸다. → `family_bond` `family_memory`<br>**B** 형제들과 똑같이 나눈다. (결과 2갈래) → `family_bond` `family_memory` `family_conflict`<br>**C** 부업을 하나 더 시작한다. → `overwork` | path_wea_08 |
| 중년 | path_wea_08 회사를 사겠다는 전화 (일반) | `career_founder`, 없음 `wea_bankrupt` | **A** 회사를 판다. → `achv_exit`(회사 매각) `job_retired`(은퇴) `debt_free`(빚 청산)<br>**B** 거절하고 회사를 더 키운다. (결과 2갈래) → `achv_company_100`(직원 백 명) `level_executive`(임원·대표) | path_wea_09 |
| 중년 | **path_wea_14** 아이 학원비 계산서 (일반) | `wea_path`, adult_039 겪음 | **A** 피아노만 남기고 나머지는 줄인다. → `family_bond` `saving_habit`<br>**B** 다 보낸다. 투자라고 생각한다. (결과 2갈래) → `overwork`<br>**C** 아이와 같이 한 달 예산표를 짠다. (결과 2갈래) → `family_bond` `money_habit` `family_conflict` | path_wea_06 |
| 중년 | **path_wea_15** 임원 제안 (일반) | `wea_path`, `level_mid`, 없음 `level_executive`, 없음 `job_self_employed` | **A** 임원을 맡는다. (결과 2갈래) → `level_executive`(임원·대표) `overwork` `health_warning`(건강 경고)<br>**B** 정중히 거절하고 지금 자리를 지킨다. → `work_life_balance`<br>**C** 퇴사하고 독립한다. (결과 2갈래) → `career_founder`(창업가) `job_self_employed`(자영업) `startup_founder`(창업 경험) `wea_struggle` | path_wea_09 |
| 노년 | path_wea_09 통장 정리하는 날 (일반) | `wea_path` | **A** 장학 재단에 일부를 기부한다. [achv_exit] → `achv_donation`(장학금 기부)<br>**B** 남은 빚부터 정리한다. [in_debt] → `debt_free`(빚 청산)<br>**C** 가족에게 나누고 남은 돈으로 여행을 간다. → `family_bond` `family_memory` | — |
| 노년 | **path_wea_16** 첫 연금 수령일 (일반) | `wea_path` | **A** 소일거리로 경비 일을 한다. → `job_employed`(재직)<br>**B** 다 큰 아이 이름으로 통장을 하나 만든다. [v4:event:adult_039] → `family_bond`<br>**C** 연금으로 매년 한 번 여행을 간다. (결과 2갈래) → 플래그 없음 | path_wea_09 |
| 노년 | **path_wea_17** 빚 정리 상담 (일반) | `wea_path`, `in_debt`, 없음 `debt_free` | **A** 집을 줄여 빚을 갚는다. [home_owner] → `debt_free`(빚 청산)<br>**B** 남은 기간 동안 천천히 갚는다. → 플래그 없음<br>**C** 채무 조정을 신청한다. (결과 2갈래) → `debt_free`(빚 청산) | path_wea_09 |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_wea_exit — 작은 가게를 큰 회사로 키운 창업가 | 97 | `achv_exit` |
| ending_path_wea_ceo — 직원 백 명의 대표 | 96 | `achv_company_100` |
| ending_path_wea_home — 대출을 다 갚은 날 | 95 | `wea_path`, `achv_home`, `debt_free` |
| ending_path_wea_stock — 스톡옵션 숫자에 0을 붙인 사람 | 94 | `wea_path`, `achv_stock_option` |
| ending_path_wea_saver — 적금 통장 하나로 시작한 자산가 | 91 | 경제력 ≥ 85, `wea_path`, `money_habit` |
| ending_path_wea_debt — 빚과 함께 늙은 사장님 | 92 | `wea_path`, `in_debt`, 없음 `debt_free` |

관련 칭호: 대출 다 갚은 집주인(`title_paid_off_home`), 맨땅에서 시작한 창업가(`title_founder`), 문패 단 사람(`title_doorplate`)

## 행복 경로

- 입구(9주차까지 무작위 10%의 원인): 유년 행복 74 이상만 들어오던 `path_hap_01`. **10주차** 예술고 진학 후 첫 학기(`arts_high` + 행복 64 → `path_hap_09`), 시험 끝난 날의 스케치북(청소년 행복 65 → `path_hap_10`), 옷장 위의 기타 케이스(청년 행복 82 → `path_hap_11`). 운 경로나 다른 네 경로(운동·지력·사회성·경제력)에 이미 들어간 사람에게는 뜨지 않는다.
- 선택·조정: 좋아하는 것과 잘하는 것(전공) → 연봉이냐 저녁이냐(전환점) → 합주 날과 부모님 방문(가족, `path_hap_13`), 취미가 일이 된 뒤(`path_hap_14`).
- 번아웃·회복: 아무것도 재미없는 일요일(`path_hap_04`) → 좋았던 일 하나 적기(`path_hap_12`) / 웃음을 잃은 채 노년 → 30년 전 수첩의 빈칸(`path_hap_16`).
- 결말 쪽: 첫 개인 공연 제안(전환점), 주민센터 로비의 첫 개인전(노년, `path_hap_15`), 문화센터 선생님(`path_hap_08`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_hap_01 주말의 비밀 취미 (일반) | 행복 ≥ 74, 없음 `luk_path` | **A** 없는 줄 하나 빼고 다섯 줄로 연습한다. → `hap_path` `creative_hobby` `music_path`<br>**B** 기타 대신 그림을 그린다. → `hap_path` `creative_hobby` `art_interest`<br>**C** 창고를 다시 닫고 게임을 한다. → `gamer` | path_hap_02 |
| 청소년 | path_hap_02 좋아하는 것과 잘하는 것 (일반) | `hap_path`, teen_019 겪음 | **A** 좋아하는 걸 전공하겠다고 말한다. (결과 2갈래) → `hap_art_major` `arts_track`(예술 전공) `family_conflict`<br>**B** 성적에 맞춰 가고 취미는 남겨 둔다. → `hap_hobby_side` | path_hap_03 |
| 청소년 | **path_hap_09** 예술고 첫 학기 (일반) | 행복 ≥ 64, `arts_high`, 없음 `hap_path`, 없음 `luk_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 내가 좋아하는 방식대로 한다. (결과 2갈래) → `hap_path` `creative_hobby`<br>**B** 남들만큼 연습하며 버틴다. → `overwork`<br>**C** 친구들과 작은 공연을 기획한다. (결과 2갈래) → `hap_path` `club_joined` | path_hap_02 |
| 청소년 | **path_hap_10** 시험 끝난 날의 스케치북 (일반) | 행복 ≥ 65, 없음 `hap_path`, 없음 `arts_high`, 없음 `luk_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 하루 종일 좋아하는 걸 한다. → `hap_path` `creative_hobby`<br>**B** 친구들을 따라 PC방에 간다. → `gamer`<br>**C** 그림 동아리를 만든다. (결과 2갈래) → `hap_path` `creative_hobby` `club_joined` | path_hap_02 |
| 청년 | path_hap_03 연봉이냐 저녁이냐 (전환점) | `hap_path`, 5년 차 고정 | **A** 6시 퇴근 회사에 간다. → `work_life_balance` `career_office`(회사원) `job_employed`(재직) `level_entry`(신입)<br>**B** 연봉 높은 회사에 간다. [지력≥55] → `career_office`(회사원) `job_employed`(재직) `overwork` `level_entry`(신입)<br>**C** 친구와 작업실을 연다. [hap_art_major] (결과 2갈래) → `career_creator`(창작자) `job_self_employed`(자영업) `achv_first_commission`(첫 작업 의뢰) `overwork` | path_hap_04, path_hap_05 |
| 청년·중년 | path_hap_04 아무것도 재미없는 일요일 (일반) | `hap_path`, `overwork` | **A** 휴직계를 낸다. (결과 2갈래) → `burnout_recovered` `hap_rest` `burnout`<br>**B** 상담을 받아 본다. → `burnout_recovered`<br>**C** 그냥 버틴다. (결과 2갈래) → `burnout` `hap_lost_joy` `health_warning`(건강 경고) | path_hap_06 |
| 청년·중년 | path_hap_05 취미 모임에서 만난 사람 (일반) | `hap_path`, 없음 `married` | **A** 다음 주에도 간다. (결과 2갈래) → `married`(결혼) `achv_band_wedding`(합주 모임 결혼식) `lifelong_friend`<br>**B** 혼자 하는 취미가 더 편하다. → `single_life`(혼자의 삶) `solo_musician` | path_hap_06 |
| 청년 | **path_hap_11** 옷장 위의 기타 케이스 (일반) | 행복 ≥ 82, 없음 `hap_path`, 없음 `luk_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 동네 취미 모임에 나간다. (결과 2갈래) → `hap_path` `creative_hobby`<br>**B** 혼자 매일 10분씩 친다. → `hap_path` `solo_musician`<br>**C** 중고로 팔고 그 돈으로 맛있는 걸 먹는다. → 플래그 없음 | path_hap_05, path_hap_06, path_hap_07 |
| 청년·중년 | **path_hap_12** 좋았던 일 하나 적기 (일반) | `hap_path`, `burnout_recovered` | **A** 하루 하나씩 1년을 채운다. → `content_life`<br>**B** 다시 일에 전부를 건다. (결과 2갈래) → `overwork` `hap_lost_joy`<br>**C** 같은 경험을 한 사람들과 모임을 연다. (결과 2갈래) → `helped_friend` `achv_rest_club`(쉼 모임 운영) | path_hap_06, path_hap_07 |
| 중년 | path_hap_06 첫 개인 공연 제안 (전환점) | `hap_path`, 없음 `hap_lost_joy` | **A** 공연을 연다. [행복≥80] (결과 2갈래) → `achv_solo_show`(개인 공연)<br>**B** 가족 앞에서만 작게 연다. → `family_bond` `content_life` `family_memory` | path_hap_07, path_hap_08 |
| 중년 | path_hap_07 통장과 웃음 사이 (일반) | `hap_path` | **A** 취미로 돈을 버는 방법을 찾는다. (결과 2갈래) → `achv_hobby_income`(취미로 번 첫 돈) `content_life`<br>**B** 지금 행복한 게 노후다. → `content_life` | path_hap_08 |
| 중년 | **path_hap_13** 합주 날과 부모님 방문 (일반) | `hap_path`, `married`, 없음 `divorced` | **A** 부모님을 합주실에 초대한다. (결과 2갈래) → `family_bond` `content_life`<br>**B** 이번 주만 합주를 쉰다. → `family_bond`<br>**C** 취미 시간은 양보하지 않는다. (결과 2갈래) → `family_conflict` | path_hap_06 |
| 중년 | **path_hap_14** 취미가 일이 된 뒤 (일반) | `hap_path`, `achv_hobby_income` | **A** 강습을 늘려 본업으로 삼는다. (결과 2갈래) → `career_creator`(창작자) `job_self_employed`(자영업) `hap_lost_joy`<br>**B** 강습을 줄이고 다시 취미로 돌린다. → `content_life`<br>**C** 수강생들과 작은 연주회를 연다. (결과 2갈래) → `achv_student_concert`(수강생 연주회) | path_hap_08 |
| 노년 | path_hap_08 문화센터 할머니 할아버지 선생님 (일반) | `hap_path` | **A** 첫 곡으로 다 같이 아는 동요를 고른다. → `achv_senior_teacher`(복지관 강사)<br>**B** 손주에게 먼저 가르쳐 본다. [v4:event:adult_039] → `family_bond` `family_memory`<br>**C** 번아웃 때 쓴 일기를 꺼내 읽는다. [burnout] → `burnout_recovered` | — |
| 노년 | **path_hap_15** 주민센터 로비의 첫 개인전 (일반) | `hap_path`, `creative_hobby`, 없음 `hap_lost_joy` | **A** 첫 개인전을 연다. (결과 2갈래) → `achv_solo_show`(개인 공연)<br>**B** 하나씩 골라 사람들에게 나눠 준다. → `family_memory`<br>**C** 상자째 동네 학교에 기증한다. → `volunteer` | — |
| 노년 | **path_hap_16** 30년 전 수첩의 빈칸 (일반) | `hap_path`, `hap_lost_joy` | **A** 빈칸에 오늘 좋았던 일을 적는다. → `burnout_recovered`<br>**B** 옛 취미를 다시 시작한다. (결과 2갈래) → `creative_hobby` `burnout_recovered`<br>**C** 수첩을 마당에서 태운다. → 플래그 없음 | — |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_hap_artist — 쉰 석짜리 무대의 주인공 | 96 | `hap_path`, `achv_solo_show` |
| ending_path_hap_teacher — 복지관 기타 선생님 | 95 | `hap_path`, `achv_senior_teacher` |
| ending_path_hap_content — 천천히 오래 웃은 사람 | 94 | `hap_path`, `content_life`, 없음 `hap_lost_joy` |
| ending_path_hap_burnout — 웃는 법을 잊었던 사람 | 93 | `hap_path`, `hap_lost_joy` |

관련 칭호: 번아웃 생존자(`title_burnout_back`), 늘 웃고 있던 사람(`title_stat_happiness`)

## 운 경로

- 입구(9주차까지 무작위 6%): 유년 운 59 이상의 뽑기 3연속(`path_luk_01`). **10주차** 돌잡이 5만 원을 잡은 아이의 자판기(`cash_grabber` → `path_luk_09`), 어릴 때 나눔이 습관인 아이의 분실물 우산(`sharing_habit` → `path_luk_17`), 잘못 걸려 온 전화(청년 운 61 → `path_luk_10`). 행복 경로·다른 네 경로와는 서로 막는다.
- 기회·손실: 해외 연수, 당첨 축하 문자(사기 → `luk_scammed`), 엘리베이터 안의 30초(전환점), 동창의 투자 제안(전환점).
- 대응·반전: 사기 문자의 두 번째 연락(신고·재피해·예방 강의, `path_luk_12`), 바지 주머니 속 복권(`path_luk_11`), 투자 손실 다음 날(가족·재시작·만회 투자, `path_luk_14`), 공항에서 만난 사람의 메일(직업 변화, `path_luk_13`).
- 노년: 손주와 뽑기 기계(자녀가 있을 때, `path_luk_15`), 경품 추첨 크루즈(`path_luk_16`), 운으로 산 인생(`path_luk_07`).

| 시기 | 사건 | 등장 조건 | 선택지 → 결과 플래그(인생 사실) | 후속 |
| --- | --- | --- | --- | --- |
| 유년 | path_luk_01 문구점 뽑기 3연속 1등 (일반) | 운 ≥ 59, 없음 `hap_path` | **A** 상품 하나를 뒤에 선 아이에게 준다. → `luk_path` `sharing_habit`<br>**B** 한 번 더 뽑는다. (결과 2갈래) → `luk_path` `achv_lucky_streak`(행운의 연속) | path_luk_02 |
| 청소년 | path_luk_02 추첨으로 뽑힌 해외 연수 (일반) | `luk_path` | **A** 여권을 만들고 연수를 간다. → `luk_exchange` `achv_exchange`(해외 연수)<br>**B** 꼭 가고 싶어 하던 친구에게 양보한다. → `helped_friend` | path_luk_03 |
| 청소년·청년 | path_luk_03 당첨 축하 문자 (일반) | `luk_path` | **A** 링크를 누른다. (결과 2갈래) → `achv_lucky_streak`(행운의 연속) `luk_scammed`<br>**B** 번호를 검색해 본다. → `luk_avoided` `special_survived` | path_luk_04 |
| 청소년 | **path_luk_09** 자판기에서 두 캔 (일반) | 운 ≥ 54, `cash_grabber`, 없음 `luk_path`, 없음 `hap_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 한 캔은 뒤에 선 친구에게 준다. → `luk_path` `sharing_habit`<br>**B** 두 캔 다 마신다. (결과 2갈래) → `luk_path`<br>**C** 관리실에 고장 신고를 한다. → 플래그 없음 | path_luk_02, path_luk_03 |
| 청소년 | **path_luk_17** 분실물 보관함의 우산 (일반) | 운 ≥ 54, `sharing_habit`, 없음 `luk_path`, 없음 `hap_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 우산을 쓰고 후배에게 고맙다고 문자한다. (결과 2갈래) → `luk_path` `sharing_habit`<br>**B** 우산이 없는 다른 친구에게 다시 빌려준다. → `luk_path` `helped_friend`<br>**C** 보관함에 그대로 두고 비를 맞는다. → 플래그 없음 | path_luk_02, path_luk_03 |
| 청년 | path_luk_04 엘리베이터 안의 30초 (전환점) | 운 ≥ 64, `luk_path`, 5년 차 고정 | **A** 동네 이야기로 30초를 꽉 채운다. [사회성≥58] (결과 2갈래) → `career_office`(회사원) `job_employed`(재직) `luk_lucky_hire` `level_entry`(신입) `luk_missed`<br>**B** 조용히 면접 답변을 외운다. → `career_office`(회사원) `job_employed`(재직) `level_entry`(신입)<br>**C** 면접 대신 동전 던지기로 진로를 정한다. → `career_freelancer`(프리랜서) `job_self_employed`(자영업) `luk_wanderer` | path_luk_05, path_luk_06 |
| 청년·중년 | path_luk_06 주운 지갑의 주인 (일반) | `luk_path` | **A** 바로 근처 파출소에 맡긴다. (결과 2갈래) → `luk_patron` `achv_good_deed`(지갑 돌려주기)<br>**B** 직접 회사로 찾아간다. → 플래그 없음 | path_luk_07 |
| 청년·중년 | path_luk_08 놓친 비행기 (일반) | `luk_path` | **A** 공항 근처에서 하룻밤 자고 내일 간다. (결과 2갈래) → `luk_airport_contact`<br>**B** 다른 항공사 표를 비싸게 산다. (결과 2갈래) → `luk_airport_contact` `luk_doubt`<br>**C** 출장을 취소하고 집에 간다. → `luk_avoided` `special_survived` | path_luk_05 |
| 청년 | **path_luk_10** 잘못 걸려 온 전화 (일반) | 운 ≥ 61, 없음 `luk_path`, 없음 `hap_path`, 없음 `fit_path`, 없음 `int_path`, 없음 `soc_path`, 없음 `wea_path` | **A** 번호가 틀렸다고 친절하게 알려 준다. (결과 2갈래) → `luk_path` `luk_lucky_hire`<br>**B** 그냥 끊는다. → 플래그 없음<br>**C** 장난으로 사장님인 척한다. (결과 2갈래) → `luk_path` `absurd_event_witness` | path_luk_03, path_luk_06 |
| 청년·중년 | **path_luk_11** 바지 주머니 속 복권 (일반) | `luk_path`, 없음 `luk_windfall` | **A** 말려서 번호를 맞춰 본다. (결과 2갈래) → `luk_small_win`<br>**B** 버리고 잊는다. → 플래그 없음<br>**C** 지갑에 부적처럼 넣고 다닌다. → 플래그 없음 | path_luk_05 |
| 청년·중년 | **path_luk_12** 사기 문자의 두 번째 연락 (일반) | `luk_scammed` | **A** 경찰에 신고하고 피해자 모임을 찾는다. (결과 2갈래) → `luk_avoided` `special_survived` `helped_friend`<br>**B** 이번엔 진짜일지도 모른다. → `luk_loss`<br>**C** 사기 문자 감별법을 동네에 알린다. (결과 2갈래) → `helped_friend` `achv_scam_guard`(사기 예방 강의) | path_luk_05 |
| 중년 | path_luk_05 동창의 투자 제안 (전환점) | 운 ≥ 72, `luk_path` | **A** 모아 둔 돈을 넣는다. [경제력≥35] (결과 2갈래) → `luk_windfall` `achv_windfall`(투자 대박) `luk_loss` `in_debt`(빚 있음)<br>**B** 조금만 넣는다. (결과 2갈래) → `luk_small_win` `luk_small_loss`<br>**C** 술값만 내고 먼저 일어난다. → `luk_avoided` `special_survived` | path_luk_06, path_luk_07 |
| 중년 | **path_luk_13** 공항에서 만난 사람의 메일 (일반) | `luk_airport_contact` | **A** 회사를 그만두고 합류한다. (결과 2갈래) → `career_startup`(스타트업 직원) `job_employed`(재직) `achv_lucky_streak`(행운의 연속) `career_office`(회사원) `comeback`(재취업)<br>**B** 주말에만 돕는다. → `lifelong_friend`<br>**C** 정중히 거절한다. → 플래그 없음 | path_luk_07 |
| 중년·노년 | **path_luk_14** 투자 손실 다음 날 (일반) | `luk_loss` | **A** 가족에게 솔직히 말한다. (결과 2갈래) → `family_bond` `luk_made_peace` `family_conflict`<br>**B** 처음부터 다시 모은다. → `saving_habit`<br>**C** 잃은 돈을 만회하려 다시 투자한다. (결과 2갈래) → `in_debt`(빚 있음) `luk_small_win` | path_luk_07 |
| 노년 | path_luk_07 운으로 산 인생 (일반) | `luk_path` | **A** "운이 좋았지. 근데 그 운을 줍는 법을 배웠어." [luk_windfall] → `achv_lucky_life`(운 좋은 인생)<br>**B** "운이 안 좋을 때가 더 많았어. 그래도 여기까지 왔지." [luk_loss] → `luk_made_peace`<br>**C** "운은 몰라도 사람은 좋았지." → `family_bond` `family_memory` | — |
| 노년 | **path_luk_15** 손주와 뽑기 기계 (일반) | `luk_path`, adult_039 겪음 | **A** 같이 한 번씩 뽑는다. (결과 2갈래) → `family_bond` `family_memory`<br>**B** 운보다 저금이 낫다고 가르친다. → `family_bond`<br>**C** 주머니 동전을 몽땅 손주에게 준다. → `family_memory` | path_luk_07 |
| 노년 | **path_luk_16** 경품 추첨 크루즈 (일반) | `luk_path` | **A** 크루즈를 탄다. (결과 2갈래) → `achv_lucky_streak`(행운의 연속)<br>**B** 가까운 사람에게 양보한다. → `family_bond`<br>**C** 현금으로 바꿔 경로당에 쓴다. → `volunteer` | path_luk_07 |

| 전용 결말 | 우선순위 | 조건 |
| --- | --- | --- |
| ending_path_luk_jackpot — 될 사람은 됐다 | 97 | `luk_windfall` |
| ending_path_luk_patron — 지갑 하나로 바뀐 인생 | 95 | `luk_patron` |
| ending_path_luk_wanderer — 동전 한 닢으로 떠난 사람 | 94 | `luk_wanderer` |
| ending_path_luk_dodger — 아슬아슬 피해 간 인생 | 91 | `luk_path`, `luk_avoided` |
| ending_path_luk_small_win — 적당히 운 좋았던 사람 | 92 | `luk_path`, `luk_small_win` |
| ending_path_luk_loss — 운이 등을 돌렸던 날들 | 93 | `luk_path`, `luk_loss` |

## 교차 사건 12개

미혼에게 이혼·무자녀에게 자녀·학생에게 은퇴 사건이 뜨지 않도록 관계·자녀·고용 상태를 조건으로 건다. 자녀 여부는 `adult_039`(첫 아이)를 겪었는지로 본다 (`childrenAtLeast` 사실 규칙과 같은 기준).

| 사건 | 시기 | 관련 경로 | 등장 조건 | 선택지 → 결과 플래그 | 역할 |
| --- | --- | --- | --- | --- | --- |
| **cross_01** 신혼집 계약서 (일반) | 청년·중년 | WEALTH, SOCIAL | `married`, 없음 `home_owner`, 없음 `housing_jeonse`, 없음 `housing_own_apartment`, 없음 `housing_own_house`, 없음 `housing_with_parents` | **A** 대출을 받아 전세로 시작한다. (결과 2갈래) → `housing_jeonse`(전세) `in_debt`(빚 있음)<br>**B** 월세로 살며 돈을 모은다. → `housing_monthly_rent`(월세) `saving_habit`<br>**C** 부모님 댁 2층에 들어간다. (결과 2갈래) → `housing_with_parents`(부모님 집) `family_bond` `family_conflict` | 관계 상태별 주거(기혼) |
| **cross_02** 혼자 살 집 고르기 (일반) | 청년·중년 | WEALTH, HAPPINESS | `single_life`, 없음 `married`, 없음 `home_owner` | **A** 회사 앞 원룸을 월세로 얻는다. → `housing_monthly_rent`(월세)<br>**B** 대출로 외곽의 작은 아파트를 산다. (결과 2갈래) → `home_owner`(내 집 마련) `housing_own_apartment`(내 집(아파트)) `in_debt`(빚 있음)<br>**C** 셰어하우스에 들어간다. (결과 2갈래) → `housing_monthly_rent`(월세) `lifelong_friend` | 관계 상태별 주거(혼자의 삶) |
| **cross_03** 새벽 3시의 아이 열 (일반) | 청년·중년 | SOCIAL, WEALTH | adult_039 겪음, `job_employed` | **A** 발표를 미루고 아이 곁에 있는다. (결과 2갈래) → `devoted_parent` `family_bond`<br>**B** 부모님께 도움을 청한다. → `family_bond`<br>**C** 오전과 오후를 나눠 맡을 사람을 찾는다. → `family_bond` | 자녀 있음 × 고용 |
| **cross_04** 아이 없는 부부의 일요일 (일반) | 중년 | HAPPINESS, SOCIAL | `married`, 없음 `adult_039 겪음`, 없음 `divorced` | **A** 둘이 새로운 취미를 시작한다. (결과 2갈래) → `content_life`<br>**B** 유기견 보호소 봉사를 다닌다. → `volunteer` `pet_bond`<br>**C** 조카들을 데리고 놀러 간다. → `family_bond` | 자녀 없음 × 관계 |
| **cross_05** 다 큰 아이의 독립 보증금 (일반) | 노년 | SOCIAL, WEALTH | adult_039 겪음 | **A** 보증금 일부를 보태 준다. (결과 2갈래) → `family_bond`<br>**B** 스스로 해 보라고 한다. → 플래그 없음<br>**C** 몇 년 더 같이 살며 돈을 모으자고 한다. → `family_memory` | 자녀 있음 × 노후 자금 |
| **cross_06** 사업 종료 안내 메일 (일반) | 중년 | WEALTH | `job_employed`, 없음 `job_self_employed`, 없음 `job_retired`, 없음 `early_retire`, 없음 `laid_off`, 없음 `job_unemployed` | **A** 바로 재취업 준비를 한다. (결과 2갈래) → `comeback`(재취업) `job_employed`(재직) `job_unemployed`(실직)<br>**B** 동료들과 남은 사업을 이어받아 창업한다. (결과 2갈래) → `career_founder`(창업가) `startup_founder`(창업 경험) `job_self_employed`(자영업) `wea_struggle`<br>**C** 이참에 1년을 쉰다. → `job_unemployed`(실직) | 고용 변동(회사 폐업) |
| **cross_07** 정년 퇴임식 (전환점) | 노년 | WEALTH, HAPPINESS | `job_employed`, 없음 `job_retired`, 없음 `early_retire`, 없음 `job_self_employed`, 1년 차 고정 | **A** 퇴임하고 쉰다. → `job_retired`(은퇴)<br>**B** 계약직으로 2년 더 일한다. [건강≥55] → `job_employed`(재직) `responsibility`<br>**C** 신입 교육 강사로 재능을 기부한다. [사회성≥55] (결과 2갈래) → `job_retired`(은퇴) `helped_friend` `volunteer` | 고용 변동(정년) |
| **cross_08** 재검 날과 실적 발표 날 (일반) | 청년·중년 | HEALTH, WEALTH | `job_employed`, `health_warning` | **A** 발표를 하고 검진은 미룬다. (결과 2갈래) → `overwork` `health_warning`(건강 경고)<br>**B** 검진을 받고 발표는 동료에게 맡긴다. → `healthy_routine`(건강 관리 습관)<br>**C** 발표를 아침으로 당기고 오후에 병원에 간다. (결과 2갈래) → 플래그 없음 | 건강과 성과의 충돌 |
| **cross_09** 주말 특근과 아이 운동회 (일반) | 청년·중년 | SOCIAL, WEALTH | `job_employed`, adult_039 겪음 | **A** 특근을 빼고 운동회에 간다. (결과 2갈래) → `family_bond` `family_memory`<br>**B** 특근을 하고 영상으로 본다. → `overwork`<br>**C** 오전 특근만 하고 오후에 합류한다. → `family_bond` | 자녀 있음 × 일 |
| **cross_10** 이혼 후 첫 명절 (일반) | 중년·노년 | SOCIAL, HAPPINESS | `divorced` | **A** 부모님 댁에 간다. (결과 2갈래) → `family_bond`<br>**B** 친구들과 명절을 보낸다. → `lifelong_friend`<br>**C** 혼자 여행을 떠난다. → `single_life`(혼자의 삶) | 이혼 후 |
| **cross_11** 계단에서 넘어진 엄마 (일반) | 중년 | SOCIAL, HEALTH | 없음 | **A** 부모님 집 근처로 이사한다. (결과 2갈래) → `family_bond`<br>**B** 주말마다 찾아간다. → `family_bond`<br>**C** 형제·친척과 돌봄 순번표를 짠다. (결과 2갈래) → `family_bond` `responsibility` `family_conflict` | 부모 돌봄 |
| **cross_12** 학자금 대출 마지막 회차 (일반) | 청년·중년 | WEALTH, INTELLIGENCE | `student_loan`, 없음 `debt_free` | **A** 마지막 회차를 내고 축하한다. → `debt_free`(빚 청산)<br>**B** 같은 금액을 모교 장학금으로 낸다. → `debt_free`(빚 청산) `helped_friend`<br>**C** 그 돈으로 야간 대학원에 등록한다. (결과 2갈래) → `edu_graduate`(대학원 진학) `overwork` | 학업 × 가계 |

## 신규 사건 ID·이미지 테마 (정원률 님 전달용)

모든 신규 사건은 결정 화면용 1인칭 장면이다 (`view: FIRST_PERSON`, 얼굴 합성 없음). `image_ids.sample.json` 에 ID·설명·파일 이름을 넣었고, `image_routing.json` 의 `eventStatTags` 에 경로 태그를 넣었다. 실제 PNG는 아직 없다 (대체 이미지로 표시).

| 이미지 ID | 사건 | 시기 | 경로 태그 | 장면 설명 |
| --- | --- | --- | --- | --- |
| `img_path_hea_09_001` | 체육 시간의 어지럼증 | 청소년 | health | 보건실 침대에 누워 천장과 수면 기록표를 올려다보는 1인칭 |
| `img_path_hea_10_001` | 입사 첫 주의 야근 문화 | 청년 | health | 불 켜진 사무실 책상들 사이에서 가방 끈을 쥔 1인칭 |
| `img_path_hea_11_001` | 아버지의 당뇨 진단 | 청년 | health | 식탁 위 진단서와 잡채가 수북한 앞접시를 바라보는 1인칭 |
| `img_path_hea_12_001` | 약봉지와 회의 일정 | 중년 | health | 약봉지 세 개와 빽빽한 회의 일정표가 놓인 책상 1인칭 |
| `img_path_hea_13_001` | 완치 판정 1년 뒤 | 중년 | health | 냉장고 속 저염 반찬 통과 프로젝트 제안 메일을 번갈아 보는 1인칭 |
| `img_path_hea_14_001` | 동네 건강 체조 강사 | 노년 | health | 공원 정자 앞에 모인 어르신들을 마주 보고 선 1인칭 |
| `img_path_hea_15_001` | 사전연명의료의향서 | 노년 | health | 보건소 상담 창구의 안내문과 빈 서류를 내려다보는 1인칭 |
| `img_path_hea_16_001` | 가정 시간 요리 실습 | 청소년 | health | 실습실 도마 위 브로콜리와 프라이팬을 내려다보는 1인칭 |
| `img_path_fit_10_001` | 운동부를 그만둔 뒤의 오후 | 청소년 | fitness | 빈 사물함 너머 창밖 운동장을 내다보는 1인칭 |
| `img_path_fit_11_001` | 도장 관장님의 대회 추천 | 청소년 | fitness | 도장 거울 앞에서 검은 띠를 고쳐 매는 관장님 손을 내려다보는 1인칭 |
| `img_path_fit_12_001` | 프로 낙방 다음 날 | 청년 | fitness | 현관에서 운동화 끈을 묶다 휴대폰 문자를 보는 1인칭 |
| `img_path_fit_13_001` | 줄넘기 열 개를 못 넘는 제자 | 청년 | fitness | 체육관 바닥에 엉킨 줄넘기와 아이의 운동화를 내려다보는 1인칭 |
| `img_path_fit_14_001` | 은퇴 경기 제안 | 중년 | fitness | 라커룸 벤치에서 등번호 유니폼을 접어 드는 1인칭 |
| `img_path_fit_15_001` | 학교 스포츠클럽 창단 | 중년 | fitness | 운동장 구석에서 공을 차는 아이들을 교무실 창문으로 내다보는 1인칭 |
| `img_path_fit_16_001` | 비 오는 날 무릎이 먼저 안다 | 노년 | fitness | 빗방울 맺힌 창가 의자에서 무릎을 주무르는 1인칭 |
| `img_path_fit_17_001` | 조기축구회 감독 제안 | 중년 | fitness | 새벽 운동장에서 회원들의 시선이 쏠린 전술판을 든 1인칭 |
| `img_path_fit_18_001` | 체육대회 반 대표 | 청소년 | fitness | 운동장 출발선에서 바통을 건네받으려 손을 뻗은 1인칭 |
| `img_path_int_11_001` | 원하지 않던 학과 | 청년 | intelligence | 대형 강의실 뒷자리에서 손을 반쯤 든 채 멈춘 1인칭 |
| `img_path_int_12_001` | 논문에서 빠진 내 이름 | 청년 | intelligence | 모니터 속 논문 저자 목록을 손가락으로 짚어 내려가는 1인칭 |
| `img_path_int_13_001` | 변리사 시험 두 번째 낙방 | 청년 | intelligence | 독서실 책상에 쌓인 기출문제 더미와 불합격 화면을 보는 1인칭 |
| `img_path_int_14_001` | 번아웃 뒤 다시 연 연구 노트 | 중년 | intelligence | 반쯤 펼친 연구 노트 위에 약봉지가 놓인 책상 1인칭 |
| `img_path_int_15_001` | 취소된 프로젝트의 노트 | 중년 | intelligence | 노트북 화면의 미완성 코드와 정리용 상자를 함께 보는 1인칭 |
| `img_path_int_16_001` | 아이의 수학 숙제 | 중년 | intelligence | 식탁 위 아이의 수학 문제집과 연필을 함께 내려다보는 1인칭 |
| `img_path_int_17_001` | 복지관 스마트폰 교실 | 노년 | intelligence | 복지관 교실 화이트보드에 그린 휴대폰 화면 앞에 선 1인칭 |
| `img_path_int_18_001` | 독학 개발자의 사이드 프로젝트 | 청년 | intelligence | 퇴근길 지하철에서 휴대폰 메모장에 앱 화면을 그리는 1인칭 |
| `img_path_soc_10_001` | 축제가 끝나고 갈라진 무리 | 청소년 | social | 휴대폰 화면에 나란히 뜬 두 단톡방 알림을 보는 1인칭 |
| `img_path_soc_11_001` | 같이 살 집을 구하는 친구들 | 청년 | social | 부동산 창문에 붙은 방 세 개짜리 매물 전단을 셋이 함께 보는 1인칭 |
| `img_path_soc_12_001` | 입사 동기 단톡방의 총무 | 청년 | social | 단톡방 공지에 뜬 총무 인수인계 메시지를 보는 1인칭 |
| `img_path_soc_13_001` | 영업 1등의 슬럼프 | 중년 | social | 0건이 찍힌 실적 화면과 오래된 고객 명함첩을 함께 보는 1인칭 |
| `img_path_soc_14_001` | 상담실 창가의 화분 | 중년 | social | 상담실 창가에 작은 화분을 내려놓는 두 손을 보는 1인칭 |
| `img_path_soc_15_001` | 육아 품앗이 단톡방 | 중년 | social | 아파트 놀이터 벤치에서 요일별 당번표를 펼친 1인칭 |
| `img_path_soc_16_001` | 현관에 쌓인 신문 | 노년 | social | 아파트 복도 현관문 앞에 쌓인 신문 세 부를 내려다보는 1인칭 |
| `img_path_soc_17_001` | 오래된 친구의 부고 | 노년 | social | 휴대폰 화면의 부고 문자와 5년 전에 멈춘 단톡방을 보는 1인칭 |
| `img_path_soc_18_001` | 전학 온 친구의 첫 점심 | 청소년 | social | 급식실 끝자리에 혼자 앉은 전학생 쪽으로 식판을 든 1인칭 |
| `img_path_wea_10_001` | 편의점 야간 알바 제안 | 청소년 | wealth | 편의점 계산대 너머 새벽 시계와 근무표를 보는 1인칭 |
| `img_path_wea_11_001` | 첫 승진 심사 | 청년 | wealth | 빈칸이 하나 남은 승진 심사 서류와 펜을 내려다보는 1인칭 |
| `img_path_wea_12_001` | 실직 석 달째 | 청년 | wealth | 냉장고에 붙은 빨간 숫자의 가계부를 바라보는 1인칭 |
| `img_path_wea_13_001` | 월세에서 전세로 | 청년 | wealth | 은행 앱의 대출 한도 화면과 월세 인상 문자를 번갈아 보는 1인칭 |
| `img_path_wea_14_001` | 아이 학원비 계산서 | 중년 | wealth | 식탁 위에 펼친 학원 고지서 세 장과 계산기를 내려다보는 1인칭 |
| `img_path_wea_15_001` | 임원 제안 | 중년 | wealth | 식당 테이블 너머 대표가 내민 새 명함 시안을 보는 1인칭 |
| `img_path_wea_16_001` | 첫 연금 수령일 | 노년 | wealth | 통장 정리기에서 나온 연금 입금 줄을 손가락으로 짚는 1인칭 |
| `img_path_wea_17_001` | 빚 정리 상담 | 노년 | wealth | 은행 창구 위에 펼쳐진 상환 계획서 세 장을 내려다보는 1인칭 |
| `img_path_hap_09_001` | 예술고 첫 학기 | 청소년 | happiness | 밤늦은 실기실 창가 자리에서 다른 학생들의 뒷모습을 보는 1인칭 |
| `img_path_hap_10_001` | 시험 끝난 날의 스케치북 | 청소년 | happiness | 책상 서랍 속 스케치북과 이어폰을 꺼내 드는 1인칭 |
| `img_path_hap_11_001` | 옷장 위의 기타 케이스 | 청년 | happiness | 이삿짐 상자 사이에서 먼지 쌓인 기타 케이스를 여는 1인칭 |
| `img_path_hap_12_001` | 좋았던 일 하나 적기 | 청년 | happiness | 작은 수첩 첫 장에 삐뚤게 적힌 한 줄을 내려다보는 1인칭 |
| `img_path_hap_13_001` | 합주 날과 부모님 방문 | 중년 | happiness | 달력의 토요일 칸에 겹쳐 적힌 두 일정을 보는 1인칭 |
| `img_path_hap_14_001` | 취미가 일이 된 뒤 | 중년 | happiness | 강습 일정이 빼곡한 달력 옆에 세워 둔 악기를 보는 1인칭 |
| `img_path_hap_15_001` | 주민센터 로비의 첫 개인전 | 노년 | happiness | 거실 바닥에 늘어놓은 상자 열두 개와 그림들을 내려다보는 1인칭 |
| `img_path_hap_16_001` | 30년 전 수첩의 빈칸 | 노년 | happiness | 누렇게 바랜 수첩의 마지막 장과 빈칸을 내려다보는 1인칭 |
| `img_path_luk_09_001` | 자판기에서 두 캔 | 청소년 | luck | 자판기 배출구에 나란히 걸린 콜라 두 캔을 내려다보는 1인칭 |
| `img_path_luk_10_001` | 잘못 걸려 온 전화 | 청년 | luck | 모르는 번호가 뜬 휴대폰 화면을 귀에 대려는 1인칭 |
| `img_path_luk_11_001` | 바지 주머니 속 복권 | 청년 | luck | 세탁기 앞에서 젖은 복권을 손바닥에 펼친 1인칭 |
| `img_path_luk_12_001` | 사기 문자의 두 번째 연락 | 청년 | luck | 휴대폰에 뜬 환급 안내 문자와 몇 년 전 문자를 나란히 보는 1인칭 |
| `img_path_luk_13_001` | 공항에서 만난 사람의 메일 | 중년 | luck | 노트북 화면의 메일과 지갑에서 꺼낸 오래된 명함을 함께 보는 1인칭 |
| `img_path_luk_14_001` | 투자 손실 다음 날 | 중년 | luck | 식탁 위 가계부와 단톡방 알림이 뜬 휴대폰을 보는 1인칭 |
| `img_path_luk_15_001` | 손주와 뽑기 기계 | 노년 | luck | 문구점 뽑기 기계 앞에서 손주의 작은 손에 동전을 쥐여 주는 1인칭 |
| `img_path_luk_16_001` | 경품 추첨 크루즈 | 노년 | luck | 마트 게시판의 당첨자 명단에서 내 이름을 찾은 1인칭 |
| `img_path_luk_17_001` | 분실물 보관함의 우산 | 청소년 | luck | 비 오는 날 분실물 보관함 속 낯익은 우산을 꺼내는 1인칭 |
| `img_cross_01_001` | 신혼집 계약서 | 청년 | wealth, social | 부동산 책상 위에 펼쳐진 계약서 세 장을 내려다보는 1인칭 |
| `img_cross_02_001` | 혼자 살 집 고르기 | 청년 | wealth, happiness | 휴대폰 지도에 표시된 집 후보 세 곳을 확대해 보는 1인칭 |
| `img_cross_03_001` | 새벽 3시의 아이 열 | 청년 | social, wealth | 어두운 방에서 체온계 숫자와 발표 자료 알림을 함께 보는 1인칭 |
| `img_cross_04_001` | 아이 없는 부부의 일요일 | 중년 | happiness, social | 조용한 거실 소파에 나란히 놓인 커피잔 두 개를 보는 1인칭 |
| `img_cross_05_001` | 다 큰 아이의 독립 보증금 | 노년 | social, wealth | 식탁 맞은편에서 조심스럽게 말을 꺼내는 다 큰 아이의 손을 보는 1인칭 |
| `img_cross_06_001` | 사업 종료 안내 메일 | 중년 | wealth | 모니터에 뜬 사업 종료 안내 메일과 조용한 사무실을 보는 1인칭 |
| `img_cross_07_001` | 정년 퇴임식 | 노년 | wealth, happiness | 책상 위 감사패와 꽃다발, 계약 연장 서류를 내려다보는 1인칭 |
| `img_cross_08_001` | 재검 날과 실적 발표 날 | 청년 | health, wealth | 달력의 같은 날짜에 겹쳐 적힌 재검과 발표 일정을 보는 1인칭 |
| `img_cross_09_001` | 주말 특근과 아이 운동회 | 청년 | social, wealth | 냉장고에 붙은 운동회 안내문과 휴대폰의 특근 공지를 함께 보는 1인칭 |
| `img_cross_10_001` | 이혼 후 첫 명절 | 중년 | social, happiness | 명절 기차표 예매 화면과 친구 단톡방을 번갈아 보는 1인칭 |
| `img_cross_11_001` | 계단에서 넘어진 엄마 | 중년 | social, health | 전화를 끊고 지도 앱에서 부모님 집까지의 거리를 보는 1인칭 |
| `img_cross_12_001` | 학자금 대출 마지막 회차 | 청년 | wealth, intelligence | 은행 앱의 마지막 회차 납부 버튼 위에 손가락을 올린 1인칭 |

