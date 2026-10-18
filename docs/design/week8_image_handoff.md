# 8주차 UI·UX 및 도트 이미지 최종 인수인계

- 담당: 정원률 / UI·UX·이미지 리소스
- 저장소: `sunhan2002-cyber/hansung-lifegame`
- 실제 로컬 폴더: `C:\Users\bestw\Documents\GitHub\hansung-lifegame`
- 작업 브랜치: `feat/week8-wonryul-pixel-assets`
- 최초 구현 기준: main `ff5d64e9badf4d9b28086c319ece257da60d220a`
- 현재 단계: 구현·자동 검사 및 사용자 에뮬레이터·최종 검증 완료. 사용자가 PR 제출을 승인했다.
- 제출: 승인된 변경을 작업 브랜치에 커밋·push하고 main 대상 PR로 제출한다. main 병합은 별도 작업이다.

## 결과와 범위

시나리오 개편 후에도 재사용할 수 있는 PNG 13장(6시기 공용 초상·7스탯 대표 장면)을 추가했다. 모두 1448×1086, 4:3이며 파일 원본을 축소하거나 잘라내지 않았다. 기존 PNG 8장은 파일과 ID를 보존하고 카탈로그의 크기만 실제 값으로 정정했다. 이미지 ID는 총 359개이며 실제 연결표에 기록된 파일은 21개다. 카탈로그 359개 모두에 PNG가 존재한다는 의미는 아니다.

초상은 동일한 기본 캐릭터가 성장·노화하는 한 세트다. 기존 성별·기질 선택 상태를 보존하며 성별별 초상은 추가하지 않았다. 사건별 신규 이미지 제작, 게임 엔진·확률·조건·시나리오·직업·가족·사망·저장 형식 변경은 이번 범위에 포함하지 않았다.

개별 파일·해상도·용도·태그·초점 30개·사건 전체 목록은 [이미지 매핑표](week8_image_mapping.md)를 참조한다. 승인 후 사용할 제목·본문은 [PR 초안](week8_image_pr_draft.md)에 정리했다.

## 이미지 선택 및 실패 처리

사건 화면은 다음 순서로 로딩을 시도한다.

1. 실제 사건 전용 이미지: 이미지 ID만 있고 파일이 없으면 다음 후보로 이동한다.
2. `eventStatTags[eventId]`의 주제와 일치하는 스탯 대표 장면: 현재 초점의 대표 태그가 목록에 있으면 그것을 우선하고, 아니면 첫 태그를 사용한다.
3. 실제 현재 생애 시기의 공용 초상.
4. 코드 placeholder.

현재 사건 209개 중 주제가 명확한 144개를 등록했다. 미등록 65개는 관련 없는 초점 그림을 추측해 붙이지 않고 전용 이미지 → 시기 초상 순서로 표시한다. 파일 누락·PNG 디코딩 실패 시 다음 후보를 시도한다. 로딩 실패로 사건과 선택지 진행을 막지 않는다.

예: `teen_007` 첫 중간고사 → 지력, `teen_009` 달리기 → 운동능력, `teen_015` 첫 아르바이트 → 경제력. `teen_042`는 준비된 시험지 전용 이미지를 먼저 표시한다. 학업 초점 중 건강 사건이 나와도 지력 그림을 사용하지 않는다. 최고 스탯이나 선택 결과로 직업·성공·최종 경로를 추정하지 않는다. 이미지 매핑은 관련 사건이 실제로 발생했을 때 표시만 바꾸며 사건 발생 규칙을 바꾸지 않는다.

`ImageRoutingRepository`가 별도 JSON을 읽고 `ImageRouting`이 후보를 만든다. `PixelAssetLoader`는 백그라운드에서 파일을 디코딩하고 8MiB 캐시를 사용한다. 디코딩 긴 변은 2048px 이내로 샘플링하지만 저장된 PNG의 규격·해상도를 변경하지 않는다. 화면 렌더링에서 보간을 끄고, 초상과 ‘장면 보기’는 전체 그림을 맞춰 표시한다. 일반 사건 배경은 화면을 채우는 과정에서 일부가 잘릴 수 있다.

## 화면별 변경

| 화면 | 최종 동작 |
| --- | --- |
| ChapterFocusScreen | 이전·현재 시기의 성장 초상 표시 |
| AutoProgressScreen | 현재 시기 초상만 표시. 기다리는 화면에는 스탯 장면을 넣지 않음 |
| DecisionScreen | 사건 전용/주제 대표 이미지 전달. 기존 ‘장면 보기’에서 흐림 없이 전체 이미지 표시 |
| RunEndScreen | 엔진의 실제 `endingPeriod` 우선, 기록이 없으면 기존 상태·사망 기록에 따라 초상 선택 |

기존 사건 텍스트·선택지·잠금·스크롤·장면 연출 구조를 유지했다. 노년기 이미지는 준비된 자산이며 노년기 게임 로직을 새로 구현했다는 뜻이 아니다. 종료 화면에서 노년기 초상을 무조건 표시하지 않는다.

## 변경 파일

| 경로 | 변경 목적 |
| --- | --- |
| .gitignore | Python 이미지 검증의 __pycache__ 제외 |
| app/src/main/assets/image_ids.sample.json | 신규 13개 등록, 기존 8개 실제 크기 정정 |
| app/src/main/assets/image_routing.json | 21개 실제 자산 경로·6시기·7스탯·30초점·144사건 연결 |
| app/src/main/java/com/example/lifegame/data/ImageRoutingRepository.kt | 별도 이미지 연결표 읽기 |
| app/src/main/java/com/example/lifegame/ui/LifeGameViewModel.kt | 콘텐츠 묶음에 이미지 연결표 전달 |
| app/src/main/java/com/example/lifegame/ui/PeriodPlayUi.kt | 사건 후보·초상·실제 종료 시기 전달 |
| app/src/main/java/com/example/lifegame/ui/components/FilmStrip.kt | 임의 예시 이미지 대체 제거, 공통 로더·후보 연결 |
| app/src/main/java/com/example/lifegame/ui/components/PixelAssetImage.kt | 비율 유지·픽셀 보간 없는 이미지 컴포넌트 |
| app/src/main/java/com/example/lifegame/ui/images/ImageRouting.kt | 파싱·주제 일치·사건 이미지 후보 선택 |
| app/src/main/java/com/example/lifegame/ui/images/PixelAssetLoader.kt | 비동기 디코딩·캐시·누락 파일 대체 |
| app/src/main/java/com/example/lifegame/ui/screens/AutoProgressScreen.kt | 대기 화면 시기 초상 표시 |
| app/src/main/java/com/example/lifegame/ui/screens/ChapterFocusScreen.kt | 이전·현재 시기의 성장 초상 |
| app/src/main/java/com/example/lifegame/ui/screens/DecisionScreen.kt | 사건 후보 연결·장면 보기 전체 맞춤 |
| app/src/main/java/com/example/lifegame/ui/screens/RunEndScreen.kt | 실제 종료 시기의 초상 |
| app/src/test/java/com/example/lifegame/ui/images/ImageRoutingTest.kt | 실제 사건 연결 및 fallback 회귀 검사 12개 |
| scripts/validate_content.py | 이미지 규격·실제 경로 연동, --images-only 지원 |
| scripts/validate_images.py | PNG·4:3·경로·태그·연결표 정합성 검사 |
| app/src/main/assets/images/week8/generic/*.png (13개) | 6시기 초상·7스탯 대표 장면. 개별 파일은 매핑표 참조 |
| docs/design/week8_image_handoff.md | 최종 인수인계·검증·연동 방법 |
| docs/design/week8_image_mapping.md | 신규·기존 자산과 전체 초점·사건 매핑표 |
| docs/design/week8_image_pr_draft.md | 승인 전 PR 제목·본문 초안 |

## 검증 결과와 근거

| 항목 | 결과 | 확인 근거 |
| --- | --- | --- |
| 이미지 ID·경로·PNG·청크 CRC·압축 스트림·4:3·메타데이터·연결표 | 통과 | 에이전트 실행 기록. ID 359, 파일 경로 21, 신규 13, 시기 6, 스탯 7, 초점 30, 사건 144 |
| Kotlin Debug 컴파일 | 통과 | JDK 21, `:app:compileDebugKotlin` 실행 기록 |
| ImageRoutingTest | 12개 통과 | 로컬 JUnit XML: 실패 0, 오류 0 |
| PeriodPlayUiTest | 12개 통과 | 로컬 JUnit XML: 실패 0, 오류 0 |
| 변경 공백 검사 | 통과 | `git diff --check` 실행 기록 |
| 기존 PNG 8장 및 엔진·시나리오 보존 | 확인 | 이전 구현 검증의 Git 대조 기록 |
| 에뮬레이터 UI 검수 | 완료 | 사용자 확인. 에이전트가 직접 수행한 검수로 표기하지 않음 |
| 최종 검증 | 완료 | 사용자 확인. PR 제출 전 에이전트가 origin/main을 fetch하여 작업 기준 커밋과 일치함을 추가 확인 |

자동 검사는 실제 사건 데이터에서 7스탯의 UI 후보와 패키지 파일 경로, 전용 이미지 우선, 무관한 초점 배제, 다중 주제, 이미지 ID 없는 pending 보완, 초상 대체, 잘못된 태그 및 경로 이탈 거부를 포함한다. 전체 프로젝트 테스트 전부의 통과를 뜻하지 않는다. 실기기 성능 수치·메모리 측정값은 수집하지 않았다.

사용자는 에뮬레이터 검수와 최종 검증을 완료했다고 확인했다. 기기/에뮬레이터 모델, API 수준, 화면 조건별 세부 결과는 전달받지 않아 임의로 작성하지 않았다. 스크린샷·영상·ZIP·백업 폴더·로컬 빌드 산출물은 제출 자료에 추가하지 않는다.

## 실행 명령

저장소 루트에서 PowerShell로 실행한다. Android Studio에서도 Gradle JDK를 21로 설정하고 각 PC의 SDK 경로를 `local.properties`에 지정한다. 이 파일은 커밋하지 않는다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
python -B scripts/validate_content.py --images-only
.\gradlew.bat --offline :app:compileDebugKotlin
.\gradlew.bat --offline :app:testDebugUnitTest --tests com.example.lifegame.ui.images.ImageRoutingTest --tests com.example.lifegame.ui.PeriodPlayUiTest
.\gradlew.bat :app:assembleDebug
git diff --check
```

JDK 경로는 각 PC에 맞춘다. 의존성이 없는 새 환경에서는 `--offline` 없이 최초 동기화를 진행한다. JDK 25에서는 현재 Gradle이 시작되지 않는 문제가 있어 JDK 21로 검증했다. Debug APK 기본 위치는 `app/build/outputs/apk/debug/app-debug.apk`이며 저장소에 올리지 않는다.

## 시연 및 재확인 경로

1. Android Studio에서 실제 저장소를 열고 에뮬레이터를 선택한 뒤 Run으로 앱을 시작한다.
2. 새 인생을 시작하고 시기를 진행한다. 초점 선택 화면의 성장 초상과 대기 화면의 현재 시기 초상을 확인한다.
3. 학업·운동·아르바이트 등 초점을 선택하고 관련 사건이 발생하면 ‘장면 보기’를 누른다. 실제 전용 파일이 없을 때 해당 사건 주제의 대표 장면이 나타나며, 화면을 누르면 사건·선택지로 돌아온다.
4. 준비된 전용 이미지가 있는 사건에서는 해당 전용 이미지가 대표 장면보다 먼저 표시된다. 전용 이미지의 ID만 있고 파일이 없으면 대표 장면 또는 시기 초상으로 이어진다.
5. 인생 종료 후 엔진에 기록된 실제 종료 시기의 초상을 확인한다. 조기 종료도 노년기로 고정하지 않는다.

사건 발생은 엔진과 조건·진행 상태에 따라 달라지므로 특정 사건이 즉시 나온다고 보장하지 않는다. 테스트 목적으로 사건 확률·선택지·저장 데이터를 바꾸지 않는다. 6시기 자산이 존재한다는 것과 모든 시기에 실제 플레이로 도달했다는 것은 구분한다. 개별 도달 여부의 세부 기록은 사용자 검수 결과에 추가할 수 있다.

## 시나리오 개편 후 연동 방법

1. 사건 ID가 바뀌면 `eventStatTags`의 키를 새 ID로 갱신하고 해당 사건을 표현할 태그를 명시한다. 여러 태그라면 첫 항목이 기본 주제다.
2. 초점 ID가 바뀌면 `focusStats`를 갱신한다. 하나의 초점이 여러 스탯을 올리더라도 대표 그림은 명시한 태그 하나로 결정한다.
3. 사건 전용 이미지가 추가되면 콘텐츠 담당자가 사건의 이미지 ID와 카탈로그를 확인하고, 이미지 담당자가 실제 파일 경로를 준비한다. 별도 경로는 `assets`에 등록하며 `assetPath`는 `images/` 기준 상대 경로다.
4. 신규 파일·ID·태그를 이미지 검증에 통과시킨 뒤 기존 대표 장면보다 전용 이미지가 우선하는지 확인한다.
5. 시기·종료 기록 형식이 바뀌면 김우영·김재겸과 실제 상태 필드 계약을 확인한다. 이미지 담당자가 종료 시기나 경로를 추정해 채우지 않는다.

## 남은 연동 사항과 제출 상태

- 이번 UI·이미지 구현과 사용자 검수는 완료됐으며 미완료 기능을 완료로 간주한 항목은 없다.
- 미등록 사건 65개는 정상적인 시기 초상 대체 범위다. 향후 사건별 이미지·주제 확정 시 추가 연결한다.
- 기준 콘텐츠에 있던 노년기 초점의 미등록 scene/moment ID와 기록 문장 수 불일치는 다른 담당 영역에 남아 있다. 이번 이미지 검증 통과를 전체 콘텐츠 QA 통과로 표시하지 않는다.
- 김재겸: 공통 화면과 이미지 삽입 위치의 후속 변경 조율. 김선한: 콘텐츠 ID·카탈로그·기존 누락 QA. 신기훈: 새 사건·초점 ID와 표현용 주제 확정. 김우영: 실제 시기·종료 기록 계약.
- 제출 문서 정리 및 사용자 PR 제출 승인이 완료됐다. 작업 브랜치에만 커밋·push하고 main 대상으로 PR을 생성한다. main 병합은 수행하지 않는다.

## PR 제출 전 확인

- 최신 `origin/main`과 작업 기준 HEAD: `ff5d64e9badf4d9b28086c319ece257da60d220a`. fetch 후 양방향 커밋 차이 `0 / 0`을 확인했다.
- 제출 파일: 코드·검증 스크립트·메타데이터·문서와 신규 PNG 13장만 포함한다. 스크린샷·영상·ZIP·백업·빌드 출력·local.properties는 제외한다.
