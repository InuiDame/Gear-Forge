# GF-001 强化系统的成功率、降级、损毁、材料消耗配置没有实现

- 优先级：P1
- 状态：已修复（方案 A，见 ADR-0009）。11 个字段全部接入，成功率/降级/损毁/材料消耗均按配置生效。
- 相关：ADR-0001、ADR-0009、GF-002、GF-021

## 问题（修复前）

`upgrades.json` 里有 11 个字段完全没有被代码读取：`baseSuccessRate`、`levelSuccessDecay`、`levelFailureDecay`、`maxConsumeMaterialPerLevel`、`consumeMaterial`、`consumeAmountBase`/`consumeAmountIncrement`、`allowDowngrade`/`downgradeLevels`、`allowDestroy`/`destroyChance`。`UpgradeConfig.getSuccessRate()`、`getConsumeAmount()` 也没有调用方。

实际行为：强化 100% 成功；模板格可以空着，只消耗 1 个锻造锤。语言文件里已经有 `upgrade_fail`、`upgrade_downgrade`、`need_diamonds` 文案，说明作者原本计划实现。

## 修复（方案 A）

本轮采用「按配置实现」，因为语言文件与配置字段都表明这是原本的设计意图，直接删掉字段会让这些配置项变成永远无法兑现的死配置。

### 材料消耗

`GearSmithingRecipe.canApply` 的强化分支改为要求模板槽放下 `consumeMaterial`，数量按 `getConsumeAmount(currentLevel + 1)` 计算：

```
getConsumeAmount(level) = clamp(consumeAmountBase + consumeAmountIncrement * (level - 1), 1, maxConsumeMaterialPerLevel)
```

`maxConsumeMaterialPerLevel` 之前也未被使用，现在作为上限——没有它时高等级会要求几十个材料，玩家既放不下也看不懂。`consumeMaterial` 为空字符串表示"强化不消耗材料"，此时模板槽必须为空，保留原有的免费强化玩法。

### 成功 / 降级 / 损毁

**关键约束**：锻造台在放入物品时就算出结果并显示，取出前可以反复放取。所以随机判定不能放在 `assemble` 里，否则等于回到 GF-002 的"预览刷结果"。

因此：
- `assemble` 的强化分支固定显示 `+1`（预览是确定的，不能刷）。
- 真正的掷骰在 `SmithingMenuMixin.onTake` 的 HEAD 处，直接改写玩家拿到的这一份产物。

掷骰顺序（`GearHelper.rollUpgradeOutcome`）：

1. 先判损毁（`allowDestroy` 开启时，概率 `destroyChance`）——排在成功之前，否则高等级时"先成功后损毁"会让配置失去意义。
2. 再判成功（概率 `getSuccessRate(当前等级)`）。
3. 失败时按 `getDowngradeChance()` 决定是否降级；`allowDowngrade=false` 或 `downgradeLevels<=0` 时不降级。

`levelFailureDecay` 的语义定为"失败时降级的概率"：这样 `allowDowngrade=true` 不等于必定掉级，配置可以做出"失败了但没掉级"的层次感。

### 客户端与服务端

`onTake` 在客户端也会执行（菜单点击预测）。Mixin 首行 `if (player.level().isClientSide()) return;` 提前返回，避免客户端多扣一次材料或掷出与服务端不同的结果。

### 提示

失败/降级/损毁分别发送 `message.gear_forge.upgrade_fail` / `.upgrade_downgrade` / `.upgrade_destroyed`（actionbar）。`.upgrade_destroyed` 是本轮新增的键。

## 默认配置下的实际曲线

| 等级 → | 成功率 | 材料（钻石） |
|---|---|---|
| 0 → 1 | 100% | 1 |
| 1 → 2 | 95% | 2 |
| 2 → 3 | 90% | 3 |
| 3 → 4 | 85% | 4 |
| 4 → 5 | 80% | 5 |
| 5 → 10 | 75% → 55% | 5 |
| 10 → 15 | 50% → 30% | 5 |

前 5 级必定成功，合计 15 钻石；6~15 级合计期望约 21 次尝试、约 103 钻石。默认 `allowDestroy=false`，永不损毁；失败时 10% 概率降 1 级。

## 验收标准

- [x] 成功率由 `baseSuccessRate` 与 `levelSuccessDecay` 决定，满级时受 `maxLevel` 限制
- [x] `allowDowngrade=false` 时失败不降级（`rollUpgradeOutcome` 提前返回 `FAIL_KEEP`）
- [x] 模板材料按等级扣除，受 `maxConsumeMaterialPerLevel` 封顶
- [x] shift 取出与普通取出走同一条 `onTake` 路径，结果一致
- [x] 预览固定 +1，无法通过反复放取改变结果
- [x] 客户端不参与结算
- [ ] 游戏内人工确认（并入 GF-022）
