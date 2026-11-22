---
status: accepted
---

# 强化台是独立方块，但与原版锻造台共用同一套锻造逻辑

本模组提供一个自己的方块 `gear_forge:forge_table`（强化台）：外观与原版锻造台一致，只是贴图下半部分换成紫色。玩家在它上面完成全部 8 种锻造操作。

同时，**原版锻造台依然可用**——两者走的是同一套 `GearSmithingRecipe`、同一套 `GearHelper` 结算函数。

## 为什么要有独立方块

不是为了加功能，而是为了**心智模型和兼容性**：

1. 原版锻造台已经被纹饰、下界合金升级、以及其它模组大量占用。本模组的 8 种操作与它们争抢同一个 `RecipeType.SMITHING` 配方列表，玩家放上"纹饰模板 + 护甲 + 钻石"时很难预期会发生什么。
2. 独立方块让"这是强化台，那是锻造台"一目了然，且不会改变他人存档里原版方块的行为。
3. 未来若要给强化加专属 UI（成功率进度、材料预览），有自己的 `MenuType` 才能扩展。

## 为什么没有复制一份锻造逻辑

`ForgeTableMenu` 直接继承 `ItemCombinerMenu`（而不是 `SmithingMenu`），复刻了三格布局与配方查找（约 100 行），但**结算部分全部委托给 `GearHelper`**：

| 职责 | 实现 |
|---|---|
| 分派哪些操作能匹配 | `GearSmithingRecipe.canApply`（唯一真源） |
| 强化掷骰 | `GearHelper.rollUpgradeOutcome` / `applyUpgradeOutcome` |
| 材料补扣 | `GearHelper.consumeTemplateExtra` |
| 材料需求计算 | `GearHelper.getUpgradeConsumeCount` |

原版锻造台通过 `SmithingMenuMixin` 调用同样这几个函数。**判定逻辑只有一份**，两个入口不会逐渐走样。

## 为什么不能继承 SmithingMenu

原版 `SmithingMenu` 的构造器把 `MenuType.SMITHING` 写死传给父类，而客户端会按 `MenuType` 实例化菜单类。继承它的话：

- 服务端拿到的是 `ForgeTableMenu`
- 客户端拿到的是 `SmithingMenu`

两边 `onTake` 行为不一致，强化掷骰只在服务端发生、客户端预测不到，界面会闪烁。所以必须有自己的 `MenuType`，也就必须直接继承 `ItemCombinerMenu`。

## 界面

`ForgeTableScreen` 继承原版 `ItemCombinerScreen`，背景直接复用 `minecraft:textures/gui/container/smithing.png`——三格布局完全一致。唯一区别是不画盔甲架预览（强化不改变外观）。

注册用 NeoForge 的 `RegisterMenuScreensEvent`：原版 `MenuScreens.register` 在 1.21.1 里已是 private。

## 配方

```
@X@
###
###
```
`@` = 铁锭，`X` = 原版锻造台，`#` = 任意木板。比原版锻造台多 3 个铁锭，作为"升级版工作台"的代价。

## Consequences

- 新增方块、方块物品、`MenuType`、客户端界面、方块状态/模型/贴图/战利品表/配方。
- 贴图是原版锻造台贴图的**派生作品**：保留原有明暗与砧面纹理，只把红棕色部分的色相替换为紫色（`270°`），近灰色像素（顶面、边缘）不动。
- 原版锻造台的行为不变，仍可完成全部操作；两个入口共享结算逻辑。
- 若将来只想保留强化台，删除 `SmithingMenuMixin` 即可，`ForgeTableMenu` 不依赖它。
