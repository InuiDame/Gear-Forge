package com.hana.gear_forge.mixin;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 弓/弩蓄力加速：每 tick 额外减少 useItemRemaining，加成来自手持弓弩和所穿盔甲上的
 * bow_charge_speed 词条、宝石和转生奖励。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected int useItemRemaining;

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void gearForge$modifyBowChargeSpeed(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        ItemStack item = player.getUseItem();
        if (item.isEmpty()) return;
        if (!item.is(Items.BOW) && !item.is(Items.CROSSBOW)) return;

        double totalBowChargeSpeed = gearForge$getBowChargeSpeed(item);
        for (ItemStack armor : player.getArmorSlots()) {
            if (!armor.isEmpty()) {
                totalBowChargeSpeed += gearForge$getBowChargeSpeed(armor);
            }
        }

        if (totalBowChargeSpeed <= 0) return;

        // 四舍五入，且只要有加成至少额外推进 1 tick
        int totalDecrease = Math.max(1, (int) Math.round(totalBowChargeSpeed));
        this.useItemRemaining = Math.max(0, this.useItemRemaining - totalDecrease);
    }

    private static double gearForge$getBowChargeSpeed(ItemStack stack) {
        double total = 0;

        List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            total += GearHelper.getAffixTotalValueDirect(affixes, "bow_charge_speed");
        }

        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            total += GearHelper.getTotalGemEffectValue(gems, "bow_charge_speed");
        }

        List<ModDataComponents.StoredBonus> stored = stack.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            total += GearHelper.getStoredBonusValue(stored, "bow_charge_speed");
        }

        return total;
    }
}
