package com.hana.gear_forge.content;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

import java.util.Map;

/**
 * 樱花木材质（26.3）：工具/护甲数值与下界合金一致，差异在特效。
 * 26.3 的 {@link ToolMaterial}/{@link ArmorMaterial} 是 record 常量，纹理走 datapack
 * 的 {@link EquipmentAsset}，修复材料走 {@link TagKey<Item>}。
 */
public final class SakuraMaterial {

    public static final ResourceKey<EquipmentAsset> EQUIPMENT_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(GearForgeMod.MODID, "sakura"));

    public static final TagKey<Item> REPAIR_TAG = ItemTags.create(
            Identifier.fromNamespaceAndPath(GearForgeMod.MODID, "sakura_repair"));

    /** 工具材质：与下界合金一致（2031 / 9.0 / 4.0 / 15）。 */
    public static final ToolMaterial TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2031,
            9.0F,
            4.0F,
            15,
            REPAIR_TAG);

    /** 护甲材质：与下界合金一致（2031 / [3,6,8,3] / 3.0 / 0.1 / 15）。 */
    public static final ArmorMaterial ARMOR = new ArmorMaterial(
            2031,
            Map.of(
                    ArmorType.BOOTS, 3,
                    ArmorType.LEGGINGS, 8,
                    ArmorType.CHESTPLATE, 6,
                    ArmorType.HELMET, 3,
                    ArmorType.BODY, 11),
            15,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.0F,
            0.1F,
            REPAIR_TAG,
            EQUIPMENT_ASSET);

    private SakuraMaterial() {}
}
