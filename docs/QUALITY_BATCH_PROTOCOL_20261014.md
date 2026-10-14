# 100组双Skill对照实验协议

**当前状态：用户已停止本批，转入App接入测试。** 保留52/100生成对、96个AI审阅输出，具名人工评分0、正式通过数未知。4笔在途未知费用维持UNCERTAIN并保留¥0.745250预留；不按零元清除、不恢复付费生成或继续评分。详见[停止核验](../reports/quality/20261014-stopped/stop-verification.json)。下文为原协议及工具用法，恢复需要用户另行授权；后续单轮手机授权不等于恢复本批。

用户于2026-10-14直接要求先修复两组失败，再完成100组双Skill与普通大模型对照。本轮使用DeepSeek Flash，狗头军师和恋爱大师均真实调用，Jev关闭。没有修改原PRD和Master；当前默认组件依据CR-20261014-05。

先使用原SMOKE-02和SMOKE-05输入回归，保留历次失败，不换内容。新增严格来源约束与最多一次单独计费格式修复；引用越界或拒绝边界错误不修复。原轮和修复均真实请求，未制造Mock候选。最终回归两组合法，SMOKE-02不回复，SMOKE-05在一次修复后得到五个候选。语义审阅曾发现Jev误归属，补充约束后重新回归；该早期问题仍记录。

原准备集有重复主题，因此保留`qa/cases.jsonl`，另建`qa/dialogue-quality-v2.jsonl`。新集十类各十组，至少80种不同最新文本；包含10组各1000条的长历史。每组主播风格、历史与媒体缺口均为虚构材料，不证明真实主播模仿效果。媒体未知场没有原件，不计算媒体识别准确率。输入SHA256：`d242de2fd690216106839e6346598b9d68a134e8afd9620ffec218b44b479646`。

同一组的两个方案接收相同完整历史、最新尾部消息目标和主播风格统计；不把预写rubric答案发给模型。普通方案一次生成，双Skill分别分析后再整合，修复额外计费。生成模型、窗口、输出上限、JSON模式相同，未切换思考模式来缩短其中一个方案。A/B来源预先随机并持久化。结构、引用、全部组件成功和历史覆盖由程序检查，语义质量另评。

新批最多4路独立用例并发，预留和结算使用同一台账，额外上限60元、不自动增加额度；已知之前本地估算¥1.534182同时计入月度控制。该数含手机及此前小样本和回归，不是已核对的服务商全账单。任何未知用量均阻断新调用。失败保留在原100组分母，不补题，不自动重新开始付费方案。

评分分两层：Codex逐组检查全部A/B候选，使用明确标识的AI审阅文件；主播通过离线匿名页面评判，使用独立空白人评CSV。五条件仍为可直接使用、不编造、利用关键历史、贴近本人风格、尊重边界；AI审阅中的风格仅比较虚构样本。没有用模型自评分或AI审阅冒充主播评分，正式≥95/100保持待人评。

运行与复现：复制`qa/batch-config-template.json`到私有目录，在本机配置接口及价格、授权范围和已有估算；Key只在运行时注入。Windows本用户DPAPI文件可由脚本解密到临时环境变量，退出清理。不要在命令、日志或版本控制中写Key。

```powershell
.\scripts\Run-QualityBatch.ps1 -Config qa/private/quality/config.local.json -Action check -DpapiKeyPath <本机DPAPI文件>
.\scripts\Run-QualityBatch.ps1 -Config qa/private/quality/config.local.json -Output qa/private/quality/run -Action run -DpapiKeyPath <本机DPAPI文件>
# 仅在用户另行授权恢复、中断已查明、用量没有UNCERTAIN且输入/代码均未变时使用
.\scripts\Run-QualityBatch.ps1 -Config qa/private/quality/config.local.json -Output qa/private/quality/run -Action run -Resume -DpapiKeyPath <本机DPAPI文件>
```

最终公开报告只导出虚构材料的结构化结果、盲评文件、费用估算和哈希。私有Key、被拒原始正文及手机聊天证据不进入源码包。公开报告目录中的`blind-mapping.json`和`*-audit.json`会揭示方案，评分者应只打开`grading`文件和`qa/judge.html`。
