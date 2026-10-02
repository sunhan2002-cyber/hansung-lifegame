# 6주차 PR 검증 체크리스트

이 문서는 6주차부터 팀원 PR을 검토할 때 사용하는 공통 기준이다. main에 직접 push하지 않고, 각자 브랜치에서 PR을 만든 뒤 팀장이 검증한다.

## 1. 공통 검증 순서

1. PR 제목이 `[Week6] 역할 - 이름 - 작업 요약` 형식인지 확인한다.
2. PR 설명에 변경 파일, 구현 요약, 테스트 결과가 있는지 확인한다.
3. 변경 파일이 담당 역할 범위를 벗어나지 않았는지 확인한다.
4. 로컬에서 해당 PR 브랜치를 받아 `test`, `assembleDebug`를 실행한다.
5. Android Studio 또는 에뮬레이터에서 실제 화면을 확인한다.
6. 문제가 있으면 merge하지 않고 수정 요청한다.
7. 검증 통과 후 main에 merge한다.

공통 명령:

```bash
./gradlew test
./gradlew assembleDebug
python scripts/validate_content.py
```

Windows에서는 다음 명령을 사용한다.

```bash
./gradlew.bat test
./gradlew.bat assembleDebug
python scripts/validate_content.py
```

## 2. 김재겸 프론트엔드 PR 검증

확인 파일:

- `LifeGameViewModel.kt`
- `LifeGameUiState.kt`
- `CharacterSelectScreen.kt`
- `GameScreen.kt`
- `ChoiceResultScreen.kt`
- `EndingScreen.kt`

검증 기준:

- 이름 입력란이 존재한다.
- 이름을 입력하면 `{name}`, `{nameCall}`이 화면에 그대로 보이지 않는다.
- 이름을 비워도 기본값으로 게임이 시작된다.
- 새 게임 → 캐릭터/이름 설정 → 사건 → 선택 결과 → 다음 사건 흐름이 유지된다.
- 임시 결말 버튼, 디버그 버튼이 제출 화면에 남아 있지 않다.
- 최소 10개 사건 진행 중 흰 화면, 검은 화면, 강제 종료가 없다.

## 3. 김우영 백엔드 PR 검증

확인 파일:

- `GameEngine.kt`
- `GameSession.kt`
- `GameEngineTest.kt`
- `GameSessionTest.kt`
- `docs/week6_backend_submission.md`

검증 기준:

- 선택 적용 시 지표가 0~100 범위 안에 남는다.
- 다음 사건 후보가 여러 개일 때 앱이 죽지 않고 하나를 선택한다.
- 돌발 이벤트가 일반 사건 2개 미만 상태에서 나오지 않는다.
- 성장 단계 전환 테스트가 존재한다.
- 결말 우선순위 테스트가 존재한다.
- 같은 priority 후보 처리 기준이 테스트에 포함된다.

## 4. 정원률 UI/UX PR 검증

확인 파일:

- `ChoiceButton.kt`
- `EventCard.kt`
- `EventImageBox.kt`
- `StatBar.kt`
- `ui/theme/*.kt`
- `docs/ui_ux_guideline.md`

검증 기준:

- 긴 선택지가 버튼 밖으로 삐져나가지 않는다.
- 사건 카드에서 제목, 이미지, 본문, 선택지가 구분된다.
- 돌발 이벤트 화면이 일반 사건과 시각적으로 구분된다.
- 지표 7개가 작은 화면에서 겹치지 않는다.
- UI/UX 담당 범위 밖의 게임 로직 파일을 불필요하게 수정하지 않았다.

## 5. 신기훈 시나리오 PR 검증

확인 파일:

- `events.sample.json`
- `characters.sample.json` 필요한 경우
- `docs/scenario/week6_event_review.md`

검증 기준:

- 총 사건 수가 200개 이상이다.
- 이번 주 추가 사건이 50개 이상이다.
- 새 돌발 이벤트가 8개 이상이다.
- `eventId` 중복이 없다.
- 모든 사건에 선택지 2개 이상이 있다.
- 모든 사건의 `imageId`가 `image_ids.sample.json`에 존재한다.
- `{name}`, `{nameCall}` 표기가 정확하다.
- `python scripts/validate_content.py`가 PASS한다.

## 6. 김선한 DB·QA PR 검증

확인 파일:

- `endings.sample.json`
- `image_ids.sample.json`
- `docs/data_schema.md`
- `docs/qa/week6_pr_checklist.md`
- `docs/qa/week6_integration_check.md`
- `docs/qa/week6_test_cases.md`
- `scripts/validate_content.py`

검증 기준:

- 결말이 30개 이상이다.
- 기본 결말이 1개 이상이다.
- 사건 imageId 누락이 없다.
- 데이터 스키마 문서의 수치가 실제 JSON과 맞다.
- 일반/위험/애매 테스트 케이스가 40개 이상이다.
- 콘텐츠 검증 스크립트가 PASS한다.
