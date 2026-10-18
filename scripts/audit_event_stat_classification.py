#!/usr/bin/env python3
"""Audit event-level stat classification under the week-8 condition policy.

BIG_INCIDENT and MILESTONE events are the hard-condition scope. Ordinary
INCIDENT events remain open choices and are classified as soft-reactive when
their outcomes change one or more stats.
"""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVENT_FILE = ROOT / "app" / "src" / "main" / "assets" / "events.sample.json"
REPORT_FILE = ROOT / "docs" / "qa" / "week8_event_stat_classification.md"
HARD_KINDS = {"BIG_INCIDENT", "MILESTONE"}


def outcome_stats(choice: dict) -> set[str]:
    outcomes = choice.get("outcomes")
    if not isinstance(outcomes, list) or not outcomes:
        outcomes = [choice]
    stats: set[str] = set()
    for outcome in outcomes:
        if not isinstance(outcome, dict):
            continue
        for stat, value in (outcome.get("statDelta") or {}).items():
            if isinstance(value, (int, float)) and value != 0:
                stats.add(str(stat))
    return stats


def main() -> int:
    data = json.loads(EVENT_FILE.read_text(encoding="utf-8"))
    hard = []
    soft = []
    statless = []
    for event in data:
        choices = [c for c in event.get("choices", []) if isinstance(c, dict)]
        affected = set().union(*(outcome_stats(c) for c in choices)) if choices else set()
        row = {
            "event_id": event.get("eventId", ""),
            "kind": event.get("kind", ""),
            "title": event.get("title", ""),
            "stats": sorted(affected),
            "choices": len(choices),
        }
        if not affected:
            statless.append(row)
        elif str(event.get("kind", "")).upper() in HARD_KINDS:
            hard.append(row)
        else:
            soft.append(row)

    hard_choices = sum(r["choices"] for r in hard)
    soft_choices = sum(r["choices"] for r in soft)
    soft_explicit = sum(
        1
        for event in data
        if str(event.get("kind", "")).upper() not in HARD_KINDS
        for choice in event.get("choices", [])
        if choice.get("requiredStats") or choice.get("requiredFlags") or choice.get("blockedFlags")
    )
    stat_counts = Counter(stat for r in hard + soft for stat in r["stats"])
    lines = [
        "# 사건 스탯 분류 재검증",
        "",
        "정책상 스탯 조건을 거는 사건은 `BIG_INCIDENT(돌발사건)`과 `MILESTONE(인생의 전환점)`으로 한정한다.",
        "일반 `INCIDENT`는 기존 명시 조건을 보존하며, 나머지는 추가 잠금 없이 선택 결과의 스탯 변화를 반영한다.",
        "",
        "## 분류 결과",
        "",
        f"- 스탯 조건 분류 완료 사건(하드 조건): **{len(hard)}개** / 선택지 **{hard_choices}개**",
        f"- 일반 사건(추가 하드 조건 미적용, 소프트 반응): **{len(soft)}개** / 선택지 **{soft_choices}개**",
        f"- 일반 사건에 기존부터 있던 명시 조건: **{soft_explicit}개 선택지**",
        f"- 결과 스탯 변화가 전혀 없는 사건: **{len(statless)}개**",
        f"- 전체 사건: **{len(data)}개**",
        "",
        "일반 사건은 이번 단계에서 새 하드 조건을 추가하지 않았다. 기존 JSON에 있던 명시 조건은 보존했으며, 나머지는 선택을 막지 않고 결과 스탯만 반영한다.",
        "",
        "## 하드 조건 분류 완료 사건",
        "",
        "| 종류 | 사건 ID | 제목 | 결과에 반영되는 스탯 | 선택지 수 |",
        "|---|---|---|---|---:|",
    ]
    for row in hard:
        stats = ", ".join(row["stats"]) or "없음"
        lines.append(f"| {row['kind']} | `{row['event_id']}` | {row['title']} | {stats} | {row['choices']} |")
    lines += ["", "## 하드 조건을 걸지 않는 일반 사건", "", "| 사건 ID | 제목 | 결과에 반영되는 스탯 | 선택지 수 |", "|---|---|---|---:|"]
    for row in soft:
        stats = ", ".join(row["stats"]) or "없음"
        lines.append(f"| `{row['event_id']}` | {row['title']} | {stats} | {row['choices']} |")
    lines += ["", "## 해석", "", f"- 하드 조건 검토 대상: {len(hard)}개 사건 / {hard_choices}개 선택지. 실제 조건 누락은 audit_choice_conditions.py에서 검사한다.", f"- 일반 사건 {len(soft)}개의 기존 명시 조건 {soft_explicit}개는 보존하고 나머지는 결과 스탯으로 플레이어 방향성을 누적한다.", f"- 스탯 변화가 없는 사건은 {len(statless)}개다. 스탯 변화가 있다는 사실만으로 능력치에 따라 결과가 달라진다고 보장하지는 않는다.", "", "## 하드·소프트 전체에서 등장한 결과 스탯", ""]
    for stat, count in sorted(stat_counts.items()):
        lines.append(f"- {stat}: {count}개 사건")
    REPORT_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"hard_events={len(hard)} hard_choices={hard_choices}")
    print(f"soft_events={len(soft)} soft_choices={soft_choices}")
    print(f"statless_events={len(statless)} total_events={len(data)}")
    print(f"report={REPORT_FILE}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
