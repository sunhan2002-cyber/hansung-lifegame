#!/usr/bin/env python3
"""Check cross-PR flag, path and image contracts; not a proof of gameplay reachability."""
import json
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app/src/main/assets"
PATHS = {"HEALTH", "FITNESS", "INTELLIGENCE", "SOCIAL", "WEALTH", "HAPPINESS", "LUCK"}
PREFIXES = {"HEALTH": "hea", "FITNESS": "fit", "INTELLIGENCE": "int", "SOCIAL": "soc",
            "WEALTH": "wea", "HAPPINESS": "hap", "LUCK": "luk"}


def load(name):
    return json.loads((ASSETS / name).read_text(encoding="utf-8"))


def validate():
    events = load("events.sample.json")
    endings = load("endings.sample.json")
    routing = load("image_routing.json")
    facts = load("life_facts.sample.json")
    produced = {"gender_male", "gender_female", "military_service_required"}
    for starter in load("characters.sample.json"):
        produced.update(starter.get("startFlags", []))
    for event in events:
        produced.add("v4:event:" + event["eventId"])
        for choice in event["choices"]:
            for outcome in choice.get("outcomes", [choice]):
                produced.update(outcome.get("addFlags", []))
    errors = []
    for event in events:
        for row in [event.get("conditions", {})] + event["choices"]:
            for field in ("requiredFlags", "blockedFlags"):
                for flag in row.get(field, []):
                    if flag not in produced:
                        errors.append(f"{event['eventId']}: unsourced {field}={flag}")
        # A structurally unconditional fallback ensures even low stats can finish the event.
        if not any(not any(c.get(k) for k in ("requiredStats", "stats", "requiredFlags", "blockedFlags", "lifeFacts"))
                   for c in event["choices"]):
            errors.append(f"{event['eventId']}: no unconditional fallback")
        tags = [event["lifePath"]] if event.get("lifePath") else event.get("relatedPaths", [])
        if tags and routing["eventStatTags"].get(event["eventId"]) != [t.lower() for t in tags]:
            errors.append(f"{event['eventId']}: authored path/image theme mismatch")
    for ending in endings:
        for field in ("requiredFlags", "blockedFlags"):
            for flag in ending.get("conditions", {}).get(field, []):
                if flag not in produced:
                    errors.append(f"{ending['endingId']}: unsourced {field}={flag}")
    for rule in facts:
        if rule["flag"] not in produced:
            errors.append(f"life fact flag has no source: {rule['flag']}")
    path_counts = Counter(e.get("lifePath") for e in events if e.get("lifePath"))
    for path in PATHS:
        if path_counts[path] < 8:
            errors.append(f"{path}: fewer than 8 path events")
        count = sum(e["endingId"].startswith("ending_path_" + PREFIXES[path] + "_") for e in endings)
        if count < 2:
            errors.append(f"{path}: fewer than 2 dedicated endings")
    return {"errors": errors, "pathEventCounts": dict(sorted(path_counts.items())),
            "factRules": len(facts), "eventCount": len(events), "endingCount": len(endings),
            "eventImageMappings": len(routing["eventStatTags"])}


if __name__ == "__main__":
    result = validate()
    print(json.dumps(result, ensure_ascii=True, indent=2))
    raise SystemExit(bool(result["errors"]))
