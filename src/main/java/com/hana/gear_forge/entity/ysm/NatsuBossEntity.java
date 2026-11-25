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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * 柚鸟夏 Boss（真 Mob 实体，三阶段）。
 *
 * <p>三阶段参照角色资料（kivo.wiki/data/character/139）的三个形态：
 * <ol>
 *   <li>原皮 · 浪漫主义者：甜派加攻、牛奶自愈（粉色）；</li>
 *   <li>乐队 · Sugar Rush 鼓手：节奏加速、攻速提升（金色）；</li>
 *   <li>坦克 · 最终手段 Beyond The Lumination：重装甲、高伤（紫色）。</li>
 * </ol>
 * 每个阶段副手都持有夏之糖霜盾，主手武器随阶段升级。
 */
public class NatsuBossEntity extends NatsuMobEntity {

    private static final float TOTAL_HP = 900.0F;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.gear_forge.natsu_boss"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)
            .setDarkenScreen(false).setCreateWorldFog(false);

    private int phase = 0;
    private int abilityCooldown = 120;
    private int phaseTransition = 0;

    public NatsuBossEntity(EntityType<? extends NatsuMobEntity> type, Level level) {
        super(type, level);
        equipForPhase(0);
    }

    /** 按阶段换装：副手恒为糖霜盾，主手与护甲随阶段升级。 */
    private void equipForPhase(int newPhase) {
        // 护甲：原皮铁甲 → 乐队金甲 → 坦克钻甲（YSM 模型需要装备才显示护甲/鞋袜层）
        ItemStack chest, legs, feet, helm, weapon;
        switch (newPhase) {
            case 1 -> {
                chest = new ItemStack(Items.GOLDEN_CHESTPLATE);
                legs = new ItemStack(Items.GOLDEN_LEGGINGS);
                feet = new ItemStack(Items.GOLDEN_BOOTS);
                helm = new ItemStack(Items.GOLDEN_HELMET);
                weapon = new ItemStack(ModItems.SAKURA_SWORD.get());
            }
            case 2 -> {
                chest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                legs = new ItemStack(Items.DIAMOND_LEGGINGS);
                feet = new ItemStack(Items.DIAMOND_BOOTS);
                helm = new ItemStack(Items.DIAMOND_HELMET);
                weapon = new ItemStack(ModItems.DIAMOND_CURVED_BLADE.get());
            }
            default -> {
                chest = new ItemStack(Items.IRON_CHESTPLATE);
                legs = new ItemStack(Items.IRON_LEGGINGS);
                feet = new ItemStack(Items.IRON_BOOTS);
                helm = new ItemStack(Items.IRON_HELMET);
                weapon = new ItemStack(ModItems.SAKURA_SWORD.get());
            }
        }
        this.setItemSlot(EquipmentSlot.HEAD, helm);
        this.setItemSlot(EquipmentSlot.CHEST, chest);
        this.setItemSlot(EquipmentSlot.LEGS, legs);
        this.setItemSlot(EquipmentSlot.FEET, feet);
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        // 每个阶段都必须拿着盾
        this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.NATSU_SHIELD.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
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
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) return;
        if (phaseTransition > 0) phaseTransition--;

        // 阶段切换：按总血量百分比（66% / 33%），不回血
        if (phase == 0 && this.getHealth() <= TOTAL_HP * 0.66F) enterPhase(1);
        else if (phase == 1 && this.getHealth() <= TOTAL_HP * 0.33F) enterPhase(2);

        // 阶段能力：原皮牛奶自愈 / 乐队节奏攻速 / 坦克重装甲
        if (abilityCooldown <= 0 && phaseTransition <= 0) useAbility();
        if (abilityCooldown > 0) abilityCooldown--;

        bossEvent.setProgress(this.getHealth() / TOTAL_HP);
        if (this.level() instanceof net.minecraft.server.level.ServerLevel sl) {
            for (ServerPlayer p : sl.getServer().getPlayerList().getPlayers()) {
                if (p.distanceTo(this) < 40) bossEvent.addPlayer(p);
                else bossEvent.removePlayer(p);
            }
        }
    }

    /** 角色技能映射：牛奶回血解负面 → 甜派加攻 → 重装甲反伤。 */
    private void useAbility() {
        switch (phase) {
            case 0 -> {
                // 「为了这种时候准备的」：喝牛奶回复并解除负面效果
                this.heal(18.0F);
                if (this.hasEffect(MobEffects.POISON)) this.removeEffect(MobEffects.POISON);
                if (this.hasEffect(MobEffects.WITHER)) this.removeEffect(MobEffects.WITHER);
                this.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                spawnParticles(ParticleTypes.HEART, 16);
                abilityCooldown = 200;
            }
            case 1 -> {
                // 「吃了这个打起精神来」：抛甜派，自身攻击力/速度提升
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 240, 2));
                this.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 240, 1));
                spawnParticles(ParticleTypes.HAPPY_VILLAGER, 20);
                abilityCooldown = 160;
            }
            default -> {
                // 最终手段 Beyond The Lumination：重装甲 + 力量 + 抗性
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 200, 2));
                this.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 3));
                this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
                spawnParticles(ParticleTypes.SMOKE, 24);
                abilityCooldown = 120;
            }
        }
    }

    private void enterPhase(int newPhase) {
        phase = newPhase;
        phaseTransition = 40;
        equipForPhase(newPhase);
        spawnParticles(ParticleTypes.CHERRY_LEAVES, 40);

        // 血条颜色随阶段：粉 → 黄 → 紫
        bossEvent.setColor(switch (newPhase) {
            case 1 -> BossEvent.BossBarColor.YELLOW;
            default -> BossEvent.BossBarColor.PURPLE;
        });

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
        // 夏之糖霜盾每次都掉
        this.spawnAtLocation(new ItemStack(ModItems.NATSU_SHIELD.get()));

        // 甜点部的契约仅限一次掉落：只给未领取过的最近击杀玩家。
        net.minecraft.world.entity.LivingEntity killer = source.getEntity() instanceof net.minecraft.world.entity.LivingEntity le
                ? le : null;
        if (killer instanceof net.minecraft.server.level.ServerPlayer sp
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
