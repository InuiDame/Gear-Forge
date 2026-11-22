# GF-004 词条和宝石配置里有未使用的字段

- 优先级：P2
- 状态：待决议（Q5）
- 相关：ADR-0003、GF-001

## 问题

以下字段写进了配置文件，用户会以为能调，但代码没有读取：

| 文件 | 字段 | 看起来的用途 |
|---|---|---|
| `affixes.json` | `rarityAffixCount`（common…legendary 各自的词条数区间） | 稀有度系统 |
| `affixes.json` | `affixRerollCostDiamonds`（默认 3） | 重铸消耗钻石 |
| `gems.json` | `socketTypes` | 插槽类型列表（实际类型硬编码在 `GearHelper.getSocketTypeForItem`） |
| 宝石定义 | `tier` | 宝石默认阶级 |
| 所有文件 | `configVersion` | 配置迁移 |

## 修复方案

- 稀有度：要么实现（重铸时先抽稀有度，再按区间决定词条数，提示文本显示稀有度颜色），要么删除。
- 重铸消耗：如果保留，需要和 GF-001 一样用 mixin 扣除多个模板材料。
- `socketTypes`、宝石 `tier`：删除。
- `configVersion`：保留，作为以后配置迁移的依据，在 `ModConfigs` 里读取并在版本变化时打印日志。

## 验收标准

配置文件里的每个字段都能在代码里找到读取方，或者在 `docs/architecture.md` 第 4 节说明用途。
