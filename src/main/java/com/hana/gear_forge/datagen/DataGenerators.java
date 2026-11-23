package com.hana.gear_forge.datagen;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.datagen.provider.CuriosProvider;
import com.hana.gear_forge.datagen.provider.ModItemModelProvider;
import com.hana.gear_forge.datagen.provider.ModRecipeProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.data.event.GatherDataRegistryEntriesEvent;

// GatherDataEvent / GatherDataRegistryEntriesEvent 都是 mod-bus 事件。
@EventBusSubscriber(modid = GearForgeMod.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        // 26.3 起 GatherDataEvent 拆成 sealed 的 Client/Server 子类，通过 instanceof 区分。
        if (event instanceof GatherDataEvent.Client) {
            event.addProvider(new ModItemModelProvider(event.getGenerator().getPackOutput()));
        }
        if (event instanceof GatherDataEvent.Server) {
            event.addProvider(new CuriosProvider(
                    GearForgeMod.MODID,
                    event.getGenerator().getPackOutput(),
                    event.getWorldLookupProvider()));
        }
    }

    @SubscribeEvent
    public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        // 26.3 配方/进度改走 registry bootstrap，注册到 GatherDataRegistryEntriesEvent。
        event.recipe(ModRecipeProvider::new);
    }
}
