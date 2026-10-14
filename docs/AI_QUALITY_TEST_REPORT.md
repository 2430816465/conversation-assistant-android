# AI质量、100组与匿名对照

2026-10-14当前生效策略：默认双Skill开启、Jev可选且默认关闭。0.3.5通过87核心、7Android本地、44Python工程检查及最终版本6项模拟器仪器测试。它们不属于AI质量评分。

**100组批次已按用户要求停止，状态USER_STOPPED_NOT_COMPLETED。** 保留52/100普通模型与双Skill生成对记录（包含失败结果），已有96个AI审阅输出（前48对）；后4对未继续审阅。具名人工评分0，正式通过数未知，≥95/100未验收。没有恢复生成或评分，没有将未完成项补造为成功。详见[停止核验](../reports/quality/20261014-stopped/stop-verification.json)及[本批协议](QUALITY_BATCH_PROTOCOL_20261014.md)。

原SMOKE-02/05失败输入保持不变，最终回归分别得到正确不回复及5条候选；严格来源规则、最多一次单独计费格式修复及原校验均保留。中途失败与Jev误归属问题没有删除，见[回归记录](../reports/dialogue-regression/20261014-v035/regression-verification.json)。有限回归不能宣布所有结构和语义问题已解决。

随后用户只授权一轮手机App接入测试：原6条文字、Jev关闭，双Skill成功及4候选，5次请求含2次格式修复；新增估算¥0.171421。原文“聊”没有被候选改成“见”，但主播已发样本0、严格口吻仍不可验证。详见[App接入报告](APP_INTEGRATION_TEST_20261014.md)。这轮不是100组补测，也没有人工质量分数。

费用：已停止批次229笔已知usage估算¥15.606016；4笔未知用量预留¥0.745250保持UNCERTAIN。加此前估算及本轮手机新增，累计已知估算¥17.311619，含预留¥18.056869；尚未核对服务商账单，不代表账户全部消费。手机和桌面账本没有自动合并。后续恢复批次需用户另行授权，未知费用须保留预留或按账单核账。

AI审阅发现部分普通模型候选遗漏历史细节、部分双Skill候选仍遗漏关键时间或格式失败。这是AI审阅而非具名人工评分，不计算正式通过率或宣称双Skill胜出。合成主播风格不能证明真实主播模仿效果。后续含Jev架构须另建目录、另行测试，不能继承双Skill结果。

2026-10-13晚间0.3.3历史记录：配置核查和既有P05B四候选工程复核已完成，当时新6组真实生成待临时Jev例外授权且未执行。详见CONFIG_AND_DIALOGUE_CHECK_20261013.md；该等待状态现由CR-05覆盖。

以下两段仅为2026-10-13手机历史快照，不能当作当前累计状态；早期桌面6组小样本另见DUAL_SKILL_TEST_20261014.md。

当时100组状态为PREPARED_NOT_EXECUTED、生成0/100、人评0/100。手机当时真实推理4次：P05失败1次，0.3.2 P05B双Skill及最终生成3次成功、4候选；结束恢复当时Jev默认值。该历史快照现已由本页顶部当前结果覆盖，失败与原授权条件仍保留。

P05B确认了回复目标和6/6已提供历史覆盖，但第三候选将“明晚聊”写成“明晚见”，且主播已发样本0，语义风险及本人风格未验收。累计usage估算¥0.156426，非已核对账单。详细真实条件及私有证据哈希见PRODUCT_REAL_DEVICE_REPORT_20261013.md和p05b-inference-verification.json。

## 已准备

原qa/cases.jsonl及manifest.json保留为历史准备集。本批使用qa/dialogue-quality-v2.jsonl及manifest-v2.json：100组明确标识的合成输入、10类各10组，增加主题变化及长历史锚点；SHA256为d242de2fd690216106839e6346598b9d68a134e8afd9620ffec218b44b479646。所有人物和主播示例均虚构，200行人工评分模板不预填分数。

长历史样本含第35/410/899条的生日/咖啡过敏/体检安排；核心测试只证明全部原文提交和锚点未被截断，不证明真实模型正确理解。媒体未知样本检验不臆造，不能代替真实图片/语音识别准确率。

qa-runner复用产品AnalysisEngine，真实HTTPS生成普通模型和明确选择的军师架构；按同一历史/问题/主播风格输入随机匿名A/B。当前DUAL_SKILL_V1要求两个Skill成功及Jev明确关闭；后续JEV_DUAL_SKILL_V1要求三个组件全部成功。计划、摘要和评分报告记录架构；架构变更或旧实验协议禁止恢复。qa/judge.html仅读取grading文件供人工评分，不访问网络、不揭示映射、不自动打分。

0.3.0按CR-20261013-04增加明确reply_target：两组均接收同一批尾部已确认对方消息ID/版本/正文及全部历史，完整组额外执行默认组件；不是只给完整组补充最新消息。实验计划标记LATEST_REPLY_TARGET_V1，旧流程目录禁止恢复，须新建私有实验目录以免复用没有目标的旧结果。100组合成输入均以已确认对方消息结束，工程校验不等于真实读屏自动识别或人工质量评分。

评分器校验100个唯一真实生成对、所选架构的必需组件SUCCEEDED及Jev状态一致、匹配输入/候选文件SHA、200行具名人工A/B评分；空白、夹具、缺组件、混用不同实验分数均拒绝。文件/哈希只能审计一致性，不能自行证明评分者身份或服务商返回真实性，仍须可靠的实际执行记录。

## 合格规则（保持冻结）

每组五项同时为1才算可用：至少一条直接用或轻微修改（明确拒绝时正确不回复可用）；不编造/混身份；正确利用关键历史；贴近主播真实格式；尊重边界。完整架构至少95/100；普通模型同样统计。

另按自然度、口吻、历史、情绪、可用性各1—5人工评分，计算偏好及平均分。偏好不强制完整架构获胜；不得把模型自评分当人工评分，不得由组件更多推断质量更好。

合成测试证明不了每位真实主播的严格风格；正式推广前还应使用经授权真实主播历史复核，不改变冻结的100组门槛。

## 本机配置及执行

当前批次已停止，以下是工具复现说明，不构成恢复付费调用的授权。新批使用Run-QualityBatch.ps1及QUALITY_BATCH_PROTOCOL_20261014.md；下列Run-Quality.ps1为原串行工具，不能拿旧数据或协议目录恢复当前批次。

不要把密钥发到聊天。qa/private/已忽略，模板没有可用Key和单价。复制qa/config-template.json到qa/private/config.local.json，填写：真实端点/模型/Key、经核验上下文与输出容量、人民币输入/输出/缓存价和来源；确认cloudConsent/componentNoticeAccepted，当前模板采用DUAL_SKILL_V1：双Skill开启、Jev关闭；后续含Jev使用qa/config-jev-template.json及独立目录，设置JEV_DUAL_SKILL_V1并保留三组件开启。

billingReviewed仅在核对供应商本月已有费用后设true。previousMonthMicroRmb这个历史字段表示本月已花微元，1元=1000000微元，只在首次创建桌面账本时计入；跨月及其他设备后续费用仍须核对。模板的窗口、model别名和端点只是配置示例，不代表供应商当前规格已被实测。

先运行1组校验实际接口及账单，再显式扩展到100组。没有自动批准无限费用：

```powershell
.\scripts\Run-Quality.ps1 -Action status
.\scripts\Run-Quality.ps1 -Limit 1 -Output qa/private/run-reviewed
.\scripts\Run-Quality.ps1 -Limit 100 -Resume -Output qa/private/run-reviewed
```

Resume要求同一输入文件和公开模型配置，保留预先随机标签并复用完整且哈希匹配的已完成结果，避免重付已成功调用。中断请求费用未知则先查供应商账单；仅UNCERTAIN可核账。两个显式操作：

```powershell
.\scripts\Run-Quality.ps1 -Action reconcile -ChargeId <待核账ID> -ActualYuan <实际金额>
.\scripts\Run-Quality.ps1 -Action confirm-budget
```

confirm-budget明确增加本月100元提醒额度，不会改变历史/组件。若完整100组实际成本超预期，可以暂停并先报告费用，不能静默降级或伪造其余结果。

人工评审：浏览器本地打开qa/judge.html，选择同一run的grading目录全部JSON；先查看/搜索完整历史，再逐项评A/B并填写评分人，保存各组，及时导出CSV及备份JSON。刷新不自动保存。不要让评分人查看blind-mapping.json或full-audit，避免泄盲。将CSV存入同一私有run目录后执行：

```powershell
python scripts/score_quality.py --run qa/private/run-reviewed --scores qa/private/run-reviewed/human-scores.csv --output qa/private/run-reviewed/quality-report.json
```

双Skill的100组至少涉及普通生成100次、军师生成300次请求；后续含Jev还需至少100次Jev请求。长历史分块/归并另计。0.3.5仅对允许的结构格式错误最多一次额外计费修复，并重新严格校验；越界引用、明确拒绝边界错误和网络用量未知不按格式修复重试。明确的HTTP429推理前限流最多退避两次。费用取决于真实token、价格及缓存；不得为满足30—100元目标删除历史或绕过提醒。

## 实际验证与未执行

本地工具实际检查：Run-Quality.ps1 的帮助、无账本状态、未核账拒绝，以及模型/Key缺失拒绝均已执行；未产生供应商调用。修复 Windows 批处理分隔符传递、中文编码及旧 JAVA_HOME 选择问题。证据：reports/product/runner-smoke.json、runner-keyless-smoke.json及日志。

qa/judge.html历史工具验证已在Chromium151执行载入、长历史搜索、空分拒绝、评分/CSV/SHA往返、备份恢复/冲突拒绝及两种布局；证据为judge-browser.json等。全部是工具夹具，自动填分不计入真实人工验收。本次新批专用完整100组审阅页面未生成或浏览器验证，不能把旧页面验证移作新批完成证明。

已执行核心结构/引用/全历史/默认组件失败阻断及本机HTTP契约；Python16项质量审计和28项证据规则，共44项通过。它们使用明确夹具，不产生质量分数。最终版本模拟器6项通过，没有模型Key或伪造候选。

已执行的真实供应商验证包括历史P05/P05B、6组诊断、两组修复回归、52对普通模型/双Skill输出及本轮App单轮。失败、成功及付费格式修复分别记录，不因结果失败认定免费。

未完成：当前双Skill完整100组、具名人工评分、≥95/100、真实主播风格/媒体准确率、服务商账单及质量增益。真实Jev和后续含Jev100组未执行。没有将部分生成或模型自评分当作验收通过。
