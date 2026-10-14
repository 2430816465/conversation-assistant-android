"""Export only synthetic own-app screenshots from an explicitly verified emulator."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--serial", required=True)
    parser.add_argument("--label", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    assert re.fullmatch(r"emulator-\d+", args.serial), "Emulator only"
    assert re.fullmatch(r"[a-z0-9-]+", args.label), "Invalid synthetic capture label"
    adb = str(Path(os.environ["LOCALAPPDATA"]) / "Android/Sdk/platform-tools/adb.exe")
    prefix = [adb, "-s", args.serial]
    assert subprocess.check_output(prefix + ["shell", "getprop", "ro.kernel.qemu"]).strip() == b"1"
    output = Path(args.output).resolve()
    output.relative_to((ROOT / "reports/ui-redesign").resolve())
    output.mkdir(parents=True, exist_ok=True)
    device = "files/qa-ui/" + args.label
    observed = json.loads(subprocess.check_output(prefix + ["exec-out", "run-as", "cn.xindong.junshi", "cat", device + "/observations.json"]))
    expected_hash = hashlib.sha256((ROOT / "deliverables/xindong-junshi-debug.apk").read_bytes()).hexdigest()
    assert observed.get("completed") is True, "Visual run incomplete; old screenshots rejected"
    assert observed.get("apkSha256") == expected_hash, "Screenshots belong to another APK"
    names = subprocess.check_output(prefix + ["exec-out", "run-as", "cn.xindong.junshi", "ls", "-1", device]).decode("utf-8").splitlines()
    for name in names:
        assert re.fullmatch(r"[a-z0-9-]+\.(png|json)", name), "Unexpected sandbox file"
        data = subprocess.check_output(prefix + ["exec-out", "run-as", "cn.xindong.junshi", "cat", device + "/" + name])
        (output / name).write_bytes(data)
    print("Exported %d synthetic UI files." % len(names))
if __name__ == "__main__":
    main()

