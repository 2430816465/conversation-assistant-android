# -*- coding: utf-8 -*-
"""Render a self-contained, explicitly unblinded AI review of synthetic evidence."""
import argparse
import hashlib
import json
from pathlib import Path


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def render(report_dir, cases_path, output):
    verification = read(report_dir / "experiment-verification.json")
    summary = read(report_dir / "ai-review-summary.json")
    decisions_path = report_dir / "ai-review-decisions.json"
    decisions = read(decisions_path)
    if verification["cases"] != 100 or summary["reviewer_type"] != "AI_CODEX" or summary["human_graded"] != 0:
        raise ValueError("Only a disclosed, complete synthetic AI review is supported")
    if digest(decisions_path) != summary["decisions_sha256"] or digest(cases_path) != verification["cases_sha256"]:
        raise ValueError("Review or dataset integrity mismatch")
    source = {c["id"]: c for c in (json.loads(line) for line in cases_path.read_text(encoding="utf-8").splitlines() if line.strip())}
    reviews = {(r["id"], r["variant"]): r for r in decisions["rows"]}
    mapping = read(report_dir / "blind-mapping.json")["rows"]
    items = []
    for row in sorted(mapping, key=lambda x: x["id"]):
        path = report_dir / "grading" / (row["id"] + ".json")
        pair = read(path)
        if digest(path) != row["grading_sha256"] or pair["input_sha256"] != row["input_sha256"]:
            raise ValueError("Output integrity mismatch")
        item = dict(id=pair["id"], category=source[pair["id"]]["category"], expected=pair["rubric"]["expected"],
                    messages=[{k: m.get(k) for k in ["id", "order", "speaker", "kind", "body", "timestamp"]}
                              for m in pair["conversation"]["messages"]],
                    key_ids=pair["rubric"]["key_message_ids"], input_sha256=pair["input_sha256"])
        for scheme in ["ordinary", "full"]:
            label = row[scheme]
            review = reviews[pair["id"], label]
            if review["grading_sha256"] != row["grading_sha256"] or review["input_sha256"] != row["input_sha256"]:
                raise ValueError("Score belongs to other output")
            audit = read(report_dir / (pair["id"] + "-" + scheme + "-audit.json"))
            item[scheme] = dict(pair[label], review=review, calls=len(audit["call_ids"]), error=audit.get("error", ""))
        items.append(item)
    if len(items) != 100 or len(reviews) != 200:
        raise ValueError("All 100 original pairs and 200 AI reviews are required")
    payload = dict(verification=verification, summary=summary, cases=items)
    # Escape script terminators even though the current material is synthetic.
    encoded = json.dumps(payload, ensure_ascii=False, separators=(",", ":"))
    for before, after in [("<", "\\u003c"), (">", "\\u003e"), ("&", "\\u0026"), ("\u2028", "\\u2028"), ("\u2029", "\\u2029")]:
        encoded = encoded.replace(before, after)
    template = Path(__file__).resolve().parents[1] / "qa/quality-review-template.html"
    html = template.read_text(encoding="utf-8").replace("__QUALITY_DATA__", encoded)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(html, encoding="utf-8")
    print("Offline AI review: 100 pairs / 200 reviews; human acceptance remains pending; bytes=%d" % output.stat().st_size)


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--report", type=Path, required=True)
    p.add_argument("--cases", type=Path, default=Path("qa/dialogue-quality-v2.jsonl"))
    p.add_argument("--output", type=Path, required=True)
    args = p.parse_args()
    render(args.report, args.cases, args.output)
