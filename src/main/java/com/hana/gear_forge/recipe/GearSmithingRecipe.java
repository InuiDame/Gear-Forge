package com.hana.gear_forge.recipe;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.GemConfig;
import com.hana.gear_forge.config.GemConfig.GemDefinition;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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

/**
 * 全部锻造操作共用的唯一配方。
 *
 * <p>设计要点（见 docs/adr/0001、ADR-0007）：
 * <ul>
 *   <li>{@link #canApply} 是分派逻辑的<b>唯一真源</b>，{@code matches} 与 {@code assemble} 都调用它，
 *       避免出现“匹配成功但装配走空分支、材料照扣”的情况。</li>
 *   <li>所有不使用模板槽的操作都要求模板槽为空。取出时会无条件把三个输入格各扣 1 个，
 *       如果允许在模板槽放钻石/下界合金块，这些材料会被白白吞掉。</li>
 *   <li>重铸的随机结果由 {@link ModDataComponents#ROLL_SALT} 确定性推导，
 *       预览阶段反复调用 {@code assemble} 不会得到不同结果，杜绝“免费刷词条”。</li>
 * </ul>
 */
public class GearSmithingRecipe implements SmithingRecipe {

    /** 唯一实例；配方本身无状态，全部状态都在物品数据组件上。 */
    public static final GearSmithingRecipe INSTANCE = new GearSmithingRecipe();

    /**
     * 强化台专用配方类型。
     *
     * <p>刻意不挂在原版 {@link RecipeType#SMITHING} 下：原版锻造台按该类型查找配方，
     * 一旦我们的操作混进去，玩家就能在锻造台上强化/重铸/镶宝石。改挂到独立类型后，
     * 锻造台连本模组的材料都放不进槽位（槽位谓词同样按配方类型取），而只有强化台
     * ({@code ForgeTableMenu}) 会查询这个类型。原版锻造模板（下界合金升级、纹饰）
     * 仍是原版自己的 SMITHING 配方，行为不变。
     */
    public static final RecipeType<SmithingRecipe> FORGE_TABLE_TYPE =
            RecipeType.simple(ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "forge_table"));

    /** 重铸盐值到随机种子的混合常量（SplitMix64 的黄金比例常数）。 */
    private static final long SALT_MIX = 0x9E3779B97F4A7C15L;

    private GearSmithingRecipe() {}

    // ===================== 分派逻辑（唯一真源） =====================

    /**
     * 判定这组输入是否构成一次合法的锻造操作。
     *
     * <p>顺序即优先级：锻造锤 → 重铸石 → 宝石镶嵌 → 宝石升阶 → 副手切换 → 附魔书 → 宝石摘除 → 转生石。
     * {@code matches} 与 {@code assemble} 必须共用这一套判断。
     */
    private static boolean canApply(SmithingRecipeInput input) {
        ItemStack template = input.template();
        ItemStack base = input.base();
        ItemStack addition = input.addition();

        if (base.isEmpty() || addition.isEmpty()) return false;

        // 耀金升级：模板槽放耀金升级模板 + 材料槽放耀金锭 + 底材是下界合金装备。
        // 放在最前面，因为它的模板槽语义与其它操作不同（要的是特定模板而不是材料）。
        if (YaokinUpgradeRecipe.isUpgradeTemplate(template)
                && YaokinUpgradeRecipe.isUpgradeMaterial(addition)
                && YaokinUpgradeRecipe.isUpgradable(base)) {
            return true;
        }

        // 樱花木升级：同一个下界合金升级模板 + 材料槽放樱花木锭 + 底材是下界合金装备。
        if (YaokinUpgradeRecipe.isUpgradeTemplate(template)
                && SakuraUpgradeRecipe.isUpgradeMaterial(addition)
                && SakuraUpgradeRecipe.isUpgradable(base)) {
            return true;
        }

        // 锻造锤：强化 +1，模板槽放消耗材料（可配置，默认钻石）。
        if (addition.is(ModItems.FORGE_HAMMER.get()) && GearHelper.isGearItem(base)) {

            UpgradeConfig upgrade = ModConfigs.getUpgradeConfig();
            int currentLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            if (currentLevel >= upgrade.getMaxLevelSafe()) return false;
            return matchesUpgradeCost(template, upgrade, GearHelper.getUpgradeConsumeCount(base, upgrade, currentLevel));
        }

        // 重铸石：重掷词条。模板槽必须为空。
        if (addition.is(ModItems.REROLL_STONE.get()) && GearHelper.isGearItem(base)) {
            return template.isEmpty();
        }

        // 宝石镶嵌：装备 + 宝石。模板槽必须为空，否则会抢占原版纹饰配方并吞掉纹饰模板。
        if (GearHelper.isGearItem(base) && isGemItem(addition)) {
            if (!template.isEmpty()) return false;
            List<ModDataComponents.GemsEntry> currentGems = base.getOrDefault(ModDataComponents.GEMS.get(), List.of());
            if (currentGems.size() >= GearHelper.getMaxSockets(base)) return false;
            String gemItemId = getGemItemId(addition);
            if (gemItemId == null) return false;
            return isGemCompatible(gemItemId, GearHelper.getSocketTypeForItem(base));
        }

        // 升阶水晶：宝石阶级 +1。模板槽必须为空。
        if (addition.is(ModItems.UPGRADE_CRYSTAL.get()) && isGemBaseItem(base)) {
            if (!template.isEmpty()) return false;
            int currentTier = base.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
            return currentTier < ModConfigs.getGemConfig().getMaxTier();
        }

        // 转移宝珠：切换副手模式。模板槽必须为空。
        if (addition.is(ModItems.TRANSFER_ORB.get()) && GearHelper.isGearItem(base)) {
            return template.isEmpty();
        }

        // 附魔书：底材必须有 enchant_compat 宝石，且书上真的带附魔。模板槽必须为空。
        if (addition.is(Items.ENCHANTED_BOOK) && GearHelper.isGearItem(base)) {
            if (!template.isEmpty()) return false;
            List<ModDataComponents.GemsEntry> gems = base.get(ModDataComponents.GEMS.get());
            if (gems == null || !GearHelper.hasGemEffectType(gems, "enchant_compat")) return false;
            return !EnchantmentHelper.getEnchantmentsForCrafting(addition).isEmpty();
        }

        // 宝石摘除器：摘掉最后镶嵌的一颗宝石。模板槽必须为空。
        if (addition.is(ModItems.GEM_REMOVER.get()) && GearHelper.isGearItem(base)) {
            if (!template.isEmpty()) return false;
            List<ModDataComponents.GemsEntry> gems = base.get(ModDataComponents.GEMS.get());
            return gems != null && !gems.isEmpty();
        }

        // 耀金铁锤：把剑重新锻打成弧刀。模板槽必须为空（只消耗铁锤本身）。
        if (addition.is(ModItems.YAOKIN_HAMMER.get())) {
            if (!template.isEmpty()) return false;
            return isSwordLike(base);
        }

        // 转生石：装备满级 + 模板槽放下界合金块（可配置）。

        if (addition.is(ModItems.REINCARNATION_STONE.get()) && GearHelper.isGearItem(base)) {
            UpgradeConfig config = ModConfigs.getUpgradeConfig();
            if (!config.isReincarnationEnabled()) return false;
            int currentLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            if (currentLevel < config.getMaxLevel()) return false;
            int maxReincarnation = config.getMaxReincarnationCount();
            if (maxReincarnation >= 0) {
                int currentCount = base.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0);
                if (currentCount >= maxReincarnation) return false;
            }
            if (template.isEmpty()) return false;
            return isItemMatchingId(template, config.getReincarnationCostMaterial())
                    && template.getCount() >= config.getReincarnationCostMaterialCount();
        }

        return false;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return canApply(input);
    }

    /**
     * 强化是否满足模板槽的材料要求。
     *
     * <p>与转生不同，这里允许模板槽为空——只要配置的消耗数量是 0。这样既支持
     * "强化完全免费"（把 {@code consumeMaterial} 置空），也支持按等级递增的材料成本。
     */
    private static boolean matchesUpgradeCost(ItemStack template, UpgradeConfig config, int required) {
        if (required <= 0) return template.isEmpty();
        if (template.isEmpty()) return false;
        return isItemMatchingId(template, config.getConsumeMaterial()) && template.getCount() >= required;
    }

    // ===================== 装配 =====================

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        // 装配前复核分派逻辑：assemble 可能被配方书预览、机器方块或其它 mod 直接调用，
        // 不能假设调用方已经走过 matches()。
        if (!canApply(input)) return ItemStack.EMPTY;

        ItemStack addition = input.addition();
        ItemStack base = input.base();
        // 强化台每次只消耗 1 个底材，产物也必须只有 1 个，否则整组底材放进去会被复制。
        ItemStack result = base.copyWithCount(1);

        // 耀金升级：产出一件全新的耀金装备，不走 copyWithCount 的路径。
        if (YaokinUpgradeRecipe.isUpgradeTemplate(input.template())
                && YaokinUpgradeRecipe.isUpgradeMaterial(addition)
                && YaokinUpgradeRecipe.isUpgradable(base)) {
            return YaokinUpgradeRecipe.upgrade(base);
        }

        // 樱花木升级：与耀金共用模板，靠材料槽的樱花木锭区分。
        if (YaokinUpgradeRecipe.isUpgradeTemplate(input.template())
                && SakuraUpgradeRecipe.isUpgradeMaterial(addition)
                && SakuraUpgradeRecipe.isUpgradable(base)) {
            return SakuraUpgradeRecipe.upgrade(base);
        }

        if (addition.is(ModItems.FORGE_HAMMER.get())) {
            // 预览固定显示 +1：真正的成功/降级/损毁由 ForgeTableMenu 在取出时掷骰决定。
            // 这样预览是确定的（不能刷），而结果又是随机的。
            int currentLevel = result.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            result.set(ModDataComponents.UPGRADE_LEVEL.get(), currentLevel + 1);
            return result;
        }

        if (addition.is(ModItems.REROLL_STONE.get())) {
            int salt = result.getOrDefault(ModDataComponents.ROLL_SALT.get(), 0);
            RandomSource random = RandomSource.create(rerollSeed(result, salt));
            result.set(ModDataComponents.AFFIXES.get(), GearHelper.rollAffixes(result, random));
            result.set(ModDataComponents.ROLL_SALT.get(), salt + 1);
            return result;
        }

        if (isGemItem(addition)) {
            List<ModDataComponents.GemsEntry> gems = new ArrayList<>(
                    result.getOrDefault(ModDataComponents.GEMS.get(), List.of()));
            String gemItemId = getGemItemId(addition);
            if (gemItemId == null) return ItemStack.EMPTY;
            int gemTier = addition.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
            gems.add(new ModDataComponents.GemsEntry(findGemId(gemItemId), gemTier));
            result.set(ModDataComponents.GEMS.get(), List.copyOf(gems));
            return result;
        }

        if (addition.is(ModItems.UPGRADE_CRYSTAL.get())) {
            int currentTier = result.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
            result.set(ModDataComponents.GEM_TIER.get(), currentTier + 1);
            return result;
        }

        if (addition.is(ModItems.TRANSFER_ORB.get())) {
            boolean current = result.getOrDefault(ModDataComponents.OFFHAND_MODE.get(), false);
            result.set(ModDataComponents.OFFHAND_MODE.get(), !current);
            return result;
        }

        if (addition.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments bookEnchants = EnchantmentHelper.getEnchantmentsForCrafting(addition);
            ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(
                    EnchantmentHelper.getEnchantmentsForCrafting(result));
            for (var entry : bookEnchants.entrySet()) {
                Holder<Enchantment> holder = entry.getKey();
                int bookLevel = entry.getIntValue();
                if (bookLevel > mutable.getLevel(holder)) {
                    mutable.set(holder, bookLevel);
                }
            }
            EnchantmentHelper.setEnchantments(result, mutable.toImmutable());
            return result;
        }

        if (addition.is(ModItems.GEM_REMOVER.get())) {
            List<ModDataComponents.GemsEntry> gems = new ArrayList<>(
                    result.getOrDefault(ModDataComponents.GEMS.get(), List.of()));
            if (gems.isEmpty()) return ItemStack.EMPTY;
            gems.remove(gems.size() - 1);
            if (gems.isEmpty()) {
                result.remove(ModDataComponents.GEMS.get());
            } else {
                result.set(ModDataComponents.GEMS.get(), List.copyOf(gems));
            }
            return result;
        }

        if (addition.is(ModItems.YAOKIN_HAMMER.get())) {
            // 重新锻打：按底材剑的材质产出对应弧刀。弧刀是同一件装备换了形态，玩家的养成投入
            // 应当全部保留：强化等级、转生次数与存储加成、词条、宝石、附魔、自定义名称都继承过来；
            // 只有弧刀自身的斩击充能从 0 开始，且弧刀使用自己的属性/耐久（不复制原版伤害、耐久组件）。
            return inheritGearData(result, curvedBladeFor(result));
        }

        if (addition.is(ModItems.REINCARNATION_STONE.get())) {
            List<ModDataComponents.StoredBonus> currentBonuses =
                    result.getOrDefault(ModDataComponents.STORED_BONUSES.get(), List.of());
            List<ModDataComponents.StoredBonus> merged = new ArrayList<>(currentBonuses);
            for (ModDataComponents.StoredBonus nb : GearHelper.calculateReincarnationBonuses(result)) {
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
            result.set(ModDataComponents.STORED_BONUSES.get(), List.copyOf(merged));
            result.set(ModDataComponents.REINCARNATION_COUNT.get(),
                    result.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0) + 1);
            result.remove(ModDataComponents.UPGRADE_LEVEL.get());
            result.remove(ModDataComponents.AFFIXES.get());
            result.remove(ModDataComponents.GEMS.get());
            result.remove(ModDataComponents.ROLL_SALT.get());
            return result;
        }

        // canApply() 已通过却落到这里说明分派表与装配表不同步，返回空产物而不是原样复制底材。
        return ItemStack.EMPTY;
    }

    // ===================== Recipe 接口 =====================

    /**
     * 本配方产物由输入动态决定，配方书/解锁 toast 无法表达，标记为 special 以免污染配方书。
     * 这也是 1.21.1 里 {@code getResultItem()} 返回 {@link ItemStack#EMPTY} 的配套约定。
     */
    @Override
    public boolean isSpecial() {
        return true;
    }

    /** 挂到强化台专用类型，原版锻造台不会匹配到本配方的任何操作。 */
    @Override
    public RecipeType<?> getType() {
        return FORGE_TABLE_TYPE;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    /**
     * 必须显式返回 false：默认实现会因为 {@link #getIngredients()} 为空而判定为 incomplete，
     * 从而让配方被配方管理器过滤掉。
     */
    @Override
    public boolean isIncomplete() {
        return false;
    }

    /**
     * 模板槽能放什么。
     *
     * <p>这个方法是模板槽 {@code mayPlace} 的唯一依据（原版 {@code ItemCombinerMenuSlotDefinition}
     * 对每个输入槽都调用它）。它必须把<b>所有</b>操作需要的模板材料都包含进来，
     * 否则会出现"配方要求材料、但槽位不允许放入"的死锁——强化需要钻石、
     * 转生需要下界合金块，两者都必须能放进这一格。
     */
    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        if (stack.isEmpty()) return false;
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        // 强化材料
        if (config.consumesMaterial() && isItemMatchingId(stack, config.getConsumeMaterial())) {
            return true;
        }
        // 耀金升级复用原版下界合金升级模板
        if (stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)) return true;
        // 转生材料
        return config.isReincarnationEnabled()
                && isItemMatchingId(stack, config.getReincarnationCostMaterial());
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
                || stack.is(ModItems.YAOKIN_HAMMER.get())
                // 耀金锭：下界合金盔甲 → 耀金盔甲的材料槽也由本配方接管，
                // 不加入这里会让强化台直接拒绝放入，升级链整体不可用。
                || stack.is(ModItems.YAOKIN_INGOT.get())
                // 樱花木锭：下界合金盔甲 → 樱花木盔甲，与耀金共用升级模板。
                || stack.is(ModItems.SAKURA_INGOT.get())
                || stack.is(Items.ENCHANTED_BOOK)
                || isGemItem(stack);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.GEAR_SMITHING.get();
    }

    // ===================== 辅助方法 =====================

    /**
     * 派生重铸用的确定种子。
     *
     * <p>种子只由「物品注册名 + 强化等级 + 转生次数 + 盐值」决定，刻意<b>不</b>把旧的词条列表算进去：
     * 那会让同一件装备在两次预览之间产生不同的“旧内容哈希”，从而又变回可刷。
     * 盐值只在真正取出后 +1，所以：
     * <ul>
     *   <li>同一次预览反复调用 {@code assemble} → 种子相同 → 词条相同（无法免费刷新）；</li>
     *   <li>取出后再放回 → 盐值已前进 → 得到新的一套词条（重铸石被正常消耗）。</li>
     * </ul>
     */
    private static long rerollSeed(ItemStack stack, int salt) {
        long seed = BuiltInRegistries.ITEM.getKey(stack.getItem()).hashCode();
        seed = seed * 31 + stack.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
        seed = seed * 31 + stack.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0);
        seed = seed * 31 + salt;
        return seed * SALT_MIX;
    }

    /** 可作为“宝石”镶嵌进装备的物品。 */
    private static boolean isGemItem(ItemStack stack) {
        return isModGem(stack) || stack.is(Items.DIAMOND) || stack.is(Items.NETHER_STAR);
    }

    /** 可作为“宝石底材”被升阶水晶提升阶级的物品。 */
    private static boolean isGemBaseItem(ItemStack stack) {
        return isModGem(stack)
                || stack.is(Items.DIAMOND) || stack.is(Items.NETHER_STAR)
                || (stack.has(ModDataComponents.GEM_TIER.get()) && !GearHelper.isGearItem(stack));
    }

    /**
     * 是否可以被耀金铁锤锻造成弧刀。
     *
     * <p>只接受剑类（{@link net.minecraft.world.item.SwordItem}）。弧刀本身就是剑的子类，
     * 但已经是弧刀的物品不再参与，避免"弧刀 → 弧刀"这种无意义转换。
     */
    private static boolean isSwordLike(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (isCurvedBlade(stack)) return false;
        return stack.getItem() instanceof net.minecraft.world.item.SwordItem;
    }

    /** 是否已经是弧刀（避免"弧刀 → 弧刀"的无意义转换）。 */
    private static boolean isCurvedBlade(ItemStack stack) {
        return stack.is(ModItems.DIAMOND_CURVED_BLADE.get())
                || stack.is(ModItems.NETHERITE_CURVED_BLADE.get())
                || stack.is(ModItems.YAOKIN_CURVED_BLADE.get());
    }

    /**
     * 底材剑对应的弧刀：钻石剑 → 钻石弧刀，下界合金剑 → 下界合金弧刀，
     * 其余剑（含耀金剑与其它模组的剑）→ 耀金弧刀。
     */
    private static ItemStack curvedBladeFor(ItemStack base) {
        if (base.is(Items.DIAMOND_SWORD)) {
            return new ItemStack(ModItems.DIAMOND_CURVED_BLADE.get());
        }
        if (base.is(Items.NETHERITE_SWORD)) {
            return new ItemStack(ModItems.NETHERITE_CURVED_BLADE.get());
        }
        return new ItemStack(ModItems.YAOKIN_CURVED_BLADE.get());
    }

    /**
     * 把底材剑上的养成数据整块迁移到新锻出的弧刀上。
     *
     * <p>弧刀是"同一件装备换形态"，所以强化等级、转生次数、转生存储加成、词条、宝石、
     * 宝石阶级、副手模式、附魔、自定义名称全部继承。刻意不迁移的东西：
     * <ul>
     *   <li>{@code ARC_SLASH_CHARGES}：弧刀自身的斩击充能，新刀从 0 开始；</li>
     *   <li>原版属性/耐久组件：弧刀用自己 {@link com.hana.gear_forge.item.custom.CurvedBladeItem} 的伤害、攻速、范围与耐久，
     *       直接复制底材组件会覆盖掉这些数值。</li>
     * </ul>
     */
    private static ItemStack inheritGearData(ItemStack source, ItemStack target) {
        copyComponentIfPresent(source, target, ModDataComponents.UPGRADE_LEVEL.get());
        copyComponentIfPresent(source, target, ModDataComponents.REINCARNATION_COUNT.get());
        copyComponentIfPresent(source, target, ModDataComponents.STORED_BONUSES.get());
        copyComponentIfPresent(source, target, ModDataComponents.AFFIXES.get());
        copyComponentIfPresent(source, target, ModDataComponents.GEMS.get());
        copyComponentIfPresent(source, target, ModDataComponents.GEM_TIER.get());
        copyComponentIfPresent(source, target, ModDataComponents.OFFHAND_MODE.get());
        copyComponentIfPresent(source, target, ModDataComponents.ROLL_SALT.get());

        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(source);
        if (!enchantments.isEmpty()) {
            EnchantmentHelper.setEnchantments(target, enchantments);
        }
        var customName = source.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        if (customName != null) {
            target.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, customName);
        }
        return target;
    }

    /** 仅当来源物品确实带有该组件时才复制到目标。 */
    private static <T> void copyComponentIfPresent(ItemStack source, ItemStack target,
                                                   net.minecraft.core.component.DataComponentType<T> type) {
        T value = source.get(type);
        if (value != null) {
            target.set(type, value);
        }
    }

    private static boolean isModGem(ItemStack stack) {
        return stack.is(ModItems.RUBY.get()) || stack.is(ModItems.SAPPHIRE.get())
                || stack.is(ModItems.EMERALD_GEM.get()) || stack.is(ModItems.TOPAZ.get())
                || stack.is(ModItems.JADE.get()) || stack.is(ModItems.ONYX.get())
                || stack.is(ModItems.SAKURA_STONE.get());
    }

    private static String getGemItemId(ItemStack stack) {
        if (stack.is(ModItems.RUBY.get())) return "gear_forge:ruby";
        if (stack.is(ModItems.SAPPHIRE.get())) return "gear_forge:sapphire";
        if (stack.is(ModItems.EMERALD_GEM.get())) return "gear_forge:emerald_gem";
        if (stack.is(ModItems.TOPAZ.get())) return "gear_forge:topaz";
        if (stack.is(ModItems.JADE.get())) return "gear_forge:jade";
        if (stack.is(ModItems.ONYX.get())) return "gear_forge:onyx";
        if (stack.is(ModItems.SAKURA_STONE.get())) return "gear_forge:sakura_stone";
        if (stack.is(Items.DIAMOND)) return "minecraft:diamond";
        if (stack.is(Items.NETHER_STAR)) return "minecraft:nether_star";
        return null;
    }

    /** 把物品 id 映射到配置里的宝石 id（配置用 "ruby" 之类的短 id，物品是 "gear_forge:ruby"）。 */
    private static String findGemId(String itemId) {
        List<GemDefinition> gems = ModConfigs.getGemConfig().getGems();
        if (gems != null) {
            for (GemDefinition gem : gems) {
                if (itemId.equals(gem.getItemId())) return gem.getId();
            }
        }
        return itemId;
    }

    private static boolean isGemCompatible(String gemItemId, String socketType) {
        GemConfig config = ModConfigs.getGemConfig();
        List<GemDefinition> gems = config.getGems();
        if (gems == null) return false;
        for (GemDefinition gem : gems) {
            if (gemItemId.equals(gem.getItemId())) {
                return gem.getCompatibleSockets() != null && gem.getCompatibleSockets().contains(socketType);
            }
        }
        return false;
    }

    public static boolean isItemMatchingId(ItemStack stack, String itemId) {
        if (itemId == null || itemId.isEmpty()) return false;
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return false;
        return BuiltInRegistries.ITEM.getOptional(id).map(stack::is).orElse(false);
    }
}
