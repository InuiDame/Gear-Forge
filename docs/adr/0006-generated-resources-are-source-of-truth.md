---
status: accepted
---

# 数据生成器产出的资源是唯一来源，提交进仓库；构建产物和本地状态不提交

物品模型、合成配方和 Curios 槽位由 `runData` 生成到 `src/generated/resources` 并提交，这样不跑数据生成也能构建，评审时能看到资源变化。`src/main/resources` 不放同路径的手写文件。整理前两处各有一份内容不同的 12 个物品模型和 Curios 槽位文件，靠 `DuplicatesStrategy.INCLUDE` 让后复制的那份覆盖前一份，实际打进 jar 的是哪份并不直观。

## 关于"重复时构建失败"

早期版本的本文件声称"不设置 `DuplicatesStrategy`，出现重复时构建直接失败"。**这是错的**：Gradle 9.2.0 在未显式设置时，`CopySpec` 解析出的默认策略是 `INCLUDE`，不是 `FAIL`（见 `DefaultCopySpec$DefaultCopySpecResolver#getDuplicatesStrategy` 的自解析分支）。也就是说"去掉 `DuplicatesStrategy.INCLUDE`"这一步是空操作，真正防止重复靠的是**没有人往 `src/main/resources` 放手写副本**这一纪律，而不是构建系统。

复核方式：

```
git diff --name-only -- src/main/resources src/generated/resources | Sort-Object
```

只要两边没有同路径文件，就不存在覆盖问题。2026-11 二次整理时实测交集为空。

如果需要真正的构建期保护，应显式加：

```groovy
tasks.named('processResources', ProcessResources) {
    duplicatesStrategy = DuplicatesStrategy.FAIL
}
```

当前**没有**加，因为 `src/main/resources` 与 `src/generated/resources` 之外还有 `generateModMetadata` 生成到 `build/generated/sources/modMetadata` 的目录，误配 `FAIL` 会让正常构建也失败；等资源来源再增加时再评估。

## 不手写的例外已经消除

`lucky_diamond` 与 `cursed_anvil` 的模型此前手写在 `src/main/resources/assets/gear_forge/models/item/`，绕过了 datagen，导致"重跑 `runData` 即可完整重建资源"不成立。2026-11 二次整理把这两个也加进了 `ModItemModelProvider`，手写副本已删除；贴图仍保留在 `src/main/resources/assets/gear_forge/textures/item/`（datagen 不生成二进制贴图）。

## 版本库卫生

`build/`、`.gradle/`、`.idea/`、`run/`、数据生成缓存 `.cache/` 和本地 AI 工具状态都不进版本库。

`gradle/wrapper/gradle-wrapper.jar` 例外，必须提交，否则全新克隆无法运行 `gradlew`。**注意 `.gitignore` 的顺序**：gitignore 采用"最后匹配的模式获胜"，因此 `!gradle/wrapper/gradle-wrapper.jar` 必须写在 `*.jar` **之后**。整理前它写在 `*.jar` 之前，实际不起作用，只是靠"该文件早已被 index 跟踪"侥幸生效——一旦有人 `git rm --cached` 或在干净克隆里重新 `git add`，wrapper jar 会被静默忽略，`gradlew` 直接坏掉。现在该否定规则已移到文件末尾。

验证方式：

```
git check-ignore -v --no-index gradle/wrapper/gradle-wrapper.jar
```

应输出 `!gradle/wrapper/gradle-wrapper.jar` 且退出码为 0（表示"被否定规则命中"，即不被忽略）。

## 行尾

`runData` 在 Windows 上写出 CRLF，而仓库其余部分按 LF 存。`.gitattributes` 因此对 `src/generated/**` 与 `*.json` 强制 `eol=lf`，避免每次数据生成后出现一批纯行尾差异的假改动。
