package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.NatsuCompanionNpc;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
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
        if (!(event.getTarget() instanceof NatsuCompanionNpc companion)) return;
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
            // 右键：打开队友的背包（9行玩家背包，可给她放装备）
            MenuProvider provider = new SimpleMenuProvider(
                    (containerId, playerInv, p) ->
                            new ChestMenu(MenuType.GENERIC_9x2, containerId, playerInv,
                                    companion.getInventory(), 2),
                    Component.translatable("container.gear_forge.natsu_companion"));
            player.openMenu(provider);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }
}
