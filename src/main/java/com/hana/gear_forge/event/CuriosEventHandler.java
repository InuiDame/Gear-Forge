package com.hana.gear_forge.event;

import com.hana.gear_forge.Curios.CursedAnvilItem;
import com.hana.gear_forge.Provider.CuriosProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import top.theillusivec4.curios.api.CuriosApi;

@EventBusSubscriber(modid = "gear_forge")
public class CuriosEventHandler {

    // On the MOD event bus
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(
                // Tell generator to run only when server data are generating
                event.includeServer(),
                new CuriosProvider(
                        "gear_forge",
                        event.getGenerator().getPackOutput(),
                        event.getExistingFileHelper(),
                        event.getLookupProvider()
                )
        );
    }
    // 经验 +15%
    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (hasCursedAnvil(event.getEntity())) {
            event.setAmount((int) (event.getAmount() * 1.15));
        }
    }

    @SuppressWarnings({"deprecation", "removal"})
    private static boolean hasCursedAnvil(Player player) {
        return !CuriosApi.getCuriosHelper()
                .findCurios(player, stack -> stack.getItem() instanceof CursedAnvilItem)
                .isEmpty();
    }

    @SubscribeEvent
    public static void onEnchantLevelSet(EnchantmentLevelSetEvent event) {
        // 附魔台事件有 BlockPos 和 Level
        // 寻找 10 格内最近的玩家
        Player player = event.getLevel().getNearestPlayer(
                event.getPos().getX(), event.getPos().getY(), event.getPos().getZ(),
                10.0, false);

        if (player != null && hasCursedAnvil(player)) {
            // 逻辑：比如佩戴饰品时，附魔等级需求全部降低 5 级
            int newLevel = Math.max(1, event.getEnchantLevel() - 5);
            event.setEnchantLevel(newLevel);
        }
    }




}
