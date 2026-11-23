package com.hana.gear_forge.item;

import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.compat.curios.CursedAnvilItem;
import com.hana.gear_forge.compat.curios.LuckyDiamondItem;
import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.YaokinMaterial;
import com.hana.gear_forge.item.custom.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** 强化台的方块物品。 */
    public static final DeferredItem<BlockItem> FORGE_TABLE = ITEMS.registerSimpleBlockItem(
            ModBlocks.FORGE_TABLE);

    // ===================== 耀金 =====================

    /** 耀金锭：3 金锭 + 3 钻石合成，用于升级下界合金装备与修复耀金物品。 */
    public static final DeferredItem<Item> YAOKIN_INGOT = registerItem("yaokin_ingot",
            () -> new Item(new Item.Properties().fireResistant()));

    /** 耀金升级锻造模板：下界合金装备 + 耀金锭 + 本模板 → 耀金装备。 */
    public static final DeferredItem<Item> YAOKIN_UPGRADE_SMITHING_TEMPLATE =
            registerItem("yaokin_upgrade_smithing_template",
                    () -> new net.minecraft.world.item.SmithingTemplateItem(
                            // appliesTo / ingredients / 标题 / 两个槽位说明 / 两个槽位图标
                            Component.translatable("item.gear_forge.yaokin_upgrade_smithing_template.applies_to")
                                    .withStyle(ChatFormatting.BLUE),
                            Component.translatable("item.gear_forge.yaokin_upgrade_smithing_template.ingredients")
                                    .withStyle(ChatFormatting.BLUE),
                            Component.translatable("upgrade.gear_forge.yaokin_upgrade")
                                    .withStyle(ChatFormatting.GRAY),
                            Component.translatable("item.gear_forge.yaokin_upgrade_smithing_template.base_slot_description"),
                            Component.translatable("item.gear_forge.yaokin_upgrade_smithing_template.additions_slot_description"),
                            java.util.List.of(
                                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                                    ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")),
                            java.util.List.of(
                                    ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"))));

    /** 耀金铁锤：把剑重新锻打成弧刀。 */
    public static final DeferredItem<Item> YAOKIN_HAMMER = registerItem("yaokin_hammer",
            () -> new YaokinHammerItem(new Item.Properties().stacksTo(1).fireResistant()));

    /** 耀金弧刀。 */
    public static final DeferredItem<Item> YAOKIN_CURVED_BLADE = registerItem("yaokin_curved_blade",
            () -> new YaokinCurvedBladeItem(new Item.Properties()
                    .fireResistant()
                    .attributes(YaokinCurvedBladeItem.createBladeAttributes())));

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
