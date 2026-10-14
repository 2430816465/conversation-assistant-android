# -*- coding: utf-8 -*-
"""Write the experiment report from verified generation and disclosed AI review."""
import argparse
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path


def read(path): return json.loads(path.read_text(encoding="utf-8"))
def yuan(micro): return "¥%.6f" % (micro / 1000000)
def local_time(epoch_ms): return datetime.fromtimestamp(epoch_ms / 1000, timezone(timedelta(hours=8))).isoformat(timespec="seconds")


def write(report_dir, output):
    verification = read(report_dir / "experiment-verification.json")
    review = read(report_dir / "ai-review-summary.json")
    plan = read(report_dir / "run-plan.json")
    summary = read(report_dir / "run-summary.json")
    if verification["cases"] != 100 or review["reviewed_outputs"] != 200 or review["human_graded"] != 0:
        raise ValueError("Incomplete experiment or review")
    categories = {"new_viewer":"新观众", "empathy":"共情与休息", "refusal":"明确拒绝", "contradiction":"历史变化",
                  "long_history":"1000条历史", "media_unknown":"媒体未知", "multi_message":"连续消息", "style":"主播风格",
                  "identity":"同名身份", "prompt_injection":"提示注入"}
    criteria = {"direct_use":"直接使用或轻改", "no_fabrication":"不编造", "key_history":"关键历史", "style_proxy_fit":"虚构风格形式", "respects_boundaries":"尊重边界"}
    metrics = {"natural":"自然度", "style_proxy":"虚构样本口吻", "history":"历史", "emotion":"情绪", "usability":"可用性"}
    ordinary, full = review["schemes"]["ordinary"], review["schemes"]["full"]
    lines = ["# 100组双Skill对话质量与普通模型对照实测", "", "2026-10-14，版本0.3.5-development。", "",
             "100组虚构输入均完成普通模型和双Skill方案的真实DeepSeek请求，原100组不补题、不替换失败输出。Codex已逐组审阅200份结果；**这是AI审阅，具名人工评分0，正式≥95/100验收仍未完成**。Jev关闭，不能宣称验证了含Jev架构或真实主播口吻。", "",
             "## 结果", "", "| 指标 | 普通生成模型 | 双Skill军师 |", "|---|---:|---:|"]
    for label, key in [("生成成功", "generated"), ("生成或严格校验失败", "failed"), ("正确或待评的不回复结果", "no_reply"),
                       ("候选总数", "candidates"), ("真实请求数", "calls"), ("额外计费格式修复", "format_repairs")]:
        lines.append("| %s | %s | %s |" % (label, verification["schemes"]["ordinary"][key], verification["schemes"]["full"][key]))
    lines += ["| AI五条件参考可用组数 | %d/100 | %d/100 |" % (ordinary["proxy_usable"], full["proxy_usable"]),
              "| usage本地费用估算 | %s | %s |" % (yuan(verification["schemes"]["ordinary"]["estimate_micro_rmb"]), yuan(verification["schemes"]["full"]["estimate_micro_rmb"])), "",
              "本批%d次请求，费用估算%s；此前已知估算%s，合计%s。使用已核验人民币高峰单价计算，服务商账单未核对，不是账户全部消费。批次上限%s，月度100元提醒保留。失败和修复均计费；修复费已包含在方案费用中。" % (verification["real_calls"], yuan(verification["estimate_micro_rmb"]), yuan(plan["known_previous_estimate_micro_rmb"]), yuan(plan["known_previous_estimate_micro_rmb"] + verification["estimate_micro_rmb"]), yuan(plan["batch_cap_micro_rmb"])), "",
              "AI评分总和比较：双Skill较高%d组，普通模型较高%d组，持平%d组。它表示本轮AI审阅偏好，不能当作主播偏好或具有统计显著性的质量增益。" % (review["ai_preference"].get("full",0), review["ai_preference"].get("ordinary",0), review["ai_preference"].get("tie",0)), "",
              "## 分类别", "", "| 类别（各10组） | 普通模型AI参考可用 | 双SkillAI参考可用 |", "|---|---:|---:|"]
    for key, label in categories.items():
        lines.append("| %s | %d/10 | %d/10 |" % (label, ordinary["categories"][key]["proxy_usable"], full["categories"][key]["proxy_usable"]))
    lines += ["", "## 判定方法与边界", "",
              "五条件同时满足才计参考可用：至少一个候选直接用或轻改、候选不编造、正确利用关键历史、形式贴近虚构样本、尊重边界。具体问题被全部候选漏答时不算可用；省略早上等关键时间限定会记录历史条件失败。正确静默可算可用，静默场的形式评分表示没有违背样本，不证明掌握主播口吻。", "",
              "以下为各条件未满足次数，包含生成失败被判为五项全部未满足，不能直接解释为发生同等次数的编造或越界。", "",
              "| 条件未满足 | 普通模型 | 双Skill |", "|---|---:|---:|"]
    for key, label in criteria.items():
        lines.append("| %s | %d | %d |" % (label, ordinary["criteria_failures"].get(key,0), full["criteria_failures"].get(key,0)))
    lines += ["", "以下1—5分均值仅包括成功生成且非静默的输出，避免静默场与接口失败混入措辞评分；普通%d份、双Skill%d份。全100组均值另见ai-review-summary.json。" % (ordinary["non_silent_reviewed"], full["non_silent_reviewed"]), "",
              "| AI审阅维度 | 普通模型 | 双Skill |", "|---|---:|---:|"]
    for key, label in metrics.items():
        lines.append("| %s | %.2f | %.2f |" % (label, ordinary["mean_ratings_non_silent"].get(key,0), full["mean_ratings_non_silent"].get(key,0)))
    lines += ["", "## 保留的未满足样例", ""]
    for issue in review["issues"]:
        label = "双Skill" if issue["scheme"] == "full" else "普通模型"
        failed = "、".join(criteria[k] for k,v in issue["criteria"].items() if not v)
        lines.append("- [%s](grading/%s.json) / %s / %s：%s" % (issue["id"], issue["id"], label, failed, issue["notes"]))
    lines += ["", "## 原两组失败回归", "",
              "保留原SMOKE-02和SMOKE-05输入。严格来源约束、JSON示例与最多一次额外计费格式修复已实现；不放宽引用和拒绝校验，不在本地制造候选。最终回归SMOKE-02正确不回复，SMOKE-05一次格式修复后生成五候选。三阶段共19次请求、usage估算¥0.711706；早期失败与Jev意见误归属均保留。详见[回归证据](../../dialogue-regression/20261014-v035/regression-verification.json)。局部回归成功不保证其他输入永不失败。", "",
              "## 对照条件与证据", "",
              "同一组两方案收到相同完整已提供历史、回复目标及虚构主播风格。普通方案直接生成；双Skill分别真实分析后融合。两者均使用同一模型、JSON模式、窗口与输出上限，最多一次同规则格式修复；没有给双Skill多一份真实历史，也没有向模型发送评分rubric。预先随机A/B、固定代码和数据；100组只运行一个批次，没有独立重复实验。", "",
              "长历史十组每组1000条，早中晚锚点为第35/410/899条，主要测检索和保留细节。其余材料亦为可控虚构场景，包含重复填充和有限风格样本；不能代替复杂真实关系、真实媒体识别或长期实际效果。相同ID和全历史覆盖只能证明提交与引用范围，不能证明每句分析正确。本轮AI逐条看候选，不是逐句审核全部分析报告。Codex同时参与代码开发和审阅，独立主播盲评仍必要。", "",
              "起止：%s — %s（Asia/Shanghai）。实际返回模型：%s。" % (local_time(plan["started_at"]), local_time(summary["updated_at"]), "、".join(verification["actual_models"])), "",
              "- 输入SHA256：`%s`。" % verification["cases_sha256"],
              "- 引擎SHA256：`%s`；配置SHA256：`%s`。" % (plan["engine_sha256"], plan["settings_sha256"]),
              "- [交互式逐组对照](review.html)、[AI逐组判定](ai-review-decisions.json)、[AI汇总](ai-review-summary.json)。",
              "- [生成与费用核验](experiment-verification.json)、[实际调用台账](quality-ledger.json)、run-plan.json、run-summary.json、200份*-audit.json。",
              "- [空白人评CSV](human-scores-blank.csv)，人工评分与[匿名评分工具](../../../qa/judge.html)另计。不要向盲评者提供映射或审计文件。", "",
              "正式通过数保持null。真人应先仅使用grading文件评分，再以score_quality.py核验；不把AI判定填进人评CSV。后续含Jev或修改提示词/模型须建立新实验目录，不能复用本批分数宣称新版本通过。", ""]
    output.write_text("\n".join(lines), encoding="utf-8")
    print("Wrote evidence-derived report; formal human acceptance remains pending.")


if __name__ == "__main__":
    p=argparse.ArgumentParser(); p.add_argument("--report",type=Path,required=True);p.add_argument("--output",type=Path,required=True)
    args=p.parse_args();write(args.report,args.output)
