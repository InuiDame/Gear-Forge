package com.hana.gear_forge.item;

import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.compat.curios.CursedAnvilItem;
import com.hana.gear_forge.compat.curios.LuckyDiamondItem;
import com.hana.gear_forge.compat.curios.YaokinCoinItem;
import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.YaokinMaterial;
import com.hana.gear_forge.item.custom.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;


public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GearForgeMod.MODID);

    public static final DeferredItem<Item> REROLL_STONE = registerItem("reroll_stone",
            RerollStoneItem::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> FORGE_HAMMER = registerItem("forge_hammer",
            ForgeHammerItem::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> RUBY = registerItem("ruby",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> SAPPHIRE = registerItem("sapphire",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> EMERALD_GEM = registerItem("emerald_gem",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> UPGRADE_CRYSTAL = registerItem("upgrade_crystal",
            UpgradeCrystalItem::new, () -> new Item.Properties().stacksTo(64));


    public static final DeferredItem<Item> TOPAZ = registerItem("topaz",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> JADE = registerItem("jade",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> ONYX = registerItem("onyx",
            Item::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> TRANSFER_ORB = registerItem("transfer_orb",
            TransferOrbItem::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> GEM_REMOVER = registerItem("gem_remover",
            GemRemoverItem::new, () -> new Item.Properties().stacksTo(64));

    public static final DeferredItem<Item> REINCARNATION_STONE = registerItem("reincarnation_stone",
            ReincarnationStoneItem::new, () -> new Item.Properties().fireResistant().stacksTo(64));

    public static final DeferredItem<Item> LUCKY_DIAMOND = registerItem(
            "lucky_diamond",
            LuckyDiamondItem::new, () -> new Item.Properties().stacksTo(1).fireResistant());

    public static final DeferredItem<Item> CURSED_ANVIL = registerItem(
            "cursed_anvil",
            CursedAnvilItem::new, () -> new Item.Properties().stacksTo(1).fireResistant());

    /** 耀金硬币：小饰品，幸运 +1，并强化幸运钻石、击杀时给点金。 */
    public static final DeferredItem<YaokinCoinItem> YAOKIN_COIN = registerTyped(
            "yaokin_coin",
            YaokinCoinItem::new, () -> new Item.Properties().stacksTo(1).fireResistant());

    /** 强化台的方块物品。 */
    public static final DeferredItem<BlockItem> FORGE_TABLE = ITEMS.registerSimpleBlockItem(
            ModBlocks.FORGE_TABLE);

    // ===================== 耀金 =====================

    /** 耀金锭：3 金锭 + 3 钻石合成，用于升级下界合金装备与修复耀金物品。 */
    public static final DeferredItem<Item> YAOKIN_INGOT = registerItem("yaokin_ingot",
            Item::new, () -> new Item.Properties().fireResistant());

    /** 耀金铁锤：把剑重新锻打成对应材质的弧刀。 */
    public static final DeferredItem<Item> YAOKIN_HAMMER = registerItem("yaokin_hammer",
            YaokinHammerItem::new, () -> new Item.Properties().stacksTo(1).fireResistant());

    /** 耀金苹果：普通金苹果的强化版，附带点金 II。 */
    public static final DeferredItem<YaokinAppleItem> YAOKIN_APPLE = registerTyped("yaokin_apple",
            YaokinAppleItem::new, () -> new Item.Properties().stacksTo(16)
                    .food(new net.minecraft.world.food.FoodProperties.Builder()
                            .nutrition(6).saturationModifier(1.6F).alwaysEdible().build()));

    /** 耀金剑：标准剑属性，8 点伤害、1.6 攻速。 */
    public static final DeferredItem<Item> YAOKIN_SWORD = registerItem("yaokin_sword",
            Item::new, () -> new Item.Properties().sword(YaokinMaterial.TOOL, 3.0F, -2.4F).fireResistant());

    /** 耀金镐。 */
    public static final DeferredItem<Item> YAOKIN_PICKAXE = registerItem("yaokin_pickaxe",
            Item::new, () -> new Item.Properties().pickaxe(YaokinMaterial.TOOL, 1.0F, -2.8F).fireResistant());

    /** 耀金斧。 */
    public static final DeferredItem<Item> YAOKIN_AXE = registerItem("yaokin_axe",
            Item::new, () -> new Item.Properties().axe(YaokinMaterial.TOOL, 5.0F, -3.0F).fireResistant());

    /** 耀金锹。 */
    public static final DeferredItem<Item> YAOKIN_SHOVEL = registerItem("yaokin_shovel",
            Item::new, () -> new Item.Properties().shovel(YaokinMaterial.TOOL, 1.5F, -3.0F).fireResistant());

    /** 耀金锄。 */
    public static final DeferredItem<Item> YAOKIN_HOE = registerItem("yaokin_hoe",
            Item::new, () -> new Item.Properties().hoe(YaokinMaterial.TOOL, -4.0F, 0.0F).fireResistant());

    // ===================== 弧刀 =====================

    /** 钻石弧刀：钻石剑用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> DIAMOND_CURVED_BLADE =
            registerTyped("diamond_curved_blade",
                    CurvedBladeItem::new, () -> CurvedBladeItem.createBladeProperties(ToolMaterial.DIAMOND));

    /** 下界合金弧刀：下界合金剑用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> NETHERITE_CURVED_BLADE =
            registerTyped("netherite_curved_blade",
                    CurvedBladeItem::new, () -> CurvedBladeItem.createBladeProperties(ToolMaterial.NETHERITE).fireResistant());

    /** 耀金弧刀：耀金剑（或其它剑）用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> YAOKIN_CURVED_BLADE =
            registerTyped("yaokin_curved_blade",
                    CurvedBladeItem::new, () -> CurvedBladeItem.createBladeProperties(YaokinMaterial.TOOL).fireResistant());

    // ===================== 耀金套装 =====================

    public static final DeferredItem<Item> YAOKIN_HELMET = registerItem("yaokin_helmet",
            Item::new, () -> new Item.Properties()
                    .humanoidArmor(YaokinMaterial.ARMOR, ArmorType.HELMET).fireResistant());

    public static final DeferredItem<Item> YAOKIN_CHESTPLATE = registerItem("yaokin_chestplate",
            Item::new, () -> new Item.Properties()
                    .humanoidArmor(YaokinMaterial.ARMOR, ArmorType.CHESTPLATE).fireResistant());

    public static final DeferredItem<Item> YAOKIN_LEGGINGS = registerItem("yaokin_leggings",
            Item::new, () -> new Item.Properties()
                    .humanoidArmor(YaokinMaterial.ARMOR, ArmorType.LEGGINGS).fireResistant());

    public static final DeferredItem<Item> YAOKIN_BOOTS = registerItem("yaokin_boots",
            Item::new, () -> new Item.Properties()
                    .humanoidArmor(YaokinMaterial.ARMOR, ArmorType.BOOTS).fireResistant());

    // 26.3 起 Item.Properties 必须带 id，否则 Item 构造时抛 "Item id not set"。
    // registerItem(name, factory, props) 由 NeoForge 自动给 props 注入 setId。
    private static DeferredItem<Item> registerItem(String name,
            java.util.function.Function<Item.Properties, Item> factory,
            Supplier<Item.Properties> props) {
        return ITEMS.registerItem(name, factory, props);
    }

    private static <T extends Item> DeferredItem<T> registerTyped(String name,
            java.util.function.Function<Item.Properties, T> factory,
            Supplier<Item.Properties> props) {
        return ITEMS.registerItem(name, factory, props);
    }
}
