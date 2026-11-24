package com.hana.gear_forge.entity.ysm;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 队友形态的小夏（真 Mob 实体，可驯服类型以支持原版跟随 AI）。
 *
 * <p>击败 Boss 后用契约生成。三态：0=跟随 1=游荡 2=站立。
 * 跟随用原版 FollowOwnerGoal（寻路绕障碍、不穿墙）。
 * 交互（右键开背包 / Shift+右键切换状态）在 YsmNpcEvents 处理。
 */
public class NatsuCompanionEntity extends TamableAnimal implements YsmModelProvider {

    private static final int CONTAINER_SIZE = 18; // 9x2
    private final net.minecraft.world.Container inventory =
            new net.minecraft.world.SimpleContainer(CONTAINER_SIZE);

    private int mode = 0; // 0=跟随 1=游荡 2=站立

    public NatsuCompanionEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setTame(true, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ATTACK_DAMAGE, 12.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true));
    }

    @Override
    public void tick() {
        super.tick();
        if (mode == 2) this.getNavigation().stop();
    }

    public void setOwner(Player player) {
        this.setOwnerUUID(player.getUUID());
    }

    public net.minecraft.world.Container getInventory() {
        return inventory;
    }

    public int getMode() {
        return mode;
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
    public boolean isFood(net.minecraft.world.item.ItemStack stack) {
        return false;
    }

    @Override
    public NatsuCompanionEntity getBreedOffspring(net.minecraft.server.level.ServerLevel level,
                                                  net.minecraft.world.entity.AgeableMob other) {
        return null; // 不可繁殖
    }

    /** 站立模式禁用跟随 goal 的移动。 */
    @Override
    public boolean isOrderedToSit() {
        return mode == 2;
    }
}
