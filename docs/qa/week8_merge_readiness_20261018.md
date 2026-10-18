# 8주차 피드백용 통합본 검증 — 2026-10-18

## 목적과 판정

5명 PR의 최신 결과물을 통합하고, 사용자의 요청에 따라 실행 오류 없이 플레이하며 피드백할 수 있는 버전을 만든다. 최종 이미지 제작량과 모든 분기의 게임 밸런스까지 완료했다는 뜻은 아니다.

통합 입력은 main `ff5d64e`, 김선한 #32 `1827323`, 신기훈 #33 `f2e15cd`, 김재겸 #34 `52803de`, 김우영 #30 `c073975`, 정원률 #35 `58ce49f`다. 별도 `codex/week8-merge-ready` 브랜치에서 통합했고 원래 로컬 작업 폴더의 미커밋 변경은 포함하지 않았다.

## 확인 결과

| 검사 | 결과 |
|---|---|
| 콘텐츠 JSON | 통과: 281개 사건, 796개 선택지, 65개 결말, 6시기·최대 30해 |
| 이미지 | 통과: 실제 등록 파일 21개(기존 8 + 신규 13), 초상 6시기, 스탯 장면 7종, 사건 매핑 216개 |
| 경로·플래그·이미지 계약 | 통과: 7경로별 8~10개 사건, 각 경로 전용 결말 2개 이상, 사실 규칙 108개, 미생성 필수 플래그 참조 0건 |
| 조건 감사 | 통과: 하드 대상 46개 사건·134개 선택지, 조건부 70개·항상 가능 64개, 누락·전체 잠금 후보 0건 |
| 단위 테스트 | Debug 132개 / Release 132개 통과, 실패·건너뜀 0개 |
| 여러 시드 완주 | 실제 assets와 앱 엔진 옵션으로 7경로 × 10시드 완주. 동일 시드 재실행 및 모든 pending 결정 복원 결과 비교 통과 |
| APK | Debug APK와 AndroidTest APK 빌드 통과 |
| 에뮬레이터 UI | 전체 19개 중 17개 통과 후 실패 2개를 수정. 최종 수정본에서 해당 2개 재검사 통과 |
| 실제 한 판 UI | 홈 → 프로필 → 결정·장 표지·자동 진행 → 결과 → 결말 통과. 장면 보기에서 UI 숨김, 다시 터치 시 복귀도 확인 |
| 실제 저장 파일 | Android SharedPreferences에 7경로의 각 진행 상태를 저장·복원하고, 통계·사실·pending·선택 결과·최종 결말 일치 확인 |
| 작은 화면 | 320×400dp·글자 2배 이미지 검수 화면과 320×640dp·글자 1.5배 인생 경로 화면 검사 통과 |

UI 19개 전체를 최종 수정본에서 한 번에 재실행한 결과는 아니다. 마지막 변경으로 영향을 받은 전체 앱 흐름과 저장 복원 2개를 다시 실행한 결과 `OK (2 tests)`를 확인했다. 모든 콘텐츠의 발생 빈도·문장 개연성·모든 결말 도달 가능성을 완전히 증명한 것도 아니다.

## 통합 중 수정한 오류

- 이미지 목록, 콘텐츠 묶음, 화면 상태 3개 충돌 파일에서 양쪽 기능을 모두 보존했다. 위치 인수 충돌이 난 이미지 테스트는 이름 인수로 수정했다.
- 새 백엔드의 인생 사실과 결말 판정을 화면에 연결했다. 이어하기에도 사실 규칙과 결말 데이터를 전달한다. 사망 시기·원인은 엔진의 기록을 우선한다.
- 영아기·노년기 pending 복원 시 같은 해 기록을 중복 생성하던 순서를 수정했다.
- 초점 없는 시기의 새 진행과 복원 단계가 서로 달랐던 문제를 수정했다.
- 사건 완료 플래그로 생성되는 자녀 수가 재시작 후에만 반영되던 문제를 수정했다.
- 초점 효과 직후 경제력 구간이 실제 수치와 어긋나던 문제를 수정했다.
- 저장 복원 시 사실 적용 순서를 카탈로그 순서 대신 실제 플래그 누적 순서로 맞췄다.
- 결과 보상만 보고 런타임에서 숨은 능력치 조건을 만들던 처리를 제거했다. 명시된 JSON 조건은 유지한다.
- 생성되지 않는 `photo_celebrity`에 의존하던 `adult_006`은 이전 만남 `adult_005`의 후속으로 연결하고 해당 문장을 맞췄다.
- 신규 경로 사건 72개의 주제를 이미지 연결표에 추가했다.
- Android 16에서 Espresso의 제거된 InputManager API 호출로 기기 검사가 시작하지 못하던 문제를 Espresso 3.7.0 / AndroidX Test JUnit 1.3.0으로 해결했다.
- 과거 홈 문구를 찾던 이미지 미리보기 테스트, LazyColumn의 잘못된 스크롤 대상, A가 항상 열려 있다고 가정하던 앱 흐름 테스트를 현재 화면에 맞췄다.
- 감사 보고서를 통합 사건 수와 최신 하드/소프트 정책으로 갱신했다.

## 재현 명령

```powershell
python scripts/validate_content.py
python scripts/validate_images.py
python scripts/validate_week8_contracts.py
python scripts/audit_choice_conditions.py
./gradlew.bat test assembleDebug assembleDebugAndroidTest --no-daemon --max-workers=2
adb -s emulator-5556 install -r -t app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r com.example.lifegame.test/androidx.test.runner.AndroidJUnitRunner
```

마지막 두 검사만 재현하려면 instrumentation 명령에 `-e class com.example.lifegame.ui.Week7FlowSmokeTest,com.example.lifegame.ui.Week8SaveIntegrationTest`를 추가한다. SDK 경로는 각자의 `local.properties`에 설정한다.

검사 기기는 AVD 이름 `Pixel_6a_API35`이지만 실제 시스템은 **Android 16 / API 36 / 16KB 페이지**다. 기기 이름을 OS 버전으로 오인하지 않는다. 기존 `Medium_Phone_API_36.1`은 저장공간이 부족해 테스트용 읽기 전용 실행 환경을 별도로 띄워 검사했다. 처음 2GB 메모리 실행은 저메모리 종료가 나서 4GB로 재실행했다.

로그와 캡처는 로컬 `C:/Users/hansung/Desktop/week8-verification-20261018` 아래 보관하며 Git에는 넣지 않는다.

## 피드백에서 확인할 것

- 집중한 능력치와 실제 중간 사건·직업·가족·결말의 연결이 자연스러운지
- 사건·전환점의 빈도, 잠금 조건의 난이도, 실패 결과의 납득 가능성
- 현재 그림 수는 신규 13장이다. 28장 이상의 최종 제작 목표는 남아 있으며, 대표 장면과 초상을 재사용하는 피드백용 범위다.
- 이미지 ID 441개가 모두 서로 다른 완성 그림을 가진다는 의미는 아니다. 전용 그림이 없으면 대표 그림 또는 시기 초상으로 대체한다.
- 조기 사망·인생 요약·선택 결과의 정보량과 가독성

참고: AndroidX Test API 수정 근거는 https://developer.android.com/jetpack/androidx/releases/test 의 Espresso 3.7.0 릴리스 노트다.
