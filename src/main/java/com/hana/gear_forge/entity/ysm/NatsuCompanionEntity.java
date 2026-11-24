package com.hana.gear_forge.entity.ysm;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
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
 * 交互（右键开装备背包 / Shift+右键切换状态）在本类 mobInteract 处理。
 */
public class NatsuCompanionEntity extends TamableAnimal implements YsmModelProvider {

    private static final int CONTAINER_SIZE = 18; // 9x2
    private final net.minecraft.world.Container inventory =
            new net.minecraft.world.SimpleContainer(CONTAINER_SIZE);

    private int mode = 0; // 0=跟随 1=游荡 2=站立

    public NatsuCompanionEntity(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
        this.setTame(true, true);
        // 队友默认不穿装备（鞋袜层由 YSM 装备层渲染，玩家可通过装备栏自行穿戴）。
        // Boss 才默认穿一套（见 NatsuBossEntity.equipDefaultGear）。
        // 装备不掉落，避免玩家通过装备栏给队友穿的装备在队友死亡后刷出。
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            this.setDropChance(slot, 0.0F);
        }
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

    /**
     * 右键交互：普通右键打开装备+背包菜单；Shift+右键切换跟随/游荡/站立。
     *
     * <p>放在实体方法里而不是 PlayerInteractEvent 事件里：1.21.1 的 TamableAnimal.mobInteract
     * 会先处理已驯服实体的右键（切换 sit）并返回 SUCCESS 消费交互，导致后续 EntityInteract
     * 事件根本不走我们的处理器。原版 Mob.interact → mobInteract 这条路径最可靠。
     */
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
