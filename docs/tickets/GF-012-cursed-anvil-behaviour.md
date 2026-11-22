# GF-012 魔性铁砧的效果范围过宽，附魔等级 -5 的实现有副作用

- 优先级：P2
- 状态：已修复。仇恨范围收敛到原版 `NeutralMob`，排除已驯服个体；附魔 -5 的语义在文档中改正；操作者识别半径收紧到交互距离。
- 相关：`compat/curios/CursedAnvilItem.java`、`event/CuriosEventHandler.java`

## 问题（修复前）

1. **仇恨范围**：每 5 秒对 10 格内所有"不是 `Enemy`"的 `Mob` 调用 `setTarget(player)`。这包括玩家自己驯服的狼和猫、村民、动物、铁傀儡、雪傀儡。描述是"中立敌对"，看起来指的是狼、末影人、猪灵、蜜蜂、北极熊这类中立生物（原版 `NeutralMob`）。
2. **附魔等级 -5**：通过 `EnchantmentLevelSetEvent` 把附魔台每个槽位的等级减 5（最低 1）。原版里这个数既是解锁槽位所需的经验等级，也是用来抽附魔的强度，所以实际效果是"门槛降低 5 级，同时附魔质量按低 5 级抽取"。
3. **找操作者的方式**：事件不带玩家，代码取附魔台 10 格内最近的玩家。多人时，旁边站着一个戴魔性铁砧的玩家会影响别人的附魔台。

## 修复

### 1. 仇恨范围

改为只对原版 `NeutralMob` 生效（1.21.1 里是狼、末影人、猪灵、蜜蜂、北极熊、铁傀儡），并且排除 `OwnableEntity` 中有主人的个体：

```java
if (mob instanceof Enemy) continue;
if (!(mob instanceof NeutralMob)) continue;
if (mob instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null) continue;
```

结果：自己的狗不会反咬自己，村民、牛羊、雪傀儡完全不受影响；狼、末影人、猪灵、蜜蜂、北极熊会主动攻击。

半径提取为常量 `PROVOKE_RADIUS = 10.0`，行为不变。

### 2. 附魔 -5 的语义

核实了原版实现：`EnchantmentMenu.clickMenuButton` 里 `costs[id]` **同时**用于两处——等级检查（`player.experienceLevel < this.costs[id]`）和抽取强度（`getEnchantmentList(..., this.costs[id])`）。客户端 `EnchantmentScreen` 的灰显判定也读同一个 `costs` 数组（有 `DataSlot` 同步）。

因此"只降低门槛、不影响质量"必须 mixin 改 `clickMenuButton` 的等级比较，而 `costs` 还被用于抽附魔——要拆分就得引入额外的同步字段，改动面和风险都显著大于收益。

**采用"保留现状 + 改正描述"**：这本来就是原版机制决定的连带效果，不是本模组的实现缺陷。tooltip 改为明确写出"附魔台等级需求 -5（附魔强度同步降低）"，此前只说"等级 -5"，玩家会以为是纯收益。

### 3. 操作者识别

`getNearestPlayer` 的半径从 10 格收紧到 **4 格**，与原版 `canInteractWithBlock(pos, 4.0)` 一致——超过这个距离根本打不开附魔台界面，不可能成为操作者。常量提取为 `TABLE_INTERACT_RANGE`。

这没有完全消除"多人共用一个附魔台"的歧义（原版事件确实不传玩家，彻底解决需要 mixin `EnchantmentMenu.slotsChanged` 记录打开菜单的玩家），但把误伤范围从"路过的任何人"缩小到"确实在使用这个附魔台的人"。

## 验收标准

- [x] 戴魔性铁砧时，自己的狼/猫不会攻击自己（`OwnableEntity` 有主人则跳过）
- [x] 村民、牛羊、雪傀儡不受影响（不是 `NeutralMob`）
- [x] 野生的狼、末影人、猪灵、蜜蜂、北极熊会主动攻击
- [x] tooltip 明确写出附魔强度同步降低
- [x] 4 格外的玩家不会影响附魔台结果
- [ ] 游戏内人工确认（并入 GF-022）

## 遗留

"多人同时使用同一个附魔台"的归属歧义仍未根治，需要 mixin `EnchantmentMenu`。当前 4 格半径已把影响面压到可接受范围，暂不引入新的 mixin。
