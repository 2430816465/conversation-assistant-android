# 验证记录

2026-10-14，在本源码副本运行：

```sh
python -m unittest discover -s scripts/tests -v
```

结果：38 tests，OK。覆盖评分、合成输入与审计工具逻辑，不等同 Android 真机或真实推理验证。

2026-10-13 原工程记录：核心77项、本地Android单元7项及Python38项检查通过；0.3.2 一轮有限配置 DeepSeek＋双 Skill 真机流程经3次请求产生4条候选。0.3.3 已构建，未将旧真机结果算作新版本结果。本次没有重新安装APK或调用真实模型API。正式A/B及人工评分仍为0/100。

详见 `docs/TEST_REPORT.md`、`docs/AI_QUALITY_TEST_REPORT.md` 和 `docs/KNOWN_ISSUES.md`。原始规格、校验文件和第三方许可证保持原样；真实聊天、密钥与设备截图未发布。
