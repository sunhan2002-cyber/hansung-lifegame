# 7주차 DB·QA 제출 보고서 - 김선한

## 1. 작업 범위

7주차 v4 개편안에 맞춰 데이터 구조와 검증 기준을 정리했다. 기존 앱이 읽는 6주차 사건 구조는 유지하면서, 다음 주부터 백엔드와 프론트가 v4 흐름을 연결할 수 있도록 `periods`, `focuses`, `kind`, `outcomes`, 이미지 규격, 저장 버전 2 기준을 추가했다.

## 2. 수정/추가 파일

- `docs/data_schema.md`
- `scripts/validate_content.py`
- `app/src/main/assets/events.sample.json`
- `app/src/main/assets/image_ids.sample.json`
- `app/src/main/assets/periods.sample.json`
- `app/src/main/assets/focuses.sample.json`
- `docs/qa/week7_v4_checklist.md`
- `docs/qa/week7_image_asset_checklist.md`
- `docs/qa/week7_simulation_plan.md`
- `docs/week7_dbqa_submission.md`

## 3. 구현 내용

- 시기 데이터 5개를 `periods.sample.json`으로 분리했다.
- 시기별 초점 25개를 `focuses.sample.json`으로 작성했다.
- 각 초점에 `annualStatDelta`, `sceneImageId`, `momentImageId`, `yearlyLogTemplates`를 연결했다.
- 기존 사건 204개에 v4 검증용 `kind`를 추가했다.
- 기존 선택지 594개에 v4 검증용 `outcomes`를 추가했다.
- hidden/death/primary/chance 구조를 검증할 수 있게 했다.
- 이미지 ID 50개를 scene/moment 용도로 추가했다.
- 이미지 파일명, PNG, 300×225 규격 검증 기준을 추가했다.
- 저장 데이터 버전 2 기준을 문서화했다.
- 7~9주차 개발 완료 QA 계획을 작성했다.

## 4. 검증 결과

```text
python scripts/validate_content.py: PASS
```

검증 결과 요약:

- image_count: 336
- period_count: 5
- period_total_years: 24
- focus_count: 25
- event_count: 204
- choices_with_v4_outcomes: 594
- ending_count: 31
- default_ending_count: 1
- character_count: 3

## 5. 다음 담당자가 이어받을 부분

- 백엔드: `periods.sample.json`, `focuses.sample.json`, `events.sample.json`의 `kind/outcomes`를 실제 v4 loader와 연결한다.
- 프론트엔드: `sceneImageId`, `momentImageId`, 추후 `povImageId`를 UI 이미지 슬롯에 연결한다.
- UI/UX: `image_ids.sample.json`에 등록된 scene/moment ID 기준으로 도트 이미지 제작물을 맞춘다.
- 시나리오: 기존 `INCIDENT` 중심 사건 중 큰 돌발로 격상할 사건을 골라 `BIG_INCIDENT`로 조정한다.

## 6. 남은 한계

- 실제 PNG 이미지는 아직 생성되지 않았으므로, 현재 검증은 파일명과 메타데이터 중심이다.
- `BIG_INCIDENT` 실제 사건은 시나리오 담당 산출물과 연결하며 추가 확장해야 한다.
- v4 데이터를 앱에서 실제로 읽는 loader는 백엔드/프론트 통합 단계에서 연결해야 한다.
