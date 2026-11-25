package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

/**
 * 把「超越沉思」的 TaCZ 枪包数据释放到游戏目录的 {@code tacz/} 下，供 TaCZ 扫描加载。
 *
 * <p>枪包不需要加密（与 YSM 柚鸟夏模型不同）：柚鸟夏模型是作者要求保密的，
 * 而枪只是角色设定里的一把武器，其数据本就来自公开的蔚蓝档案枪包。
 * 这里从 jar 内资源 {@code /assets/gear_forge/tacz/natsu_gunpack.zip} 读出，
 * 每次启动比对内容，不同就覆盖到 {@code tacz/gearforge_natsu.zip}。
 *
 * <p>依赖：玩家必须安装 TaCZ（mods.toml 已声明 required）并启用枪包加载；
 * 本 mod 只提供超越沉思这一把枪的数据，其余枪包由玩家自行安装。
 */
public final class TaczPackInstaller {

    private static final String RESOURCE = "/assets/gear_forge/tacz/natsu_gunpack.zip";
    /** 独立文件名，避免覆盖玩家自己的蓝包。 */
    private static final String TARGET_NAME = "gearforge_natsu.zip";

    private TaczPackInstaller() {}

    /**
     * 必须在 mod 构造阶段调用（早于 TaCZ 的资源包扫描），否则当次启动扫不到。
     * 由 {@link GearForgeMod} 构造函数直接调用，不走事件总线。
     */
    public static void install() {
        try (InputStream in = TaczPackInstaller.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                GearForgeMod.LOGGER.warn("TaCZ gunpack resource missing: {}", RESOURCE);
                return;
            }
            byte[] data = in.readAllBytes();

            Path dir = Path.of("tacz");
            Files.createDirectories(dir);
            Path target = dir.resolve(TARGET_NAME);

            // 内容相同则跳过，避免每次启动都重写
            if (Files.exists(target) && MessageDigest.isEqual(Files.readAllBytes(target), data)) {
                GearForgeMod.LOGGER.info("TaCZ gunpack already up to date: {}", target);
                return;
            }

            Files.write(target, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            GearForgeMod.LOGGER.info("Installed TaCZ gunpack (Beyond The Lumination): {}", target);
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install TaCZ gunpack: {}", e.toString());
        }
    }
}
