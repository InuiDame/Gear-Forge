package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.event.NatsuCompanionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 队友形态的小夏（真 Mob 实体，可驯服类型以支持跟随 AI）。
 *
 * <p>三态：0=跟随 1=范围游荡 2=站立。三态的 AI goal 严格互斥：
 * 只有跟随态才启用 FollowOwnerGoal，游荡态用 RandomStrollGoal 原地晃悠，
 * 站立态全部移动 goal 停用。
 *
 * <p>其他能力：跟随态离主人过远自动瞬移回来；手持武器攻击苦力怕时会盯着它，
 * 在其点燃即将爆炸时举盾减伤；死亡后由 NatsuCompanionEvents 定时在主人周围复活。
 */
public class NatsuCompanionEntity extends TamableAnimal implements YsmModelProvider {

    public static final int CONTAINER_SIZE = 18; // 9x2
    /** 死亡到复活的间隔（tick，20/s）。 */
    public static final int RESPAWN_DELAY = 30 * 20;

    private final net.minecraft.world.Container inventory =
            new net.minecraft.world.SimpleContainer(CONTAINER_SIZE);

    private int mode = 0; // 0=跟随 1=游荡 2=站立
    private int shieldRaiseTicks = 0;   // 举盾剩余 tick（>0 表示正在格挡）
    private int creeperWatchTicks = 0;  // 盯住苦力怕的剩余 tick
    private UUID watchingCreeper = null;

    private FollowOwnerGoal followGoal;
    private RandomStrollGoal strollGoal;

    public NatsuCompanionEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setTame(true, true);
        // 队友默认副手持盾（角色设定：重装甲坦克），主手空手由玩家通过装备栏配置。
        this.setItemSlot(EquipmentSlot.OFFHAND,
                new ItemStack(com.hana.gear_forge.item.ModItems.NATSU_SHIELD.get()));
        for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 14.0)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.15, true));
        // 三态互斥：跟随 goal 只在 mode==0 生效；游荡 goal 只在 mode==1 生效
        this.followGoal = new FollowOwnerGoal(this, 1.15, 6.0F, 2.0F) {
            @Override
            public boolean canUse() {
                return mode == 0 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return mode == 0 && super.canContinueToUse();
            }
        };
        this.strollGoal = new RandomStrollGoal(this, 0.8, 40) {
            @Override
            public boolean canUse() {
                return mode == 1 && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return mode == 1 && super.canContinueToUse();
            }
        };
        this.goalSelector.addGoal(2, this.followGoal);
        this.goalSelector.addGoal(3, this.strollGoal);
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) return;

        // 站立态：停止当前寻路
        if (mode == 2) this.getNavigation().stop();

        // 跟随态：离主人过远瞬移回来（>32 格），避免寻路跟不上
        if (mode == 0 && this.tickCount % 20 == 0 && this.getOwnerUUID() != null) {
            LivingEntity ownerEntity = this.getOwner();
            if (ownerEntity instanceof Player owner && owner.distanceTo(this) > 32.0F) {
                teleportNearOwner(owner);
            }
        }

        // 苦力怕举盾：若正在盯着一只点燃的苦力怕且距离近，则举盾
        if (shieldRaiseTicks > 0) shieldRaiseTicks--;
        if (creeperWatchTicks > 0) creeperWatchTicks--;
        if (watchingCreeper != null && this.level() instanceof ServerLevel sl) {
            net.minecraft.world.entity.Entity e = sl.getEntity(watchingCreeper);
            ItemStack main = this.getItemBySlot(EquipmentSlot.MAINHAND);
            boolean meleeHeld = !main.isEmpty()
                    && main.getItem() instanceof net.minecraft.world.item.SwordItem;
            if (meleeHeld && e instanceof Creeper creeper && creeper.isAlive() && creeper.isIgnited()
                    && creeper.distanceTo(this) < 5.0F) {
                shieldRaiseTicks = 20; // 持续刷新举盾（手持近战 + 苦力怕已点燃）
            }
        }
    }

    private void teleportNearOwner(Player owner) {
        // 在主人周围找一个安全落点（最多尝试 12 次）
        for (int i = 0; i < 12; i++) {
            double a = this.random.nextDouble() * Math.PI * 2;
            double r = 2.0 + this.random.nextDouble() * 2.0;
            double px = owner.getX() + Math.cos(a) * r;
            double py = owner.getY();
            double pz = owner.getZ() + Math.sin(a) * r;
            net.minecraft.core.BlockPos bp = net.minecraft.core.BlockPos.containing(px, py, pz);
            if (this.level().getBlockState(bp).isAir()
                    && this.level().getBlockState(bp.below()).isSolid()) {
                this.teleportTo(px, py, pz);
                this.getNavigation().stop();
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(ParticleTypes.PORTAL, px, py + 1, pz,
                            16, 0.5, 1.0, 0.5, 0.0);
                }
                return;
            }
        }
    }

    /** 攻击苦力怕时记录目标，供爆炸前举盾判定。 */
    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof Creeper creeper) {
            watchingCreeper = creeper.getUUID();
            creeperWatchTicks = 100;
        }
        return hit;
    }

    /** 是否正在举盾格挡（供伤害事件减伤与渲染同步）。仅点燃的苦力怕近身时举盾。 */
    public boolean isShieldRaised() {
        return shieldRaiseTicks > 0;
    }

    /** 爆炸伤害减免（由 NatsuCompanionEvents 在 LivingDamageEvent 中调用）。 */
    public float reduceExplosionDamage(float amount) {
        if (isShieldRaised() && hasShieldInOffhand()) {
            if (this.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 1.2, this.getZ(),
                        10, 0.4, 0.4, 0.4, 0.02);
            }
            return amount * 0.25F; // 举盾减免 75%
        }
        return amount;
    }

    private boolean hasShieldInOffhand() {
        ItemStack s = this.getItemBySlot(EquipmentSlot.OFFHAND);
        return s.is(com.hana.gear_forge.item.ModItems.NATSU_SHIELD.get()) || s.is(Items.SHIELD);
    }

    @Override
    public void die(DamageSource source) {
        // 死亡前存档，NatsuCompanionEvents 在 LivingDeathEvent 里读取
        if (this.level() instanceof ServerLevel sl) {
            NatsuCompanionEvents.saveForRespawn(this);
        }
        super.die(source);
    }

    public void setOwner(Player player) {
        this.setOwnerUUID(player.getUUID());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (player.isShiftKeyDown()) {
            cycleMode();
            String key = "message.gear_forge.natsu_mode_" + getMode();
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.SUCCESS;
        }

        final int companionId = getId();
        final Component title = Component.translatable("container.gear_forge.natsu_companion");
        MenuProvider provider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return title;
            }

            @Override
            public net.minecraft.world.inventory.AbstractContainerMenu createMenu(
                    int containerId, net.minecraft.world.entity.player.Inventory playerInv,
                    net.minecraft.world.entity.player.Player p) {
                return new com.hana.gear_forge.menu.CompanionMenu(
                        containerId, playerInv, NatsuCompanionEntity.this);
            }

            @Override
            public void writeClientSideData(
                    net.minecraft.world.inventory.AbstractContainerMenu menu,
                    net.minecraft.network.RegistryFriendlyByteBuf buf) {
                buf.writeInt(companionId);
            }
        };
        serverPlayer.openMenu(provider);
        return InteractionResult.SUCCESS;
    }

    public net.minecraft.world.Container getInventory() {
        return inventory;
    }

    public int getMode() {
        return mode;
    }

    /** 由复活档案恢复模式。 */
    public void setMode(int mode) {
        this.mode = Math.max(0, Math.min(2, mode));
    }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        if (mode == 2) this.getNavigation().stop();
    }

    @Override
    public String getModelId() {
        return NATSU_MODEL_ID;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public NatsuCompanionEntity getBreedOffspring(ServerLevel level,
                                                  net.minecraft.world.entity.AgeableMob other) {
        return null;
    }

    @Override
    public boolean isOrderedToSit() {
        return mode == 2;
    }
}
