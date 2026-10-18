#!/usr/bin/env python3
"""Apply the reviewed disposition for every choice in events.sample.json.

This is intentionally conservative: a choice is gated only when its outcome
has one clear stat or its text/flags identify one clear ability. Narrative,
reaction, and special-survival choices remain explicitly always available.
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
EVENT_FILE = ROOT / "app" / "src" / "main" / "assets" / "events.sample.json"
REPORT_FILE = ROOT / "docs" / "qa" / "week8_choice_condition_resolution.md"

STATS = ("건강", "운동능력", "지력", "사회성", "경제력", "행복", "운")
THRESHOLDS = {"INFANT": 50, "CHILD": 52, "TEEN": 55, "YOUNG_ADULT": 58, "MIDDLE": 60, "OLD_AGE": 65}
STAGE_PERIODS = {
    "INFANT": {"INFANT"}, "CHILD": {"CHILD"}, "TEEN": {"TEEN"},
    "ADULT": {"YOUNG_ADULT", "MIDDLE"},
}

# These are semantic signals, not a replacement for the outcome stat. Flags
# carry more weight because they are the content author's intended path tag.
FLAG_SIGNALS = {
    "건강": "health healthy recovered burnout medicine sick routine sleep",
    "운동능력": "sports athlete soccer basketball swim marathon gym coach training military taekwondo run fitness",
    "지력": "study exam school book science research university college debate reporter writer coding program test class focus champion rocket baduk hackathon sharp_eye",
    "사회성": "friend friendship family love married parent child team leader choir band volunteer wedding colleague reunion mentor social customer",
    "경제력": "money wealth job career startup shop home house promoted laid_off salary retirement business investor vocational pro_athlete customer",
    "행복": "arts music piano art hobby travel dream fan party play free_play bucket content rest family_bond",
    "운": "gacha luck random chance survived opportunity lottery risk twist accident secret",
}
KOREAN_SIGNALS = {
    "건강": "건강 병 약 휴식 회복 몸 감기 아프 잠 수면 사우나 걷기 쉬자 마라톤",
    "운동능력": "축구 농구 수영 달리 뛰 운동 경기 훈련 선수 체육 태권도 마라톤 입대 질주",
    "지력": "도서 공부 시험 대학 전공 연구 문제 자료 책 읽 과학 토론 설명 계획 정리 조사 앱 테스트 문과 숙제",
    "사회성": "친구 가족 엄마 아빠 결혼 연애 사랑 말 설득 대표 회장 팀 동료 선생 사람 전화 같이 도와 선물 편지 자녀 아이 대화",
    "경제력": "돈 회사 취업 직업 승진 창업 가게 집 주거 사고 팔 수입 월급 투자 대출 퇴사 지원 공모 위로금 파견",
    "행복": "놀 취미 피아노 미술 노래 음악 여행 파티 웃 즐 휴가 꿈 소원 사진 케이크 고기 게임 팬 공연 예술",
    "운": "뽑기 우연 기회 모험 찍 몰래 비밀 한 번 한번 감 기적 센서 자동 살짝 복권 운",
}


def periods(event: dict[str, Any]) -> set[str]:
    values = event.get("periods")
    if isinstance(values, list) and values:
        return {str(value).upper() for value in values}
    return STAGE_PERIODS.get(str(event.get("stage", "")).upper(), set())


def primary(choice: dict[str, Any]) -> dict[str, Any]:
    outcomes = choice.get("outcomes")
    if isinstance(outcomes, list):
        for outcome in outcomes:
            if isinstance(outcome, dict) and outcome.get("primary"):
                return outcome
        for outcome in outcomes:
            if isinstance(outcome, dict):
                return outcome
    return choice


def positive_stats(choice: dict[str, Any]) -> dict[str, int]:
    delta = primary(choice).get("statDelta") or choice.get("statDelta") or {}
    return {key: value for key, value in delta.items() if key in STATS and isinstance(value, int) and value > 0}


def has_explicit_condition(choice: dict[str, Any]) -> bool:
    return bool(choice.get("requiredStats") or choice.get("requiredFlags") or choice.get("blockedFlags") or choice.get("alwaysAvailable"))


def is_hard_condition_event(event: dict[str, Any]) -> bool:
    return str(event.get("kind", "")).upper() in {"BIG_INCIDENT", "MILESTONE"} or str(event.get("type", "")).upper() == "SPECIAL"


def threshold(event_periods: set[str]) -> int:
    values = [THRESHOLDS[p] for p in event_periods if p in THRESHOLDS]
    return min(values) if values else 50


def semantic_scores(event: dict[str, Any], choice: dict[str, Any]) -> Counter[str]:
    flags = " ".join(str(flag).lower() for flag in choice.get("addFlags", []))
    text = " ".join(str(value).lower() for value in (event.get("title", ""), event.get("text", ""), choice.get("label", ""), flags))
    scores: Counter[str] = Counter()
    for stat, signals in FLAG_SIGNALS.items():
        scores[stat] += sum(3 for signal in signals.split() if signal in flags)
    for stat, signals in KOREAN_SIGNALS.items():
        scores[stat] += sum(1 for signal in signals.split() if signal in text)
    return scores


def choose_stat(event: dict[str, Any], choice: dict[str, Any]) -> tuple[str | None, str]:
    positives = positive_stats(choice)
    if len(positives) == 1:
        return next(iter(positives)), "single_positive_outcome_stat"
    scores = semantic_scores(event, choice)
    best = max(scores.values(), default=0)
    winners = [stat for stat, score in scores.items() if score == best and score > 0]
    if len(winners) == 1 and best >= 3:
        return winners[0], "semantic_flag_or_text_match"
    return None, "narrative_or_ambiguous_choice"


def resolve(events: list[dict[str, Any]]) -> tuple[Counter[str], list[dict[str, Any]]]:
    counts: Counter[str] = Counter()
    rows: list[dict[str, Any]] = []
    for event in events:
        event_periods = periods(event)
        choices = event.get("choices") if isinstance(event.get("choices"), list) else []
        if not is_hard_condition_event(event):
            counts["soft_only_event"] += 1
            counts["soft_only_choice"] += len(choices)
            continue
        for choice in choices:
            if not isinstance(choice, dict):
                continue
            if has_explicit_condition(choice):
                counts["existing"] += 1
                rows.append({"eventId": event.get("eventId"), "choiceId": choice.get("choiceId"), "disposition": "existing"})
                continue
            if event_periods == {"INFANT"}:
                choice["alwaysAvailable"] = True
                choice["conditionReview"] = "infant_baseline"
                choice["alwaysAvailableReason"] = "영아기 기본 선택지는 초기 능력치 조건 없이 제공"
                counts["infant_always_available"] += 1
                rows.append({"eventId": event.get("eventId"), "choiceId": choice.get("choiceId"), "disposition": "alwaysAvailable", "reason": "infant_baseline"})
                continue
            stat, basis = choose_stat(event, choice)
            if stat:
                choice["requiredStats"] = {stat: threshold(event_periods)}
                choice["conditionReview"] = "confirmed"
                choice["conditionBasis"] = basis
                counts["confirmed_condition"] += 1
                rows.append({"eventId": event.get("eventId"), "choiceId": choice.get("choiceId"), "disposition": "conditioned", "stat": stat, "threshold": threshold(event_periods), "basis": basis})
            else:
                choice["alwaysAvailable"] = True
                choice["conditionReview"] = "confirmed"
                choice["alwaysAvailableReason"] = "서술·반응·돌발 선택지로 능력치 잠금보다 선택 가능성이 우선"
                counts["always_available"] += 1
                rows.append({"eventId": event.get("eventId"), "choiceId": choice.get("choiceId"), "disposition": "alwaysAvailable", "reason": basis})

        # Preserve at least one playable option even when existing conditions
        # make every choice gated.
        if choices and all(has_explicit_condition(c) and not c.get("alwaysAvailable") for c in choices if isinstance(c, dict)):
            fallback = choices[-1]
            fallback.pop("requiredStats", None)
            fallback["alwaysAvailable"] = True
            fallback["conditionReview"] = "fallback_choice"
            fallback["alwaysAvailableReason"] = "사건 내 최소 한 선택지는 항상 실행 가능해야 함"
            counts["fallback_opened"] += 1
    return counts, rows


def write_report(counts: Counter[str], rows: list[dict[str, Any]]) -> None:
    by_stat: Counter[str] = Counter()
    current = json.loads(EVENT_FILE.read_text(encoding="utf-8"))
    for event in current:
        if not is_hard_condition_event(event):
            continue
        for choice in event.get("choices", []):
            for stat in (choice.get("requiredStats") or {}):
                by_stat[stat] += 1
    lines = [
        "# 8주차 선택지 조건 확정 결과", "",
        f"통합 콘텐츠의 하드 조건 대상 {counts['hard_choice']}개 선택지를 검토한다. 일반 사건 {counts['soft_only_choice']}개 선택지는 기존 명시 조건을 보존하고 새 자동 잠금을 추가하지 않는다.", "",
        f"- 하드 조건 대상 사건: {counts['hard_event']}개",
        f"- 하드 조건 대상 선택지: {counts['hard_choice']}개",
        f"- 일반 사건(소프트 반응만): {counts['soft_only_event']}개 사건 / {counts['soft_only_choice']}개 선택지",
        f"- 입력 시점에 이미 처리된 하드 선택지: {counts['existing']}개",
        f"- 조건 확정 추가: {counts['confirmed_condition']}개",
        f"- 영아기 항상 실행 가능: {counts['infant_always_available']}개",
        f"- 의미상 항상 실행 가능: {counts['always_available']}개",
        f"- 전체 잠금 방지를 위해 추가로 연 선택지: {counts['fallback_opened']}개",
        f"- 최종 조건부 선택지: {counts['final_gated']}개",
        f"- 최종 항상 실행 가능 선택지: {counts['final_always_available']}개",
        f"- 최종 무분류 선택지: {counts['final_unresolved']}개",
        "",
        "## 확정 조건 스탯별 수량", "",
    ]
    for stat, count in sorted(by_stat.items()):
        lines.append(f"- {stat}: {count}개")
    lines.extend(["", "## 정책", "", "- 단일 상승 스탯 결과는 해당 스탯의 시기별 임계값으로 조건화했다.", "- 플래그·문장으로 주 능력이 분명한 다중 스탯 선택지는 의미상 주 스탯으로 조건화했다.", "- 반응·연출·돌발 생존 선택지는 항상 실행 가능으로 확정했다.", "- 각 사건에는 최소 하나의 항상 실행 가능한 선택지를 남겼다.", ""])
    REPORT_FILE.parent.mkdir(parents=True, exist_ok=True)
    REPORT_FILE.write_text("\n".join(lines), encoding="utf-8")


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    data = json.loads(EVENT_FILE.read_text(encoding="utf-8"))
    counts, rows = resolve(data)
    counts["hard_event"] = sum(1 for event in data if is_hard_condition_event(event))
    counts["hard_choice"] = sum(len(event.get("choices", [])) for event in data if is_hard_condition_event(event))
    final_gated = 0
    final_always = 0
    final_unresolved = 0
    for event in data:
        if not is_hard_condition_event(event):
            continue
        for choice in event.get("choices", []):
            if choice.get("alwaysAvailable"):
                final_always += 1
            elif choice.get("requiredStats") or choice.get("requiredFlags") or choice.get("blockedFlags"):
                final_gated += 1
            else:
                final_unresolved += 1
    counts["final_gated"] = final_gated
    counts["final_always_available"] = final_always
    counts["final_unresolved"] = final_unresolved
    EVENT_FILE.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_report(counts, rows)
    print("8주차 선택지 조건 확정")
    for key, value in counts.items():
        print(f"{key}: {value}")
    print(f"final_gated: {final_gated}")
    print(f"final_always_available: {final_always}")
    print(f"final_unresolved: {final_unresolved}")
    print(f"report: {REPORT_FILE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
