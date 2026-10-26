# 10주차 최종 인수 보고 — 김선한 QA 베이스라인

2026-10-26. **최종 앱 인수: FAIL.** QA 도구·기준선 측정을 수행했으며 미완료 기능을 통과로 처리하지 않았다. 다른 담당자의 10주차 구현이 합쳐지면 같은 검사로 재판정해야 한다.

## 실행 기준과 결과

- 게임 기준: main `0991b33`. 검사 소스: `6b3730e`. 게임 코드·콘텐츠의 동작을 이번 QA 브랜치에서 임의 변경하지 않았다.
- 자동 데이터 검사: 281사건·796선택지·945결과·65결말·47칭호, 구조 오류 0. 파서 미지원 removeFlags 2곳 확인.
- 의미 검토: 전환점·돌발 140선택지 중 열린 대안 유지 70, 조건 유지 29, 조건 재설계 권고 26, 의도/문구 보완 15. 전체 796개 행은 감사 JSON에 있으며 일반 사건의 새 잠금을 강제하지 않았다.
- Python 검사기 회귀 13개 통과. Kotlin/Android 호스트 단위 테스트 debug/release **각 135개, 실패 0, skip 0**. 1,000사건 메모리 로딩 및 실제 최소 스탯 경계 시험 포함.
- 실제 엔진 기본 표본 10,000회 + 별도 동일 시드 재실행 20회. 기본 표본의 치명 불변식 위반 0. 목표 미달: 행복 RANDOM 4.8%, 운 RANDOM 16.4%.
- Android instrumentation **10개 중 7개 PASS, 3개 FAIL**. 결과 복원·완료 목록 저장·알 수 없는 저장 버전에서 실패.
- 실제 프로세스 force-stop: 선택 전 pending 사건·저장 상태 일치 PASS, 선택 후 결과 화면 복원 FAIL. 각각 새 PID를 확인했다.
- 기기: AVD 이름은 Pixel_6a_API35이나 **실제 getprop 값은 Android 16 / SDK 36**이다. 이름만 보고 API35로 보고하지 않았다. emulator-5580을 read-only/no-snapshot으로 따로 실행했고 비행기 모드 1·Wi-Fi 해제로 기본 흐름을 검증했다.
- debug/release/AndroidTest APK 빌드 성공. release는 unsigned이며 설치용은 debug다. 이미지 21개 등 리소스 수와 설치 APK 체크섬은 qa/build_evidence.json을 참고한다.

## 요구사항별 판정

| 요구 | 판정 | 증거와 한계 |
| --- | --- | --- |
| F10-01 오프라인 진행 | 범위 내 PASS | 비행기 모드의 실제 Activity 새 게임→결말 smoke, 10,000회 진행/잠금 불변식. 모든 기기/모든 경로의 UI를 완주한 것은 아님 |
| F10-02 7경로 인과성 | FAIL | 스탯 지향 성공 재현은 모두 존재하나 행복/운 무작위 도달 목표 미달. 사건·사실·결말 분포와 시드 공개 |
| F10-03 건강·사망 | 부분 PASS | 10,000회 영아/유년 사망·시기 불일치 0, 실제 렌더링 청소년 사망 표기 확인. 청년/중년 사망의 별도 UI 시연은 미검증 |
| F10-04 건강 의미 일치 | FAIL | adult_070의 좋은 재취업도 -25, legacy 회복 정책 남음. 상태 제거 플래그도 미지원 |
| F10-05 인생 사실·요약 | 부분 확인 | 7경로 결말 UI 사실·가독성 검사 통과. 특정 직업을 단정하는 옛 결말 전체 의미 일관성은 미확정 |
| F10-06 칭호 | FAIL | 47개 데이터의 기본값/조건/참조 검사는 통과. 앱 실행 연결 없음 |
| F10-07 영구 저장·복원 | FAIL | instrumentation 실패 3개, 실제 force-stop 결과 복원 실패. 선택 전 사건은 일치 |
| F10-08 종료 경험 | FAIL | 현재 요약 화면은 있으나 주마등→미래 모습→기록증 흐름 없음 |
| F10-09 성장·체형 | FAIL | 21개 이미지와 generic 초상은 있으나 동일 인물 6시기×3체형 조립 없음 |
| F10-10 사진·Fake | FAIL/실행 불가 | 사진 선택·동의/삭제·Fake 생성 계약/구현이 없으므로 실패·타임아웃 인수 불가 |
| F10-11 최종 UI | FAIL | 실제 화면에 상단 흰 카드·건강 숫자·초점 박스 유지. 건강 색/시기 하단 등 최신 시안 미반영. 작은 화면·큰 글꼴의 기존 8주차 항목은 통과 |
| F10-12 데이터·의미 | FAIL | 구조 검사 PASS, 의미 검토 41개 재설계/명확화 미확정. 신규 204개는 아직 없어서 검사할 수 없음 |
| F10-13 장기 시뮬레이션 | 실행 완료 / 목표 FAIL | 10,000회 수행, 치명 불변식 0, 행복/운 기준 미달. 미관측 결말 12개 재현/조건 검토 남음 |
| F10-14 빌드·인수 | 부분 완료 | 호스트 테스트·APK·CI·시연 절차 회수. 전체 앱의 최종 제출 판정은 위 실패 해결 후 가능 |

## 재검증 명령

```powershell
python scripts/validate_content.py
python scripts/validate_images.py
python scripts/validate_week8_contracts.py
python scripts/audit_choice_conditions.py
python scripts/audit_event_stat_classification.py
python -m unittest discover -s scripts/tests -v
python scripts/validate_week10.py
.\gradlew.bat test :app:week10Simulation assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon --max-workers=2
python scripts/summarize_week10.py --input app/build/reports/week10 --out docs/week10/qa
```

Windows 한글 경로의 JVM 테스트 클래스 로딩 문제가 있어 호스트 빌드는 영문 경로 작업 트리에서 실행했다. 최종 인수 모드는 `validate_week10.py --acceptance`와 `summarize_week10.py ... --acceptance`이며 **현재 둘 다 종료 코드 1**이다. 기본 데이터 구조 검사가 초록이어도 이 실패를 덮지 않는다.

## 제출된 증거

- `qa/content_audit.json`, `qa/content_audit.md`, `qa/semantic_review.json`: 전수 구조/의미 검토와 소스 해시. 원문이 바뀌면 이전 의미 검토를 무효화한다.
- `qa/simulation_report.md`, `qa/summary.json`, `qa/coverage.json`, `qa/demo_seeds.json`, `qa/ending_witnesses.json`: 분포·도달·재현. 칭호는 독립 QA 기대값이며 앱 구현 증거가 아니다.
- `qa/device_tests.json`, `qa/instrumentation.log`, `qa/process_restore.json`: 기기 통과/실패와 실제 프로세스 재시작.
- `qa/build_evidence.json`, `qa/strict_gate_results.json`: APK·테스트 수·최종 인수 명령 결과.
- `qa/team_fix_requests.md`: 담당자별 수정 요청 14개.
- 원본 10,000개 runs.jsonl, 빌드 로그, UI 캡처 31개는 로컬 `C:/Users/hansung/Desktop/week10-sunhan-qa-20261026` 및 CI 산출물로 보관하며 저장소에 대용량 바이너리를 커밋하지 않는다.

## CI 상태

GitHub Actions의 첫 실행은 SDK manager의 PATH 누락으로 실패했고 SDK 설치 경로를 명시해 수정했다. 수정 후 실행 [40279398196](https://github.com/sunhan2002-cyber/hansung-lifegame/actions/runs/40279398196)은 데이터 검사·단위 테스트·10,000회·APKs 모두 성공했다. 이는 해당 커밋의 호스트 CI 증거이며 최종 앱/기기 인수 PASS가 아니다. 최종 변경분 CI 결과는 개인 제출 문서에 추가 기록한다.

자동화 설정은 [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [setup-python](https://github.com/actions/setup-python), [upload-artifact](https://github.com/actions/upload-artifact)의 공식 사용법과 실제 태그 SHA를 확인해 고정했다.
