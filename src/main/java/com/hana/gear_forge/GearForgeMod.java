package com.hana.gear_forge;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.item.ModCreativeTabs;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.recipe.ModRecipeSerializers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(GearForgeMod.MODID)
public class GearForgeMod {
    public static final String MODID = "gear_forge";

    public GearForgeMod(IEventBus modEventBus, ModContainer modContainer) {
        ModConfigs.init();

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
    }
}
