---
status: accepted
---

# 平衡配置在服务端读取，登录时下发快照给客户端

`config/gear_forge/` 下的四份 JSON（`affixes.json`、`upgrades.json`、`gems.json`、`mob_gear.json`）由服务端持有权威值。客户端本地也保留同名文件，但一旦连上服务器，服务端会在玩家登录时用 `gear_forge:config_sync` 把当前内存快照发过去，客户端**只覆盖内存、不写回自己的文件**；断线后重新读取本地文件。

## 为什么

伤害、属性、掉落全部由服务端按服务端配置结算，而工具提示、物品属性表（`ItemAttributeModifierEvent` 在双端都会触发）、弓蓄力预测在客户端按客户端配置计算。两边文件不一致时：

1. 提示里的宝石数值与实际结算值不同——最直接的信任问题。
2. `MAX_HEALTH` / `ARMOR` / `MOVEMENT_SPEED` 在客户端本地被算错，血条、护甲图标、移动速度预测与服务端权威值错位。
3. 从配置里删掉的词条 id 在客户端查不到，提示里显示裸 id。
4. 锻造台/重铸预览先客户端算一遍再被服务端结果覆盖，强化上限或插槽数不同时会闪烁。

此前这四个后果全部存在（工单 GF-007）。

## 实现

- `network/ConfigSyncPayload`：4 个 UTF-8 字符串（四份 JSON 文本）。用 JSON 文本而不是逐字段二进制，是因为配置模型会随平衡调整增删字段，逐字段编解码会让"客户端模组版本略旧"直接断连；JSON 由接收端 Gson 解析，缺字段/多字段都能自然降级。单段上限 32767 字符，默认四份合计约 20 KB，够用；若将来配置显著变大需要改成分段或压缩。
- `network/ModNetwork`：在 `RegisterPayloadHandlersEvent` 里 `playToClient`。该事件实现 `IModBusEvent`，FML 按事件类型自动把它注册到 mod 总线。
- `network/ConfigSyncEvents`：`PlayerEvent.PlayerLoggedInEvent`（游戏总线）里 `PacketDistributor.sendToPlayer`。用 `EventPriority.LOWEST` 让其它 mod 先处理登录，避免包早于连接就绪。
- `client/ClientConfigSync`：`ClientPlayerNetworkEvent.LoggingOut` 时 `ModConfigs.reload()`，恢复本地文件。类上带 `value = Dist.CLIENT`。
- `ModConfigs.applyServerSnapshot(...)`：任何一段解析失败都保留原有值，绝不把配置清空——配置错误只应影响数值，不应让整个模组失效。

单人游戏里集成服务端与客户端共享同一 JVM 和同一批静态字段，重复应用同一份快照是幂等的。

## 未采用的做法

把词条/宝石定义改成 `DataPackRegistryEvent.NewRegistry` 同步注册表 + `data/<ns>/gear_forge/affix/*.json`，由原版按世界同步。这是更"正统"的方案，但要重写 ADR-0003 并迁移所有现有配置文件，收益只是省掉一个自定义 payload。当前规模不值得。

## Consequences

- 服务器改配置后需要重启（或将来接 `/reload` 命令调用 `ModConfigs.reload()` + `ConfigSyncEvents.broadcast()`）。
- 客户端本地配置仅在单人游戏和断线状态下生效。
- 新增第五份配置文件时，必须同时改 `ConfigSyncPayload` 的字段数和 `ModConfigs.toSnapshot()`，否则不会同步。
