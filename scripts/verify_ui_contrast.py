"""Check text/icon contrast using the actual native theme tokens."""
import json
import re
from pathlib import Path
from datetime import date

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "product-app/src/main/java/cn/xindong/junshi/JunshiTheme.kt"
def luminance(hex_color):
    channels = [int(hex_color[i:i+2], 16) / 255 for i in (0, 2, 4)]
    linear = [x / 12.92 if x <= .04045 else ((x + .055) / 1.055) ** 2.4 for x in channels]
    return sum(x * w for x, w in zip(linear, (.2126, .7152, .0722)))
def ratio(a, b):
    low, high = sorted((luminance(a), luminance(b)))
    return (high + .05) / (low + .05)

def main():
    source = SOURCE.read_text(encoding="utf-8-sig")
    checks = []
    for label, block in re.findall(r"private val (Light|Dark)Palette=(?:light|dark)ColorScheme\((.*?)\n\)", source, re.S):
        tokens = dict(re.findall(r"(\w+)=Color\(0xFF([A-Fa-f0-9]{6})\)", block))
        tokens.update({name: "FFFFFF" for name in re.findall(r"(\w+)=Color.White", block)})
        for fg, bg in [
            ("onPrimary", "primary"), ("onPrimaryContainer", "primaryContainer"),
            ("onSecondary", "secondary"), ("onSecondaryContainer", "secondaryContainer"),
            ("onBackground", "background"), ("onSurface", "surface"),
            ("onSurfaceVariant", "surfaceVariant"), ("onError", "error"),
            ("onErrorContainer", "errorContainer"), ("primary", "surface"),
            ("primary", "surfaceVariant"), ("onSurfaceVariant", "background"),
            ("onSurfaceVariant", "surface"), ("onSurface", "surfaceContainerHigh"),
            ("onSurfaceVariant", "surfaceContainerHighest")
        ]:
            result = ratio(tokens[fg], tokens[bg])
            checks.append(dict(mode=label, foreground=fg, background=bg,
                               ratio=round(result, 3), minimum=4.5, passed=result >= 4.5))
        result = ratio(tokens["outline"], tokens["surface"])
        checks.append(dict(mode=label, foreground="outline", background="surface",
                           ratio=round(result, 3), minimum=3, passed=result >= 3))
    assert len(checks) == 32, "Both actual theme palettes must be parsed"
    report = dict(source=str(SOURCE.relative_to(ROOT)), checks=checks,
                  note="Enabled semantic text and outline pairs; disabled controls, raster edges and SystemUI are outside this token check.")
    output = ROOT / "reports/ui-redesign" / date.today().strftime("%Y%m%d") / "contrast.json"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    failed = [check for check in checks if not check["passed"]]
    print("Native theme contrast: %d/%d pairs passed." % (len(checks)-len(failed), len(checks)))
    if failed:
        print(json.dumps(failed, indent=2))
        raise SystemExit(1)
if __name__ == "__main__":
    main()

