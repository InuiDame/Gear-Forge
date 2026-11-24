package com.hana.gear_forge.entity.ysm;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * 队友形态的小夏（YSM 假玩家）。击败 Boss 后用契约生成。
 *
 * <p>三态：0=跟随、1=范围游荡、2=站立不动。交互由 {@link com.hana.gear_forge.event.YsmNpcEvents}
 * 统一处理（右键打开装备背包、Shift+右键切换状态）。
 */
public class NatsuCompanionNpc extends YsmNpc {

    private int mode = 0;              // 0=跟随 1=游荡 2=站立
    private String ownerUuid = "";
    private int attackCooldown = 0;
    private Player ownerCache;

    public NatsuCompanionNpc(MinecraftServer server, ServerLevel level,
                             double x, double y, double z, Player owner) {
        super(server, level, "小夏", new Vec3(x, y, z), NATSU_MODEL_ID);
        if (owner != null) this.ownerUuid = owner.getStringUUID();
        this.setHealth(200);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isDeadOrDying()) return;

        if (ownerCache == null || !ownerCache.isAlive()) {
            ownerCache = findOwner();
        }

        if (mode != 2 && attackCooldown <= 0) {
            // 攻击主人附近的敌对生物
            var enemies = this.level().getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class,
                    this.getBoundingBox().inflate(8));
            for (var e : enemies) {
                if (e.isAlive() && this.distanceTo(e) < 2.5) {
                    e.hurt(this.damageSources().mobAttack(this), 12.0F);
                    attackCooldown = 20;
                    break;
                }
            }
        }
        if (attackCooldown > 0) attackCooldown--;

        Vec3 target = null;
        double speed = 0.10;
        if (mode == 0 && ownerCache != null && this.distanceTo(ownerCache) > 5.0) {
            target = ownerCache.position();
        } else if (mode == 1) {
            // 范围游荡：周期性随机选点（以当前位置为中心 12 格）
            if (this.tickCount % 100 == 0 && this.getRandom().nextFloat() < 0.5F) {
                double a = this.getRandom().nextDouble() * Math.PI * 2;
                double r = 6 + this.getRandom().nextDouble() * 6;
                wanderTarget = new Vec3(this.getX() + Math.cos(a) * r,
                        this.getY(), this.getZ() + Math.sin(a) * r);
            }
            if (wanderTarget != null && this.distanceToSqr(wanderTarget) < 2.0) wanderTarget = null;
            target = wanderTarget;
            speed = 0.08;
        }

        if (target != null) {
            Vec3 dir = target.subtract(this.position()).normalize().scale(speed);
            this.setDeltaMovement(this.getDeltaMovement().add(dir.x * 0.15, 0, dir.z * 0.15)
                    .multiply(0.85, 1.0, 0.85));
            this.setYRot((float) (Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI));
        } else if (mode == 2) {
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
        }
    }

    private Vec3 wanderTarget;

    public int getMode() { return mode; }

    public void cycleMode() {
        mode = (mode + 1) % 3;
        if (mode == 2) this.setDeltaMovement(0, 0, 0);
    }

    public void setOwner(Player player) {
        this.ownerUuid = player.getStringUUID();
        this.ownerCache = player;
    }

    private Player findOwner() {
        if (ownerUuid.isEmpty()) return null;
        try {
            return this.level().getPlayerByUUID(java.util.UUID.fromString(ownerUuid));
        } catch (Exception e) {
            return null;
        }
    }

    public void despawn() {
        this.level().getServer().getPlayerList().remove(this);
    }
}
