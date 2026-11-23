# 问题清单

整理时间：2026-11-10。优先级：P0 会导致物品丢失、刷物品或需要立即验证；P1 功能失效或明显错误；P2 设计或体验问题；P3 代码整洁。

## 已修复（2026-11 整理，待游戏内验证，见 GF-022）

改动已放进 G: 仓库的暂存区，尚未提交。

### 玩法 bug

| 问题 | 修复 |
|---|---|
| 锻造台底材放一整组（例如 64 根木棍）时，产物也是一整组，但只扣 1 个，可以无限复制 | `assemble` 只复制 1 个底材 |
| 转生要求 8 个下界合金块，实际只扣 1 个 | 新增 `SmithingMenuMixin`，取出时补扣 |
| 强化的攻击/护甲/韧性、转生保留的攻击和护甲，既挂了属性又在伤害事件里再加一遍，实际是提示值的两倍 | 只保留属性修饰符（ADR-0004，**待确认 Q1**） |
| 几件装备上同名属性修饰符 id 相同，四件盔甲的强化护甲只生效一件，脱下一件会把其它件的也移除 | 修饰符 id 加槽位后缀 |
| 盔甲属性挂在 `ANY` 槽，拿在手里也生效 | 只在自己的护甲槽生效（**待确认 Q2**） |
| 多颗宝石的移速加成只算第一颗 | 先累加再挂一次 |
| 两个都有荆棘词条的实体互相攻击时无限反弹 | 荆棘伤害不再触发荆棘 |
| 高强化装备的固定减伤能抵消虚空和 `/kill` 伤害，掉进虚空死不掉 | 跳过 `#bypasses_invulnerability` 伤害 |
| 雷电词条的雷暴加成在设置伤害之后才计算，不生效 | 移到设置伤害之前 |
| 掠夺词条会复制玩家死亡时的整个背包，以及怪物身上掉落的装备 | 玩家死亡和有耐久的掉落不参与复制 |
| 经验加成词条按“掉落物数量 × 比例”取整，几乎总是 0 | 改用 `LivingExperienceDropEvent` 按比例加经验 |
| 魔性铁砧的经验 +15% 对 1–6 点的经验球被截断成 0 | 小数部分按概率补足 |
| 魔性铁砧会把附魔台不可用的槽位（等级 0）变成 1 级 | 等级 0 的槽位不处理 |
| 强化怪物在 `FinalizeSpawnEvent` 时处理，此时怪物还没有装备，功能从未生效 | 改为怪物加入世界时处理；词条列表改为不可变列表 |
| 拉弓时每 tick 在服务端和客户端各打一条 INFO 日志 | 删除调试日志 |
| 高阶翡翠让盔甲耐久损耗变成负数 | 损耗下限为 0 |
| 箭矢伤害按当前手持的弓计算，射出后换手会改变结果 | 优先用箭记录的发射武器 |
| 配置里宝石缺少 `itemId`、`rarity`、`effects` 时提示文本空指针 | 判空 |
| 寒冰类减速效果的等级可能为负 | 下限为 0 |

### 构建与仓库

| 问题 | 修复 |
|---|---|
| `neoforge.mods.toml` 依赖段写成 `dependencies.gear_forege`，第二条依赖的 modId 也写错，Curios 用的是旧字段 `mandatory`，三条依赖声明全部无效 | 改用 `${mod_id}`，第二条改为 `minecraft`，Curios 改为 `type="required"` |
| `gradle-wrapper.jar` 被 `*.jar` 规则忽略，全新克隆无法运行 `gradlew` | 用 Gradle 9.2.0 重新生成并提交 |
| `build/`、`.gradle/`、`.idea/`、`run/`（含存档和日志）、AI 工具状态共 585 个文件被提交 | 移出版本库（本地文件保留），重写 `.gitignore` |
| `src/main/resources` 和 `src/generated/resources` 各有一份内容不同的 12 个物品模型和 Curios 槽位，靠 `DuplicatesStrategy.INCLUDE` 互相覆盖 | 删除手写副本，槽位的 `order`/`icon` 合并进 `CuriosProvider`，去掉 `INCLUDE`（ADR-0006） |
| 数据生成缓存 `.cache/` 被打进 jar（排除规则路径写错） | 修正排除规则 |
| `mod_license=All Rights Reserved` 与 MIT 的 `LICENSE` 冲突 | 改为 MIT |
| 显示名是 `gear_forge`，作者、描述没有写进元数据 | 显示名 `Gear Forge`，补上作者和描述 |
| `build.gradle` 有重复的 `runs` 块、硬编码 Curios 版本、Gradle 10 将移除的属性赋值写法 | 清理 |
| `GatherDataEvent` 分别在两个类里注册 | 合并到 `DataGenerators` |
| 空的 `accesstransformer.cfg`、4 个没有对应物品的附魔书模型 | 删除 |

## 待处理

| ID | 标题 | 优先级 | 状态 |
|---|---|---|---|
| [GF-022](GF-022-verify-2026-11-fixes-in-game.md) | 游戏内验证 2026-11 整理的修复 | P0 | 待验证 |
| [GF-006](GF-006-block-enchantments-cancel-break.md) | 挖掘附魔取消方块破坏：潜影盒内容丢失、未成熟作物刷产出、不掉耐久 | P0 | 待修复，Q12 |
| [GF-001](GF-001-upgrade-config-not-implemented.md) | 强化成功率、降级、损毁、材料消耗配置没有实现 | P1 | 待决议 Q3 |
| [GF-002](GF-002-reroll-preview-scumming.md) | 重铸结果可通过反复放取免费刷新 | P1 | 待决议 Q4 |
| [GF-003](GF-003-config-merge-overwrites-user-arrays.md) | 配置合并覆盖用户对数组字段的修改 | P1 | 待修复 |
| [GF-005](GF-005-arrow-enchantments-broken.md) | 箭矢附魔实现与描述不符 | P1 | 待修复 |
| [GF-007](GF-007-config-not-synced-to-client.md) | 配置不从服务端同步到客户端 | P1 | 待决议 Q7 |
| [GF-009](GF-009-lucky-diamond-drop-rates.md) | 幸运钻石地牢掉率 100% | P1 | 待决议 Q6 |
| [GF-010](GF-010-roll-affixes-crash-on-bad-config.md) | 配置不当时抽词条抛异常 | P1 | 待修复 |
| [GF-018](GF-018-no-automated-tests.md) | 没有自动化测试 | P1 | 待决议 Q13 |
| [GF-004](GF-004-unused-affix-gem-config.md) | 词条和宝石配置里有未使用的字段 | P2 | 待决议 Q5 |
| [GF-008](GF-008-hardcoded-chinese-in-tooltips.md) | 提示文本硬编码中文 | P2 | 待修复，Q16 |
| [GF-012](GF-012-cursed-anvil-behaviour.md) | 魔性铁砧效果范围过宽、附魔 -5 有副作用 | P2 | 待决议 Q8 |
| [GF-013](GF-013-flat-defense-applies-to-all-damage.md) | 固定减伤对摔落、溺水、饥饿等也生效 | P2 | 待决议 Q9 |
| [GF-014](GF-014-stick-counts-as-gear.md) | 木棍被当作装备 | P2 | 待决议 Q10 |
| [GF-015](GF-015-bow-charge-speed-also-scales-damage.md) | 弓蓄力加速同时放大箭伤 | P2 | 待决议 Q11 |
| [GF-019](GF-019-ci-and-release-process.md) | 没有 CI，版本号和发布流程不统一 | P2 | 待决议 Q14 |
| [GF-011](GF-011-creative-tab-order-and-dead-enchant-key.md) | 创造标签页顺序、charity_bundle 死引用 | P3 | 默认方案，Q16 |
| [GF-016](GF-016-unique-gem-effect-substring-match.md) | 宝石特殊效果靠字符串包含匹配 | P3 | 默认方案，Q16 |
| [GF-017](GF-017-max-health-affix-display-mismatch.md) | 生命上限词条显示值与实际差一倍 | P3 | 默认方案，Q16 |
| [GF-020](GF-020-package-naming.md) | 包命名不规范 | P3 | 默认方案，Q16 |
| [GF-021](GF-021-unused-lang-keys.md) | 语言文件里有大量未使用的键 | P3 | 依赖 GF-001 |

## 建议顺序

1. GF-022（先确认本次修复在游戏里没问题）
2. GF-006、GF-003、GF-010、GF-005：不需要决议或决议很小的 P0/P1
3. GF-018：在继续改玩法前先搭好测试，后面的修复都带测试
4. 按决议处理 GF-001、GF-002、GF-004、GF-007、GF-009
5. P2/P3 和 GF-019
