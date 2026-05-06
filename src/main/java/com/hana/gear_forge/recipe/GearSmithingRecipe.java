package com.hana.gear_forge.recipe;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.GemConfig;
import com.hana.gear_forge.config.GemConfig.GemDefinition;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class GearSmithingRecipe implements SmithingRecipe {
    public static final GearSmithingRecipe INSTANCE = new GearSmithingRecipe();

    private GearSmithingRecipe() {}

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        ItemStack template = input.template();
        ItemStack base = input.base();
        ItemStack addition = input.addition();

        if (base.isEmpty() || addition.isEmpty()) return false;

        // Upgrade
        if (addition.is(ModItems.FORGE_HAMMER.get()) && GearHelper.isGearItem(base)) {
            int currentLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            UpgradeConfig config = ModConfigs.getUpgradeConfig();
            if (currentLevel >= config.getMaxLevel()) return false;
            if (!template.isEmpty() && !template.is(Items.DIAMOND)) return false;
            return template.isEmpty() || template.getCount() >= 1;
        }

        // Reroll
        if (addition.is(ModItems.REROLL_STONE.get()) && GearHelper.isGearItem(base)) {
            if (!template.isEmpty() && !template.is(Items.DIAMOND)) return false;
            return template.isEmpty() || template.getCount() >= 1;
        }

        // Gem Socket
        if (GearHelper.isGearItem(base) && isGemItem(addition)) {
            List<ModDataComponents.GemsEntry> currentGems = base.getOrDefault(ModDataComponents.GEMS.get(), List.of());
            int maxSockets = GearHelper.getMaxSockets(base);
            if (currentGems.size() >= maxSockets) return false;
            String gemItemId = getGemItemId(addition);
            if (gemItemId == null) return false;
            String socketType = GearHelper.getSocketTypeForItem(base);
            return isGemCompatible(gemItemId, socketType);
        }

        // Gem Tier Upgrade
        if (addition.is(ModItems.UPGRADE_CRYSTAL.get()) && isGemBaseItem(base)) {
            int currentTier = base.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
            return currentTier < ModConfigs.getGemConfig().getMaxTier();
        }

        // Toggle Offhand
        if (addition.is(ModItems.TRANSFER_ORB.get()) && GearHelper.isGearItem(base)) {
            return true;
        }

        // Enchant Apply (需要 enchant_compat 宝石 → 允许冲突附魔)
        if (addition.is(Items.ENCHANTED_BOOK) && GearHelper.isGearItem(base)) {
            List<ModDataComponents.GemsEntry> gems = base.get(ModDataComponents.GEMS.get());
            return gems != null && GearHelper.hasGemEffectType(gems, "enchant_compat");
        }

        // Gem Removal (逐个移除，只要有宝石就行)
        if (addition.is(ModItems.GEM_REMOVER.get()) && GearHelper.isGearItem(base)) {
            List<ModDataComponents.GemsEntry> currentGems = base.get(ModDataComponents.GEMS.get());
            return currentGems != null && !currentGems.isEmpty();
        }

        // Reincarnation
        if (addition.is(ModItems.REINCARNATION_STONE.get()) && GearHelper.isGearItem(base)) {
            UpgradeConfig config = ModConfigs.getUpgradeConfig();
            if (!config.isReincarnationEnabled()) return false;
            int currentLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            if (currentLevel < config.getMaxLevel()) return false;
            if (template.isEmpty()) return false;
            String costMaterialId = config.getReincarnationCostMaterial();
            int requiredCount = config.getReincarnationCostMaterialCount();
            return isItemMatchingId(template, costMaterialId) && template.getCount() >= requiredCount;
        }

        return false;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        ItemStack base = input.base();
        ItemStack addition = input.addition();
        ItemStack result = base.copy();

        // Upgrade
        if (addition.is(ModItems.FORGE_HAMMER.get())) {
            int currentLevel = result.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            result.set(ModDataComponents.UPGRADE_LEVEL.get(), currentLevel + 1);
            return result;
        }

        // Reroll
        if (addition.is(ModItems.REROLL_STONE.get())) {
            RandomSource random = RandomSource.create();
            List<ModDataComponents.AffixEntry> newAffixes = GearHelper.rollAffixes(result, random);
            result.set(ModDataComponents.AFFIXES.get(), newAffixes);
            return result;
        }

        // Gem Socket
        if (isGemItem(addition) && GearHelper.isGearItem(base)) {
            List<ModDataComponents.GemsEntry> currentGems = new ArrayList<>(
                    result.getOrDefault(ModDataComponents.GEMS.get(), List.of()));
            String gemItemId = getGemItemId(addition);
            if (gemItemId != null) {
                int gemTier = addition.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
                currentGems.add(new ModDataComponents.GemsEntry(findGemId(gemItemId), gemTier));
                result.set(ModDataComponents.GEMS.get(), currentGems);
            }
            return result;
        }

        // Gem Tier Upgrade
        if (addition.is(ModItems.UPGRADE_CRYSTAL.get()) && isGemBaseItem(base)) {
            int currentTier = result.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
            result.set(ModDataComponents.GEM_TIER.get(), currentTier + 1);
            result.setCount(1);   // ← 关键修复：只产出 1 个升级后的宝石
            return result;
        }

        // Toggle Offhand
        if (addition.is(ModItems.TRANSFER_ORB.get()) && GearHelper.isGearItem(base)) {
            boolean current = result.getOrDefault(ModDataComponents.OFFHAND_MODE.get(), false);
            result.set(ModDataComponents.OFFHAND_MODE.get(), !current);
            return result;
        }

        // Enchant Apply (无冲突限制，因为匹配已保证有 enchant_compat)
        if (addition.is(Items.ENCHANTED_BOOK) && GearHelper.isGearItem(base)) {
            ItemEnchantments bookEnchants = EnchantmentHelper.getEnchantmentsForCrafting(addition);
            if (!bookEnchants.isEmpty()) {
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                        EnchantmentHelper.getEnchantmentsForCrafting(result));
                for (var entry : bookEnchants.entrySet()) {
                    Holder<Enchantment> holder = entry.getKey();
                    int bookLevel = entry.getIntValue();
                    int existingLevel = mutable.getLevel(holder);
                    if (bookLevel > existingLevel) {
                        mutable.set(holder, bookLevel);
                    }
                }
                EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
            }
            return result;
        }

        // Gem Removal (逐个移除最后一个宝石)
        if (addition.is(ModItems.GEM_REMOVER.get()) && GearHelper.isGearItem(base)) {
            List<ModDataComponents.GemsEntry> currentGems = new ArrayList<>(
                    result.getOrDefault(ModDataComponents.GEMS.get(), List.of()));
            if (!currentGems.isEmpty()) {
                currentGems.remove(currentGems.size() - 1);
                if (currentGems.isEmpty()) {
                    result.remove(ModDataComponents.GEMS.get());
                } else {
                    result.set(ModDataComponents.GEMS.get(), currentGems);
                }
            }
            return result;
        }

        // Reincarnation
        if (addition.is(ModItems.REINCARNATION_STONE.get()) && GearHelper.isGearItem(base)) {
            List<ModDataComponents.StoredBonus> currentBonuses =
                    result.getOrDefault(ModDataComponents.STORED_BONUSES.get(), List.of());
            List<ModDataComponents.StoredBonus> newBonuses = GearHelper.calculateReincarnationBonuses(result);
            List<ModDataComponents.StoredBonus> merged = new ArrayList<>(currentBonuses);
            for (ModDataComponents.StoredBonus nb : newBonuses) {
                boolean found = false;
                for (int i = 0; i < merged.size(); i++) {
                    if (merged.get(i).id().equals(nb.id())) {
                        merged.set(i, new ModDataComponents.StoredBonus(nb.id(),
                                merged.get(i).value() + nb.value()));
                        found = true;
                        break;
                    }
                }
                if (!found) merged.add(nb);
            }
            result.set(ModDataComponents.STORED_BONUSES.get(), merged);
            result.set(ModDataComponents.REINCARNATION_COUNT.get(),
                    result.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0) + 1);
            result.remove(ModDataComponents.UPGRADE_LEVEL.get());
            result.remove(ModDataComponents.AFFIXES.get());
            result.remove(ModDataComponents.GEMS.get());
            return result;
        }

        return result;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        if (stack.is(Items.DIAMOND)) return true;
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        if (config.isReincarnationEnabled()) {
            String costMaterial = config.getReincarnationCostMaterial();
            if (isItemMatchingId(stack, costMaterial)) return true;
        }
        return false;
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return GearHelper.isGearItem(stack) || isGemBaseItem(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return stack.is(ModItems.FORGE_HAMMER.get())
                || stack.is(ModItems.REROLL_STONE.get())
                || stack.is(ModItems.UPGRADE_CRYSTAL.get())
                || stack.is(ModItems.TRANSFER_ORB.get())
                || stack.is(ModItems.GEM_REMOVER.get())
                || stack.is(ModItems.REINCARNATION_STONE.get())
                || stack.is(Items.ENCHANTED_BOOK)
                || isGemItem(stack);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GEAR_SMITHING.get();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.SMITHING;
    }

    @Override
    public boolean isIncomplete() {
        return false;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 1;
    }

    // ========= 辅助方法 =========

    private static boolean isGemItem(ItemStack stack) {
        return stack.is(ModItems.RUBY.get()) || stack.is(ModItems.SAPPHIRE.get())
                || stack.is(ModItems.EMERALD_GEM.get()) || stack.is(ModItems.TOPAZ.get())
                || stack.is(ModItems.JADE.get()) || stack.is(ModItems.ONYX.get())
                || stack.is(Items.DIAMOND) || stack.is(Items.NETHER_STAR);
    }

    private static boolean isGemBaseItem(ItemStack stack) {
        return stack.is(ModItems.RUBY.get()) || stack.is(ModItems.SAPPHIRE.get())
                || stack.is(ModItems.EMERALD_GEM.get()) || stack.is(ModItems.TOPAZ.get())
                || stack.is(ModItems.JADE.get()) || stack.is(ModItems.ONYX.get())
                || (stack.has(ModDataComponents.GEM_TIER.get()) && !GearHelper.isGearItem(stack));
    }

    private static String getGemItemId(ItemStack stack) {
        if (stack.is(ModItems.RUBY.get())) return "gear_forge:ruby";
        if (stack.is(ModItems.SAPPHIRE.get())) return "gear_forge:sapphire";
        if (stack.is(ModItems.EMERALD_GEM.get())) return "gear_forge:emerald_gem";
        if (stack.is(ModItems.TOPAZ.get())) return "gear_forge:topaz";
        if (stack.is(ModItems.JADE.get())) return "gear_forge:jade";
        if (stack.is(ModItems.ONYX.get())) return "gear_forge:onyx";
        if (stack.is(Items.DIAMOND)) return "minecraft:diamond";
        if (stack.is(Items.NETHER_STAR)) return "minecraft:nether_star";
        return null;
    }

    private static String findGemId(String itemId) {
        GemConfig config = ModConfigs.getGemConfig();
        for (GemDefinition gem : config.getGems()) {
            if (gem.getItemId().equals(itemId)) return gem.getId();
        }
        return itemId;
    }

    private static boolean isGemCompatible(String gemItemId, String socketType) {
        GemConfig config = ModConfigs.getGemConfig();
        for (GemDefinition gem : config.getGems()) {
            if (gem.getItemId().equals(gemItemId) && gem.getCompatibleSockets().contains(socketType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isItemMatchingId(ItemStack stack, String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        String[] parts = itemId.split(":");
        if (parts.length != 2) return false;
        return stack.is(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(parts[0], parts[1])));
    }
}