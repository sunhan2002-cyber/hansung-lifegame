# 김우영 본 제작 배치 01

- 제출 대상: `adult_012`, `adult_017`, `adult_022`, `adult_027`
- 재구도 검토 후보: `adult_007` — 2026-11-09 새 파일에서 세 빈자리 전체가 중앙 x=34~66% 계산 크롭에 남는 것을 확인했다.
- 이전 `adult_007` 후보의 반려 기록은 manifest `rejectedDrafts`에 그대로 보존했다. 새 후보도 팀 의미 검수·실기기 크롭·라우팅 승인 전에는 승인 처리하지 않는다.
- 규격: 300×225 PNG, 4:3, FIRST_PERSON
- 생성: OpenAI built-in image generation
- 검수 단계: 원본 의미·필수 사물·중앙 x=34~66% 계산 크롭 자체검수
- 실제 앱 크롭: Windows WHPX 비활성으로 Pixel_7 AVD가 부팅되지 않아 팀/가속 활성 환경에서 추가 확인 필요

이 배치는 사건 내용과 선택 결과, 기존 사건 ID와 imageId를 변경하지 않는다. 중앙 `image_ids.sample.json`과 `image_routing.json`도 수정하지 않는다.

## adult_007 재구도 후보

- 파일: `app/src/main/assets/images/followup_20261031/img_adult_friends_drinks_001.png`
- SHA-256: `7c242b11d4a02a565c6bf0a0b359806a1afa5f3a24c56bce76c28bdea0f78d9e`
- 규격: 300×225 PNG, 4:3
- 계산 크롭: x=102..197(원본 300px의 중앙 34~66%)에서 앞쪽 빈 의자 1개와 건너편 빈 의자 2개가 모두 분리되어 남는다.
- 상태: `SELF_REVIEW_PASS_PENDING_TEAM_REVIEW`; 실제 앱 크롭과 팀 의미 검수는 미완료다.
