package com.hana.gear_forge.mixin;

import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.recipe.GearSmithingRecipe;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版锻造台取出产物时三个输入槽各只扣 1 个。转生需要的材料数量可配置（默认 8 个下界合金块），
 * 这里在原版扣除之前补扣剩下的部分。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(method = "onTake", at = @At("HEAD"))
    private void gearForge$consumeReincarnationCost(Player player, ItemStack result, CallbackInfo ci) {
        SmithingMenu self = (SmithingMenu) (Object) this;
        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();

        if (!addition.is(ModItems.REINCARNATION_STONE.get()) || !GearHelper.isGearItem(base)) return;

        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        if (!GearSmithingRecipe.isItemMatchingId(template, config.getReincarnationCostMaterial())) return;

        // 原版随后还会再扣 1 个
        int extra = config.getReincarnationCostMaterialCount() - 1;
        if (extra > 0) {
            template.shrink(Math.min(extra, template.getCount() - 1));
        }
    }
}
