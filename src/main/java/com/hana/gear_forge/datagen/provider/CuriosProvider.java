package com.hana.gear_forge.datagen.provider;

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
        // 图标必须用 Curios 9.5.1 里真实存在的贴图：curios:slot/empty_custom_slot 在这个版本并不存在，
        // 用了会让饰品栏渲染成紫黑缺失贴图。
        this.createSlot("hhthing")
                .order(2000)
                .size(4)
                .icon(ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_curio_slot"))
                .dropRule(ICurio.DropRule.ALWAYS_KEEP)
                .addCosmetic(true);
    }
}
