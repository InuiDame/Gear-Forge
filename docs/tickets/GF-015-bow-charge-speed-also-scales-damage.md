# GF-015 弓蓄力加速同时放大箭矢伤害

- 优先级：P2
- 状态：已修复：删掉箭伤侧四处蓄力叠乘；Mixin 改用小数累加器，长期推进速率与配置值一致。
- 相关：`event/GearEventHandlers.java:249`、`:257`、`:288`；`mixin/LivingEntityMixin.java`

## 问题

`bow_charge_speed`（黄玉宝石、弓蓄力加速词条、转生保留）有两个效果：

1. `LivingEntityMixin` 每 tick 额外推进蓄力进度，拉弓更快——和名称一致。
2. `handleRangedProjectileDamage` 把箭伤再乘以 `1 + 蓄力加速`，弓上的宝石、弓上的词条、转生保留、四件盔甲上的加成各乘一次。T5 黄玉（+150%）一颗就让箭伤变成 2.5 倍，几项叠乘后可以到十倍以上。

伤害本来已经有专门的 `projectile_damage` 效果负责，蓄力加速再放大伤害看起来是重复设计。

## 修复方案

删除 `handleRangedProjectileDamage` 里所有按 `bow_charge_speed` 乘伤害的代码，蓄力加速只影响蓄力速度。如果作者确实想要“快速拉弓也有伤害奖励”，改成加法汇总后只乘一次，并在提示文本里写明。

## 验收标准

带 T5 黄玉的弓满蓄力射出的箭，伤害等于无宝石弓的伤害 + `projectile_damage` 加成。
