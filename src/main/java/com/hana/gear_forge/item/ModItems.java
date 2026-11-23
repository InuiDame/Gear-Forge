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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(GearForgeMod.MODID);

    public static final DeferredItem<Item> REROLL_STONE = registerItem("reroll_stone",
            () -> new RerollStoneItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> FORGE_HAMMER = registerItem("forge_hammer",
            () -> new ForgeHammerItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> RUBY = registerItem("ruby",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> SAPPHIRE = registerItem("sapphire",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> EMERALD_GEM = registerItem("emerald_gem",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> UPGRADE_CRYSTAL = registerItem("upgrade_crystal",
            () -> new UpgradeCrystalItem(new Item.Properties().stacksTo(64)));


    public static final DeferredItem<Item> TOPAZ = registerItem("topaz",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> JADE = registerItem("jade",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> ONYX = registerItem("onyx",
            () -> new Item(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> TRANSFER_ORB = registerItem("transfer_orb",
            () -> new TransferOrbItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> GEM_REMOVER = registerItem("gem_remover",
            () -> new GemRemoverItem(new Item.Properties().stacksTo(64)));

    public static final DeferredItem<Item> REINCARNATION_STONE = registerItem("reincarnation_stone",
            () -> new ReincarnationStoneItem(new Item.Properties().fireResistant().stacksTo(64)));

    public static final DeferredItem<Item> LUCKY_DIAMOND = registerItem(
            "lucky_diamond",
            LuckyDiamondItem::new);

    public static final DeferredItem<Item> CURSED_ANVIL = registerItem(
            "cursed_anvil",
            CursedAnvilItem::new);

    /** 耀金硬币：小饰品，幸运 +1，并强化幸运钻石、击杀时给点金。 */
    public static final DeferredItem<YaokinCoinItem> YAOKIN_COIN = registerTyped(
            "yaokin_coin",
            YaokinCoinItem::new);

    /** 强化台的方块物品。 */
    public static final DeferredItem<BlockItem> FORGE_TABLE = ITEMS.registerSimpleBlockItem(
            ModBlocks.FORGE_TABLE);

    // ===================== 耀金 =====================

    /** 耀金锭：3 金锭 + 3 钻石合成，用于升级下界合金装备与修复耀金物品。 */
    public static final DeferredItem<Item> YAOKIN_INGOT = registerItem("yaokin_ingot",
            () -> new Item(new Item.Properties().fireResistant()));

    /** 耀金铁锤：把剑重新锻打成对应材质的弧刀。 */
    public static final DeferredItem<Item> YAOKIN_HAMMER = registerItem("yaokin_hammer",
            () -> new YaokinHammerItem(new Item.Properties().stacksTo(1).fireResistant()));

    /** 耀金苹果：普通金苹果的强化版，附带点金 II。 */
    public static final DeferredItem<YaokinAppleItem> YAOKIN_APPLE = registerTyped("yaokin_apple",
            YaokinAppleItem::new);

    /** 耀金剑：标准剑属性，8 点伤害、1.6 攻速。 */
    public static final DeferredItem<SwordItem> YAOKIN_SWORD = registerTyped("yaokin_sword",
            () -> new SwordItem(YaokinMaterial.TIER, new Item.Properties()
                    .fireResistant()
                    .attributes(SwordItem.createAttributes(YaokinMaterial.TIER, 3, -2.4F))));

    /** 耀金镐。 */
    public static final DeferredItem<PickaxeItem> YAOKIN_PICKAXE = registerTyped("yaokin_pickaxe",
            () -> new PickaxeItem(YaokinMaterial.TIER, new Item.Properties()
                    .fireResistant()
                    .attributes(PickaxeItem.createAttributes(YaokinMaterial.TIER, 1.0F, -2.8F))));

    /** 耀金斧。 */
    public static final DeferredItem<AxeItem> YAOKIN_AXE = registerTyped("yaokin_axe",
            () -> new AxeItem(YaokinMaterial.TIER, new Item.Properties()
                    .fireResistant()
                    .attributes(AxeItem.createAttributes(YaokinMaterial.TIER, 5.0F, -3.0F))));

    /** 耀金锹。 */
    public static final DeferredItem<ShovelItem> YAOKIN_SHOVEL = registerTyped("yaokin_shovel",
            () -> new ShovelItem(YaokinMaterial.TIER, new Item.Properties()
                    .fireResistant()
                    .attributes(ShovelItem.createAttributes(YaokinMaterial.TIER, 1.5F, -3.0F))));

    /** 耀金锄。 */
    public static final DeferredItem<HoeItem> YAOKIN_HOE = registerTyped("yaokin_hoe",
            () -> new HoeItem(YaokinMaterial.TIER, new Item.Properties()
                    .fireResistant()
                    .attributes(HoeItem.createAttributes(YaokinMaterial.TIER, -4.0F, 0.0F))));

    // ===================== 弧刀 =====================

    /** 钻石弧刀：钻石剑用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> DIAMOND_CURVED_BLADE =
            registerTyped("diamond_curved_blade",
                    () -> new CurvedBladeItem(Tiers.DIAMOND, new Item.Properties()
                            .attributes(CurvedBladeItem.createBladeAttributes(Tiers.DIAMOND))));

    /** 下界合金弧刀：下界合金剑用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> NETHERITE_CURVED_BLADE =
            registerTyped("netherite_curved_blade",
                    () -> new CurvedBladeItem(Tiers.NETHERITE, new Item.Properties()
                            .fireResistant()
                            .attributes(CurvedBladeItem.createBladeAttributes(Tiers.NETHERITE))));

    /** 耀金弧刀：耀金剑（或其它剑）用耀金铁锤重锻而成。 */
    public static final DeferredItem<CurvedBladeItem> YAOKIN_CURVED_BLADE =
            registerTyped("yaokin_curved_blade",
                    () -> new CurvedBladeItem(YaokinMaterial.TIER, new Item.Properties()
                            .fireResistant()
                            .attributes(CurvedBladeItem.createBladeAttributes(YaokinMaterial.TIER))));

    // ===================== 耀金套装 =====================

    public static final DeferredItem<ArmorItem> YAOKIN_HELMET = registerTyped("yaokin_helmet",
            () -> new ArmorItem(YaokinMaterial.ARMOR, ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant()));

    public static final DeferredItem<ArmorItem> YAOKIN_CHESTPLATE = registerTyped("yaokin_chestplate",
            () -> new ArmorItem(YaokinMaterial.ARMOR, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().fireResistant()));

    public static final DeferredItem<ArmorItem> YAOKIN_LEGGINGS = registerTyped("yaokin_leggings",
            () -> new ArmorItem(YaokinMaterial.ARMOR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().fireResistant()));

    public static final DeferredItem<ArmorItem> YAOKIN_BOOTS = registerTyped("yaokin_boots",
            () -> new ArmorItem(YaokinMaterial.ARMOR, ArmorItem.Type.BOOTS,
                    new Item.Properties().fireResistant()));

    private static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }

    /** 泛型重载：用于 {@link ArmorItem} 这类需要保留具体类型的注册。 */
    private static <T extends Item> DeferredItem<T> registerTyped(String name, Supplier<T> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }
}
