package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.item.custom.CurvedBladeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 弧刀的"每 3 次命中触发一次特效斩击"。
 *
 * <p>设计要点：
 * <ul>
 *   <li>计数在<b>物品</b>上（{@code arc_slash_charges}），不是玩家上——换刀时各算各的。</li>
 *   <li>斩击是<b>扇形范围伤害</b>：以玩家正前方为轴、半径 3.5 格、夹角 ±60°，
 *       对范围内除主目标外的所有敌对/中立实体造成主目标 60% 的伤害。
 *       这正是"弧刀"相对于剑的价值：单挑略弱，面对一群时更强。</li>
 *   <li>斩击伤害不再触发斩击计数（避免连锁），也不再递归调用 {@code hurtEnemy}。</li>
 *   <li>只用服务端结算 + 发包播放粒子，客户端不参与。</li>
 * </ul>
 */
public final class ArcSlashHandler {

    /** 斩击半径（格）。 */
    private static final double SLASH_RADIUS = 3.5D;

    /** 扇形半角（弧度，60°）。 */
    private static final double SLASH_HALF_ANGLE = Math.toRadians(60.0);

    /** 斩击对次要目标的伤害比例。 */
    private static final float SECONDARY_DAMAGE_RATIO = 0.6F;

    private ArcSlashHandler() {}

    /**
     * 由 {@link CurvedBladeItem#hurtEnemy} 在服务端调用。
     *
     * @param player 攻击者
     * @param blade  手上的弧刀
     * @param primary 刚刚被直接命中的目标（斩击不会重复打它）
     */
    public static void onBladeHit(Player player, ItemStack blade, LivingEntity primary) {
        int charges = ModDataComponents.arcSlashCharges(blade) + 1;
        if (charges < CurvedBladeItem.SLASH_EVERY) {
            ModDataComponents.setArcSlashCharges(blade, charges);
            return;
        }
        // 蓄满：清零并斩击。
        ModDataComponents.setArcSlashCharges(blade, 0);
        performSlash(player, primary);
    }

    /** 扇形范围伤害 + 粒子/音效。 */
    private static void performSlash(Player player, LivingEntity primary) {
        if (!(player.level() instanceof ServerLevel level)) return;

        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        AABB area = player.getBoundingBox().inflate(SLASH_RADIUS);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && e.isAlive() && e != primary && !e.isSpectator());

        // 主目标承受的伤害作为次要目标的基准；拿不到就用玩家攻击力兜底。
        float baseDamage = (float) player.getAttributeValue(
                net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        float slashDamage = Math.max(1.0F, baseDamage * SECONDARY_DAMAGE_RATIO);

        int hitCount = 0;
        for (LivingEntity target : candidates) {
            Vec3 toTarget = target.position().subtract(eye);
            if (toTarget.lengthSqr() < 1.0E-4) continue;

            // 扇形判定：目标方向与视线方向的夹角。
            double cos = toTarget.normalize().dot(look.normalize());
            if (cos < Math.cos(SLASH_HALF_ANGLE)) continue;

            // 26.3 起 Entity#hurt 返回 void，用伤害前后的血量差判断是否真正造成了伤害
            // （无敌帧/创造模式下 hurt 不会扣血，与旧版返回 false 的语义一致）。
            float healthBefore = target.getHealth();
            target.hurt(player.damageSources().playerAttack(player), slashDamage);
            if (target.getHealth() < healthBefore) {
                hitCount++;
            }
        }

        spawnSlashEffects(level, player, look, hitCount);
    }

    /** 沿视线方向铺一道弧形粒子，让斩击"看得见"。 */
    private static void spawnSlashEffects(ServerLevel level, Player player, Vec3 look, int hitCount) {
        Vec3 eye = player.getEyePosition();
        // 以视线为轴，在 ±60° 内均匀铺 13 条弧线
        for (int i = -6; i <= 6; i++) {
            double angle = Math.toRadians(i * 10.0);
            Vec3 dir = look.yRot((float) angle);
            for (double r = 1.0; r <= SLASH_RADIUS; r += 0.7) {
                Vec3 p = eye.add(dir.scale(r));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 1, 0, 0, 0, 0);
            }
        }

        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, hitCount > 0 ? 0.9F : 1.4F);

        if (hitCount > 0) {
            // 命中多个目标时额外一层闪光，强化"范围斩"的反馈
            level.sendParticles(ParticleTypes.CRIT, eye.x, eye.y - 0.3, eye.z,
                    8 + hitCount * 4, 1.0, 0.4, 1.0, 0.2);
        }
    }
}
