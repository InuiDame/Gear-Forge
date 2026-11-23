package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class ReincarnationStoneItem extends Item {
    public ReincarnationStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.gear_forge.reincarnation_stone").withStyle(ChatFormatting.GOLD));

        // 把配置里真正需要的模板材料与数量写进简介，避免玩家还要去翻配置文件。
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        String materialId = config.getReincarnationCostMaterial();
        int count = config.getReincarnationCostMaterialCount();
        if (materialId != null && !materialId.isBlank() && count > 0) {
            Component materialName = Component.literal(materialId);
            try {
                ResourceLocation rl = ResourceLocation.tryParse(materialId);
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item != null) materialName = item.getName(new ItemStack(item));
            } catch (RuntimeException ignored) {
                // 数据包的材料 id 可能不存在，回退显示原始 id，不让它打断 tooltip。
            }
            tooltip.add(Component.translatable("tooltip.gear_forge.reincarnation_cost", count, materialName)
                    .withStyle(ChatFormatting.YELLOW));
        }

        super.appendHoverText(stack, context, tooltip, flag);
    }
}