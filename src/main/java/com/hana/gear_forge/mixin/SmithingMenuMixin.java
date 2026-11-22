package com.hana.gear_forge.mixin;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.recipe.GearSmithingRecipe;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 原版 {@link SmithingMenu#onTake} 只把三个输入槽各扣 1 个，而转生需要的模板材料是可配置的
 * （默认 8 个下界合金块）。这里在原版扣除之前补扣差额，并保证给原版留下 1 个，
 * 否则原版的 {@code shrinkStackInSlot} 会在空格上再做一次无意义的 shrink。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(
            method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void gearForge$consumeReincarnationCost(Player player, ItemStack result, CallbackInfo ci) {
        SmithingMenu self = (SmithingMenu) (Object) this;

        // 只处理“这次取出确实是转生产物”的情况：result 上的转生次数必须比底材多 1。
        int resultCount = result.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0);
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        int baseCount = base.getOrDefault(ModDataComponents.REINCARNATION_COUNT.get(), 0);
        if (resultCount <= baseCount) return;

        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        int required;
        try {
            required = config.getReincarnationCostMaterialCount();
        } catch (RuntimeException e) {
            return;
        }
        // 原版稍后还会再扣 1 个，这里只补差额；对非法配置（<1 或 >64）直接放弃补扣而不是抛异常。
        int extra = Math.min(required, 64) - 1;
        if (extra <= 0) return;

        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        if (!GearSmithingRecipe.isItemMatchingId(template, config.getReincarnationCostMaterial())) return;

        int removable = Math.min(extra, template.getCount() - 1);
        if (removable <= 0) return;

        template.shrink(removable);
        self.getSlot(SmithingMenu.TEMPLATE_SLOT).setChanged();
    }
}
