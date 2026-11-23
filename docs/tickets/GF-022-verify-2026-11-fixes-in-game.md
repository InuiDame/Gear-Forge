# GF-022 游戏内验证 2026-11 整理的修复

- 优先级：P0
- 状态：待验证
- 相关：`docs/development.md` 第 4 节“手动回归清单”、ADR-0004

## 背景

2026-11 整理修复的问题（见 [README](README.md) 的“已修复”部分）已经通过：

- `./gradlew build` 编译；
- `./gradlew runData`，生成结果除 Curios 槽位外无变化；
- `runGameTestServer` 冒烟：mod 和 Curios 依赖正常加载，`LivingEntityMixin`、`SmithingMenuMixin` 注入成功。

还没有进游戏实际操作验证。

## 要做的事

1. 按 `docs/development.md` 的手动回归清单逐项在 `runClient` 里验证，勾选结果。
2. 在一个旧存档（1.6.51 之前创建、带有强化/词条装备）里加载，确认装备数据正常、属性修饰符没有残留重复。
3. GF-018 落地后，把清单里能自动化的项目改成 GameTest。

## 验收标准

清单全部通过；不通过的项目各自新建 ticket。
