package com.hana.gear_forge.item.custom;

import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

public class ReincarnationStoneItem extends Item {
    public ReincarnationStoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.gear_forge.reincarnation_stone").withStyle(ChatFormatting.GOLD));

        // 把配置里真正需要的模板材料与数量写进简介，避免玩家还要去翻配置文件。
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        String materialId = config.getReincarnationCostMaterial();
        int count = config.getReincarnationCostMaterialCount();
        if (materialId != null && !materialId.isBlank() && count > 0) {
            Component materialName = Component.literal(materialId);
            try {
                Identifier rl = Identifier.tryParse(materialId);
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item.isPresent()) {
                    var holder = item.get();
                    materialName = holder.value().getName(new ItemStack(holder));
                }
            } catch (RuntimeException ignored) {
                // 数据包的材料 id 可能不存在，回退显示原始 id，不让它打断 tooltip。
            }
            tooltip.accept(Component.translatable("tooltip.gear_forge.reincarnation_cost", count, materialName)
                    .withStyle(ChatFormatting.YELLOW));
        }

        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
