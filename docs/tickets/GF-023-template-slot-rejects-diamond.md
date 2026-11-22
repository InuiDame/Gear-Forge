# GF-023 钻石无法放入模板槽，导致强化完全无法使用

- 优先级：P0（强化是核心玩法，此 bug 让整个功能不可用）
- 状态：已修复。同时按作者要求新增独立的强化台方块。
- 相关：ADR-0010、GF-001

## 问题

`GearSmithingRecipe.isTemplateIngredient` 只接受转生材料：

```java
return config.isReincarnationEnabled()
        && isItemMatchingId(stack, config.getReincarnationCostMaterial());
```

而这个方法是模板槽 `mayPlace` 的**唯一依据**（原版 `ItemCombinerMenuSlotDefinition` 对每个输入槽都调用它）。强化需要钻石作为模板材料，但钻石被 `isTemplateIngredient` 拒绝，玩家根本放不进去 → `matches` 永远为 false → 结果格永远为空。

这是 GF-001 修复引入的回归：强化开始要求模板材料时，忘了同步放开模板槽的准入条件。

## 修复

`isTemplateIngredient` 改为覆盖**所有**操作需要的模板材料：

```java
if (config.consumesMaterial() && isItemMatchingId(stack, config.getConsumeMaterial())) return true;
return config.isReincarnationEnabled()
        && isItemMatchingId(stack, config.getReincarnationCostMaterial());
```

教训已写进方法注释：任何"配方要求某格放某物"的改动，都必须同步检查那一格的 `mayPlace` 判定，否则就是死锁。

## 同时新增：强化台方块

按作者要求新增 `gear_forge:forge_table`（强化台），贴图由原版锻造台下半部分换色而来。详见 ADR-0010。

要点：

- `ForgeTableMenu` 直接继承 `ItemCombinerMenu`（不能继承 `SmithingMenu`，否则客户端会按 `MenuType.SMITHING` 实例化出原版类，两边 `onTake` 行为不一致）。
- 结算逻辑全部委托 `GearHelper`，与原版锻造台的 `SmithingMenuMixin` 共用，判定只有一份。
- 界面复用原版 `smithing.png`，注册走 NeoForge 的 `RegisterMenuScreensEvent`（原版 `MenuScreens.register` 在 1.21.1 已是 private）。

## 验收

- [x] 钻石可以放入模板槽
- [x] 强化台方块可放置、可右键打开界面、破坏掉落自身
- [x] 强化台与原版锻造台都能完成全部 8 种操作
- [x] `gradlew clean build` 通过，`runServer` 启动 0 ERROR
- [ ] 游戏内实测强化全流程（并入 GF-022）
