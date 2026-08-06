package com.hana.gear_forge.mixin;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Accessor("useItemRemaining")
    public abstract int getUseItemRemaining();

    @Accessor("useItemRemaining")
    public abstract void setUseItemRemaining(int value);

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void gearForge$modifyBowChargeSpeed(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        ItemStack item = player.getUseItem();
        if (item.isEmpty()) return;
        if (!item.is(Items.BOW) && !item.is(Items.CROSSBOW)) return;

        double totalBowChargeSpeed = 0;

        List<ModDataComponents.AffixEntry> affixes = item.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            totalBowChargeSpeed += GearHelper.getAffixTotalValueDirect(affixes, "bow_charge_speed");
        }

        List<ModDataComponents.GemsEntry> gems = item.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            totalBowChargeSpeed += GearHelper.getTotalGemEffectValue(gems, "bow_charge_speed");
        }

        List<ModDataComponents.StoredBonus> stored = item.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            totalBowChargeSpeed += GearHelper.getStoredBonusValue(stored, "bow_charge_speed");
        }

        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.isEmpty()) continue;

            List<ModDataComponents.AffixEntry> armorAffixes = armor.get(ModDataComponents.AFFIXES.get());
            if (armorAffixes != null) {
                totalBowChargeSpeed += GearHelper.getAffixTotalValueDirect(armorAffixes, "bow_charge_speed");
            }

            List<ModDataComponents.GemsEntry> armorGems = armor.get(ModDataComponents.GEMS.get());
            if (armorGems != null) {
                totalBowChargeSpeed += GearHelper.getTotalGemEffectValue(armorGems, "bow_charge_speed");
            }

            List<ModDataComponents.StoredBonus> armorStored = armor.get(ModDataComponents.STORED_BONUSES.get());
            if (armorStored != null) {
                totalBowChargeSpeed += GearHelper.getStoredBonusValue(armorStored, "bow_charge_speed");
            }
        }

        if (totalBowChargeSpeed <= 0) return;

        int totalDecrease = (int) Math.floor(totalBowChargeSpeed);
        double fraction = totalBowChargeSpeed - totalDecrease;
        if (fraction >= 0.5) {
            totalDecrease++;
        }
        totalDecrease = Math.max(1, totalDecrease);

        int old = getUseItemRemaining();
        setUseItemRemaining(Math.max(0, old - totalDecrease));

        GearForgeMod.LOGGER.info("[GearForge-Mixin] bow_charge_speed=" + totalBowChargeSpeed
                + " useItemRemaining=" + old + " -> " + getUseItemRemaining()
                + " side=" + (player.level().isClientSide() ? "CLIENT" : "SERVER"));
    }
}