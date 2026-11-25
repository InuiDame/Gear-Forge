package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 柚鸟夏 Boss（真 Mob 实体，三阶段远程枪战）。
 *
 * <p>三阶段对应角色形态（kivo.wiki/data/character/139）：
 * <ol>
 *   <li>原皮 · 甜点师：甜派投掷（范围伤害+爱心）+ 牛奶自愈；</li>
 *   <li>乐队 · Sugar Rush 鼓手：音波冲击波（范围击退）+ 三连点射；</li>
 *   <li>坦克 · 最终手段：Beyond The Lumination 冲锋枪扫射 + 震地冲击。</li>
 * </ol>
 * 每个阶段主手恒持专属冲锋枪、副手恒持夏之糖霜盾。
 * 技能由 tick 里的序列状态机驱动：粒子 + 音效 + 范围判定，不依赖网络同步。
 */
public class NatsuBossEntity extends NatsuMobEntity {

    private static final float TOTAL_HP = 900.0F;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.gear_forge.natsu_boss"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(false).setCreateWorldFog(false);

    private int phase = 0;
    private int abilityCooldown = 80;
    private int phaseTransition = 0;

    // 技能序列状态：0=空闲 1=甜派投掷 2=音波冲击 3=三连点射 4=SMG扫射 5=震地
    private int skill = 0;
    private int skillTick = 0;
    private int shotCount = 0;
    private Vec3 skillAim = Vec3.ZERO;

    public NatsuBossEntity(EntityType<? extends NatsuMobEntity> type, Level level) {
        super(type, level);
        equipForPhase(0);
    }

    /** 按阶段换装：主手恒为冲锋枪、副手恒为糖霜盾，护甲随阶段升级。 */
    private void equipForPhase(int newPhase) {
        ItemStack chest, legs, feet, helm;
        switch (newPhase) {
            case 1 -> {
                chest = new ItemStack(Items.GOLDEN_CHESTPLATE);
                legs = new ItemStack(Items.GOLDEN_LEGGINGS);
                feet = new ItemStack(Items.GOLDEN_BOOTS);
                helm = new ItemStack(Items.GOLDEN_HELMET);
            }
            case 2 -> {
                chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                feet = new ItemStack(Items.DIAMOND_BOOTS);
                helm = new ItemStack(Items.DIAMOND_HELMET);
            }
            default -> {
                chest = new ItemStack(Items.IRON_CHESTPLATE);
                legs = new ItemStack(Items.IRON_LEGGINGS);
                feet = new ItemStack(Items.IRON_BOOTS);
                helm = new ItemStack(Items.IRON_HELMET);
            }
        }
        this.setItemSlot(EquipmentSlot.HEAD, helm);
        this.setItemSlot(EquipmentSlot.CHEST, chest);
        this.setItemSlot(EquipmentSlot.LEGS, legs);
        this.setItemSlot(EquipmentSlot.FEET, feet);
        // 三个阶段都拿着枪
        this.setItemSlot(EquipmentSlot.MAINHAND, NatsuWeaponHelper.createGun());
        // 三个阶段都拿着盾
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.NATSU_SHIELD.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return baseAttributes()
                .add(Attributes.MAX_HEALTH, TOTAL_HP)
                .add(Attributes.MOVEMENT_SPEED, 0.34)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        // 技能/阶段/血条全是服务端逻辑；客户端实体也 tick，但绝不能强转 ClientLevel→ServerLevel。
        if (this.level().isClientSide || this.isDeadOrDying()) return;
        if (phaseTransition > 0) phaseTransition--;

        if (phase == 0 && this.getHealth() <= TOTAL_HP * 0.66F) enterPhase(1);
        else if (phase == 1 && this.getHealth() <= TOTAL_HP * 0.33F) enterPhase(2);

        // 技能序列优先执行（施法期间用 noAi 停住，让技能有前摇、不像贴脸木桩）
        if (skill != 0) {
            this.getNavigation().stop();
            tickSkill();
        } else if (abilityCooldown <= 0 && phaseTransition <= 0) {
            startSkill();
        }
        if (abilityCooldown > 0) abilityCooldown--;

        bossEvent.setProgress(this.getHealth() / TOTAL_HP);
        if (this.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                if (p.distanceTo(this) < 40) bossEvent.addPlayer(p);
                else bossEvent.removePlayer(p);
            }
        }
    }

    private ServerPlayer nearestTarget() {
        ServerLevel sl = (ServerLevel) this.level();
        ServerPlayer best = null;
        double bestDist = 32.0 * 32.0;
        for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive()) continue;
            double d = p.distanceToSqr(this);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return best;
    }

    /** 选技能：每阶段两套交替。 */
    private void startSkill() {
        ServerPlayer target = nearestTarget();
        if (target == null) return;
        skillAim = target.getEyePosition(1.0F);
        skillTick = 0;
        shotCount = 0;

        // 用总施法次数交替两套技能
        int alt = (this.tickCount / 100) % 2;
        switch (phase) {
            case 0 -> skill = alt == 0 ? 1 : 1; // 阶段1：甜派投掷 + 自愈合在序列里
            case 1 -> skill = alt == 0 ? 2 : 3; // 阶段2：音波冲击 / 三连点射
            default -> skill = alt == 0 ? 4 : 5; // 阶段3：SMG扫射 / 震地冲击
        }

        Component cast = Component.translatable("message.gear_forge.natsu_skill_" + skill)
                .withStyle(ChatFormatting.LIGHT_PURPLE);
        if (this.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) if (p.distanceTo(this) < 32) p.sendSystemMessage(cast);
        }
    }

    /** 技能序列逐 tick 驱动。 */
    private void tickSkill() {
        skillTick++;
        ServerLevel sl = (ServerLevel) this.level();

        switch (skill) {
            case 1 -> skillSweetPie(sl);
            case 2 -> skillSonicBoom(sl);
            case 3 -> skillTripleShot(sl);
            case 4 -> skillSMGBurst(sl);
            case 5 -> skillGroundSlam(sl);
            default -> { skill = 0; return; }
        }
    }

    // ===== 阶段1：甜派投掷（抛物线视觉 + 命中范围爱心伤害 + 自愈）=====
    private void skillSweetPie(ServerLevel sl) {
        // 前摇 12 tick：蓄力粒子
        if (skillTick <= 12) {
            ring(ParticleTypes.HEART, 1, 0.3);
            return;
        }
        if (skillTick == 13) {
            play(SoundEvents.SNOWBALL_THROW, 1.0F, 1.2F);
            // 抛出甜派：用 Snowball 作视觉，命中逻辑用范围判定模拟
            throwPie(sl);
        }
        if (skillTick == 26) {
            // 落点爆开：范围伤害 + 击退 + 爱心粒子
            Vec3 impact = skillAim;
            sl.sendParticles(ParticleTypes.HEART, impact.x, impact.y, impact.z, 30, 1.5, 1.0, 1.5, 0.08);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, impact.x, impact.y, impact.z, 20, 1.2, 1.0, 1.2, 0.05);
            play(SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.5F);
            areaDamage(sl, impact, 4.0F, 14.0F, 1.2, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 1));
            // 「为了这种时候准备的」：喝牛奶自愈
            this.heal(24.0F);
            this.removeEffect(MobEffects.POISON);
            this.removeEffect(MobEffects.WITHER);
        }
        if (skillTick >= 34) endSkill(150);
    }

    private void throwPie(ServerLevel sl) {
        // 用原版雪球做视觉弹道（无色无伤），真正伤害在落点范围判定
        net.minecraft.world.entity.projectile.Snowball pie =
                new net.minecraft.world.entity.projectile.Snowball(this.level(), this);
        Vec3 from = this.getEyePosition(1.0F).add(this.getLookAngle().scale(0.6));
        pie.setPos(from);
        Vec3 delta = skillAim.subtract(from);
        double flight = 26; // tick
        pie.shoot(delta.x / flight, delta.y / flight + 0.02, delta.z / flight, 1.0F, 0.0F);
        sl.addFreshEntity(pie);
    }

    // ===== 阶段2：音波冲击波（环形扩散 + 范围击退伤害）=====
    private void skillSonicBoom(ServerLevel sl) {
        if (skillTick <= 10) {
            ring(ParticleTypes.NOTE, 2, 0.2);
            return;
        }
        if (skillTick == 11) {
            play(SoundEvents.NOTE_BLOCK_BASS.value(), 1.2F, 0.8F);
            sl.sendParticles(ParticleTypes.SONIC_BOOM,
                    this.getX(), this.getY() + 1.2, this.getZ(), 3, 0, 0, 0, 0);
            // 半径 6 范围伤害 + 强击退
            areaDamage(sl, this.position(), 6.0F, 13.0F, 2.4, new MobEffectInstance(MobEffects.CONFUSION, 120, 0));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 2));
        }
        // 扩散波纹
        if (skillTick <= 30 && skillTick % 2 == 0) {
            double r = (skillTick - 10) * 0.25;
            ringAt(sl, ParticleTypes.NOTE, r, 24);
        }
        if (skillTick >= 32) endSkill(120);
    }

    // ===== 阶段2：三连点射（曳光弹道逐发命中）=====
    private void skillTripleShot(ServerLevel sl) {
        int[] fireAt = {8, 16, 24};
        if (skillTick == fireAt[0] || skillTick == fireAt[1] || skillTick == fireAt[2]) {
            play(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.3F);
            aimAtNearest(sl);
            traceShot(sl, 9.0F, ParticleTypes.CRIT);
            shotCount++;
        }
        if (skillTick >= 32) endSkill(120);
    }

    // ===== 阶段3：SMG 扫射（8 发高速曳光，边扫边微移）=====
    private void skillSMGBurst(ServerLevel sl) {
        if (skillTick <= 6) {
            ring(ParticleTypes.SMOKE, 1, 0.15);
            return;
        }
        // 每 3 tick 一发，共 8 发
        int elapsed = skillTick - 6;
        if (elapsed > 0 && elapsed % 3 == 0 && shotCount < 8) {
            aimAtNearest(sl);
            play(SoundEvents.CROSSBOW_SHOOT, 0.7F, 1.5F + shotCount * 0.02F);
            traceShot(sl, 8.0F, ParticleTypes.FLAME);
            shotCount++;
        }
        if (skillTick >= 36) endSkill(100);
    }

    // ===== 阶段3：震地冲击（跳起砸地，大范围高伤+强击退）=====
    private void skillGroundSlam(ServerLevel sl) {
        if (skillTick <= 14) {
            // 蓄力上跳
            if (skillTick == 1) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.9, 0));
                play(SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
            }
            ring(ParticleTypes.SMOKE, 1, 0.25);
            return;
        }
        if (skillTick == 15) {
            // 砸地
            this.setDeltaMovement(this.getDeltaMovement().multiply(1, -1, 1));
            play(SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.6F);
            sl.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 6, 1.5, 0.4, 1.5, 0);
            sl.sendParticles(ParticleTypes.DUST_PLUME, this.getX(), this.getY(), this.getZ(), 24, 2.0, 0.2, 2.0, 0.05);
            areaDamage(sl, this.position(), 7.0F, 20.0F, 2.8, new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
            this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
        }
        if (skillTick >= 28) endSkill(110);
    }

    // ===== 通用工具 =====

    private void aimAtNearest(ServerLevel sl) {
        ServerPlayer t = nearestTarget();
        if (t != null) skillAim = t.getEyePosition(1.0F);
    }

    /** 线性弹道：沿视线逐段生成曳光粒子，命中路径上的玩家造成伤害。 */
    private void traceShot(ServerLevel sl, float damage, ParticleOptions trail) {
        Vec3 from = this.getEyePosition(1.0F).add(this.getLookAngle().scale(0.7));
        Vec3 dir = skillAim.subtract(from).normalize();
        double maxDist = 32.0;
        Vec3 hit = null;
        for (double d = 0.5; d <= maxDist; d += 0.8) {
            Vec3 pos = from.add(dir.scale(d));
            sl.sendParticles(trail, pos.x, pos.y, pos.z, 2, 0.08, 0.08, 0.08, 0.0);
            // 命中判定：距离玩家身体中心 0.9 以内
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                if (!p.isAlive()) continue;
                Vec3 c = p.getBoundingBox().getCenter();
                if (pos.distanceTo(c) < 0.9) { hit = pos; hurtPlayer(p, damage); break; }
            }
            if (hit != null) break;
            // 撞墙即止
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(pos);
            if (!sl.getBlockState(bp).isAir()) break;
        }
    }

    /** 范围伤害 + 击退 + 可选负面效果。 */
    private void areaDamage(ServerLevel sl, Vec3 center, float radius, float damage,
                            double knockback, MobEffectInstance effect) {
        AABB box = AABB.ofSize(center, radius * 2, radius * 2, radius * 2);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == this || !e.isAlive()) continue;
            double d = e.distanceToSqr(center.x, center.y, center.z);
            if (d > radius * radius) continue;
            hurtPlayerLike(e, damage * (1.0F - (float) Math.sqrt(d) / (radius * 1.6F)));
            Vec3 diff = e.position().subtract(center);
            Vec3 kb = new Vec3(diff.x, 0, diff.z).normalize().scale(knockback);
            e.knockback(knockback, kb.x, kb.z);
            if (effect != null) e.addEffect(effect);
        }
    }

    private void hurtPlayer(ServerPlayer p, float dmg) {
        p.hurt(this.damageSources().mobAttack(this), dmg);
    }

    private void hurtPlayerLike(LivingEntity e, float dmg) {
        e.hurt(this.damageSources().mobAttack(this), Math.max(1.0F, dmg));
    }

    private void ring(ParticleOptions type, int count, double spread) {
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(type, this.getX(), this.getY() + 1.2, this.getZ(),
                    count, spread, spread, spread, 0.02);
        }
    }

    private void ringAt(ServerLevel sl, ParticleOptions type, double radius, int count) {
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            sl.sendParticles(type,
                    this.getX() + Math.cos(a) * radius, this.getY() + 0.2,
                    this.getZ() + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    private void play(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE, volume, pitch);
    }

    private void endSkill(int cooldown) {
        skill = 0;
        skillTick = 0;
        shotCount = 0;
        abilityCooldown = cooldown;
    }

    private void enterPhase(int newPhase) {
        phase = newPhase;
        phaseTransition = 40;
        skill = 0;
        equipForPhase(newPhase);
        spawnParticles(ParticleTypes.CHERRY_LEAVES, 40);
        play(SoundEvents.NOTE_BLOCK_CHIME.value(), 1.2F, 1.0F);

        bossEvent.setColor(switch (newPhase) {
            case 1 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.PURPLE;
        });

        Component msg = Component.translatable("message.gear_forge.natsu_phase_" + (phase + 1))
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
        if (this.level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) p.sendSystemMessage(msg);
        }
        bossEvent.setName(Component.translatable("entity.gear_forge.natsu_boss")
                .append(Component.literal(" · " + (phase + 1) + "/3")));
    }

    private void spawnParticles(ParticleOptions type, int count) {
        if (this.level() instanceof ServerLevel sl) {
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
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        this.spawnAtLocation(new ItemStack(ModItems.NATSU_SHIELD.get()));
        this.spawnAtLocation(NatsuWeaponHelper.createGun());

        LivingEntity killer = source.getEntity() instanceof LivingEntity le ? le : null;
        if (killer instanceof ServerPlayer sp
                && !com.hana.gear_forge.event.NatsuItemEvents.hasClaimedContract(sp)) {
            this.spawnAtLocation(new ItemStack(ModItems.NATSU_CONTRACT.get()));
            com.hana.gear_forge.event.NatsuItemEvents.markContractClaimed(sp);
        }
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
