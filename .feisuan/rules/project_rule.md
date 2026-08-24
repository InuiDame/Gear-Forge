
# Gear Forge 项目开发规则

> 本规则文件根据当前项目配置、工作环境与目录结构整理。  
> **代码作者：Admin**  
> **规则快照时间：2026-08-11 00:13:08**

## 一、项目环境与工作目录

| 项目 | 配置 |
|---|---|
| 操作系统 | Windows 10 |
| 工作目录 | `H:\下载\Gear Forge` |
| 项目类型 | Minecraft NeoForge Mod |
| Mod ID | `gear_forge` |
| Java 包根路径 | `com.hana.gear_forge` |
| 构建工具 | Gradle Wrapper |
| 构建脚本 | `build.gradle`，Groovy DSL |
| Java SDK | JDK 21.0.11 |
| Java 编译版本 | Java 21 |
| 代码作者 | Admin |

所有命令默认在以下目录执行：

~~~bat
cd /d "H:\下载\Gear Forge"
~~~

项目路径包含空格，脚本、IDE 配置及外部工具引用该路径时必须正确使用引号。源代码中不得硬编码本机绝对路径。

---

## 二、技术栈与版本要求

### 2.1 核心技术栈

- **Minecraft Mod 平台**：NeoForge。
- **NeoForge ModDev Gradle 插件**：`2.0.141`。
- **Java**：JDK 21.0.11，Gradle Toolchain 固定为 Java 21。
- **构建工具**：Gradle Wrapper，发行类型为 `BIN`。
- **Java 插件**：`java-library`。
- **发布插件**：`maven-publish`。
- **IDE 插件**：`idea`。
- **映射**：使用 Parchment，版本由以下 Gradle 属性控制：
  - `parchment_mappings_version`
  - `parchment_minecraft_version`
- **NeoForge 版本**：由 `project.neo_version` 控制。
- **Minecraft 版本**：由 `minecraft_version` 和 `parchment_minecraft_version` 控制。当前 Curios 依赖包含 `1.21.1`，因此项目目标版本应以 Minecraft 1.21.1 配置为准；如 `gradle.properties` 中存在差异，以配置文件实际值为准。

### 2.2 构建插件约束

当前 `build.gradle` 使用以下插件：

- `java-library`
- `maven-publish`
- `net.neoforged.moddev`，版本 `2.0.141`
- `idea`

未经确认不得随意升级 NeoForge、ModDev、Minecraft 或 Parchment 版本。升级时必须同步检查：

1. NeoForge API 是否发生不兼容变更。
2. Parchment 映射是否匹配 Minecraft 版本。
3. Curios 依赖是否存在对应版本。
4. 数据生成、客户端运行及服务端运行是否正常。

Gradle 版本以 `gradle/wrapper/gradle-wrapper.properties` 中的配置为准，不应依赖开发机全局安装的 Gradle。

---

## 三、项目目录结构

以下目录结构以当前项目快照为准。`run` 目录主要用于开发运行时数据，不能作为业务源代码目录。

~~~text
Gear Forge/
├── build.gradle
├── .modmind/
│   └── minecraft/
│       └── mods/
├── docs/
├── gradle/
│   └── wrapper/
├── run/
│   ├── config/
│   │   └── gear_forge/
│   ├── defaultconfigs/
│   ├── downloads/
│   ├── logs/
│   │   └── telemetry/
│   ├── mods/
│   ├── resourcepacks/
│   └── saves/
│       └── 新的世界/
│           ├── advancements/
│           ├── data/
│           ├── datapacks/
│           ├── DIM-1/
│           │   └── data/
│           ├── DIM1/
│           │   └── data/
│           ├── entities/
│           ├── playerdata/
│           ├── poi/
│           ├── region/
│           ├── serverconfig/
│           └── stats/
└── src/
    ├── generated/
    │   └── resources/
    │       ├── assets/
    │       │   ├── gear_forge/
    │       │   │   └── models/
    │       │   │       └── item/
    │       │   └── minecraft/
    │       │       └── models/
    │       │           └── item/
    │       └── data/
    │           └── gear_forge/
    │               ├── advancement/
    │               │   └── recipes/
    │               │       ├── misc/
    │               │       └── tools/
    │               ├── curios/
    │               │   └── slots/
    │               └── recipe/
    └── main/
        ├── java/
        │   └── com/
        │       └── hana/
        │           └── gear_forge/
        │               ├── command/
        │               ├── component/
        │               ├── config/
        │               ├── Curios/
        │               ├── datagen/
        │               ├── event/
        │               ├── item/
        │               │   └── custom/
        │               ├── loot/
        │               ├── mixin/
        │               ├── Provider/
        │               ├── recipe/
        │               └── util/
        ├── resources/
        │   ├── assets/
        │   │   └── gear_forge/
        │   │       ├── lang/
        │   │       ├── models/
        │   │       │   └── item/
        │   │       └── textures/
        │   │           └── item/
        │   ├── data/
        │   │   ├── curios/
        │   │   │   └── tags/
        │   │   │       └── item/
        │   │   ├── gear_forge/
        │   │   │   ├── curios/
        │   │   │   │   ├── entities/
        │   │   │   │   └── slots/
        │   │   │   ├── enchantment/
        │   │   │   ├── loot_modifiers/
        │   │   │   └── recipe/
        │   │   └── neoforge/
        │   │       └── loot_modifiers/
        │   └── META-INF/
        └── templates/
            └── META-INF/
~~~

### 3.1 目录职责

| 目录 | 职责 |
|---|---|
| `src/main/java` | 手写 Java 源代码 |
| `src/main/resources` | 手写资源、语言文件、模型、纹理、数据包及元数据模板 |
| `src/generated/resources` | 数据生成器输出的资源，不应作为主要手写源文件 |
| `src/main/templates` | Mod 元数据模板，由 Gradle 任务生成最终资源 |
| `datagen` | 数据生成逻辑及 Provider |
| `item/custom` | 自定义物品及物品行为 |
| `component` | 物品、实体或其他游戏对象的数据组件逻辑 |
| `command` | Brigadier/NeoForge 命令 |
| `config` | Mod 配置相关代码 |
| `event` | NeoForge 事件处理 |
| `Curios` | Curios API 集成 |
| `loot` | 战利品表及战利品修改器相关代码 |
| `recipe` | 配方相关代码 |
| `mixin` | Mixin 注入代码，仅在 NeoForge 配置允许且确有必要时使用 |
| `util` | 通用工具类，禁止堆放无明确职责的业务逻辑 |
| `run` | 客户端、服务端及世界存档等本地运行数据，不应混入业务代码 |

现有的 `Curios`、`Provider` 包名使用了大写字母。新增包必须使用全小写命名；旧包如需迁移，应通过独立重构完成，避免边开发边造成包路径混乱。

---

## 四、依赖管理规范

### 4.1 当前依赖

| 依赖 | Gradle 配置 | 使用规则 |
|---|---|---|
| Curios NeoForge API | `compileOnly` | 编译期使用 API，不得依赖实现细节 |
| Curios NeoForge 完整包 | `runtimeOnly` | 当前用于开发运行时 |
| NeoForge/Minecraft | 由 NeoForge ModDev 插件提供 | 必须使用当前映射和 NeoForge API |

当前 Curios 依赖为：

~~~groovy
runtimeOnly "top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1"
compileOnly "top.theillusivec4.curios:curios-neoforge:9.5.1+1.21.1:api"
~~~

### 4.2 依赖规则

- Curios 相关代码优先依赖 API，禁止直接调用 Curios 实现类或内部实现。
- 如果 Curios 是可选依赖，相关功能必须隔离，并在运行时检查 Mod 是否存在，避免在没有 Curios 时触发类加载错误。
- 当前 `build.gradle` 定义了 `localRuntime` 配置，并使 `runtimeClasspath` 继承该配置。仅用于开发测试、不应被发布者传递的依赖，优先使用 `localRuntime`。
- 不得随意将开发期依赖声明为 `implementation` 或 `api`。
- 只有当依赖类型出现在对外暴露的 Java API 中时，才考虑使用 `api`；普通内部实现优先使用 `implementation`。
- 新增 Maven 仓库前必须确认依赖确实无法从现有仓库获取，并说明原因。
- 当前额外仓库为 Curios Maven 仓库，除必要情况外不得添加其他外部仓库。
- 依赖版本统一放在 Gradle 属性或版本目录中管理，禁止在多个文件中重复硬编码。
- 项目未声明 Spring Boot、Spring Data JPA、Lombok 等依赖：
  - 不得使用 Controller、Service、Repository、Entity 等 Spring 分层规范。
  - 不得使用 `@Transactional`、`@Valid`、JPA 注解或 Lombok 注解。
  - 如确需增加依赖，必须同步更新构建规则和依赖说明。

---

## 五、NeoForge 代码架构规范

### 5.1 生命周期与注册

- 所有物品、方块、配方、数据组件等注册内容必须在 NeoForge 规定的生命周期内完成。
- 优先使用 NeoForge 推荐的注册机制，例如 `DeferredRegister` 或对应注册事件。
- 注册对象应集中管理，避免在普通业务方法中临时注册。
- 静态初始化代码不得执行耗时操作、访问世界对象或依赖客户端环境。
- 必须区分 Mod Event Bus 与游戏事件总线，事件注册到错误总线时应及时修正。
- Mod ID、注册名称和资源路径必须使用统一的 `gear_forge` 命名空间。

### 5.2 客户端与服务端隔离

- 服务端不能加载客户端专用类，例如渲染器、屏幕、键位及客户端模型相关类。
- 客户端逻辑应放在独立的客户端初始化代码中，并使用 NeoForge 的环境判断机制。
- 服务端必须是游戏状态和玩家行为的最终权威，不能信任客户端传来的数量、位置、权限或奖励数据。
- 涉及网络数据包时必须校验发送者、世界、权限、数据范围和执行线程。
- 不得在服务端代码中直接引用仅客户端存在的类。

### 5.3 事件、命令与 Mixin

- 事件处理器只处理当前事件职责，复杂业务应委托给独立方法或功能类。
- 命令必须进行权限检查、参数校验和异常处理。
- 命令反馈应使用 Minecraft/NeoForge 的消息系统，不得向控制台直接打印普通业务信息。
- 能通过公开事件或扩展点实现的功能，不优先使用 Mixin。
- Mixin 必须记录：
  - 注入目标及原因。
  - 注入时机。
  - 兼容性风险。
  - 失败后的影响。
- Mixin 注入应尽可能小，禁止大范围覆盖原版逻辑。

---

## 六、数据生成与资源规范

### 6.1 资源目录

- 手写资源放在 `src/main/resources`。
- 数据生成资源放在 `src/generated/resources`。
- `build.gradle` 已将 `src/generated/resources` 加入主资源源集，不得删除该配置。
- 生成结果应通过数据生成器维护，禁止直接修改生成文件后又覆盖生成器结果。
- 资源命名必须符合 Minecraft ResourceLocation 约束：
  - 使用小写字母、数字、下划线、短横线和路径分隔符。
  - 禁止使用空格、中文或大写字母作为资源 ID。
- Mod 自有资源使用 `gear_forge` 命名空间，不得误放到 `minecraft` 命名空间。
- 语言文件、模型、纹理、配方、战利品表和标签必须保持路径与注册名称一致。

### 6.2 数据生成

数据生成运行配置会将输出写入：

~~~text
src/generated/resources/
~~~

运行数据生成：

~~~bat
gradlew.bat runData
~~~

数据生成器必须：

- 使用现有 `src/main/resources` 作为已有资源输入。
- 不覆盖不属于自身职责的手写资源。
- 保证重复执行结果稳定。
- 避免将临时文件、缓存文件写入最终资源目录。
- 修改生成规则后重新运行数据生成，并检查生成文件变更是否符合预期。

### 6.3 Mod 元数据

- `src/main/templates/META-INF` 中的文件属于模板，不是最终展开后的资源。
- `generateModMetadata` 任务会替换以下属性：
  - `minecraft_version`
  - `minecraft_version_range`
  - `neo_version`
  - `loader_version_range`
  - `mod_id`
  - `mod_name`
  - `mod_license`
  - `mod_version`
- 新增模板占位符时，必须同步加入 `generateModMetadata` 的 `replaceProperties`。
- 缺失属性会导致资源处理失败，不得通过硬编码绕过属性替换。
- IDE 同步会执行 `generateModMetadata`，不得手动提交 `build/generated` 下的临时结果。

### 6.4 开发文件排除

最终资源会排除以下开发文件或目录：

- `**/*.bbmodel`
- `src/generated/**/.cache`
- `opencode.json`
- `INSTRUCTIONS.md`
- `mc-src/**`
- `.opencode/**`

新增开发辅助文件时，应确认不会被错误打包进最终 Mod。

---

## 七、代码风格与命名规范

### 7.1 命名

| 类型 | 命名规则 | 示例 |
|---|---|---|
| 类、接口、枚举 | `UpperCamelCase` | `GearItem` |
| 方法、变量、参数 | `lowerCamelCase` | `registerItems()` |
| 常量 | `UPPER_SNAKE_CASE` | `MAX_DAMAGE` |
| 包名 | 全小写 | `com.hana.gear_forge.item` |
| 注册名称、资源 ID | 小写下划线或短横线 | `steel_gear` |
| 数据生成类 | 以 `Provider` 结尾 | `ModRecipeProvider` |
| 物品类 | 以 `Item` 结尾 | `CustomGearItem` |
| 配方类 | 以 `Recipe` 结尾 | `GearRecipe` |
| 配置类 | 以 `Config` 结尾 | `GearConfig` |
| 事件类 | 以 `Event` 或明确事件职责命名 | `PlayerEventHandler` |

项目不是 Web 应用，不强制使用 `DTO`、`DO`、`BO`、`VO`、`Query` 等命名。只有确实存在对应职责时才使用这些后缀。

### 7.2 格式

- Java 和 Gradle 源文件统一使用 UTF-8。
- Java 使用 4 个空格缩进，不使用 Tab。
- import 必须明确，避免通配符 import。
- 方法应保持单一职责，避免在注册、事件、数据生成类中混入无关业务。
- 公共 API、注册对象和跨模块使用的类型应保持稳定，修改时评估兼容性。
- 不得提交无用 import、调试代码、死代码或被注释掉的大段旧实现。
- 不创建只有一层转发、没有实际抽象价值的接口与实现类。

### 7.3 注释与 Javadoc

- 所有新增类、方法和字段必须添加 Javadoc 或必要的说明注释。
- 新增注释、Javadoc 和 Gradle 配置注释必须使用用户第一语言：**简体中文**。
- 新增公共方法的 Javadoc 应说明参数、返回值、异常、线程要求及客户端/服务端限制。
- 复杂的注册、事件、Mixin、数据生成逻辑必须说明设计原因，而不仅是重复代码含义。
- 标识符、Minecraft API 名称和 Gradle DSL 关键字保留英文。
- 新增类的 Javadoc 可添加：

~~~java
/**
 * 齿轮类物品的基础行为。
 *
 * @author Admin
 */
~~~

- 不要求修改第三方模板或已有英文注释；除非该部分正在被重构，否则避免无关文本变更。

---

## 八、日志、异常与安全规范

### 8.1 日志

- 禁止使用 `System.out.println`、`System.err.println` 和直接打印堆栈。
- 使用 NeoForge/Minecraft 推荐的 SLF4J 日志机制，例如 `LogUtils.getLogger()` 或项目已有 Logger。
- 日志必须包含必要上下文，但不得输出密码、令牌、完整敏感数据或大量玩家数据。
- `DEBUG` 日志不得在高频 Tick、渲染循环或大规模实体遍历中无条件输出。
- 普通运行信息使用合适的日志级别，异常必须包含原因和必要上下文。

### 8.2 输入与权限

- 所有命令参数、网络包、配置值和外部资源输入必须校验。
- 服务端不得信任客户端传入的权限、物品数量、坐标、伤害值和奖励结果。
- 涉及玩家、世界或管理员操作时必须执行权限检查。
- 不得从不可信输入直接构造文件路径、执行外部命令或访问任意网络地址。
- 不使用 SQL、JPA 或手动拼接 SQL；Minecraft 数据应使用 NeoForge/Minecraft 提供的配置、组件、资源和数据包机制。

---

## 九、性能与线程规范

- 不得在主线程中执行阻塞式网络请求、大量文件操作或长时间计算。
- 不得在每个 Tick 中重复创建大量临时对象、遍历所有实体或执行无必要的全量扫描。
- 对频繁访问的数据进行合理缓存，但必须在世界卸载、玩家退出或配置变更时正确清理。
- 世界、实体、区块和物品操作必须在正确的游戏线程执行。
- 异步任务不得直接操作非线程安全的 Minecraft 世界对象；如需回到主线程，必须使用对应调度机制。
- 数据生成、资源处理和构建任务应交由 Gradle/NeoForge 任务完成，不在游戏运行时重复执行。

---

## 十、Gradle 与构建规范

### 10.1 常用命令

Windows 环境统一使用项目自带的 Wrapper：

~~~bat
cd /d "H:\下载\Gear Forge"

:: 查看可用任务
gradlew.bat tasks

:: 编译并构建 Mod
gradlew.bat build

:: 清理后重新构建
gradlew.bat clean build

:: 运行数据生成
gradlew.bat runData

:: 启动客户端开发环境
gradlew.bat runClient

:: 启动服务端开发环境
gradlew.bat runServer

:: 启动 GameTest 服务端；仅在存在有效 GameTest 时运行
gradlew.bat runGameTestServer
~~~

构建失败时使用：

~~~bat
gradlew.bat build --stacktrace
~~~

### 10.2 构建配置

- `java.toolchain.languageVersion` 必须保持 Java 21，禁止降级到 Java 17 或更低版本。
- `tasks.withType(JavaCompile)` 已固定 UTF-8，新增任务也必须使用 UTF-8。
- `version` 使用 `mod_version`，`group` 使用 `mod_group_id`，不得在脚本中重复硬编码。
- Mod 归档名称保持为 `gear_forge`，除非发布策略明确变更。
- `maven-publish` 当前发布到项目下的本地 `repo` 目录；发布前应检查生成物和 POM，不得将开发专用依赖错误发布。
- `idea` 配置已启用依赖源码和 Javadoc 下载，IDE 同步时不得关闭。
- `processResources` 当前使用 `DuplicatesStrategy.INCLUDE`。资源路径仍应保持唯一，不得依赖重复资源的加载顺序解决冲突。
- 当前脚本中存在多处 `runs` 配置，修改客户端或服务端运行参数时应先确认现有配置，避免重复声明或互相覆盖。
- 不得把 `run` 目录中的世界、日志、缓存和下载内容当作源代码修改提交。

---

## 十一、验证要求

每次提交前至少完成以下检查：

1. 执行 `gradlew.bat build`，确保编译和资源处理成功。
2. 修改数据生成逻辑后执行 `gradlew.bat runData`。
3. 检查 `src/generated/resources` 是否只包含预期变更。
4. 启动客户端，验证物品、模型、纹理、语言文件和交互行为。
5. 启动服务端，确保没有客户端类加载错误。
6. 涉及命令时验证权限、错误参数和服务端执行结果。
7. 涉及 Curios 时验证 Curios 存在和不存在两种运行场景，或明确声明其为必需依赖。
8. 检查最终构建产物，确认开发文件未被打包。
9. 检查日志中不存在无意义的高频输出、异常堆栈或调试信息。

---

## 十二、基本设计原则

- **SOLID**：保持职责清晰、低耦合和可扩展。
- **DRY**：提取重复的注册、资源生成和校验逻辑。
- **KISS**：优先使用 NeoForge 官方 API 和简单实现。
- **YAGNI**：不为尚未确定的功能增加复杂抽象。
- **服务端权威**：所有影响游戏状态的结果由服务端确认。
- **事件优先**：优先使用 NeoForge 事件和扩展点，谨慎使用 Mixin。
- **资源与代码分离**：可由 JSON、数据包或数据生成器表达的内容，不应硬编码在 Java 中。
- **可维护性优先**：代码、资源、生成器和构建配置必须保持职责边界清晰。
