package com.hana.gear_forge.datagen;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.Provider.CuriosProvider;
import com.hana.gear_forge.Provider.ModItemModelProvider;
import com.hana.gear_forge.Provider.ModRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

// GatherDataEvent is a mod-bus event; FML routes it to the mod bus automatically.
@EventBusSubscriber(modid = GearForgeMod.MODID)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput packOutput = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(packOutput, existingFileHelper));

        generator.addProvider(event.includeServer(),
                new ModRecipeProvider(packOutput, lookupProvider));

        generator.addProvider(event.includeServer(),
                new CuriosProvider(GearForgeMod.MODID, packOutput, existingFileHelper, lookupProvider));
    }
}
