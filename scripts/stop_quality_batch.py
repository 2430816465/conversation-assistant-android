# -*- coding: utf-8 -*-
"""Record an explicit user stop AFTER the request workers have been terminated."""
import argparse
import hashlib
import json
import shutil
import time
from pathlib import Path


def read(path): return json.loads(path.read_text(encoding="utf-8"))
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def freeze(root, out):
    run = root / "run"
    marker = root / "user-stop.json"
    if marker.exists():
        raise ValueError("Stop already recorded; preserve the original")
    ledger_path = run / "quality-ledger.json"
    summary_path = run / "run-summary.json"
    ledger, summary = read(ledger_path), read(summary_path)
    snapshot = root / "before-user-stop"
    snapshot.mkdir(exist_ok=False)
    for name in ["quality-ledger.json", "run-summary.json"]:
        shutil.copyfile(run / name, snapshot / name)
    interrupted = []
    for row in ledger["rows"]:
        if row["state"] == "RESERVED":
            interrupted.append(dict(id=row["id"], owner=row["owner"], component=row["component"],
                                    reservation_micro_rmb=row["reserved"]))
            row["state"] = "UNCERTAIN"
    # Preserve conservative reserved amounts. No invented usage or zero-fee reconciliation.
    temporary = ledger_path.with_suffix(".stop.tmp")
    temporary.write_text(json.dumps(ledger, ensure_ascii=False, indent=2), encoding="utf-8")
    temporary.replace(ledger_path)
    settled = [r for r in ledger["rows"] if r["state"] == "TOKEN_ESTIMATE"]
    unknown = [r for r in ledger["rows"] if r["state"] == "UNCERTAIN"]
    previous = sum(r["charged"] for r in ledger["rows"] if r["state"] == "KNOWN_EXTERNAL_ESTIMATE")
    grading = sorted((run / "grading").glob("*.json"))
    reviews_path = root / "ai-review-decisions.json"
    reviews = read(reviews_path)["rows"] if reviews_path.exists() else []
    now = int(time.time() * 1000)
    report = dict(state="USER_STOPPED_NOT_COMPLETED", requested=100, generated_pairs=len(grading),
                  ai_reviewed_outputs=len(reviews), human_graded=0, formal_pass=None,
                  settled_request_records=len(settled), settled_estimate_micro_rmb=sum(r["charged"] for r in settled),
                  uncertain_records=len(unknown), uncertain_reservations_micro_rmb=sum(r["reserved"] for r in unknown),
                  known_previous_estimate_micro_rmb=previous, provider_bill_verified=False,
                  stopped_at=now, reason="User stopped dialogue quality testing and switched to App integration testing.",
                  interrupted=interrupted, cases_sha256=summary["cases_sha256"], settings_sha256=summary["settings_sha256"],
                  grading=[dict(file=p.name, sha256=digest(p)) for p in grading],
                  ledger_sha256=digest(ledger_path),
                  note="In-flight usage is unknown, not free; no paid batch resume. Completed results and previous review decisions are retained.")
    summary.update(state="USER_STOPPED", updated_at=now, call_count=len(settled)+len(unknown),
                   settled_usage_estimate_micro_rmb=report["settled_estimate_micro_rmb"],
                   unresolved_reservation_micro_rmb=report["uncertain_reservations_micro_rmb"],
                   usage_estimate_micro_rmb=report["settled_estimate_micro_rmb"]+report["uncertain_reservations_micro_rmb"],
                   known_total_estimate_micro_rmb=previous+report["settled_estimate_micro_rmb"]+report["uncertain_reservations_micro_rmb"],
                   error="Explicit user stop; interrupted reservations are UNCERTAIN pending provider evidence.")
    summary_path.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    marker.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    out.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(marker, out / "stop-verification.json")
    shutil.copyfile(summary_path, out / "run-summary.json")
    print(json.dumps({k:v for k,v in report.items() if k not in ["grading", "interrupted"]},ensure_ascii=False,indent=2))


if __name__ == "__main__":
    p=argparse.ArgumentParser();p.add_argument("--root",type=Path,required=True);p.add_argument("--output",type=Path,required=True)
    args=p.parse_args();freeze(args.root,args.output)
