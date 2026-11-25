package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

/**
 * 释放 mod 自带的原创 TaCZ 枪包到游戏目录 {@code tacz/}。
 *
 * <p>枪包是本项目用 Blockbench 原创绘制的「超越甜梦」冲锋枪
 * （namespace {@code gear_forge}，GunId {@code gear_forge:natsu_smg}），
 * 只含这一把枪，模型/贴图/数据全部自有，动画与音效复用 TaCZ 内置资源，
 * 因此不依赖玩家另外安装蔚蓝档案枪包，也不加密（与 YSM 模型不同）。
 *
 * <p>必须在 mod 构造阶段调用（GearForgeMod 构造函数开头），早于 TaCZ 作为
 * RepositorySource 在资源包发现阶段扫描 {@code tacz/} 目录，保证当次启动即生效。
 */
public final class TaczPackInstaller {

    private static final String BUNDLED_RESOURCE = "/assets/gear_forge/tacz/gearforge_natsu_smg.zip";
    private static final String TARGET_NAME = "gearforge_natsu_smg.zip";

    private TaczPackInstaller() {}

    /** 在 mod 构造阶段释放原创枪包到 {@code tacz/}。 */
    public static void install() {
        try {
            Path dir = Path.of("tacz");
            Files.createDirectories(dir);
            try (InputStream in = TaczPackInstaller.class.getResourceAsStream(BUNDLED_RESOURCE)) {
                if (in == null) {
                    GearForgeMod.LOGGER.warn("Bundled TaCZ gunpack resource missing: {}", BUNDLED_RESOURCE);
                    return;
                }
                byte[] data = in.readAllBytes();
                Path target = dir.resolve(TARGET_NAME);
                if (Files.exists(target) && MessageDigest.isEqual(Files.readAllBytes(target), data)) {
                    return;
                }
                Files.write(target, data, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                GearForgeMod.LOGGER.info("Installed original TaCZ gunpack (Beyond Sweetness): {}", target);
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install TaCZ gunpack: {}", e.toString());
        }
    }
}
