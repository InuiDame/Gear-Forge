package com.hana.gear_forge.content;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 本模组的自定义状态效果。
 *
 * <p>{@link #MIDAS_TOUCH}：点金。等级 I 挖石头掉 1 个金锭，等级 II 掉 2 个。
 * 多个来源（耀金苹果、耀金硬币、耀金四件套）的等级会相加，由 {@code MidasTouchHandler} 统一合成。
 *
 * <p>{@link #GILDED}：金化。让玩家对猪灵视为穿着金装，见 {@code PiglinAiMixin}。
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, GearForgeMod.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> MIDAS_TOUCH =
            MOB_EFFECTS.register("midas_touch",
                    () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE8C25A) {});

    public static final DeferredHolder<MobEffect, MobEffect> GILDED =
            MOB_EFFECTS.register("gilded",
                    () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x8FD8F0) {});

    private ModMobEffects() {}
}
