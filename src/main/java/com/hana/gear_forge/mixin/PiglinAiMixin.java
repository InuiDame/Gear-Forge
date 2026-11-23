package com.hana.gear_forge.mixin;

import com.hana.gear_forge.content.ModMobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 金化：让带有金化效果的玩家对猪灵视为穿着金装。
 *
 * <p>原版 {@link PiglinAi#isWearingGold(LivingEntity)} 只遍历盔甲槽并调用
 * {@code ItemStack.makesPiglinsNeutral}；这里在方法头直接返回 true，
 * 不改变原版对普通金装的处理。
 */
@Mixin(PiglinAi.class)
public class PiglinAiMixin {

    @Inject(method = "isWearingGold(Lnet/minecraft/world/entity/LivingEntity;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void gearForge$gildedCountsAsGold(LivingEntity entity,
                                                     CallbackInfoReturnable<Boolean> cir) {
        if (entity.hasEffect(ModMobEffects.GILDED)) {
            cir.setReturnValue(true);
        }
    }
}
