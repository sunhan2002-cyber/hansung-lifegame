# 7주차 과제 — v4 개편 기준

GitHub PR: https://github.com/sunhan2002-cyber/hansung-lifegame/pull/25

이번 주 목표는 기존 사건 나열형 게임을 v4 구조로 바꾸는 것이다.

흐름:
홈/프로필 → 장 표지에서 시기 초점 선택 → 자동 진행 → 결정/돌발 → 결과 확률 → 기록/결말

중요 규칙:
- 능력치로 선택지를 잠그지 않는다. 능력치는 결과 확률에 영향을 준다.
- 이야기상 불가능한 선택지만 플래그 조건으로 숨긴다.
- 장면 이미지는 전부 도트풍이다.
- 피그마 예시와 같은 필름 프레임, 도트풍, 차분한 빈티지 톤을 기준으로 한다.
- main 직접 push 금지. 브랜치 → PR → 팀장 검증 → merge.

## 김우영 — 백엔드/GameEngine

- 시기/해/초점/자동 진행 모델 구현
- 결정/돌발 pending 상태 구현
- 결과 확률 엔진 뼈대 구현
- 고정 seed로 같은 선택이면 같은 결과가 나오게 구현
- sceneImageId, povImageId, momentImageId, TransitionCue 제공
- 테스트 최소 8개 이상

수정 예상 파일:
- GameModels.kt
- GameEngine.kt
- GameSession.kt
- GameEngineTest.kt

## 김재겸 — 프론트엔드

- 홈/프로필 화면
- 장 표지·초점 고르기 화면
- 자동 진행 화면
- 결정·돌발 화면
- 숫자 확률 대신 가능성 낮음/반반쯤/높음/매우 높음 표시
- 도트 이미지 placeholder와 FilterQuality.None 처리

수정 예상 파일:
- LifeGameApp.kt
- LifeGameViewModel.kt
- LifeGameUiState.kt
- GameScreen.kt
- 신규 HomeScreen.kt, ChapterFocusScreen.kt, AutoProgressScreen.kt, DecisionScreen.kt

## 정원률 — UI/UX·도트 이미지

- 피그마 예시 기반 도트 이미지 스타일 가이드 작성
- 화면 레이아웃 기준 작성
- 이미지 생성 프롬프트 작성
- 3인칭 평소 장면 5장 + 1인칭 돌발 장면 3장 후보 또는 프롬프트 작성

생성 파일:
- docs/design/week7_pixel_art_style_guide.md
- docs/design/week7_screen_layout_guide.md
- docs/design/week7_image_generation_prompts.md

## 신기훈 — 시나리오·콘텐츠

- 시기별 초점 문구 20개 작성
- 대표 확률 사건 5개 작성: 몸살, 시험, 승진, 해고/구조조정, 입영 통지서
- 각 사건에 outcomes 구조 작성
- 사건별 imageId와 도트 이미지 요청서 작성

수정/생성 파일:
- events.sample.json
- focuses.sample.json
- docs/scenario/week7_focus_and_incident_plan.md
- docs/scenario/week7_pixel_scene_requests.md

## 김선한 — DB·QA

- v4 데이터 스키마 문서화
- validate_content.py 보강
- imageId/이미지 파일 검증 기준 작성
- 7~9주차 3주 QA 계획 작성
- hidden death, 영아·유년 사망 금지, outcome 누락, focus 중복 검증

수정/생성 파일:
- docs/data_schema.md
- scripts/validate_content.py
- docs/qa/week7_v4_checklist.md
- docs/qa/week7_image_asset_checklist.md
- docs/qa/week7_simulation_plan.md

## 제출 방식

1. main 최신 pull
2. 브랜치 생성: feat/week7-{role}-{name}
3. 자기 역할 파일만 수정
4. test / assembleDebug 실행
5. GitHub PR 생성
6. Notion 카드에 PR 링크와 검증 결과 작성
7. 팀장 검증 통과 후 merge

상세 문서:
- docs/week7/week7_assignments_v4.md
- docs/week7/three_week_development_plan_v4.md
