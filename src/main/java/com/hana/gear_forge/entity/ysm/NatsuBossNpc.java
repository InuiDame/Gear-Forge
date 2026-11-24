package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 柚鸟夏 Boss（YSM 假玩家形态）。
 *
 * <p>三阶段（参考角色设定）：
 * <ol>
 *   <li>阶段1「浪漫家」：近战挥击 + 甜派自愈/加攻。</li>
 *   <li>阶段2「麻烦制造者」：远程甜派弹 + 更频繁自愈。</li>
 *   <li>阶段3「最终手段·Beyond The Lumination」：狂暴高速连击。</li>
 * </ol>
 * 击败后掉落"甜点部的契约"用于生成队友。
 */
public class NatsuBossNpc extends YsmNpc {

    /** Boss 总血量。三阶段共享同一条血条，按剩余血量百分比切换阶段。 */
    private static final float TOTAL_HP = 900.0F;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.gear_forge.natsu_boss"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(false).setCreateWorldFog(false);

    private final ServerLevel level;
    private int phase = 0;
    private int abilityCooldown = 100;
    private int attackCooldown = 0;
    private int phaseTransition = 0;
    private Player target;

    public NatsuBossNpc(net.minecraft.server.MinecraftServer server, ServerLevel level,
                        double x, double y, double z) {
        super(server, level, "柚鸟夏", new Vec3(x, y, z), NATSU_MODEL_ID);
        this.level = level;
        // 假玩家默认 maxHealth 只有 20，必须先把 MAX_HEALTH 属性提到 900，
        // 否则 setHealth(900) 会被 clamp 到 20，且触发"一出生就残血三阶段"。
        var attr = this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (attr != null) attr.setBaseValue(TOTAL_HP);
        this.setHealth(TOTAL_HP);
        bossEvent.setProgress(1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) return;

        if (phaseTransition > 0) {
            phaseTransition--;
            if (phaseTransition % 4 == 0) spawnBurst();
        }

        // 找目标
        if (target == null || !target.isAlive() || target.distanceTo(this) > 40) {
            target = level.getNearestPlayer(this, 40);
        }

        if (target != null) {
            // 面向目标
            Vec3 to = target.position().subtract(this.position());
            this.setYRot((float) (Math.atan2(-to.x, to.z) * 180.0 / Math.PI));

            // 靠近（阶段越高越快）——服务端直接 absMoveTo 驱动
            double dist = this.distanceTo(target);
            if (dist > 2.2 && phaseTransition <= 0) {
                this.moveToward(target.position(), 0.10 + phase * 0.04);
            } else if (dist <= 2.2 && attackCooldown <= 0 && phaseTransition <= 0) {
                attack();
            }
        }

        // 阶段切换：按总血量百分比（66% / 33%），不回血
        if (phase == 0 && this.getHealth() <= TOTAL_HP * 0.66F) enterPhase(1);
        else if (phase == 1 && this.getHealth() <= TOTAL_HP * 0.33F) enterPhase(2);

        // 甜派能力
        if (abilityCooldown <= 0 && phaseTransition <= 0 && target != null) {
            sweetPie();
            abilityCooldown = (phase == 2 ? 100 : 180);
        }
        if (abilityCooldown > 0) abilityCooldown--;
        if (attackCooldown > 0) attackCooldown--;

        bossEvent.setProgress(this.getHealth() / TOTAL_HP);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            if (p.distanceTo(this) < 40) bossEvent.addPlayer(p);
            else bossEvent.removePlayer(p);
        }
    }

    private void attack() {
        attackCooldown = (phase == 2 ? 12 : 22);
        if (target != null && target.distanceTo(this) < 3.0) {
            target.hurt(this.damageSources().mobAttack(this), 10.0F + phase * 4.0F);
            spawnBurst();
        }
    }

    private void sweetPie() {
        this.heal(10.0F + phase * 6.0F);
        this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, phase));
        if (level instanceof ServerLevel sl) {
            Vec3 p = this.position();
            sl.sendParticles(ParticleTypes.HEART, p.x, p.y + 1.5, p.z,
                    16, 1.0, 1.0, 1.0, 0.05);
        }
    }

    private void enterPhase(int newPhase) {
        phase = newPhase;
        phaseTransition = 40;   // 切阶段短暂无敌+特效，不回血
        spawnBurst();
        Component msg = Component.translatable("message.gear_forge.natsu_phase_" + (phase + 1))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
        bossEvent.setName(Component.translatable("entity.gear_forge.natsu_boss")
                .append(Component.literal(" · " + (phase + 1) + "/3")));
    }

    private void spawnBurst() {
        if (level instanceof ServerLevel sl) {
            Vec3 p = this.position();
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, p.x, p.y + 1.5, p.z,
                    40, 1.5, 2.0, 1.5, 0.1);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (phaseTransition > 0) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossEvent.removeAllPlayers();
        // 掉落队友契约
        this.spawnAtLocation(new ItemStack(ModItems.NATSU_CONTRACT.get()));
        Component msg = Component.translatable("message.gear_forge.natsu_defeated")
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        for (ServerPlayer p : level.getServer().getPlayerList().getPlayers()) {
            p.sendSystemMessage(msg);
        }
    }

    /** 从 PlayerList 移除（NPC 销毁时调用）。 */
    public void despawn() {
        bossEvent.removeAllPlayers();
        this.level.getServer().getPlayerList().remove(this);
    }
}
