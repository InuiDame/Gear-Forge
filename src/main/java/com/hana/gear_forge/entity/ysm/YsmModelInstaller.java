package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 把内置的柚鸟夏 .ysm 模型【解密】后安装到 YSM 的 custom 目录（若不存在）。
 *
 * <p>模型以 AES-128-CBC 加密的 {@code natsu.ysm.enc} 形式打包在 jar 中，
 * 明文 .ysm 不入库、不进 jar，防止他人直接从 mod 包中提取模型文件。
 * 本类在 common setup 阶段读取密文、运行时解密并写出明文供 YSM 加载。
 *
 * <p>YSM 在启动时扫描 {@code config/yes_steve_model/custom/*.ysm}。首次进游戏即可用；
 * 重启后 YSM 会自动加载它。模型约 540KB，只在缺失时安装，不覆盖玩家已有同名文件。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class YsmModelInstaller {

    /** 安装后的明文文件名，也是 YSM 的 modelId。 */
    public static final String MODEL_FILE = "natsu.ysm";

    /** jar 内的加密资源路径。 */
    private static final String ENCRYPTED_RESOURCE = "/assets/gear_forge/ysm/natsu.ysm.enc";

    /** 安装标记：记录已安装模型的版本，用于替换旧版明文/旧版本模型。 */
    private static final String MARKER_FILE = "natsu.ysm.installed";
    private static final String MARKER_VERSION = "aes-v1";

    // AES-128-CBC 密钥与 IV：16 字节，必须与加密工具一致
    private static final byte[] KEY = "GfrgYSMKey2026!!".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV = "NatsuYSMiv000001".getBytes(StandardCharsets.UTF_8);

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
            Path marker = dir.resolve(MARKER_FILE);

            // 已安装且版本匹配则不重复安装；版本不匹配（如从旧明文升级）则覆盖
            if (Files.exists(target) && Files.exists(marker)
                    && MARKER_VERSION.equals(Files.readString(marker).trim())) {
                return;
            }

            try (InputStream in = YsmModelInstaller.class.getResourceAsStream(ENCRYPTED_RESOURCE)) {
                if (in == null) {
                    GearForgeMod.LOGGER.warn("Encrypted YSM model resource not found: {}", ENCRYPTED_RESOURCE);
                    return;
                }
                byte[] encrypted = in.readAllBytes();
                byte[] plain = decrypt(encrypted);
                Files.write(target, plain,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                Files.writeString(marker, MARKER_VERSION,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                GearForgeMod.LOGGER.info("Installed decrypted YSM model to {} (restart game to load)", target);
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install YSM model: {}", e.toString());
        }
    }

    /** AES-128-CBC 解密模型数据。 */
    private static byte[] decrypt(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        return cipher.doFinal(encrypted);
    }
}
