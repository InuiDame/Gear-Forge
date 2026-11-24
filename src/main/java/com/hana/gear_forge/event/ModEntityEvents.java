package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ModEntities;
import com.hana.gear_forge.entity.ysm.NatsuBossEntity;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** 注册自定义实体的属性（血量/速度/攻击力等）。 */
@EventBusSubscriber(modid = GearForgeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityEvents {

    private ModEntityEvents() {}

    @SubscribeEvent
    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.NATSU_BOSS.get(), NatsuBossEntity.createAttributes().build());
        event.put(ModEntities.NATSU_COMPANION.get(), NatsuCompanionEntity.createAttributes().build());
    }
}
