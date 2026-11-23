# GF-016 宝石特殊效果靠字符串包含关系匹配

- 优先级：P3
- 状态：待修复（按默认方案，见 Q16）
- 相关：`util/GearHelper.java:140` `hasUniqueGemEffect`

## 问题

下界之星的“抗性提升”效果在配置里是 `{"type": "unique", "value": 0, "description": "提供抗性提升效果"}`，没有标识具体是哪种特殊效果。代码判断时检查“描述里包含 `nether_star`，或者宝石 id 包含 `nether_star`”。改了宝石 id、或者整合包作者新加一颗 id 里带 `nether_star` 的宝石，行为都会意外改变；描述是给人看的文本，不应该参与逻辑。

## 修复方案

1. 宝石效果增加 `id` 字段，特殊效果写成 `{"type": "unique", "id": "resistance_aura"}`。
2. `hasUniqueGemEffect` 按 `type == unique && id == 参数` 判断。
3. 配置合并时，旧配置里没有 `id` 的 `unique` 效果按宝石 id 补默认值，保证老存档行为不变。

## 验收标准

把下界之星宝石的 id 改名后，抗性效果仍然生效；描述改成任意文字也不影响。
