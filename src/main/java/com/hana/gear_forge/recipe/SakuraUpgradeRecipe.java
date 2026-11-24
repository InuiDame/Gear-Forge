package com.hana.gear_forge.recipe;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * 樱花木升级：下界合金装备 + 下界合金升级模板 + 樱花木锭 → 对应的樱花木装备。
 *
 * <p>与耀金升级共用原版下界合金升级模板（模板槽），靠材料槽的物品区分去向：
 * 放耀金锭走 {@link YaokinUpgradeRecipe}，放樱花木锭走本类。数值与下界合金一致，
 * 差异在印记/砍树/施肥/魔法减伤等特效。
 */
public final class SakuraUpgradeRecipe {

    private static final Map<Item, Item> UPGRADES = Map.ofEntries(
            Map.entry(Items.NETHERITE_HELMET, ModItems.SAKURA_HELMET.get()),
            Map.entry(Items.NETHERITE_CHESTPLATE, ModItems.SAKURA_CHESTPLATE.get()),
            Map.entry(Items.NETHERITE_LEGGINGS, ModItems.SAKURA_LEGGINGS.get()),
            Map.entry(Items.NETHERITE_BOOTS, ModItems.SAKURA_BOOTS.get()),
            Map.entry(Items.NETHERITE_SWORD, ModItems.SAKURA_SWORD.get()),
            Map.entry(Items.NETHERITE_PICKAXE, ModItems.SAKURA_PICKAXE.get()),
            Map.entry(Items.NETHERITE_AXE, ModItems.SAKURA_AXE.get()),
            Map.entry(Items.NETHERITE_SHOVEL, ModItems.SAKURA_SHOVEL.get()),
            Map.entry(Items.NETHERITE_HOE, ModItems.SAKURA_HOE.get()));

    private SakuraUpgradeRecipe() {}

    public static boolean isUpgradable(ItemStack base) {
        return !base.isEmpty() && UPGRADES.containsKey(base.getItem());
    }

    public static ItemStack upgrade(ItemStack base) {
        Item result = UPGRADES.get(base.getItem());
        if (result == null) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(result);
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentsForCrafting(base);
        if (!enchantments.isEmpty()) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(out, enchantments);
        }
        var name = base.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name != null) {
            out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        }
        if (base.isDamageableItem() && out.isDamageableItem() && base.isDamaged()) {
            double ratio = (double) base.getDamageValue() / base.getMaxDamage();
            out.setDamageValue((int) Math.round(ratio * out.getMaxDamage()));
        }
        return out;
    }

    /** 是否是樱花升级材料（樱花木锭）。 */
    public static boolean isUpgradeMaterial(ItemStack stack) {
        return stack.is(ModItems.SAKURA_INGOT.get());
    }
}
