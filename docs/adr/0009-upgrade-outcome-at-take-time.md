---
status: accepted
---

# 强化的随机结算放在取出时，预览固定显示成功

锻造锤的强化有成功率、降级和损毁。这三件事都必须发生在**玩家真正取走产物**的时刻，不能发生在 `assemble` 里。

## 为什么不能放在 assemble

原版锻造台的 `createResult()` 在任意输入槽变化时都会被调用（`ItemCombinerMenu.slotsChanged`），而 `assemble` 就是它的产物计算入口。如果在那里掷骰：

- 玩家把锻造锤拿出来再放回去就能无限重掷，直到成功再取走——和 GF-002 的重铸刷词条是同一个漏洞。
- 客户端与服务端各掷一次（随机源不同），预览显示的结果与实际拿到的不是同一份。

## 做法

- `GearSmithingRecipe.assemble` 的强化分支固定把等级写成 `+1`。预览永远是"成功"，是确定的，不能刷。
- `SmithingMenuMixin.onTake` 的 `@At("HEAD")` 处掷骰，直接改写玩家拿到的这一份 `result`：

| 结果 | 对 result 的处理 |
|---|---|
| `SUCCESS` | 不动（`assemble` 已写成 +1） |
| `FAIL_KEEP` | 等级还原成底材的等级 |
| `FAIL_DOWNGRADE` | 等级减去 `downgradeLevels`，下限 0 |
| `DESTROYED` | `result.setCount(0)`，玩家拿不到东西 |

掷骰顺序是**先损毁、再成功**。反过来的话，高等级时"先判成功、成功后再生效损毁"会让 `allowDestroy` 的语义变成"必定先成功再销毁"，配置失去意义。

## levelFailureDecay 的语义

`upgrades.json` 里这个字段原本没有明确的用途说明。定为"失败时真正降级的概率"：

```
失败 → 若 allowDowngrade 且 downgradeLevels > 0 → 以 levelFailureDecay 的概率降级，否则保持等级
```

这样 `allowDowngrade=true` 不等于必定掉级，配置可以做出"失败了但没掉级"的层次感，也解释了为什么它叫 decay 而不是 chance。

## 客户端

`onTake` 在客户端也会跑（菜单点击预测）。Mixin 首行 `if (player.level().isClientSide()) return;` 提前返回，否则客户端会多扣一次材料、并且可能掷出与服务端不同的结果，导致界面闪烁。

## 材料消耗

`getConsumeAmount(level)` 加上 `maxConsumeMaterialPerLevel` 封顶（默认 5）。这个字段此前也未被使用，但它是必要的——没有上限时 15 级需要 15 个材料，模板槽虽然放得下 64 个，玩家体验上完全无法预期。

`consumeMaterial` 为空字符串表示"强化不消耗材料"，此时模板槽必须为空，保留免费强化玩法。

## Consequences

- 强化不再必定成功，默认配置下 15 级需要约 118 钻石（前 5 级 15 个 + 6~15 级期望 103 个）。
- 想让强化回到"必成且免费"：`baseSuccessRate: 1.0`、`levelSuccessDecay: 0.0`、`consumeMaterial: ""`。
- 失败反馈走 actionbar（`message.gear_forge.upgrade_fail` / `.upgrade_downgrade` / `.upgrade_destroyed`）。
- 与 ADR-0008（重铸盐值）是同一类问题的同一类解法：**预览必须是纯函数，随机只能发生在取出时**。
