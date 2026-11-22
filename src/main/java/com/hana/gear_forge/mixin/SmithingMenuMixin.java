package com.hana.gear_forge.mixin;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让<b>原版锻造台</b>也能使用本模组的锻造操作。
 *
 * <p>强化台（{@code ForgeTableMenu}）自己实现了同样的逻辑，因为它的 {@code MenuType}
 * 与这里不同、无法共用 mixin。两处都调用 {@link GearHelper#applyUpgradeOutcome} 与
 * {@link GearHelper#consumeTemplateExtra}，判定逻辑只有一份。
 *
 * <p>这里只做两件事：
 * <ol>
 *   <li>补扣多出来的模板材料（转生需要多个、强化按等级递增）。</li>
 *   <li>强化的成功/降级/损毁掷骰——必须发生在取出时，不能放在 {@code assemble} 里，
 *       否则锻造台预览可以反复放取刷结果。</li>
 * </ol>
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(
            method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void gearForge$onTake(Player player, ItemStack result, CallbackInfo ci) {
        // 客户端只是预测，真正结算在服务端。
        if (player.level().isClientSide()) return;

        SmithingMenu self = (SmithingMenu) (Object) this;
        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
        ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        UpgradeConfig config = ModConfigs.getUpgradeConfig();

        if (addition.is(ModItems.FORGE_HAMMER.get()) && GearHelper.isGearItem(base)) {
            int baseLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            GearHelper.consumeTemplateExtra(template, config.getConsumeMaterial(),
                    GearHelper.getUpgradeConsumeCount(base, config, baseLevel));
            GearHelper.applyUpgradeOutcome(player, result, baseLevel, config);
            return;
        }

        if (addition.is(ModItems.REINCARNATION_STONE.get()) && GearHelper.isGearItem(base)) {
            int level = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
            if (config.isReincarnationEnabled() && level >= config.getMaxLevelSafe()) {
                GearHelper.consumeTemplateExtra(template, config.getReincarnationCostMaterial(),
                        config.getReincarnationCostMaterialCount());
            }
        }
    }
}
