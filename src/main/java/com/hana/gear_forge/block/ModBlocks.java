package com.hana.gear_forge.block;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 方块注册。方块物品在 {@link com.hana.gear_forge.item.ModItems} 里一并注册。 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(GearForgeMod.MODID);

    /**
     * 强化台。
     *
     * <p>属性照抄原版锻造台：金属音、硬度 2.5、抗爆 6.0。刻意不设
     * {@code requiresCorrectToolForDrops()}——原版锻造台任何工具都能掉落，保持一致，
     * 避免玩家挖错工具白挖。
     */
    public static final DeferredBlock<ForgeTableBlock> FORGE_TABLE =
            BLOCKS.registerBlock("forge_table",
                    ForgeTableBlock::new,
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.5F, 6.0F)
                            .sound(SoundType.METAL));

    private ModBlocks() {}
}
