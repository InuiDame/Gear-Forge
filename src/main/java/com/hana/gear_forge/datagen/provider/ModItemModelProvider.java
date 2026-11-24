package com.hana.gear_forge.datagen.provider;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.data.PackOutput;

/**
 * 26.3 起 NeoForge 的 ItemModelProvider 被删除，改用原版 {@link ModelProvider} +
 * {@link ItemModelGenerators}。
 *
 * <p>注意：原版 ModelProvider 会按 modId 自动为所有本模组的 BlockItem 生成模型（指向方块模型），
 * 所以 forge_table 无需手动注册；其余非方块物品必须逐个注册，否则校验会失败。
 */
public class ModItemModelProvider extends ModelProvider {

    public ModItemModelProvider(PackOutput output) {
        super(output, GearForgeMod.MODID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        // ===================== 原有材料 =====================
        flat(itemModels, ModItems.REROLL_STONE.get());
        handheld(itemModels, ModItems.FORGE_HAMMER.get());
        flat(itemModels, ModItems.UPGRADE_CRYSTAL.get());
        flat(itemModels, ModItems.RUBY.get());
        flat(itemModels, ModItems.SAPPHIRE.get());
        flat(itemModels, ModItems.EMERALD_GEM.get());
        flat(itemModels, ModItems.TOPAZ.get());
        flat(itemModels, ModItems.JADE.get());
        flat(itemModels, ModItems.ONYX.get());
        flat(itemModels, ModItems.TRANSFER_ORB.get());
        flat(itemModels, ModItems.GEM_REMOVER.get());
        flat(itemModels, ModItems.REINCARNATION_STONE.get());
        flat(itemModels, ModItems.LUCKY_DIAMOND.get());
        flat(itemModels, ModItems.CURSED_ANVIL.get());
        // FORGE_TABLE 是 BlockItem，由 ModelProvider 自动指向 gear_forge:block/forge_table。

        // ===================== 耀金系列 =====================
        flat(itemModels, ModItems.YAOKIN_INGOT.get());
        flat(itemModels, ModItems.YAOKIN_APPLE.get());
        flat(itemModels, ModItems.YAOKIN_COIN.get());
        handheld(itemModels, ModItems.YAOKIN_HAMMER.get());
        handheld(itemModels, ModItems.YAOKIN_SWORD.get());
        handheld(itemModels, ModItems.YAOKIN_PICKAXE.get());
        handheld(itemModels, ModItems.YAOKIN_AXE.get());
        handheld(itemModels, ModItems.YAOKIN_SHOVEL.get());
        handheld(itemModels, ModItems.YAOKIN_HOE.get());
        handheld(itemModels, ModItems.DIAMOND_CURVED_BLADE.get());
        handheld(itemModels, ModItems.NETHERITE_CURVED_BLADE.get());
        handheld(itemModels, ModItems.YAOKIN_CURVED_BLADE.get());
        flat(itemModels, ModItems.YAOKIN_HELMET.get());
        flat(itemModels, ModItems.YAOKIN_CHESTPLATE.get());
        flat(itemModels, ModItems.YAOKIN_LEGGINGS.get());
        flat(itemModels, ModItems.YAOKIN_BOOTS.get());

        // 樱花木系列
        flat(itemModels, ModItems.SAKURA_INGOT.get());
        flat(itemModels, ModItems.SAKURA_NUGGET.get());
        flat(itemModels, ModItems.SAKURA_STONE.get());
        flat(itemModels, ModItems.SAKURA_DRINK.get());
        flat(itemModels, ModItems.SAKURA_COOKIE.get());
        handheld(itemModels, ModItems.SAKURA_SWORD.get());
        handheld(itemModels, ModItems.SAKURA_PICKAXE.get());
        handheld(itemModels, ModItems.SAKURA_AXE.get());
        handheld(itemModels, ModItems.SAKURA_SHOVEL.get());
        handheld(itemModels, ModItems.SAKURA_HOE.get());
        flat(itemModels, ModItems.SAKURA_HELMET.get());
        flat(itemModels, ModItems.SAKURA_CHESTPLATE.get());
        flat(itemModels, ModItems.SAKURA_LEGGINGS.get());
        flat(itemModels, ModItems.SAKURA_BOOTS.get());
    }

    private static void flat(ItemModelGenerators itemModels, net.minecraft.world.item.Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
    }

    private static void handheld(ItemModelGenerators itemModels, net.minecraft.world.item.Item item) {
        itemModels.generateFlatItem(item, ModelTemplates.FLAT_HANDHELD_ITEM);
    }
}
