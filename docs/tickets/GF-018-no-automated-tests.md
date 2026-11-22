# GF-018 没有自动化测试

- 优先级：P1
- 状态：待决议（Q13）
- 相关：`docs/development.md` 第 4 节

## 问题

仓库没有 `src/test`，也没有注册任何 GameTest。`runGameTestServer` 会因为“No test functions were given!”直接失败。本次整理发现的刷物品、双倍计算、属性 id 冲突等问题，都是简单测试就能抓住的。

## 修复方案

两层测试：

**JUnit（`src/test/java`）**：不依赖游戏运行的纯逻辑。

- `ModConfigs` 合并：用户修改保留、默认字段补齐、带 id 数组合并（GF-003）。
- `GearHelper.rollAffixes`：数量边界、权重、禁用词条（GF-010）。
- `calculateReincarnationBonuses`：前缀和累加规则。

需要把这些逻辑从依赖 `FMLPaths`、`ItemStack` 的代码里拆出纯函数，或者引入 NeoForge 的 JUnit 支持（`neoForge.unitTest`）。

**GameTest（`src/main/java/.../gametest`，命名空间 `gear_forge`）**：

- 锻造：整组底材只产出 1 个；转生扣除 8 个材料；满级不能再强化；宝石插槽上限。
- 属性：四件强化盔甲护甲值相加；手持盔甲不加护甲；强化剑伤害等于提示值。
- 伤害：荆棘互打不递归；虚空伤害不被减免。
- 挖掘附魔：潜影盒内容不丢（GF-006）。

`runGameTestServer` 在 CI 上作为必过检查（见 GF-019）。

## 验收标准

`./gradlew test runGameTestServer` 通过，并且故意改回整理前的刷物品代码时至少一个测试失败。
