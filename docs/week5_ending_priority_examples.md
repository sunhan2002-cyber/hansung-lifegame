# 5주차 결말 우선순위 판정 예시 — 김우영

## 판정 규칙

1. 기본 결말(`isDefault = true`)은 일반 후보에서 제외한다.
2. 지표 최솟값·최댓값, 필수 플래그, 차단 플래그를 모두 만족한 결말만 후보가 된다.
3. 후보를 `priority` 내림차순으로 정렬한다.
4. 우선순위가 같으면 `endingId` 오름차순으로 정렬한다.
5. 정렬 결과의 첫 번째 결말 하나만 선택한다.
6. 일반 후보가 없을 때만 기본 결말을 선택한다. 기본 결말이 여러 개면 `endingId`가 가장 빠른 하나를 선택한다.

## 판정 예시

| 예시 | 조건을 통과한 후보 | 정렬 결과 | 최종 선택 | 이유 |
| --- | --- | --- | --- | --- |
| 서로 다른 우선순위 | `ending_high(100)`, `ending_low(10)` | high → low | `ending_high` | 가장 높은 우선순위 |
| 같은 우선순위 | `ending_b(80)`, `ending_a(80)` | a → b | `ending_a` | 동점일 때 ID 오름차순 |
| 차단·필수 플래그 | blocked(100), missing_flag(90), eligible(10) | eligible | `ending_eligible` | 부적격 후보 제거 후 판정 |
| 일반 후보 없음 | 없음 | 없음 | `ending_default` | 기본 결말 폴백 |
| 동일 조건 25개 | priority 1~25인 결말 25개 | 25 → … → 1 | `ending_25` 하나 | 30회 반복해도 같은 하나만 선택 |

`GameEngine.resolveEndingDecision()`은 최종 결말, 정렬된 후보 ID, 판정 사유를 반환한다. 기존 호출부는 `resolveEnding()`을 그대로 사용하며 동일한 판정 함수를 거친다.

## 자동 검증

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

- 전체 단위 테스트 35개
- 실패 0개, 오류 0개, 건너뜀 0개
- Android 디버그 APK 빌드 성공
