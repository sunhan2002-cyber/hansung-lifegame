# 최종 UI 가이드 — 자연스러운 성장과 독립 외형 v4

2026-10-29. 승인한 중간 밀도 픽셀 표현에서 얼굴을 약간 덜 둥글게 조정했다. 몸·피부·눈·머리는 분리 PNG로 조합한다. 이전의 사실적인 긴 얼굴, 굵은 2등신 표현과 통합 머리 교체 실험은 최종 APK에서 제외했다.

## 디자인과 실제 앱 대조

|기준|실제 확인|
|---|---|
|자연스러운 인물|얼굴·외형 anchor 유지, 18개 독립 몸, 시기별 키·옷·자세·노년 백발|
|독립 외형|피부/머리/눈을 하나씩 바꿔 다른 선택을 유지하는 UI 검사|
|성장·체형|기존 bodyTypeFor 사용, 새 네 레이어를 함께 표시해 시기 혼합 방지|
|상단 캐릭터·건강|투명 캐릭터와 14dp 건강바, 숫자 없음, 80/60 색 경계, 초점 박스 없음|
|360dp·큰 글꼴|360×740dp·글꼴 1.3배, 긴 본문/선택/결과는 선명하고 필요하면 스크롤|
|시기선·카드|하단 -O-O-O- 시기선, 어두운 반투명 카드·ABC 왼쪽 위 통합 모서리·우측 화살표|
|밝은 배경|운동장 배경에서 제목·본문·카드·건강 확인, 배경만 blur|
|장면 보기|실제 그림 Fit·blur 해제·모든 UI 숨김, 한 번 터치로 같은 결과 복귀|
|주마등|실제 선택 기록의 시기 순서, 1인칭 사건에 인물을 억지로 겹치지 않음|
|미래·기록증|새 캐릭터·종료 그림·칭호·실제 인생 사실과 미기록 안내|

## 화면과 상태

|화면|기본·긴 문구|로딩·실패|
|---|---|---|
|외형 선택|세 시기 미리보기·피부/머리/눈 독립 변경|사진 동의 거부 후 기본 모습|
|사건/선택/결과|밝은 배경·긴 문구·잠금 사유·ABC 모서리|이미지 누락 시 기존 대체 경로|
|성장|6시기×3체형·같은 발 위치|새 레이어 준비 전 이전 완성 조합 유지|
|장면 보기|그림만 남는 화면|한 번 터치로 같은 결과 복귀|
|주마등|실제로 기록된 장면·건너뛰기·다시 보기|사진/서버 없이도 진행|
|미래 모습|마지막 모습·기기 안 그림|Fake 지연→취소 안내·대체|
|기록증|긴 칭호·교육/직업/가족/업적/선택 스크롤|모르는 사실은 기록 없음|
|홈/칭호|지난 인생 열기·저장된 칭호 목록|재실행 후 보존|

#43의 화면 구조를 유지하고 이미지 연결·로딩에 필요한 범위만 수정했다. 엔진과 저장 모델은 수정하지 않았다.

## 실제 앱 화면 증거

아래는 최종 APK를 에뮬레이터에서 렌더링한 캡처다. 360dp·큰 글꼴 화면은 테스트 데이터로 실제 UI를 구성한 것이다.

- [청년 세 체형](../../output/week10/avatar_v4/evidence/pilot_three_bodies.png)
- [여섯 시기·세 체형](../../output/week10/avatar_v4/evidence/all_18_bodies.png)
- [독립 외형 선택](../../output/week10/avatar_v4/evidence/week10-frontend/w10_independent_appearance.png)
- [밝은 배경](../../output/week10/avatar_v4/evidence/week10-frontend/w10_bright_background.png)
- [긴 사건과 ABC 카드](../../output/week10/avatar_v4/evidence/week10-frontend/w10_decision_360dp.png)
- [장면 보기](../../output/week10/avatar_v4/evidence/week10-frontend/w10_scene_only.png)
- [주마등](../../output/week10/avatar_v4/evidence/week10-frontend/w10_recap.png)
- [미래 로딩](../../output/week10/avatar_v4/evidence/week10-frontend/w10_future_loading.png)
- [미래 대체](../../output/week10/avatar_v4/evidence/week10-frontend/w10_future_fallback.png)
- [기록증](../../output/week10/avatar_v4/evidence/week10-frontend/w10_certificate_top.png)
- [홈](../../output/week10/avatar_v4/evidence/week10-frontend/w10_home_records.png)
- [칭호](../../output/week10/avatar_v4/evidence/week10-frontend/w10_titles.png)

아래는 앱 캡처와 구분하는 PNG 합성 검수 자료다.

- [분리된 네 파트와 조합](../../output/week10/avatar_v4/independent_parts.png)
- [18개 소재 비교](../../output/week10/avatar_v4/all_18_packaged.png)
- [청년 36외형](../../output/week10/avatar_v4/options_36_young_adult.png)
- [영아 36외형](../../output/week10/avatar_v4/options_36_infant.png)
- [노년 36외형](../../output/week10/avatar_v4/options_36_old_age.png)
- [장면 24장](../../output/week10/scene_contact_sheet.png)

일반 배경은 인물을 둘 바닥과 여백을 확보했다. 경로 사건 14개는 1인칭을 우선하며 건강/운동·행복/운을 구분한다. 유산 분배는 변호사 사무실, 시험과 취미는 교재·그림·메시지가 함께 있는 책상으로 교체했다.

실물 휴대전화와 실제 사진 생성 서버는 검증하지 않았다. 주마등의 과거 체형은 시기별 스탯 이력이 없어 마지막 체형을 사용한다. 자동 검사 통과와 사용자의 최종 미적 평가는 구분한다.

