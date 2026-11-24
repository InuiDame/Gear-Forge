package com.hana.gear_forge.item;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.ModEnchantments;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GearForgeMod.MODID);

    // 第一页：核心材料与工具
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_FORGE_CORE_TAB =
            CREATIVE_TABS.register("gear_forge_core", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GearForgeMod.MODID + ".core"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.FORGE_HAMMER.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FORGE_TABLE.get());
                        output.accept(ModItems.FORGE_HAMMER.get());
                        output.accept(ModItems.REROLL_STONE.get());
                        output.accept(ModItems.UPGRADE_CRYSTAL.get());
                        output.accept(ModItems.RUBY.get());
                        output.accept(ModItems.SAPPHIRE.get());
                        output.accept(ModItems.EMERALD_GEM.get());
                        output.accept(ModItems.TOPAZ.get());
                        output.accept(ModItems.JADE.get());
                        output.accept(ModItems.ONYX.get());
                        output.accept(ModItems.TRANSFER_ORB.get());
                        output.accept(ModItems.GEM_REMOVER.get());
                        output.accept(ModItems.REINCARNATION_STONE.get());
                        // 耀金铁锤是锻造工具，留在核心页；其余耀金装备移到「装备」页。
                        output.accept(ModItems.YAOKIN_HAMMER.get());
                    }).build());

    // 装备页：耀金套装、剑、工具、弧刀、苹果
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_FORGE_EQUIPMENT_TAB =
            CREATIVE_TABS.register("gear_forge_equipment", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GearForgeMod.MODID + ".equipment"))
                    .withTabsBefore(GEAR_FORGE_CORE_TAB.getKey())
                    .icon(() -> ModItems.YAOKIN_CHESTPLATE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.YAOKIN_INGOT.get());
                        output.accept(ModItems.YAOKIN_APPLE.get());
                        output.accept(ModItems.YAOKIN_SWORD.get());
                        output.accept(ModItems.YAOKIN_PICKAXE.get());
                        output.accept(ModItems.YAOKIN_AXE.get());
                        output.accept(ModItems.YAOKIN_SHOVEL.get());
                        output.accept(ModItems.YAOKIN_HOE.get());
                        output.accept(ModItems.DIAMOND_CURVED_BLADE.get());
                        output.accept(ModItems.NETHERITE_CURVED_BLADE.get());
                        output.accept(ModItems.YAOKIN_CURVED_BLADE.get());
                        output.accept(ModItems.YAOKIN_HELMET.get());
                        output.accept(ModItems.YAOKIN_CHESTPLATE.get());
                        output.accept(ModItems.YAOKIN_LEGGINGS.get());
                        output.accept(ModItems.YAOKIN_BOOTS.get());
                        // 樱花木系列
                        output.accept(ModItems.SAKURA_INGOT.get());
                        output.accept(ModItems.SAKURA_NUGGET.get());
                        output.accept(ModItems.SAKURA_STONE.get());
                        output.accept(ModItems.SAKURA_DRINK.get());
                        output.accept(ModItems.SAKURA_COOKIE.get());
                        output.accept(ModItems.SAKURA_SWORD.get());
                        output.accept(ModItems.SAKURA_PICKAXE.get());
                        output.accept(ModItems.SAKURA_AXE.get());
                        output.accept(ModItems.SAKURA_SHOVEL.get());
                        output.accept(ModItems.SAKURA_HOE.get());
                        output.accept(ModItems.SAKURA_HELMET.get());
                        output.accept(ModItems.SAKURA_CHESTPLATE.get());
                        output.accept(ModItems.SAKURA_LEGGINGS.get());
                        output.accept(ModItems.SAKURA_BOOTS.get());
                        // 柚鸟夏 Boss / 队友
                        output.accept(ModItems.NATSU_SUMMON.get());
                        output.accept(ModItems.NATSU_CONTRACT.get());
                        // 图中盾牌
                        output.accept(ModItems.NATSU_SHIELD.get());
                    }).build());

    // 第二页：饰品
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_FORGE_CURIOS_TAB =
            CREATIVE_TABS.register("gear_forge_curios", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GearForgeMod.MODID + ".curios"))
                    .withTabsBefore(GEAR_FORGE_EQUIPMENT_TAB.getKey())
                    .icon(() -> ModItems.LUCKY_DIAMOND.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.LUCKY_DIAMOND.get());
                        output.accept(ModItems.YAOKIN_COIN.get());
                        output.accept(ModItems.CURSED_ANVIL.get());
                    }).build());

    // 第三页：附魔书专栏
    // 三页排成一条无环的链：附魔 → 饰品 → 核心 → 战斗。
    // 旧写法让附魔页和饰品页都声明"排在核心页之前"，两者之间没有先后约束，
    // 实际顺序取决于注册顺序。
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_FORGE_ENCHANTMENT_TAB =
            CREATIVE_TABS.register("gear_forge_enchantment", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GearForgeMod.MODID + ".enchantment"))
                    .withTabsBefore(GEAR_FORGE_CURIOS_TAB.getKey())
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .displayItems((params, output) -> {
                        List<ResourceKey<Enchantment>> enchantKeys = List.of(
                                ModEnchantments.LIGHTNING_ARROW,
                                ModEnchantments.FROST_ARROW,
                                ModEnchantments.EXPLOSIVE_ARROW,
                                ModEnchantments.ARMOR_PIERCING_ARROW,
                                ModEnchantments.MINING_CRIT,
                                ModEnchantments.ORE_HUNTER,
                                ModEnchantments.HARVEST_BOUNTY
                        );

                        params.holders().lookup(Registries.ENCHANTMENT).ifPresent(enchantRegistry -> {
                            for (ResourceKey<Enchantment> key : enchantKeys) {
                                enchantRegistry.get(key).ifPresent(holder -> {
                                    int maxLevel = holder.value().getMaxLevel();
                                    for (int level = 1; level <= maxLevel; level++) {
                                        output.accept(createEnchantedBook(holder, level));
                                    }
                                });
                            }
                        });
                    }).build());

    private static ItemStack createEnchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        stack.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return stack;
    }
}