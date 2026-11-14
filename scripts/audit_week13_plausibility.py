#!/usr/bin/env python3
"""Week 13 (2026-11-13) plausibility audit for 신기훈's scenario content.

Checks, without touching any file:
- choice labels carry no trailing period (body and result sentences keep theirs)
- love line order: meet -> date -> deepen/conflict -> marriage talk -> 상견례 -> wedding -> child plan -> birth
  (no path from "no relationship" straight to engaged/married/child)
- 상견례 happens before marriage; post-marriage family gatherings are separate events
- workplace words (회사·출근·육아휴직·퇴임식 …) only appear in events gated by job_employed,
  except an explicit allowlist where the text is a job offer, job entry, or a third party's job
- employment outcomes remove the conflicting employment states
- auto events: >=5 per period (>=5 without conditions), elapsed-time/progress triggers only,
  dedupe present, stat effect only when statDelta is non-empty
- scenario extensions only reference existing events, choices, titles and auto events

Exit code 1 when any error is found. Report: --report path (JSON).
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "app" / "src" / "main" / "assets"
PERIODS = ["INFANT", "CHILD", "TEEN", "YOUNG_ADULT", "MIDDLE", "OLD_AGE"]
STATS = {"건강", "운동능력", "지력", "사회성", "경제력", "행복", "운"}
WORK = re.compile(r"회사|출근|퇴근|팀장|육아휴직|퇴임|월급|회식|사내|인사팀|사무실|직장|야근|입사|퇴사|승진|정년")
# Events that may mention work without job_employed, with the reason reviewed on 2026-11-13.
WORK_ALLOWLIST = {
    "teen_019": "특성화고 결과의 미래 취업 제안",
    "adult_001": "대학·취업 갈림길(입사 합격 문자 자체가 선택지)",
    "adult_071": "산업기능요원·사회복무요원 복무 형태 설명",
    "adult_072": "무직자의 첫 취업 사건",
    "adult_076": "'알바든 회사든' 일반 표현",
    "path_int_07": "졸업 후 진로 선택(입사 선택지)",
    "path_soc_04": "첫 일자리 선택",
    "path_wea_01": "청소년 아르바이트 첫 급여",
    "path_wea_03": "첫 일자리 계약서 선택",
    "path_wea_07": "가게 주인이 다시 취업하는 선택지",
    "path_wea_08": "창업자 본인의 회사",
    "path_wea_09": "변호사 사무실(제3자 장소)",
    "path_hap_03": "첫 일자리 선택",
    "path_hap_05": "댓글 단 사람의 출근길(제3자)",
    "path_luk_05": "동창의 회사(제3자)",
    "path_luk_10": "미래 첫 직장 상사(취업 계기)",
    "path_fit_12": "낙방 뒤 재활센터 첫 출근(취업 결과)",
    "path_fit_17": "조기축구회 회식(동호회)",
    "path_wea_12": "실직자의 재취업 사건",
}
EMPLOYMENT = {"job_employed", "job_self_employed", "job_unemployed", "job_retired", "early_retire", "laid_off", "comeback"}
TRIGGERS = {"ELAPSED_TIME", "PROGRESS_STAGE"}
STAGES = {"PERIOD_START", "PERIOD_MID", "PERIOD_END", "AFTER_EVENT"}
AUTO_ADD_ALLOWED = {"dating", "partner_second_chance"}


def load(name: str):
    path = ASSETS / name
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else None


def label_has_period(label: str) -> bool:
    if label.endswith(".") and not label.endswith(".."):
        return True
    return len(label) >= 2 and label[-1] in "\"'" and label[-2] == "." and not label.endswith(".." + label[-1])


def audit(bundle: dict) -> dict:
    events, auto, ext = bundle["events"], bundle["auto"], bundle["ext"]
    titles, focuses = bundle["titles"], bundle["focuses"]
    errors: list[dict] = []

    def err(where: str, message: str):
        errors.append({"location": where, "message": message})

    by_id = {e["eventId"]: e for e in events}
    produced = {f"v4:event:{e['eventId']}" for e in events}
    for e in events:
        for c in e["choices"]:
            for o in c["outcomes"]:
                produced.update(o.get("addFlags", []))

    # 1. labels ------------------------------------------------------------
    for e in events:
        for c in e["choices"]:
            if label_has_period(c["label"]):
                err(f"{e['eventId']}:{c['choiceId']}", f"choice label ends with a period: {c['label']}")

    # 2. love line ---------------------------------------------------------
    for e in events:
        req = set(e["conditions"].get("requiredFlags", []))
        blk = set(e["conditions"].get("blockedFlags", []))
        eid = e["eventId"]
        for c in e["choices"]:
            for o in c["outcomes"]:
                add = set(o.get("addFlags", []))
                where = f"{eid}:{c['choiceId']}:{o['outcomeId']}"
                if "married" in add and not {"engaged", "families_met"} <= req:
                    err(where, "adds married without engaged+families_met (skips marriage talk/상견례/wedding)")
                if "engaged" in add and not {"dating", "rel_steady"} <= req:
                    err(where, "adds engaged without dating+rel_steady")
                if "has_child" in add and not ("married" in req and ("child_plan" in req or "child_plan" in add)):
                    err(where, "adds has_child without married and a child plan")
                if "child_plan" in add and "married" not in req:
                    err(where, "adds child_plan without married")
                if "dating" in add and not {"dating", "married", "engaged"} <= blk:
                    err(where, "starts a relationship in an event that does not block dating/married/engaged")
        if "v4:event:adult_039" in req:
            err(eid, "child-dependent event must use lifeFacts.minChildrenCount, not the adult_039 completion flag")
        if "상견례" in e["title"] or eid == "love_family_meet_01":
            if "engaged" not in req or "married" not in blk:
                err(eid, "상견례 must require engaged and block married")
        if "married" in req and "상견례" in e["title"] + e["text"]:
            err(eid, "post-marriage event mentions 상견례")

    # 3. employment ----------------------------------------------------------
    for e in events:
        eid = e["eventId"]
        req = set(e["conditions"].get("requiredFlags", []))
        texts = [e["title"], e["text"]] + [c["label"] for c in e["choices"]] + [o["text"] for c in e["choices"] for o in c["outcomes"]]
        hits = sorted({m for t in texts for m in WORK.findall(t)})
        if hits and "job_employed" not in req and eid not in WORK_ALLOWLIST:
            err(eid, f"workplace words without job_employed gate: {hits}")
        for c in e["choices"]:
            for o in c["outcomes"]:
                add, rm = set(o.get("addFlags", [])), set(o.get("removeFlags", []))
                where = f"{eid}:{c['choiceId']}:{o['outcomeId']}"
                employed = bool(add & {"job_employed", "comeback"})
                if employed and not {"job_unemployed", "job_self_employed", "job_retired"} <= rm:
                    err(where, "employment outcome keeps conflicting employment states")
                elif not employed and "job_self_employed" in add and not {"job_employed", "job_unemployed"} <= rm:
                    err(where, "self-employment outcome keeps job_employed/job_unemployed")
                elif not employed and add & {"job_unemployed", "laid_off", "job_retired", "early_retire"} and "job_employed" not in rm:
                    err(where, "leaving work keeps job_employed")
                if {"comeback", "laid_off"} <= add:
                    order = o["addFlags"]
                    if order.index("comeback") < order.index("laid_off"):
                        err(where, "comeback must be added after laid_off so the final fact is EMPLOYED")
    for f in focuses:
        for t in f.get("yearlyLogTemplates", []):
            if WORK.search(t):
                err(f["focusId"], f"focus yearly log assumes a job: {t}")
    job_titles = {"title_cat_birthday", "title_alarm_seven", "title_free_morning", "title_chicken"}
    for t in titles:
        if t["titleId"] not in job_titles and WORK.search(t["description"]):
            err(t["titleId"], f"title description assumes a job: {t['description']}")

    # 4. auto events --------------------------------------------------------
    auto_ids = set()
    if auto is None:
        err("auto_events.sample.json", "missing")
        auto = {"events": []}
    per_period, open_per_period = Counter(), Counter()
    auto_produced = set()
    for a in auto["events"]:
        auto_produced.update(a.get("addFlags", []))
    for a in auto["events"]:
        aid = a.get("eventId", "")
        where = f"auto:{aid}"
        if aid in auto_ids or aid in by_id or not re.fullmatch(r"auto_[a-z0-9_]+", aid):
            err(where, "auto eventId must be unique, auto_ prefixed and not collide with events")
        auto_ids.add(aid)
        for key in ("periods", "text", "trigger", "triggerRule", "conditions", "statEffect", "statDelta", "addFlags",
                    "removeFlags", "repeatable", "dedupe"):
            if key not in a:
                err(where, f"missing field {key}")
        if a.get("trigger") not in TRIGGERS:
            err(where, f"trigger must be one of {sorted(TRIGGERS)}")
        rule = a.get("triggerRule", {})
        if a.get("trigger") == "PROGRESS_STAGE" and rule.get("stage") not in STAGES:
            err(where, "PROGRESS_STAGE needs triggerRule.stage")
        if a.get("trigger") == "ELAPSED_TIME" and not isinstance(rule.get("afterAutoSteps"), int):
            err(where, "ELAPSED_TIME needs integer triggerRule.afterAutoSteps")
        if rule.get("stage") == "AFTER_EVENT" and rule.get("afterEventId") not in by_id:
            err(where, f"afterEventId does not exist: {rule.get('afterEventId')}")
        if re.search(r"매년|해마다|EVERY_YEAR|YEARLY", json.dumps(rule, ensure_ascii=False)):
            err(where, "auto events must not use a yearly counter")
        delta = a.get("statDelta", {})
        if (a.get("statEffect") == "STAT") != bool(delta):
            err(where, "statEffect STAT exactly when statDelta is non-empty")
        if set(delta) - STATS:
            err(where, f"unknown stat {sorted(set(delta) - STATS)}")
        if set(a.get("addFlags", [])) - AUTO_ADD_ALLOWED:
            err(where, f"auto events may only add {sorted(AUTO_ADD_ALLOWED)}")
        if a.get("repeatable") is not False or not a.get("dedupe", {}).get("key"):
            err(where, "auto events must be non-repeatable with a dedupe key")
        cond = a.get("conditions", {})
        for flag in cond.get("requiredFlags", []) + cond.get("blockedFlags", []):
            if flag not in produced and flag not in auto_produced:
                err(where, f"unsourced condition flag {flag}")
        req = set(cond.get("requiredFlags", []))
        if WORK.search(a.get("text", "")) and "job_employed" not in req:
            err(where, "auto text assumes a job without job_employed")
        if re.search(r"배우자|결혼기념일", a.get("text", "")) and "married" not in req:
            err(where, "spouse text without married")
        if re.search(r"아이|손주", a.get("text", "")) and not cond.get("lifeFacts", {}).get("minChildrenCount"):
            err(where, "child text without lifeFacts.minChildrenCount")
        for p in a.get("periods", []):
            if p not in PERIODS:
                err(where, f"invalid period {p}")
            per_period[p] += 1
            if not cond.get("requiredFlags") and not cond.get("blockedFlags") and not cond.get("lifeFacts"):
                open_per_period[p] += 1
    for p in PERIODS:
        if per_period[p] < 5:
            err(f"auto:{p}", f"fewer than 5 auto events ({per_period[p]})")
        if open_per_period[p] < 5:
            err(f"auto:{p}", f"fewer than 5 unconditional auto events ({open_per_period[p]})")

    # 5. extensions --------------------------------------------------------
    if ext is None:
        err("scenario_extensions.sample.json", "missing")
    else:
        targets = set(by_id) | auto_ids | {"ENDING"}
        for s in ext.get("recallSlots", []):
            src = by_id.get(s["sourceEventId"])
            if src is None:
                err(s["slotId"], "unknown sourceEventId")
                continue
            known = {c["choiceId"] for c in src["choices"]}
            if set(s["sourceChoiceIds"]) - known:
                err(s["slotId"], "unknown sourceChoiceIds")
            for t in s["showIn"]:
                if t not in targets:
                    err(s["slotId"], f"unknown showIn target {t}")
            for flag in s.get("requiredFlags", []) + s.get("blockedFlags", []):
                if flag not in produced and flag not in auto_produced:
                    err(s["slotId"], f"unsourced flag {flag}")
        cards = ext.get("turningPointCards", {})
        for domain in ("EDUCATION", "CAREER", "FAMILY"):
            if len(cards.get(domain, [])) < 2:
                err(f"cards:{domain}", "needs at least 2 turning-point candidates")
            for card in cards.get(domain, []):
                if card["eventId"] not in by_id:
                    err(card["cardId"], "unknown eventId")
        goal_ids = {g["titleId"] for g in ext.get("titleGoals", [])}
        if goal_ids != {t["titleId"] for t in titles}:
            err("titleGoals", "must cover every title exactly")
        for npc in ext.get("npcReappearances", []):
            for ap in npc["appearances"]:
                if ap["id"] not in targets:
                    err(npc["npcId"], f"unknown appearance {ap['id']}")
        if re.search(r"지난 인생|이전 인생|비교", json.dumps(ext.get("recallSlots", []), ensure_ascii=False)):
            err("recallSlots", "recall lines must not compare past lives")

    return {
        "errors": errors,
        "metrics": {
            "events": len(events),
            "autoEvents": len(auto["events"]),
            "autoPerPeriod": {p: per_period[p] for p in PERIODS},
            "autoUnconditionalPerPeriod": {p: open_per_period[p] for p in PERIODS},
            "workAllowlist": len(WORK_ALLOWLIST),
        },
    }


def load_bundle() -> dict:
    return {"events": load("events.sample.json"), "auto": load("auto_events.sample.json"),
            "ext": load("scenario_extensions.sample.json"), "titles": load("titles.sample.json"),
            "focuses": load("focuses.sample.json")}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    result = audit(load_bundle())
    text = json.dumps(result, ensure_ascii=False, indent=2)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(text + "\n", encoding="utf-8")
    print(json.dumps({"errors": len(result["errors"]), **result["metrics"]}, ensure_ascii=True, indent=2))
    for e in result["errors"][:50]:
        print(f"ERROR {e['location']}: {e['message']}".encode("ascii", "backslashreplace").decode())
    return int(bool(result["errors"]))


if __name__ == "__main__":
    sys.exit(main())
