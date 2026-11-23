package com.hana.gear_forge.mixin;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 弓/弩蓄力加速：每 tick 额外减少 {@code useItemRemaining}，加成来自手持弓弩和所穿盔甲上的
 * {@code bow_charge_speed} 词条、宝石与转生奖励。
 *
 * <p>用“小数累加器”而不是 {@code Math.max(1, round(bonus))}：后者会让任何大于 0 的加成
 * 都至少额外推进 1 tick（弓的 20 tick 蓄力直接变成 10 tick），与配置数值完全脱钩。
 * 累加器放在 {@link Player} 的持久数据里，跨 tick 保持小数部分。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Shadow
    protected int useItemRemaining;

    @Unique
    private static final String GEAR_FORGE_CHARGE_ACC = "gear_forge_charge_acc";

    @Inject(method = "aiStep", at = @At("TAIL"))
    private void gearForge$modifyBowChargeSpeed(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof Player player)) return;

        ItemStack item = player.getUseItem();
        if (item.isEmpty()) return;
        if (!item.is(Items.BOW) && !item.is(Items.CROSSBOW)) return;

        double total = GearHelper.getBowChargeSpeed(item);
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (!armor.isEmpty()) {
                total += GearHelper.getBowChargeSpeed(armor);
            }
        }

        var data = player.getPersistentData();
        if (total <= 0) {
            data.remove(GEAR_FORGE_CHARGE_ACC);
            return;
        }

        // 累加小数部分，攒够 1 tick 才真正推进，保证长期推进速率与配置值一致。
        double acc = data.getDoubleOr(GEAR_FORGE_CHARGE_ACC, 0.0) + total;
        int decrease = (int) acc;
        acc -= decrease;
        if (acc <= 1.0E-6) {
            data.remove(GEAR_FORGE_CHARGE_ACC);
        } else {
            data.putDouble(GEAR_FORGE_CHARGE_ACC, acc);
        }

        if (decrease > 0) {
            this.useItemRemaining = Math.max(0, this.useItemRemaining - decrease);
        }
    }
}
