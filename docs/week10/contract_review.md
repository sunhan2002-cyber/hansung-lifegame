# 김선한 QA의 현재 계약 대조

2026-10-26. 게임 기준 main 0991b33. **김우영의 공유 contracts.md는 아직 없다. 아래는 현재 소스 관찰이며 팀 합의나 최종 계약 승인이 아니다.**

| 항목 | 현재 실제 구현 | 최종 명세와의 차이·담당 |
| --- | --- | --- |
| 능력치·시기 | 7스탯, 6시기×5해. StatConditions는 min/max 모델 보유 | V4ContentParser는 최소값만 읽는다. maxStats 사용 시 새 검사기의 최종 인수 모드가 실패한다. 김우영 |
| 결과 건강 | statDelta.health + lifeDelta + 연간 감소 | adult_070의 좋은 재취업 a_better에도 lifeDelta -25. 제거/명시 우선 규칙을 합의해야 한다. 김우영·신기훈 |
| 플래그 | 결과 addFlags와 완료 사건 플래그를 합집합 | removeFlags 2개는 파서·엔진 미지원. path_soc_07:A:a_silent, path_hap_05:A:a_couple. 김우영 |
| 저장 | V4SavedRun(playerName, state), saveFormatVersion=2 기록 | decode는 버전을 검사하지 않는다. 선택 결과·화면 phase·완전한 choiceLog·완료 목록 저장 없음. 김우영 |
| 화면 복원 | pending 사건과 엔진 상태 복원 | continueLife가 outcome을 null로 초기화한다. 결과 화면이 생략된다. 김우영(저장)·김재겸(화면) |
| 완료 인생 | ViewModel의 mutableState 목록 | 새 ViewModel에서 빈 목록. runId별 영구 저장 및 재열람이 필요하다. 김우영·김재겸 |
| 칭호 | JSON 47개, 기본 1개, 플래그 참조·조건 정적 검사 통과 | 실제 parseTitles/판정/수집 UI 연결 미구현. QA의 expectedTitleId는 독립 기대값일 뿐이다. 김우영·김재겸 |
| 종료 | RunEndScreen에서 요약·사실·선택 이력 표시 | 주마등→미래 모습→기록증 전환 미구현. 김재겸 |
| 캐릭터 | 6시기 generic 초상, 7스탯 장면, 실제 main PNG 21개 | 같은 인물의 18체형 조립·얼굴 anchor·외형/사진 설정 없음. 정원률·김재겸 |
| 생성 | 게임은 오프라인 assets 사용 | GenerationClient/Fake·실패 대체 계약 없음. 실제 서버뿐 아니라 Fake 인수도 아직 불가. 김우영·김재겸 |

healthStatus/wealthNote 규칙은 현재 엔진 사실 모델이 아닌 UI 카탈로그가 표시하는 필드이므로 무조건 미지원으로 분류하지 않았다. 반대로 없는 fact 타입이나 중복 flag/fact 매핑은 새 검증기에서 실패 처리한다. 얼굴 시트·투명 몸 소재의 크기/비율 계약이 오면 기존 장면용 4:3 이미지 검사를 유형별로 분리해야 하며 전체 이미지 검사를 끄면 안 된다.

## 계약이 제출되면 재검토할 항목

1. 저장 버전·현재 슬롯/완료 목록·결과 phase·선택/결과 ID와 표시 문구·이미지 참조 스냅샷.
2. 칭호 우선순위/동률/기본값과 기존 47개 JSON 필드 호환성.
3. removeFlags와 addFlags 충돌 순서, 현재 사실·과거 업적의 구분.
4. 건강 명시값·legacy lifeDelta·연간 감소의 적용 순서와 콘텐츠 마이그레이션.
5. bodyType/faceSheet/anchor·생성 DTO·취소/실패/타임아웃 계약.

검사기와 회귀 테스트는 이 계약에 맞춰 갱신한다. 실제 JSON과 모델을 연결하지 않은 문서만으로 통과 처리하지 않는다.
