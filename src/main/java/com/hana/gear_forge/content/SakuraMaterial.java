package com.hana.gear_forge.content;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * 樱花木材质：一个工具等级 + 一套盔甲材质，数值与下界合金完全一致。
 *
 * <p>樱花木锭由下界合金锭 + 樱花原木合成，樱花套由下界合金套经锻造模板升级而来，
 * 所以基础数值刻意对齐下界合金；它在数值之外提供的是印记、砍树、施肥与魔法减伤等特效。
 */
public final class SakuraMaterial {

    /** 盔甲材质注册表（1.21.1 是普通注册表）。 */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, GearForgeMod.MODID);

    /** 工具等级：与下界合金一致（uses=2031 / speed=9.0 / damage=4.0 / enchant=15）。 */
    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 2031;
        }

        @Override
        public float getSpeed() {
            return 9.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 4.0F;
        }

        @Override
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.SAKURA_INGOT.get());
        }
    };

    /** 盔甲材质：与下界合金一致（[3,6,8,3] / toughness 3.0 / knockback 0.1 / enchant 15）。 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMOR =
            ARMOR_MATERIALS.register("sakura", () -> {
                EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET, 3);
                defense.put(ArmorItem.Type.CHESTPLATE, 6);
                defense.put(ArmorItem.Type.LEGGINGS, 8);
                defense.put(ArmorItem.Type.BOOTS, 3);
                defense.put(ArmorItem.Type.BODY, 11);
                return new ArmorMaterial(
                        defense,
                        15,
                        SoundEvents.ARMOR_EQUIP_NETHERITE,
                        () -> Ingredient.of(ModItems.SAKURA_INGOT.get()),
                        List.of(new ArmorMaterial.Layer(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "sakura"))),
                        3.0F,
                        0.1F);
            });

    private SakuraMaterial() {}
}
