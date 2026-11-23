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

    /*
     * 26.3 起 GatherDataEvent 是 abstract sealed 类，事件总线不允许为抽象类注册监听器，
     * 否则客户端启动即抛 "Cannot register listeners for abstract class ...GatherDataEvent"。
     * 必须分别订阅它的两个具体子类 Client / Server，不能在抽象参数上做 instanceof。
     */
    @SubscribeEvent
    public static void gatherDataClient(GatherDataEvent.Client event) {
        event.addProvider(new ModItemModelProvider(event.getGenerator().getPackOutput()));
    }

    @SubscribeEvent
    public static void gatherDataServer(GatherDataEvent.Server event) {
        event.addProvider(new CuriosProvider(
                GearForgeMod.MODID,
                event.getGenerator().getPackOutput(),
                event.getWorldLookupProvider()));
    }

    @SubscribeEvent
    public static void gatherRegistryEntries(GatherDataRegistryEntriesEvent event) {
        // 26.3 配方/进度改走 registry bootstrap，注册到 GatherDataRegistryEntriesEvent。
        event.recipe(ModRecipeProvider::new);
    }
}
