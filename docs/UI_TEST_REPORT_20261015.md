# 0.3.7 视觉改版与交互回归报告

2026-10-15，Asia/Shanghai。执行依据为 CR-20261015-07；被测产品 **0.3.7-preview / versionCode 9**。界面完成改版与工程检查，不将这些结果解释为用户已满意或正式产品验收通过。

## 环境与证据绑定

Windows / PowerShell，沿用 JDK17、SDK36、BuildTools35、Gradle8.13 和现有依赖。显式核验 emulator-5580 为 qemu=1、Android15/API35 AOSP；本轮 adb devices 只有模拟器，无实体红米。无窗口启动已有 AVD，未修改全局 Java 或 BIOS。

最终 APK 73,929,517 字节，SHA256 `3d6d5303152d18b3ea74c9bb4ad47f6bd950f783e8adbbcae6d07916fbab2693`。已安装 base.apk 哈希一致。VisualUiTest 的 observations.json 记录版本、实际 APK 哈希、各图尺寸与时间，以及 completed 标志；导出脚本仅允许显式模拟器且要求 completed=true、哈希等于交付包，失败运行不能导出为成功证据。

所有界面材料均为测试 APK 创建的合成账户、文字及候选。SYNTHETIC_UI_ONLY 保存报告仅用于检查真实候选控件，未调用服务商、未产生费用、未操作微信发送，不在产品中提供假模型回退。

## 工程结果

| 层级 | 实际结果 | 原始证据 |
|---|---|---|
| 冻结基线 | 原 PRD/Master SHA256 及 190 行非空源文索引一致 | verify_baseline.py |
| 上游内容及许可 | 11 项锁定文件逐字节一致 | verify_product_sources.py |
| 核心 JUnit | 87 通过，0 失败/错误/跳过 | reports/product/kotlin/ |
| Android 本地 | 7 通过，0 失败/错误/跳过 | reports/product/android-unit/ |
| Python | 44 通过 | reports/ui-redesign/20261015/python-tests.txt |
| 最终 APK 工作流仪器回归 | 13 通过，Time 68.902s | reports/ui-redesign/20261015/all-instrumentation.txt |
| 实际颜色对比度 | 32/32 通过，规定文字≥4.5、轮廓≥3 | reports/ui-redesign/20261015/contrast.json |
| 构建/签名 | productVerification、qa-runner、apksigner 成功 | build-final.txt、apk-inspection.json |
| lint | 0 错误、3 警告、2 提示 | reports/product/lint/lint-results-debug.txt |

138 项 JVM/Python 检查与 13 项仪器测试分别计数；同一 VisualUiTest 在不同尺寸重复执行，不冒充新增功能用例。十三项用例覆盖注册/改密、导入校对与草稿旋转、最新消息核对/重放/换人清除、模型设置与删除确认、候选编辑复制与实际已发反馈、接口草稿/价格阻断、OCR选行不自动核对、Keystore/SQLite隔离重启、原件范围/删除、账户唯一性、实际 MediaPlayer 播放、实际 ML Kit 合成图 OCR，以及页面渲染/导航触控目标。OCR exact_match=false，仍只按管线验证，不计作准确率通过。

## 页面与布局复核

默认 1080×2400 / density420，以及下列五种配置均实际运行并保存截图。每次截取 14 个页面：登录、军师、最新消息、历史导入、批次校对、档案、我的、模型、接口、Key输入、预算、指南、候选和编辑面板。合计默认14张、矩阵70张，均有元数据；旧图不替代最终截图。

| 配置 | 像素 / density | 逻辑尺寸 | 字体/夜间 |
|---|---|---|---|
| compact-light | 750×1624 / 320 | 375×812dp | 1.0 / 关 |
| compact-dark | 750×1624 / 320 | 375×812dp | 1.0 / 开 |
| large-text-dark | 750×1624 / 320 | 375×812dp | 2.0 / 开 |
| landscape-light | 1624×750 / 320 | 812×375dp | 1.0 / 关 |
| tablet-light | 1600×2560 / 320 | 800×1280dp | 1.0 / 关 |

最终矩阵 **5/5 通过**，见 reports/ui-redesign/20261015/matrix-014547/matrix.json 与 matrix-final-console.txt；默认最终复截 **1/1 通过，17.646秒**，日志为 default-render-final.txt。脚本恢复原尺寸、密度、字号和动画设置。早期矩阵 matrix-014308 也通过，作为过程记录保留，不重复累加成功数。

人工图像复核采用抽查：默认登录、军师、档案、我的、候选与编辑面板，以及小屏编辑、深色我的、两倍字体最新消息、横屏接口和平板档案，文字及主要操作可读。长页在视口外的内容需滚动，测试同时核验关键控件可到达。最终候选整卡可见，稳定编辑面板不再有整个背景文字透出。未宣称全部截图逐像素比较或用户视觉验收通过。

## 暴露与修正

第一次工作流在退出账户时，旧联系人卡重绘仍访问已清空账户，产生空指针崩溃。账户卡和我的页改为本次组合的可空快照，锁定后安全退出组合。第二次暴露页面状态 key 重复：状态条出现/消失改变 LazyColumn 项目位置，导致 SaveableStateProvider 重复注册。为 context/status/content/footer 设置稳定 key，固定保留状态项，并在延迟项目回调前快照工作区状态键。第三次完整回归 13/13 通过；未删除业务断言。

修正前原始日志保存为 all-instrumentation-first-failure.txt、all-instrumentation-second-failure.txt；前序构建日志另存 build-before-lock-fix.txt、build-before-lazy-key-fix.txt。浅色辅助文字首次有一组对比度不足，调整为 #706259 后实际32组通过。

默认编辑面板早期截图截在 Android Dialog 打开动画中，背景透出。矩阵关闭动画后不复现；最终截图增加窗口空闲与短暂合成等待，复核实际稳定画面，不修改产品透明度来掩盖截帧问题。候选截图改为先滚动到真实编辑按钮，方便观察完整卡片；更短的合成账户名改善展示，业务断言不变。

剩余 lint 为 targetSdk35 版本警告、两项可选 modifier 参数顺序建议、两项 Long 状态装箱提示；本轮未提高 targetSdk 或引入生产依赖。

## 未执行与结论边界

本轮无真实模型请求、无新增 API 费用。已停止质量批次账本 SHA256 保持 `f37ee66292ff0fe1128c549fa6cefcffb1bc8685ba421edd731bd0ad31e943b6`；52 对记录含失败、96 个 AI 审阅输出、人评0、正式通过数未知，未恢复批次或宣称双Skill优于普通模型。

未执行新版红米覆盖升级、微信读取、撤权恢复、锁屏、长期耗电/ANR、真实主播风格或云端调用。旧 G1 后台无通知失败和旧版手机有限结果保留。没有无条件自动识别全部最新消息、微信全部历史权限、跨设备同步、密码找回、ZIP一键恢复或自动ASR。界面渲染、触控与模拟器流程通过，只支持本报告列明的工程结论。

复现：先 Build-Product.ps1，再 Test-ProductEmulator.ps1 -Serial emulator-5580；之后 Test-ProductUiMatrix.ps1 -Serial emulator-5580 -ReportDate 20261015，及 verify_ui_contrast.py / Inspect-ProductApk.ps1。两个 UI 脚本明确拒绝真机。
