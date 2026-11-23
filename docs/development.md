# 开发、测试与发布流程

本文分两部分：**现状**是仓库目前实际能做的事；标了“待定”的部分取决于 [tickets](tickets/) 里还没决议的事项。

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
| `./gradlew runGameTestServer` | 运行 GameTest 后退出。**目前没有任何 GameTest，会以 “No test functions were given!” 失败** |

`run/` 整个目录都不进版本库（存档、日志、配置、下载的资源）。

## 3. 代码和资源约定

- 物品模型、合成配方、Curios 槽位由数据生成器产出。改了 `Provider/` 下的 provider 后运行 `runData`，把 `src/generated/resources` 的变化一起提交；`.cache/` 不提交。
- 不要在 `src/main/resources` 放和生成结果同路径的文件。`processResources` 没有设置重复处理策略，同路径文件会让构建失败（这是有意的，见 ADR-0006）。
- 模组元数据只改 `gradle.properties`（版本、名称、作者、描述、依赖版本范围），模板 `src/main/templates/META-INF/neoforge.mods.toml` 一般不用动。注意模板里的注释也会被当成 Groovy 模板解析，不能出现 `${`。
- 新增 mixin 时同步登记到 `gear_forge.mixins.json`。注入目标用 Mojang 名（NeoForge 1.21.1 运行时就是 Mojang 映射，不需要 refmap）。
- 游戏内配置在 `run/config/gear_forge/*.json`，只在启动时读取，改完要重启。

## 4. 测试

### 现状

- 没有单元测试，也没有 GameTest（`src/test` 不存在）。
- 验证全靠手动：`runClient` 进 `run/saves` 里的测试存档，用 `/gear_forge test spawnmob` 生成带装备的怪物。

### 冒烟检查（每次改动后至少做）

1. `./gradlew build` 通过。
2. `./gradlew runData` 后 `git status` 里 `src/generated` 没有意外变化。
3. `./gradlew runGameTestServer`：在没有 GameTest 之前，它会在加载完所有 mod 和 mixin 之后才因为没有测试而退出，可以用来检查 mod 能否加载、mixin 能否注入。设置环境变量 `JAVA_TOOL_OPTIONS=-Dmixin.debug.verbose=true` 后，日志里应该出现 `Mixing LivingEntityMixin ...` 和 `Mixing SmithingMenuMixin ...`，且没有 `FATAL` / `InvalidInjectionException`。

### 2026-11 整理改动的手动回归清单

这些改动已经通过编译和加载检查，但还没有进游戏实际验证：

- [ ] 一组 64 根木棍 + 锻造锤：结果格只有 1 根 +1 木棍，取出后底材剩 63。
- [ ] 一组宝石 + 升阶水晶：产出 1 个升阶宝石，底材少 1。
- [ ] 满级装备 + 转生石 + 8 个下界合金块：转生成功后模板格的 8 个全部消耗；放 9 个剩 1；放 7 个结果格为空。
- [ ] 四件都强化过的盔甲：护甲值等于四件之和；脱下一件只减少那一件的数值。
- [ ] 强化过的胸甲拿在手里：护甲值不变。
- [ ] +10 剑的实际伤害等于提示文本里的攻击伤害（不再额外加一遍强化攻击）。
- [ ] 两个都穿荆棘词条盔甲的实体互打：不卡死、不栈溢出。
- [ ] 高强化 + 多宝石的盔甲掉进虚空：正常死亡。
- [ ] 困难难度下僵尸/骷髅出生时偶尔带强化装备（`mob_gear.json` 概率可临时调到 1.0 验证）。
- [ ] 带经验加成词条的武器击杀：掉落经验按比例增加。
- [ ] 带魔性铁砧拾取经验球：长期平均多 15%。
- [ ] 带掠夺词条的武器杀死穿装备的僵尸：普通掉落被复制，装备不被复制。
- [ ] 拉弓时日志不再刷 `[GearForge-Mixin]`。

### 目标（待定，见 GF-018）

- 纯逻辑（配置合并、词条抽取、数值汇总）用 JUnit。
- 锻造配方、属性叠加、伤害结算用 NeoForge GameTest，放进 `runGameTestServer`。
- CI 上跑 `build` 和 `runGameTestServer`。

## 5. 版本与发布

### 现状

- 版本号只在 `gradle.properties` 的 `mod_version` 里维护，目前是 `1.6.51`。
- 已有 tag：`1.6.0`、`1.6.1`、`1.6.2`、`1.6.4`、`1.64fix`、`1.65`、`1.651`，命名不统一，和 `mod_version` 也对不上。
- 没有 CI，没有 changelog，提交信息多为 “update”。
- 发布渠道（GitHub Releases、Modrinth、CurseForge、MC 百科等）在仓库里没有记录。

### 手动发布步骤（在 GF-019 决议前沿用）

1. 改 `gradle.properties` 的 `mod_version`。
2. `./gradlew build`，走一遍第 4 节的冒烟检查和相关回归项。
3. 提交，打 tag，推送。
4. 把 `build/libs/gear_forge-<版本>.jar` 上传到发布渠道。

## 6. 提交前检查

- `git status` 里不应出现 `build/`、`.gradle/`、`.idea/`、`run/`、`*.jar`（`gradle/wrapper/gradle-wrapper.jar` 除外）。
- 本地 AI 工具状态（`.feisuan/`、`modmind.project.json`、`docs/last-ai-response.txt`）已被忽略，不要强制加入。
