package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 把内置的柚鸟夏 .ysm 模型安装到 YSM 的 custom 目录（若不存在）。
 *
 * <p>YSM 在启动时扫描 {@code config/yes_steve_model/custom/*.ysm}。本类在本 mod 的
 * common setup 阶段复制模型，因此首次进游戏即可用；重启后 YSM 会自动加载它。
 * 注意：模型文件较大（约 540KB），只在缺失时复制，不覆盖玩家已有的同名文件。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class YsmModelInstaller {

    /** 安装后的文件名，也是 YSM 的 modelId。 */
    public static final String MODEL_FILE = "natsu.ysm";

    private YsmModelInstaller() {}

    @SubscribeEvent
    public static void onSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(YsmModelInstaller::install);
    }

    private static void install() {
        try {
            Path dir = Path.of("config", "yes_steve_model", "custom");
            Files.createDirectories(dir);
            Path target = dir.resolve(MODEL_FILE);
            if (Files.exists(target)) return;   // 不覆盖

            String resource = "/assets/gear_forge/ysm/natsu.ysm";
            try (InputStream in = YsmModelInstaller.class.getResourceAsStream(resource)) {
                if (in == null) {
                    GearForgeMod.LOGGER.warn("YSM model resource not found: {}", resource);
                    return;
                }
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                GearForgeMod.LOGGER.info("Installed YSM model to {} (restart game to load)", target);
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install YSM model: {}", e.getMessage());
        }
    }
}
