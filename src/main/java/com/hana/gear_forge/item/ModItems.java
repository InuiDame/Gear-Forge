package com.hana.gear_forge.item;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.item.custom.*;
import net.minecraft.world.item.Item;
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
    private static DeferredItem<Item> registerItem(String name, Supplier<Item> itemSupplier) {
        return ITEMS.register(name, itemSupplier);
    }
}
