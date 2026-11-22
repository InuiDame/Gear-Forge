# GF-001 强化系统的成功率、降级、损毁、材料消耗配置没有实现

- 优先级：P1
- 状态：待决议（Q3）
- 相关：ADR-0001、GF-002、GF-021

## 问题

`upgrades.json` 里有 11 个字段完全没有被代码读取：`baseSuccessRate`、`levelSuccessDecay`、`levelFailureDecay`、`maxConsumeMaterialPerLevel`、`consumeMaterial`、`consumeAmountBase`/`consumeAmountIncrement`、`allowDowngrade`/`downgradeLevels`、`allowDestroy`/`destroyChance`。`UpgradeConfig.getSuccessRate()`、`getConsumeAmount()` 也没有调用方。

实际行为：强化 100% 成功；模板格可以空着（`GearSmithingRecipe.java:48`），只消耗 1 个锻造锤。语言文件里已经有 `upgrade_fail`、`upgrade_downgrade`、`need_diamonds` 文案，说明作者原本计划实现。

## 修复方案

**方案 A：按配置实现**

1. `matches`：模板格必须是 `consumeMaterial`，数量 ≥ `getConsumeAmount(当前等级)`。
2. 结果预览显示 +1，并在提示里写出成功率。
3. 成功/失败在取出时决定，不能在 `assemble` 里决定（预览阶段可以反复放取）。在 `SmithingMenuMixin.onTake` 中：按成功率掷骰；失败时按配置降级或损毁，改写玩家拿到的结果。需要处理普通取出和 shift 快速取出两条路径（快速取出时 `onTake` 在物品移入背包之后才调用）。
4. 用 mixin 补扣 `getConsumeAmount - 1` 个模板材料（和转生同一机制）。
5. 失败时发送 `upgrade_fail` / `upgrade_downgrade` 消息。

随机数来源依赖 GF-002 的结论（同样存在“预览 vs 实际”问题）。

**方案 B：删除这些字段**

删掉 `UpgradeConfig` 里的这 11 个字段和对应 getter、语言文件里的失败类文案，`README` 写明强化必定成功。

## 验收标准

- A：成功率为 0 时取出必定失败；`allowDowngrade=false` 时失败不降级；模板材料按等级扣除正确；shift 取出和普通取出结果一致。
- B：配置文件里不再出现这些字段，旧配置文件中的这些字段被合并逻辑保留也不影响加载。
