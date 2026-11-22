package com.hana.gear_forge.menu;

import com.hana.gear_forge.block.ModBlocks;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
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
    private final List<RecipeHolder<SmithingRecipe>> recipes;
    @Nullable
    private RecipeHolder<SmithingRecipe> selectedRecipe;

    public ForgeTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public ForgeTableMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(ModMenus.FORGE_TABLE.get(), containerId, playerInventory, access);
        this.level = playerInventory.player.level();
        this.recipes = this.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING);
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(TEMPLATE_SLOT, 8, 48,
                        stack -> this.recipes.stream().anyMatch(r -> r.value().isTemplateIngredient(stack)))
                .withSlot(BASE_SLOT, 26, 48,
                        stack -> this.recipes.stream().anyMatch(r -> r.value().isBaseIngredient(stack)))
                .withSlot(ADDITIONAL_SLOT, 44, 48,
                        stack -> this.recipes.stream().anyMatch(r -> r.value().isAdditionIngredient(stack)))
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
        List<RecipeHolder<SmithingRecipe>> matches =
                this.level.getRecipeManager().getRecipesFor(RecipeType.SMITHING, input, this.level);
        if (matches.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            this.selectedRecipe = null;
            return;
        }
        RecipeHolder<SmithingRecipe> holder = matches.get(0);
        ItemStack result = holder.value().assemble(input, this.level.registryAccess());
        if (result.isItemEnabled(this.level.enabledFeatures())) {
            this.selectedRecipe = holder;
            this.resultSlots.setRecipeUsed(holder);
            this.resultSlots.setItem(0, result);
        }
    }

    @Override
    protected void onTake(Player player, ItemStack result) {
        result.onCraftedBy(player.level(), player, result.getCount());
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

    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        return this.findSlotToQuickMoveTo(stack).orElse(TEMPLATE_SLOT);
    }

    private OptionalInt findSlotToQuickMoveTo(ItemStack stack) {
        return this.recipes.stream()
                .flatMapToInt(holder -> findSlotMatchingIngredient(holder.value(), stack).stream())
                .filter(slot -> !this.getSlot(slot).hasItem())
                .findFirst();
    }

    private static OptionalInt findSlotMatchingIngredient(SmithingRecipe recipe, ItemStack stack) {
        if (recipe.isTemplateIngredient(stack)) return OptionalInt.of(TEMPLATE_SLOT);
        if (recipe.isBaseIngredient(stack)) return OptionalInt.of(BASE_SLOT);
        return recipe.isAdditionIngredient(stack) ? OptionalInt.of(ADDITIONAL_SLOT) : OptionalInt.empty();
    }

    /** 供客户端界面判断是否显示"缺材料"提示。 */
    public boolean hasRecipeError() {
        return this.getSlot(TEMPLATE_SLOT).hasItem()
                && this.getSlot(BASE_SLOT).hasItem()
                && this.getSlot(ADDITIONAL_SLOT).hasItem()
                && !this.getSlot(RESULT_SLOT).hasItem();
    }

    /** 界面贴图沿用原版锻造台。 */
    public static final net.minecraft.resources.ResourceLocation GUI_TEXTURE =
            net.minecraft.resources.ResourceLocation.withDefaultNamespace("textures/gui/container/smithing.png");
}
