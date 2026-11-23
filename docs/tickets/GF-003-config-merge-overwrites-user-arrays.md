# GF-003 配置合并会覆盖用户对数组字段的修改

- 优先级：P1
- 状态：待修复
- 相关：ADR-0003

## 问题

`ModConfigs.mergeJsonArrays`（`ModConfigs.java:107`）遇到元素不是“带 id 的对象”的数组时，直接返回默认值（第 112 行 `return defaults;`）。受影响的字段每次启动都被重置为代码默认值：

- 每个词条的 `valueRange`
- 每颗宝石的 `compatibleSockets`
- `gems.json` 的 `socketTypes`
- `upgrades.json` 的 `specialBonusLevels`
- `affixes.json` 的 `rarityAffixCount` 里的各个数组

例如把攻击力词条的范围改成 `[5, 30]`，重启后又变回 `[1, 15]`。

附带问题：每次启动都会重写配置文件并覆盖 `.old` 备份（第 59 行），连续启动两次后备份里已经是合并后的内容，起不到备份作用。

## 修复方案

1. 不带 id 的数组：用户文件里有就用用户的（`return loaded;`），只有缺失时才取默认值（缺失已由 `mergeJsonObjects` 处理）。
2. 只有合并结果和原文件不同时才写回，并且只在写回前备份一次。
3. 补单元测试：用户修改的数值范围在合并后保留；新增的字段被补上；带 id 的数组新增默认项被追加。

## 验收标准

- 改 `valueRange`、`compatibleSockets`、`specialBonusLevels` 后重启，修改保留。
- 配置文件没有变化时启动，文件修改时间不变，不生成新的 `.old`。
