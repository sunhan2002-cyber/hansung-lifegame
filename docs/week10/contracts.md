# 10주차 통합 계약

이 문서는 프론트엔드·콘텐츠·게임 로직이 같은 규칙으로 연결되기 위한 최종 계약이다.

## 선택 결과와 플래그

- `Outcome.addFlags`: 결과 적용 후 추가할 플래그
- `Outcome.removeFlags`: 결과 적용 시 제거할 플래그
- 적용 순서는 **removeFlags → addFlags → 완료 사건 플래그**다.
- 같은 플래그가 `removeFlags`와 `addFlags`에 동시에 있으면 추가가 이긴다.
- 현재 상태형 인생 사실(학력·직업·고용·혼인·주거)은 연결 플래그 제거 시 같은 값일 때만 비운다.
- 자녀 수와 이미 달성한 업적은 상태 플래그 제거만으로 과거 기록을 되돌리지 않는다.

콘텐츠 예시:

```json
{
  "outcomeId": "divorce",
  "text": "각자의 길을 걷기로 했다.",
  "chance": 100,
  "removeFlags": ["married"],
  "addFlags": ["divorced"]
}
```

## 능력치 조건

- 사건과 선택지는 `minStats`/`stats`/`requiredStats`와 `maxStats`를 사용할 수 있다.
- 최소·최대 조건은 모두 충족해야 한다.
- 존재하지 않는 스탯 이름은 값으로 반영하지 않는다. 검증 스크립트에서 별도로 오류 처리한다.

## 건강 계산

- 결과 건강 변화 = `statDelta.health + lifeDelta + annualHealthDelta(period)`
- `annualHealthDelta`는 영아기·유년기·청소년기 `0`, 청년기·중년기·노년기 `-5`다.
- 세 항목은 각각 한 번만 더한다.
- 명시적 `outcomes`가 있는 `BIG_INCIDENT`에 숨은 기본 피해를 넣지 않는다.
- 건강이 0이 되면 해당 시기를 `deathPeriod`, 원인을 `deathCause`에 즉시 기록한다.
- 영아기·유년기에는 건강 하한 1을 유지한다.

## 칭호

- `titles.sample.json`을 `ContentRepository.loadTitles()`로 읽는다.
- 조건이 맞는 칭호 중 `priority` 내림차순, 동률이면 `titleId` 오름차순으로 하나를 고른다.
- 기본 칭호는 정확히 하나여야 하며 조건 칭호가 없을 때 사용한다.
- 완료 기록에는 `titleId`뿐 아니라 당시 `text`, `description`도 저장한다.

## 저장·복원

- 현재 저장 포맷은 v3다.
- 현재 인생에는 `PeriodRunState`, 전체 선택 기록, 읽는 중인 결과, 아바타 설정, 결정된 칭호를 저장한다.
- 결과를 적용한 직후 저장하므로 앱을 종료해도 같은 결과가 다시 추첨되거나 중복 적용되지 않는다.
- 완료 인생은 `runId`로 upsert한다. 종료 버튼을 다시 눌러도 중복 저장되지 않는다.
- 완료 인생은 현재 인생과 별도 키에 저장되며 새 게임을 시작해도 지워지지 않는다.
- v1/v2 저장은 누락된 새 필드를 기본값으로 채워 읽는다. 손상·미지원 미래 버전은 `V4LoadResult.Invalid`로 구분해 안내 후 현재 슬롯만 비운다.

## 아바타·생성 기능

- 체형은 운동능력 70 이상이면 `MUSCULAR`가 최우선이다.
- 그 외 건강 35 미만 또는 운동능력 30 미만이면 `SLIM`, 나머지는 `STANDARD`다.
- `GenerationClient`는 얼굴 시트, 미래 모습, 회고 생성 계약을 제공한다.
- 이미지 생성 제한 30초, 텍스트 생성 제한 3초를 사용한다.
- 실패·시간 초과·취소는 플레이 상태를 변경하지 않고 로컬 기본 자산/문구로 대체한다.
- 사진 동의가 없으면 얼굴 시트 생성을 호출하지 않는다.

## 프론트엔드 연결 지점

- 홈의 지난 인생: `V4ProgressStore.loadCompletedLives()`
- 이어하기: `V4ProgressStore.load()`의 `choices`, `pendingOutcome`, `avatar`, `awardedTitle`
- 결과 화면: `SavedOutcome`을 그대로 표시하고 재계산·재추첨하지 않는다.
- 결말 화면: `CompletedLifeRecord`의 결말, 칭호, 선택 기록, 아바타를 사용한다.
- 주마등: `recapMomentsFrom()`으로 실제 선택 기록 중 이미지가 있는 장면만 시간순·중복 제거해 사용한다.
