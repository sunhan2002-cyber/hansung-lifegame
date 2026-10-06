# PR #14 로컬 통합 기록

담당자: 정원률 · 최초 통합: 2026-10-03 · 별도 브랜치 충돌 복구: 2026-10-05

## 현재 적용 위치

- 프로젝트: `C:/Users/bestw/Documents/GitHub/hansung-lifegame`
- 현재 통합 브랜치: `feat/week6-uiux-vintage-wonryul`
- 기존 UI/UX 작업: [PR #19](https://github.com/sunhan2002-cyber/hansung-lifegame/pull/19), 커밋 `94b1e6c9fb26a44085ee0909134d1fee4c5950b8`
- 함께 적용한 디자인: [PR #14 — 시안 7 C안 빈티지 필름 UI](https://github.com/sunhan2002-cyber/hansung-lifegame/pull/14), 커밋 `8674696cdc17db3779f2b5d78a91df5bfee92824`

사용자가 요청한 범위는 로컬 통합과 충돌 복구뿐이다. 현재 vintage 브랜치의 HEAD는 main 기준 `b8abc5b`이며, PR #19의 UI/UX 작업과 PR #14의 빈티지 디자인을 합친 파일은 작업 폴더의 미커밋 변경으로만 남아 있다. commit, push, GitHub PR 병합은 실행하지 않았다. 기존 `feat/week6-uiux-wonryul` 브랜치는 `94b1e6c` 그대로여서 PR #19와 동일하며, #14와 통합하기 전 상태를 보존한다. main과 GitHub PR #19에도 이번 변경은 적용하지 않았다.

## 2026-10-05 별도 브랜치 충돌 복구

새 vintage 브랜치가 main 기준으로 생성된 뒤 기존 미커밋 통합 작업을 옮기는 과정에서 파일 5개에 충돌이 발생했다. 수정 전 작업 파일·Git 인덱스·브랜치 커밋을 프로젝트 밖의 `review/vintage-conflict-recovery-20261005/before/`에 백업했다.

PR #19의 원본 트리에 2026-10-03 검증 완료 통합 패치를 적용해 목표 파일을 재구성했다. 충돌 표시를 제거하고, main 기반에서 빠진 `ChoiceButton.kt`, `EventCard.kt`, `EventImageBox.kt`, `StatBar.kt`의 #19 개선도 복원했다. 별도 결과 화면의 삭제와 통합 게임 화면 구조를 유지했다. 이번 복구는 현재 vintage 브랜치의 로컬 파일에만 적용하며, 기존 UI/UX 브랜치와 main의 커밋은 변경하지 않는다.

## 통합한 내용

| 파일 | 현재 동작 |
| --- | --- |
| `ui/LifeGameApp.kt` | 선택 결과를 같은 게임 화면에서 표시하고 다음 사건 또는 결말로 연결. 데이터가 없을 때 복구 안내 표시 |
| `ui/LifeGameUiState.kt` | 지난 사건 기록, 이미지 파일 매칭, 나이 표시와 문구 치환을 지원 |
| `ui/LifeGameViewModel.kt` | 기록 조회용 사건 목록과 이미지 매칭 정보를 UI에 전달 |
| `ui/components/FilmStrip.kt` | 필름 전환·왜곡과 이미지 로딩. 파일이 없거나 읽기 실패 시 그림과 안내 표시, 개발용 이미지 ID 비노출 |
| `ui/components/LifeHeader.kt` | 나이·성장 단계·인생선·능력치 버튼. 큰 글씨에서 버튼이 다음 줄로 이동 |
| `ui/screens/GameScreen.kt` | 필름·사건 본문·선택 결과·선택지를 한 스크롤로 표시. 돌발 사건 팝업과 능력치 창 제공 |
| `ui/screens/HistoryPanel.kt` | 지난 사건과 실제 지표 변화 조회, 현재 사건으로 복귀. 큰 글씨에서는 복귀 영역도 스크롤에 포함 |
| `ui/theme/Color.kt`, `Theme.kt`, `Type.kt` | 종이색 배경, 잉크색 요소와 세리프 본문 등 빈티지 스타일 |
| `Week6UiTest.kt`, `Week6GameplaySmokeTest.kt` | 통합된 화면 구조에 맞춘 회귀 검증과 실제 플레이 검증 |

별도 결과 화면인 `ChoiceResultScreen.kt`는 PR #14의 흐름에 맞춰 제거했다. 기존 `ChoiceButton`, `EventCard`, `EventImageBox`의 UI/UX 개선 파일은 유지했으며, 새 게임 화면은 필름 화면의 요소를 사용한다. `StatBar`의 수치 제한과 접근성 정보, 좁은 화면에서 지표를 한 열로 표시하는 개선은 새 능력치 창에서도 사용한다.

## 충돌 해결과 추가 보정

`LifeGameApp.kt`, `LifeGameUiState.kt`, `GameScreen.kt`, `ChoiceResultScreen.kt`의 충돌을 해결했다. PR #14의 화면 구조를 채택하면서 다음 가독성 개선을 함께 적용했다.

- 긴 사건 본문과 결과 문구를 생략하지 않고 끝까지 읽도록 유지.
- 긴 선택지와 다음 해 버튼을 같은 스크롤에 포함하고 최소 터치 높이를 확보.
- 선택지의 영향 지표를 문구 아래에 표시해 본문 폭을 확보.
- 돌발 팝업에 스크롤과 안전 영역을 적용하고 긴 배지를 줄바꿈.
- 새 게임의 첫 사건이 돌발 사건일 때도 팝업 표시. 실제 플레이 테스트는 돌발 팝업을 확인한 뒤 사건 본문으로 진입.
- 실제 반영된 `appliedDelta`를 표시하며, 변화가 없을 때 `변화 없음` 안내.
- 능력치 창의 `결말 화면 확인 (임시)` 버튼 제거.
- 이미지 로딩의 코드 검사 오류와 팝업의 불필요한 제약 레이아웃을 수정.
- 보조 글자를 더 어둡게 조정해 종이색 배경에서 대비 약 4.98:1을 확보.

## 검증 결과 (2026-10-03 통합 당시)

| 검증 | 결과 |
| --- | --- |
| `testDebugUnitTest`, `testReleaseUnitTest` | 각 35개 통과: 엔진 31개, 세션 4개. 실패·오류 0개 |
| `assembleDebug`, `assembleDebugAndroidTest` | 앱과 UI 테스트 APK 빌드 성공 |
| `lintDebug` | 오류 0개, 기존 경고 7개: 라이브러리 업데이트 안내 6개, 앱 아이콘 미지정 1개 |
| Pixel 9 / Android API 36, AndroidJUnitRunner 직접 실행 | 최종 `OK (12 tests)`: UI 회귀 11개와 실제 게임 흐름 1개 모두 통과 |
| 당시 Git 상태 | 충돌 표시 없음, 기존 UI/UX HEAD 유지, 추가 커밋 없음 |

UI 회귀 검증은 320×400dp와 글꼴 배율 2배에서 긴 선택지 3개, 사건 전환 시 스크롤 초기화, 7개 지표와 접근성 수치, 긴 돌발 사건과 첫 진입 팝업, 다음 돌발 사건 팝업, 실제 반영 지표 변화, 변화 없음 안내, 긴 결과와 다음 해 버튼, 임시 결말 버튼 비노출, 읽기 실패한 이미지의 대체 안내, 기록 조회와 현재 사건 복귀를 확인했다. 실제 Activity와 assets로는 두 사건을 선택하고 결과를 읽은 뒤 다음 사건으로 진행했다.

중간 실행에서 에뮬레이터 연결이 끊겼으나, 다시 연결한 최종 실행은 12개 테스트 전체가 끝나고 성공 결과를 반환했다. 화면 캡처·테스트 로그·전체 변경 백업 패치는 프로젝트 밖의 Codex 작업 폴더 `review/pr14-local-merge/`에 보관했다.

## 확인 범위와 남은 사항

- 표시 나이는 PR #14의 규칙인 성장 단계당 사건 5개와 성인기 사건당 4년을 따른다. 데이터 모델의 실제 나이 필드는 아니다.
- 이름 치환은 PR #14의 기본값 `선한`/`선한아`를 사용한다. 사용자 이름 입력 기능을 이번 통합에서 추가하지 않았다.
- 큰 글씨와 실제 플레이 자동 테스트로 검증했다. Android Studio의 Run 버튼을 직접 조작해 검증한 것은 아니다.
- PR #19의 기존 작업 기록은 `ui_ux_guideline.md`에 보존했다. 현재 로컬 구현은 이 문서가 기준이다.
- 이 통합 내용을 GitHub에 반영하려면 사용자의 별도 commit/push 승인이 필요하다. 이번 작업에서는 모두 금지되어 실행하지 않았다.

## 2026-10-05 복구 후 검증

| 검증 | 복구 후 결과 |
| --- | --- |
| `testDebugUnitTest`, `testReleaseUnitTest` | 각 35개 통과, 실패·오류 0개 |
| `assembleDebug`, `assembleDebugAndroidTest` | 성공 |
| `lintDebug` | 오류 0개, 기존 경고 7개 |
| 전용 Pixel 9 / API 36, AndroidJUnitRunner | `OK (12 tests)` — 실제 게임 흐름 1개와 UI 회귀 11개 모두 통과 |
| 소스 비교 | `app/src` 33개 파일이 재구성한 통합 목표와 모두 일치. 공통 컴포넌트 4개는 PR #19 커밋과 동일 |
| Git 상태 | 충돌 인덱스·충돌 표시 없음, 스테이지된 파일 없음. 기존 UI/UX 브랜치 `94b1e6c`, main·vintage HEAD `b8abc5b` 유지 |

이번 복구 결과는 변경 파일 13개와 신규 파일 6개, 총 19개 파일의 미커밋 로컬 변경으로 남겼다. 기존 PR #19가 여전히 열려 있고 head가 `94b1e6c`인 것도 GitHub에서 다시 확인했다. commit, push, main 병합은 하지 않았다.

이번 검증의 로그, 빌드 검사 결과와 통합 변경 백업 패치는 프로젝트 밖 `review/vintage-conflict-recovery-20261005/`에 저장했다. 테스트에만 사용한 숨김 에뮬레이터는 검증 후 종료한다.
