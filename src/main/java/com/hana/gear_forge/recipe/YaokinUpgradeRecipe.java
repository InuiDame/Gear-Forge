package com.hana.gear_forge.recipe;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

/**
 * 耀金升级：下界合金装备 + 耀金升级模板 + 耀金锭 → 对应的耀金装备。
 *
 * <p>这是原版"锻造模板"范式的复用：模板槽放耀金升级模板、底材放下界合金装备、
 * 材料槽放耀金锭。之所以不用原版 {@code SmithingTransformRecipe} 数据文件，是因为
 * 那样每个部位要写 5 个 JSON，且无法被 {@code ForgeTableMenu} 的独立菜单共享；
 * 写成代码判断后由强化台独占；原版锻造台只保留原版模板自己的升级与纹饰。
 */
public final class YaokinUpgradeRecipe {

    /**
     * 下界合金装备 → 耀金装备 的映射。
     *
     * <p>包含四件盔甲、剑和四种工具。耀金装备不再有工作台配方，唯一来源就是用
     * 原版下界合金升级模板在强化台上升级。
     *
     * <p>剑升级成耀金剑，不是弧刀；弧刀仍然由耀金铁锤把剑重锻得到（见
     * {@code GearSmithingRecipe} 的耀金铁锤分支）。两条路径产物不同，不会混淆。
     */
    private static final Map<Item, Item> UPGRADES = Map.ofEntries(
            Map.entry(Items.NETHERITE_HELMET, ModItems.YAOKIN_HELMET.get()),
            Map.entry(Items.NETHERITE_CHESTPLATE, ModItems.YAOKIN_CHESTPLATE.get()),
            Map.entry(Items.NETHERITE_LEGGINGS, ModItems.YAOKIN_LEGGINGS.get()),
            Map.entry(Items.NETHERITE_BOOTS, ModItems.YAOKIN_BOOTS.get()),
            Map.entry(Items.NETHERITE_SWORD, ModItems.YAOKIN_SWORD.get()),
            Map.entry(Items.NETHERITE_PICKAXE, ModItems.YAOKIN_PICKAXE.get()),
            Map.entry(Items.NETHERITE_AXE, ModItems.YAOKIN_AXE.get()),
            Map.entry(Items.NETHERITE_SHOVEL, ModItems.YAOKIN_SHOVEL.get()),
            Map.entry(Items.NETHERITE_HOE, ModItems.YAOKIN_HOE.get()));

    private YaokinUpgradeRecipe() {}

    /** 底材是否是可以升为耀金的下界合金装备。 */
    public static boolean isUpgradable(ItemStack base) {
        return !base.isEmpty() && UPGRADES.containsKey(base.getItem());
    }

    /** 给定底材返回对应的耀金装备；不可升级时返回空。 */
    public static ItemStack upgrade(ItemStack base) {
        Item result = UPGRADES.get(base.getItem());
        if (result == null) return ItemStack.EMPTY;

        ItemStack out = new ItemStack(result);
        // 保留附魔与自定义名称，其它锻造状态（强化/词条/宝石）不继承——
        // 换了一件物品，旧组件的语义不再成立。
        var enchantments = net.minecraft.world.item.enchantment.EnchantmentHelper
                .getEnchantmentsForCrafting(base);
        if (!enchantments.isEmpty()) {
            net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(out, enchantments);
        }
        var name = base.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (name != null) {
            out.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, name);
        }
        // 耐久按比例保留，避免"升个级修满耐久"被当成免费修理。
        if (base.isDamageableItem() && out.isDamageableItem() && base.isDamaged()) {
            double ratio = (double) base.getDamageValue() / base.getMaxDamage();
            out.setDamageValue((int) Math.round(ratio * out.getMaxDamage()));
        }
        return out;
    }

    /** 是否使用原版下界合金升级模板。耀金不再有自己的模板。 */
    public static boolean isUpgradeTemplate(ItemStack stack) {
        return stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
    }

    /** 是否是耀金升级材料（耀金锭）。 */
    public static boolean isUpgradeMaterial(ItemStack stack) {
        return stack.is(ModItems.YAOKIN_INGOT.get());
    }

    /** 让 IDE 知道 ArmorItem 的 import 有意义（下界合金装备都是 ArmorItem 子类）。 */
    @SuppressWarnings("unused")
    private static final Class<?> ARMOR_TYPE = ArmorItem.class;
}
