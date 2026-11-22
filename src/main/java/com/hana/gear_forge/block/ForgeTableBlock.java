package com.hana.gear_forge.block;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.menu.ForgeTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SmithingTableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 强化台方块。
 *
 * <p>继承原版 {@link SmithingTableBlock} 以复用它的"右键打开界面"逻辑，
 * 只把菜单换成 {@link ForgeTableMenu}。外观与锻造台一致，只是下半部分贴图换成紫色。
 *
 * <p>为什么不直接给原版锻造台加功能：原版锻造台已经被其它模组（纹饰、下界合金升级）
 * 和数据包大量使用，本模组的 8 种锻造操作会与它们争抢同一个 {@code RecipeType.SMITHING}
 * 配方列表。独立方块让玩家有明确的"这是强化台、那是锻造台"的心智模型，
 * 也避免在他人存档里改变原版方块的行为。
 */
public class ForgeTableBlock extends SmithingTableBlock {

    private static final Component CONTAINER_TITLE =
            Component.translatable("container." + GearForgeMod.MODID + ".forge_table");

    public ForgeTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (containerId, inventory, player) ->
                        new ForgeTableMenu(containerId, inventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(state.getMenuProvider(level, pos));
        player.awardStat(Stats.INTERACT_WITH_SMITHING_TABLE);
        return InteractionResult.CONSUME;
    }
}
