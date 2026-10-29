# 건강 계산 단일 규칙

건강 변화는 엔진의 `GameEngine.resolveOutcomeStatDelta()` 한 곳에서 계산한다.

```text
최종 건강 변화 = statDelta.health + lifeDelta + 시기별 연간 변화
```

| 시기 | 연간 변화 |
|---|---:|
| 영아기 | 0 |
| 유년기 | 0 |
| 청소년기 | 0 |
| 청년기 | -5 |
| 중년기 | -5 |
| 노년기 | -5 |

예: 청년기 결과가 `statDelta.health=-10`, `lifeDelta=+2`라면 최종 변화는 `-13`이다. `BIG_INCIDENT`라는 종류만으로 추가 피해를 넣지 않는다. 콘텐츠 작성자가 피해를 원하면 해당 outcome에 `statDelta.health` 또는 `lifeDelta`를 명시한다.

프론트엔드는 건강 변화량을 별도로 계산하지 않고 엔진 적용 전후의 `Stats` 차이를 표시한다.
