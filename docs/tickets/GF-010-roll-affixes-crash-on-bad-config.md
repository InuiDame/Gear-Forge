# GF-010 配置不当时抽词条会抛异常

- 优先级：P1
- 状态：待修复
- 相关：`util/GearHelper.java:39`

## 问题

`rollAffixes` 计算 `random.nextInt(maxCount - minCount + 1)`，其中 `maxCount = min(maxAffixCount, 可用词条数)`。以下情况参数 ≤ 0，`nextInt` 抛 `IllegalArgumentException`：

- `minAffixCount` 大于 `maxAffixCount`；
- 禁用词条后可用词条数少于 `minAffixCount`。

重铸在锻造台预览时调用，异常发生在菜单更新里；怪物生成时调用（`MobGearEventHandler`），异常发生在实体加入世界时。两者都可能让服务端报错或玩家被踢出。

另外词条 `weight` 为 0 或负数时总权重计算会出错，`valueRange` 少于两个元素时数值恒为 0 且没有提示。

## 修复方案

1. `minCount = clamp(minAffixCount, 0, maxCount)`，`maxCount ≥ minCount` 后再抽。
2. 过滤 `weight <= 0` 的词条。
3. 加载配置时校验并打印警告：`min > max`、权重非正、`valueRange` 长度不为 2 或下限大于上限。
4. 单元测试覆盖上述边界。

## 验收标准

把 `minAffixCount` 设成 10、禁用到只剩 2 个词条，重铸正常得到 2 个词条，日志里有一条配置警告。
