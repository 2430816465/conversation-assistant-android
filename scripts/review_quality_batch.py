# -*- coding: utf-8 -*-
"""Evidence-bound Codex review support. Never writes human scores or grants formal acceptance."""
import argparse
import collections
import hashlib
import json
from pathlib import Path

CRITERIA = ["direct_use", "no_fabrication", "key_history", "style_proxy_fit", "respects_boundaries"]
METRICS = ["natural", "style_proxy", "history", "emotion", "usability"]


def read(path): return json.loads(path.read_text(encoding="utf-8"))
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def show(run, first, last):
    for index in range(first, last + 1):
        path = run / "grading" / ("SYN-%03d.json" % index)
        if not path.is_file(): continue
        pair = read(path); messages = pair["conversation"]["messages"]
        style = list(dict.fromkeys(m["body"] for m in messages if m["speaker"] == "STREAMER" and m["actualSent"]))
        anchors = set(pair["rubric"]["key_message_ids"])
        truth = [{"order": m["order"], "speaker": m["speaker"], "body": m["body"]} for m in messages if m["id"] in anchors]
        context = [{"speaker": m["speaker"], "body": m["body"]} for m in messages[-6:]]
        compact = dict(id=pair["id"], messages=len(messages), expected=pair["rubric"]["expected"], style=style,
                       anchors=truth, recent_context=context)
        for label in ["A", "B"]:
            v = pair[label]
            compact[label] = dict(status=v.get("generation_status", "SUCCEEDED"), no_reply=v["no_reply"],
                                  reason=v["no_reply_reason"] if v["no_reply"] else "",
                                  candidates=[dict(n=i + 1, style=c["style"], text=c["segments"]) for i, c in enumerate(v["candidates"])])
        print(json.dumps(compact, ensure_ascii=False))


def record(run, decisions_path, items):
    saved = read(decisions_path) if decisions_path.is_file() else dict(reviewer_type="AI_CODEX", formal_acceptance=False, human_graded=0, rows=[])
    if saved["reviewer_type"] != "AI_CODEX" or saved["formal_acceptance"] is not False:
        raise ValueError("This file is reserved for disclosed AI review")
    indexed = {(r["id"], r["variant"]): r for r in saved["rows"]}
    for item in items:
        key = item["id"], item["variant"]
        if key in indexed: raise ValueError("Review already recorded; preserve the original or explicitly use a new review file")
        path = run / "grading" / (item["id"] + ".json"); pair = read(path)
        variant = pair[item["variant"]]
        if len(item["criteria"]) != 5 or any(v not in [0, 1] for v in item["criteria"]): raise ValueError("Five explicit decisions required")
        if len(item["ratings"]) != 5 or any(v not in [1, 2, 3, 4, 5] for v in item["ratings"]): raise ValueError("Five comparative ratings required")
        if not isinstance(item["notes"], str) or not item["notes"].strip(): raise ValueError("Evidence notes required")
        if variant.get("generation_status") == "FAILED" and (any(item["criteria"]) or item["ratings"] != [1] * 5):
            raise ValueError("Generation failures must remain failures")
        item = dict(item, input_sha256=pair["input_sha256"], grading_sha256=digest(path),
                    criteria=dict(zip(CRITERIA, item["criteria"])), ratings=dict(zip(METRICS, item["ratings"])))
        indexed[key] = item
    saved["rows"] = [indexed[k] for k in sorted(indexed)]
    decisions_path.parent.mkdir(parents=True, exist_ok=True)
    decisions_path.write_text(json.dumps(saved, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Saved disclosed AI reviews:", len(saved["rows"]), "/200; human scores remain 0.")


def summarize(run, decisions_path, output):
    saved = read(decisions_path); rows = saved["rows"]
    if saved.get("reviewer_type") != "AI_CODEX" or len(rows) != 200: raise ValueError("All 200 disclosed AI reviews required")
    mapping = read(run / "blind-mapping.json")["rows"]
    indexed = {(r["id"], r["variant"]): r for r in rows}
    if set(indexed) != {(m["id"], v) for m in mapping for v in ["A", "B"]}: raise ValueError("Missing or duplicate review")
    source = {json.loads(line)["id"]: json.loads(line) for line in (Path(__file__).resolve().parents[1] / "qa/dialogue-quality-v2.jsonl").read_text(encoding="utf-8").splitlines()}
    counts = {s: dict(proxy_usable=0, criteria_failures=collections.Counter(), rating_sums=collections.Counter(),
                      non_silent_reviewed=0, non_silent_sums=collections.Counter(), categories={}) for s in ["ordinary", "full"]}
    preferences = collections.Counter(); issues = []
    for m in mapping:
        pair = read(run / "grading" / (m["id"] + ".json"))
        totals = {}
        for scheme in counts:
            r = indexed[m["id"], m[scheme]]
            if r["input_sha256"] != m["input_sha256"] or r["grading_sha256"] != m["grading_sha256"]:
                raise ValueError("Review belongs to other output")
            stats = counts[scheme]; passed = all(r["criteria"].values()); stats["proxy_usable"] += int(passed)
            category = source[m["id"]]["category"]
            stats["categories"].setdefault(category, dict(reviewed=0, proxy_usable=0))
            stats["categories"][category]["reviewed"] += 1; stats["categories"][category]["proxy_usable"] += int(passed)
            stats["criteria_failures"].update(k for k, v in r["criteria"].items() if not v)
            stats["rating_sums"].update(r["ratings"]); totals[scheme] = sum(r["ratings"].values())
            variant = pair[m[scheme]]
            if variant.get("generation_status") == "SUCCEEDED" and not variant["no_reply"]:
                stats["non_silent_reviewed"] += 1
                stats["non_silent_sums"].update(r["ratings"])
            if not passed: issues.append(dict(id=m["id"], scheme=scheme, variant=m[scheme],
                                             generation_status=variant.get("generation_status"), criteria=r["criteria"], notes=r["notes"]))
        preferences["tie" if totals["full"] == totals["ordinary"] else max(totals, key=totals.get)] += 1
    for stats in counts.values():
        stats["mean_ratings"] = {k: v / 100 for k, v in stats.pop("rating_sums").items()}
        total = stats["non_silent_reviewed"]
        stats["mean_ratings_non_silent"] = {k: v / total for k, v in stats.pop("non_silent_sums").items()} if total else {}
    report = dict(reviewer_type="AI_CODEX", state="AI_REVIEWED_NOT_HUMAN_ACCEPTED", cases=100, reviewed_outputs=200,
                  human_graded=0, formal_acceptance=False, formal_pass=None, schemes=counts, ai_preference=dict(preferences), issues=issues,
                  decisions_sha256=digest(decisions_path), limitations="Synthetic examples and AI review cannot verify real host style or replace host blind acceptance; candidate-only review, not every analysis sentence.")
    output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "issues"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    p = argparse.ArgumentParser(); p.add_argument("--run", type=Path, required=True); p.add_argument("--first", type=int, default=1)
    p.add_argument("--last", type=int, default=100); args = p.parse_args(); show(args.run, args.first, args.last)
