#!/usr/bin/env python3
"""Week 10 data integrity audit. --acceptance additionally enforces unfinished delivery gates.

This never assigns new stat gates, changes story data, or calls a QA title oracle app code.
Run existing validators as well; this extends, rather than replaces, their checks.
"""
from __future__ import annotations

import argparse
from collections import Counter
import hashlib
import json
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
STATS = {"건강", "운동능력", "지력", "사회성", "경제력", "행복", "운",
         "health", "fitness", "intelligence", "social", "wealth", "happiness", "luck"}
FACTS = {"educationLevel", "fieldOfStudy", "careerTrack", "careerLevel", "employmentStatus",
         "maritalStatus", "minChildrenCount", "housingType", "wealthBand", "requiredAchievements"}
RULE_FACTS = {"educationLevel", "fieldOfStudy", "careerTrack", "careerLevel", "employmentStatus",
              "maritalStatus", "childrenAtLeast", "housingType", "achievement", "healthStatus", "wealthNote"}
TOKENS = {"name", "nameCall"}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"duplicate JSON object key: {key}")
        result[key] = value
    return result


def load_bundle(asset_dir):
    return {name: json.loads((asset_dir / f"{name}.sample.json").read_text(encoding="utf-8-sig"),
                             object_pairs_hook=unique_object)
            for name in ("events", "endings", "titles", "periods", "focuses", "characters", "life_facts", "image_ids")}


def audit(bundle, parser_source="", source_tree=""):
    errors, runtime_gaps, choices_review = [], [], []
    events, endings, titles = (bundle[k] for k in ("events", "endings", "titles"))
    produced = {"gender_male", "gender_female", "military_service_required"}
    periods = {r["periodId"]: r["years"] for r in bundle["periods"]}
    images = {r["imageId"] for r in bundle["image_ids"]}
    achievement_flags = {r["flag"] for r in bundle["life_facts"] if r.get("fact") == "achievement"}
    ids_by_scope = {}

    def error(where, message):
        errors.append({"location": where, "message": message})

    def unique(rows, key, where):
        values = [r.get(key) for r in rows]
        for value, count in Counter(values).items():
            if not isinstance(value, str) or not value or count != 1:
                error(where, f"invalid/duplicate {key}: {value!r} count={count}")
        ids_by_scope[where] = len(values)

    for name, key in (("events", "eventId"), ("endings", "endingId"), ("titles", "titleId"),
                      ("periods", "periodId"), ("focuses", "focusId"), ("characters", "characterId"), ("image_ids", "imageId")):
        # Starter data uses id in some old schemas; existing validator owns that schema.
        if bundle[name] and key not in bundle[name][0] and name == "characters":
            continue
        unique(bundle[name], key, name)
    for row in bundle["characters"]:
        produced.update(row.get("startFlags", []))
    for event in events:
        produced.add("v4:event:" + event["eventId"])
        for choice in event["choices"]:
            for outcome in choice["outcomes"]:
                produced.update(outcome.get("addFlags", choice.get("addFlags", [])))

    def flags(node, where):
        for key in ("requiredFlags", "blockedFlags", "addFlags", "removeFlags"):
            value = node.get(key, [])
            if not isinstance(value, list) or any(not isinstance(v, str) or not v for v in value):
                error(where, f"{key} must be nonempty-string array")
                continue
            if key != "addFlags":
                for flag in value:
                    if flag not in produced:
                        error(where, f"unsourced {key}: {flag}")
        req, blocked = node.get("requiredFlags", []), node.get("blockedFlags", [])
        if isinstance(req, list) and isinstance(blocked, list) and set(req) & set(blocked):
            error(where, "same flag is both required and blocked")
        if set(node.get("addFlags", [])) & set(node.get("removeFlags", [])):
            error(where, "same result flag is added and removed")

    def stats(value, where, bounded=False):
        if not isinstance(value, dict):
            error(where, "stat object required")
            return
        for stat, number in value.items():
            if stat not in STATS:
                error(where, f"unknown stat: {stat}")
            if type(number) is not int or (bounded and not 0 <= number <= 100):
                error(where, f"invalid stat value: {stat}={number}")

    def conditions(node, where):
        flags(node, where)
        for key in ("stats", "requiredStats", "minStats", "maxStats"):
            if key in node:
                stats(node[key], where + "." + key, bounded=True)
        for key, minimum in node.get("minStats", {}).items():
            maximum = node.get("maxStats", {}).get(key, 100)
            if type(minimum) is int and type(maximum) is int and minimum > maximum:
                error(where, f"min > max for {key}")
        for key in node.get("lifeFacts", {}):
            if key not in FACTS:
                error(where, f"unread lifeFacts field: {key}")
        child_min = node.get("lifeFacts", {}).get("minChildrenCount")
        if child_min is not None and (type(child_min) is not int or child_min < 0):
            error(where, "minChildrenCount must be a nonnegative integer")
        achievements = node.get("lifeFacts", {}).get("requiredAchievements", [])
        if not isinstance(achievements, list) or any(a not in achievement_flags for a in achievements):
            error(where, "requiredAchievements must reference produced achievement flags")
        for unsupported in ("requiresFlags", "excludesFlags", "alwaysAvailableIf"):
            if node.get(unsupported):
                error(where, f"unsupported condition alias: {unsupported}")
        if node.get("maxStats") and 'optJSONObject("maxStats")' not in parser_source:
            runtime_gaps.append({"location": where, "code": "MAX_STATS_NOT_PARSED"})

    for event in events:
        eid = event["eventId"]
        conditions(event.get("conditions", {}), eid + ".conditions")
        explicit = event.get("periods", [])
        for period in explicit:
            if period not in periods:
                error(eid, f"invalid period: {period}")
            year = event.get("yearInPeriod")
            if year is not None and (type(year) is not int or not 1 <= year <= periods.get(period, 0)):
                error(eid, f"year outside period: {year}")
        for key in ("imageId", "povImageId", "momentImageId"):
            if event.get(key) and event[key] not in images:
                error(eid, f"unknown {key}: {event[key]}")
        unique(event["choices"], "choiceId", eid + ".choices")
        fallback = False
        for choice in event["choices"]:
            cid = f"{eid}:{choice['choiceId']}"
            conditions(choice, cid)
            is_open = not any(choice.get(k) for k in ("requiredStats", "stats", "requiredFlags", "blockedFlags", "lifeFacts"))
            fallback |= is_open
            if choice.get("alwaysAvailable") is True and not is_open:
                error(cid, "alwaysAvailable contradicts actual runtime requirements")
            unique(choice["outcomes"], "outcomeId", cid + ".outcomes")
            chances = [o.get("chance") for o in choice["outcomes"]]
            if any(type(v) is not int or not 0 <= v <= 100 for v in chances) or sum(v for v in chances if type(v) is int) != 100:
                error(cid, "authored base chance must be integer 0..100 with sum 100")
            for outcome in choice["outcomes"]:
                where = cid + ":" + outcome["outcomeId"]
                flags(outcome, where)
                stats(outcome.get("statDelta", {}), where + ".statDelta")
                stats(outcome.get("chanceWeights", {}), where + ".chanceWeights")
                if outcome.get("hidden") and (outcome.get("primary") or outcome.get("previewText")):
                    error(where, "hidden result leaked into preview")
                if outcome.get("death") and (set(explicit) & {"INFANT", "CHILD"} or event.get("stage") in {"INFANT", "CHILD"}):
                    error(where, "forbidden infant/child death")
                if outcome.get("removeFlags") and 'optJSONArray("removeFlags")' not in parser_source:
                    runtime_gaps.append({"location": where, "code": "REMOVE_FLAGS_NOT_PARSED"})
            hard = event.get("kind") in {"MILESTONE", "BIG_INCIDENT"}
            authored_requirements = {k: choice[k] for k in ("requiredStats", "stats", "requiredFlags", "blockedFlags", "lifeFacts") if choice.get(k)}
            review_input = {"title": event["title"], "text": event["text"], "label": choice["label"], "requirements": authored_requirements, "kind": event["kind"]}
            choices_review.append({"eventId": eid, "choiceId": choice["choiceId"], "kind": event.get("kind"),
                "label": choice["label"], "requirements": {k: choice[k] for k in ("requiredStats", "requiredFlags", "blockedFlags") if choice.get(k)},
                "sourceHash": hashlib.sha256(json.dumps(review_input, ensure_ascii=False, sort_keys=True).encode()).hexdigest(),
                "structurallyOpen": is_open, "hardScope": hard,
                "authoredBasis": choice.get("conditionBasis") or choice.get("alwaysAvailableReason"),
                "semanticReview": "EXISTING_AUTHOR_RATIONALE_NOT_NEW_MANUAL_CERTIFICATION" if choice.get("conditionBasis") or choice.get("alwaysAvailableReason") else "REVIEW_REASON_NEEDED"})
        if not fallback:
            error(eid, "no structurally unconditional alternative")

    for kind, rows in (("ending", endings), ("title", titles)):
        if sum(row.get("isDefault") is True for row in rows) != 1:
            error(kind, "exactly one default required")
        for row in rows:
            where = row[kind + "Id"]
            if type(row.get("priority")) is not int:
                error(where, "priority must be integer")
            conditions(row.get("conditions", {}), where)
            extra = set(row.get("conditions", {})) - {"minStats", "maxStats", "requiredFlags", "blockedFlags", "lifeFacts"}
            if extra:
                error(where, f"unknown {kind} conditions: {sorted(extra)}")
            if row.get("isDefault") and any(row.get("conditions", {}).values()):
                error(where, "default must not require conditions")
    seen_rules = set()
    for i, rule in enumerate(bundle["life_facts"]):
        if rule.get("flag") not in produced:
            error(f"life_facts[{i}]", "rule flag has no producer")
        if rule.get("fact") not in RULE_FACTS:
            error(f"life_facts[{i}]", f"unread fact type: {rule.get('fact')}")
        key = (rule.get("flag"), rule.get("fact"))
        if key in seen_rules:
            error(f"life_facts[{i}]", f"duplicate flag/fact mapping: {key}")
        seen_rules.add(key)
        if rule.get("fact") == "childrenAtLeast":
            try:
                value = str(rule["value"])
                if not value.isdigit():
                    raise ValueError()
            except (KeyError, ValueError):
                error(f"life_facts[{i}]", "childrenAtLeast must be a nonnegative integer value")

    def text_tokens(node, location):
        if isinstance(node, str):
            for token in re.findall(r"\{([^{}]+)\}", node):
                if token not in TOKENS:
                    error(location, f"unsupported token {{{token}}}")
        elif isinstance(node, list):
            for i, child in enumerate(node):
                text_tokens(child, f"{location}[{i}]")
        elif isinstance(node, dict):
            for key, child in node.items():
                text_tokens(child, location + "." + key)
    text_tokens(bundle, "assets")
    choice_count = sum(len(e["choices"]) for e in events)
    gates = []
    if choice_count < 1000:
        gates.append({"code": "CHOICE_TARGET_NOT_MET", "actual": choice_count, "required": 1000, "owner": "신기훈"})
    if "parseTitles" not in source_tree:
        gates.append({"code": "TITLE_RUNTIME_NOT_CONNECTED", "owner": "김우영·김재겸"})
    if not (ROOT / "docs/week10/contracts.md").exists():
        gates.append({"code": "SHARED_CONTRACT_NOT_SUBMITTED", "owner": "김우영·김선한 검토"})
    gates.extend(runtime_gaps)
    return {"schemaVersion": 1, "errors": errors, "runtimeGaps": runtime_gaps, "acceptanceFailures": gates,
        "metrics": {"events": len(events), "choices": choice_count, "outcomes": sum(len(c["outcomes"]) for e in events for c in e["choices"]),
                    "endings": len(endings), "titles": len(titles), "lifeFactRules": len(bundle["life_facts"]), "producedFlags": len(produced),
                    "scopedIdCollectionsChecked": len(ids_by_scope)},
        "choiceReview": choices_review,
        "scope": "STATIC_DATA_INTEGRITY; source capability scans are not runtime proof; semantic review is separate"}


def apply_semantic_review(result, document):
    """Never carry an old decision over to an edited choice or an unseen new event."""
    known = {r["id"]: r for r in document.get("entries", [])}
    counts = Counter()
    for row in result["choiceReview"]:
        if not row["hardScope"]:
            continue
        key = row["eventId"] + ":" + row["choiceId"]
        review = known.get(key)
        if review is None or review.get("sourceHash") != row["sourceHash"]:
            verdict = "UNREVIEWED_OR_CHANGED"
        else:
            verdict = review["verdict"]
            row["semanticReason"] = review["reason"]
        row["semanticReview"] = verdict
        counts[verdict] += 1
        if verdict not in {"KEEP_OPEN", "KEEP_CONDITION"}:
            result["acceptanceFailures"].append({"code": "CHOICE_MEANING_REVIEW_OPEN", "location": key, "verdict": verdict, "owner": "신기훈·김우영"})
    result["semanticReviewCounts"] = dict(counts)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--assets", type=Path, default=ASSETS)
    parser.add_argument("--report", type=Path, default=ROOT / "docs/week10/qa/content_audit.json")
    parser.add_argument("--acceptance", action="store_true")
    parser.add_argument("--semantic-review", type=Path, default=ROOT / "docs/week10/qa/semantic_review.json")
    args = parser.parse_args()
    try:
        source = ROOT / "app/src/main/java/com/example/lifegame"
        result = audit(load_bundle(args.assets), (source / "data/V4ContentParser.kt").read_text(encoding="utf-8"),
                       "\n".join(p.read_text(encoding="utf-8") for p in source.rglob("*.kt")))
        document = json.loads(args.semantic_review.read_text(encoding="utf-8")) if args.semantic_review.exists() else {}
        apply_semantic_review(result, document)
        result["sourceCommit"] = __import__("subprocess").check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
        result["assetSha256"] = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in args.assets.glob("*.json")}
    except (OSError, ValueError, KeyError, TypeError) as exc:
        result = {"errors": [{"location": str(args.assets), "message": str(exc)}], "acceptanceFailures": []}
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# 10주차 콘텐츠·조건 감사", "", f"검증 소스: `{result.get('sourceCommit', 'unknown')}`.", "",
             f"구조 오류 {len(result['errors'])}건, 최종 인수 미해결 {len(result['acceptanceFailures'])}건.", "",
             "자동 구조 검사, 기존 작성 근거, 이번 AI 의미 검토는 서로 다른 증거다. 일반 사건 전체가 새로 수동 승인됐다고 간주하지 않는다.", "",
             "## 수량", "", "```json", json.dumps(result.get("metrics", {}), ensure_ascii=False, indent=2), "```", "",
             "## 돌발·전환점 의미 검토", "", "```json", json.dumps(result.get("semanticReviewCounts", {}), ensure_ascii=False, indent=2), "```", "",
             "KEEP는 현재 설계 유지 의견, CLARIFY/CHANGE는 담당자의 재설계·설명 보완이 필요하다는 의견이다. 임계값의 실제 균형을 보증하지 않는다. 문구/맥락/조건이 바뀌면 sourceHash가 달라져 미검토로 돌아간다.", "",
             "| 사건:선택 | 행동 | 조건 | 검토 | 근거 |", "| --- | --- | --- | --- | --- |"]
    for row in result.get("choiceReview", []):
        if row["hardScope"]:
            fields = [row["eventId"] + ":" + row["choiceId"], row["label"], json.dumps(row["requirements"], ensure_ascii=False), row["semanticReview"], row.get("semanticReason", "새 의미 검토 필요")]
            lines.append("| " + " | ".join(str(v).replace("|", "\\|").replace("\n", " ") for v in fields) + " |")
    lines += ["", "## 실행 연결 누락", "", "```json", json.dumps(result.get("runtimeGaps", []), ensure_ascii=False, indent=2), "```", "",
              "칭호 실행·선택지 목표·공유 계약 등 전체 미해결 목록과 796개 전수 행은 동명의 JSON 보고서를 참고한다."]
    args.report.with_suffix(".md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k in {"metrics", "errors", "runtimeGaps", "semanticReviewCounts"}}, ensure_ascii=True, indent=2))
    print(f"final_acceptance_open_items={len(result['acceptanceFailures'])}; full details: {args.report}")
    return int(bool(result["errors"] or (args.acceptance and result["acceptanceFailures"])))


if __name__ == "__main__":
    raise SystemExit(main())
