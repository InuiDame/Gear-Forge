package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;

/**
 * 释放 mod 自带的超越沉思 TaCZ 枪包到游戏目录 {@code tacz/}。
 *
 * <p>枪包基于原版超越沉思，但使用独立 namespace {@code gearforge}（GunId
 * {@code gearforge:natsu}），不与玩家安装的蔚蓝档案枪包（bluearchive）冲突。
 * 改动：物品栏图标去青色十字光环、弹药 HUD 换枪械剪影、模型追加乌兹式圆筒枪口
 * 与钢丝折叠枪托；动画/音效保持原版。不加密（与 YSM 模型不同）。
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
                GearForgeMod.LOGGER.info("Installed TaCZ gunpack (Beyond Lumination, namespace gearforge): {}", target);
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install TaCZ gunpack: {}", e.toString());
        }
    }
}
