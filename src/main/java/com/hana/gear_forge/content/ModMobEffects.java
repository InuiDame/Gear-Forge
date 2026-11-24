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
 * <p>{@link #MIDAS_TOUCH}：点金。等级 N 挖石头掉 N 个金锭（I 掉 1 个、II 掉 2 个、III 掉 3 个，以此类推）。
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

    /** 樱花之护佑：等级 N 减免 N×10% 魔法伤害（套装给 5 级、樱花石宝石给 1 级）。 */
    public static final DeferredHolder<MobEffect, MobEffect> SAKURA_BLESSING =
            MOB_EFFECTS.register("sakura_blessing",
                    () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xFFB7C5) {});

    private ModMobEffects() {}
}
