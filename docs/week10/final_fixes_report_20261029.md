# 10주차 통합본 잔여 수정 및 회의 시연 검사

기준일: 2026-10-29. 작업 브랜치: `codex/week10-final-fixes`.
PR 44 기반에 PR 39·41을 추가한 로컬 통합본 `6560b3b`에서 시작했다.
원격 main 병합·푸시는 이 작업에 포함하지 않았다.

## 완료한 수정

| 순서 | 수정 | 검증 |
|---|---|---|
| 1 | 현재 상태 플래그를 지워도 과거에 얻은 업적은 보존 | 자녀·업적 보존 회귀 검사 |
| 2 | 결과 화면의 ‘새로 열린 길’을 저장하고 복원 | JSON 왕복·구버전 호환, 기존 실패 Android 검사 재통과 |
| 3 | 이미 있던 플래그를 재획득하면 최신 사실로 복원 | 결혼→이혼→재혼 후 동기화 결과 일치 |
| 4 | 개인 PC 원본·geometry 파일 없이 이미지 검증 | PNG 115개, 648개 조합, 장면 24개 통과 |
| 5 | 완료 인생 중 일부가 손상돼도 정상 기록 복구 | 손상 원문 별도 백업, 미래/깨진 형식 보존 및 새 기록 별도 저장 Android 검사 |
| 6 | 각 선택 결과 당시의 체형을 저장해 주마등에 반영 | 과거 체형·시기·저장 왕복 검사. 기존 기록은 기본 체형 사용 |
| 7 | 변경·미해결 선택지 45개 개별 재검토 및 실제 칭호 판정 비교 | 의미 검토 미해결 0, 10,000회 칭호 불일치 0 |

12개 선택지에서 행동 의사 자체에 붙은 조건을 제거했다. 도움 요청·손잡기·양보·재응시,
이미 합격한 회사 선택 등이 대상이다. 시험 수행·신체 수행·구매 자금 등 의미가 직접 연결되는
조건은 유지했다. 개별 판단 근거는 `docs/week10/qa/semantic_review.json`에 기록했다.

## 검증 결과

- Debug 169개 + Release 169개, 총 338개 단위 테스트 통과. 건너뛴 검사 없음.
- Python 검증기 회귀 검사 14개 통과.
- 콘텐츠·이미지 경로·기존 경로 계약·10주차 정적 인수 검사 통과.
- 실제 콘텐츠 10,000회 실행, 불변식 오류 0건, 동일 정책·시드 재실행 비교 20회.
- 실제 앱 엔진과 독립 QA 계산의 칭호 불일치 0건.
- 결말 66개 모두 합성 조건 상태에서 실제 엔진 판정 일치.
- Debug·Release APK 내부 PNG 160개 각각 저장소 원본 바이트와 일치.
- Android 16/API 36 전체 검사 43개 통과 (213.92초).

## 남은 한계

10,000회 일반 플레이 표본에서 결말 56개를 관측했다. 아래 10개는 합성 조건 판정만
확인했으며 정상 플레이 도달 경로까지 증명한 것은 아니다.

`ending_ai_storyteller`, `ending_calm_recovery`, `ending_fragile_but_alive`,
`ending_late_blooming_developer`, `ending_lucky_adventurer`, `ending_rescue_survivor`,
`ending_star_athlete`, `ending_steady_app_maker`, `ending_tiny_startup_survivor`, `ending_wealth_builder`.

실제 외부 AI 생성 연동은 기존과 같이 Fake/로컬 대체 범위다. 이번 결과를 실서비스 AI 연동 완료로
보지 않는다. 이번 검사에는 새로운 수동 시각 검사나 모든 기종 검증은 포함하지 않았다.

## 시연 APK와 재현

회의용 설치 파일: `인생게임_10주차_회의시연용.apk` (Debug).
SHA-256: `624f64fafc7caab4c17e95ed396686bd543596f832d2ed33c41c7121294d8ad7`.

```powershell
.\gradlew.bat test assembleDebug assembleRelease assembleDebugAndroidTest --no-daemon --console=plain
python scripts/validate_week10.py --acceptance
python scripts/validate_week10_assets.py
.\gradlew.bat week10Simulation -Pweek10SourceCommit=<검증할커밋> -Pweek10ReportDir=<결과폴더> --no-daemon --console=plain
```

시연 순서: 새 인생 시작 → 선택지·조건 안내 확인 → 선택 결과·새 사실·새로 열린 길 확인 →
결과 화면에서 앱 재시작 후 이어하기 → 성장과 다른 체형 확인 → 결말·칭호·주마등 확인 →
홈의 지난 인생 재열람. 개발용 합성 상태를 일반 플레이 시연으로 사용하지 않는다.

