# 7주차 팀원 과제 검증 결과 — 2026-10-13

검증 기준: `docs/week7/week7_assignments_v4.md`, `docs/week7/notion_week7_assignment_post.md`

검증 방식:

- GitHub PR 실제 파일 목록 확인
- 각 PR 브랜치를 별도 worktree로 checkout
- `python scripts/validate_content.py` 실행 가능한 브랜치는 실행
- `./gradlew.bat test` 실행
- `./gradlew.bat assembleDebug` 실행
- 역할별 7주차 요구 산출물 충족 여부 확인

## 전체 결론

| 담당자 | 역할 | PR | 판정 | 이유 |
|---|---|---:|---|---|
| 김선한 | DB·QA | #26 | 통과 | v4 스키마, 검증 스크립트, QA 문서, periods/focuses/imageId 구조 제출. validate/test/assemble 통과 |
| 김우영 | 백엔드 | #20 | 조건부 통과 | 시기/초점/자동 진행/결정 pending/확률 outcome/seed/이미지 필드 구현. 단, PR #26/#27 데이터가 먼저 들어간 뒤 최종 통합 검증 필요 |
| 신기훈 | 시나리오·콘텐츠 | #27 | 통과 | 비영아 초점 20개, 대표 확률 사건 5개 성격, BIG_INCIDENT 20개, 이미지 요청서 15개 제출. validate/test/assemble 통과 |
| 김재겸 | 프론트엔드 | #28 | 조건부 통과 | 홈→프로필→장 표지→자동 진행→결정/결과 화면 구조와 캡처 제출. 단, #20/#26/#27 위에 쌓인 PR이라 선행 PR 병합 후 최종 충돌 검증 필요 |
| 정원률 | UI/UX | #29 | 조건부 통과 | 노션 기준의 가이드/프롬프트/이미지 후보는 충족. 1448×1086 4:3 원본도 에뮬레이터에서 정상 표시 확인. 단, 작은 화면+큰 글자 UI 테스트 1개 실패 |

## GitHub PR 확인

| PR | 브랜치 | base | 상태 | merge 상태 |
|---:|---|---|---|---|
| #20 | `feat/week7-backend-wooyoung` | `main` | OPEN | CLEAN |
| #26 | `feat/week7-db-qa-sunhan` | `main` | OPEN | CLEAN |
| #27 | `feat/week7-scenario-kihoon` | `feat/week7-db-qa-sunhan` | OPEN | CLEAN |
| #28 | `feat/week7-frontend-jaegyeom` | `main` | OPEN | CLEAN |
| #29 | `week7-uiux-wonryul` | `main` | OPEN | CLEAN |

## 자동 검증 결과

| PR | validate_content.py | gradlew test | gradlew assembleDebug |
|---:|---|---|---|
| #26 김선한 | PASS | PASS | PASS |
| #27 신기훈 | PASS | PASS | PASS |
| #20 김우영 | PASS | PASS | PASS |
| #28 김재겸 | PASS | PASS | PASS |
| #29 정원률 | 기존 데이터 기준 PASS | PASS | PASS, connectedDebugAndroidTest 1건 실패 |

정원률 PR은 일반 빌드와 단위 테스트는 통과한다. 이미지 생성 원본을 300×225로 직접 뽑을 필요는 없으며, 1448×1086 4:3 원본도 에뮬레이터 디자인 미리보기에서 정상 표시된다. 다만 연결 UI 테스트에서 작은 화면+큰 글자 조건 1건이 실패했다.

## 역할별 세부 검증

### 김선한 — DB·QA, PR #26

요구사항 대비 확인:

- `docs/data_schema.md` 갱신: 충족
- `scripts/validate_content.py` 보강: 충족
- `periods.sample.json`: 5개, 총 24년 구조 확인
- `focuses.sample.json`: 25개, 시기별 5개 확인
- `events.sample.json`: 204개, 모든 선택지 outcome 구조 추가
- `image_ids.sample.json`: 336개 확인
- QA 문서 3개 확인
  - `docs/qa/week7_v4_checklist.md`
  - `docs/qa/week7_image_asset_checklist.md`
  - `docs/qa/week7_simulation_plan.md`

판정: 통과.

남은 의존성:

- 실제 PNG 이미지 품질 검증은 정원률 이미지 규격 보완 후 다시 확인해야 한다.
- BIG_INCIDENT 실제 사건 품질은 신기훈 PR과 함께 최종 확인한다.

### 김우영 — 백엔드/GameEngine, PR #20

요구사항 대비 확인:

- `LifePeriod`, `PeriodDefinition`, `Focus`, `TurnStep`, `TransitionCue`, `DecisionKind`, `Outcome` 계열 모델 추가: 충족
- 시기 초점 선택 및 시기 중 초점 고정 구조: 충족
- 자동 진행 step 반환: 충족
- 마일스톤 우선, 일반/큰 돌발 추첨 뼈대: 충족
- outcomes 확률 처리, seed 재현성: 충족
- hidden/death 처리 및 preview 비노출 구조: 충족
- `sceneImageId`, `povImageId`, `momentImageId`, `TransitionCue` 제공: 충족
- 신규 테스트: 8개 이상 충족

판정: 조건부 통과.

조건:

- #26 DB·QA와 #27 시나리오 데이터가 먼저 병합된 뒤, v4 loader와 실제 데이터 통합 검증을 한 번 더 해야 한다.
- 현재 PR 자체는 test/assemble 통과.

### 신기훈 — 시나리오·콘텐츠, PR #27

요구사항 대비 확인:

- 비영아 시기 초점 20개: 충족
- 각 초점의 해마다 기록 문장 3개: 충족
- 대표 확률 사건 5개 성격: 충족
  - 몸살: `adult_068`
  - 시험: 기존/대표 시험 사건 포함
  - 승진: `adult_069`
  - 해고/구조조정 성격: `adult_070` 조직 개편 면담 사건
  - 입영 통지서: `adult_071`
- `BIG_INCIDENT`: 20개 확인
- hidden death: 1개, 청년/중년기에만 존재, 영아·유년 사망 0건
- 이미지 요청서: 15개 확인
- JSON 파싱: PASS

판정: 통과.

보완하면 좋은 점:

- `adult_070`은 구조조정 성격은 맞지만 제목에 “해고/구조조정” 단어가 직접 들어가지는 않는다. 팀 회의에서 표현을 더 직관적으로 바꿀지 결정하면 된다.
- 입영 통지서는 성별/병역 플래그 정책이 프론트/백엔드와 연결되어야 최종 완성된다.

### 김재겸 — 프론트엔드, PR #28

요구사항 대비 확인:

- 홈/프로필 화면: 충족
- 장 표지·초점 선택 화면: 충족
- 자동 진행 화면: 충족
- 결정·돌발 화면: 충족
- 숫자 확률 대신 가능성 문구 표시: 충족
- hidden 결과 UI 비노출 테스트: 충족 보고 및 테스트 존재
- 도트 이미지 placeholder, `FilterQuality.None`: 충족
- 실행 화면 캡처 5장 제출: 충족

판정: 조건부 통과.

조건:

- #28은 #20, #26, #27 위에 쌓인 PR이라 선행 PR merge 후 main 기준으로 다시 diff가 줄어드는지 확인해야 한다.
- 이어하기 영구 저장은 아직 8주차 범위로 남아 있다.

### 정원률 — UI/UX·도트 이미지, PR #29

요구사항 대비 확인:

- `docs/design/week7_pixel_art_style_guide.md`: 충족
- `docs/design/week7_screen_layout_guide.md`: 충족
- `docs/design/week7_image_generation_prompts.md`: 충족
- 프롬프트 8개 이상: 충족
- 실제 이미지 후보 8개 이상: 충족
- 화면 4개 레이아웃 기준: 충족
- 초기 화면의 `디자인 미리보기` 버튼: 에뮬레이터에서 확인
- 1448×1086 PNG 원본의 4:3 필름 표시: 에뮬레이터에서 정상 확인

실제 이미지 크기 확인:

- `docs/design/samples/week7/individual/v1/*.png`: 전부 1448×1086, 4:3
- `docs/design/samples/week7/week7_normal_5_scenes_v1.png`: 1774×887
- `docs/design/samples/week7/week7_pov_3_scenes_v1.png`: 1448×1086

에뮬레이터 확인:

- `Pixel_6a_API35`에서 PR #29 브랜치 APK 설치 성공
- 초기 화면 `이어하기` 아래 `디자인 미리보기` 버튼 확인
- `디자인 미리보기` 진입 성공
- 첫 번째 샘플 `유년기 친구` 이미지가 필름 프레임 안에서 잘림 없이 표시됨
- 화면에 `원본 1448×1086 · 4:3` 안내 문구 표시 확인

자동 검증:

- `./gradlew.bat test`: PASS
- `./gradlew.bat assembleDebug`: PASS
- `./gradlew.bat connectedDebugAndroidTest`: 14개 중 13개 통과, 1개 실패
- 실패 테스트: `Week7ImageSampleLayoutTest.imageAndControlsRemainAccessibleAt320By400WithLargeText`
- 실패 위치: `image_sample_next`가 320×400 화면 + 큰 글자 조건에서 표시되지 않음

판정: 조건부 통과.

정정 사항:

- 300×225로 직접 생성하지 못한 것은 불통과 사유가 아니다.
- 노션 과제 기준이 “이미지 후보 또는 프롬프트”였으므로 7주차 제출 조건은 충족으로 본다.
- 1448×1086 4:3 원본은 앱에서 그대로 표시 가능하다.
- 후속 보완은 이미지 해상도 축소가 아니라 작은 화면/큰 글자 환경에서 버튼 접근성을 보장하는 레이아웃 조정이다.

수정 요청:

- `ImageSamplePreviewScreen`에서 320×400 + 큰 글자 조건에서도 `다음 이미지`, `이전 이미지`, `장면 고르기`가 스크롤 후 표시되도록 조정한다.
- 이미지 자체는 원본 1448×1086을 유지해도 된다.
- 앱 적용 전에는 파일 용량, 로딩 속도, 도트 흐림 여부를 한 번 더 확인한다.

## 권장 merge 순서
정원률 #29는 이미지 해상도 때문에 막을 필요는 없다. 다만 connectedDebugAndroidTest 1건 실패가 있으므로 작은 화면+큰 글자 레이아웃 보완 후 merge하는 것이 좋다.

현재 기준 권장 순서:

1. #26 김선한 DB·QA merge
2. #27 신기훈 시나리오 merge 또는 base 재지정 후 merge
3. #20 김우영 백엔드 merge 전 최신 main rebase/merge 확인
4. #28 김재겸 프론트엔드 merge 전 선행 PR 반영 후 최종 diff 확인
5. #29 정원률 UI/UX는 작은 화면+큰 글자 레이아웃 테스트 보완 후 재검증

## Notion 반영용 요약

```text
7주차 과제 검증 결과

김선한 DB·QA #26: 통과
- v4 스키마, 검증 스크립트, QA 문서 제출
- validate/test/assemble PASS

김우영 백엔드 #20: 조건부 통과
- 시기/초점/자동 진행/결정 pending/확률 outcome/seed/이미지 필드 구현
- test/assemble PASS
- #26/#27 병합 후 최종 통합 검증 필요

신기훈 시나리오 #27: 통과
- 초점 20개, 대표 확률 사건 5개, BIG_INCIDENT 20개, 이미지 요청서 15개 제출
- validate/test/assemble PASS

김재겸 프론트엔드 #28: 조건부 통과
- 홈/프로필/장 표지/자동 진행/결정 화면과 캡처 제출
- test/assemble PASS
- 선행 PR 반영 후 최종 diff 확인 필요

정원률 UI/UX #29: 조건부 통과
- 가이드/프롬프트/이미지 후보 제출 충족
- 1448×1086 4:3 원본은 에뮬레이터 디자인 미리보기에서 정상 표시 확인
- connectedDebugAndroidTest 1건 실패: 작은 화면+큰 글자 조건에서 다음 이미지 버튼 접근성 보완 필요
```



