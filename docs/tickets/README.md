# 问题清单

整理时间：2026-11-10，二次整理：2026-11-22。优先级：P0 会导致物品丢失、刷物品或需要立即验证；P1 功能失效或明显错误；P2 设计或体验问题；P3 代码整洁。

**二次整理的修复已全部落地并通过 `./gradlew build` 与 `./gradlew runServer` 实测（服务器 `Done (4.240s)`）。** 仍在「待处理」的条目是尚未决议或刻意暂缓的。

## 已修复

### 2026-11-22 二次整理

| 问题 | 修复 | 对应 |
|---|---|---|
| 重铸结果在预览阶段掷骰，1 颗重铸石可无限刷词条 | 词条改由 `roll_salt` 派生确定种子，预览不掷骰，盐值只在真正取出后 +1 | GF-002 |
| 宝石镶嵌分支不校验模板槽，抢占原版钻石纹饰配方并吞掉纹饰模板 | 宝石/升阶/副手/附魔/摘除全部要求模板槽为空 | 新 |
| 模板槽无条件被扣 1，放钻石/下界合金块会被白吞 | 同上；`isTemplateIngredient` 不再接受钻石 | 新 |
| `matches` 与 `assemble` 是两套独立 if 链，可能出现"匹配成功但装配空转、材料照扣" | 抽出 `canApply` 作为唯一真源，两处共用；空附魔书不会再扣材料 | 新 |
| `assemble` 末尾兜底 `return result` 会原样复制底材 | 兜底改为返回 `ItemStack.EMPTY` | 新 |
| 配方产物由输入动态决定，却未标记 `isSpecial()`，会污染配方书/解锁 toast | 加 `isSpecial() = true` | 新 |
| 挖掘附魔取消方块破坏：潜影盒内容丢失、工具不掉耐久、无方块经验、可绕过保护 mod | 改用 `BlockDropsEvent` 追加/替换掉落，原版破坏流程完整保留 | GF-006 |
| 丰收附魔对未成熟作物生效，可站桩刷产出 | 只对 `isMaxAge` / `age == MAX_AGE` 的作物生效 | GF-006 |
| 箭矢附魔用 `getHolderOrThrow`，数据包删附魔后每次命中/破坏/tooltip 抛异常刷屏 | 改用 `getHolder(...)` 返回 `Optional`，缺失时返回 0 | 新 |
| 箭矢附魔按"当前手持武器"判定，射出后换手会改变结果 | 统一用 `arrow.getWeaponItem()`，与装备伤害路径共用同一逻辑 | GF-005 |
| 爆炸箭用 `setAmount(固定值)` 覆盖原伤害，强力弓反而只打 5~13 | 改为叠加 | GF-005 |
| 破甲箭/冰霜箭在 `LivingIncomingDamageEvent` 里嵌套 `hurt()`：占无敌帧、抵消外层箭伤、创造模式被扣血 | 破甲改用 `Reduction.ARMOR` 修饰器；冰霜直接叠加 | GF-005 |
| 4 个弓弩附魔的 `primary_items` 与 `supported_items` 不相交，附魔台永远拿不到 | `supported_items` 指向自定义标签 `#gear_forge:enchantable/arrow_weapons`（含 bow 与 crossbow） | 新 |
| `harvest_bounty` 引用不存在的 `#minecraft:enchantable/hoe`，静默解析为空集 | 改为 `#minecraft:hoes` | 新 |
| `all_stats` 同时走属性 + 固定加伤 + 固定减伤三条路径，1 点等于 4 份收益 | 只走属性路径（护甲/幸运/攻击/生命各 +1） | 新 |
| 转生保留的 7 个词条 id 从未被任何结算路径查询，提示显示却不生效 | `max_health`/`luck`/`knockback_boost`/`thorns` 接回结算 | 新 |
| 转生的 `gem_movement_speed` 被加了两次（`movement_speed` 已有 `gem_` 前缀回退） | 删除重复项 | 新 |
| `bow_charge_speed` 同时加速蓄力并在箭伤里叠乘，可叠加到十倍以上 | 只影响蓄力；箭伤不再随蓄力放大 | GF-015 |
| `Math.max(1, round(charge))` 让任何加成至少翻倍蓄力速度 | 改用小数累加器，长期推进速率与配置值一致 | GF-015 |
| 翡翠 `durability_boost` 既减损耗又每秒修复，T2 起护甲永不掉耐久 | 只减损耗；系数统一在 `durabilityDamageFactor` 钳制到 `[0.05, 1]` | 新 |
| 固定减伤对摔落/溺水/饥饿/魔法等 `#bypasses_armor` 伤害生效 | 跳过这些伤害；闪避只对有攻击者的伤害生效 | GF-013 |
| 掠夺复制只排除"有耐久"的物品，潜影盒（含整箱内容）/不死图腾/下界之星会被整件复制 | 排除容器类/药水/书类/唯一物；复制时剥掉容器内容与自定义名称；额外次数上限 3 | 新 |
| `LootModEvents` 与掠夺复制同事件无优先级，是否复制幸运钻石取决于注册顺序 | `LootModEvents` 标 `LOWEST` 且只认玩家击杀 | 新 |
| `rollAffixes` 在 `minAffixCount > maxAffixCount` 时抛 `IllegalArgumentException`（怪物加入世界/重铸/命令三处） | 全部钳制，非法配置返回空列表 | GF-010 |
| 配置合并把用户数组换回默认值，用户改不了 `valueRange` / `effects` | 数组按"用户优先"合并，只有具名集合才按 id 补全 | GF-003 |
| 配置每次启动无条件重写主文件和 `.old`，只读目录会静默回退默认 | 只在补全真的改动内容时才写回 | GF-003 |
| 配置不从服务端同步，客户端提示/属性表/蓄力预测与服务端结算不一致 | 新增 `gear_forge:config_sync` payload，登录时下发服务端快照 | GF-007 |
| 地下城箱子 100% 掉幸运钻石 | 改为 5% | GF-009 |
| 魔性铁砧注入的 `minecraft:chests/village/village_blacksmith` 在 1.21.1 不存在，永不触发 | 改为 `village_weaponsmith` + 新增 `village_toolsmith`，两条都 2% | 新 |
| Curios 槽位图标 `curios:slot/empty_custom_slot` 在 9.5.1 不存在，渲染成紫黑方块 | 改用 `curios:slot/empty_curio_slot` | 新 |
| `lucky_diamond` / `cursed_anvil` 模型绕过 datagen 手写，`runData` 无法重建完整资源 | 加进 `ModItemModelProvider`，删除手写副本 | 新 |
| 木棍被当作装备（`Items.STICK` 在 `isGearItem` 里） | 移除该分支；只认有耐久与弓弩/三叉戟/盾牌 | GF-014 |
| 强化怪物在概率判定之前就打持久标记，每只怪都往存档写一条 NBT | 只在真的强化时写 | 新 |
| 调试命令用 `word()` 无法输入带命名空间的实体 id；生成数量没有上限 | 改 `string()`；数量上限 64 | 新 |
| 包名 `Curios` / `Provider` 首字母大写，`ModGlobalLootModifiers` 放在 `component` 包 | `compat/curios`、`datagen/provider`、`loot`；`ModEnchantments` 移到 `content` | GF-020 |
| `.gitignore` 的 `!gradle/wrapper/gradle-wrapper.jar` 写在 `*.jar` **之前**，规则实际失效 | 移到文件末尾（gitignore 最后匹配者胜） | 新 |
| `runData` 在 Windows 写出 CRLF，每次生成都产生行尾假改动 | `.gitattributes` 对 `src/generated/**` 与 `*.json` 强制 `eol=lf` | 新 |
| `mixins.json` 的 `onTake` 注入未写全描述符 | 写成 `onTake(...Player;...ItemStack;)V` | 新 |
| 创造标签页顺序不确定：附魔页与饰品页都声明"排在核心页之前" | 改成无环链：附魔 → 饰品 → 核心 → 战斗 | GF-011 |
| `charity_bundle` 是死键（无附魔 JSON、无 lang、无行为） | 从 `ModEnchantments` 与创造页移除 | GF-011 |
| 生命上限词条属性侧乘 2，提示值只有实际的一半 | 去掉 ×2；`all_stats` 的生命加成同样改为 ×1 | GF-017 |
| 语言文件有 19 条无引用的 `message.gear_forge.*` 键 | 删除；en/zh 各剩 60 条且键集合一致 | GF-021 |
| 强化成功率、降级、损毁、材料消耗 11 个配置字段完全没被读取，强化永远 100% 成功且只消耗 1 个锻造锤 | 全部接入：材料按等级递增（maxConsumeMaterialPerLevel 封顶）；成功/降级/损毁在取出时掷骰，预览固定 +1 不可刷；客户端不参与结算 | GF-001 |
| 魔性铁砧对"所有非 Enemy 的 Mob"生效，会让自己的狗/猫、村民、牛羊反咬玩家 | 只对原版 NeutralMob 生效，排除 OwnableEntity 有主人的个体 | GF-012 |
| 魔性铁砧的"附魔 -5"实为门槛与附魔强度同时下降，tooltip 却只写"等级 -5" | 核实原版 costs 双用途后保留现状，tooltip 改为"等级需求 -5（附魔强度同步降低）" | GF-012 |
| 附魔台用 10 格内最近玩家近似操作者，路过的人也会影响别人的附魔 | 半径收紧到 4 格（与原版交互距离一致） | GF-012 |
| isTemplateIngredient 只接受转生材料，钻石放不进模板槽，导致强化完全不可用（GF-001 引入的回归） | 放开为覆盖所有操作需要的模板材料 | GF-023 |
| 原版锻造台与纹饰/下界合金升级及其它模组争抢同一份 SMITHING 配方列表，玩家无法预期 | 新增独立方块 gear_forge:forge_table（强化台），与原版锻造台共用同一套结算逻辑 | GF-023 |

### 2026-11-10 首次整理

| 问题 | 修复 |
|---|---|
| 锻造台底材放一整组（例如 64 根木棍）时，产物也是一整组，但只扣 1 个，可以无限复制 | `assemble` 只复制 1 个底材 |
| 转生要求 8 个下界合金块，实际只扣 1 个 | 新增 `SmithingMenuMixin`，取出时补扣 |
| 强化的攻击/护甲/韧性、转生保留的攻击和护甲，既挂了属性又在伤害事件里再加一遍，实际是提示值的两倍 | 只保留属性修饰符（ADR-0004） |
| 几件装备上同名属性修饰符 id 相同，四件盔甲的强化护甲只生效一件，脱下一件会把其它件的也移除 | 修饰符 id 加槽位后缀 |
| 盔甲属性挂在 `ANY` 槽，拿在手里也生效 | 只在自己的护甲槽生效 |
| 多颗宝石的移速加成只算第一颗 | 先累加再挂一次 |
| 两个都有荆棘词条的实体互相攻击时无限反弹 | 荆棘伤害不再触发荆棘 |
| 高强化装备的固定减伤能抵消虚空和 `/kill` 伤害 | 跳过 `#bypasses_invulnerability` 伤害 |
| 雷电词条的雷暴加成在设置伤害之后才计算，不生效 | 移到设置伤害之前 |
| 掠夺词条会复制玩家死亡时的整个背包，以及怪物身上掉落的装备 | 玩家死亡不参与；装备类不参与 |
| 经验加成词条按"掉落物数量 × 比例"取整，几乎总是 0 | 改用 `LivingExperienceDropEvent` 按比例加经验 |
| 魔性铁砧的经验 +15% 对 1–6 点的经验球被截断成 0 | 小数部分按概率补足 |
| 魔性铁砧会把附魔台不可用的槽位（等级 0）变成 1 级 | 等级 0 的槽位不处理 |
| 强化怪物在 `FinalizeSpawnEvent` 时处理，此时怪物还没有装备 | 改为怪物加入世界时处理；词条列表改为不可变列表 |
| 拉弓时每 tick 在服务端和客户端各打一条 INFO 日志 | 删除调试日志 |
| 箭矢伤害按当前手持的弓计算 | 优先用箭记录的发射武器 |
| 配置里宝石缺少字段时提示文本空指针 | 判空 |
| 寒冰类减速效果的等级可能为负 | 下限为 0 |
| `neoforge.mods.toml` 依赖段写错，三条依赖声明全部无效 | 改用 `${mod_id}`，Curios 改为 `type="required"` |
| `gradle-wrapper.jar` 被 `*.jar` 规则忽略 | 重新生成并提交（**但 `.gitignore` 顺序问题当时未真正修好，见上表**） |
| `build/`、`.gradle/`、`.idea/`、`run/` 共 585 个文件被提交 | 移出版本库，重写 `.gitignore` |
| `src/main/resources` 和 `src/generated/resources` 各有一份内容不同的资源 | 删除手写副本，去掉 `INCLUDE`（ADR-0006） |
| 数据生成缓存 `.cache/` 被打进 jar | 修正排除规则 |
| `mod_license=All Rights Reserved` 与 MIT 的 `LICENSE` 冲突 | 改为 MIT |
| 显示名、作者、描述没有写进元数据 | 补上 |
| `build.gradle` 有重复的 `runs` 块、硬编码 Curios 版本 | 清理 |
| `GatherDataEvent` 分别在两个类里注册 | 合并到 `DataGenerators` |
| 空的 `accesstransformer.cfg`、4 个没有对应物品的附魔书模型 | 删除 |

## 待处理

| ID | 标题 | 优先级 | 状态 |
|---|---|---|---|
| [GF-004](GF-004-unused-affix-gem-config.md) | 词条和宝石配置里有未使用的字段 | P2 | 待决议 |
| [GF-008](GF-008-hardcoded-chinese-in-tooltips.md) | 提示文本与配置里的名称/描述硬编码中文 | P2 | 待修复 |
| [GF-016](GF-016-unique-gem-effect-substring-match.md) | 宝石特殊效果靠字符串包含匹配 | P3 | 待修复 |
| [GF-018](GF-018-no-automated-tests.md) | 没有自动化测试 | P2 | 待决议 |
| [GF-019](GF-019-ci-and-release-process.md) | 没有 CI，版本号和发布流程不统一 | P2 | 待决议 |
| [GF-022](GF-022-verify-2026-11-fixes-in-game.md) | 游戏内逐项验证整理后的行为 | P0 | 待验证 |

## 建议顺序

1. GF-022：先按 [docs/development.md](../development.md) 第 4 节的回归清单在游戏里逐项过一遍。构建、`runData`、`runServer` 已经通过（`Done (3.101s)`，0 ERROR，两个 mixin 均注入成功），但玩法行为需要人工确认。
2. GF-008、GF-016：不需要决议的 P2/P3 清理。
3. GF-018：需要作者决定测试投入。
4. GF-019：CI 与发布流程。
