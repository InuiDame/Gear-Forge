# GF-021 语言文件里有大量未使用的翻译键

- 优先级：P3
- 状态：待修复，依赖 GF-001 决议
- 相关：`assets/gear_forge/lang/*.json`

## 问题

以下 `message.gear_forge.*` 键在代码里没有使用，来自早期“右键使用材料”的设计或未实现的功能：`no_gear`、`need_diamonds`、`reroll_success`、`max_level`、`upgrade_success`、`upgrade_fail`、`upgrade_downgrade`、`gem_socketed`、`max_sockets`、`incompatible_gem`、`gem_tier_upgraded`、`gem_max_tier`、`enchant_applied`、`no_enchantments`、`offhand_mode_on`、`offhand_mode_off`、`gems_removed`、`reincarnation_success`、`reincarnation_fail_level`。另外 `itemGroup.gear_forge`（不带后缀）也没有使用；`itemGroup.gear_forge.core` 等三个是代码拼接出来的，在用。

## 修复方案

1. GF-001 选方案 A 时保留 `upgrade_fail`、`upgrade_downgrade`、`need_diamonds` 并接入代码。
2. 其余键删除，或者在锻造台无结果时用它们给玩家提示原因（例如插槽已满、宝石不兼容）。后者需要额外的界面提示机制，单独评估。
3. 加一个简单的检查脚本或测试：代码里引用的键在两个语言文件里都存在，语言文件里的键都被引用。

## 验收标准

两个语言文件的键集合一致，且每个键都有引用方。
