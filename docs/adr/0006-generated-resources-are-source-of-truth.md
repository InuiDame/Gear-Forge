---
status: accepted
---

# 数据生成器产出的资源是唯一来源，提交进仓库；构建产物和本地状态不提交

物品模型、合成配方和 Curios 槽位由 `runData` 生成到 `src/generated/resources` 并提交，这样不跑数据生成也能构建，评审时能看到资源变化。`src/main/resources` 不放同路径的手写文件，`processResources` 也不设置 `DuplicatesStrategy`，出现重复时构建直接失败。整理前两处各有一份内容不同的 12 个物品模型和 Curios 槽位文件，靠 `DuplicatesStrategy.INCLUDE` 让后复制的那份覆盖前一份，实际打进 jar 的是哪份并不直观。

`build/`、`.gradle/`、`.idea/`、`run/`、数据生成缓存 `.cache/` 和本地 AI 工具状态都不进版本库。`gradle/wrapper/gradle-wrapper.jar` 例外，必须提交，否则全新克隆无法运行 `gradlew`（整理前它被 `*.jar` 规则挡掉了）。
