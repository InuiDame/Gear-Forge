# GF-009 幸运钻石掉率异常

- 优先级：P1
- 状态：待决议（Q6）
- 相关：`data/gear_forge/loot_modifiers/`、`event/LootModEvents.java`

## 问题

幸运钻石（幸运 +20 的饰品，不可堆叠）有三个来源：

| 来源 | 位置 | 概率 |
|---|---|---|
| 地牢箱子 | `add_lucky_diamond_to_dungeon.json` | **100%**（`random_chance` 为 1） |
| 废弃矿井箱子 | `add_lucky_diamond_to_mineshaft.json` | 5% |
| 僵尸死亡 | `LootModEvents.java:22` | 2%（第 21 行注释写的是 20%） |

地牢 100% 看起来像测试时留下的值。僵尸掉落写在代码里，无法用数据包调整，也不受抢夺附魔和 `killed_by_player` 条件约束（被岩浆、摔落杀死的僵尸也会掉）。

## 修复方案

1. 地牢概率改成作者确定的数值。
2. 僵尸掉落改成全局战利品修改器 `add_lucky_diamond_to_zombie.json`，条件用 `loot_table_id: minecraft:entities/zombie` + `killed_by_player` + `random_chance_with_enchanted_bonus`（抢夺加成），删除 `LootModEvents`。
3. 修正注释。

## 验收标准

三个来源的概率都在 JSON 里，数值与决议一致；非玩家击杀的僵尸不掉幸运钻石。
