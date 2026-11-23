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
 * 耀金材质：一个工具材质 + 一套盔甲材质。
 *
 * <p>26.3 起，原 {@code Tier} 被 {@link ToolMaterial}（record）取代，
 * {@code ArmorMaterial} 也从可注册接口变成纯 record 常量（不再需要 DeferredRegister）。
 * 修复材料改为 {@link TagKey<Item>}，纹理改为 datapack 里的 {@link EquipmentAsset}。
 * 数值保持与 1.21.1 版本一致。
 */
public final class YaokinMaterial {

    /** 耀金设备资源 key（对应 assets/gear_forge/equipment/yaokin.json）。 */
    public static final ResourceKey<EquipmentAsset> EQUIPMENT_ASSET =
            ResourceKey.create(EquipmentAssets.ROOT_ID,
                    Identifier.fromNamespaceAndPath(GearForgeMod.MODID, "yaokin"));

    /** 耀金修复材料标签（data/gear_forge/tags/item/yaokin_repair.json）。 */
    public static final TagKey<Item> REPAIR_TAG = ItemTags.create(
            Identifier.fromNamespaceAndPath(GearForgeMod.MODID, "yaokin_repair"));

    /**
     * 工具材质：下界合金是 durability=2031 / speed=9.0 / damage=4.0 / enchant=15。
     * 耀金：耐久 +20%（2437）、速度 9.5、附魔亲和 18。
     */
    public static final ToolMaterial TOOL = new ToolMaterial(
            BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
            2437,
            9.5F,
            4.0F,
            18,
            REPAIR_TAG);

    /**
     * 盔甲材质：下界合金 durability=2031 / defense [3,6,8,3] / toughness 3.0 / knockback 0.1 / enchant 15。
     * 耀金：耐久 +20%（2437）、护甲值各 +1、韧性 3.5、附魔亲和 18。
     */
    public static final ArmorMaterial ARMOR = new ArmorMaterial(
            2437,
            Map.of(
                    ArmorType.BOOTS, 4,
                    ArmorType.LEGGINGS, 9,
                    ArmorType.CHESTPLATE, 7,
                    ArmorType.HELMET, 4,
                    ArmorType.BODY, 11),
            18,
            SoundEvents.ARMOR_EQUIP_NETHERITE,
            3.5F,
            0.1F,
            REPAIR_TAG,
            EQUIPMENT_ASSET);

    private YaokinMaterial() {}
}
