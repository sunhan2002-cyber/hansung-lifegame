# Week7 개별 예시 이미지 8장 — 사용자 육안 검수 완료 v1

> 현재 실행 경로(2026-10-13 정리 후): 첫 화면 **디자인 미리보기**에서 8장 검수 화면으로 바로 들어간다. 이전 화면 고르기·게임 흐름 미리보기는 제거했다. [업무 경계 정리](../../../../week7_frontend_alignment.md).

- 생성일: 2026-10-13
- 도구: 기본 제공 image_gen. 저장한 독립 TXT 8개를 수정 없이 각각 별도의 생성 호출에 사용했다.
- 참조 이미지: 없음. 각 결과는 한 장면·한 시점의 단일 이미지다.
- 구성: 3인칭 평소 5장 + 1인칭 결정/돌발 3장.
- 육안 검수: **완료 (2026-10-13)** — 사용자가 임시 예시 보기에서 8장 직접 확인 후 완료했다고 보고.
- 최종 앱 규격: **300×225 축소 보류 / 파일명·매핑·규격 최종 검증 대기**.

| 순서 | 장면 | 원본 후보 PNG | 실제 해상도 | 사용 프롬프트 |
| ---: | --- | --- | --- | --- |
| 1 | 유년기 친구 | [img_scene_child_friend_001.png](img_scene_child_friend_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_scene_child_friend_001.txt) |
| 2 | 청소년기 학업 | [img_scene_teen_study_001.png](img_scene_teen_study_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_scene_teen_study_001.txt) |
| 3 | 청소년기 아르바이트 | [img_scene_teen_part_time_001.png](img_scene_teen_part_time_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_scene_teen_part_time_001.txt) |
| 4 | 청년기 도전 | [img_scene_young_adult_adventure_001.png](img_scene_young_adult_adventure_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_scene_young_adult_adventure_001.txt) |
| 5 | 중년기 취미 | [img_scene_middle_hobby_001.png](img_scene_middle_hobby_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_scene_middle_hobby_001.txt) |
| 6 | 1인칭 시험 | [img_pov_exam_paper_001.png](img_pov_exam_paper_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_pov_exam_paper_001.txt) |
| 7 | 1인칭 몸살 | [img_pov_flu_morning_001.png](img_pov_flu_morning_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_pov_flu_morning_001.txt) |
| 8 | 1인칭 회사 출입문 | [img_pov_layoff_door_001.png](img_pov_layoff_door_001.png) | 1448×1086 | [TXT](../../../../prompts/week7/img_pov_layoff_door_001.txt) |

## 확인한 내용과 남은 검수

8개 파일이 정상적으로 읽히는 PNG이고 모두 1448×1086, 4:3임을 확인했다. 2026-10-13 사용자는 당시 경로인 **디자인 미리보기 → 화면 고르기 → 예시 이미지 8장 보기**에서 8장 육안 검수를 완료했다고 보고했다. 이는 정리 전의 검사 이력이며, 현재 경로는 위 안내처럼 디자인 미리보기에서 직접 진입한다. 이 보고를 전체 예시 8장의 검수 완료 상태에 반영했다.

생성 원본과 프롬프트는 그대로 보관한다. **300×225 파일은 아니며 축소는 사용자 요청으로 보류**한다. 재표본화·팔레트 정리 등 후처리는 수행하지 않았다.
육안 검수 완료는 임시 화면의 예시 확인 기록이다. 실제 고유 색상 수 40개 이하를 계측하거나, 300×225·파일명·매핑의 최종 검증을 통과한 게임 자산으로 승인한 것은 아니다. 최종 규격 검증은 김선한 담당, 실제 게임 연결 후 표시는 프론트와 후속 확인한다.

[실제 사용한 전체 프롬프트](prompts_used.json), [원본 크기·출처·프롬프트 해시](generation_metadata.json)를 함께 보관한다.
imageId별 파일명으로 예시를 정리하고 원본 복사본을 `app/src/debug/assets/images/design_preview/week7/`에 넣었다. 임시 검수 화면에만 연결했으며 `app/src/main/assets/images/`나 실제 사건·초점의 이미지 매핑에는 연결하지 않았다.
기존 모음 후보와 프롬프트 파일은 유지한다. commit·push·PR 없음.

## 팀원 인계 자료

[Week7 팀원 인계](../../../../week7_handoff.md)에 8장별 대응표, 담당별 확인 사항, 현재 검수 코드·실행 안내·프론트 인계와 보류 항목을 정리했다. 문서 준비 완료이며 실제 팀원 전달·원격 공유는 미실시다.
