# 9주차 최신 통합 검증 — 2026-10-26

## 통합 대상

- 기준 main: `2b4620d`, 8주차 통합 PR #36.
- `abb88f2` / feat/week9-backend-balance: 실제 콘텐츠 기반 다중 시드 테스트 및 보고서.
- `7fd1cfb` / feat/week9-kihoon-scenario / PR #37: 결과 갈래·사인·칭호 데이터, 수능 시점 및 초점 조정.
- 두 브랜치를 모두 포함하는 통합 지점: `623dbfd`.
- 통합 후 표현용 `image_routing.focusStats.focus_child_help`를 social→health로 조정했다. 최근 콘텐츠의 건강 초점과 배경 테마를 맞춘 것이며 엔진 계산 변경은 아니다.
- 감사 스크립트로 현재 사건 분류·선택지 조건 보고서를 재생성했다. 10주차 과제·인수 명세·과거 설계 보존본을 추가했다.

## 데이터 검사

아래 명령은 통합 콘텐츠를 대상으로 통과했다.

```powershell
python scripts/validate_content.py
python scripts/validate_images.py
python scripts/validate_week8_contracts.py
python scripts/audit_choice_conditions.py
python scripts/audit_event_stat_classification.py
```

사건 281, 선택지 796, 결말 65. 전환점/돌발 중점 범위 48사건·140선택지, 명시 조건 70·열린 대안 70. 일반 233사건·656선택지 중 477개는 정책상 신규 잠금 없음. 조건 감사 누락·잘못된 조건·대안 없는 사건 0건. 실제 main PNG는 21개이며 debug 중복은 가산하지 않았다.

## 호스트 빌드와 단위 테스트

환경: Windows, JDK 17, Gradle 8.9, Android SDK는 기존 local.properties 사용.

원래 한글 작업 경로에서 `test assembleDebug` 실행 시 테스트 클래스 16종이 ClassNotFoundException으로 초기화에 실패했다. 클래스 파일은 생성돼 있었다. 같은 커밋을 영문 경로 `C:/lifegame-week10-verify-20261026`의 분리된 Git 작업 트리에서 다시 실행한 결과는 다음과 같다. 경로에 따른 실행 환경 문제로 판단하며, 검사를 비활성화하거나 테스트 코드를 수정하지 않았다.

```powershell
.\gradlew.bat test assembleDebug --no-daemon --max-workers=2
```

- BUILD SUCCESSFUL.
- debug 단위 테스트 **133개, 실패 0, 오류 0, skip 0**.
- release 단위 테스트 **133개, 실패 0, 오류 0, skip 0**.
- debug APK 빌드 성공. release 단위 테스트 성공은 release APK 빌드·서명 완료와 같은 뜻이 아니다.
- 로그: 로컬 `C:/Users/hansung/Desktop/week10-integration-20261026/gradle-ascii.log`. 사용자별 절대 경로는 팀원에게 강제하지 않으며 영문 경로 clone/worktree에서 같은 명령으로 재현한다.

## 실제 350회 비교군 결과

Week9BalanceSimulationTest, 시드 20261000~20261049 ×7경로. 초점은 목표 스탯에 맞추고 실행 가능한 선택지 중 시드/사건/시기/해 해시로 고른다. 목표 스탯 최대 보상 선택 정책과 다르다. debug와 release의 분포는 같으며, 두 번 실행한 350회를 700개의 서로 다른 표본으로 세지 않는다.

| 경로 | 전용 결말 | 비율 | 도달 결말 종류 | 노년 이전 사망 |
| --- | ---: | ---: | ---: | ---: |
| 건강 | 27/50 | 54% | 8 | 0 |
| 운동능력 | 20/50 | 40% | 8 | 0 |
| 지력 | 43/50 | 86% | 5 | 1 |
| 사회성 | 24/50 | 48% | 11 | 0 |
| 경제력 | 20/50 | 40% | 11 | 0 |
| 행복 | 5/50 | 10% | 14 | 1 |
| 운 | 3/50 | 6% | 11 | 1 |

영아·유년 사망 0, 모든 선택지 잠김 0, 150단계 내 미종료 0, 사망 시기 불일치 0. 노년 이전 사망 총 3/350. 경로 전용 결말은 해당 ending_path 접두어 기준이므로 유사 테마의 일반 결말까지 포함한 체감과 구분한다.

## 검증 범위와 남은 일

이번 통합 검사는 9주차 결과가 기존 게임에 합쳐져 빌드·단위 테스트·정적 데이터 검사를 통과하는지를 확인했다. **10,000회 장기 검증, 기기 UI 인수, 완료 인생 영구 저장, 칭호 실제 연결, 성장 캐릭터 조립, 실제 AI 생성 서비스는 완료한 것으로 보지 않는다.** 행복·운 밸런스가 완성됐다고 평가하지 않는다. 10주차 최종 인수 명세와 담당 숙제로 넘긴다.

## 최종 확인

검증 소스 커밋: `2f7c8cb`(9주차 두 브랜치, 10주차 문서, 건강 초점 이미지 태그, 테스트 입력 보완 포함).

실제 JSON을 파일로 읽는 테스트가 콘텐츠만 바뀌었을 때도 재실행되도록 `app/build.gradle.kts`의 Test 입력에 `src/main/assets`를 추가했다. 이 변경 뒤 아래 명령을 다시 실행했다.

```powershell
.\gradlew.bat test assembleDebug assembleRelease --no-daemon --max-workers=2
```

- BUILD SUCCESSFUL, debug/release 단위 테스트 각각 133개 모두 통과. XML 갱신 시각으로 테스트가 재실행된 것을 확인했다.
- debug 설치 APK 및 **서명 없는 release APK** 빌드 성공, release lintVital 통과.
- 두 APK를 ZIP으로 열어 실제 PNG 21개와 `focus_child_help: health` 라우팅 포함을 확인했다.
- debug SHA-256: `6a5520ddc20dd630bfc3995eaf99ae25b61e0245a5f7b50d30fcd676772035ea`.
- unsigned release SHA-256: `5c1fc489aca9296f87f28700e7faf46af458e5f02d47d4c97ac41674533acfb1`.
- 최종 로그: 로컬 `C:/Users/hansung/Desktop/week10-integration-20261026/gradle-content-inputs.log`.
- 설치용 사본: 로컬 `C:/Users/hansung/Desktop/week10-integration-20261026/app-week9-integrated-debug.apk`. 이번 작업에서 기기 설치·기존 플레이 갱신은 수행하지 않았다.

원격 브랜치도 추가 대조했다. 미포함처럼 보이는 과거 인수인계 브랜치의 문서는 현재 main의 파일과 동일하고, wrapper 줄 끝 정책도 이미 반영돼 있다. 과거 6주차 UI 브랜치 전체를 다시 병합해 현행 8·9주차 기능을 되돌리지 않았다.
