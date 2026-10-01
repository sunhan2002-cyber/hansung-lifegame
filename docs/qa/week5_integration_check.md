# 5주차 통합 검증 체크리스트

이 문서는 5주차 팀원 제출물을 main에 반영하기 전 팀장이 확인할 기준을 정리한다. 팀원은 GitHub에 바로 push하지 않고 Notion 카드에 먼저 제출한다.

## 1. 제출물 접수

| 순서 | 확인 항목 | 통과 기준 |
| ---: | --- | --- |
| 1 | Notion 카드 상태 | `제출됨` 또는 이에 준하는 상태로 이동되어 있다. |
| 2 | 제출 파일 | ZIP 또는 수정 파일 목록이 첨부되어 있다. |
| 3 | 담당자 설명 | 작업 요약, 수정 파일, 실행 방법, 테스트 결과가 적혀 있다. |
| 4 | 기준 브랜치 | 4주차 전체 병합 이후 최신 `main` 기준으로 작업했다. |

## 2. 압축 해제 및 파일 경로 확인

| 담당 | 허용 파일 |
| --- | --- |
| 프론트엔드 | `LifeGameApp.kt`, `LifeGameViewModel.kt`, `ui/screens/*.kt` |
| UI/UX | `docs/ui_ux_guideline.md`, `ui/theme/*.kt`, `ui/components/*.kt` |
| 백엔드 | `GameModels.kt`, `GameEngine.kt`, `GameEngineTest.kt` |
| 시나리오 | `app/src/main/assets/events.sample.json` |
| DB·콘텐츠 | `ContentRepository.kt`, `ProgressStore.kt`, `endings.sample.json`, `image_ids.sample.json`, `docs/data_schema.md` |
| 팀장·QA | `docs/qa/week5_integration_check.md`, `docs/qa/week5_test_cases.md` |

범위 밖 파일이 있으면 바로 병합하지 않고 이유를 확인한다.

## 3. JSON 검증

| 파일 | 확인 기준 |
| --- | --- |
| `events.sample.json` | 사건 120개 이상, 선택지 2개 이상, `eventId` 중복 없음 |
| `endings.sample.json` | 결말 25개 이상, 기본 결말 1개 이상, `endingId` 중복 없음 |
| `image_ids.sample.json` | `imageId` 중복 없음, 사건에서 쓰는 모든 `imageId` 존재 |

검증 시 확인할 항목:

- JSON 파싱 성공 여부
- 필수 필드 누락 여부
- 지표명 오타 여부
- `stage`, `type` 값 통일 여부
- 실제 앱에서 fallback 표시가 가능한지

## 4. Android 빌드 검증

영문 경로에서 검증한다.

```powershell
./gradlew test
./gradlew assembleDebug
```

통과 기준:

- 기존 테스트 실패 없음
- 디버그 APK 생성 성공
- 컴파일 오류 없음

## 5. 수동 실행 검증

| 순서 | 시나리오 | 기대 결과 |
| ---: | --- | --- |
| 1 | 앱 실행 | 시작 화면이 뜬다. |
| 2 | 새 게임 선택 | 캐릭터 선택 화면으로 이동한다. |
| 3 | 캐릭터 선택 후 게임 시작 | 게임 화면으로 이동한다. |
| 4 | 사건 표시 | 제목, 단계, 본문, 이미지 대체 영역, 선택지가 보인다. |
| 5 | A/B 선택 | 결과 화면으로 이동하고 지표 변화가 보인다. |
| 6 | 다음 사건 | 다음 사건 또는 결말 화면으로 안전하게 이동한다. |

## 6. GitHub 반영 기준

아래를 모두 만족할 때만 push 또는 PR을 진행한다.

- 제출 파일 경로가 숙제 기준과 맞는다.
- JSON 검증을 통과했다.
- `./gradlew test`가 성공했다.
- `./gradlew assembleDebug`가 성공했다.
- 앱 실행 흐름을 최소 1회 확인했다.
- 검증 결과를 Notion 카드에 기록했다.

## 7. 수정 요청 기준

다음 중 하나라도 있으면 `수정 요청`으로 돌린다.

- 빌드 실패
- JSON 파싱 실패
- 담당 범위 밖 핵심 파일 수정
- 필수 파일 누락
- imageId 누락
- 결말 수 기준 미달
- 선택지 누락
- 앱 실행 시 강제 종료
