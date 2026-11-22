# GF-011 创造模式标签页顺序不确定；charity_bundle 附魔键没有定义

- 优先级：P3
- 状态：已修复：移除 `charity_bundle` 死键；三个创造标签页改成无环链（附魔 → 饰品 → 核心 → 战斗），此前附魔页与饰品页都声明"排在核心页之前"，顺序取决于注册顺序。
- 相关：`item/ModCreativeTabs.java`、`Provider/ModEnchantments.java`

## 问题

1. 附魔页和饰品页都写了 `withTabsBefore(核心页)`（`ModCreativeTabs.java:51`、`:81`），两页的先后顺序没有约束。
2. `ModEnchantments.CHARITY_BUNDLE`（第 17 行）指向 `gear_forge:charity_bundle`，没有对应的附魔 JSON。附魔页查不到它会静默跳过，不会出错，但属于死代码。

## 修复方案

1. 饰品页改成 `withTabsBefore(附魔页)`。
2. 删除 `CHARITY_BUNDLE` 和附魔页列表里的对应项（如果它是计划中的附魔，改为新建 ticket 实现）。

## 验收标准

创造模式标签顺序固定为：核心 → 附魔 → 饰品。
