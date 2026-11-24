package com.hana.gear_forge.entity;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.NatsuBossEntity;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 自定义实体注册（柚鸟夏 Boss / 队友，真 Mob 实体）。 */
public final class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, GearForgeMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NatsuBossEntity>> NATSU_BOSS =
            ENTITY_TYPES.register("natsu_boss",
                    () -> EntityType.Builder.of(NatsuBossEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(64)
                            .build("natsu_boss"));

    public static final DeferredHolder<EntityType<?>, EntityType<NatsuCompanionEntity>> NATSU_COMPANION =
            ENTITY_TYPES.register("natsu_companion",
                    () -> EntityType.Builder.of(NatsuCompanionEntity::new, MobCategory.MISC)
                            .sized(0.6F, 1.8F)
                            .clientTrackingRange(64)
                            .build("natsu_companion"));

    private ModEntities() {}
}
