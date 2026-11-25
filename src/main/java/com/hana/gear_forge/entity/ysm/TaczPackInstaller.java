package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

/**
 * 释放 mod 自带的超越沉思 TaCZ 枪包（仅图标小改）到游戏目录 {@code tacz/}。
 *
 * <p>枪包基于原版超越沉思（namespace {@code bluearchive}，GunId {@code bluearchive:natsu}），
 * 只做了两处图标小改：物品栏图标去掉青色十字光环、弹药 HUD 头像换成枪械剪影；
 * 3D 模型与动画音效完全保持原版，保证渲染正常。不加密（与 YSM 模型不同）。
 *
 * <p>必须在 mod 构造阶段调用（GearForgeMod 构造函数开头），早于 TaCZ 作为
 * RepositorySource 在资源包发现阶段扫描 {@code tacz/} 目录，保证当次启动即生效。
 */
public final class TaczPackInstaller {

    private static final String BUNDLED_RESOURCE = "/assets/gear_forge/tacz/gearforge_natsu.zip";
    private static final String TARGET_NAME = "gearforge_natsu.zip";

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
