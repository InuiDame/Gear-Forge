package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 柚鸟夏 Boss（真 Mob 实体，三阶段）。
 *
 * <p>玩家攻击按打怪判定生效；AI 自动寻路接近目标、不穿墙；头身朝向由原版动画处理。
 */
public class NatsuBossEntity extends NatsuMobEntity {

    private static final float TOTAL_HP = 900.0F;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.gear_forge.natsu_boss"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(false).setCreateWorldFog(false);

    private int phase = 0;
    private int abilityCooldown = 100;
    private int phaseTransition = 0;

    public NatsuBossEntity(EntityType<? extends NatsuMobEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes()
                .add(Attributes.MAX_HEALTH, TOTAL_HP)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) return;

        if (phaseTransition > 0) phaseTransition--;

        // 阶段切换：按总血量百分比（66% / 33%），不回血
        if (phase == 0 && this.getHealth() <= TOTAL_HP * 0.66F) enterPhase(1);
        else if (phase == 1 && this.getHealth() <= TOTAL_HP * 0.33F) enterPhase(2);

        // 甜派能力：自愈 + 增益 + 爱心粒子
        if (abilityCooldown <= 0 && phaseTransition <= 0) {
            this.heal(10.0F + phase * 6.0F);
            this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, phase));
            spawnParticles(ParticleTypes.HEART, 16);
            abilityCooldown = (phase == 2 ? 100 : 180);
        }
        if (abilityCooldown > 0) abilityCooldown--;

        bossEvent.setProgress(this.getHealth() / TOTAL_HP);
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                if (p.distanceTo(this) < 40) bossEvent.addPlayer(p);
                else bossEvent.removePlayer(p);
            }
        }
    }

    private void enterPhase(int newPhase) {
        phase = newPhase;
        phaseTransition = 40;
        spawnParticles(ParticleTypes.CHERRY_LEAVES, 40);
        Component msg = Component.translatable("message.gear_forge.natsu_phase_" + (phase + 1))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) p.sendSystemMessage(msg);
        }
        bossEvent.setName(Component.translatable("entity.gear_forge.natsu_boss")
                .append(Component.literal(" · " + (phase + 1) + "/3")));
    }

    private void spawnParticles(net.minecraft.core.particles.ParticleOptions type, int count) {
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            sl.sendParticles(type, this.getX(), this.getY() + 1.5, this.getZ(),
                    count, 1.0, 1.0, 1.0, 0.05);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (phaseTransition > 0) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,
                                       DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.NATSU_CONTRACT.get()));
    }

    @Override
    public void die(DamageSource source) {
        bossEvent.removeAllPlayers();
        super.die(source);
    }

    public int getPhase() {
        return phase;
    }
}
