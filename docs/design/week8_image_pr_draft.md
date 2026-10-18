# feat(week8): 시기별 성장 초상 및 스탯별 도트 이미지 연결

> 사용자 승인 후 제출하는 PR 본문이다. 대상 브랜치: `main`, 작업 브랜치: `feat/week8-wonryul-pixel-assets`.

## 변경 내용

사건별 이미지가 아직 준비되지 않은 상황에서도 플레이어의 성장 시기와 사건 주제를 이해할 수 있도록 공용 성장 초상 6장과 스탯 대표 장면 7장을 연결했습니다. 대기 화면에는 현재 시기의 초상을 표시하고, 사건에서는 준비된 전용 이미지가 없을 때 주제에 맞는 대표 장면을 표시합니다.

- 신규 13장은 모두 1448×1086 PNG, 4:3입니다. 기존 8장은 보존하고 카탈로그의 실제 크기 정보만 정정했습니다.
- 이미지 경로·6시기·7스탯·30초점·144사건의 연결을 별도 `image_routing.json`으로 관리합니다. 사건 209개 중 주제 미지정 65개는 전용 이미지가 없으면 시기 초상을 사용합니다.
- 선택 순서는 **전용 이미지 → 사건 주제 대표 장면 → 시기 초상 → placeholder**입니다. 여러 주제 사건은 현재 초점과 일치하는 태그를 우선하며 무관한 초점 그림을 붙이지 않습니다.
- 파일 누락·디코딩 실패 시 다음 후보로 이동합니다. 비동기 디코딩, 8MiB 캐시, 픽셀 보간 없는 표시를 적용했습니다.
- 초점 선택 화면은 성장 초상, 대기 화면은 현재 초상, 사건의 ‘장면 보기’는 전체 장면, 종료 화면은 실제 종료 시기 초상을 표시합니다.
- 게임 진행·사건 발생 확률·스탯 조건·시나리오·직업·가족·사망 판정·저장 형식은 변경하지 않았습니다. 노년기 이미지를 추가한 것으로 노년기 로직을 새로 구현했다고 보고하지 않습니다.

## 주요 변경 파일

| 경로 | 내용 |
| --- | --- |
| `app/src/main/assets/images/week8/generic/` | 신규 PNG 13장 |
| `app/src/main/assets/image_ids.sample.json`, `image_routing.json` | 카탈로그·정확한 경로·표현용 주제 매핑 |
| `app/src/main/java/com/example/lifegame/ui/images/` | ImageRouting·PixelAssetLoader |
| `app/src/main/java/com/example/lifegame/data/ImageRoutingRepository.kt` | 연결표 읽기 |
| `app/src/main/java/com/example/lifegame/ui/PeriodPlayUi.kt`, `LifeGameViewModel.kt` | 이미지 후보와 실제 시기 전달 |
| `app/src/main/java/com/example/lifegame/ui/components/FilmStrip.kt`, `PixelAssetImage.kt` | 공통 이미지 표시·대체 |
| `app/src/main/java/com/example/lifegame/ui/screens/ChapterFocusScreen.kt`, `AutoProgressScreen.kt`, `DecisionScreen.kt`, `RunEndScreen.kt` | 기존 화면에 최소 연결 |
| `scripts/validate_content.py`, `scripts/validate_images.py` | PNG·4:3·경로·메타데이터·매핑 검사 |
| `app/src/test/java/com/example/lifegame/ui/images/ImageRoutingTest.kt` | 사건 이미지 연결 회귀 테스트 |
| `docs/design/week8_image_*.md`, `.gitignore` | 인수인계·매핑표·PR 초안, Python 캐시 제외 |

전체 변경 목록과 13장 개별 파일은 `docs/design/week8_image_handoff.md`, `docs/design/week8_image_mapping.md`를 참조합니다.

## 실행 명령 및 테스트 결과

저장소 루트에서 JDK 21과 Android SDK를 설정합니다. 아래 JDK 경로는 검증한 로컬 환경의 예입니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21.0.12.1'
python -B scripts/validate_content.py --images-only
.\gradlew.bat --offline :app:compileDebugKotlin
.\gradlew.bat --offline :app:testDebugUnitTest --tests com.example.lifegame.ui.images.ImageRoutingTest --tests com.example.lifegame.ui.PeriodPlayUiTest
.\gradlew.bat :app:assembleDebug
git diff --check
```

| 검증 | 결과 |
| --- | --- |
| 이미지 검증 | 통과: ID 359, 연결 파일 21, 신규 13, 시기 6, 스탯 7, 초점 30, 사건 144 |
| Kotlin Debug 컴파일 | 통과 |
| 관련 단위 테스트 | 24개 통과, 실패·오류 0 (ImageRoutingTest 12, PeriodPlayUiTest 12) |
| 공백 검사 | 통과 |
| 에뮬레이터 UI 검수 | 사용자 확인 완료 |
| 최종 검증 | 사용자 확인 완료. PR 제출 전 최신 origin/main과 작업 기준 커밋 일치 추가 확인 |

전체 프로젝트 테스트 전체 통과나 실기기 메모리 측정 완료를 의미하지 않습니다. 에뮬레이터 기종·API 수준 및 세부 화면별 로그는 제공받지 않아 기재하지 않았습니다. 새 환경에서 의존성이 없으면 최초 실행 시 `--offline`을 제외합니다.

## 시연 경로

1. Android Studio에서 저장소를 열고 Gradle JDK 21 및 SDK를 설정한 뒤 에뮬레이터에서 Run합니다.
2. 새 인생 시작 → 시기 진행 → 초점 선택에서 성장 초상을 확인합니다. 대기 화면에는 현재 시기 초상만 표시됩니다.
3. 관련 사건 발생 → ‘장면 보기’: `teen_007`은 지력, `teen_009`는 운동능력, `teen_015`는 경제력 대표 장면을 사용합니다. 화면을 누르면 사건·선택지로 돌아옵니다.
4. `teen_042`처럼 실제 전용 이미지가 준비된 사건은 전용 그림을 먼저 표시합니다. 미등록 주제·누락 파일은 시기 초상 또는 placeholder로 대체합니다.
5. 인생 종료 → 실제 엔진 종료 시기에 맞는 초상을 확인합니다.

사건 발생은 기존 엔진 조건을 따르며 특정 사건을 강제로 발생시키는 기능은 추가하지 않았습니다. 자동 테스트는 실제 사건 파일과 UI 후보 경로를 사용해 7개 스탯의 연결을 확인합니다.

## 후속 연동

시나리오 개편으로 사건·초점 ID가 바뀌면 `eventStatTags`와 `focusStats`를 갱신합니다. 사건 전용 이미지가 준비되면 기존 ID와 실제 경로를 맞춰 우선 사용합니다. 기준 콘텐츠의 노년기 초점 scene/moment ID 누락 및 기록 문장 수 문제는 콘텐츠 담당 영역에 남겼습니다.

스크린샷·영상·ZIP·백업·로컬 빌드 출력은 포함하지 않습니다. 사용자 승인에 따라 작업 브랜치에서 main 대상으로 PR을 제출합니다.
