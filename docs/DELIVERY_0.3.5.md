# 0.3.5 研发交付

2026-10-14，当前版本0.3.5-development/versionCode7，Android11+，Debug签名。**App内真实生成及相关交互有限通过，尚未达到正式产品验收。** 冻结PRD及Master原文不改；用户直接批准的变更见[变更记录](CHANGE_REQUEST_20261012.md)。

本版修复严格结构输出失败的处理：每个组件最多一次额外计费格式修复，重新执行原结构与来源校验，不采用手工拼接的假响应；越界引用、明确拒绝边界错误和未知网络费用不按格式修复重试。保留原错误及响应哈希。另修复App首页版本号固定为0.3.2的问题，改为显示实际BuildConfig版本。

默认狗头军师和恋爱大师开启、Jev关闭，可后续独立配置Jev。旧无Jev凭证的默认开启配置迁移后需重新核对云端范围；已有明确配置保留。当前真实手机使用DeepSeek Flash，不证明任意模型兼容或含Jev效果。

| 交付 | 路径与状态 |
|---|---|
| 产品APK | [xindong-junshi-debug.apk](../deliverables/xindong-junshi-debug.apk)，73,114,125字节，约69.7MiB；已正常覆盖安装指定红米 |
| 源码 | [xindong-junshi-source.zip](../deliverables/xindong-junshi-source.zip)，固定依赖/Wrapper、源码、测试、许可、公开报告；排除密钥、真实聊天证据及构建缓存 |
| 校验清单 | [SHA256SUMS.txt](../deliverables/SHA256SUMS.txt)、[源码逐文件清单](../deliverables/source-files.json) |
| 安装与操作 | [PRODUCT_INSTALL.md](PRODUCT_INSTALL.md) |
| 真机App接入报告 | [APP_INTEGRATION_TEST_20261014.md](APP_INTEGRATION_TEST_20261014.md)及[脱敏核验JSON](../reports/app-integration/20261014/phone-verification.json) |
| 质量与问题 | [AI_QUALITY_TEST_REPORT.md](AI_QUALITY_TEST_REPORT.md)、[KNOWN_ISSUES.md](KNOWN_ISSUES.md)、[阶段状态](STATUS.md) |
| 独立G1探针 | 0.1.4 APK保留，包名不同，没有AI；旧后台失败不改写 |

产品APK SHA256：

```text
46f0377296c9e0148cb3f2d968c35ba69a88e24666a25c0fd11e4179fbe6e717
```

本版工程检查：87核心＋7Android本地＋44Python＝138项通过；最终APK的API35模拟器6项仪器测试通过；lint0错误1警告2提示，签名检查通过。它们不计入AI质量分数。

指定红米0.3.2→0.3.5正常升级，账户/6条历史/原件/旧报告保留。用户授权的一轮App真实生成执行5次请求，其中2次格式修复，最终4候选；编辑、复制、报告、证据回查、原件查看、SAF导出通过。新增估算¥0.171421，未核对服务商账单；临时云端授权结束恢复关闭，双Skill开启、Jev关闭。

原SMOKE-02/05相同输入最终回归取得合法结果，详见[实验协议](QUALITY_BATCH_PROTOCOL_20261014.md)和回归记录；不是所有JSON或语义问题已消失。用户已停止100组批次：52/100生成对、96个AI审阅输出、具名人工评分0、正式通过数未知。4笔未知费用保留¥0.745250预留；本次App授权没有恢复100组。

微信前台读取本轮未测，系统无障碍当前关闭；全部微信历史未知；主播真实已发样本0，严格口吻未验收。候选仍由主播选择、编辑、复制并自行发送，本轮没有微信输入或发送。Jev、稳定性及≥95/100均保持未验收。
