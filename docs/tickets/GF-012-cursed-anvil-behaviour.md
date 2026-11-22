# GF-012 魔性铁砧的效果范围过宽，附魔等级 -5 的实现有副作用

- 优先级：P2
- 状态：待决议（Q8）
- 相关：`Curios/CursedAnvilItem.java`、`event/CuriosEventHandler.java`

## 问题

1. **仇恨范围**：每 5 秒对 10 格内所有“不是 `Enemy`”的 `Mob` 调用 `setTarget(player)`（`CursedAnvilItem.java:52`）。这包括玩家自己驯服的狼和猫、村民、动物、铁傀儡、雪傀儡。描述是“中立敌对”，看起来指的是狼、末影人、猪灵、蜜蜂、北极熊这类中立生物（原版 `NeutralMob`）。
2. **附魔等级 -5**：通过 `EnchantmentLevelSetEvent` 把附魔台每个槽位的等级减 5（最低 1，`CuriosEventHandler.java:51`）。原版里这个数既是解锁槽位所需的经验等级，也是用来抽附魔的强度（实际扣除的经验固定是 1/2/3 级）。所以现在的效果是“门槛降低 5 级，同时附魔质量按低 5 级抽取”，不只是降低需求。
3. **找操作者的方式**：事件不带玩家，代码取附魔台 10 格内最近的玩家。多人时，旁边站着一个戴魔性铁砧的玩家，会影响别人的附魔台。

## 修复方案

1. 仇恨只对 `NeutralMob` 生效，排除 `OwnableEntity` 中有主人的个体；或者按作者给出的列表用实体标签 `gear_forge:cursed_anvil_provokes` 控制。
2. 如果本意是“只降低门槛、不影响质量”：保留原 cost，mixin `EnchantmentMenu.clickMenuButton` 里的等级检查（`experienceLevel < costs[id]`）改为减 5 后比较；客户端界面的灰显判断也要同步改。如果本意就是降低强度换取更低门槛，保留现状，把描述改成“附魔台等级 -5（附魔强度同步降低）”。
3. 操作者：mixin `EnchantmentMenu.slotsChanged`（菜单持有玩家）或在 `PlayerContainerEvent.Open` 时记录打开附魔台的玩家，替代“最近玩家”。

## 验收标准

戴魔性铁砧时，自己的狗不会攻击自己，村民和铁傀儡不受影响，狼/末影人会主动攻击；附魔效果与决议一致。
