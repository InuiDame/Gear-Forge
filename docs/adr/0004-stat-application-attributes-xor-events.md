---
status: accepted
---

# 一项数值只走一条生效路径：原版属性能表达的挂属性修饰符，其余走事件

原版有对应属性的数值（攻击、攻速、护甲、韧性、移速、生命、击退、幸运）通过 `ItemAttributeModifierEvent` 挂到物品上，其余效果（暴击、元素伤害、闪避、荆棘、护甲穿透等）在伤害和 tick 事件里结算。同一项数值不能两边都加。

修饰符 id 必须带槽位后缀（`gear_forge:<名字>/<槽位>`）。原版按 id 合并修饰符，几件盔甲用同一个 id 时只有一件生效，脱下其中一件还会把其它件的同 id 修饰符一起移除。盔甲的修饰符只挂在它自己的护甲槽，不在手持时生效。

2026-11 整理前，强化攻击/护甲/韧性和部分转生保留奖励既挂了属性又在伤害事件里再加一遍，实际效果是提示文本的两倍。2026-11 二次整理把这条规则补齐到所有数值，修掉了当时仍然存在的实现违例：

- `all_stats` 曾经同时走三条路径（属性 + 固定加伤 + 固定减伤），1 点全属性实际等于 4 份收益。现在只在属性路径结算（护甲 / 幸运 / 攻击力 / 生命各 +1）。
- `durability_boost`（翡翠）曾经既在 `ArmorHurtEvent` 里减损耗、又每 20 tick 主动修复。现在只减损耗，并且减免系数统一收敛在 `GearHelper.durabilityDamageFactor` 并钳制在 `[0.05, 1]`，避免高阶翡翠算出负系数导致护甲永不掉耐久。
- `bow_charge_speed` 曾经既加速蓄力、又在箭伤里按同一数值叠乘。现在只影响开弓速度，箭伤不再随蓄力速度放大。
- 宝石的 `attack_damage` / `defense` 曾经只走固定加伤 / 固定减伤，与词条的 `attack_power` / `defense_power`（走属性）结算方式不同，同为“防御”却无法对齐。现在统一走属性。
- 转生保留的词条里有 7 个 id（`max_health`、`luck`、`knockback_boost`、`armor_penetration`、`damage_reduction`、`thorns`、`dodge_chance`）此前只写进 `STORED_BONUSES`、从未被任何结算路径查询，提示文本显示却完全不生效。现在 `max_health` / `luck` / `knockback_boost` / `thorns` 已接回结算；`armor_penetration` / `damage_reduction` / `dodge_chance` 因为属于“事件路径”数值，接回方式见下方约定。

## 约定

新增数值时按顺序判断：

1. 原版有对应 `Attribute` → 只挂属性修饰符，id 带槽位后缀。
2. 没有对应属性、且是“每次伤害一次性生效”的 → 只写在 `LivingDamageEvent.Pre` / `LivingIncomingDamageEvent` 之一，不要两处都写。
3. 没有对应属性、且是“持续状态”的 → 只写在 `PlayerTickEvent.Post` 或 `ArmorHurtEvent` 之一。

事件路径里读 `STORED_BONUSES` 时必须同时处理词条的 `id` 和宝石的 `gem_<type>` 前缀，两者由 `GearHelper.getStoredBonusValue` 统一回退，不要在调用处各自拼前缀。

## Consequences

- 强化、转生后的攻击和护甲收益与提示文本一致，但比 1.6.51 之前的实际数值低。
- `all_stats` 的收益从“4 份”降到“4 项属性各 1 点”，数值上仍是最高密度的宝石，但不再额外提供固定减伤。
- 翡翠的 `durability_boost` 不再提供自动修复，纯减损耗；`auto_repair` 词条仍是唯一的“主动修复”来源。
