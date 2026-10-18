# 8주차 이미지 매핑표

현재 `image_routing.json`, `image_ids.sample.json`, `focuses.sample.json`, `events.sample.json`을 대조한 제출용 스냅샷이다. 실행 시 사용하는 원본은 JSON 연결표이며, 이 문서를 앱에서 읽지는 않는다. 사건·초점 ID나 자산이 변경되면 연결표와 이 표를 함께 갱신한다.

## 공통 규격과 표시 원칙

- 신규 13장: PNG, 1448×1086, 정확한 4:3. 검증 기준은 고정 해상도가 아닌 양의 정수 크기와 `width * 3 == height * 4`다.
- 아래 assetPath는 모두 `app/src/main/assets/images/`를 기준으로 한다. fileName은 경로의 마지막 파일명이다.
- 초상은 공용 캐릭터 한 명의 성장·노화 표현이며 성별·기질별 전용 세트는 아니다. 남녀·기질 선택 상태를 바꾸지 않는다.
- 사건 전용 이미지 → 사건 주제와 일치하는 대표 장면 → 현재 시기 초상 → placeholder 순서로 시도한다. 파일 누락·디코딩 실패 시 다음 후보로 이동한다.
- 사건의 여러 태그 중 `focusStats[현재 초점]`과 일치하는 태그를 우선하고, 일치하지 않으면 첫 태그를 사용한다. 태그 없는 사건에 현재 초점 그림을 무조건 붙이지 않는다.
- 대기 화면에는 시기 초상만 표시한다. 사건 화면의 ‘장면 보기’는 전체 이미지를 4:3 비율로 맞춘다.

## 신규 시기별 초상 6장

| 시기 | imageId | usage | assetPath | 사용 화면 | 최종 대체 문구 |
| --- | --- | --- | --- | --- | --- |
| 영아기 / INFANT | img_portrait_default_infant_001 | character | week8/generic/img_portrait_default_infant_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |
| 유년기 / CHILD | img_portrait_default_child_001 | character | week8/generic/img_portrait_default_child_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |
| 청소년기 / TEEN | img_portrait_default_teen_001 | character | week8/generic/img_portrait_default_teen_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |
| 청년기 / YOUNG_ADULT | img_portrait_default_young_adult_001 | character | week8/generic/img_portrait_default_young_adult_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |
| 중년기 / MIDDLE | img_portrait_default_middle_001 | character | week8/generic/img_portrait_default_middle_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |
| 노년기 / OLD_AGE | img_portrait_default_old_age_001 | character | week8/generic/img_portrait_default_old_age_001.png | 초점 선택·대기·실제 종료 시기 | 현재 시기의 모습을 준비 중입니다 |

## 신규 스탯 대표 장면 7장

| 주제 | statTag | imageId | usage | assetPath | 대체 순서 |
| --- | --- | --- | --- | --- | --- |
| 건강 | health | img_stat_health_001 | scene | week8/generic/img_stat_health_001.png | 시기 초상 → placeholder |
| 운동능력 | fitness | img_stat_fitness_001 | scene | week8/generic/img_stat_fitness_001.png | 시기 초상 → placeholder |
| 지력 | intelligence | img_stat_intelligence_001 | scene | week8/generic/img_stat_intelligence_001.png | 시기 초상 → placeholder |
| 사회성 | social | img_stat_social_001 | scene | week8/generic/img_stat_social_001.png | 시기 초상 → placeholder |
| 경제력 | wealth | img_stat_wealth_001 | scene | week8/generic/img_stat_wealth_001.png | 시기 초상 → placeholder |
| 행복 | happiness | img_stat_happiness_001 | scene | week8/generic/img_stat_happiness_001.png | 시기 초상 → placeholder |
| 운 | luck | img_stat_luck_001 | scene | week8/generic/img_stat_luck_001.png | 시기 초상 → placeholder |

## 보존한 기존 이미지 8장

파일과 ID를 유지했다. 신규 제작 수에 포함하지 않는다. 카탈로그의 폭·높이만 실제 PNG 크기로 정정했다. 초점 장면 3장은 재사용 가능한 원본으로 유지하며, 대기 화면에서 스탯 장면을 다시 표시하기 위한 자산으로 쓰지 않는다.

| imageId / fileName에서 .png 제외 | usage | 실제 크기 | assetPath | 재사용 범위 |
| --- | --- | --- | --- | --- |
| img_pov_exam_paper_001 | pov | 1448×1086 | design_preview/week7/img_pov_exam_paper_001.png | 기말고사 시험지를 내려다보는 1인칭 장면 |
| img_pov_flu_morning_001 | pov | 1448×1086 | design_preview/week7/img_pov_flu_morning_001.png | 몸살로 누운 채 천장과 체온계를 보는 1인칭 장면 |
| img_pov_layoff_door_001 | pov | 1448×1086 | design_preview/week7/img_pov_layoff_door_001.png | 구조조정 통보 날, 사원증을 찍어도 열리지 않는 출입문 앞 1인칭 장면 |
| img_scene_child_friend_001 | scene | 1448×1086 | design_preview/week7/img_scene_child_friend_001.png | CHILD 친구 초점의 3인칭 도트 진행 장면 |
| img_scene_middle_hobby_001 | scene | 1448×1086 | design_preview/week7/img_scene_middle_hobby_001.png | MIDDLE 취미 초점의 3인칭 도트 진행 장면 |
| img_scene_teen_part_time_001 | scene | 1448×1086 | design_preview/week7/img_scene_teen_part_time_001.png | TEEN 아르바이트 초점의 3인칭 도트 진행 장면 |
| img_scene_teen_study_001 | scene | 1448×1086 | design_preview/week7/img_scene_teen_study_001.png | TEEN 학업 초점의 3인칭 도트 진행 장면 |
| img_scene_young_adult_adventure_001 | scene | 1448×1086 | design_preview/week7/img_scene_young_adult_adventure_001.png | YOUNG_ADULT 도전 초점의 3인칭 도트 진행 장면 |

## 초점 30개 → 대표 스탯

이 표는 그림 선택용이다. 초점의 스탯 증감이나 사건 발생 확률을 변경하지 않는다. 실제 선택된 초점 ID를 사용하며 최고 스탯으로 초점을 추정하지 않는다.

| 시기 | focusId | 초점 이름 | 대표 태그 | 대표 imageId |
| --- | --- | --- | --- | --- |
| 영아기 | focus_infant_bonding | 애착 | social | img_stat_social_001 |
| 영아기 | focus_infant_explore | 탐색 | intelligence | img_stat_intelligence_001 |
| 영아기 | focus_infant_rhythm | 생활 리듬 | health | img_stat_health_001 |
| 영아기 | focus_infant_play | 놀이 | happiness | img_stat_happiness_001 |
| 영아기 | focus_infant_health | 건강 | health | img_stat_health_001 |
| 유년기 | focus_child_study | 학업 | intelligence | img_stat_intelligence_001 |
| 유년기 | focus_child_friend | 친구 | social | img_stat_social_001 |
| 유년기 | focus_child_sports | 운동 | fitness | img_stat_fitness_001 |
| 유년기 | focus_child_creative | 상상 놀이 | happiness | img_stat_happiness_001 |
| 유년기 | focus_child_help | 집안일 돕기 | social | img_stat_social_001 |
| 청소년기 | focus_teen_study | 학업 | intelligence | img_stat_intelligence_001 |
| 청소년기 | focus_teen_club | 친구·동아리 | social | img_stat_social_001 |
| 청소년기 | focus_teen_fitness | 운동 | fitness | img_stat_fitness_001 |
| 청소년기 | focus_teen_part_time | 아르바이트 | wealth | img_stat_wealth_001 |
| 청소년기 | focus_teen_rest | 마음 챙기기 | happiness | img_stat_happiness_001 |
| 청년기 | focus_young_adult_career | 커리어 | wealth | img_stat_wealth_001 |
| 청년기 | focus_young_adult_relationship | 관계 | social | img_stat_social_001 |
| 청년기 | focus_young_adult_money | 돈 관리 | wealth | img_stat_wealth_001 |
| 청년기 | focus_young_adult_health | 건강 | health | img_stat_health_001 |
| 청년기 | focus_young_adult_adventure | 도전 | luck | img_stat_luck_001 |
| 중년기 | focus_middle_family | 가족 | social | img_stat_social_001 |
| 중년기 | focus_middle_career_peak | 성과 | wealth | img_stat_wealth_001 |
| 중년기 | focus_middle_health | 건강 | health | img_stat_health_001 |
| 중년기 | focus_middle_hobby | 취미 | happiness | img_stat_happiness_001 |
| 중년기 | focus_middle_community | 동네·모임 | social | img_stat_social_001 |
| 노년기 | focus_old_age_family | 가족과 대화 | social | img_stat_social_001 |
| 노년기 | focus_old_age_health | 건강 관리 | health | img_stat_health_001 |
| 노년기 | focus_old_age_hobby | 취미 생활 | happiness | img_stat_happiness_001 |
| 노년기 | focus_old_age_community | 동네 기록 | intelligence | img_stat_intelligence_001 |
| 노년기 | focus_old_age_reflection | 인생 돌아보기 | happiness | img_stat_happiness_001 |

## 사건 주제 매핑 요약

사건 209개 중 144개에 주제를 기록했다. 나머지 65개는 전용 파일이 없으면 시기 초상으로 대체한다. 아래 표는 전용 파일이 없을 때의 후보를 설명하며, 실제 전용 파일이 있으면 항상 먼저 사용한다. 여러 주제가 있는 사건은 현재 초점과 일치하는 주제를 우선한다.

| 태그 | 연결된 사건 수 |
| --- | --- |
| 건강 / health | 11 |
| 운동능력 / fitness | 16 |
| 지력 / intelligence | 24 |
| 사회성 / social | 46 |
| 경제력 / wealth | 18 |
| 행복 / happiness | 29 |
| 운 / luck | 3 |

다중 태그 사건은 주제별 집계에 중복 포함된다. 사건 전용 이미지 ID의 변경·추가는 `events.sample.json`을 담당하는 팀원과 합의한다. 기존 ID가 유지되고 파일만 준비되면 대표 이미지보다 먼저 표시된다.

## 대표 장면을 연결한 사건 144개

| eventId | 사건 제목 | 전용 imageId | 주제 순서 | 대표 imageId 후보 |
| --- | --- | --- | --- | --- |
| infant_001 | 브로콜리 이유식 | img_infant_baby_food_001 | health | img_stat_health_001 |
| infant_002 | 첫 이가 나는 중 | img_infant_first_tooth_001 | health | img_stat_health_001 |
| infant_007 | 돌잡이 | img_infant_toys_001 | happiness | img_stat_happiness_001 |
| infant_008 | 예방접종 날 | img_infant_vaccine_001 | health | img_stat_health_001 |
| infant_009 | 첫 단어 내기 | img_infant_first_word_001 | social | img_stat_social_001 |
| infant_010 | 첫 걸음마 녹화 중 | img_infant_first_steps_001 | fitness | img_stat_fitness_001 |
| infant_011 | 할머니의 그림책 | img_infant_picture_book_001 | intelligence | img_stat_intelligence_001 |
| infant_013 | 명절 친척 모임 | img_infant_holiday_001 | social | img_stat_social_001 |
| infant_014 | 크레파스와 하얀 벽 | img_infant_crayon_001 | happiness | img_stat_happiness_001 |
| infant_015 | 동요 타임 | img_infant_song_001 | happiness | img_stat_happiness_001 |
| infant_018 | 사촌 형의 방문 | img_infant_cousin_001 | social | img_stat_social_001 |
| infant_019 | 어린이집 첫날 | img_infant_daycare_001 | social | img_stat_social_001 |
| infant_020 | 거실 연주회 | img_infant_concert_001 | happiness | img_stat_happiness_001 |
| infant_023 | 회전목마 | img_infant_carousel_001 | happiness | img_stat_happiness_001 |
| infant_027 | 아빠의 잠자리 동화 | img_infant_bedtime_story_001 | intelligence | img_stat_intelligence_001 |
| infant_028 | 친구의 생일 케이크 | img_infant_birthday_cake_001 | happiness | img_stat_happiness_001 |
| infant_029 | 낮잠 시간 | img_infant_nap_001 | health | img_stat_health_001 |
| child_003 | 반장 선거 | img_child_election_001 | social | img_stat_social_001 |
| child_004 | 학원 고르기 | img_child_academy_001 | intelligence | img_stat_intelligence_001 |
| child_005 | 받아쓰기 전날 | img_child_dictation_001 | intelligence | img_stat_intelligence_001 |
| child_006 | 첫 심부름 | img_child_first_errand_001 | wealth | img_stat_wealth_001 |
| child_008 | 첫 용돈 | img_child_allowance_001 | wealth | img_stat_wealth_001 |
| child_011 | 운동부 종목 고르기 | img_child_sports_pick_001 | fitness | img_stat_fitness_001 |
| child_012 | 문화부 반 고르기 | img_child_culture_pick_001 | happiness | img_stat_happiness_001 |
| child_013 | 예술부 반 고르기 | img_child_arts_pick_001 | happiness | img_stat_happiness_001 |
| child_014 | 강아지 입양 | img_child_puppy_001 | happiness | img_stat_happiness_001 |
| child_016 | 밀린 방학 일기 | img_child_diary_001 | intelligence | img_stat_intelligence_001 |
| child_017 | 운동회 계주 | img_child_relay_001 | fitness | img_stat_fitness_001 |
| child_018 | 응원단장 스카우트 | img_child_cheer_leader_001 | social | img_stat_social_001 |
| child_019 | 과학의 날 | img_child_science_day_001 | intelligence | img_stat_intelligence_001 |
| child_021 | 독서왕 후보 | img_child_library_001 | intelligence | img_stat_intelligence_001 |
| child_022 | 비밀 기지 | img_child_secret_base_001 | happiness | img_stat_happiness_001 |
| child_023 | 급식의 시금치 | img_child_lunch_001 | health | img_stat_health_001 |
| child_024 | 여름 수영 강습 | img_child_swimming_001 | fitness | img_stat_fitness_001 |
| child_025 | 혼자 밥 먹는 전학생 | img_child_new_student_001 | social | img_stat_social_001 |
| child_026 | 지호의 이사 | img_child_friend_moving_001 | social | img_stat_social_001 |
| child_028 | 누가 날 부른다 | img_child_class_nap_001 | intelligence | img_stat_intelligence_001 |
| child_029 | 종료 3초 전 | img_child_basketball_001 | fitness | img_stat_fitness_001 |
| child_030 | 수영 대회 결승 | img_child_swim_meet_001 | fitness | img_stat_fitness_001 |
| child_031 | 과학 경진대회 | img_child_science_fair_001 | intelligence | img_stat_intelligence_001 |
| child_032 | 숙제 토론 대회 | img_child_debate_001 | social | img_stat_social_001 |
| child_033 | 어린이 바둑 대회 | img_child_baduk_001 | intelligence | img_stat_intelligence_001 |
| child_034 | 사생대회 | img_child_drawing_contest_001 | happiness | img_stat_happiness_001 |
| child_035 | 합창 대회 솔로 | img_child_choir_001 | happiness | img_stat_happiness_001 |
| child_036 | 학예회의 나무 3 | img_child_school_play_001 | happiness | img_stat_happiness_001 |
| child_037 | 결승전 승부차기 | img_child_penalty_001 | fitness | img_stat_fitness_001 |
| child_038 | 피아노 콩쿠르 | img_child_piano_contest_001 | happiness | img_stat_happiness_001 |
| child_039 | 여름휴가 투표 | img_child_vacation_001 | happiness | img_stat_happiness_001 |
| child_040 | 구구단 7단 | img_child_times_table_001 | intelligence | img_stat_intelligence_001 |
| child_041 | 그네 차례 | img_child_swing_line_001 | social | img_stat_social_001 |
| child_042 | 보조 바퀴 떼는 날 | img_child_bicycle_001 | fitness | img_stat_fitness_001 |
| child_044 | 체험학습 버스 맨 뒷자리 | img_child_bus_trip_001 | social | img_stat_social_001 |
| child_045 | 학급 신문 기자 | img_child_class_newspaper_001 | social | img_stat_social_001 |
| teen_003 | 중학교 동아리 | img_teen_club_001 | social | img_stat_social_001 |
| teen_005 | 짝 바꾸는 날 | img_teen_new_seatmate_001 | social | img_stat_social_001 |
| teen_006 | 점심 방송 사고 | img_teen_broadcast_001 | social | img_stat_social_001 |
| teen_007 | 첫 중간고사 | img_teen_midterm_001 | intelligence | img_stat_intelligence_001 |
| teen_009 | 오래달리기 측정 | img_teen_running_001 | fitness | img_stat_fitness_001 |
| teen_011 | 단톡방의 사진 | img_teen_group_chat_001 | social | img_stat_social_001 |
| teen_012 | 축제 무대 사고 | img_teen_band_stage_001 | happiness | img_stat_happiness_001 |
| teen_013 | 청소년 해커톤 | img_teen_hackathon_001 | intelligence | img_stat_intelligence_001 |
| teen_014 | 교내 토론대회 | img_teen_debate_001 | social | img_stat_social_001 |
| teen_015 | 방학 아르바이트 | img_teen_part_time_001 | wealth | img_stat_wealth_001 |
| teen_016 | 부모님 결혼기념일 | img_teen_parents_anniversary_001 | social | img_stat_social_001 |
| teen_019 | 고등학교 원서 | img_teen_high_school_001 | intelligence | img_stat_intelligence_001 |
| teen_020 | 청소년 대표 선발 | img_teen_tryout_001 | fitness | img_stat_fitness_001 |
| teen_021 | 축제와 짝사랑 | img_teen_crush_001 | social | img_stat_social_001 |
| teen_022 | 수학여행의 밤 | img_teen_school_trip_001 | happiness | img_stat_happiness_001 |
| teen_023 | 문과냐 이과냐 | img_teen_career_001 | intelligence | img_stat_intelligence_001 |
| teen_024 | 선생님의 오답 | img_teen_wrong_answer_001 | intelligence | img_stat_intelligence_001 |
| teen_025 | 편의점의 진상 손님 | img_teen_convenience_store_001 | wealth | img_stat_wealth_001 |
| teen_026 | 새벽의 메시지 | img_teen_midnight_message_001 | social | img_stat_social_001 |
| teen_028 | 성적표 사인 | img_teen_report_card_001 | intelligence | img_stat_intelligence_001 |
| teen_030 | 반티 투표 | img_teen_class_shirt_001 | social | img_stat_social_001 |
| teen_031 | 스터디 카페 | img_teen_study_cafe_001 | intelligence | img_stat_intelligence_001 |
| teen_032 | 숙제냐 생일 파티냐 | img_teen_party_or_homework_001 | intelligence → social → happiness | img_stat_intelligence_001 / img_stat_social_001 / img_stat_happiness_001 |
| teen_034 | 셀프 염색 | img_teen_hair_dye_001 | happiness | img_stat_happiness_001 |
| teen_036 | 학생회장 선거 공약 | img_teen_student_council_001 | social | img_stat_social_001 |
| teen_037 | 수능 D-100 | img_teen_d100_001 | intelligence | img_stat_intelligence_001 |
| teen_038 | 수능 날 아침 | img_teen_exam_day_001 | intelligence | img_stat_intelligence_001 |
| teen_040 | 야자 시간의 배고픔 | img_teen_night_study_001 | intelligence | img_stat_intelligence_001 |
| teen_041 | 아무 일 없는 주말 | img_teen_free_weekend_001 | happiness | img_stat_happiness_001 |
| teen_042 | 기말고사 1교시 | img_pov_exam_paper_001 | intelligence | img_stat_intelligence_001 |
| adult_002 | 프로 구단의 제안 | img_adult_pro_offer_001 | fitness | img_stat_fitness_001 |
| adult_003 | 음이탈 레전드 | img_adult_audition_001 | happiness | img_stat_happiness_001 |
| adult_005 | 이상형 발견 | img_adult_ideal_type_001 | social | img_stat_social_001 |
| adult_006 | 이상형과 눈이 마주쳤다 | img_adult_ideal_type_star_001 | social | img_stat_social_001 |
| adult_007 | 금요일 밤 술자리 | img_adult_friends_drinks_001 | social | img_stat_social_001 |
| adult_008 | 이상형 옆자리 | img_adult_drinks_ideal_001 | social | img_stat_social_001 |
| adult_009 | 건배사를 해라 | img_adult_drinks_toast_001 | social | img_stat_social_001 |
| adult_010 | 고기 장인의 밤 | img_adult_drinks_grill_001 | social | img_stat_social_001 |
| adult_012 | 학점과 동아리 | img_adult_campus_001 | social | img_stat_social_001 |
| adult_013 | 최종 면접 | img_adult_interview_001 | wealth | img_stat_wealth_001 |
| adult_014 | 첫 출근길 | img_adult_first_commute_001 | wealth | img_stat_wealth_001 |
| adult_015 | 첫 월급 | img_adult_first_salary_001 | wealth | img_stat_wealth_001 |
| adult_016 | 회식 자리 배치 | img_adult_dinner_seats_001 | social | img_stat_social_001 |
| adult_017 | 팀장님의 잔 | img_adult_dinner_boss_001 | social | img_stat_social_001 |
| adult_019 | 문 앞 자리의 기회 | img_adult_dinner_door_001 | luck | img_stat_luck_001 |
| adult_020 | 첫 회식 | img_adult_company_dinner_001 | social | img_stat_social_001 |
| adult_021 | 회식 다음 날 아침 | img_adult_hangover_001 | health | img_stat_health_001 |
| adult_023 | 길을 묻는 사람 | img_adult_asking_way_001 | social → luck | img_stat_social_001 / img_stat_luck_001 |
| adult_024 | 거실 사무실 창업 | img_adult_startup_001 | wealth | img_stat_wealth_001 |
| adult_025 | 특허가 될지도 모르는 아이디어 | img_adult_patent_001 | intelligence | img_stat_intelligence_001 |
| adult_026 | 이번엔 진짜라는 친구 | img_adult_crypto_001 | wealth | img_stat_wealth_001 |
| adult_027 | 운동 필요 | img_adult_gym_001 | fitness | img_stat_fitness_001 |
| adult_028 | 첫 차 | img_adult_first_car_001 | wealth | img_stat_wealth_001 |
| adult_029 | 휴가 사유란 | img_adult_leave_form_001 | happiness | img_stat_happiness_001 |
| adult_030 | 베프의 결혼식 | img_adult_wedding_speech_001 | social | img_stat_social_001 |
| adult_031 | 결혼식 사회 부탁 | img_adult_wedding_mc_001 | social | img_stat_social_001 |
| adult_032 | 결혼 이야기 | img_adult_proposal_001 | social | img_stat_social_001 |
| adult_033 | 아빠와 팔씨름 | img_adult_arm_wrestling_001 | fitness | img_stat_fitness_001 |
| adult_034 | 주말 아침 플로깅 | img_adult_plogging_001 | fitness | img_stat_fitness_001 |
| adult_035 | 10년 만의 동창회 | img_adult_reunion_001 | social | img_stat_social_001 |
| adult_036 | 20년 만의 타임캡슐 | img_adult_time_capsule_001 | happiness | img_stat_happiness_001 |
| adult_037 | 불 꺼지지 않는 회사 | img_adult_job_offer_001 | wealth | img_stat_wealth_001 |
| adult_038 | 번아웃 | img_adult_burnout_001 | happiness | img_stat_happiness_001 |
| adult_039 | 첫 아이 | img_adult_newborn_001 | social | img_stat_social_001 |
| adult_040 | 30년 만의 의뢰 | img_adult_mural_001 | happiness | img_stat_happiness_001 |
| adult_041 | 30년 만기 대출 | img_adult_house_001 | wealth | img_stat_wealth_001 |
| adult_043 | 엄마의 수술 | img_adult_hospital_001 | health | img_stat_health_001 |
| adult_045 | 첫 집들이 | img_adult_housewarming_001 | social | img_stat_social_001 |
| adult_046 | 수상한 전화 | img_adult_scam_call_001 | wealth | img_stat_wealth_001 |
| adult_047 | 애매한 사이의 청첩장 | img_adult_wedding_invite_001 | social | img_stat_social_001 |
| adult_048 | 절대 안 죽는 화분 | img_adult_office_plant_001 | happiness | img_stat_happiness_001 |
| adult_049 | 중고 거래 첫 문의 | img_adult_secondhand_001 | wealth | img_stat_wealth_001 |
| adult_050 | 워크숍 장기자랑 | img_adult_talent_show_001 | happiness | img_stat_happiness_001 |
| adult_051 | 아빠의 칠순 | img_adult_father_70th_001 | social | img_stat_social_001 |
| adult_053 | 팔이 짧아졌다 | img_adult_reading_glasses_001 | health | img_stat_health_001 |
| adult_055 | 닫힌 방문 | img_adult_teen_child_001 | social | img_stat_social_001 |
| adult_056 | 희망퇴직 공고 | img_adult_early_retire_001 | wealth | img_stat_wealth_001 |
| adult_057 | 퇴직하면 치킨집? | img_adult_second_career_001 | wealth | img_stat_wealth_001 |
| adult_058 | 재검 통지서 | img_adult_health_recheck_001 | health | img_stat_health_001 |
| adult_060 | 늦게 배운 덕질 | img_adult_fandom_001 | happiness | img_stat_happiness_001 |
| adult_061 | 생애 첫 10km | img_adult_marathon_001 | fitness | img_stat_fitness_001 |
| adult_062 | 50년 지기 | img_adult_lifelong_friend_001 | social | img_stat_social_001 |
| adult_063 | 빈 방 | img_adult_empty_nest_001 | social | img_stat_social_001 |
| adult_064 | 부모님의 봄 | img_adult_parents_spring_001 | social | img_stat_social_001 |
| adult_067 | 일요일 밤 11시 | img_adult_sunday_night_001 | happiness | img_stat_happiness_001 |
| adult_068 | 온몸이 두들겨 맞은 아침 | img_pov_flu_morning_001 | health | img_stat_health_001 |
| adult_069 | 승진 심사 전날 | img_pov_promotion_review_001 | wealth | img_stat_wealth_001 |
| adult_070 | 구조조정 통보 | img_pov_layoff_door_001 | wealth | img_stat_wealth_001 |
| special_006 | 꿈속의 정답 | img_special_dream_answer_001 | luck | img_stat_luck_001 |
| special_009 | 너무 평범하게 건강한 사람 | img_special_health_research_001 | health | img_stat_health_001 |
| special_016 | 피구공의 궤적 | img_special_dodgeball_001 | fitness | img_stat_fitness_001 |

## 주제 미지정 사건 65개

제목·스탯 증감만으로 자동 분류하지 않았다. 개편 시나리오에서 주제를 확정하면 `eventStatTags`에 명시적으로 추가한다. 아래 사건도 전용 파일이 있으면 그 파일을 사용한다.

| eventId | 사건 제목 | 전용 imageId | 전용 이미지 누락 시 |
| --- | --- | --- | --- |
| infant_003 | 리모컨의 비밀 | img_infant_remote_001 | 해당 사건 시기의 초상 → placeholder |
| infant_004 | 신생아실 최고 목청 | img_infant_loud_cry_001 | 해당 사건 시기의 초상 → placeholder |
| infant_005 | 5분 컷 목욕 | img_infant_bath_001 | 해당 사건 시기의 초상 → placeholder |
| infant_006 | 새벽 1시의 자장가 | img_infant_night_001 | 해당 사건 시기의 초상 → placeholder |
| infant_012 | 공원의 비둘기 떼 | img_infant_park_001 | 해당 사건 시기의 초상 → placeholder |
| infant_016 | 거꾸로가 좋아 | img_infant_upside_down_001 | 해당 사건 시기의 초상 → placeholder |
| infant_017 | 놀이터 모래밭 | img_infant_sandbox_001 | 해당 사건 시기의 초상 → placeholder |
| infant_021 | 첫 미용실 | img_infant_first_haircut_001 | 해당 사건 시기의 초상 → placeholder |
| infant_022 | 오리 변기 | img_infant_potty_001 | 해당 사건 시기의 초상 → placeholder |
| infant_024 | 엘리베이터 버튼 | img_infant_elevator_button_001 | 해당 사건 시기의 초상 → placeholder |
| infant_025 | 할머니 댁 마당의 닭 | img_infant_rooster_001 | 해당 사건 시기의 초상 → placeholder |
| infant_026 | 볼풀에서 잃어버린 신발 | img_infant_ball_pit_001 | 해당 사건 시기의 초상 → placeholder |
| infant_030 | 장난감 정리 시간 | img_infant_cleanup_001 | 해당 사건 시기의 초상 → placeholder |
| child_001 | 방과 후 선택 | img_child_school_001 | 해당 사건 시기의 초상 → placeholder |
| child_002 | 엄마의 호출 | img_child_mom_calling_001 | 해당 사건 시기의 초상 → placeholder |
| child_007 | 장래 희망 발표 | img_child_dream_001 | 해당 사건 시기의 초상 → placeholder |
| child_009 | 산타의 정체 | img_child_christmas_001 | 해당 사건 시기의 초상 → placeholder |
| child_010 | 동아리 신청서 | img_child_club_001 | 해당 사건 시기의 초상 → placeholder |
| child_015 | 사라진 지우개 | img_child_eraser_001 | 해당 사건 시기의 초상 → placeholder |
| child_020 | 고장 난 시계 해부 | img_child_clock_001 | 해당 사건 시기의 초상 → placeholder |
| child_027 | 첫 스마트폰 계약서 | img_child_smartphone_001 | 해당 사건 시기의 초상 → placeholder |
| child_043 | 학교 앞 병아리 | img_child_chick_001 | 해당 사건 시기의 초상 → placeholder |
| child_046 | 쉬는 시간 10분 | img_child_break_time_001 | 해당 사건 시기의 초상 → placeholder |
| child_047 | 하굣길 문방구 | img_child_stationery_001 | 해당 사건 시기의 초상 → placeholder |
| teen_001 | 처음 입는 교복 | img_teen_uniform_001 | 해당 사건 시기의 초상 → placeholder |
| teen_002 | 거꾸로 입은 교복 | img_teen_backwards_uniform_001 | 해당 사건 시기의 초상 → placeholder |
| teen_004 | 급식 새치기 | img_teen_lunch_line_001 | 해당 사건 시기의 초상 → placeholder |
| teen_008 | 방 청소 잔소리 | img_teen_room_001 | 해당 사건 시기의 초상 → placeholder |
| teen_010 | 비 오는 하굣길 | img_teen_rainy_day_001 | 해당 사건 시기의 초상 → placeholder |
| teen_017 | 스크린 타임 경고 | img_teen_screen_time_001 | 해당 사건 시기의 초상 → placeholder |
| teen_018 | 아픈 콩이 | img_teen_sick_dog_001 | 해당 사건 시기의 초상 → placeholder |
| teen_027 | 주차장의 아빠 | img_teen_father_car_001 | 해당 사건 시기의 초상 → placeholder |
| teen_029 | 4교시 매점 달리기 | img_teen_school_store_001 | 해당 사건 시기의 초상 → placeholder |
| teen_033 | 졸업앨범 촬영 날 | img_teen_yearbook_photo_001 | 해당 사건 시기의 초상 → placeholder |
| teen_035 | 종점에서 눈을 떴다 | img_teen_bus_terminal_001 | 해당 사건 시기의 초상 → placeholder |
| teen_039 | 졸업식 날 | img_teen_graduation_001 | 해당 사건 시기의 초상 → placeholder |
| adult_001 | 대학이냐 취업이냐 | img_adult_crossroad_001 | 해당 사건 시기의 초상 → placeholder |
| adult_004 | 자취 첫 주 | img_adult_living_alone_001 | 해당 사건 시기의 초상 → placeholder |
| adult_011 | 열여섯 살 콩이 | img_adult_old_dog_001 | 해당 사건 시기의 초상 → placeholder |
| adult_018 | 동기와 탈출 작전 | img_adult_dinner_escape_001 | 해당 사건 시기의 초상 → placeholder |
| adult_022 | 밤 11시의 카톡 | img_adult_late_message_001 | 해당 사건 시기의 초상 → placeholder |
| adult_042 | 이삿짐 속 상자 | img_adult_childhood_box_001 | 해당 사건 시기의 초상 → placeholder |
| adult_044 | 도로 주행 시험 | img_adult_driving_test_001 | 해당 사건 시기의 초상 → placeholder |
| adult_052 | 마흔 번째 생일 | img_adult_birthday_001 | 해당 사건 시기의 초상 → placeholder |
| adult_054 | 회의 중 꿀잠 | img_adult_meeting_nap_001 | 해당 사건 시기의 초상 → placeholder |
| adult_059 | 키오스크 앞에서 | img_adult_kiosk_001 | 해당 사건 시기의 초상 → placeholder |
| adult_065 | 쉰 번째 생일 | img_adult_fiftieth_birthday_001 | 해당 사건 시기의 초상 → placeholder |
| adult_066 | 퇴근길 편의점 | img_adult_convenience_night_001 | 해당 사건 시기의 초상 → placeholder |
| adult_071 | 입영 통지서 | img_pov_enlistment_letter_001 | 해당 사건 시기의 초상 → placeholder |
| special_001 | 시식 코너의 유혹 | img_special_dumpling_001 | 해당 사건 시기의 초상 → placeholder |
| special_002 | 강아지의 새끼가 되었다 | img_special_dog_carry_001 | 해당 사건 시기의 초상 → placeholder |
| special_003 | 낙엽 속의 장수말벌 | img_special_hornet_001 | 해당 사건 시기의 초상 → placeholder |
| special_004 | 끝나지 않는 미역 | img_special_seaweed_001 | 해당 사건 시기의 초상 → placeholder |
| special_005 | 나를 거부하는 자동문 | img_special_auto_door_001 | 해당 사건 시기의 초상 → placeholder |
| special_007 | 뚜껑 닫힌 물병 | img_special_water_bottle_001 | 해당 사건 시기의 초상 → placeholder |
| special_008 | 엘리베이터 속 사장님 | img_special_elevator_001 | 해당 사건 시기의 초상 → placeholder |
| special_010 | 공원 벤치의 장기 고수 | img_special_janggi_001 | 해당 사건 시기의 초상 → placeholder |
| special_011 | 등산로의 멧돼지 | img_special_boar_001 | 해당 사건 시기의 초상 → placeholder |
| special_012 | 로봇청소기 납치 사건 | img_special_robot_vacuum_001 | 해당 사건 시기의 초상 → placeholder |
| special_013 | 비눗방울 대참사 | img_special_bubble_001 | 해당 사건 시기의 초상 → placeholder |
| special_014 | 미끄럼틀 정전기 | img_special_static_slide_001 | 해당 사건 시기의 초상 → placeholder |
| special_015 | 날아가는 탕수육 소스 | img_special_sauce_001 | 해당 사건 시기의 초상 → placeholder |
| special_017 | 멈춘 엘리베이터 | img_special_stuck_elevator_001 | 해당 사건 시기의 초상 → placeholder |
| special_018 | 회전문 무한 루프 | img_special_revolving_door_001 | 해당 사건 시기의 초상 → placeholder |
| special_019 | 중고 안마의자의 역습 | img_special_massage_chair_001 | 해당 사건 시기의 초상 → placeholder |
