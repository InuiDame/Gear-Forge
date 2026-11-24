package com.hana.gear_forge.mixin;

import net.minecraft.world.entity.WalkAnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 暴露 WalkAnimationState 的私有相位字段，供 YSM 渲染代理精确同步行走动画。 */
@Mixin(WalkAnimationState.class)
public interface WalkAnimationAccessor {

    @Accessor("speedOld")
    void gearForge$setSpeedOld(float value);

    @Accessor("speed")
    void gearForge$setSpeed(float value);

    @Accessor("position")
    void gearForge$setPosition(float value);

    @Accessor("position")
    float gearForge$getPosition();

    @Accessor("speed")
    float gearForge$getSpeed();

    @Accessor("speedOld")
    float gearForge$getSpeedOld();
}
