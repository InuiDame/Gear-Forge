package com.hana.gear_forge;

import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.content.ModMobEffects;
import com.hana.gear_forge.content.YaokinMaterial;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.item.ModCreativeTabs;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.loot.ModGlobalLootModifiers;
import com.hana.gear_forge.menu.ModMenus;
import com.hana.gear_forge.recipe.ModRecipeSerializers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(GearForgeMod.MODID)
public class GearForgeMod {
    public static final String MODID = "gear_forge";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GearForgeMod(IEventBus modEventBus, ModContainer modContainer) {
        ModConfigs.init();

        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModMenus.MENU_TYPES.register(modEventBus);
        ModMobEffects.MOB_EFFECTS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModGlobalLootModifiers.GLM_SERIALIZERS.register(modEventBus);


    }



}
