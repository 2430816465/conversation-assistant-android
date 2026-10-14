# 验证记录

## 2026-10-15 同步与复验

2026-10-15，在 GitHub 源码副本复验 **0.3.7-preview / versionCode 9**：

| 检查 | 本次结果 |
|---|---|
| `productVerification` | 构建成功；包含 QA 工具编译与 Debug APK |
| Kotlin 核心 JUnit | 87 通过，0 失败 / 错误 / 跳过 |
| Android 本地 JUnit | 7 通过，0 失败 / 错误 / 跳过 |
| Python 工具单元检查 | 44 通过 |
| Android 仪器测试 APK 编译 | `assembleDebugAndroidTest` 成功；本次未安装运行 |
| Android lint | 0 错误、3 警告、2 提示 |
| 冻结规格 | PRD / Master 原 SHA256 匹配；190 行非空源文索引 |
| 上游 Skill 与许可 | 11 项锁定资源逐字节 SHA256 匹配 |
| 新版截图导出脚本 | Python 语法解析通过 |

以下命令为 Windows PowerShell，在根目录执行；需 JDK 17 与 Android SDK 36：

```powershell
.\scripts\Build-Product.ps1
.\gradlew.bat :product-app:assembleDebugAndroidTest :product-app:lintDebug --console=plain --no-daemon
python -m unittest discover -s scripts/tests -v
python scripts/verify_baseline.py
python scripts/verify_product_sources.py
```

本次源码副本的 Debug APK SHA256：`dc7a862226f05733ea2d33918a7aa688deaad2b7f5706552946d99ee47881d82`。构建路径及打包过程会影响产物；它与原工程报告中的安装包分别记录，不声称可复现构建或已安装此包。

初次复验发现旧公开副本遗漏双 Skill 所需的 7 个参考资源，导致 `bundledSkillsAreActualPinnedDocuments` 失败。已按原锁文件补齐，未修改上游内容或业务断言；随后核心、本地测试和完整构建通过。

原工程 0.3.7 的模拟器、布局矩阵及界面记录见 [原工程界面报告](docs/UI_TEST_REPORT_20261015.md) 和 [交付记录](docs/DELIVERY_0.3.7.md)，不计作本次 GitHub 副本重新执行的设备测试。本轮未连接实体手机、未调用付费模型、未恢复已停止质量批次。原工程 52 对生成记录含失败、96 个 AI 审阅输出、0 份人工评分，不能推导正式质量通过率。

源码文件及发布字节哈希见 [SOURCE_INVENTORY.json](SOURCE_INVENTORY.json)，同步范围见 [同步摘要](validation/source-sync-20261015.json)。

## 历史记录（原日期保留）

2026-10-14，在本源码副本运行：

```sh
python -m unittest discover -s scripts/tests -v
```

结果：38 tests，OK。覆盖评分、合成输入与审计工具逻辑，不等同 Android 真机或真实推理验证。

2026-10-13 原工程记录：核心77项、本地Android单元7项及Python38项检查通过；0.3.2 一轮有限配置 DeepSeek＋双 Skill 真机流程经3次请求产生4条候选。0.3.3 已构建，未将旧真机结果算作新版本结果。本次没有重新安装APK或调用真实模型API。正式A/B及人工评分仍为0/100。

详见 `docs/TEST_REPORT.md`、`docs/AI_QUALITY_TEST_REPORT.md` 和 `docs/KNOWN_ISSUES.md`。原始规格、校验文件和第三方许可证保持原样；真实聊天、密钥与设备截图未发布。
