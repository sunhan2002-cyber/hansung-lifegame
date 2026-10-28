# 실제 이미지 manifest

2026-10-29. 이미지 카탈로그 **522 ID**, 실제 라우팅 그림 **45장**(기존 21 + 신규 장면 24). 성장 몸 18개와 외형 파생 PNG는 장면 수에 포함하지 않는다.

## 장면 24장

일반 배경 6, 경로 사건 14(7경로 각각 2), 종료 대체 4. 24개 SHA-256이 모두 다르다. 이번 재작업에서 유산 분배(path_wea_09)·시험과 취미(path_hap_10) 두 장을 최신 사건 문맥에 맞춰 교체했다. 교체 전 그림은 수량에서 제외한다.

출처: 내장 OpenAI 이미지 생성·편집 도구. 외부 사진·사용자 사진은 입력하지 않았다. 원본·프롬프트·변경 이력은 generated_scene_sources.json, 파일 해시는 week10_scene_manifest.json에 있다. 원본은 output/week10/originals/, 앱 파일은 아래 경로에 있다. 경로는 저장소 루트 기준.

|imageId|실제 앱 파일|사건/용도|크기|중복|출처|
|---|---|---|---|---|---|
|img_week10_bg_infant|app/src/main/assets/images/week10/scenes/img_week10_bg_infant.png|bg_infant: bg_infant|300×225|없음|imagegen|
|img_week10_bg_child|app/src/main/assets/images/week10/scenes/img_week10_bg_child.png|bg_child: bg_child|300×225|없음|imagegen|
|img_week10_bg_teen|app/src/main/assets/images/week10/scenes/img_week10_bg_teen.png|bg_teen: bg_teen|300×225|없음|imagegen|
|img_week10_bg_young_adult|app/src/main/assets/images/week10/scenes/img_week10_bg_young_adult.png|bg_young_adult: bg_young_adult|300×225|없음|imagegen|
|img_week10_bg_middle|app/src/main/assets/images/week10/scenes/img_week10_bg_middle.png|bg_middle: bg_middle|300×225|없음|imagegen|
|img_week10_bg_old_age|app/src/main/assets/images/week10/scenes/img_week10_bg_old_age.png|bg_old_age: bg_old_age|300×225|없음|imagegen|
|img_path_hea_09_001|app/src/main/assets/images/week10/scenes/img_path_hea_09_001.png|path_hea_09: 체육 시간의 어지럼증|300×225|없음|imagegen|
|img_path_hea_10_001|app/src/main/assets/images/week10/scenes/img_path_hea_10_001.png|path_hea_10: 입사 첫 주의 야근 문화|300×225|없음|imagegen|
|img_path_fit_09_001|app/src/main/assets/images/week10/scenes/img_path_fit_09_001.png|path_fit_09: 시니어 마라톤 출발선|300×225|없음|imagegen|
|img_path_fit_10_001|app/src/main/assets/images/week10/scenes/img_path_fit_10_001.png|path_fit_10: 운동부를 그만둔 뒤의 오후|300×225|없음|imagegen|
|img_path_int_09_001|app/src/main/assets/images/week10/scenes/img_path_int_09_001.png|path_int_09: 평생 연구의 마지막 퍼즐|300×225|없음|imagegen|
|img_path_int_10_001|app/src/main/assets/images/week10/scenes/img_path_int_10_001.png|path_int_10: 마지막 강의|300×225|없음|imagegen|
|img_path_soc_09_001|app/src/main/assets/images/week10/scenes/img_path_soc_09_001.png|path_soc_09: 칠순 잔치의 방명록|300×225|없음|imagegen|
|img_path_soc_10_001|app/src/main/assets/images/week10/scenes/img_path_soc_10_001.png|path_soc_10: 축제가 끝나고 갈라진 무리|300×225|없음|imagegen|
|img_path_wea_09_001|app/src/main/assets/images/week10/scenes/img_path_wea_09_001.png|path_wea_09: 통장 정리하는 날|300×225|없음|imagegen|
|img_path_wea_10_001|app/src/main/assets/images/week10/scenes/img_path_wea_10_001.png|path_wea_10: 편의점 야간 알바 제안|300×225|없음|imagegen|
|img_path_hap_09_001|app/src/main/assets/images/week10/scenes/img_path_hap_09_001.png|path_hap_09: 예술고 첫 학기|300×225|없음|imagegen|
|img_path_hap_10_001|app/src/main/assets/images/week10/scenes/img_path_hap_10_001.png|path_hap_10: 시험 끝난 날의 스케치북|300×225|없음|imagegen|
|img_path_luk_09_001|app/src/main/assets/images/week10/scenes/img_path_luk_09_001.png|path_luk_09: 자판기에서 두 캔|300×225|없음|imagegen|
|img_path_luk_10_001|app/src/main/assets/images/week10/scenes/img_path_luk_10_001.png|path_luk_10: 잘못 걸려 온 전화|300×225|없음|imagegen|
|img_week10_ending_achievement|app/src/main/assets/images/week10/scenes/img_week10_ending_achievement.png|ending_achievement: ending_achievement|300×225|없음|imagegen|
|img_week10_ending_relationship|app/src/main/assets/images/week10/scenes/img_week10_ending_relationship.png|ending_relationship: ending_relationship|300×225|없음|imagegen|
|img_week10_ending_peace|app/src/main/assets/images/week10/scenes/img_week10_ending_peace.png|ending_peace: ending_peace|300×225|없음|imagegen|
|img_week10_ending_health|app/src/main/assets/images/week10/scenes/img_week10_ending_health.png|ending_health: ending_health|300×225|없음|imagegen|

## 최신 main 대비 신규 사건 매핑

현재 콘텐츠 352사건·1009선택지·66결말. origin/main 대비 신규 사건은 71개이며, 아래에서 전용과 테마 대체를 구분한다. 대체를 전용 그림 수로 가산하지 않는다.

후보 순서: 명시 사건 imageId/기존 파일명 → 사건에 맞는 스탯 테마 → 시기 배경 → 기존 시기 초상 → placeholder. 기본 초상 6 ID와 스탯 7 ID를 보존했다. 종료 대체는 endingScenes의 achievement/relationship/peace/health이며 특정 학교·직업·가족 수를 사실로 확정하는 사진이 아니다.

|사건 ID|실제 제목|적용|
|---|---|---|
|path_hea_09|체육 시간의 어지럼증|전용: img_path_hea_09_001|
|path_hea_10|입사 첫 주의 야근 문화|전용: img_path_hea_10_001|
|path_hea_11|아버지의 당뇨 진단|테마 대체: health|
|path_hea_12|약봉지와 회의 일정|테마 대체: health|
|path_hea_13|완치 판정 1년 뒤|테마 대체: health|
|path_hea_14|동네 건강 체조 강사|테마 대체: health|
|path_hea_15|사전연명의료의향서|테마 대체: health|
|path_hea_16|가정 시간 요리 실습|테마 대체: health|
|path_fit_10|운동부를 그만둔 뒤의 오후|전용: img_path_fit_10_001|
|path_fit_11|도장 관장님의 대회 추천|테마 대체: fitness|
|path_fit_12|프로 낙방 다음 날|테마 대체: fitness|
|path_fit_13|줄넘기 열 개를 못 넘는 제자|테마 대체: fitness|
|path_fit_14|은퇴 경기 제안|테마 대체: fitness|
|path_fit_15|학교 스포츠클럽 창단|테마 대체: fitness|
|path_fit_16|비 오는 날 무릎이 먼저 안다|테마 대체: fitness|
|path_fit_17|조기축구회 감독 제안|테마 대체: fitness|
|path_fit_18|체육대회 반 대표|테마 대체: fitness|
|path_int_11|원하지 않던 학과|테마 대체: intelligence|
|path_int_12|논문에서 빠진 내 이름|테마 대체: intelligence|
|path_int_13|변리사 시험 두 번째 낙방|테마 대체: intelligence|
|path_int_14|번아웃 뒤 다시 연 연구 노트|테마 대체: intelligence|
|path_int_15|취소된 프로젝트의 노트|테마 대체: intelligence|
|path_int_16|아이의 수학 숙제|테마 대체: intelligence|
|path_int_17|복지관 스마트폰 교실|테마 대체: intelligence|
|path_int_18|독학 개발자의 사이드 프로젝트|테마 대체: intelligence|
|path_soc_10|축제가 끝나고 갈라진 무리|전용: img_path_soc_10_001|
|path_soc_11|같이 살 집을 구하는 친구들|테마 대체: social|
|path_soc_12|입사 동기 단톡방의 총무|테마 대체: social|
|path_soc_13|영업 1등의 슬럼프|테마 대체: social|
|path_soc_14|상담실 창가의 화분|테마 대체: social|
|path_soc_15|육아 품앗이 단톡방|테마 대체: social|
|path_soc_16|현관에 쌓인 신문|테마 대체: social|
|path_soc_17|오래된 친구의 부고|테마 대체: social|
|path_soc_18|전학 온 친구의 첫 점심|테마 대체: social|
|path_wea_10|편의점 야간 알바 제안|전용: img_path_wea_10_001|
|path_wea_11|첫 승진 심사|테마 대체: wealth|
|path_wea_12|실직 석 달째|테마 대체: wealth|
|path_wea_13|월세에서 전세로|테마 대체: wealth|
|path_wea_14|아이 학원비 계산서|테마 대체: wealth|
|path_wea_15|임원 제안|테마 대체: wealth|
|path_wea_16|첫 연금 수령일|테마 대체: wealth|
|path_wea_17|빚 정리 상담|테마 대체: wealth|
|path_hap_09|예술고 첫 학기|전용: img_path_hap_09_001|
|path_hap_10|시험 끝난 날의 스케치북|전용: img_path_hap_10_001|
|path_hap_11|옷장 위의 기타 케이스|테마 대체: happiness|
|path_hap_12|좋았던 일 하나 적기|테마 대체: happiness|
|path_hap_13|합주 날과 부모님 방문|테마 대체: happiness|
|path_hap_14|취미가 일이 된 뒤|테마 대체: happiness|
|path_hap_15|주민센터 로비의 첫 개인전|테마 대체: happiness|
|path_hap_16|30년 전 수첩의 빈칸|테마 대체: happiness|
|path_luk_09|자판기에서 두 캔|전용: img_path_luk_09_001|
|path_luk_10|잘못 걸려 온 전화|전용: img_path_luk_10_001|
|path_luk_11|바지 주머니 속 복권|테마 대체: luck|
|path_luk_12|사기 문자의 두 번째 연락|테마 대체: luck|
|path_luk_13|공항에서 만난 사람의 메일|테마 대체: luck|
|path_luk_14|투자 손실 다음 날|테마 대체: luck|
|path_luk_15|손주와 뽑기 기계|테마 대체: luck|
|path_luk_16|경품 추첨 크루즈|테마 대체: luck|
|path_luk_17|분실물 보관함의 우산|테마 대체: luck|
|cross_01|신혼집 계약서|테마 대체: wealth, social|
|cross_02|혼자 살 집 고르기|테마 대체: wealth, happiness|
|cross_03|새벽 3시의 아이 열|테마 대체: social, wealth|
|cross_04|아이 없는 부부의 일요일|테마 대체: happiness, social|
|cross_05|다 큰 아이의 독립 보증금|테마 대체: social, wealth|
|cross_06|사업 종료 안내 메일|테마 대체: wealth|
|cross_07|정년 퇴임식|테마 대체: wealth, happiness|
|cross_08|재검 날과 실적 발표 날|테마 대체: health, wealth|
|cross_09|주말 특근과 아이 운동회|테마 대체: social, wealth|
|cross_10|이혼 후 첫 명절|테마 대체: social, happiness|
|cross_11|계단에서 넘어진 엄마|테마 대체: social, health|
|cross_12|학자금 대출 마지막 회차|테마 대체: wealth, intelligence|

## 캐릭터 소재

현재 앱은 avatar_v4만 사용한다. **몸 18 + 피부/얼굴/노출 피부 54 + 눈 18 + 머리 24 + 얼굴 예제 시트 1 = PNG 115개**다. 이는 고유 삽화 115개라는 뜻이 아니다. 몸의 독립 생성 원본은 18개이며, 얼굴 바탕 6·머리 분리 편집 4·눈 분리 편집 3을 사용한다. 얼굴 시트는 기본 외형 합성 예제로 실제 사진 생성 결과가 아니다.

제작·분리 편집은 imagegen, 자르기·최근접 배치·피부와 백발의 팔레트 치환은 package_natural_avatars.py다. 얼굴+머리 통합 PNG 교체 방식은 최종 앱에서 제외했다. 레이어와 ID 계약은 avatar_asset_contract.md, 원본/프롬프트는 avatar_v4_sources.json(deliveredLayerSource=true)으로 추적한다.

|실제 파일|imageId/선택 키|용도|크기|SHA-256 중복|출처|
|---|---|---|---|---|---|
|app/src/main/assets/images/avatar_v4/body/body_child_muscular.png|avatar_body: body_child_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_child_slim.png|avatar_body: body_child_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_child_standard.png|avatar_body: body_child_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_infant_muscular.png|avatar_body: body_infant_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_infant_slim.png|avatar_body: body_infant_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_infant_standard.png|avatar_body: body_infant_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_middle_muscular.png|avatar_body: body_middle_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_middle_slim.png|avatar_body: body_middle_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_middle_standard.png|avatar_body: body_middle_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_old_age_muscular.png|avatar_body: body_old_age_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_old_age_slim.png|avatar_body: body_old_age_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_old_age_standard.png|avatar_body: body_old_age_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_teen_muscular.png|avatar_body: body_teen_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_teen_slim.png|avatar_body: body_teen_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_teen_standard.png|avatar_body: body_teen_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_young_adult_muscular.png|avatar_body: body_young_adult_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_young_adult_slim.png|avatar_body: body_young_adult_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/body/body_young_adult_standard.png|avatar_body: body_young_adult_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/examples/face_sheet_example.png|example: face_sheet_example|분리 레이어/팔레트 파생/규격 예제|1152×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_child_eyes_big.png|avatar_eyes: eyes_child_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_child_eyes_default.png|avatar_eyes: eyes_child_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_child_eyes_narrow.png|avatar_eyes: eyes_child_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_infant_eyes_big.png|avatar_eyes: eyes_infant_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_infant_eyes_default.png|avatar_eyes: eyes_infant_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_infant_eyes_narrow.png|avatar_eyes: eyes_infant_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_middle_eyes_big.png|avatar_eyes: eyes_middle_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_middle_eyes_default.png|avatar_eyes: eyes_middle_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_middle_eyes_narrow.png|avatar_eyes: eyes_middle_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_old_age_eyes_big.png|avatar_eyes: eyes_old_age_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_old_age_eyes_default.png|avatar_eyes: eyes_old_age_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_old_age_eyes_narrow.png|avatar_eyes: eyes_old_age_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_teen_eyes_big.png|avatar_eyes: eyes_teen_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_teen_eyes_default.png|avatar_eyes: eyes_teen_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_teen_eyes_narrow.png|avatar_eyes: eyes_teen_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_young_adult_eyes_big.png|avatar_eyes: eyes_young_adult_eyes_big|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_young_adult_eyes_default.png|avatar_eyes: eyes_young_adult_eyes_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/eyes_young_adult_eyes_narrow.png|avatar_eyes: eyes_young_adult_eyes_narrow|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_child_hair_bob_brown.png|avatar_hair: hair_child_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_child_hair_curly_brown.png|avatar_hair: hair_child_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_child_hair_default.png|avatar_hair: hair_child_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_child_hair_long_black.png|avatar_hair: hair_child_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_infant_hair_bob_brown.png|avatar_hair: hair_infant_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_infant_hair_curly_brown.png|avatar_hair: hair_infant_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_infant_hair_default.png|avatar_hair: hair_infant_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_infant_hair_long_black.png|avatar_hair: hair_infant_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_middle_hair_bob_brown.png|avatar_hair: hair_middle_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_middle_hair_curly_brown.png|avatar_hair: hair_middle_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_middle_hair_default.png|avatar_hair: hair_middle_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_middle_hair_long_black.png|avatar_hair: hair_middle_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_old_age_hair_bob_brown.png|avatar_hair: hair_old_age_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_old_age_hair_curly_brown.png|avatar_hair: hair_old_age_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_old_age_hair_default.png|avatar_hair: hair_old_age_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_old_age_hair_long_black.png|avatar_hair: hair_old_age_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_teen_hair_bob_brown.png|avatar_hair: hair_teen_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_teen_hair_curly_brown.png|avatar_hair: hair_teen_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_teen_hair_default.png|avatar_hair: hair_teen_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_teen_hair_long_black.png|avatar_hair: hair_teen_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_young_adult_hair_bob_brown.png|avatar_hair: hair_young_adult_hair_bob_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_young_adult_hair_curly_brown.png|avatar_hair: hair_young_adult_hair_curly_brown|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_young_adult_hair_default.png|avatar_hair: hair_young_adult_hair_default|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/hair_young_adult_hair_long_black.png|avatar_hair: hair_young_adult_hair_long_black|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_deep_muscular.png|avatar_skin: skin_child_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_deep_slim.png|avatar_skin: skin_child_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_deep_standard.png|avatar_skin: skin_child_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_default_muscular.png|avatar_skin: skin_child_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_default_slim.png|avatar_skin: skin_child_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_default_standard.png|avatar_skin: skin_child_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_tan_muscular.png|avatar_skin: skin_child_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_tan_slim.png|avatar_skin: skin_child_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_child_skin_tan_standard.png|avatar_skin: skin_child_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_deep_muscular.png|avatar_skin: skin_infant_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_deep_slim.png|avatar_skin: skin_infant_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_deep_standard.png|avatar_skin: skin_infant_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_default_muscular.png|avatar_skin: skin_infant_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_default_slim.png|avatar_skin: skin_infant_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_default_standard.png|avatar_skin: skin_infant_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_tan_muscular.png|avatar_skin: skin_infant_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_tan_slim.png|avatar_skin: skin_infant_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_infant_skin_tan_standard.png|avatar_skin: skin_infant_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_deep_muscular.png|avatar_skin: skin_middle_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_deep_slim.png|avatar_skin: skin_middle_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_deep_standard.png|avatar_skin: skin_middle_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_default_muscular.png|avatar_skin: skin_middle_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_default_slim.png|avatar_skin: skin_middle_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_default_standard.png|avatar_skin: skin_middle_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_tan_muscular.png|avatar_skin: skin_middle_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_tan_slim.png|avatar_skin: skin_middle_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_middle_skin_tan_standard.png|avatar_skin: skin_middle_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_deep_muscular.png|avatar_skin: skin_old_age_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_deep_slim.png|avatar_skin: skin_old_age_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_deep_standard.png|avatar_skin: skin_old_age_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_default_muscular.png|avatar_skin: skin_old_age_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_default_slim.png|avatar_skin: skin_old_age_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_default_standard.png|avatar_skin: skin_old_age_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_tan_muscular.png|avatar_skin: skin_old_age_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_tan_slim.png|avatar_skin: skin_old_age_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_old_age_skin_tan_standard.png|avatar_skin: skin_old_age_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_deep_muscular.png|avatar_skin: skin_teen_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_deep_slim.png|avatar_skin: skin_teen_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_deep_standard.png|avatar_skin: skin_teen_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_default_muscular.png|avatar_skin: skin_teen_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_default_slim.png|avatar_skin: skin_teen_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_default_standard.png|avatar_skin: skin_teen_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_tan_muscular.png|avatar_skin: skin_teen_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_tan_slim.png|avatar_skin: skin_teen_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_teen_skin_tan_standard.png|avatar_skin: skin_teen_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_deep_muscular.png|avatar_skin: skin_young_adult_skin_deep_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_deep_slim.png|avatar_skin: skin_young_adult_skin_deep_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_deep_standard.png|avatar_skin: skin_young_adult_skin_deep_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_default_muscular.png|avatar_skin: skin_young_adult_skin_default_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_default_slim.png|avatar_skin: skin_young_adult_skin_default_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_default_standard.png|avatar_skin: skin_young_adult_skin_default_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_tan_muscular.png|avatar_skin: skin_young_adult_skin_tan_muscular|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_tan_slim.png|avatar_skin: skin_young_adult_skin_tan_slim|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
|app/src/main/assets/images/avatar_v4/face/skin_young_adult_skin_tan_standard.png|avatar_skin: skin_young_adult_skin_tan_standard|분리 레이어/팔레트 파생|192×312|없음|생성 원본의 분리·배치|
