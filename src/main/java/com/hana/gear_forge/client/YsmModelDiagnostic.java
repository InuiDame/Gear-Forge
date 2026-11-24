package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import java.util.Map;

/**
 * 诊断用：进入世界后把 YSM 客户端模型注册表的所有 key 打到日志，
 * 用来确定内置模型到底以哪个 key 注册（"natsu" 还是内部中文名等）。只打印一次。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class YsmModelDiagnostic {

    private static boolean done = false;
    private static int ticks = 0;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (done) return;
        // 只在本地玩家、进入世界后等 ~40 tick 再 dump，确保 YSM 完成模型扫描
        if (!event.getEntity().level().isClientSide) return;
        if (!event.getEntity().isLocalPlayer()) return;
        if (++ticks < 40) return;
        done = true;
        try {
            Map<String, ?> registry = com.elfmcys.yesstevemodel.O0Ooo000O0ooO00Oooo00oOO
                    .oOo0OO0O0o000OO0O000oo0o();
            GearForgeMod.LOGGER.info("=== YSM CLIENT MODEL REGISTRY ({} entries) ===", registry.size());
            for (String key : registry.keySet()) {
                GearForgeMod.LOGGER.info("YSM MODEL KEY: '{}'", key);
            }
        } catch (Throwable t) {
            GearForgeMod.LOGGER.warn("Failed to dump YSM registry: {}", t.toString());
        }
    }
}
