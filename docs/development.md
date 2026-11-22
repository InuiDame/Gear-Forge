# 开发、测试与发布流程

本文分两部分：**现状**是仓库目前实际能做的事；标了"待定"的部分取决于 [tickets](tickets/) 里还没决议的事项。

## 1. 环境准备

- JDK 21。Gradle toolchain 会固定用 21 编译，本机没有时 foojay 插件会尝试自动下载。
- 不需要单独安装 Gradle。`gradlew` / `gradlew.bat` 会按 `gradle/wrapper/gradle-wrapper.properties` 从阿里云镜像下载 Gradle 9.2.0。
- 首次构建需要联网，要访问：NeoForge Maven（NeoForge、NeoForm、Parchment）、`maven.theillusivec4.top`（Curios）、Gradle 插件门户。之后可以加 `--offline`。
- 之前的 AI 日志里记录过 Gradle 证书校验失败的问题。遇到时先检查代理和系统时间，不要在 `gradle.properties` 里关闭 SSL 校验。

### IntelliJ IDEA

1. 直接打开仓库根目录，按 Gradle 项目导入。
2. 同步时会自动执行 `generateModMetadata`，并由 ModDevGradle 生成 `Client` / `Server` / `Data` / `GameTestServer` 运行配置。
3. `.idea/`、`*.iml` 不进版本库，换机器后重新导入即可。

## 2. 常用任务

| 命令 | 作用 |
|---|---|
| `./gradlew build` | 编译并打包，产物 `build/libs/gear_forge-<版本>.jar` |
| `./gradlew runClient` | 开发客户端，游戏目录是 `run/` |
| `./gradlew runServer` | 开发服务端（首次需要在 `run/eula.txt` 写 `eula=true`） |
| `./gradlew runData` | 重新生成 `src/generated/resources` |
| `./gradlew runGameTestServer` | 运行 GameTest 后退出。**目前没有任何 GameTest，会以 "No test functions were given!" 失败** |

`run/` 整个目录都不进版本库（存档、日志、配置、下载的资源）。

## 3. 代码和资源约定

- 物品模型、合成配方、Curios 槽位由数据生成器产出。改了 `datagen/provider/` 下的 provider 后运行 `runData`，把 `src/generated/resources` 的变化一起提交；`.cache/` 不提交。
- 不要在 `src/main/resources` 放和生成结果同路径的文件（ADR-0006）。**注意**：这条靠纪律保证，不靠构建系统——Gradle 在没有显式设置 `DuplicatesStrategy` 时默认是 `INCLUDE` 而非 `FAIL`，出现重复不会报错，只会静默按复制顺序覆盖。复核方式：
  ```
  git diff --name-only -- src/main/resources src/generated/resources
  ```
  两边不应该有同路径文件。
- 模组元数据只改 `gradle.properties`（版本、名称、作者、描述、依赖版本范围），模板 `src/main/templates/META-INF/neoforge.mods.toml` 一般不用动。注意模板里的注释也会被当成 Groovy 模板解析，不能出现 `${`。
- 新增 mixin 时同步登记到 `gear_forge.mixins.json`。注入目标用 Mojang 名（NeoForge 1.21.1 运行时就是 Mojang 映射，不需要 refmap），并且**写全方法描述符**，避免将来出现同名重载时注入到错误目标。
- 游戏内配置在 `run/config/gear_forge/*.json`，服务端启动时读取。登录时会把服务端快照下发给客户端（ADR-0007），所以联机时以服务端文件为准；改完要重启服务端。
- 附魔的 `supported_items` / `primary_items` 如果要同时覆盖弓和弩，**不能写成数组**——`HolderSet` 的数组写法不接受带 `#` 的标签条目，会直接让服务器启动失败（`Not a valid resource location: #minecraft:...`）。正确做法是新建一个物品标签（参考 `data/gear_forge/tags/item/enchantable/arrow_weapons.json`）再整体引用。
- 附魔等级一律用 `registry.getHolder(key)` 而不是 `getHolderOrThrow`，这样数据包删掉某条附魔时返回 0 而不是每次命中/破坏都抛异常。

## 4. 测试

### 现状

- 没有单元测试，也没有 GameTest（`src/test` 不存在）。
- 验证全靠手动：`runClient` 进 `run/saves` 里的测试存档，用 `/gear_forge test spawnmob` 生成带装备的怪物。

### 冒烟检查（每次改动后至少做）

1. `./gradlew build` 通过。
2. `./gradlew runData` 后 `git status` 里 `src/generated` 没有意外变化。
3. `./gradlew runServer` 能走到 `Done (Xs)! For help, type "help"`。这一步比 `runGameTestServer` 更有用：它会真正加载所有数据包（附魔、标签、战利品修改器、配方），
   数据写错会在这里直接崩，而 `runGameTestServer` 也会崩但错误信息淹没在 GameTest 的失败里。
4. 设置环境变量 `JAVA_TOOL_OPTIONS=-Dmixin.debug.verbose=true` 后，日志里应该出现 `Mixing LivingEntityMixin ...` 和 `Mixing SmithingMenuMixin ...`，且没有 `FATAL` / `InvalidInjectionException`。

### 2026-11-22 二次整理的手动回归清单

构建、`runData`、`runServer` 全部已通过；下面是需要进游戏亲眼确认的行为项。

**锻造台**

- [ ] 一组 64 根木棍 + 锻造锤：结果格只有 1 根 +1 木棍，取出后底材剩 63。
- [ ] 一组宝石 + 升阶水晶：产出 1 个升阶宝石，底材少 1。
- [ ] 满级装备 + 转生石 + 8 个下界合金块：转生成功、模板格清空；放 9 个剩 1；放 7 个结果格为空。
- [ ] 强化/重铸/镶嵌/升阶/副手/附魔书/摘除这七种操作，模板槽放钻石时**结果格为空**（不再白吞钻石）。
- [ ] 护甲 + 任意纹饰模板 + 钻石：结果仍是原版纹饰，不会被当成宝石镶嵌，纹饰模板不被吞。
- [ ] 重铸：放入重铸石看到一套词条后，把重铸石拿出再放回，**词条不变**；取出后再放回，词条才变。
- [ ] 空附魔书（`/give @s minecraft:enchanted_book`）+ 带 `enchant_compat` 宝石的装备：结果格为空，材料不被吞。

**强化成功率（GF-001 / ADR-0009）**

- [ ] 0→5 级：每次必定成功，模板格分别消耗 1/2/3/4/5 个钻石。
- [ ] 6→15 级：每级消耗 5 个钻石（maxConsumeMaterialPerLevel 封顶），成功率从 75% 递减到 30%。
- [ ] 强化失败：等级不变，actionbar 提示"强化失败，等级不变"。
- [ ] 把 llowDowngrade 设为 false 后失败不再降级；设为 true 时约 10% 的失败会降 1 级并提示。
- [ ] 把 destroyChance 设为 1.0、llowDestroy 设为 true：取出后装备消失并提示"装备已损毁"。
- [ ] 把 consumeMaterial 设为空字符串：模板槽必须为空，强化不消耗材料（回到免费强化）。
- [ ] 预览固定显示 +1，反复放取锻造锤不改变结果；只有真正取出才掷骰。

**魔性铁砧（GF-012）**

- [ ] 戴着魔性铁砧站在自己驯服的狼/猫旁边：宠物不会攻击自己。
- [ ] 站在村民、牛羊、雪傀儡旁边：它们完全不受影响。
- [ ] 站在野生狼/末影人/猪灵/蜜蜂/北极熊旁边：它们会主动攻击。
- [ ] 打开附魔台：tooltip 与界面等级需求都体现 -5，附魔强度同步降低。
- [ ] 4 格外的另一名玩家戴着魔性铁砧，不影响本玩家的附魔台结果。

**属性与伤害**

- [ ] 四件都强化过的盔甲：护甲值等于四件之和；脱下一件只减少那一件的数值。
- [ ] 强化过的胸甲拿在手里：护甲值不变。
- [ ] +10 剑的实际伤害等于提示文本里的攻击伤害。
- [ ] 镶 1 颗钻石（`all_stats`）：护甲、幸运、攻击各 +1，生命 +1，且受击时不再额外减伤。
- [ ] 带 `max_health` / `luck` / `knockback_boost` / `thorns` 词条的装备转生后，`[转生]` 那几行数值确实生效。
- [ ] 穿戴 1 颗 T2 翡翠的护甲：仍然会掉耐久（不再永不磨损）；`auto_repair` 词条仍是唯一的自动修复来源。

**生存类**

- [ ] 两个人各穿荆棘词条盔甲互打：不卡死、不栈溢出。
- [ ] 高强化 + 多宝石的盔甲掉进虚空、`/kill`、摔落、溺水：前两者必死，后两者不应被固定减伤完全免疫。

**附魔**

- [ ] 附魔台能把闪电/霜冻/爆炸/破甲附到**弓**上（此前只能付到弩）。
- [ ] 丰收附魔只对成熟作物触发；对刚种下的小麦破坏不产生额外产物。
- [ ] 带挖矿暴击的镐挖潜影盒：内容正常掉落，工具掉耐久，方块经验正常。
- [ ] 爆炸箭对强力弓：箭伤 = 弓的基础伤害 + 爆炸额外伤害，不再被抹成 5~13。
- [ ] 破甲箭对穿钻甲的目标：伤害提高但目标不会被无视无敌帧连击；创造模式玩家不会被扣血。

**战利品与怪物**

- [ ] 困难难度下僵尸/骷髅出生时偶尔带强化装备（`mob_gear.json` 概率可临时调到 1.0 验证）。
- [ ] 带掠夺词条的武器杀死穿装备的僵尸：普通掉落被复制，装备不被复制，潜影盒不被整件复制。
- [ ] 僵尸 2% 掉幸运钻石，且只在玩家击杀时掉。
- [ ] 村庄武器匠/工具匠箱子偶尔出魔性铁砧（2%），地牢箱子偶尔出幸运钻石（5%）。

**多人**

- [ ] 服务端改 `affixes.json` 里的某个数值后重启，客户端连上后提示文本显示的是**服务端**的数值。
- [ ] 客户端断线回主菜单后，提示文本恢复成客户端本地配置的数值。

### 目标（待定，见 GF-018）

- 纯逻辑（配置合并、词条抽取、数值汇总、耐久系数钳制）用 JUnit。
- 锻造配方、属性叠加、伤害结算用 NeoForge GameTest，放进 `runGameTestServer`。
- CI 上跑 `build` 和 `runServer` 启动检查。

## 5. 版本与发布

### 现状

- 版本号只在 `gradle.properties` 的 `mod_version` 里维护，目前是 `1.6.51`。
- `modmind.project.json` 里的 `projectVersion` / `loaderVersion` 是过期快照（1.1.3 / 21.1.244），**不要**据此改 `neo_version`；版本真值只以 `gradle.properties` 为准。
- 已有 tag：`1.6.0`、`1.6.1`、`1.6.2`、`1.6.4`、`1.64fix`、`1.65`、`1.651`，命名不统一，和 `mod_version` 也对不上。
- 没有 CI，没有 changelog，提交信息多为 "update"。
- 发布渠道（GitHub Releases、Modrinth、CurseForge、MC 百科等）在仓库里没有记录。

### 手动发布步骤（在 GF-019 决议前沿用）

1. 改 `gradle.properties` 的 `mod_version`。
2. `./gradlew build`，走一遍第 4 节的冒烟检查和相关回归项。
3. 提交，打 tag，推送。
4. 把 `build/libs/gear_forge-<版本>.jar` 上传到发布渠道。

## 6. 提交前检查

- `git status` 里不应出现 `build/`、`.gradle/`、`.idea/`、`run/`、`*.jar`（`gradle/wrapper/gradle-wrapper.jar` 除外）。
- 确认 wrapper jar 没被误忽略：
  ```
  git check-ignore -v --no-index gradle/wrapper/gradle-wrapper.jar
  ```
  应输出 `!gradle/wrapper/gradle-wrapper.jar`（否定规则命中 = 不被忽略）。
- `git diff --name-only -- src/main/resources src/generated/resources` 两边不应有同路径文件。
- 本地 AI 工具状态（`.feisuan/`、`modmind.project.json`、`docs/last-ai-response.txt`）已被忽略，不要强制加入。
