package com.hana.gear_forge.mixin;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.config.UpgradeConfig;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.recipe.GearSmithingRecipe;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 锻造台的两处"原版做不到"的补丁。
 *
 * <h2>1. 多材料补扣</h2>
 * 原版 {@link SmithingMenu#onTake} 只把三个输入槽各扣 1 个。转生需要可配置数量的模板材料
 * （默认 8 个下界合金块），强化也需要按等级递增的材料。这里在原版扣除之前补扣差额，
 * 并给原版留下 1 个，否则原版的 {@code shrinkStackInSlot} 会在空格上再做一次无意义的 shrink。
 *
 * <h2>2. 强化的成功/降级/损毁</h2>
 * 锻造台在放入物品时就算出结果并显示，取出前可以反复放取。因此随机判定<b>不能</b>放在
 * {@code assemble} 里（那样可以反复预览刷成功），必须在真正取出时（{@code onTake}）掷骰，
 * 并直接改写玩家拿到的这一份产物。{@code assemble} 只负责"显示成 +1"。
 */
@Mixin(SmithingMenu.class)
public abstract class SmithingMenuMixin {

    @Inject(
            method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD"))
    private void gearForge$onTake(Player player, ItemStack result, CallbackInfo ci) {
        SmithingMenu self = (SmithingMenu) (Object) this;

        // 客户端只是预测，真正结算在服务端；这里提前返回避免客户端多扣一次材料。
        if (player.level().isClientSide()) return;

        ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();
        ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();

        if (addition.is(ModItems.FORGE_HAMMER.get()) && GearHelper.isGearItem(base)) {
            gearForge$resolveUpgrade(self, player, result, base);
            return;
        }

        gearForge$consumeReincarnationCost(self, addition, base);
    }

    /** 强化的材料补扣 + 结果掷骰。 */
    private static void gearForge$resolveUpgrade(SmithingMenu self, Player player,
                                                 ItemStack result, ItemStack base) {
        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        int baseLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);

        // 先补扣材料：原版随后还会扣 1 个，这里只扣差额。
        int required = GearHelper.getUpgradeConsumeCount(base, config, baseLevel);
        gearForge$consumeTemplate(self, config.getConsumeMaterial(), required);

        // 再掷骰并改写结果。result 是玩家实际会拿到的那一份（服务端侧）。
        GearHelper.UpgradeOutcome outcome = GearHelper.rollUpgradeOutcome(
                config, baseLevel, player.getRandom());

        switch (outcome) {
            case SUCCESS -> {
                // assemble 已经写成 +1，保持不动。
                return;
            }
            case FAIL_KEEP -> {
                // 等级不变：把结果还原成底材的等级。
                result.set(ModDataComponents.UPGRADE_LEVEL.get(), baseLevel);
                gearForge$notify(player, "message.gear_forge.upgrade_fail");
            }
            case FAIL_DOWNGRADE -> {
                int downgraded = Math.max(0, baseLevel - config.getDowngradeLevelsSafe());
                if (downgraded <= 0) {
                    result.remove(ModDataComponents.UPGRADE_LEVEL.get());
                } else {
                    result.set(ModDataComponents.UPGRADE_LEVEL.get(), downgraded);
                }
                gearForge$notify(player, "message.gear_forge.upgrade_downgrade");
            }
            case DESTROYED -> {
                // 把结果清空，玩家拿不到任何东西——装备已被"锻毁"。
                result.setCount(0);
                gearForge$notify(player, "message.gear_forge.upgrade_destroyed");
            }
        }
    }

    /** 转生的材料补扣。 */
    private static void gearForge$consumeReincarnationCost(SmithingMenu self, ItemStack addition, ItemStack base) {
        if (!addition.is(ModItems.REINCARNATION_STONE.get()) || !GearHelper.isGearItem(base)) return;

        UpgradeConfig config = ModConfigs.getUpgradeConfig();
        if (!config.isReincarnationEnabled()) return;
        // 只有底材真的满级时才会走转生，避免误扣。
        int currentLevel = base.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
        if (currentLevel < config.getMaxLevelSafe()) return;

        gearForge$consumeTemplate(self, config.getReincarnationCostMaterial(),
                config.getReincarnationCostMaterialCount());
    }

    /**
     * 从模板槽补扣材料，给原版留 1 个。
     *
     * <p>对非法配置（数量 &lt; 1、&gt; 64）直接放弃补扣而不是抛异常：配置写错只应导致
     * 材料消耗不准确，不应让锻造台崩掉。
     */
    private static void gearForge$consumeTemplate(SmithingMenu self, String materialId, int required) {
        if (required <= 1) return;
        int extra = Math.min(required, 64) - 1;
        if (extra <= 0) return;

        ItemStack template = self.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem();
        if (!GearSmithingRecipe.isItemMatchingId(template, materialId)) return;

        int removable = Math.min(extra, template.getCount() - 1);
        if (removable <= 0) return;

        template.shrink(removable);
        self.getSlot(SmithingMenu.TEMPLATE_SLOT).setChanged();
    }

    private static void gearForge$notify(Player player, String langKey) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.displayClientMessage(Component.translatable(langKey), true);
        }
    }
}
