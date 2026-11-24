package com.hana.gear_forge.entity.ysm;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * 柚鸟夏 Boss 基类：真正的 Mob 实体（PathfinderMob）。
 *
 * <p>相比旧的假 ServerPlayer 方案，真 Mob 拥有原版伤害判定（攻击按打怪逻辑生效）、
 * 原版 AI 寻路（跟随绕障碍、不穿墙）、原版头身旋转与行走动画。
 * YSM 柚鸟夏模型由客户端渲染代理（NatsuEntityRenderer）绘制。
 */
public abstract class NatsuMobEntity extends PathfinderMob implements YsmModelProvider {

    protected NatsuMobEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder baseAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0);
    }

    @Override
    public String getModelId() {
        return NATSU_MODEL_ID;
    }
}
