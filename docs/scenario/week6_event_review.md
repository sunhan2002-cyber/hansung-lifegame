# 6주차 시나리오 사건 리뷰

- 담당: 신기훈 (시나리오·콘텐츠)
- 대상 파일: `app/src/main/assets/events.sample.json`, `app/src/main/assets/image_ids.sample.json`
- 비교 기준: 6주차 시작 시점 main(사건 150개) → 현재(사건 204개)
- 관련 PR: #16 (정지 칸 다듬기, 지표 조정, 50대 확장), 이번 PR (사건 보완 40개, 이 문서)

## 1. 6주차 기준 충족 여부

| 기준 | 결과 | 판정 |
| --- | --- | --- |
| 사건 50개 이상 추가 | 54개 추가 (일반 45 + 돌발 9) | 충족 |
| 총 사건 200개 이상 | 204개 | 충족 |
| 돌발 이벤트 8개 이상 (이번 주 추가) | 9개 추가 (전체 19개) | 충족 |
| 시나리오 리뷰 문서 | `docs/scenario/week6_event_review.md` (이 문서) | 충족 |

## 2. 이번 주 변경 요약

1. **정지 칸 다듬기 (#16)**: 정지 칸 7개 모두 선택지 3개, 결과 문장에 '이후 인생' 한 줄, 고등학교 원서에 예술고 선택지 추가
2. **지표 오르는 폭 조정 (#16)**: 양수 `statDelta`만 0.7배 (반올림, 최소 1)
3. **50대 확장 (#16)**: 성인기 후반 사건 13개 + 돌발 1개, 마지막 정지 칸을 '쉰 번째 생일'로 이동
4. **사건 보완 (이번 PR)**: 일반 32개 + 돌발 8개. 사건 수가 적던 유아기·청소년기 위주로 채움

## 3. 단계별 사건 수

| 단계 | 6주차 시작 | 이번 주 추가 (일반) | 현재 (일반) |
| --- | ---: | ---: | ---: |
| 유아기 (INFANT) | 22 | 8 | 30 |
| 아동기 (CHILD) | 41 | 6 | 47 |
| 청소년기 (TEEN) | 31 | 10 | 41 |
| 성인기 (ADULT) | 46 | 21 | 67 |
| 돌발 이벤트 | 10 | 9 | 19 |
| **합계** | **150** | **54** | **204** (선택지 594개) |

## 4. 이번 주 추가한 일반 사건 (45개)

### 유아기 (8개)

| ID | 제목 | 등장 조건 | 선택지 | 얻는 경험 | PR |
| --- | --- | --- | ---: | --- | --- |
| infant_021 | 첫 미용실 | 없음 | 3 | - | 이번 PR |
| infant_022 | 오리 변기 | 없음 | 3 | `potty_trained` | 이번 PR |
| infant_023 | 회전목마 | 없음 | 2 | - | 이번 PR |
| infant_024 | 엘리베이터 버튼 | 없음 | 3 | - | 이번 PR |
| infant_025 | 할머니 댁 마당의 닭 | 없음 | 3 | `rooster_friend` | 이번 PR |
| infant_026 | 볼풀에서 잃어버린 신발 | 없음 | 3 | - | 이번 PR |
| infant_027 | 아빠의 잠자리 동화 | 없음 | 3 | `storyteller` | 이번 PR |
| infant_028 | 친구의 생일 케이크 | 없음 | 3 | - | 이번 PR |

### 아동기 (6개)

| ID | 제목 | 등장 조건 | 선택지 | 얻는 경험 | PR |
| --- | --- | --- | ---: | --- | --- |
| child_040 | 구구단 7단 | 없음 | 3 | - | 이번 PR |
| child_041 | 그네 차례 | 없음 | 3 | - | 이번 PR |
| child_042 | 보조 바퀴 떼는 날 | 없음 | 3 | `bike_rider` | 이번 PR |
| child_043 | 학교 앞 병아리 | 없음 | 3 | `raised_chick` | 이번 PR |
| child_044 | 체험학습 버스 맨 뒷자리 | 없음 | 3 | - | 이번 PR |
| child_045 | 학급 신문 기자 | 없음 | 3 | `young_reporter` | 이번 PR |

### 청소년기 (10개)

| ID | 제목 | 등장 조건 | 선택지 | 얻는 경험 | PR |
| --- | --- | --- | ---: | --- | --- |
| teen_028 | 성적표 사인 | 없음 | 3 | - | 이번 PR |
| teen_029 | 4교시 매점 달리기 | 없음 | 3 | - | 이번 PR |
| teen_030 | 반티 투표 | 없음 | 3 | `dino_class` | 이번 PR |
| teen_031 | 스터디 카페 | 없음 | 3 | - | 이번 PR |
| teen_032 | 숙제냐 생일 파티냐 | 없음 | 3 | - | 이번 PR |
| teen_033 | 졸업앨범 촬영 날 | 없음 | 3 | - | 이번 PR |
| teen_034 | 셀프 염색 | 없음 | 3 | - | 이번 PR |
| teen_035 | 종점에서 눈을 떴다 | 없음 | 3 | - | 이번 PR |
| teen_036 | 학생회장 선거 공약 | 없음 | 3 | `campaign_crew` | 이번 PR |
| teen_038 | 수능 날 아침 | 제외 `vocational_high` | 3 | `exam_finished` `family_bond` | 이번 PR |

### 성인기 (21개)

| ID | 제목 | 등장 조건 | 선택지 | 얻는 경험 | PR |
| --- | --- | --- | ---: | --- | --- |
| adult_044 | 도로 주행 시험 | 없음 | 3 | `driver_license` | 이번 PR |
| adult_045 | 첫 집들이 | 없음 | 3 | - | 이번 PR |
| adult_046 | 수상한 전화 | 없음 | 3 | `family_bond` | 이번 PR |
| adult_047 | 애매한 사이의 청첩장 | 없음 | 3 | - | 이번 PR |
| adult_048 | 절대 안 죽는 화분 | 없음 | 3 | `plant_parent` | 이번 PR |
| adult_049 | 중고 거래 첫 문의 | 없음 | 3 | - | 이번 PR |
| adult_050 | 워크숍 장기자랑 | 없음 | 3 | - | 이번 PR |
| adult_051 | 아빠의 칠순 | 없음 | 3 | `family_bond` | 이번 PR |
| adult_053 | 팔이 짧아졌다 | 없음 | 3 | `reading_glasses` | #16 |
| adult_054 | 회의 중 꿀잠 | `class_nap_mom` | 3 | - | #16 |
| adult_055 | 닫힌 방문 | `married` | 3 | `family_bond` | #16 |
| adult_056 | 희망퇴직 공고 | 없음 | 3 | `early_retire` | #16 |
| adult_057 | 퇴직하면 치킨집? | `early_retire` | 3 | `chicken_shop` `small_cafe` | #16 |
| adult_058 | 재검 통지서 | 없음 | 3 | `health_scare` | #16 |
| adult_059 | 키오스크 앞에서 | 없음 | 3 | - | #16 |
| adult_060 | 늦게 배운 덕질 | 없음 | 3 | `fan_life` | #16 |
| adult_061 | 생애 첫 10km | 없음 | 3 | `marathon` `volunteer` | #16 |
| adult_062 | 50년 지기 | `jiho_reunion` | 3 | `lifelong_friend` | #16 |
| adult_063 | 빈 방 | `married` | 3 | `empty_nest` | #16 |
| adult_064 | 부모님의 봄 | 없음 | 3 | `family_bond` `parents_last_trip` | #16 |
| adult_065 | 쉰 번째 생일 | 없음 | 3 | `bucket_list` `family_bond` `second_half` | #16 |

## 5. 돌발 이벤트 (전체 19개, ★ = 이번 주 추가)

| ID | 단계 | 제목 | 결과 경험 |
| --- | --- | --- | --- |
| special_001 | 유아기 | 시식 코너의 유혹 | `survived_special` `special_injury` |
| special_002 | 유아기 | 강아지의 새끼가 되었다 | `survived_special` `special_injury` `pet_bond` |
| special_003 | 아동기 | 낙엽 속의 장수말벌 | `survived_special` `death_crisis` |
| special_004 | 아동기 | 끝나지 않는 미역 | `survived_special` `death_crisis` `special_injury` |
| special_005 | 청소년기 | 나를 거부하는 자동문 | `survived_special` `auto_door_legend` |
| special_006 | 청소년기 | 꿈속의 정답 | `survived_special` `weird_rumor` |
| special_007 | 성인기 | 뚜껑 닫힌 물병 | `survived_special` `interview_fail` |
| special_008 | 성인기 | 엘리베이터 속 사장님 | `special_injury` `survived_special` |
| special_009 | 성인기 | 너무 평범하게 건강한 사람 | `survived_special` `research_subject` |
| special_010 | 성인기 | 공원 벤치의 장기 고수 | `survived_special` `janggi_mentor` |
| special_011 | 성인기 | ★ 등산로의 멧돼지 | `survived_special` `special_injury` |
| special_012 | 유아기 | ★ 로봇청소기 납치 사건 | `survived_special` `special_injury` |
| special_013 | 유아기 | ★ 비눗방울 대참사 | `survived_special` `special_injury` |
| special_014 | 아동기 | ★ 미끄럼틀 정전기 | `survived_special` `special_injury` |
| special_015 | 아동기 | ★ 날아가는 탕수육 소스 | `survived_special` `special_injury` |
| special_016 | 청소년기 | ★ 피구공의 궤적 | `survived_special` `special_injury` |
| special_017 | 청소년기 | ★ 멈춘 엘리베이터 | `survived_special` |
| special_018 | 성인기 | ★ 회전문 무한 루프 | `survived_special` `special_injury` |
| special_019 | 성인기 | ★ 중고 안마의자의 역습 | `survived_special` `special_injury` |

## 6. 정지 칸 (7개)

| ID | 단계 | 제목 | 선택지 |
| --- | --- | --- | --- |
| infant_007 | 유아기 | 돌잡이 | 피아노 장난감을 잡는다. / 판사봉을 잡는다. / 5만 원권을 잡는다. |
| child_004 | 아동기 | 학원 고르기 | 피아노. / 태권도. / "나 그냥 놀래." |
| teen_019 | 청소년기 | 고등학교 원서 | 인문계. / 특성화고. / 예술고 실기 시험에 도전한다. |
| teen_039 | 청소년기 | 졸업식 날 | 친구들한테 간다. / 가족이랑 고깃집에 간다. / 친구들을 전부 고깃집에 데려간다. |
| adult_001 | 성인기 | 대학이냐 취업이냐 | 대학. / 취업. / 1년 쉬면서 생각해 본다. |
| adult_032 | 성인기 | 결혼 이야기 | 결혼한다. / 파견부터 다녀온다. / "같이 파견 가자." |
| adult_065 | 성인기 | 쉰 번째 생일 | "앞으로 하고 싶은 것 50개"를 적는다. / 가족과 사진을 찍는다. / 소원: "앞으로 50년 더!" |

## 7. 복선·연결 정리 (이번 주 추가분)

| 앞 사건 (경험) | 이어지는 사건 |
| --- | --- |
| 누가 날 부른다 → "엄마!" (`class_nap_mom`) | 회의 중 꿀잠 (사장님: "그래, 엄마다.") |
| 20년 만의 타임캡슐 (`jiho_reunion`) | 50년 지기 |
| 희망퇴직 공고 (`early_retire`) | 퇴직하면 치킨집? |
| 결혼 이야기 (`married`) | 닫힌 방문, 빈 방 |
| 고등학교 원서 → 특성화고 (`vocational_high`) | 수능 날 아침 **제외** |
| 고등학교 원서 → 예술고 (`arts_high`) | 문과냐 이과냐 **제외** |

## 8. 지표 균형

시뮬레이션 조건: 정지 칸 반드시 포함, 단계별 사건 수 4 / 6 / 6 / 9, 돌발 이벤트 단계마다 40% 확률, 무작위 선택, 5,000판

| 항목 | 지표 조정 전 (150개) | 현재 (204개) |
| --- | ---: | ---: |
| 한 지표라도 100에 닿는 판 | 3.7% | 0% |
| 한 지표라도 90 이상인 판 | 26.2% | 1.8% |

새 사건도 조정된 기준(오르는 값 1~4, 정지 칸 최대 +5)에 맞춰 썼습니다.

## 9. 검증 결과

| 구분 | 항목 | 결과 |
| --- | --- | --- |
| 일반 | JSON 파싱 | 성공 |
| 일반 | 사건 / 선택지 / 돌발 | 204 / 594 / 19 |
| 위험 | eventId 중복 | 없음 |
| 위험 | 모든 사건 A/B 선택지 | 존재 |
| 애매 | fallback 단계별 2개 이상 | 충족 (각 2개) |
| 추가 | 정지 칸 단계별 1개 이상, 등장 조건 없음 | 충족 |
| 추가 | 이미지 ID 누락 (`image_ids.sample.json`) | 0건 |
| 추가 | 금지 형식 (`requiredStats`, 한국어 단계명, 소문자 type, 7개 외 지표) | 0건 |
| 추가 | 일반 사건의 모든 선택지에 이득과 대가가 함께 있음 | 충족 |
| 빌드 | `./gradlew test` | BUILD SUCCESSFUL (43개 통과, 실패 0) |
| 빌드 | `./gradlew assembleDebug` | BUILD SUCCESSFUL |

## 10. 리뷰 체크리스트 (팀장 검토용)

- [ ] 문장이 화면에서 2~3줄 안에 들어가는가 (상황 1~2문장, 결과 1~3문장)
- [ ] 나이에 맞지 않는 상황이 없는가 (유아기에 경제력 변화 없음, 진로·돈은 청소년기 이후)
- [ ] 플레이어 성별을 가정한 표현이 없는가 (이상형, 부모님 호칭 등 중립 표현 사용)
- [ ] 욕설·과한 표현이 없는가 (술자리 사건은 모든 단계에 마시지 않는 선택지 있음)
- [ ] `{name}` 바로 뒤에 조사가 붙지 않았는가 (검증 스크립트로 확인, 0건)
- [ ] 돌발 이벤트가 실제 위험을 장난처럼 다루지 않는가

## 11. 확인 필요 / 남은 일

1. **게임 로직**: 지금 `GameEngine`은 단계당 5개(`EVENTS_PER_STAGE = 5`)를 `priority` → `eventId` 순으로 고릅니다. 그래서 정지 칸 일부와 대부분의 사건이 실제 플레이에 나오지 않습니다. 정지 칸 규칙(`isMilestone`)과 단계별 사건 수 4 / 6 / 6 / 9 반영이 필요합니다.
2. **결말 연결**: `endings.sample.json`이 조건으로 쓰는 경험 15개(`study_focus`, `club_joined`, `family_memory` 등)가 시나리오에 없는 이름입니다. 시나리오 쪽 경험 이름을 맞출지, 결말 조건을 바꿀지 DB 담당과 정해야 합니다.
3. **이미지**: 이미지 ID는 모두 목록에 있지만 실제 그림 파일은 아직 없습니다 (`fallbackText`로 대체 표시).
4. **보류**: 돌발 이벤트 결과를 지표로 판정하는 기능은 회의 결정으로 보류 중입니다.
