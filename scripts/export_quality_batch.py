# -*- coding: utf-8 -*-
"""Export only completed synthetic experiment evidence. Never export local keys or raw phone evidence."""
import argparse
import csv
import hashlib
import json
import shutil
from pathlib import Path

from prepare_quality import SCORE_FIELDS


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def export(run, cases_path, out):
    summary, plan = read(run / "run-summary.json"), read(run / "run-plan.json")
    if summary.get("cases") != 100 or summary.get("state") != "GENERATED_AWAITING_REVIEW" or summary.get("real_interface_calls") is not True:
        raise ValueError("Only a finished 100-pair real generation run may be exported")
    if plan.get("architecture") != "DUAL_SKILL_V1" or plan.get("provider_bill_verified") is not False:
        raise ValueError("Architecture or unverified-billing disclosure mismatch")
    if digest(cases_path) != plan["cases_sha256"]:
        raise ValueError("Benchmark changed")
    source = {c["id"]: c for c in (json.loads(line) for line in cases_path.read_text(encoding="utf-8").splitlines() if line.strip())}
    mapping = read(run / "blind-mapping.json")["rows"]
    charges = read(run / "quality-ledger.json")["rows"]
    calls = {c["id"]: c for c in charges if c["id"] != "known-previous-estimate"}
    if len(source) != 100 or len(mapping) != 100 or {m["id"] for m in mapping} != set(source):
        raise ValueError("Exactly the original 100 distinct cases are required")
    if any(c["state"] in {"RESERVED", "UNCERTAIN"} for c in calls.values()):
        raise ValueError("Unsettled calls cannot be declared completed")
    counts = {s: dict(generated=0, failed=0, no_reply=0, candidates=0, format_repairs=0, calls=0, estimate_micro_rmb=0) for s in ["ordinary", "full"]}
    checked = []; all_call_ids = set(); actual_models = set()
    for m in sorted(mapping, key=lambda x: x["id"]):
        case_id = m["id"]; case = source[case_id]
        if case["provenance"] != "SYNTHETIC_NOT_REAL_WECHAT": raise ValueError("No real chats may be exported")
        grading = run / "grading" / (case_id + ".json"); pair = read(grading)
        if digest(grading) != m["grading_sha256"] or pair["input_sha256"] != m["input_sha256"]:
            raise ValueError("Blind output integrity mismatch")
        if pair["conversation"] != case["conversation"] or pair["query"] != case["query"] or pair["rubric"] != case["rubric"]:
            raise ValueError("Blind input changed or omitted history")
        for scheme, hash_field in [("ordinary", "input_sha256"), ("full", "case_input_sha256")]:
            path = run / (case_id + "-" + scheme + "-audit.json"); audit = read(path)
            if audit[hash_field] != m["input_sha256"] or audit.get("real_interface_calls") is not True:
                raise ValueError("Missing matching paid-call audit")
            used = audit["call_ids"]
            if not used or not set(used).issubset(calls) or all_call_ids.intersection(used):
                raise ValueError("Missing or duplicate call ownership")
            all_call_ids.update(used)
            stats = counts[scheme]; stats["calls"] += len(used)
            stats["estimate_micro_rmb"] += sum(calls[i]["charged"] for i in used)
            stats["format_repairs"] += sum("格式修复1/1" in calls[i]["component"] for i in used)
            variant = pair[m[scheme]]
            if audit["generation_status"] != variant["generation_status"]: raise ValueError("Generation status mismatch")
            if audit["generation_status"] == "FAILED":
                if audit.get("result") is not None or variant["no_reply"] or variant["candidates"]:
                    raise ValueError("Failure disguised as success")
                stats["failed"] += 1
            else:
                result = audit["result"]; stats["generated"] += 1; stats["no_reply"] += int(result["no_reply"])
                stats["candidates"] += len(result["candidates"]); actual_models.add(audit["model"])
                expected_candidates = [{"style": c["style"], "segments": c["segments"]} for c in result["candidates"]]
                if variant["candidates"] != expected_candidates or variant["no_reply"] != result["no_reply"]:
                    raise ValueError("Blind candidate changed")
                allowed = {message["id"] for message in case["conversation"]["messages"]}
                for fact in result["analysis"]["facts"]:
                    if not fact["evidence_ids"] or not set(fact["evidence_ids"]).issubset(allowed):
                        raise ValueError("Uncited or foreign fact adopted")
                if scheme == "full":
                    components = {c["name"]: c["state"] for c in audit["components"]}
                    if components.get("Jev") != "USER_DISABLED" or any(components.get(n) != "SUCCEEDED" for n in ["狗头军师", "恋爱大师"]):
                        raise ValueError("Required Skill missing")
                    coverage = audit["coverage"]
                    if coverage["available"] != len(allowed) or coverage["processed"] != len(allowed) or set(coverage["sourceIds"]) != allowed:
                        raise ValueError("History coverage mismatch")
            checked.append(dict(id=case_id, scheme=scheme, generation_status=audit["generation_status"], sha256=digest(path), call_ids=used))
    if all_call_ids != set(calls) or summary["call_count"] != len(calls):
        raise ValueError("Unassigned or unrecorded paid call")
    out.mkdir(parents=True, exist_ok=True)
    (out / "grading").mkdir(exist_ok=True)
    for name in ["run-plan.json", "run-summary.json", "blind-mapping.json", "quality-ledger.json"]:
        shutil.copyfile(run / name, out / name)
    for m in mapping:
        shutil.copyfile(run / "grading" / (m["id"] + ".json"), out / "grading" / (m["id"] + ".json"))
        for scheme in ["ordinary", "full"]:
            name = m["id"] + "-" + scheme + "-audit.json"
            shutil.copyfile(run / name, out / name)
    with (out / "human-scores-blank.csv").open("w", encoding="utf-8-sig", newline="") as h:
        writer = csv.DictWriter(h, fieldnames=SCORE_FIELDS); writer.writeheader()
        for m in sorted(mapping, key=lambda x: x["id"]):
            for label in ["A", "B"]:
                writer.writerow(dict(case_id=m["id"], variant=label, input_sha256=m["input_sha256"], grading_sha256=m["grading_sha256"]))
    report = dict(state="REAL_GENERATION_VERIFIED_NOT_HUMAN_ACCEPTED", cases=100, schemes=counts, actual_models=sorted(actual_models),
                  real_calls=len(calls), estimate_micro_rmb=sum(c["charged"] for c in calls.values()), provider_bill_verified=False,
                  human_graded=0, formal_pass=None, architecture="DUAL_SKILL_V1", jev_calls=0,
                  cases_sha256=plan["cases_sha256"], output_protocol=plan["output_protocol"], audits=checked,
                  limits="All material and style samples are synthetic; coverage and valid IDs do not prove semantic fidelity or real host style.")
    (out / "experiment-verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in report.items() if k != "audits"}, ensure_ascii=False, indent=2))
    return report


if __name__ == "__main__":
    parser = argparse.ArgumentParser(); parser.add_argument("--run", type=Path, required=True)
    parser.add_argument("--cases", type=Path, default=Path("qa/dialogue-quality-v2.jsonl")); parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args(); export(args.run, args.cases, args.out)
