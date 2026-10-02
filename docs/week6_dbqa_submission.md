# 6주차 DB·QA 제출 요약

담당자: 김선한  
역할: DB·콘텐츠 데이터 / QA  
브랜치 권장명: `feat/week6-db-qa-sunhan`  
PR 제목 권장: `[Week6] DBQA - 김선한 - 데이터 검증과 PR 체크리스트 추가`

## 1. 수행한 작업

| 구분 | 내용 |
| --- | --- |
| 결말 데이터 | `endings.sample.json` 결말을 26개에서 31개로 확장 |
| 이미지 ID 매칭 | 사건 150개의 `imageId`가 `image_ids.sample.json`에 모두 존재하는지 확인 |
| 데이터 스키마 | `docs/data_schema.md`를 6주차 실제 JSON 기준으로 갱신 |
| 자동 검증 | `scripts/validate_content.py` 추가 |
| PR 검증 기준 | `docs/qa/week6_pr_checklist.md` 작성 |
| 통합 검증표 | `docs/qa/week6_integration_check.md` 작성 |
| 테스트 케이스 | `docs/qa/week6_test_cases.md` 작성, 총 44개 테스트 케이스 정리 |
| 테스트 경로 | Kotlin 단위 테스트 파일을 표준 경로 `app/src/test/kotlin`으로 이동 |

## 2. 현재 데이터 수치

| 항목 | 수치 |
| --- | ---: |
| 사건 | 150개 |
| 일반 사건 | 140개 |
| 돌발 사건 | 10개 |
| 결말 | 31개 |
| 이미지 ID | 232개 |
| 캐릭터 | 3개 |
| 기본 결말 | 1개 |

성장 단계별 사건 수:

| 단계 | 사건 수 |
| --- | ---: |
| INFANT | 24 |
| CHILD | 43 |
| TEEN | 33 |
| ADULT | 50 |

## 3. 6주차에 추가한 결말

| endingId | 제목 |
| --- | --- |
| `ending_late_blooming_developer` | 늦게 피었지만 오래 가는 개발자가 되었다 |
| `ending_everyday_athlete` | 매일 조금씩 강해지는 생활 체육인이 되었다 |
| `ending_people_connector` | 사람들을 자연스럽게 이어 주는 사람이 되었다 |
| `ending_small_stage_big_memory` | 작은 무대를 큰 추억으로 만드는 사람이 되었다 |
| `ending_tiny_startup_survivor` | 작게 시작해 끝까지 버티는 창업자가 되었다 |

## 4. 자동 검증 결과

실행 명령:

```bash
python scripts/validate_content.py
```

결과:

```text
PASS: content JSON files are valid.
```

경고:

```text
event count is 150; week6 scenario target is 200+
```

이 경고는 신기훈 시나리오 담당의 6주차 목표와 연결된 항목이다. DB·QA 기준에서는 현재 사건 150개의 JSON 형식, 선택지 수, 중복 ID, imageId 매칭이 통과했다.

## 5. 빌드 및 테스트 결과

현재 프로젝트 폴더 경로 `C:\Users\hansung\Desktop\고급모바일프로그래밍`에서는 Gradle/JUnit worker classpath에 한글 경로가 깨져 전달되어 `ClassNotFoundException`이 발생했다. 같은 소스를 ASCII 경로 `C:\lifegame-week6-dbqa-verify-20261002-1429`에 복사해 검증한 결과는 다음과 같다.

| 명령 | 결과 |
| --- | --- |
| `python scripts/validate_content.py` | PASS |
| `./gradlew.bat clean test` | BUILD SUCCESSFUL |
| `./gradlew.bat assembleDebug` | BUILD SUCCESSFUL |

## 6. PR 설명문 초안

```text
## 작업 내용
- endings.sample.json 결말을 31개로 확장했습니다.
- scripts/validate_content.py를 추가해 사건/결말/이미지 ID/캐릭터 JSON을 자동 검증하도록 했습니다.
- docs/data_schema.md를 6주차 실제 데이터 기준으로 갱신했습니다.
- 6주차 PR 체크리스트, 통합 검증표, 테스트 케이스 문서를 추가했습니다.
- Kotlin 단위 테스트 파일을 표준 테스트 경로 app/src/test/kotlin으로 이동했습니다.

## 검증 결과
- python scripts/validate_content.py: PASS
- ./gradlew.bat clean test: PASS, ASCII 검증 경로에서 확인
- ./gradlew.bat assembleDebug: PASS, ASCII 검증 경로에서 확인

## 참고
- 현재 사건 수는 150개이며, 6주차 시나리오 담당 PR에서 200개 이상으로 확장해야 합니다.
- 현재 사건 imageId 누락은 0개입니다.
```
