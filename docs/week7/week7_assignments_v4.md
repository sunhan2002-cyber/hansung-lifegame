# 7주차 팀원별 개발 숙제 — 개편안 v4 기준

- 기준 문서: `개편안_v4_AI용_명세.md`, `개편안_v4_팀장확인용.md`
- 기준 커밋: `main` 86cb6b7 이후 최신 main
- 회의 확정 방향: 시안 8 흐름, 시기 단위 진행, 초점 고정, 자동 진행, 결정/돌발, 도트 이미지, 홈/이어하기
- 제출 방식: 각자 브랜치 생성 → 작업 → 테스트 → PR 생성 → 팀장 검증 후 merge
- 브랜치 규칙: `feat/week7-{role}-{name}`
- PR 제목 규칙: `[7주차] 역할명 - 이름`
- main 직접 push 금지

## 0. 이번 주 전체 목표

7주차 목표는 기존 사건 나열형 게임을 버리고, v4의 핵심 흐름을 앱 안에 들어갈 수 있는 구조로 바꾸는 것이다.

이번 주에는 완성형 이미지와 전체 밸런스를 끝내는 것이 아니라, 아래 흐름이 코드·데이터·화면에서 이어질 수 있어야 한다.

```text
홈/프로필
→ 장 표지에서 시기 초점 선택
→ 자동 진행 화면에서 해가 지나감
→ 결정 또는 돌발이 나오면 자동 진행 정지
→ 선택지 결과가 확률로 결정됨
→ 기록과 능력치가 반영됨
→ 다음 해 또는 다음 시기
```

중요한 변경점은 다음과 같다.

1. 능력치로 선택지를 잠그지 않는다. 능력치는 선택지 결과 확률을 바꾼다.
2. 이야기상 불가능한 선택지만 플래그 조건으로 숨긴다. 예: 연인이 없는데 프러포즈.
3. 해마다 직접 선택지를 누르는 구조가 아니라, 초점 선택 후 자동 진행된다.
4. 장면 이미지는 전부 도트풍이다.
5. 평소 진행은 3인칭 도트, 결정·돌발은 1인칭 도트다.
6. 게임 중 실시간 이미지 생성은 하지 않는다. 7주차에는 이미지 생성 시험과 규격 확정이 목표다.
7. 서버 없이도 `FakeGenerationClient` 또는 임시 이미지로 앱 흐름이 완주 가능해야 한다.

## 1. 공통 제출 조건

각자 PR에는 아래 항목이 반드시 있어야 한다.

- 수정한 파일 목록
- 어떤 R 요구사항을 처리했는지 명시
- 실행한 테스트 명령
- 통과/실패 결과
- 아직 미완성인 부분
- 다음 담당자가 이어받아야 할 파일

권장 PR 본문 형식:

```markdown
## 작업 범위
- R-xx:
- R-xx:

## 수정 파일
- `...`

## 검증
- [ ] `./gradlew.bat test`
- [ ] `./gradlew.bat assembleDebug`
- [ ] 관련 스크립트/수동 확인

## 남은 문제
- 
```

## 2. 김우영 — 백엔드/GameEngine 숙제

### 목표

시기 단위 자동 진행이 가능한 엔진 뼈대를 만든다. 기존 `EVENTS_PER_STAGE = 5` 방식에서 벗어나, `periods.json` 기준으로 해가 진행되고 결정/돌발이 뜨는 구조를 만든다.

### 수정 대상 파일

- `app/src/main/java/com/example/lifegame/domain/model/GameModels.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameEngine.kt`
- `app/src/main/java/com/example/lifegame/domain/engine/GameSession.kt`
- `app/src/test/java/com/example/lifegame/domain/engine/GameEngineTest.kt`
- 필요 시 신규 테스트 파일

### 생성/추가할 모델

아래 개념이 코드에 들어가야 한다.

- `LifePeriod`: `INFANT`, `CHILD`, `TEEN`, `YOUNG_ADULT`, `MIDDLE`
- `PeriodDefinition`: period, order, years
- `Focus`: focusId, period, label, statEffects, description
- `TurnStep`: 현재 진행 상태, 표시할 기록, 전환 종류, pending decision
- `TransitionCue`: `NOISE`, `DISTORT`, `CHAPTER`
- `DecisionKind`: `MILESTONE`, `INCIDENT`, `BIG_INCIDENT`
- `Outcome`: resultText, statDelta, addFlags, chance, hidden, death

### 구현할 기능

1. `periods.json` 또는 임시 코드 정의를 기준으로 시기별 해 수를 읽는다.
2. 시기 시작 시 초점이 없으면 장 표지 단계로 보낸다.
3. 초점은 시기 동안 바꿀 수 없게 한다.
4. 한 해 진행 시 초점 효과를 능력치에 적용한다.
5. 결정할 일이 없는 해는 자동으로 다음 해로 넘어갈 수 있는 `TurnStep`을 반환한다.
6. 예약된 마일스톤이 있으면 돌발보다 먼저 반환한다.
7. 큰 돌발과 일반 돌발 추첨 뼈대를 만든다.
8. 선택지 결과는 단일 결과가 아니라 `outcomes` 목록 중 확률로 결정되게 한다.
9. 같은 `runSeed`와 같은 선택이면 같은 결과가 나오게 한다.
10. 사망 결과가 나오면 즉시 결말 단계로 가는 상태를 만든다.

### 이미지 관련 숙제

백엔드는 이미지를 직접 만들지는 않지만, 이미지가 엔진 상태와 연결될 수 있게 아래 필드를 제공해야 한다.

- 평소 진행: `sceneImageId`
- 결정/돌발: `povImageId`
- 주마등 기록: `momentImageId`
- 전환 종류: `TransitionCue`

이 값이 없으면 프론트와 UI/UX가 도트 이미지를 연결할 수 없다.

### 테스트 기준

- 고정 seed로 24턴 개발용 루프가 끝까지 진행된다.
- 같은 seed + 같은 선택은 같은 결과를 낸다.
- 초점은 시기 중간에 바뀌지 않는다.
- 마일스톤이 있는 해에는 일반 돌발이 뜨지 않는다.
- 결정/돌발이 뜨면 자동 진행이 멈춘다.
- 숨은 사망 결과가 있더라도 UI에 넘기는 preview에는 노출되지 않는다.

### 완료 기준

- `./gradlew.bat test` 통과
- 새 엔진 테스트 최소 8개 이상
- PR 본문에 고정 seed 테스트 결과 작성

## 3. 김재겸 — 프론트엔드 숙제

### 목표

시안 8의 화면 흐름을 앱 화면으로 연결한다. 이번 주 목표는 완성 디자인이 아니라, 장 표지 → 자동 진행 → 결정/돌발 → 기록 반영 흐름이 실제 화면에서 이동되는 것이다.

### 수정 대상 파일

- `app/src/main/java/com/example/lifegame/ui/LifeGameApp.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt`
- `app/src/main/java/com/example/lifegame/ui/LifeGameUiState.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/StartScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/CharacterSelectScreen.kt`
- `app/src/main/java/com/example/lifegame/ui/screens/GameScreen.kt`
- 신규 권장:
  - `app/src/main/java/com/example/lifegame/ui/screens/HomeScreen.kt`
  - `app/src/main/java/com/example/lifegame/ui/screens/ChapterFocusScreen.kt`
  - `app/src/main/java/com/example/lifegame/ui/screens/AutoProgressScreen.kt`
  - `app/src/main/java/com/example/lifegame/ui/screens/DecisionScreen.kt`

### 구현할 기능

1. 앱 첫 화면을 홈/프로필 화면으로 바꾼다.
2. 홈 화면에 이어하기, 새 인생 살기, 현재 인생 카드가 보이게 한다.
3. 프로필 만들기 화면에 성별 선택 UI 자리를 만든다.
4. 장 표지 화면에서 시기 초점 5개 중 하나를 고르게 한다.
5. 자동 진행 화면에 시기명, N번째 해, 능력치 버튼, 시기 진행 막대, 필름 이미지 영역, 기록 한 줄을 배치한다.
6. 결정/돌발이 오면 자동 진행이 멈추고 결정 화면으로 이동한다.
7. 선택지에는 숫자 %를 보여주지 않고 `가능성 낮음/반반쯤/높음/매우 높음` 문구만 보여준다.
8. 숨은 결과는 UI에 표시하지 않는다.
9. 배속 버튼 1× / 2× / 4×와 일시정지 버튼 UI를 만든다. 실제 배속 로직은 백엔드 상태와 연결 가능한 형태면 된다.

### 이미지 생성 관련 숙제

프론트는 실제 생성 모델을 붙이는 것이 아니라, 이미지 연결 구조를 만든다.

- `sceneImageId`를 받아 필름 프레임 안에 3인칭 도트 이미지를 표시할 자리 만들기
- `povImageId`를 받아 결정/돌발 화면에 1인칭 도트 이미지를 표시할 자리 만들기
- 이미지가 없을 때는 피그마 예시와 같은 비율의 placeholder를 표시하기
- `FilterQuality.None` 또는 이에 준하는 방식으로 도트 이미지가 흐려지지 않게 표시하기

### 테스트 기준

- 홈 → 프로필/새 인생 → 장 표지 → 자동 진행 → 결정 화면까지 이동된다.
- 이름과 `{nameCall}` 자리 표시가 화면에 그대로 남지 않는다.
- 숨은 결과 문구가 UI 트리에 표시되지 않는다.
- 이미지를 못 불러와도 앱이 크래시 나지 않는다.

### 완료 기준

- `./gradlew.bat test` 통과
- `./gradlew.bat assembleDebug` 통과
- 실행 화면 캡처 3장 첨부
  - 홈 화면
  - 장 표지/초점 선택
  - 결정/돌발 화면

## 4. 정원률 — UI/UX·도트 이미지 규격 숙제

### 목표

피그마 예시 이미지와 같은 형식, 느낌, 도트풍을 기준으로 7주차 화면·이미지 규격을 확정한다. 팀원이 이미지 생성 AI를 써도 결과물이 같은 톤으로 나오도록 프롬프트와 후처리 규칙까지 정한다.

### 참고 이미지

- `고모프 긴급회의 후 피그마예시 .png`
- Figma 시안 8 개편 흐름

### 생성할 파일

- `docs/design/week7_pixel_art_style_guide.md`
- `docs/design/week7_screen_layout_guide.md`
- `docs/design/week7_image_generation_prompts.md`
- 가능하면 예시 이미지 폴더:
  - `docs/design/samples/week7/`

### 이미지 생성 숙제 — 필수

피그마 예시와 같은 톤으로 아래 이미지를 생성하거나, 생성 프롬프트와 후보 이미지를 정리한다.

#### 1차 필수 이미지 8장

| 구분 | 수량 | 설명 |
|---|---:|---|
| 3인칭 평소 장면 | 5장 | 유년/청소년/청년/중년 초점별 대표 장면. 캐릭터가 화면 안에 보임 |
| 1인칭 돌발 장면 | 3장 | 몸살, 시험/결정, 편의점/아르바이트 같은 POV 장면 |

#### 반드시 맞출 스타일

- 도트풍 픽셀아트
- 300×225 px, 4:3
- 필름 프레임에 들어갔을 때 잘리는 부분이 없어야 함
- 색상 40색 이하 느낌
- 디더링 없음
- 빈티지 종이 배경과 어울리는 차분한 톤
- 과한 네온, 그라데이션, 이모지, 3D 렌더링 금지
- 피그마 예시처럼 “조용한 인생 기록 필름” 느낌

### 프롬프트 예시 형식

```text
Korean life simulation game scene, pixel art, 300x225, 4:3, limited 40 color palette, no dithering, vintage film frame mood, calm cinematic lighting, third-person view, a teenage Korean boy studying at a wooden desk at night, desk lamp, notebooks, small room, emotional but not dramatic, no text, no UI, no watermark
```

1인칭 장면 예시:

```text
first-person pixel art scene, 300x225, 4:3, limited 40 color palette, no dithering, vintage film mood, Korean teenager lying in bed with a thermometer in hand, morning light through curtains, feverish atmosphere, no face visible, no text, no UI, no watermark
```

### 화면 설계 숙제

아래 화면 4개를 피그마 예시 기준으로 정리한다.

1. 홈/프로필 화면
2. 장 표지·초점 고르기 화면
3. 자동 진행 화면
4. 결정·돌발 화면

각 화면마다 아래를 작성한다.

- 화면 목적
- 필수 정보
- 버튼 위치
- 이미지 비율
- 글자 크기 기준
- 선택지/카드 간격
- 작은 화면에서 잘리지 않는 기준

### 완료 기준

- 이미지 프롬프트 8개 이상
- 실제 생성 후보 또는 참고 후보 8개 이상
- 화면별 레이아웃 기준 4개 화면 작성
- 프론트가 바로 구현할 수 있게 컴포넌트 단위로 정리

## 5. 신기훈 — 시나리오·콘텐츠 숙제

### 목표

v4 방식에 맞춰 기존 사건을 “시기별 돌발/마일스톤/큰 돌발”로 재정리하고, 결과 확률 구조에 맞는 대표 사건을 작성한다.

### 수정 대상 파일

- `app/src/main/assets/events.sample.json`
- 신규 권장:
  - `app/src/main/assets/focuses.sample.json`
  - `docs/scenario/week7_focus_and_incident_plan.md`
  - `docs/scenario/week7_pixel_scene_requests.md`

### 작성할 콘텐츠

#### 1. 시기별 초점 문구 20개

영아기를 제외하고 유년·청소년·청년·중년 각 5개씩 작성한다.

각 초점에는 아래를 포함한다.

- focusId
- 시기
- 이름
- 짧은 설명
- 상승 능력치
- 하락 능력치
- 해마다 기록 문장 예시 3개

#### 2. 대표 확률 사건 5개

아래 사건은 반드시 작성한다.

| 사건 | 목적 |
|---|---|
| 몸살 | 건강 스탯과 숨은 악화/사망 구조 확인 |
| 시험 | 지력에 따라 결과 확률 변화 확인 |
| 승진 | 경제력/직장 단계 확인 |
| 해고 또는 구조조정 | 경제력 하락과 회복 동기 확인 |
| 입영 통지서 | 성별·군복무 갈림길 확인 |

각 사건은 아래 구조를 가져야 한다.

- eventId
- periods
- kind
- weight
- conditions
- view: `FIRST_PERSON` 또는 `THIRD_PERSON`
- imageId
- title
- text
- choices
- outcomes
- primary outcome
- hidden death가 있는 경우 hidden 처리

#### 3. 이미지 생성 요청서

사건별로 필요한 이미지를 정리한다.

- imageId
- 장면 종류: 1인칭/3인칭
- 장면 설명
- 등장하면 안 되는 요소
- 도트 이미지 프롬프트 초안

### 중요 규칙

- 능력치가 낮다고 선택지를 잠그는 문장으로 쓰지 않는다.
- 결과 가능성이 달라진다는 식으로 쓴다.
- 영아·유년기 사망 금지.
- 전쟁, 학대, 실제 질병 공포를 과하게 다루는 소재 금지.
- 개복치식 유머는 가능하지만 너무 잔인하게 쓰지 않는다.
- 숨은 사망은 청소년기 이후부터만 사용한다.

### 완료 기준

- 초점 20개
- 대표 확률 사건 5개
- 각 사건 선택지 최소 2개 이상
- 각 선택지 결과 최소 1개 이상
- 이미지 요청서 10개 이상
- JSON 파싱 가능

## 6. 김선한 — DB·QA·검증 숙제

### 목표

v4 데이터 구조를 문서화하고, 팀원 산출물이 잘못 들어와도 자동으로 잡을 수 있는 검증 기준을 만든다. 이미지 생성 산출물도 데이터와 연결 가능해야 한다.

### 수정 대상 파일

- `docs/data_schema.md`
- `scripts/validate_content.py`
- `app/src/main/assets/endings.sample.json`
- `app/src/main/assets/image_ids.sample.json`
- 신규 권장:
  - `app/src/main/assets/periods.sample.json`
  - `app/src/main/assets/focuses.sample.json`
  - `docs/qa/week7_v4_checklist.md`
  - `docs/qa/week7_image_asset_checklist.md`
  - `docs/qa/week7_simulation_plan.md`

### 구현/작성할 내용

#### 1. 데이터 스키마 문서 갱신

아래 항목을 문서에 추가한다.

- periods
- focuses
- events.kind: `MILESTONE`, `INCIDENT`, `BIG_INCIDENT`
- outcomes
- chance
- hidden death
- TransitionCue
- imageId 규칙
- 얼굴 시트/몸 부품/장면 이미지 규격
- 저장 데이터 버전 2

#### 2. 검증 스크립트 보강

`scripts/validate_content.py`에 아래 검증을 추가한다.

- period ID 중복 없음
- focus ID 중복 없음
- 시기마다 초점 5개인지 확인
- event kind 값 검증
- outcome 없는 선택지 검출
- primary outcome 중복 또는 누락 검출
- hidden death가 UI preview로 나가지 않게 표시 필드 검증
- 영아·유년기 death 결과 금지
- imageId가 `image_ids.sample.json`에 존재하는지 확인
- image fileName 규격 확인
- maxStats 파싱 기준 문서화
- `{name}`, `{nameCall}` 치환 누락 가능 문장 검출

#### 3. 이미지 생성 검증 기준

이미지 산출물은 아래 기준으로 검사한다.

- 300×225 px인지
- PNG인지
- 4:3 비율인지
- 1인칭/3인칭 구분이 명확한지
- 도트풍인지
- 텍스트/워터마크가 들어가지 않았는지
- imageId와 실제 파일명이 맞는지
- 앱에서 없을 때 placeholder로 대체 가능한지

#### 4. 3주 개발 완료 QA 계획

`docs/qa/week7_simulation_plan.md`에 아래를 작성한다.

- 7주차 검증 기준
- 8주차 검증 기준
- 9주차 검증 기준
- 각 주차별 통과 조건
- merge 전 확인 명령

### 완료 기준

- `python scripts/validate_content.py` 실행 가능
- `./gradlew.bat test` 통과
- `./gradlew.bat assembleDebug` 통과
- 이미지 검증 체크리스트 작성
- 3주 개발 완료 기준 작성

## 7. 팀장 검증 기준

팀장은 각 PR을 아래 순서로 본다.

1. PR 브랜치가 main 최신 기준인지 확인
2. 역할별 요구 파일이 있는지 확인
3. 이미지 생성 산출물 또는 프롬프트가 있는지 확인
4. v4 규칙 위반 여부 확인
   - 능력치로 선택지 잠금 금지
   - 숫자 확률 UI 노출 금지
   - 숨은 결과 UI 노출 금지
   - 영아·유년 사망 금지
   - main 직접 push 금지
5. 테스트 실행
6. 앱 실행 또는 화면 캡처 확인
7. 통과하면 merge

## 8. 팀원에게 보낼 짧은 공지

```text
7주차부터 앱 구조를 v4로 개편함.
기존처럼 사건을 순서대로 보는 방식이 아니라,
홈/프로필 → 장 표지에서 시기 초점 선택 → 자동 진행 → 결정/돌발 → 결과 확률 → 기록/결말 구조로 바꿀 거임.

중요한 점:
- 능력치로 선택지를 잠그는 게 아니라 결과 확률이 바뀌는 방식임.
- 장면 이미지는 전부 도트풍이고, 피그마 예시랑 같은 느낌으로 맞춰야 함.
- 각자 숙제에는 개발 작업 + 이미지 관련 산출물이 같이 들어감.
- main 직접 push 금지, 브랜치 따서 PR 올리면 팀장이 검증 후 merge함.

자기 역할별 세부 숙제는 docs/week7/week7_assignments_v4.md 보고 그대로 하면 됨.
```
