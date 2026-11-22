package com.hana.gear_forge.loot;

import com.hana.gear_forge.loot.AddItemModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

// 修改后的 ModGlobalLootModifiers.java
public class ModGlobalLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "gear_forge");

    // 注册一个名为 'add_item' 的序列化器
    public static final Supplier<MapCodec<AddItemModifier>> ADD_ITEM =
            GLM_SERIALIZERS.register("add_item", () -> AddItemModifier.CODEC);
}
