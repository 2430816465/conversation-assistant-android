# 本机账号、功能交互与界面测试报告

历史报告：对应 0.3.6；当前 deliverables 为 0.3.7，报告与证据见 UI_TEST_REPORT_20261015.md。下文日期、哈希和测试均保留当时事实。

日期 2026-10-14，Asia/Shanghai；被测产品 0.3.6-preview / versionCode 8。执行依据为用户 CR-20261014-06。以下均为实际命令与渲染结果，合成材料不会被算作真实微信或 AI 质量证据。

## 环境与被测包

Windows / PowerShell 7，工程沿用 JDK17、SDK36、BuildTools35、Gradle8.13 和现有锁定依赖；没有新增生产依赖。显式选用 `emulator-5580`，核验 qemu=1，Android15 / API35 AOSP。最终构建成功，APK 73,731,573 字节，SHA256 `6a789b6fbfb5887cfbac382eb27ed15a16ce5587171985f94c2efd6b68a0a6d9`；已安装 base.apk 一致。

本轮 ADB 仅列出模拟器，没有红米；未安装、解锁或测试真实手机。所有账户、聊天、OCR 选行和候选 UI 材料标为 SYNTHETIC。候选夹具只编入 androidTest 测试 APK，产品无伪造候选回退路径；未配置真实 Key，不调用服务商。

## 自动检查结果

| 层级 | 结果 | 证据 |
|---|---|---|
| 冻结文件 | 两份原始 SHA256 一致，190 行非空源文索引 | verify_baseline.py |
| 第三方 Skill / 许可 | 11 项锁定文件哈希一致 | verify_product_sources.py |
| 核心 JUnit | 87 通过，0 失败、错误、跳过 | reports/product/kotlin/ |
| Android 本地 | 7 通过，0 失败、错误、跳过 | reports/product/android-unit/ |
| Python | 44 通过 | reports/ui-redesign/20261014/python-tests.txt |
| 最终包仪器回归 | **13 通过**，0 失败 | reports/ui-redesign/20261014/all-instrumentation.txt |
| 响应布局 | **5/5 配置通过**，分别执行同一 VisualUiTest | reports/ui-redesign/20261014/matrix-145315/matrix.json |
| 颜色对比度 | 32/32 通过，普通文字≥4.5、规定轮廓≥3 | reports/ui-redesign/20261014/contrast.json |
| 构建 / 签名 | productVerification、qa-runner、apksigner 成功 | build-final.txt；reports/product/artifact-verification.json |
| lint | 0 错误、3 警告、2 提示 | reports/product/lint/lint-results-debug.txt |

138 项 JVM/Python 工程检查、13 项仪器用例、5 次布局配置执行分别计数，不把重复执行的布局用例算成新增独立功能。

## 十三项仪器用例

1. 注册密码不一致保留错误；修改密码；旧密码拒绝；新密码登录成功。
2. 两行合成历史导入先进入待校对区；导航和旋转保留核对草稿；未逐行确认不能入库；搜索不裁剪历史；退出锁定。
3. 最新对方消息表单跨导航和旋转保留；核对后入库；云端未授权显示实际错误且无费用；同次观察重放拒绝；换观众清空旧正文。
4. 模型设置草稿跨导航和旋转保留；保存真实开关；读取未授权显示实际状态；费用与指南入口；删除名称不符不可提交。
5. 合成保存报告中的候选可编辑并复制到系统剪贴板；保存草稿不入历史；另行确认实际发送后才入库。
6. 接口名称、模型、合成 Key 在同一进程的旋转中保留；不核对价格不能保存；接口保存只是主播配置草稿；明确保存后才持久化，无网络调用。
7. 合成 OCR 预览选行填表；身份和方向不会自动勾选，无 AI、无历史入库。
8. Keystore / SQLite 数据重启解密，异账户访问拒绝。
9. 媒体加密原件恢复、范围校验和删除清理。
10. 账号名称去空格且不区分大小写禁止重复；改密保留原 ID 与资料，密码字符数组擦除。
11. 合成 WAV 用实际 MediaPlayer 准备和播放。
12. 合成图片用实际 ML Kit 识别，结果 exact_match=false，**只判管线，不判准确率**。
13. 实际渲染 12 个页面，四项底部导航可见且高度至少 48dp，关键输入项可滚动到达，无费用。

## 布局矩阵与图像复核

| 配置 | 像素 / density | 逻辑尺寸 | 字体 / 夜间 | 结果 |
|---|---|---|---|---|
| compact-light | 750×1624 / 320 | 375×812dp | 1.0 / 关 | 通过 |
| compact-dark | 750×1624 / 320 | 375×812dp | 1.0 / 开 | 通过 |
| large-text-dark | 750×1624 / 320 | 375×812dp | 2.0 / 开 | 通过 |
| landscape-light | 1624×750 / 320 | 812×375dp | 1.0 / 关 | 通过 |
| tablet-light | 1600×2560 / 320 | 800×1280dp | 1.0 / 关 | 通过 |

每种配置保存登录、军师、最新消息、历史导入、批次校对、档案、我的、模型、接口、密钥输入、预算、指南 12 个截图，共 60 张；默认 1080×2400 / density420 的 12 张另存 default-phone-light。脚本结束恢复原尺寸、密度、字体和动画设置。

人工图像复核采用抽查，不宣称逐像素比对全部截图：默认登录和军师、小屏历史导入、深色我的、两倍字体登录和最新消息、横屏接口、平板档案均可阅读；长页通过滚动查看。按钮使用真实控件，导入/候选并非静态页面。

早期默认截图的系统状态栏图标下沿被裁切。改变模拟器显示配置并恢复后，系统设置与 App 状态栏均恢复正常；最终默认截图由同一 APK 重新执行 VisualUiTest 取得，额外一次布局测试通过，日志 default-render-after-resize.txt。此观察支持模拟器显示状态问题，不证明某个 App 代码修复了它，也不替代红米显示适配。

## 过程中暴露和修正的问题

接口校验提示出现/移除时，LazyColumn 项目身份变化曾影响配置名称输入；改为稳定项目 key 和固定校验项。接口编辑状态提升到进程内 controller，在旋转时保留，退出或关闭后清空，未把 Key 写入 rememberSaveable。最终用例同时核验旋转前后字段、价格阻断和保存结果。

测试脚本最初对 Dialog 目标强行调用滚动、尝试点击折叠标题、在 LazyColumn 尚未组合底部按钮时直接查找，造成测试定位失败；改为按实际展开按钮、可见性和 scrollToNode 操作，保留原业务断言。测试输入完成后等待 Compose 空闲，避免旋转或提交抢在输入状态提交前。旧失败原始输出保存为 workflow-*-failures.txt 与 interface-*-failure.txt，不抹去过程记录。

剩余 lint：targetSdk35 的版本警告，两项 Compose 可选 modifier 参数顺序建议，两项 Long 状态装箱提示。本轮不提高 targetSdk 或增加依赖，以免未经真机复测改变系统行为。

## 未执行与结论边界

本轮新增真实 AI 请求为 0；已停止质量批次的账本 SHA256 仍为 `f37ee66292ff0fe1128c549fa6cefcffb1bc8685ba421edd731bd0ad31e943b6`。52 对生成记录含失败、96 个 AI 审阅输出、人工评分 0 的状态保持，不宣称 95/100 或组合优于普通模型。

新版红米覆盖升级、模型调用、微信前台读取与悬浮候选、撤权恢复、锁屏、长时稳定、真实主播口吻均未执行。旧版有限真机和后台无通知失败保留在相应报告。无跨设备同步、密码找回、ZIP 一键恢复或自动 ASR。模拟器检查证明列明的工程与 UI 行为，不能证明完整冻结 P0 或正式产品质量已通过。

复现：先 Build-Product.ps1 构建并更新交付 APK，再 Test-ProductEmulator.ps1 -Serial emulator-5580；之后 Test-ProductUiMatrix.ps1 -Serial emulator-5580。两个测试脚本核验显式模拟器，禁止用于真实手机。报告、截图和测试源码随完整工程交付。
