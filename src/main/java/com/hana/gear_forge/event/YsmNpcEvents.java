package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * YSM 假玩家 NPC 的交互与生命周期事件。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class YsmNpcEvents {

    private YsmNpcEvents() {}

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NatsuCompanionEntity companion)) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        if (player.isShiftKeyDown()) {
            // Shift+右键：切换跟随/游荡/站立
            companion.cycleMode();
            String key = "message.gear_forge.natsu_mode_" + companion.getMode();
            player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else {
            // 右键：打开队友的装备 + 背包菜单
            final int companionId = companion.getId();
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
                    return new com.hana.gear_forge.menu.CompanionMenu(containerId, playerInv, companion);
                }

                @Override
                public void writeClientSideData(
                        net.minecraft.world.inventory.AbstractContainerMenu menu,
                        net.minecraft.network.RegistryFriendlyByteBuf buf) {
                    // 客户端构造 CompanionMenu 时据此从 level 找队友实体
                    buf.writeInt(companionId);
                }
            };
            player.openMenu(provider);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
