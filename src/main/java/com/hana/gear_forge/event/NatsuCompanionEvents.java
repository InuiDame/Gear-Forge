package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ModEntities;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 队友小夏的跨生命周期能力：
 * <ul>
 *   <li>死亡时保存装备/背包/模式/主人，{@link NatsuCompanionEntity#RESPAWN_DELAY} 后在主人周围复活；</li>
 *   <li>举盾状态下减免爆炸伤害（苦力怕）；</li>
 *   <li>契约仅限一次掉落：已拥有队友的玩家不再重复获得契约。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class NatsuCompanionEvents {

    /** 队友死亡档案。key=主人 UUID，一个主人同时只有一个待复活的队友。 */
    private static final Map<UUID, RespawnRecord> PENDING = new HashMap<>();

    private NatsuCompanionEvents() {}

    /** 死亡存档（由 NatsuCompanionEntity.die 调用）。 */
    public static void saveForRespawn(NatsuCompanionEntity companion) {
        UUID ownerId = companion.getOwnerUUID();
        if (ownerId == null) return;
        RespawnRecord rec = new RespawnRecord();
        rec.respawnAt = companion.level().getGameTime() + NatsuCompanionEntity.RESPAWN_DELAY;
        rec.mode = companion.getMode();
        rec.ownerId = ownerId;
        for (int i = 0; i < companion.getInventory().getContainerSize(); i++) {
            rec.inventory.set(i, companion.getInventory().getItem(i).copy());
        }
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            rec.equipment.put(slot, companion.getItemBySlot(slot).copy());
        }
        PENDING.put(ownerId, rec);
    }

    /** 爆炸伤害减伤：仅对举盾的队友生效。 */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof NatsuCompanionEntity companion)) return;
        if (!event.getSource().is(DamageTypes.EXPLOSION)
                && !event.getSource().is(DamageTypes.PLAYER_EXPLOSION)) return;
        event.setNewDamage(companion.reduceExplosionDamage(event.getNewDamage()));
    }

    /** 每个服务端 tick 检查复活档案。 */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING.isEmpty()) return;
        ServerLevel level = event.getServer().overworld();
        long now = level.getGameTime();
        List<UUID> done = new ArrayList<>();

        for (Map.Entry<UUID, RespawnRecord> entry : PENDING.entrySet()) {
            RespawnRecord rec = entry.getValue();
            if (now < rec.respawnAt) continue;

            ServerPlayer owner = event.getServer().getPlayerList().getPlayer(rec.ownerId);
            if (owner == null) continue; // 主人不在线，等上线再复活

            NatsuCompanionEntity companion = ModEntities.NATSU_COMPANION.get().spawn(
                    owner.serverLevel(),
                    entity -> {
                        entity.setOwner(owner);
                        entity.setMode(rec.mode);
                        for (int i = 0; i < entity.getInventory().getContainerSize()
                                && i < rec.inventory.size(); i++) {
                            entity.getInventory().setItem(i, rec.inventory.get(i).copy());
                        }
                        for (Map.Entry<EquipmentSlot, ItemStack> e : rec.equipment.entrySet()) {
                            entity.setItemSlot(e.getKey(), e.getValue().copy());
                        }
                        for (EquipmentSlot slot : EquipmentSlot.values()) entity.setDropChance(slot, 0.0F);
                    },
                    findSafePos(owner), MobSpawnType.MOB_SUMMONED, true, false);

            if (companion != null) {
                owner.sendSystemMessage(Component.translatable("message.gear_forge.natsu_respawn")
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                done.add(entry.getKey());
            }
        }
        for (UUID id : done) PENDING.remove(id);
    }

    private static BlockPos findSafePos(ServerPlayer owner) {
        for (int i = 0; i < 12; i++) {
            double a = owner.getRandom().nextDouble() * Math.PI * 2;
            double r = 2.0 + owner.getRandom().nextDouble() * 2.0;
            BlockPos p = BlockPos.containing(
                    owner.getX() + Math.cos(a) * r, owner.getY(), owner.getZ() + Math.sin(a) * r);
            if (owner.serverLevel().getBlockState(p).isAir()
                    && owner.serverLevel().getBlockState(p.below()).isSolid()) return p;
        }
        return owner.blockPosition().above();
    }

    private static final class RespawnRecord {
        private long respawnAt;
        private int mode;
        private UUID ownerId;
        private final NonNullList<ItemStack> inventory =
                NonNullList.withSize(NatsuCompanionEntity.CONTAINER_SIZE, ItemStack.EMPTY);
        private final Map<EquipmentSlot, ItemStack> equipment = new HashMap<>();
    }
}
