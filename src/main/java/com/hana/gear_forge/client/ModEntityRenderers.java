package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ModEntities;
import com.hana.gear_forge.entity.ysm.NatsuBossEntity;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 注册柚鸟夏实体的 YSM 渲染器（客户端）。 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ModEntityRenderers {

    private ModEntityRenderers() {}

    @SubscribeEvent
    public static void onRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NATSU_BOSS.get(), YsmEntityRenderer::new);
        event.registerEntityRenderer(ModEntities.NATSU_COMPANION.get(), YsmEntityRenderer::new);
    }
}
