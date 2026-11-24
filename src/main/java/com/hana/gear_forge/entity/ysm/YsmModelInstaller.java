package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.GearForgeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * 柚鸟夏模型的加密加载器（作者要求：玩家不得取得模型源文件）。
 *
 * <p>模型以 AES-128-CBC 加密的 {@code natsu.ysm.enc} 打包在 jar 中。运行流程：
 * <ol>
 *   <li>common setup：解密成临时明文写到 YSM 的 custom 目录，供 YSM 在
 *       ServerAboutToStartEvent 扫描加载；</li>
 *   <li>ServerStartedEvent：YSM 已把模型编译进内存（native 对象，含编译后的顶点数据），
 *       此时立即删除磁盘上的明文 .ysm，只保留密文在 jar 中。</li>
 * </ol>
 * 这样正常游玩时磁盘上没有可取走的模型源文件；YSM 渲染用的是内存对象，不依赖该文件。
 * 明文存在窗口仅限启动到 ServerStartedEvent 之间（数秒）。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class YsmModelInstaller {

    /** YSM 加载用的临时明文文件名（启动后删除）。 */
    public static final String MODEL_FILE = "natsu.ysm";

    private static final String ENCRYPTED_RESOURCE = "/assets/gear_forge/ysm/natsu.ysm.enc";

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
            Path dir = modelDir();
            Files.createDirectories(dir);
            Path target = dir.resolve(MODEL_FILE);
            try (InputStream in = YsmModelInstaller.class.getResourceAsStream(ENCRYPTED_RESOURCE)) {
                if (in == null) {
                    GearForgeMod.LOGGER.warn("Encrypted YSM model resource missing: {}", ENCRYPTED_RESOURCE);
                    return;
                }
                byte[] plain = decrypt(in.readAllBytes());
                // 每次启动都写明文（上次已被删除），确保 YSM 能扫描到
                Files.write(target, plain,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
                GearForgeMod.LOGGER.info("Installed temporary YSM model for loading: {}", target);
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to install YSM model: {}", e.toString());
        }
    }

    private static Path modelDir() {
        return Path.of("config", "yes_steve_model", "custom");
    }

    /** 删除磁盘明文模型（YSM 已加载进内存后调用）。供服务端与客户端渲染器共用。 */
    public static void deletePlaintext() {
        try {
            Path target = modelDir().resolve(MODEL_FILE);
            if (Files.deleteIfExists(target)) {
                GearForgeMod.LOGGER.info("Removed temporary plaintext YSM model (anti-leak).");
            }
        } catch (Exception e) {
            GearForgeMod.LOGGER.warn("Failed to remove temporary YSM model: {}", e.toString());
        }
    }

    /** YSM 已加载完成后，删除磁盘明文，防止玩家复制模型源文件。 */
    @EventBusSubscriber(modid = GearForgeMod.MODID)
    public static final class Cleanup {
        private Cleanup() {}

        @SubscribeEvent
        public static void onServerStarted(ServerStartedEvent event) {
            // 延迟到服务端 tick 后删除，给客户端留出加载模型的时间窗。
            // 客户端渲染器首次成功套用模型后也会调用 deletePlaintext() 双保险。
            event.getServer().execute(() -> {
                try {
                    deletePlaintext();
                } catch (Exception ignored) {
                }
            });
        }
    }

    /** AES-128-CBC 解密模型数据。 */
    private static byte[] decrypt(byte[] encrypted) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY, "AES"), new IvParameterSpec(IV));
        return cipher.doFinal(encrypted);
    }
}
