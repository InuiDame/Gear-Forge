package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** Registers the blocking predicate used by the Blockbench-exported shield model override. */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public final class ShieldItemProperties {

    private ShieldItemProperties() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.NATSU_SHIELD.get(),
                ResourceLocation.withDefaultNamespace("blocking"),
                (ItemStack stack, ClientLevel level, LivingEntity entity, int seed) ->
                        entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F
        ));
    }
}
