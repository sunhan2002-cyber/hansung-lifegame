# 10주차 시연·결함 재현 안내

현재 빌드는 최종 인수 FAIL인 QA 베이스라인이다. 정상 동선과 알려진 실패를 함께 보여준다. 사진·칭호·성장 합성·주마등을 구현한 것처럼 시연하지 않는다.

## 1. 실제 앱 시연

설치용 로컬 사본: `C:/Users/hansung/Desktop/week10-sunhan-qa-20261026/lifegame-week10-sunhan-qa-debug.apk`. 체크섬은 `qa/build_evidence.json`.

1. 비행기 모드에서 실행한다. 홈→새 인생 살기→이름/성별/기질 선택→시작.
2. 영아기 돌잡이에서 3선택지·건강·시기를 확인한다. 본문 타이핑이 끝난 뒤 조작한다. 첫 터치는 타이핑을 마치는 데 쓰일 수 있다.
3. 장면 보기→UI와 블러 제거→화면 한 번 터치→선택지 복귀를 확인한다. 현재 장면 보기에는 상하 여백이 있으며 최신 화면 시안은 미반영이다.
4. 선택 전 앱을 force-stop하고 재실행→이어하기: 같은 돌잡이·능력치가 유지된다.
5. 선택해서 결과 문장과 변화/사실을 확인한다. 결과 화면에서 force-stop→재실행→이어하기: **현재 결과 화면이 사라지는 결함이 재현된다.**
6. 유년기부터 초점을 선택하고 다음 결정까지 진행한다. 잠금이 나타나면 실제 잠금 이유와 열린 대안을 확인한다.
7. 결말의 실제 시기·사인·교육/직업/가족/주거·주요 선택을 읽는다. 과거 인생 저장·칭호·종료 연출은 아직 최종 구현이 아니다.

시드 입력 UI는 현재 없다. 따라서 아래 고정 시드의 엔진 실행과 실제 앱의 시간 기반 시드를 같은 결과라고 약속하지 않는다.

## 2. 엔진의 고정 시드 재현

`qa/demo_seeds.json`에는 경로마다 전용 결말 성공 1개와 다른 결말 1개의 정책·시드·시작 능력치·전체 사건/선택/결과 순서가 있다. `summary.json`은 경로별 500회 분포다.

```powershell
.\gradlew.bat :app:week10Simulation -Pweek10Policy=RANDOM -Pweek10Target=HEALTH -Pweek10Seed=20261002 --no-daemon --max-workers=2
```

단일 재현은 `app/build/reports/week10/replay.json`에 기록된다. 정책은 RANDOM/TARGET/LOW_HEALTH/LOW_STATS/BOUNDARY, 목표는 HEALTH/FITNESS/INTELLIGENCE/SOCIAL/WEALTH/HAPPINESS/LUCK다. 다른 시연은 demo_seeds.json의 값을 사용한다. 임의 플래그/결말을 주입하지 않고 실제 세션을 처음부터 실행한다.

10,000회 전체 재현:

```powershell
.\gradlew.bat :app:week10Simulation --no-daemon --max-workers=2
python scripts/summarize_week10.py --input app/build/reports/week10 --out docs/week10/qa
```

예상 결과: 치명 불변식 0, 행복 RANDOM 4.8%·운 RANDOM 16.4%로 최종 목표 미달. 다른 콘텐츠 커밋에서는 분포가 달라질 수 있으므로 assetSha256과 커밋을 함께 비교한다.

## 3. 기기 테스트 재현

기존 사용자 진행이 있는 기기 대신 테스트용 AVD를 사용한다. 이번 검증은 별도 read-only/no-snapshot emulator-5580에서 수행했다. AVD 이름과 실제 Android 버전은 다를 수 있으므로 getprop으로 기록한다.

```powershell
adb -s emulator-5580 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5580 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5580 shell am instrument -w -r -e class com.example.lifegame.ui.Week8SaveIntegrationTest,com.example.lifegame.ui.Week8LifePathScreenTest,com.example.lifegame.ui.Week7FlowSmokeTest,com.example.lifegame.ui.Week10AcceptanceProbeTest com.example.lifegame.test/androidx.test.runner.AndroidJUnitRunner
```

현재 10개 중 7개 통과, 3개 실패가 기준선이다. 테스트는 개인 저장 슬롯과 분리된 SharedPreferences를 쓰며, ViewModel 재생성과 실제 프로세스 종료는 별도 증거로 구분한다. Android runner의 최종 INSTRUMENTATION_CODE만 보고 성공으로 판단하지 말고 개별 상태 코드와 FAILURES를 확인한다.

## 4. 최종 회수 때 추가할 시연

김우영 저장·칭호·Fake, 김재겸 종료/기록·최신 건강 UI, 정원률 성장 소재, 신기훈 새 콘텐츠가 통합되면 사진 취소/실패, 건강 색 4경계, 완료 인생 3개 재열람, 실제 주마등·미래 모습·칭호를 추가한다. 아직 확인할 수 없는 기능을 성공한 것으로 보고하지 않는다.
