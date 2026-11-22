# GF-008 提示文本里硬编码中文，英文环境也显示中文

- 优先级：P2
- 状态：待修复
- 相关：`event/GearEventHandlers.java` `ClientEvents`

## 问题

- 转生保留奖励的前缀 `"[转生]"`、`"宝石:"`、`"强化:"` 写死在代码里（`GearEventHandlers.java:927` 附近）。
- `STAT_NAME_MAP`（第 948 行）把属性名写死成中文。
- 词条名、宝石名直接取配置里的 `name` 字段，默认值是中文，英文客户端看到的是中文。
- `en_us.json` 里有 `itemGroup`、物品、附魔等翻译，但没有词条和宝石名的键。

## 修复方案

1. 新增翻译键：`affix.gear_forge.<id>`、`gem.gear_forge.<id>`、`stat.gear_forge.<key>`、`tooltip.gear_forge.stored_prefix` 等，补全 `en_us.json` 和 `zh_cn.json`。
2. 显示时优先用翻译键；翻译不存在时（例如整合包作者新增的词条）回退到配置里的 `name`，再回退到 id。
3. 提示文本全部用 `Component.translatable`，不要拼接字符串。

## 验收标准

英文客户端里，词条、宝石、转生奖励的提示文本没有中文；在配置里新增一个只有 `name` 的词条，提示显示这个 `name`。
