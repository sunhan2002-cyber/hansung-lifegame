# Week7 UI/UX·도트 예시 팀원 인계

- 작성일: 2026-10-13
- 담당: 정원률 — UI/UX, 도트 스타일·예시 제작
- 대상: 신기훈(시나리오), 김선한(DB·QA), 김재겸(프론트), 김우영(엔진)
- 로컬 브랜치: `week7-uiux-wonryul`
- 기준 HEAD: `86cb6b728c90bce5de9a2fe24325ba1e8f8cb62e`와 그 위의 미커밋 변경
- 공유 상태: **인계 자료 작성 완료 / 팀원에게 실제 전달·원격 공유는 미실시**. 현재 로컬 파일을 원격 저장소에 게시한 상태로 취급하지 않는다. commit·push·PR 없음.

스타일·UI 수치, 네 화면 설계, 독립 프롬프트 8개, 개별 예시 이미지 8장, 임시 이미지 검수 화면을 준비했다. 중복 기능 구현은 김재겸 PR 기준으로 넘기고 이 브랜치에는 디자인·이미지 검수만 유지한다. [정리 범위](week7_frontend_alignment.md). 사용자가 2026-10-13에 이미지 8장의 육안 검수 완료를 보고했다.

**현재 이미지 원본은 1448×1086 PNG, 4:3이다. 300×225 축소는 사용자 요청으로 보류 중이며, 최종 앱 자산 규격 검증과 실제 게임 연결은 완료하지 않았다.**

### 300×225 처리 보류 사유

- **생성 도구의 설정:** 이번 대화에서 사용한 이미지 생성 도구에는 출력 해상도를 직접 지정하는 옵션이 없었다. 프롬프트의 300×225 문구만으로 실제 크기가 보장되지 않아, 8장 모두 1448×1086·4:3 원본으로 생성됐다. 별도 OpenAI API의 크기 설정은 대화에서 검토했지만 이번 제작에는 사용하지 않았다.
- **사용자의 진행 결정:** 사용자가 “축소는 일단 생략”하기로 하여 원본으로 장면·스타일·필름 표시를 확인하고, 예시 제작·육안 검수·인계 자료 준비까지 먼저 진행했다. 파일 축소는 보류 상태로 남겼다.
- **최종 적용 시 처리:** 최종 앱 규격 300×225는 계속 유지한다. 기존 원본이 같은 4:3이므로 다시 생성하지 않고 별도 축소할 수 있다. 처리 재개 후 작은 크기에서 도트·장면 가독성을 확인하고 최종 규격 검증을 진행한다.


## 1. 근거와 적용 범위

- 과제: 로컬에 fetch된 [Week7 팀원별 과제](https://github.com/sunhan2002-cyber/hansung-lifegame/blob/de214be5a2e5e1e1d5f3a1e187e70909f7af065f/docs/week7/week7_assignments_v4.md), 커밋 `de214be5a2e5e1e1d5f3a1e187e70909f7af065f`.
- 장면 내용·시점·금지 요소·imageId: 로컬에 fetch된 신기훈의 [도트 이미지 요청서](https://github.com/sunhan2002-cyber/hansung-lifegame/blob/f4800f0ec3af1f09b51ec2b263e1ef60dcf60ca2/docs/scenario/week7_pixel_scene_requests.md), 커밋 `f4800f0ec3af1f09b51ec2b263e1ef60dcf60ca2`.
- Figma는 빈티지 필름 분위기와 화면 흐름의 참고다. 문서의 디자인 수치는 사용자가 코드로 확인하며 확정한 값이며 Figma 원본 측정값이 아니다.
- 이번 예시 8장은 요청서 전체 15건 중 선택한 8건이다. 요청서의 나머지 7건을 이번 8장 예시 작업에서 제작한 것으로 표시하지 않는다.
- **필수 규격**, **사용자 확정 수치**, **권장 제작 방식**은 각 가이드의 표기를 따른다. 실제 고유 색상 수 40개 이하 계측은 미수행이며, 과제의 표현은 `40색 이하 느낌`이다.

## 2. 먼저 열어볼 산출물

| 자료 | 경로·용도 | 상태 |
| --- | --- | --- |
| 도트 스타일 기준 | [week7_pixel_art_style_guide.md](week7_pixel_art_style_guide.md) — 화풍·규격·금지 요소·제작/검수 기준 | 작성 완료 |
| 화면 설계 기준 | [week7_screen_layout_guide.md](week7_screen_layout_guide.md) — 홈/프로필, 장 표지·중점 선택, 자동 진행, 결정·돌발 | 네 화면 작성 완료, 컴포넌트 단위 포함 |
| 독립 프롬프트 안내 | [week7_image_generation_prompts.md](week7_image_generation_prompts.md) — TXT 1개로 이미지 1장 생성 | 8개 작성 완료 |
| 디자인 수치·이미지 검수 사용법 | [week7_ui_tuning.md](week7_ui_tuning.md) — Tokens 적용 범위, 실행 방법, 검사 기록 | 디자인 수치 확정, 현재 참조·후속 인계 값 구분 |
| 개별 예시·검수 상태 | [individual/v1/README.md](samples/week7/individual/v1/README.md) — 장면별 PNG·TXT와 실제 크기 | 예시 8장, 사용자 육안 검수 완료 |
| 실제 생성 입력 기록 | [prompts_used.json](samples/week7/individual/v1/prompts_used.json) — 생성 당시 TXT 본문 사본 | 보관 완료 |
| 원본·진행 상태 기록 | [generation_metadata.json](samples/week7/individual/v1/generation_metadata.json) — 크기·시점 수량·프롬프트 해시·검수/보류 상태 | 최종 앱 자산 승인과 구분 |

`promptSha256`은 생성에 사용한 UTF-8 텍스트(LF 줄바꿈)의 해시다. Windows TXT 파일의 CRLF를 포함한 원시 바이트 해시와는 다를 수 있다.

과거 모음 이미지 2장과 모음용 [prompts.json](samples/week7/prompts.json)은 생성 이력이다. 이번 인계 대상은 아래의 독립 TXT 8개와 개별 PNG 8장이다.

## 3. 장면 8건과 대응 파일

아래 전부 사용자 육안 검수 완료(2026-10-13), 현재 원본 1448×1086 PNG다. 연결 정보는 요청서의 참조이며 실제 게임 매핑에 적용되었다는 뜻은 아니다.

| 장면 | 시점 | 요청서 연결 | imageId·최종 예정 파일명 기준 | 프롬프트 | 원본 예시 |
| --- | --- | --- | --- | --- | --- |
| 유년기 친구 | 3인칭 | 유년기 중점 · 친구 | `img_scene_child_friend_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_scene_child_friend_001.txt) | [PNG](samples/week7/individual/v1/img_scene_child_friend_001.png) |
| 청소년기 학업 | 3인칭 | 청소년기 중점 · 학업 | `img_scene_teen_study_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_scene_teen_study_001.txt) | [PNG](samples/week7/individual/v1/img_scene_teen_study_001.png) |
| 청소년기 아르바이트 | 3인칭 | 청소년기 중점 · 아르바이트 | `img_scene_teen_part_time_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_scene_teen_part_time_001.txt) | [PNG](samples/week7/individual/v1/img_scene_teen_part_time_001.png) |
| 청년기 도전 | 3인칭 | 청년기 중점 · 도전 | `img_scene_young_adult_adventure_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_scene_young_adult_adventure_001.txt) | [PNG](samples/week7/individual/v1/img_scene_young_adult_adventure_001.png) |
| 중년기 취미 | 3인칭 | 중년기 중점 · 취미 | `img_scene_middle_hobby_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_scene_middle_hobby_001.txt) | [PNG](samples/week7/individual/v1/img_scene_middle_hobby_001.png) |
| 시험 | 1인칭 | `teen_042` 기말고사 1교시 | `img_pov_exam_paper_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_pov_exam_paper_001.txt) | [PNG](samples/week7/individual/v1/img_pov_exam_paper_001.png) |
| 몸살 | 1인칭 | `adult_068` 온몸이 두들겨 맞은 아침 | `img_pov_flu_morning_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_pov_flu_morning_001.txt) | [PNG](samples/week7/individual/v1/img_pov_flu_morning_001.png) |
| 회사 출입문 | 1인칭 | `adult_070` 열리지 않는 출입문 | `img_pov_layoff_door_001` / 동일 ID의 `.png` | [TXT](prompts/week7/img_pov_layoff_door_001.txt) | [PNG](samples/week7/individual/v1/img_pov_layoff_door_001.png) |

## 4. 담당별로 받을 내용과 후속 확인

역할은 과제와 기존 가이드에 근거한다. 아래 확인 순서는 인계를 위한 권장 절차다.

| 담당 | 받을 자료 | 후속 확인·업무 경계 |
| --- | --- | --- |
| 정원률 | 스타일·레이아웃·프롬프트·예시·본 인계 문서 | 작성·제작·사용자 육안 검수·인계 자료 정리 완료. 이후 내용 변경으로 예시 보완이 필요할 때 기준을 제공. 300×225 처리는 현재 보류 |
| 신기훈 | 3절 장면표와 TXT 8개, 기준 요청서 | 사건·중점의 의도, 시점·설명·금지 요소·imageId가 이후 시나리오 변경과 일치하는지 확인. 요청 변경은 imageId와 설명을 함께 전달 |
| 김선한 | 개별 PNG 8장, 상태 JSON, 원본 요청서 | 최종 imageId/fileName·등록 데이터·PNG·4:3·해상도 검증 담당. 현재 1448×1086 예시를 300×225 통과 파일로 판정하지 않음. 최종 파일 준비 후 규격 검증 |
| 김재겸 | 네 화면 가이드·디자인 수치·검수 코드 | 실제 화면 구현·이동·저장/이어하기·이미지 연결 담당. 중복 기능은 PR #28 기준으로 유지하고 전달받은 디자인 기준을 적용 |
| 김우영 | 화면에 필요한 연차·시기·중점·능력치·공개 가능성의 기준 | 실제 시기 진행·중점 효과·결정/돌발 판정·확률·저장 담당. 실제 규칙·상태를 공급. 정원률의 작업 범위는 디자인·이미지 검수 |

300×225 처리를 재개할 때 제작 담당·시점은 팀에서 정한다. 이 문서는 사용자 보류 결정을 해제하거나 다른 팀원에게 즉시 축소 작업을 지시하는 문서가 아니다.

## 5. 프론트에 넘길 디자인 기준

공통 수치와 Tokens/Color/Type/Theme 구조는 유지한다. 전체 값과 현재 적용 범위는 [수치 관리 안내](week7_ui_tuning.md)를 따른다.

- 바깥 여백 12dp, 영역 간격 16dp, 카드 내부 14dp, 카드 사이 10dp, 모서리 8dp.
- 주요 버튼 최소 56dp, 능력치 버튼 최소 48dp.
- 제목 24sp/32sp, 이야기·선택·결과 본문 16sp/26sp. 긴 문구는 줄바꿈·스크롤로 접근.
- 시기·연차·초점·능력치 공유 기능은 김재겸 PR 구현을 기준으로 한다. 문구 `N번째 해`, `이번 시기 초점`, `N해 고정`, 결과 뒤 `계속하기`/`인생 정리하기`를 따른다.
- 필름 그림 영역 4:3, 위·아래 띠 14dp, 좌우 테두리 10dp. 원본 전체 표시·보간/디더링 해제 기준 유지.

김재겸 PR #28에는 실제 장 표지·자동 진행·결정/돌발·능력치 공유 구현이 있다. 정원률의 독립 기능 구현·상태 필드·타이머·관련 테스트는 제거/철회했다. PR의 실제 화면에 디자인 수치·색상·필름 표시를 적용하는 것이 후속 협업 범위다. 자세한 처리와 담당별 경계는 [프론트 업무 경계 정리](week7_frontend_alignment.md)에 있다.

아래는 현재 로컬의 디자인 정의·이미지 검수 코드다. 실제 v4 화면 전체에 적용됐다는 뜻은 아니다. 필름의 4:3은 검수 화면이 높이를 계산하여 적용하고, FilmStrip은 호출자가 전달한 높이를 사용한다.

현재 유지하는 코드:

| 구성 | 코드 | 용도 |
| --- | --- | --- |
| 공통 수치 | [Week7DesignTokens.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Week7DesignTokens.kt) | 화면 모양의 확정 기준·후속 적용 값 |
| 색상·글자 | [Color.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Color.kt), [Type.kt](../../app/src/main/java/com/example/lifegame/ui/theme/Type.kt) | 공통 시각 스타일 |
| 필름 그림 | [FilmStrip.kt](../../app/src/main/java/com/example/lifegame/ui/components/FilmStrip.kt) | 전체 장면 표시·확정 필름 수치 |
| 검수 진입 | [DesignPreviewScreen.kt](../../app/src/main/java/com/example/lifegame/ui/screens/DesignPreviewScreen.kt) | 이미지 보기·초기화·복귀만 제공 |
| 이미지 검수 | [ImageSamplePreviewScreen.kt](../../app/src/main/java/com/example/lifegame/ui/screens/ImageSamplePreviewScreen.kt) | 이미지 8장 전환·선택·정지 상태 표시 |
| 검수 목록 | [Week7ImageSamples.kt](../../app/src/main/java/com/example/lifegame/ui/preview/Week7ImageSamples.kt) | 장면별 제목·시점·debug 이미지 경로 |

## 6. 직접 실행해 보는 방법

1. 저장소 루트 [run-preview.cmd](../../run-preview.cmd) 또는 Android Studio의 app/debug를 실행한다.
2. 첫 화면 **디자인 미리보기**를 누르면 **예시 이미지 검수**로 바로 들어간다.
3. **이전 이미지 / 다음 이미지 / 장면 고르기**로 8장을 확인한다.
4. **다시 보기**로 첫 장, **돌아가기**로 첫 화면에 복귀한다.

예전 `화면 고르기`와 장 표지·자동 진행·사건 미리보기는 제거했다. 이미지 원본 사본은 `app/src/debug/assets/images/design_preview/week7/`이며 실제 게임 매핑·저장과 분리한다. 김재겸 PR의 v4 흐름은 이 브랜치에 가져오지 않았고, 로컬의 기존 게임은 HEAD 기반 흐름이다.

## 7. 검증과 남은 협업

| 항목 | 현재 상태 | 후속 담당 |
| --- | --- | --- |
| 필수 가이드 3개·네 화면 모양 기준 | 작성 완료·유지 | 정원률이 기준 제공, 김재겸이 실제 화면에 적용 |
| TXT 8개·PNG 8장·생성 기록 | 유지, 파일 변경 없음 | 신기훈 요청 대응·김선한 최종 규격 확인 |
| 이미지 육안 검수 | 사용자 2026-10-13 완료 보고 유지 | 변경된 검수 진입과 큰 글씨 표시는 사용자 확인 |
| 중복 기능 구현 정리 | 제거/철회 완료, 김재겸 PR 기준으로 담당 분리 | 김재겸·김우영 |
| 300×225 처리 | 사용자 요청으로 보류 | 재개 시 담당·일정 합의 |
| 실제 고유 색상 수 40개 이하 | 계측 미수행, 원문은 40색 이하 느낌 | 별도 권장 제작 목표 |
| 최종 imageId/fileName·규격·실제 이미지 연결 | 대기 | 김선한·김재겸 |
| 디자인 차이 적용 | 색상·여백·글자·필름·전체 이미지 표시 협의 필요 | 정원률·김재겸 |

정리 전의 장 표지·자동 진행·결정 흐름 검사 기록은 과거 버전의 이력이다. 현재 코드의 통과 근거로 사용하지 않는다. 기존 이미지 큰 글씨 검사에서 제목 overflow를 수정했지만 해당 자동 검사를 다시 통과했다고 기록하지 않는다.

2026-10-13 정리 후 검증:

- `assembleDebug`, `assembleDebugAndroidTest`, `compileReleaseKotlin` 통과.
- 단위 테스트 Debug 51개 + Release 51개, 실패·오류 0개.
- debug APK에 원본과 바이트 단위로 동일한 예시 이미지 8장 포함 확인.
- 중복 기능 파일 9개 제거, 기존 기능 파일 7개 원본 복원, 제거 파일의 Kotlin 참조 없음 및 문서 링크 확인.
- PNG·TXT·실제 생성 입력 기록 27개 원본 변경 없음. 백엔드·실제 게임 assets·ViewModel 변경 없음.
- 브랜치·HEAD 유지, 스테이징 변경 없음. commit·push·PR·merge 없음.

사용자 요청에 따라 에뮬레이터 실행·자동 화면 검사는 하지 않았다. 직접 진입·전환·초기화·복귀와 큰 글씨 표시는 사용자 확인 대상이다.

## 8. 인계 준비 상태

- [x] 디자인·프롬프트·예시·육안 검수·보류 사유 문서 유지.
- [x] 김재겸 PR과 중복 기능 구현 정리 및 역할 경계 기록.
- [x] 이미지 검수 전용 진입과 현재 코드 참조·실행 안내 정리.
- [ ] 팀원에게 실제 전달·접근 확인 — 이 작업에서는 발송하지 않음.
- [ ] 실제 PR 화면에 디자인 차이 적용·육안 검수 — 김재겸이 적용, 정원률이 모양 확인.
- [ ] 300×225 처리·최종 규격 검증·실제 이미지 연결 — 보류·후속 작업.

2026-10-13 MD 설명 정리: 확정 디자인·로컬 적용·프론트 PR 구현·과거 검사 이력을 구분했다. 문서 링크와 Tokens 직접 참조를 확인하며, 이번 설명 수정에서는 앱·이미지·프롬프트 변경이나 추가 빌드·에뮬레이터 검사를 하지 않는다.
