package com.hana.gear_forge.menu;

import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.recipe.GearSmithingRecipe;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 强化台的菜单。
 *
 * <p>为什么不继承 {@link SmithingMenu}：原版 {@code SmithingMenu} 的构造器把
 * {@code MenuType.SMITHING} 写死传给父类，而客户端会按 {@code MenuType} 实例化菜单类。
 * 继承它的话，服务端拿到的是 {@link ForgeTableMenu}、客户端拿到的却是 {@code SmithingMenu}，
 * 两边的 {@code onTake} 行为不一致。
 *
 * <p>所以这里改为直接继承 {@link ItemCombinerMenu}，复刻 {@code SmithingMenu} 的三格布局与
 * 配方查找逻辑（约 100 行），从而可以挂在自己的 {@link MenuType} 上。
 * 布局常量与父类保持一致，界面贴图可以直接复用原版 {@code smithing.png}。
 */
public class ForgeTableMenu extends ItemCombinerMenu {

    public static final int TEMPLATE_SLOT = 0;
    public static final int BASE_SLOT = 1;
    public static final int ADDITIONAL_SLOT = 2;
    public static final int RESULT_SLOT = 3;

    private final Level level;
    private final RecipePropertySet templateItemTest;
    private final RecipePropertySet baseItemTest;
    private final RecipePropertySet additionItemTest;
    @Nullable
    private RecipeHolder<SmithingRecipe> selectedRecipe;

    public ForgeTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public ForgeTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(containerId, playerInventory, access, playerInventory.player.level());
    }

    private ForgeTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access, Level level) {
        super(ModMenus.FORGE_TABLE.get(), containerId, playerInventory, access,
                createInputSlotDefinitions(level.recipeAccess()));
        this.level = level;
        this.templateItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        this.baseItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_BASE);
        this.additionItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_ADDITION);
    }

    /**
     * 输入槽谓词：原版配方属性集 <b>或</b> 本配方自己的 ingredient 判定。
     *
     * <p>26.3 的 {@code SMITHING_*} 属性集是从所有 {@link SmithingRecipe} 的
     * {@code templateIngredient()/baseIngredient()/additionIngredient()} 提取的，
     * 与本配方挂在哪个 {@code RecipeType} 无关。但本配方是运行时分派型，这三个方法
     * 返回空/AIR，所以属性集里没有本模组的锻造锤、宝石、耀金锭、强化材料等——若只用
     * 属性集判断，这些材料根本放不进强化台。因此这里附加 {@link GearSmithingRecipe}
     * 原有的 isXxxIngredient 判定（纯静态逻辑，客户端也能用，配置已由网络层同步）。
     *
     * <p>原版锻造台仍然只用 {@code SMITHING_*} 属性集，拿不到本模组材料，自然也就
     * 无法执行本模组操作；再加上配方归档在 {@link GearSmithingRecipe#FORGE_TABLE_TYPE}，
     * 它连配方都查不到。
     */
    private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions(RecipeAccess recipes) {
        RecipePropertySet templateItemTest = recipes.propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        RecipePropertySet baseItemTest = recipes.propertySet(RecipePropertySet.SMITHING_BASE);
        RecipePropertySet additionItemTest = recipes.propertySet(RecipePropertySet.SMITHING_ADDITION);
        GearSmithingRecipe modRecipe = GearSmithingRecipe.INSTANCE;
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(TEMPLATE_SLOT, 8, 48,
                        stack -> templateItemTest.test(stack) || modRecipe.isTemplateIngredient(stack))
                .withSlot(BASE_SLOT, 26, 48,
                        stack -> baseItemTest.test(stack) || modRecipe.isBaseIngredient(stack))
                .withSlot(ADDITIONAL_SLOT, 44, 48,
                        stack -> additionItemTest.test(stack) || modRecipe.isAdditionIngredient(stack))
                .withResultSlot(RESULT_SLOT, 98, 48)
                .build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.FORGE_TABLE.get());
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return this.selectedRecipe != null
                && this.selectedRecipe.value().matches(this.createRecipeInput(), this.level);
    }

    @Override
    public void createResult() {
        SmithingRecipeInput input = this.createRecipeInput();
        Optional<RecipeHolder<SmithingRecipe>> foundRecipe;
        if (this.level instanceof ServerLevel serverLevel) {
            // 只查强化台专用配方类型：本模组的所有强化/重铸/镶嵌等操作都归档在这里，
            // 原版锻造台只查 RecipeType.SMITHING，因此匹配不到本配方。
            foundRecipe = serverLevel.recipeAccess().getRecipeFor(
                    GearSmithingRecipe.FORGE_TABLE_TYPE, input, serverLevel);
        } else {
            // 26.3 客户端只同步配方属性集，不持有完整配方，和原版 SmithingMenu 一样交给服务端计算结果。
            foundRecipe = Optional.empty();
        }
        foundRecipe.ifPresentOrElse(holder -> {
            ItemStack result = holder.value().assemble(input);
            if (result.isItemEnabled(this.level.enabledFeatures())) {
                this.selectedRecipe = holder;
                this.resultSlots.setRecipeUsed(holder);
                this.resultSlots.setItem(0, result);
                return;
            }
            this.selectedRecipe = null;
            this.resultSlots.setRecipeUsed(null);
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        }, () -> {
            this.selectedRecipe = null;
            this.resultSlots.setRecipeUsed(null);
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        });
    }

    @Override
    protected void onTake(Player player, ItemStack result) {
        result.onCraftedBy(player, result.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());

        // 客户端只做预测，真正的材料扣除与掷骰都在服务端。
        if (!player.level().isClientSide()) {
            this.resolveOutcome(player, result);
        }

        this.shrinkStackInSlot(TEMPLATE_SLOT);
        this.shrinkStackInSlot(BASE_SLOT);
        this.shrinkStackInSlot(ADDITIONAL_SLOT);
        this.access.execute((lvl, pos) -> lvl.levelEvent(1044, pos, 0));
    }

    /** 强化掷骰 / 转生材料补扣。 */
    private void resolveOutcome(Player player, ItemStack result) {
        ItemStack base = this.getSlot(BASE_SLOT).getItem();
        ItemStack addition = this.getSlot(ADDITIONAL_SLOT).getItem();
        ItemStack template = this.getSlot(TEMPLATE_SLOT).getItem();
        UpgradeConfig config = ModConfigs.getUpgradeConfig();

        if (addition.is(ModItems.FORGE_HAMMER.get()) && GearHelper.isGearItem(base)) {
            int baseLevel = base.getOrDefault(
                    com.hana.gear_forge.component.ModDataComponents.UPGRADE_LEVEL.get(), 0);
            GearHelper.consumeTemplateExtra(template, config.getConsumeMaterial(),
                    GearHelper.getUpgradeConsumeCount(base, config, baseLevel));
            GearHelper.applyUpgradeOutcome(player, result, baseLevel, config);
            return;
        }

        if (addition.is(ModItems.REINCARNATION_STONE.get()) && GearHelper.isGearItem(base)) {
            int level = base.getOrDefault(
                    com.hana.gear_forge.component.ModDataComponents.UPGRADE_LEVEL.get(), 0);
            if (config.isReincarnationEnabled() && level >= config.getMaxLevelSafe()) {
                GearHelper.consumeTemplateExtra(template, config.getReincarnationCostMaterial(),
                        config.getReincarnationCostMaterialCount());
            }
        }
    }

    private List<ItemStack> getRelevantItems() {
        return List.of(this.inputSlots.getItem(TEMPLATE_SLOT),
                this.inputSlots.getItem(BASE_SLOT),
                this.inputSlots.getItem(ADDITIONAL_SLOT));
    }

    private SmithingRecipeInput createRecipeInput() {
        return new SmithingRecipeInput(this.inputSlots.getItem(TEMPLATE_SLOT),
                this.inputSlots.getItem(BASE_SLOT),
                this.inputSlots.getItem(ADDITIONAL_SLOT));
    }

    private void shrinkStackInSlot(int index) {
        ItemStack stack = this.inputSlots.getItem(index);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            this.inputSlots.setItem(index, stack);
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    protected boolean canMoveIntoInputSlots(ItemStack stack) {
        return this.findSlotToQuickMoveTo(stack).isPresent();
    }

    /** 与输入槽谓词保持一致：属性集或本配方判定，二者命中其一即可放入。 */
    private OptionalInt findSlotToQuickMoveTo(ItemStack stack) {
        GearSmithingRecipe modRecipe = GearSmithingRecipe.INSTANCE;
        if (!this.getSlot(TEMPLATE_SLOT).hasItem()
                && (this.templateItemTest.test(stack) || modRecipe.isTemplateIngredient(stack))) {
            return OptionalInt.of(TEMPLATE_SLOT);
        }
        if (!this.getSlot(BASE_SLOT).hasItem()
                && (this.baseItemTest.test(stack) || modRecipe.isBaseIngredient(stack))) {
            return OptionalInt.of(BASE_SLOT);
        }
        return !this.getSlot(ADDITIONAL_SLOT).hasItem()
                && (this.additionItemTest.test(stack) || modRecipe.isAdditionIngredient(stack))
                ? OptionalInt.of(ADDITIONAL_SLOT) : OptionalInt.empty();
    }

    /** 供客户端界面判断是否显示"缺材料"提示。 */
    public boolean hasRecipeError() {
        return this.getSlot(TEMPLATE_SLOT).hasItem()
                && this.getSlot(BASE_SLOT).hasItem()
                && this.getSlot(ADDITIONAL_SLOT).hasItem()
                && !this.getSlot(RESULT_SLOT).hasItem();
    }

    /** 界面贴图沿用原版锻造台。 */
    public static final net.minecraft.resources.Identifier GUI_TEXTURE =
            net.minecraft.resources.Identifier.withDefaultNamespace("textures/gui/container/smithing.png");
}
