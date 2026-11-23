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
 * 耀金材质：一个工具等级 + 一套盔甲材质。
 *
 * <p>数值取向：耀金是"下界合金的强化版"——耐久与附魔亲和更高，护甲值与韧性略高。
 * 玩家要先把下界合金装备升到耀金，已经付出了下界合金的成本，所以提升幅度刻意保持克制：
 * 值得升级，但不会让下界合金变得毫无意义。
 *
 * <p>盔甲材质在 1.21.1 是<b>普通注册表</b>（不是数据驱动），必须注册进
 * {@code BuiltInRegistries.ARMOR_MATERIAL} 才能被 {@code ArmorItem} 的 {@code Holder} 引用。
 */
public final class YaokinMaterial {

    /** 盔甲材质注册表。 */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, GearForgeMod.MODID);

    /**
     * 工具等级。
     *
     * <p>下界合金是 uses=2031 / speed=9.0 / damage=4.0 / enchant=15。
     * 耀金：耐久 +20%（2437）、速度 9.5、附魔亲和 18。
     */
    public static final Tier TIER = new Tier() {
        @Override
        public int getUses() {
            return 2437;
        }

        @Override
        public float getSpeed() {
            return 9.5F;
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
            return 18;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.YAOKIN_INGOT.get());
        }
    };

    /**
     * 盔甲材质。
     *
     * <p>下界合金是 [3, 6, 8, 3] / toughness 3.0 / knockback 0.1 / enchant 15。
     * 耀金：护甲值各 +1，韧性 3.5，附魔亲和 18。
     * {@code ArmorItem.Type.BODY} 对应狼铠，一并给数值以免其它模组查询时拿到 0。
     */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> ARMOR =
            ARMOR_MATERIALS.register("yaokin", () -> {
                EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.HELMET, 4);
                defense.put(ArmorItem.Type.CHESTPLATE, 7);
                defense.put(ArmorItem.Type.LEGGINGS, 9);
                defense.put(ArmorItem.Type.BOOTS, 4);
                defense.put(ArmorItem.Type.BODY, 11);
                return new ArmorMaterial(
                        defense,
                        18,
                        SoundEvents.ARMOR_EQUIP_NETHERITE,
                        () -> Ingredient.of(ModItems.YAOKIN_INGOT.get()),
                        List.of(new ArmorMaterial.Layer(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "yaokin"))),
                        3.5F,
                        0.1F);
            });

    private YaokinMaterial() {}
}
