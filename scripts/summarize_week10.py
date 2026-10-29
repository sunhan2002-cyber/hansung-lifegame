#!/usr/bin/env python3
"""Turn actual engine output into a release assessment, keeping unmet goals visible."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import shutil

PATHS = ["HEALTH", "FITNESS", "INTELLIGENCE", "SOCIAL", "WEALTH", "HAPPINESS", "LUCK"]


def assess(summary):
    failures = []
    if summary["runs"] < 10000:
        failures.append("F10-13: fewer than 10,000 actual lives")
    if summary["invariantFailureRuns"]:
        failures.append(f"F10-13: invariant failure runs={summary['invariantFailureRuns']}")
    if summary.get("titleStatus") == "ACTUAL_ENGINE_WITH_INDEPENDENT_QA_COMPARISON":
        mismatches = sum(group.get("titleMismatches", 0) for group in summary["groups"].values())
        if mismatches:
            failures.append(f"F10-07: actual title resolver mismatches={mismatches}")
    for path in PATHS:
        for policy in ("RANDOM", "TARGET"):
            g = summary["groups"].get(f"{policy}/{path}", {})
            if g.get("runs", 0) < 500:
                failures.append(f"F10-13: {policy}/{path} fewer than 500 lives")
        g = summary["groups"].get(f"RANDOM/{path}", {})
        if len(g.get("endings", {})) < 3:
            failures.append(f"F10-13: {path} random ending diversity below 3")
        if path in {"HAPPINESS", "LUCK"} and g.get("pathEndingRate", 0) < 0.20:
            failures.append(f"F10-02/13: {path} random path rate={g.get('pathEndingRate', 0):.1%}, target>=20%")
        if summary["groups"].get(f"TARGET/{path}", {}).get("pathEndings", 0) < 3:
            failures.append(f"F10-02/13: {path} fewer than 3 directed success seeds")
    randoms = [summary["groups"][f"RANDOM/{p}"] for p in PATHS]
    before_middle = sum(n for g in randoms for p, n in g["deathPeriods"].items() if p not in {"MIDDLE", "OLD_AGE"})
    total = sum(g["runs"] for g in randoms)
    if before_middle / total > 0.08:
        failures.append(f"F10-03/13: pre-middle random death rate {before_middle / total:.1%} exceeds 8%")
    return failures


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--input", type=Path, required=True)
    p.add_argument("--out", type=Path, default=Path("docs/week10/qa"))
    p.add_argument("--acceptance", action="store_true")
    args = p.parse_args()
    summary = json.loads((args.input / "summary.json").read_text(encoding="utf-8"))
    failures = assess(summary)
    coverage = {"events": Counter(), "choices": Counter(), "outcomes": Counter(), "lockedChoices": Counter(), "bigIncidentsByPeriod": Counter()}
    examples = {}
    with (args.input / "runs.jsonl").open(encoding="utf-8") as f:
        actual = 0
        for line in f:
            run = json.loads(line)
            actual += 1
            example_key = f"{run['target']}/{'path' if run['pathEnding'] else 'other'}"
            if example_key not in examples:
                examples[example_key] = {k: v for k, v in run.items() if k in {"policy", "target", "seed", "initialStats", "endingId", "pathEnding", "deathPeriod", "stats", "facts", "trace"}}
            for row in run["trace"]:
                e, c, o = row["eventId"], row["choiceId"], row["outcomeId"]
                coverage["events"][e] += 1
                coverage["choices"][e + ":" + c] += 1
                coverage["outcomes"][e + ":" + c + ":" + o] += 1
                coverage["lockedChoices"].update(e + ":" + cid for cid in row["lockedChoiceIds"])
                if row["kind"] == "BIG_INCIDENT":
                    coverage["bigIncidentsByPeriod"][row["period"]] += 1
    if actual != summary["runs"]:
        failures.append(f"Raw record count {actual} != summary count {summary['runs']}")
    args.out.mkdir(parents=True, exist_ok=True)
    for name in ("summary.json", "counterexamples.json", "ending_witnesses.json"):
        if (args.input / name).resolve() != (args.out / name).resolve():
            shutil.copyfile(args.input / name, args.out / name)
    (args.out / "coverage.json").write_text(json.dumps(coverage, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (args.out / "demo_seeds.json").write_text(json.dumps(examples, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assessment = {"status": "FAIL" if failures else "PASS", "failures": failures,
                  "scope": "F10-02/03/13 simulation gates only; does not certify UI, storage, titles or full final acceptance"}
    (args.out / "simulation_acceptance.json").write_text(json.dumps(assessment, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# 10주차 실제 콘텐츠 10,000회 시뮬레이션", "",
             f"검증 소스: `{summary['sourceCommit']}`. 실행 {actual:,}회, 소요 {summary['durationMs']/1000:.1f}초.", "",
             "기본 시작 능력치는 7종 모두 50, 성별/시작 캐릭터 보정 플래그 없음이다. LOW_STATS는 전부 10, BOUNDARY는 1/34/35/59/60/69/70/79/80/99/100으로 변경한다. UI 기본 캐릭터 보정·남성 군복무 경로와 동일한 표본으로 주장하지 않는다.", "",
             "RANDOM은 목표 스탯 초점을 택하고 가능한 선택지를 시드·사건·시기·해 해시로 고른다. TARGET은 같은 초점과 기본 확률 가중 평균 스탯 보상이 최대인 선택을 고른다. 건강 보상은 lifeDelta를 합산한다. 실제 보정 확률을 미리 아는 최적 정책은 아니다. LOW_HEALTH는 반대로 건강 기대 변화가 최소인 초점·선택을 고른다. LOW_STATS는 연간 합계가 최소인 초점과 해시 선택, BOUNDARY는 경계 시작값과 RANDOM 선택이다. LOW_HEALTH/LOW_STATS의 경로 열은 결말 분류용 라벨이며 해당 경로 초점을 선택했다는 뜻이 아니다.", "",
             "| 정책 | 경로 | 실행 | 전용 결말 | 비율 | 결말 종류 | 노년 이전 사망 | 평균 돌발 |",
             "| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: |"]
    for key, g in sorted(summary["groups"].items()):
        policy, path = key.split("/")
        deaths = sum(n for p, n in g["deathPeriods"].items() if p != "OLD_AGE")
        lines.append(f"| {policy} | {path} | {g['runs']} | {g['pathEndings']} | {g['pathEndingRate']:.1%} | {len(g['endings'])} | {deaths} | {g['meanBigIncidents']:.2f} |")
    lines += ["", "## 인수 판정", "", f"**{assessment['status']}**. 치명 불변식 위반 판 수: {summary['invariantFailureRuns']}.", ""]
    lines += ["- " + x for x in failures] or ["- 이 시뮬레이션 범위의 수치 기준 통과."]
    lines += ["", f"관측 사건 {summary['seenEvents']}개, 결말 {summary['seenEndings']}개. 미관측 결말 {len(summary['unseenEndings'])}개:", "",
              "`" + "`, `".join(summary["unseenEndings"]) + "`", "",
              "## 증거와 제한", "",
              f"동일 정책·시드 재실행 {summary['replayChecks']}회 비교. 매 {summary['restorationSampleInterval']}번째 실행의 각 phase에서 엔진 상태 복원을 검사했다. 이는 Android 저장소 또는 실제 프로세스 종료 시험을 대신하지 않는다.", "",
              "summary.json에는 경로별 교육·직업·가족·주거·경제력 분포가 있다. coverage.json은 실제 사건/선택/결과/잠금 및 시기별 돌발 노출 수, demo_seeds.json은 재현 선택 순서다. raw runs.jsonl은 별도 빌드 산출물이며 저장소에 대용량 원본을 넣지 않는다.", "",
              ("칭호는 실제 엔진 판정과 독립 QA 기대값을 비교했다. 경로별 actualTitles/titleMismatches에서 확인한다." if summary.get("titleStatus") == "ACTUAL_ENGINE_WITH_INDEPENDENT_QA_COMPARISON" else "칭호 분포는 QA 독립 기대값이며 앱 칭호 기능의 실행 결과가 아니다.") + " ending_witnesses.json은 합성 상태에서 조건·우선순위를 확인한 자료로, 정상 플레이로 도달했다는 증거가 아니다. 미관측/증인 불일치는 담당자 검토 대상으로 남긴다."]
    (args.out / "simulation_report.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps(assessment, ensure_ascii=True, indent=2))
    return int(bool(args.acceptance and failures))


if __name__ == "__main__":
    raise SystemExit(main())
