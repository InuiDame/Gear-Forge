package com.hana.gear_forge.datagen.provider;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.data.PackOutput;
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
        // 这两个此前是手写在 src/main/resources 里的模型，与 ADR-0006「generated 是唯一来源」冲突，
        // 也让“重跑 runData 即可完整重建资源”不成立。
        basicItem(ModItems.LUCKY_DIAMOND.get());
        basicItem(ModItems.CURSED_ANVIL.get());
        // 方块物品的模型继承方块模型，用原版锻造台的贴图拼出紫色版本。
        withExistingParent("forge_table", modLoc("block/forge_table"));

        // 耀金系列
        basicItem(ModItems.YAOKIN_INGOT.get());
        basicItem(ModItems.YAOKIN_APPLE.get());
        basicItem(ModItems.YAOKIN_COIN.get());
        handheldItem(ModItems.YAOKIN_HAMMER.get());
        handheldItem(ModItems.YAOKIN_SWORD.get());
        handheldItem(ModItems.YAOKIN_PICKAXE.get());
        handheldItem(ModItems.YAOKIN_AXE.get());
        handheldItem(ModItems.YAOKIN_SHOVEL.get());
        handheldItem(ModItems.YAOKIN_HOE.get());
        handheldItem(ModItems.DIAMOND_CURVED_BLADE.get());
        handheldItem(ModItems.NETHERITE_CURVED_BLADE.get());
        handheldItem(ModItems.YAOKIN_CURVED_BLADE.get());
        basicItem(ModItems.YAOKIN_HELMET.get());
        basicItem(ModItems.YAOKIN_CHESTPLATE.get());
        basicItem(ModItems.YAOKIN_LEGGINGS.get());
        basicItem(ModItems.YAOKIN_BOOTS.get());

        // 樱花木系列
        basicItem(ModItems.SAKURA_INGOT.get());
        basicItem(ModItems.SAKURA_NUGGET.get());
        basicItem(ModItems.SAKURA_STONE.get());
        basicItem(ModItems.SAKURA_DRINK.get());
        basicItem(ModItems.SAKURA_COOKIE.get());
        handheldItem(ModItems.SAKURA_SWORD.get());
        handheldItem(ModItems.SAKURA_PICKAXE.get());
        handheldItem(ModItems.SAKURA_AXE.get());
        handheldItem(ModItems.SAKURA_SHOVEL.get());
        handheldItem(ModItems.SAKURA_HOE.get());
        basicItem(ModItems.SAKURA_HELMET.get());
        basicItem(ModItems.SAKURA_CHESTPLATE.get());
        basicItem(ModItems.SAKURA_LEGGINGS.get());
        basicItem(ModItems.SAKURA_BOOTS.get());

        // 柚鸟夏 Boss / 队友（YSM）
        basicItem(ModItems.NATSU_SUMMON.get());
        basicItem(ModItems.NATSU_CONTRACT.get());

    }

}
