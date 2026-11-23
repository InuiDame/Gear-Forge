# GF-020 包命名不规范，Provider 包职责混杂

- 优先级：P3
- 状态：待修复（按默认方案，见 Q16）

## 问题

- `com.hana.gear_forge.Curios`、`com.hana.gear_forge.Provider` 首字母大写，不符合 Java 包名全小写的惯例；在大小写不敏感的文件系统上和同名小写包容易冲突。
- `Provider` 里同时放了附魔 `ResourceKey` 常量（`ModEnchantments`，运行时代码）和数据生成 provider（只在 `runData` 用）。`datagen` 包里却只有一个入口类。
- `component` 包里放着 `ModGlobalLootModifiers`，和数据组件无关。

## 修复方案

| 现在 | 改为 |
|---|---|
| `Curios.*` | `curios.*` |
| `Provider.ModEnchantments` | `enchantment.ModEnchantments` |
| `Provider.ModItemModelProvider`、`ModRecipeProvider`、`CuriosProvider` | `datagen.*` |
| `component.ModGlobalLootModifiers` | `loot.ModGlobalLootModifiers` |

纯重命名，注册 id 和数据组件名都不变，不影响存档。放在一个单独的提交里，方便评审。

## 验收标准

`./gradlew build runData` 通过，`src/generated` 无变化，存档里的装备数据正常读取。
