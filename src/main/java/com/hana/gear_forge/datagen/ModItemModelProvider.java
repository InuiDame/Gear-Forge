package com.hana.gear_forge.datagen;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, GearForgeMod.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(ModItems.REROLL_STONE.get());
        handheldItem(ModItems.FORGE_HAMMER.get());
        basicItem(ModItems.UPGRADE_CRYSTAL.get());
        basicItem(ModItems.RUBY.get());
        basicItem(ModItems.SAPPHIRE.get());
        basicItem(ModItems.EMERALD_GEM.get());
        basicItem(ModItems.TOPAZ.get());
        basicItem(ModItems.JADE.get());
        basicItem(ModItems.ONYX.get());
        basicItem(ModItems.TRANSFER_ORB.get());
        basicItem(ModItems.GEM_REMOVER.get());
        basicItem(ModItems.REINCARNATION_STONE.get());

    }

}
