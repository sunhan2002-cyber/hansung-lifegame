# 7주차 도트 이미지 요청서

- 담당: 신기훈 (시나리오·콘텐츠)
- 대상: 7주차 대표 확률 사건 5개(결정 화면 1인칭 + 주마등) 10장, 시기별 초점 진행 장면 5장 — 총 15장
- 관련 요구사항: UI-05 도트풍 통일, DR-03 이미지 ID, FR-15 주마등
- 모든 imageId는 `image_ids.sample.json`에 등록되어 있다 (15 / 15)

## 1. 공통 규격 (`docs/qa/week7_image_asset_checklist.md` 기준)

| 항목 | 기준 |
| --- | --- |
| 파일 | PNG, 300×225 px (4:3), 파일명 = `imageId.png` |
| 화풍 | 16비트 레트로 게임 도트. 실사·3D·웹툰풍·수채화풍 금지 |
| 1인칭 (pov) | 결정/돌발 화면. 내 손과 시야만 보이고 내 얼굴은 나오지 않는다 |
| 3인칭 (scene) | 자동 진행 화면. 작은 전신 캐릭터와 배경이 함께 보인다 |
| 회상 (moment) | 주마등 썸네일. 작게 줄여도 어떤 사건인지 알아볼 수 있게 단순하게, 바랜 색감 |
| 주인공 외형 | 성별이 드러나지 않는 중립 외형 (성별 설정 확정 전) |
| 모든 이미지 공통 금지 | 글자·숫자·워터마크·로고, 실제 브랜드/학교/회사/기관 마크, 실사·3D·웹툰풍 |
| 위험 장면 | 다치거나 아픈 장면은 직접적으로 그리지 않고 상황(이불, 약봉지 등)으로 표현 |

## 2. 요청 목록

| # | imageId | 연결 | 시점 | usage |
| ---: | --- | --- | --- | --- |
| 1 | `img_pov_exam_paper_001` | teen_042 기말고사 1교시 | 1인칭 | pov |
| 2 | `img_moment_exam_paper_001` | teen_042 주마등 | 3인칭 (회상) | moment |
| 3 | `img_pov_flu_morning_001` | adult_068 온몸이 두들겨 맞은 아침 | 1인칭 | pov |
| 4 | `img_moment_flu_morning_001` | adult_068 주마등 | 3인칭 (회상) | moment |
| 5 | `img_pov_promotion_review_001` | adult_069 승진 심사 전날 | 1인칭 | pov |
| 6 | `img_moment_promotion_review_001` | adult_069 주마등 | 3인칭 (회상) | moment |
| 7 | `img_pov_layoff_door_001` | adult_070 구조조정 통보 | 1인칭 | pov |
| 8 | `img_moment_layoff_door_001` | adult_070 주마등 | 3인칭 (회상) | moment |
| 9 | `img_pov_enlistment_letter_001` | adult_071 입영 통지서 | 1인칭 | pov |
| 10 | `img_moment_enlistment_letter_001` | adult_071 주마등 | 3인칭 (회상) | moment |
| 11 | `img_scene_child_friend_001` | 초점 유년기 · 친구 | 3인칭 | scene |
| 12 | `img_scene_teen_study_001` | 초점 청소년기 · 학업 | 3인칭 | scene |
| 13 | `img_scene_teen_part_time_001` | 초점 청소년기 · 아르바이트 | 3인칭 | scene |
| 14 | `img_scene_young_adult_adventure_001` | 초점 청년기 · 도전 | 3인칭 | scene |
| 15 | `img_scene_middle_hobby_001` | 초점 중년기 · 취미 | 3인칭 | scene |

## 3. 장면별 요청

### 1. `img_pov_exam_paper_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | teen_042 기말고사 1교시 |
| 장면 종류 | 1인칭 |
| 장면 설명 | 교실 책상 위에 뒤집힌 시험지와 연필을 쥔 내 손. 앞자리 친구의 등과 칠판 위 시계가 흐릿하게 보인다. 긴장되지만 무섭지 않은 낮 교실 분위기. |
| 등장하면 안 되는 요소 | 시험지 속 실제 문제·정답 글씨, 커닝 장면, 교복 학교 마크 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, first-person view from a student desk, hands holding a pencil over a face-down exam sheet, classmate's back in front, wall clock above blackboard, soft daylight classroom, muted warm palette, 4:3, no text, no numbers, no logo, no watermark
```

### 2. `img_moment_exam_paper_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | teen_042 주마등 |
| 장면 종류 | 3인칭 (회상) |
| 장면 설명 | 교복 차림 주인공이 복도 창가에서 성적표를 들고 서 있는 작은 장면. 표정은 결과를 알 수 없게 옆모습. |
| 등장하면 안 되는 요소 | 성적표 숫자·글씨, 우는/좌절한 얼굴 클로즈업 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro style, small third-person scene, teenage student in school uniform standing by a hallway window holding a report card, side view, nostalgic faded sepia tint, simple background, 4:3, no text, no numbers, no logo, no watermark
```

### 3. `img_pov_flu_morning_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_068 온몸이 두들겨 맞은 아침 |
| 장면 종류 | 1인칭 |
| 장면 설명 | 침대에 누워 올려다본 천장. 화면 가장자리가 살짝 흐릿하고, 이불 밖으로 나온 손에 체온계, 머리맡에 울리는 휴대폰 알람. |
| 등장하면 안 되는 요소 | 피, 주사기 클로즈업, 응급실·구급차, 해골 등 사망 암시 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, first-person view lying in bed looking up at the ceiling, blurry vignette edges, hand holding a thermometer out of the blanket, smartphone alarm glowing on the nightstand, early morning bluish light, 4:3, no text, no numbers, no logo, no watermark
```

### 4. `img_moment_flu_morning_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_068 주마등 |
| 장면 종류 | 3인칭 (회상) |
| 장면 설명 | 이불을 김밥처럼 둘둘 만 주인공이 침대에 누워 있고, 옆 탁자에 약봉지와 물컵. 살짝 우스운 분위기. |
| 등장하면 안 되는 요소 | 병원 침대·링거, 고통스러운 표정 클로즈업 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro style, small third-person scene, person wrapped tightly in a blanket like a burrito on a bed, medicine pouch and glass of water on the side table, cozy and slightly comical mood, faded nostalgic tint, 4:3, no text, no logo, no watermark
```

### 5. `img_pov_promotion_review_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_069 승진 심사 전날 |
| 장면 종류 | 1인칭 |
| 장면 설명 | 밤 책상 위에 펼친 보고서와 노트북, 식은 커피. 스탠드 불빛 아래 내 두 손이 보고서를 정리 중. 창밖은 도시 야경. |
| 등장하면 안 되는 요소 | 보고서·화면 속 글씨와 그래프 숫자, 실제 회사 로고 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, first-person view at a desk at night, hands arranging a report next to a laptop and a cold coffee mug, desk lamp warm light, city night view through window, 4:3, no readable text, no numbers, no logo, no watermark
```

### 6. `img_moment_promotion_review_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_069 주마등 |
| 장면 종류 | 3인칭 (회상) |
| 장면 설명 | 사무실 자리에서 새 명함 상자를 들고 어색하게 웃는 주인공. 뒤에서 동료 둘이 박수. |
| 등장하면 안 되는 요소 | 명함 글씨·직함, 회사 로고 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro style, small third-person office scene, office worker holding a small box of new business cards with an awkward smile, two coworkers clapping behind, faded nostalgic tint, 4:3, no text, no logo, no watermark
```

### 7. `img_pov_layoff_door_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_070 구조조정 통보 |
| 장면 종류 | 1인칭 |
| 장면 설명 | 구조조정 통보 날 아침, 회사 출입 게이트 앞에서 사원증을 찍는 내 손. 게이트 표시등은 빨간색, 다른 사람들은 지나가는 중. 차갑지만 과장되지 않은 아침 로비. |
| 등장하면 안 되는 요소 | 경비원의 강제 퇴거, 화면 속 글씨, 실제 회사 로고 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, first-person view at an office lobby turnstile, hand tapping an ID badge on the reader, red indicator light, other workers passing by, cool morning lobby light, 4:3, no text, no logo, no watermark
```

### 8. `img_moment_layoff_door_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_070 주마등 |
| 장면 종류 | 3인칭 (회상) |
| 장면 설명 | 회사 건물 앞에서 짐 상자를 안고 하늘을 올려다보는 주인공. 쓸쓸하지만 다시 시작할 것 같은 맑은 하늘. |
| 등장하면 안 되는 요소 | 우는 얼굴 클로즈업, 비 오는 절망적 분위기 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro style, small third-person scene, person holding a cardboard box of belongings in front of an office building, looking up at a clear sky, bittersweet but hopeful mood, faded nostalgic tint, 4:3, no text, no logo, no watermark
```

### 9. `img_pov_enlistment_letter_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_071 입영 통지서 |
| 장면 종류 | 1인칭 |
| 장면 설명 | 현관 우편함 앞에서 두 손으로 들고 있는 누런 공문 봉투. 뒤로 부엌에서 걱정스럽게 보는 가족의 실루엣. |
| 등장하면 안 되는 요소 | 총기·전쟁·군사 장비, 계급장·부대 마크, 봉투 위 기관명 글씨(병무청 로고 포함) (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, first-person view holding a plain official manila envelope with both hands by an apartment mailbox, family silhouette watching from the kitchen behind, warm evening light, 4:3, no text on envelope, no emblem, no weapons, no logo, no watermark
```

### 10. `img_moment_enlistment_letter_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | adult_071 주마등 |
| 장면 종류 | 3인칭 (회상) |
| 장면 설명 | 짧게 자른 머리로 정문 앞에서 가족에게 손을 흔드는 주인공의 뒷모습. 따뜻한 송별 분위기. |
| 등장하면 안 되는 요소 | 총기·전쟁·군사 훈련 장면, 부대 이름·마크, 성별을 강조하는 외형 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro style, small third-person scene, person with a very short haircut waving goodbye to family in front of a plain gate, seen from behind, warm farewell mood, faded nostalgic tint, 4:3, no text, no emblem, no weapons, no logo, no watermark
```

### 11. `img_scene_child_friend_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | 초점 유년기 · 친구 |
| 장면 종류 | 3인칭 |
| 장면 설명 | 해 질 녘 동네 놀이터에서 친구들과 술래잡기하는 아이. 미끄럼틀, 그네, 멀리 아파트 창 불빛. |
| 등장하면 안 되는 요소 | 위험한 놀이(높은 곳에서 뛰어내림), 실제 아파트 브랜드 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, third-person side view, small child playing tag with friends at a neighborhood playground at sunset, slide and swings, apartment windows lit in the distance, warm orange palette, 4:3, no text, no logo, no watermark
```

### 12. `img_scene_teen_study_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | 초점 청소년기 · 학업 |
| 장면 종류 | 3인칭 |
| 장면 설명 | 밤 독서실 칸막이 책상에서 공부하는 학생의 뒷모습. 포스트잇이 빼곡한 벽, 스탠드 불빛 하나. |
| 등장하면 안 되는 요소 | 문제집 글씨·숫자, 에너지 음료 브랜드 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, third-person view from behind, teenage student studying at a partitioned study-room desk at night, wall covered with sticky notes, single desk lamp glow, deep blue palette, 4:3, no readable text, no logo, no watermark
```

### 13. `img_scene_teen_part_time_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | 초점 청소년기 · 아르바이트 |
| 장면 종류 | 3인칭 |
| 장면 설명 | 밤 편의점 계산대에서 앞치마를 맨 학생이 손님에게 봉투를 건네는 장면. 형광등 불빛, 유리창 밖은 어둠. |
| 등장하면 안 되는 요소 | 실제 편의점 브랜드·간판 글씨, 술·담배 진열 강조 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, third-person side view, teenage part-timer in an apron handing a bag to a customer at a convenience store counter at night, fluorescent light, dark street outside the glass, 4:3, no text, no brand signage, no logo, no watermark
```

### 14. `img_scene_young_adult_adventure_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | 초점 청년기 · 도전 |
| 장면 종류 | 3인칭 |
| 장면 설명 | 처음 와 본 도시 골목에서 배낭을 메고 지도를 보는 청년. 낯선 간판 모양들, 노을. |
| 등장하면 안 되는 요소 | 실제 국가·도시 랜드마크와 국기, 간판 글씨 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, third-person view, young adult with a backpack looking at a map in an unfamiliar city alley, colorful blank shop signs, sunset sky, adventurous mood, 4:3, no text, no flags, no logo, no watermark
```

### 15. `img_scene_middle_hobby_001`

| 항목 | 내용 |
| --- | --- |
| 연결 | 초점 중년기 · 취미 |
| 장면 종류 | 3인칭 |
| 장면 설명 | 베란다에 장비를 잔뜩 펼쳐 놓고 뿌듯해하는 중년 주인공. 낚싯대·캠핑 의자·카메라가 섞여 있고 택배 상자가 쌓여 있다. |
| 등장하면 안 되는 요소 | 실제 장비 브랜드 로고, 택배 송장 글씨 (+ 공통 금지) |

도트 프롬프트 초안:

```text
pixel art, 16-bit retro game style, third-person view, middle-aged person proudly surrounded by hobby gear on an apartment balcony, fishing rod, camping chair and camera, stacked delivery boxes, cheerful afternoon light, 4:3, no text, no logo, no watermark
```

## 4. 확인 방법

1. 생성한 PNG를 `app/src/main/assets/images/`에 `imageId.png` 이름으로 넣는다
2. `python scripts/validate_content.py`를 실행해 300×225 크기 검사를 통과하는지 본다
3. 글씨·로고가 들어갔는지는 사람이 직접 본다 (자동 검사로 못 잡음)
