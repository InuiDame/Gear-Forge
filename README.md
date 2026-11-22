# Gear Forge

Minecraft 1.21.1 / NeoForge 模组。在原版锻造台上给装备做强化、随机词条、宝石镶嵌和转生，另外提供 7 个附魔和 2 个 Curios 饰品。

- Mod ID：`gear_forge`
- 依赖：NeoForge 21.1.228+，[Curios API](https://github.com/TheIllusiveC4/Curios) 9.4+（必需）
- 许可证：MIT（见 [LICENSE](LICENSE)）

## 玩法速览

所有锻造操作都在原版锻造台完成：装备放第二格（底材），材料放第三格，第一格（模板）按操作需要放材料。

| 材料（第三格） | 操作 | 模板（第一格） |
|---|---|---|
| 锻造锤 | 强化等级 +1（上限默认 15） | 空或钻石 |
| 重铸石 | 重新随机词条 | 空或钻石 |
| 红宝石、蓝宝石、绿宝石、黄玉、翡翠、玛瑙、钻石、下界之星 | 镶嵌宝石（每件默认最多 3 颗） | 空 |
| 宝石摘除器 | 摘下最后镶嵌的宝石 | 空 |
| 升阶水晶（底材放宝石） | 宝石阶级 +1（上限默认 5） | 空 |
| 转移宝珠 | 切换副手模式 | 空 |
| 附魔书（装备上需有绿宝石） | 无视冲突写入附魔 | 空 |
| 转生石（装备需满级） | 转生，保留数值奖励 | 8 个下界合金块（可配置） |

数值平衡在 `config/gear_forge/` 下的四个 JSON 文件里调整（`affixes.json`、`gems.json`、`upgrades.json`、`mob_gear.json`），首次启动时自动生成。

## 开发

```bash
./gradlew build          # 构建，产物在 build/libs/
./gradlew runClient      # 启动开发客户端
./gradlew runData        # 重新生成 src/generated/resources
```

完整的环境搭建、测试和发布流程见 [docs/development.md](docs/development.md)。

## 文档

- [docs/architecture.md](docs/architecture.md)：代码结构和各子系统的工作方式
- [docs/development.md](docs/development.md)：开发、测试、构建、发布流程
- [docs/adr/](docs/adr/)：架构决策记录
- [docs/tickets/](docs/tickets/)：已知问题和修复方案
- [GLOSSARY.md](GLOSSARY.md)：术语表
