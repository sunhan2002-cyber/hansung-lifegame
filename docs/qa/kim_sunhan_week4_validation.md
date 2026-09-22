# 김선한 4주차 DB·QA 과제 검증 결과

검증일: 2026-09-21
담당자: 김선한
역할: DB·콘텐츠 데이터 담당 / 팀장 QA·문서화

## 최종 판정

**로컬 제출물 기준 통과**로 판정한다.

총 30개 검증 항목을 확인했고, 30개 모두 PASS했다. 다만 현재 팀 GitHub에는 Android 프로젝트 뼈대와 Gradle Wrapper가 아직 없으므로 `./gradlew test`, `./gradlew assembleDebug`는 실행하지 못했다. 이 두 명령은 김재겸 프론트엔드 프로젝트가 올라온 뒤 통합 검증 단계에서 실행한다.

## 생성 파일 확인

| 파일 | 결과 |
| --- | --- |
| `app/src/main/assets/endings.sample.json` | PASS |
| `app/src/main/assets/image_ids.sample.json` | PASS |
| `app/src/main/java/com/example/lifegame/data/ContentRepository.kt` | PASS |
| `app/src/main/java/com/example/lifegame/data/ProgressStore.kt` | PASS |
| `docs/data_schema.md` | PASS |
| `docs/qa/test_cases.md` | PASS |
| `docs/qa/before_after_check.md` | PASS |
| `docs/qa/8week_scope.md` | PASS |
| `docs/qa/review_checklist.md` | PASS |

## 데이터 검증

| 검증 항목 | 결과 |
| --- | --- |
| 결말 JSON 파싱 | PASS, 21개 |
| 이미지 ID JSON 파싱 | PASS, 80개 |
| 결말 20개 이상 | PASS |
| 이미지 ID 80개 이상 | PASS |
| 결말 ID 중복 없음 | PASS, 21/21 unique |
| 이미지 ID 중복 없음 | PASS, 80/80 unique |
| 기본 결말 존재 | PASS, `ending_ordinary_path` |
| 결말 필수 항목 누락 없음 | PASS |
| 이미지 필수 항목 누락 없음 | PASS |
| 결말 조건 지표명 허용 범위 | PASS |

## 코드 구조 검증

| 검증 항목 | 결과 |
| --- | --- |
| `ContentRepository.loadEvents()` | PASS |
| `ContentRepository.loadEndings()` | PASS |
| `ProgressStore.saveProgress()` | PASS |
| `ProgressStore.loadProgress()` | PASS |
| `ProgressStore.clearProgress()` | PASS |
| JSON 예외 처리 | PASS, `runCatching` / `getOrElse` 사용 |
| 저장 방식 | PASS, `SharedPreferences` 사용 |

## QA 문서 검증

| 검증 항목 | 결과 |
| --- | --- |
| 일반 테스트 10개 | PASS |
| 위험 테스트 10개 | PASS |
| 애매 테스트 10개 | PASS |
| 8주 범위표 수량 기준 | PASS |

## 확인하지 못한 항목

- Android 프로젝트에 병합한 뒤의 실제 빌드
- `./gradlew test`
- `./gradlew assembleDebug`

위 항목은 김재겸 프론트엔드 Android 프로젝트가 올라온 뒤에만 검증할 수 있다.

## 팀장 처리 기준

1. Notion에는 ZIP 제출 가능.
2. 아직 GitHub push는 하지 않는다.
3. 프론트엔드 프로젝트와 병합한 뒤 `./gradlew test`, `./gradlew assembleDebug`를 실행한다.
4. 통합 검증 통과 후 GitHub push하고 commit 링크를 Notion 카드에 남긴다.

## 결말명 톤 수정 기록

2026-09-21에 웹 자료를 다시 확인한 뒤 결말명을 수정했다. 참고한 방향은 인생 시뮬레이션 게임에서 직업, 활동, 관계, 돈, 행복, 불운, 특수 사건 누적이 결말을 가르는 구조다. 기존의 딱딱한 번역체 제목을 한국어 게임 UI에서 자연스럽게 보이는 문장형 제목으로 바꿨다.

예시:

- `균형을 이룬 삶` → `무난하지만 단단한 어른이 되었다`
- `천재 연구자의 삶` → `연구실 불빛을 끄지 않는 사람이 되었다`
- `자산을 일군 삶` → `돈 걱정은 덜 하는 삶을 만들었다`
- `황당 사건의 전설` → `친구들 술자리 단골 썰이 되었다`
- `나만의 길을 걸은 삶` → `평범하지만 내 인생이었다`
