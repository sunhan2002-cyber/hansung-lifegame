#!/usr/bin/env python3
"""Audit explicit conditions on every event choice.

This report deliberately does not mutate content. It separates choices that
already have a condition from choices where a condition can be proposed from
the primary outcome. A teammate must review ambiguous proposals before adding
them to the game data.
"""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "app" / "src" / "main" / "assets"
REPORT_DIR = ROOT / "docs" / "qa"
EVENT_FILE = ASSET_DIR / "events.sample.json"

STAT_KEYS = {"건강", "운동능력", "지력", "사회성", "경제력", "행복", "운"}
STAGE_PERIODS = {
    "INFANT": {"INFANT"},
    "CHILD": {"CHILD"},
    "TEEN": {"TEEN"},
    "ADULT": {"YOUNG_ADULT", "MIDDLE"},
}
STAT_THRESHOLDS = {
    "INFANT": 50,
    "CHILD": 52,
    "TEEN": 55,
    "YOUNG_ADULT": 58,
    "MIDDLE": 60,
    "OLD_AGE": 65,
}


def periods_for_event(event: dict[str, Any]) -> set[str]:
    explicit = event.get("periods")
    if isinstance(explicit, list) and explicit:
        return {str(value).upper() for value in explicit}
    return STAGE_PERIODS.get(str(event.get("stage", "")).upper(), set())


def primary_outcome(choice: dict[str, Any]) -> dict[str, Any]:
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
    outcome = primary_outcome(choice)
    delta = outcome.get("statDelta") or choice.get("statDelta") or {}
    if not isinstance(delta, dict):
        return {}
    return {
        str(stat): int(value)
        for stat, value in delta.items()
        if stat in STAT_KEYS and isinstance(value, int) and value > 0
    }


def explicit_condition(choice: dict[str, Any]) -> tuple[bool, list[str]]:
    fields: list[str] = []
    for key in ("requiredStats", "stats", "requiredFlags", "blockedFlags", "requiresFlags", "excludesFlags"):
        value = choice.get(key)
        if value:
            fields.append(key)
    if choice.get("alwaysAvailable") is True:
        fields.append("alwaysAvailable")
    return bool(fields), fields


def threshold_for(periods: set[str]) -> int:
    known = [STAT_THRESHOLDS[p] for p in periods if p in STAT_THRESHOLDS]
    return min(known) if known else 50


def audit(events: list[dict[str, Any]]) -> tuple[dict[str, Any], list[dict[str, Any]]]:
    details: list[dict[str, Any]] = []
    category_counts: Counter[str] = Counter()
    stat_counts: Counter[str] = Counter()
    event_choice_counts: Counter[str] = Counter()
    invalid: list[dict[str, Any]] = []
    all_locked_events: list[str] = []
    total = 0

    for event in events:
        event_id = str(event.get("eventId", "<missing>"))
        periods = periods_for_event(event)
        choices = event.get("choices") if isinstance(event.get("choices"), list) else []
        locked_count = 0
        for index, choice in enumerate(choices):
            if not isinstance(choice, dict):
                continue
            total += 1
            choice_id = str(choice.get("choiceId", index))
            has_condition, fields = explicit_condition(choice)
            positives = positive_stats(choice)
            unknown_stats = []
            for key in (choice.get("requiredStats") or choice.get("stats") or {}):
                if key not in STAT_KEYS and key not in {"health", "fitness", "intelligence", "social", "wealth", "happiness", "luck"}:
                    unknown_stats.append(str(key))
            invalid_thresholds = []
            for key, value in (choice.get("requiredStats") or choice.get("stats") or {}).items():
                if not isinstance(value, int) or not 0 <= value <= 100:
                    invalid_thresholds.append({"stat": str(key), "value": value})

            if has_condition:
                category = "explicit"
                if fields and all(field not in {"alwaysAvailable"} for field in fields):
                    locked_count += 1
            elif periods == {"INFANT"}:
                category = "infant_unconditional"
            elif len(positives) == 1:
                category = "candidate"
                stat = next(iter(positives))
                stat_counts[stat] += 1
            else:
                category = "ambiguous"

            category_counts[category] += 1
            item = {
                "eventId": event_id,
                "choiceId": choice_id,
                "periods": sorted(periods),
                "category": category,
                "conditionFields": fields,
                "positiveStats": positives,
                "suggestedThreshold": threshold_for(periods) if category == "candidate" else None,
                "label": choice.get("label", ""),
            }
            if unknown_stats:
                item["unknownStats"] = unknown_stats
            if invalid_thresholds:
                item["invalidThresholds"] = invalid_thresholds
            details.append(item)
            if unknown_stats or invalid_thresholds:
                invalid.append(item)

        if choices and locked_count == len(choices):
            all_locked_events.append(event_id)
            event_choice_counts[event_id] = len(choices)

    metrics = {
        "eventCount": len(events),
        "choiceCount": total,
        "explicitConditionCount": category_counts["explicit"],
        "explicitConditionRate": round(category_counts["explicit"] / total * 100, 2) if total else 0,
        "missingConditionCount": total - category_counts["explicit"],
        "reviewRequiredCount": category_counts["candidate"] + category_counts["ambiguous"],
        "candidateConditionCount": category_counts["candidate"],
        "ambiguousConditionCount": category_counts["ambiguous"],
        "infantUnconditionalCount": category_counts["infant_unconditional"],
        "conditionStatCandidates": dict(stat_counts),
        "invalidConditionCount": len(invalid),
        "allChoicesLockedEventCount": len(all_locked_events),
        "allChoicesLockedEvents": all_locked_events,
    }
    return metrics, details


def markdown(metrics: dict[str, Any], details: list[dict[str, Any]]) -> str:
    lines = [
        "# 8주차 선택지 조건 전수 감사",
        "",
        "> 이 보고서는 선택지 데이터를 자동으로 분류한 1차 감사 결과다. `candidate`는 결과 스탯만 보고 조건을 제안할 수 있는 항목이며, 실제 반영 전 시나리오 담당자의 의미 검토가 필요하다.",
        "",
        "## 현황",
        "",
        f"- 사건: **{metrics['eventCount']}개**",
        f"- 선택지: **{metrics['choiceCount']}개**",
        f"- 명시 조건 보유: **{metrics['explicitConditionCount']}개 ({metrics['explicitConditionRate']}%)**",
        f"- 명시 조건 미적용: **{metrics['missingConditionCount']}개**",
        f"  - 영아기 무조건 진행으로 분류: {metrics['infantUnconditionalCount']}개",
        f"  - 실제 의미 검토 필요: **{metrics['reviewRequiredCount']}개**",
        f"    - 단일 상승 스탯으로 조건 제안 가능: {metrics['candidateConditionCount']}개",
        f"    - 의미를 직접 판단해야 하는 모호한 항목: {metrics['ambiguousConditionCount']}개",
        f"- 잘못된 스탯·임계값: {metrics['invalidConditionCount']}개",
        f"- 모든 선택지가 잠기는 사건 후보: {metrics['allChoicesLockedEventCount']}개",
        "",
        "## 스탯별 조건 제안 후보",
        "",
    ]
    for stat, count in sorted(metrics["conditionStatCandidates"].items()):
        lines.append(f"- {stat}: {count}개")
    lines.extend(["", "## 후속 작업", "", "1. `choice_condition_audit.json`의 `candidate` 항목을 시나리오 의미와 대조한다.", "2. 의미가 맞는 항목만 `requiredStats` 또는 플래그 조건으로 반영한다.", "3. `ambiguous` 항목은 조건 없이 둘지, 플래그·시기 조건을 둘지 결정하고 사유를 기록한다.", "4. 조건을 반영한 뒤 이 감사 스크립트를 다시 실행해 누락 수를 갱신한다.", ""])
    if metrics["allChoicesLockedEvents"]:
        lines.extend(["## 전체 잠금 사건", "", *[f"- `{event_id}`" for event_id in metrics["allChoicesLockedEvents"]], ""])
    return "\n".join(lines)


def main() -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    if not EVENT_FILE.exists():
        print(f"ERROR: missing {EVENT_FILE}")
        return 1
    events = json.loads(EVENT_FILE.read_text(encoding="utf-8"))
    if not isinstance(events, list):
        print("ERROR: events.sample.json must contain a list")
        return 1
    metrics, details = audit(events)
    REPORT_DIR.mkdir(parents=True, exist_ok=True)
    (REPORT_DIR / "choice_condition_audit.json").write_text(
        json.dumps({"metrics": metrics, "choices": details}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    (REPORT_DIR / "week8_choice_condition_audit.md").write_text(markdown(metrics, details), encoding="utf-8")
    print("8주차 선택지 조건 전수 감사")
    for key, value in metrics.items():
        print(f"{key}: {value}")
    print(f"report: {REPORT_DIR / 'week8_choice_condition_audit.md'}")
    print(f"details: {REPORT_DIR / 'choice_condition_audit.json'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
