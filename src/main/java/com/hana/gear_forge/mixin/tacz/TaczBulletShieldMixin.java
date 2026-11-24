package com.hana.gear_forge.mixin.tacz;

import com.hana.gear_forge.item.ModItems;
import com.tacz.guns.util.TacHitResult;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional TaCZ integration for MC 1.21.1. TaCZ is not a runtime dependency. */
@Mixin(targets = "com.tacz.guns.entity.EntityKineticBullet")
public abstract class TaczBulletShieldMixin {

    @Inject(
            method = "onHitEntity(Lcom/tacz/guns/util/TacHitResult;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void gearForge$blockBulletWithNatsuShield(TacHitResult hitResult, Vec3 startVec, Vec3 endVec,
                                                       CallbackInfo ci) {
        Entity bullet = (Entity) (Object) this;
        if (bullet.level().isClientSide()) return;

        Entity hitEntity = hitResult.getEntity();
        if (!(hitEntity instanceof LivingEntity living)) return;
        if (!living.isBlocking()) return;

        ItemStack shield = living.getUseItem();
        if (shield.isEmpty() || !shield.is(ModItems.NATSU_SHIELD.get())) return;

        Vec3 view = living.calculateViewVector(0.0F, living.getYHeadRot());
        Vec3 toLiving = bullet.position().vectorTo(living.position());
        toLiving = new Vec3(toLiving.x, 0.0, toLiving.z);
        if (toLiving.lengthSqr() < 1.0E-6 || toLiving.normalize().dot(view) >= 0.0) return;

        if (bullet.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, living.getX(), living.getY(), living.getZ(),
                    SoundEvents.SHIELD_BLOCK, living.getSoundSource(),
                    1.0F, 0.8F + serverLevel.random.nextFloat() * 0.4F);
            shield.hurtAndBreak(1, serverLevel, living, broken -> {});
        }

        bullet.discard();
        ci.cancel();
    }
}
