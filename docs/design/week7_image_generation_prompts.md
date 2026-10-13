# Week7 단일 장면 생성용 독립 프롬프트 8개

- 정리일: 2026-10-13
- 원본 요청: 로컬 `origin/feat/week7-scenario-kihoon`의 `docs/scenario/week7_pixel_scene_requests.md`, 커밋 `f4800f0ec3af1f09b51ec2b263e1ef60dcf60ca2`.
- 기준: [도트 스타일 가이드](week7_pixel_art_style_guide.md).
- 상태: **TXT 1개 → 단일 장면 이미지 1장**. 독립 TXT 8개와 개별 예시 PNG 8장을 준비했고, 2026-10-13 사용자가 임시 예시 보기에서 8장 육안 검수를 완료했다고 보고했다. 원본은 각각 1448×1086, 4:3이며 300×225 축소는 보류한다. 최종 규격·매핑 검증과 실제 게임 연결은 별도다.
- 인계 자료: [Week7 팀원 인계](week7_handoff.md) — 산출물·담당별 확인 사항·보류 항목 정리 완료.

## 1. 사용할 파일

**지금 사용할 프롬프트는 아래 표의 TXT 8개다. 한 번의 생성 요청에는 필요한 TXT 하나의 본문 전체만 사용한다.**
8개 TXT를 한 번에 붙여 넣거나 이전 모음용 JSON의 normalInitial/pov를 사용하는 방식이 아니다.
추가 공통 프롬프트·다른 TXT·참조 이미지 없이 각 파일 하나만으로 해당 장면을 요청할 수 있다.

### TXT와 JSON의 용도

TXT는 이미지 생성 입력창에 복사하기 위한 일반 텍스트다. 이번 수동 작업에는 TXT만으로 충분하다.
JSON은 여러 프롬프트와 출처를 프로그램에서 읽거나 생성 이력을 보관할 때 유용하며 필수 형식은 아니다.
앱의 이미지 ID 매핑 JSON과 생성 프롬프트 JSON은 서로 다른 자료다.

**과거 생성 이력:** [samples/week7/prompts.json](samples/week7/prompts.json)은 예전에 만든 모음 이미지의 기록이다. 단일 장면 생성에 사용하지 않는다. 내부의 생성 지시문은 보관용으로 유지한다.
그 파일의 normalInitial/pov는 모음용이고 normalLayoutRefinement는 원본 이미지를 첨부하는 편집용이다.
현재 TXT 8개는 각각 한 장면만 서술하고, 단일 4:3 화면·한 카메라 시점·한 순간을 명시한다. 파일명과 내용은 표의 장면에 대응한다.
공통 스타일·장면 설명·공통/장면별 금지 조건이 각 파일에 모두 들어 있으므로 다른 파일을 추가로 합칠 필요가 없다.
imageId·한글 제목·연결 정보는 아래 표와 파일명에만 보관하며 프롬프트 안에 이미지 제목처럼 넣지 않는다.

## 2. 파일 목록

| 순서 | 장면 | 연결 | imageId | 복사용 TXT |
| ---: | --- | --- | --- | --- |
| 1 | 유년기 친구 | 초점 유년기 · 친구 | `img_scene_child_friend_001` | [img_scene_child_friend_001.txt](prompts/week7/img_scene_child_friend_001.txt) |
| 2 | 청소년기 학업 | 초점 청소년기 · 학업 | `img_scene_teen_study_001` | [img_scene_teen_study_001.txt](prompts/week7/img_scene_teen_study_001.txt) |
| 3 | 청소년기 아르바이트 | 초점 청소년기 · 아르바이트 | `img_scene_teen_part_time_001` | [img_scene_teen_part_time_001.txt](prompts/week7/img_scene_teen_part_time_001.txt) |
| 4 | 청년기 도전 | 초점 청년기 · 도전 | `img_scene_young_adult_adventure_001` | [img_scene_young_adult_adventure_001.txt](prompts/week7/img_scene_young_adult_adventure_001.txt) |
| 5 | 중년기 취미 | 초점 중년기 · 취미 | `img_scene_middle_hobby_001` | [img_scene_middle_hobby_001.txt](prompts/week7/img_scene_middle_hobby_001.txt) |
| 6 | 1인칭 시험 | teen_042 기말고사 1교시 | `img_pov_exam_paper_001` | [img_pov_exam_paper_001.txt](prompts/week7/img_pov_exam_paper_001.txt) |
| 7 | 1인칭 몸살 | adult_068 온몸이 두들겨 맞은 아침 | `img_pov_flu_morning_001` | [img_pov_flu_morning_001.txt](prompts/week7/img_pov_flu_morning_001.txt) |
| 8 | 1인칭 회사 출입문 | adult_070 열리지 않는 출입문 | `img_pov_layoff_door_001` | [img_pov_layoff_door_001.txt](prompts/week7/img_pov_layoff_door_001.txt) |

## 3. 사용 순서

1. 생성할 장면 하나를 골라 해당 TXT 하나를 연다. 본문 전체를 복사한다.
2. 이미지 생성 입력창에 그 TXT 하나만 붙여 넣는다. 파일 제목·imageId·다른 장면 설명을 추가하지 않는다. 한 장면 이미지 한 장을 생성한다.
3. 생성 도구가 크기·비율 설정을 제공하는 경우 지원 범위에서 4:3을 선택한다. 이번에 사용한 도구에는 출력 크기를 직접 고정하는 설정이 없었으므로 프롬프트로 4:3 구도를 요청하고 실제 결과를 검사했다. 크기 문구만으로 파일 해상도가 보장되지는 않는다. 최종 앱 규격은 300×225이며 현재 예시 원본의 축소는 사용자 요청으로 보류한다.
4. 참조 이미지 없이 사용할 수 있다. 여러 장면을 만들 때는 다음 TXT로 별도의 생성 요청을 한다.
5. 결과의 내용·시점·금지 요소·도트 표현을 확인한다. 문제가 있는 부분만 구체적으로 수정 요청한다.
6. 최종 적용 때는 개별 이미지를 300×225 PNG로 처리하고 김선한의 imageId/fileName·규격 검증 후 프론트에서 연결한다. 현재 8장은 육안 검수를 마친 예시 원본이며 이 단계까지 승인된 게임 자산은 아니다. 검토용 TXT 자체를 앱 이미지 폴더에 넣는 것은 아니다.

## 4. 수정이 필요한가

기존 지시로 평소 5장면·POV 3장면의 모음은 생성됐다. 평소 모음의 아래 두 컷이 넓게 나온 점은 배치 보완으로 수정했다.
현재 TXT는 단일 장면 전용으로 다시 정리했다. 원본 요청의 장면 내용·금지 요소는 유지하면서 각 파일 첫 문장과 출력·시점 조건을 단독 생성 기준으로 명확히 했다.

프롬프트의 규격 문구는 실제 파일 검사 대신 사용할 수 없다. 생성 도구는 지정한 크기·색 수·금지 요소를 항상 정확히 지키지는 않는다.
과거 모음의 전체 크기는 1774×887 / 1448×1086이고, 현재 단일 장면 원본 8장은 각각 1448×1086, 4:3이다. 모두 개별 300×225 최종 자산은 아니다. 현재 예시 8장의 육안 검수는 사용자 완료 보고를 반영했다. 실제 고유 색상 수 40개 이하 계측은 별도이며 수행하지 않았다.
문제없는 프롬프트까지 일괄 수정하지 않고, 결과에 나타난 문제와 연결되는 조건만 보완한다.

## 5. 결과 검수와 역할

- 정원률: 요청 장면·시점·빈티지 톤·도트 표현·금지 요소·중요 부분 잘림 여부 확인 및 프롬프트 보완.
- 신기훈: 사건/중점 요구, 시나리오 설명, imageId와 요청 변경 확인.
- 김선한: 승인된 개별 PNG의 300×225·4:3·파일명·매핑 검증.
- 프론트: 승인된 자산을 sceneImageId/povImageId와 필름 영역에 연결.

2026-10-13 현재 상태: TXT 8개로 개별 예시 이미지 8장을 생성하고 임시 검수 화면에 연결했다. 사용자가 8장 육안 검수 완료를 보고했다. [결과·실제 크기·사용 프롬프트·검수 상태](samples/week7/individual/v1/README.md)를 확인한다.

현재 debug 이미지 검수 경로는 **첫 화면 → 디자인 미리보기 → 예시 이미지 검수**다. 장 표지·자동 진행·결정 미리보기는 제거했으며 게임 동작 구현은 프론트·백엔드 담당이다. 원본 복사본은 `app/src/debug/assets/images/design_preview/week7/`에만 넣었고 정식 사건·초점의 이미지 매핑은 변경하지 않았다. 300×225 축소·최종 규격 검증은 보류·후속 작업으로 유지한다. commit·push·PR 없음.
