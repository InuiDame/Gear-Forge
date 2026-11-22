---
status: accepted
---

# 重铸结果是盐值的确定性函数，预览不掷骰

`RerollStone`（重铸石）的词条不再在 `assemble` 里用 `RandomSource.create()` 现掷。改为：从物品上的 `gear_forge:roll_salt`（int，默认 0）派生出确定种子，算出词条后把盐值 +1 写回产物。

## 问题

原版锻造台的 `createResult()` 在**任意输入槽变化**时都会被调用（`ItemCombinerMenu.slotsChanged`），而 `assemble` 就是它的产物计算入口。旧实现每次调用都 `RandomSource.create()` 掷一套新词条，于是：

- 玩家把重铸石拿出来再放回去，就能无限免费重掷，直到刷到满意的一套再取走——核心经济机制被完全绕过。
- 客户端和服务端各掷一次（随机源不同），预览看到的词条和实际拿到的不是同一套，直到槽位同步覆盖。

## 做法

```java
int salt = result.getOrDefault(ROLL_SALT, 0);
RandomSource random = RandomSource.create(rerollSeed(result, salt));
result.set(AFFIXES, rollAffixes(result, random));
result.set(ROLL_SALT, salt + 1);
```

`rerollSeed` 只由「物品注册名 + 强化等级 + 转生次数 + 盐值」决定，**刻意不把旧的词条列表算进去**——那会让同一件装备在两次预览之间因为"旧内容哈希不同"再次产生不同结果，等于又变回可刷。

盐值只在真正取出后才前进（产物带着 `salt + 1` 回到背包），所以：

- 同一次预览反复调用 `assemble` → 种子相同 → 词条相同，**无法免费刷新**。
- 取出后再放回 → 盐值已前进 → 得到新的一套词条，重铸石被正常消耗。

## 为什么不选其它做法

- **把掷骰挪到服务端 `onTake`**：`assemble` 只返回不带词条的底材副本，预览显示占位。问题在 Shift 取出路径——`ItemCombinerMenu.quickMoveStack` 传进 `onTake` 的是已被移走的栈，需要在 mixin 里额外接管，出错面更大。
- **首次为某组输入算出结果时就扣重铸石**：实现复杂，且与"预览不该消耗材料"的直觉冲突。

## Consequences

- 重铸不再能刷，`docs/tickets/GF-002` 关闭。
- 物品上多了一个 `roll_salt` 组件；转生时会清除它（转生同时清掉词条）。
- `assemble` 现在是纯函数（同一输入必得同一输出），这让客户端预览与服务端结果天然一致，也让它对其它 mod 的"预览/自动合成"调用安全。
