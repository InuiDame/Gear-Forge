package com.hana.gear_forge.Provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import top.theillusivec4.curios.api.CuriosDataProvider;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.concurrent.CompletableFuture;

public class CuriosProvider extends CuriosDataProvider {

    public CuriosProvider(String modId, PackOutput output,
                          ExistingFileHelper fileHelper,
                          CompletableFuture<HolderLookup.Provider> registries) {
        super(modId, output, fileHelper, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries, ExistingFileHelper fileHelper) {
        // Accessory slot used by the lucky diamond and cursed anvil (see data/curios/tags/item/hhthing.json).
        this.createSlot("hhthing")
                .order(2000)
                .size(4)
                .icon(ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_custom_slot"))
                .dropRule(ICurio.DropRule.ALWAYS_KEEP)
                .addCosmetic(true);
    }
}
