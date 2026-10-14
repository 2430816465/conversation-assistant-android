# 0.3.4-development 双Skill默认配置交付

2026-10-14。按用户直接授权，当前默认使用狗头军师、恋爱大师及生成模型，Jev保留为可选、默认关闭，后续配置独立接口后可开启。两份原冻结文档字节保留，变更见[CR-05](CHANGE_REQUEST_20261012.md)。两个Skill仍实际调用生成模型，关闭Jev不等于没有DeepSeek费用。

升级仅迁移旧版未配有效Jev凭证却默认开启的设置一次；保留模型/密钥/历史/风格/Skill选择，云端范围要求重新确认。已配置的Jev启用选择、之后手动开启选择保留；开启但无有效配置仍明确失败，不能静默跳过。后续添加Jev不用重建主播或聊天档案。

80项核心、7项Android本地、40项Python＝127项工程检查通过，qa-runner编译、APK签名及权限检查通过，lint0错误1警告2提示。补充无事实引用等最终校验的明确错误文案，仍拒绝不合格结果，不自动重试。研发Debug签名，Android11+、target35、versionCode6。包体和当前SHA256以reports/product/artifact-verification.json及deliverables/SHA256SUMS.txt为准。

真实DeepSeek小样本6场均尝试，4场合法结果共17候选；明确拒绝场JSON失败、未知语音场事实引用失败，未伪造通过。18次usage估算¥0.666050，非已核对账单；正式100组对照及人评仍0/100。详见[逐项结果与失败](DUAL_SKILL_TEST_20261014.md)、[Jev费用估算](JEV_COST_ESTIMATE_20261014.md)、[已知问题](KNOWN_ISSUES.md)。

本次无ADB设备，0.3.4未真机安装或测试；手机最后实测0.3.2。覆盖升级和迁移、权限/导航/稳定性须后续真机核验，不能用桌面结果代替。安装时不要卸载旧版，解锁后选主播/观众，在“模型”核对双Skill、Jev关闭及云端发送范围，再保存。操作见[安装与使用](PRODUCT_INSTALL.md)。

- [产品APK](../deliverables/xindong-junshi-debug.apk)
- [完整源码ZIP](../deliverables/xindong-junshi-source.zip)
- [SHA256](../deliverables/SHA256SUMS.txt)
- [工程测试](TEST_REPORT.md)、[阶段状态](STATUS.md)、[环境报告](ENVIRONMENT_REPORT.md)

当前可做研发试用，输出结构稳定性、OCR校对、真实口吻、真机稳定性及正式质量尚未验收，不是已完成最终验收的生产版。
