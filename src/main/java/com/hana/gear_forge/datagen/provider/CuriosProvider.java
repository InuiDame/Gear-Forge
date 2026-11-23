package com.hana.gear_forge.datagen.provider;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import top.theillusivec4.curios.api.CuriosDataProvider;
import top.theillusivec4.curios.api.common.DropRule;

import java.util.concurrent.CompletableFuture;

public class CuriosProvider extends CuriosDataProvider {

    public CuriosProvider(String modId, PackOutput output,
                          CompletableFuture<HolderLookup.Provider> registries) {
        super(modId, output, registries);
    }

    @Override
    public void generate(HolderLookup.Provider registries) {
        // Accessory slot used by the lucky diamond and cursed anvil (see data/curios/tags/item/hhthing.json).
        // 图标必须用 Curios 里真实存在的贴图：curios:slot/empty_custom_slot 并不存在，
        // 用了会让饰品栏渲染成紫黑缺失贴图。
        // 26.3 的自定义槽位图标走 GUI 图集 sprite；curios:slot/empty_curio_slot 在
        // 本版本没有进入 gui 图集，会渲染成紫黑缺失贴图。改指向我们自己打进
        // assets/gear_forge/textures/gui/sprites/hhthing_slot.png 的 sprite。
        this.createSlot("hhthing")
                .order(2000)
                .size(4)
                .icon(Identifier.fromNamespaceAndPath("gear_forge", "hhthing_slot"))
                .dropRule(DropRule.ALWAYS_KEEP)
                .addCosmetic(true);
    }
}
