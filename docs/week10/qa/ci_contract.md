# CI 실행과 최종 인수의 구분

기준: 2026-11-19 리팩토링. 워크플로는 `.github/workflows/week10-qa.yml`이다.

## PR·main 기본 검사

- 콘텐츠 구조·ID·참조·확률·조건·이미지 라우팅 검사와 Python 회귀 테스트.
- Android lint, Debug/Release 단위 테스트, 실제 콘텐츠 10,000회 시뮬레이션 결과 생성.
- Debug/Release APK와 계측 테스트 APK 빌드, 체크섬 기록.
- `verify_release_security.py`로 실제 Release APK의 직접 생성 클라이언트·서비스 키 포함을 검사한다. 이 검사는 최종 인수 옵션과 무관하게 기본 필수 검사다.
- 별도 API 35 에뮬레이터에서 `connectedDebugAndroidTest`를 실행한다. APK 빌드만 성공한 것을 기기 테스트 성공으로 표시하지 않는다.
- 기본 계측 범위는 아래 14개 클래스, 현재 소스 기준 63개 테스트다. 실행 필터와 `verify_android_test_results.py --require-class` 목록에 같은 클래스를 지정한다.
- 생성된 JUnit XML이 없거나 실제 성공한 테스트가 0개이거나 필수 클래스 중 하나라도 성공한 테스트가 없으면 실패한다. 실행 실패·XML 오류도 실패다.

| 기본 계측 클래스 | 테스트 수 | 보호하는 동작 |
| --- | ---: | --- |
| `Week13ScreenFlowTest` | 10 | 작은 화면·큰 글씨, 시작/이어하기, 도전 코드, 외형 저장과 주사위 계산 |
| `Week13FrontendFinishFlowTest` | 8 | 첫 컷신·복원, 자동 사건 기록과 능력치, 200% 글씨에서 상세 열기·본문 스크롤 시 자동 진행 정지와 명시 재개 |
| `ResultRewardOnceTest`, `CompletedLifeRecoveryTest` | 7 | 보상 중복 방지, 손상·미래 버전 완료 기록 보존 |
| `ContentAndSaveFailureTest` | 8 | 필수 콘텐츠 오류, 손상 저장 원본 보존, 메모리 반영과 디스크 저장 구분, 저장 실패 재시도, 완료 기록 중복 방지 |
| `ExperienceEffectsTest` | 7 | 설정 저장, 키보드·터치·접근성 입력, 최신 콜백, 시스템 동작 줄이기, 화면 수명주기에 따른 애니메이션 정지 |
| `HomeLobbyTest` | 7 | 첫 방문·진행 중·완주 후 로비, 저장 덮어쓰기 확인, 실제 기록 연결, 작은 화면·큰 글씨, 시스템 아이콘 대비·복원 |
| `EndingDurabilityRecreationTest` | 1 | 화면 재생성·뒤로 가기에도 종료 저장 완료를 기다리고, 완료 기록 저장·진행 슬롯 제거 후 한 번만 로비 진입 |
| `Dice3DTest` | 5 | 실제 GL 프레임, 주어진 여섯 눈 표시, 동작 줄이기, GL 사용 불가 시 정적 대체, 프로필 재굴림 픽셀 비교, 렌더링 중단 복구 |
| `PortraitVariantTest` | 2 | 세로 파생본과 가로 원본 선택, 원본·파생본 지문·치수·검수 상태 오류의 원본 복귀 |
| `CompletionCelebrationTest` | 4 | 실제 기록증 칭호 배지와 완주 표시, 입력 유지, 복원·다시 보기 시 재생 방지, 수명주기·움직임 설정 반영 |
| `SceneAtmosphereFlowTest` | 2 | 대표 실제 사건 3개의 움직임, 움직임 끄기와 그림만 보기의 정적 원본, 선택·결과 콜백 미호출 |
| `AvatarBlinkTest` | 1 | 눈을 감았다 뜬 뒤 저장된 외형 복원 |
| `SceneDepthTest` | 1 | 세 장면의 깊이 변화와 원화 픽셀 복원 |

기본 검사 통과는 최종 개발 인수 완료를 뜻하지 않는다. 의미 검토와 이미지 검수의 대기 항목, 시뮬레이션의 경로 도달성 목표는 보고서에 그대로 남는다.

## 최종 인수 실행

`workflow_dispatch`의 `final_acceptance=true`로 실행한다.

1. 기본 검사와 함께 클래스 필터 없는 전체 Android 계측 테스트를 실행한다. 모든 사건 이미지를 순회하는 비용이 큰 검사는 이 범위에 유지하며, 기본 실행 필터에는 추가하지 않는다.
2. `validate_week10.py --acceptance`로 의미 검토 미완료와 실행 연결 누락을 실패 처리한다.
3. `validate_followup_image_coverage.py --acceptance`로 이미지 최종 검수 미완료를 실패 처리한다.
4. `summarize_week10.py --acceptance`로 시뮬레이션 불변식과 목표 경로 재현 기준을 실패 처리한다.

각 최종 인수 검사는 별도 단계다. 한 검사가 실패해도 취소하지 않는 한 나머지 검사 결과를 생성한다. 승인 기록·검수 지문·게임 조건을 자동으로 완화하지 않는다. 서명된 배포 파일과 실기기 성능 검증은 이 CI 통과만으로 인증되지 않는다.

## 의미 검토 대기 사유

- `MISSING_REVIEW`: 해당 사건·선택의 검토 기록이 없다.
- `SOURCE_CHANGED`: 기존 검토는 있지만 제목·본문·행동·조건·사건 종류로 만든 지문이 현재 콘텐츠와 다르다. 이전 지문과 판정을 보고서에 남기고 새 의미 검토를 기다린다. 단순 문장부호 수정인지 여부는 지문만으로 판정하지 않는다.
- `REVIEW_ACTION_REQUIRED`: 현재 콘텐츠에 대한 검토가 있으나 보완 판정이 해결되지 않았다.

모두 최종 인수에서는 차단 항목이다. `semantic_review.json` 승인값을 일괄 갱신하지 않는다.

기존 V1 기록은 선택 개방성의 검토다. 새 V2 기록은 사건 발생 조건과 해당 선택의 모든 결과·보상·플래그를 읽고 검토하며, 별도 `fullSourceHash`로 검토 범위의 변경을 감지한다. V1 기록을 V2 검토로 일괄 승격하지 않는다. V2에는 검토자·날짜·검토한 결과 ID·개별 근거를 남긴다. 원문의 이미지 ID·그림 시점 변경은 별도 이미지 검수로 다루며 텍스트 의미 검토가 그림 승인까지 대신하지 않는다.

## 실패 증거

성공·실패와 무관하게 데이터·시뮬레이션·lint·단위 테스트·계측 XML·APK를 아티팩트로 보존한다. 에뮬레이터 부팅 로그와 logcat도 남기며 CI 전용 에뮬레이터는 종료한다. 부팅 대기는 최대 6분, 기기 테스트 단계는 최대 20분이다.

기기 실행 명령과 SDK 옵션은 [Android 계측 테스트 명령](https://developer.android.com/studio/test/command-line), [에뮬레이터 명령](https://developer.android.com/studio/run/emulator-commandline), [AVD 생성 명령](https://developer.android.com/tools/avdmanager)을 따른다. GPU는 폐기 예정인 `swiftshader_indirect` 대신 현재 [공식 소프트웨어 렌더링 옵션](https://developer.android.com/studio/run/emulator-acceleration)의 `software`를 사용한다.
