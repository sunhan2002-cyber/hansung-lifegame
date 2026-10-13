# 7주차 v4 QA 체크리스트

이 문서는 v4 개편 PR을 merge하기 전 팀장이 확인할 기준이다. 7주차부터는 단순히 파일이 올라왔는지보다, 시기별 초점·자동 진행·결정/돌발·확률 결과 규칙을 지켰는지를 우선 확인한다.

## 1. 공통 merge 전 확인

- [ ] PR이 `main` 최신 기준 브랜치에서 만들어졌는가
- [ ] main 직접 push가 아닌 PR 제출인가
- [ ] PR 본문에 수정 파일, 요구사항 처리 항목, 테스트 결과가 적혀 있는가
- [ ] `python scripts/validate_content.py`가 PASS인가
- [ ] `./gradlew.bat test`가 PASS인가
- [ ] `./gradlew.bat assembleDebug`가 PASS인가
- [ ] 앱 실행 또는 화면 캡처로 주요 흐름을 확인했는가

## 2. v4 핵심 규칙

- [ ] 능력치로 선택지를 잠그지 않았는가
- [ ] 능력치는 결과 확률 보정에만 쓰이는가
- [ ] 숫자 확률을 UI에 그대로 노출하지 않는가
- [ ] hidden outcome이 선택지 preview에 노출되지 않는가
- [ ] `INFANT`, `CHILD` 사건에 death outcome이 없는가
- [ ] 이야기상 불가능한 선택지만 flag 조건으로 숨기는가
- [ ] 한 시기에서 선택한 초점은 시기 중간에 바뀌지 않는가
- [ ] 결정/돌발이 나오면 자동 진행이 멈추는가
- [ ] 마일스톤은 일반 돌발보다 우선 등장하는가

## 3. 백엔드/GameEngine PR 확인

- [ ] `LifePeriod`, `PeriodDefinition`, `Focus`, `TurnStep`, `TransitionCue`, `Outcome` 모델이 있는가
- [ ] 고정 seed에서 같은 선택 결과가 재현되는가
- [ ] 24턴 개발용 루프가 끝까지 진행되는가
- [ ] `sceneImageId`, `povImageId`, `momentImageId`를 전달하는가
- [ ] 숨은 결과와 사망 결과 테스트가 있는가

## 4. 프론트엔드 PR 확인

- [ ] 홈 화면이 있는가
- [ ] 장 표지에서 시기별 초점을 선택할 수 있는가
- [ ] 자동 진행 화면이 시기명, 진행 막대, 기록, 능력치 변화를 보여주는가
- [ ] 결정/돌발 화면에서 선택지가 질적 가능성으로 표시되는가
- [ ] 이미지가 없어도 placeholder로 크래시 없이 표시되는가
- [ ] `{name}`, `{nameCall}`이 화면에 그대로 남지 않는가

## 5. UI/UX PR 확인

- [ ] 피그마 예시와 같은 흑백/필름/도트풍 방향을 따르는가
- [ ] 도트 이미지가 흐려지지 않게 표시되는 기준이 있는가
- [ ] 3인칭 진행 이미지와 1인칭 결정 이미지가 구분되는가
- [ ] 작은 화면에서 버튼, 제목, 설명이 잘리지 않는가
- [ ] 이미지 프롬프트와 금지 스타일이 문서화되어 있는가

## 6. 시나리오 PR 확인

- [ ] 시기별 초점 문구가 작성되어 있는가
- [ ] 대표 확률 사건에 outcomes가 있는가
- [ ] imageId와 scene prompt가 함께 작성되어 있는가
- [ ] 영아·유년 사망 결과가 없는가
- [ ] 어색한 번역투가 아닌 자연스러운 한국어 문장인가

## 7. DB·QA PR 확인

- [ ] `periods.sample.json`이 있는가
- [ ] `focuses.sample.json`이 있는가
- [ ] `events.sample.json`에 v4 `kind`와 `outcomes`가 있는가
- [ ] `image_ids.sample.json`에 v4 scene/moment/pov 이미지 규격이 반영되어 있는가
- [ ] `docs/data_schema.md`가 실제 파일 기준으로 갱신되어 있는가
- [ ] 이미지 QA와 3주 개발 QA 문서가 있는가
