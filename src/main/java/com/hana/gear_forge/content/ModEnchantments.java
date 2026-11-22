package com.hana.gear_forge.content;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantments {
    public static final ResourceKey<Enchantment> LIGHTNING_ARROW = createKey("lightning_arrow");
    public static final ResourceKey<Enchantment> FROST_ARROW = createKey("frost_arrow");
    public static final ResourceKey<Enchantment> EXPLOSIVE_ARROW = createKey("explosive_arrow");
    public static final ResourceKey<Enchantment> ARMOR_PIERCING_ARROW = createKey("armor_piercing_arrow");
    public static final ResourceKey<Enchantment> MINING_CRIT = createKey("mining_crit");
    public static final ResourceKey<Enchantment> ORE_HUNTER = createKey("ore_hunter");
    public static final ResourceKey<Enchantment> HARVEST_BOUNTY = createKey("harvest_bounty");

    private static ResourceKey<Enchantment> createKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, name));
    }
}