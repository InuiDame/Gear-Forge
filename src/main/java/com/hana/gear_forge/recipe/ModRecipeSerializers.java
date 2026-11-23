package com.hana.gear_forge.recipe;

import com.hana.gear_forge.GearForgeMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, GearForgeMod.MODID);

    private static final MapCodec<GearSmithingRecipe> CODEC = MapCodec.unit(GearSmithingRecipe.INSTANCE);
    private static final StreamCodec<RegistryFriendlyByteBuf, GearSmithingRecipe> STREAM_CODEC =
            StreamCodec.unit(GearSmithingRecipe.INSTANCE);

    // 26.3 起 RecipeSerializer 是 final record，直接构造，不再 implements。
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GearSmithingRecipe>> GEAR_SMITHING =
            RECIPE_SERIALIZERS.register("gear_smithing",
                    () -> new RecipeSerializer<>(CODEC, STREAM_CODEC));
}
