---
status: accepted
---

# Curios 是必需依赖

幸运钻石和魔性铁砧直接实现 `ICurioItem`，事件处理里直接调用 `CuriosApi`，没有做可选集成层。没有 Curios 时这些类无法加载，模组会崩溃，所以 `neoforge.mods.toml` 把 Curios 声明为 `required`。2026-11 之前元数据里的依赖段写成了 `dependencies.gear_forege`（拼写错误），三个依赖声明实际都没生效。

## Considered Options

- 可选集成：把饰品相关类隔离到只在 Curios 存在时才加载的包里，没有 Curios 时不注册这两个物品。两个饰品不是核心玩法，如果以后有玩家反馈不想装 Curios，可以改成这样。
