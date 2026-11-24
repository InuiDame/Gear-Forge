package com.hana.gear_forge.mixin;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.YsmModelInstaller;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 资源重载时自动重新解密注入明文模型，扫描完成后再删除。
 *
 * <p>矛盾：YSM 每次资源重载（返回主菜单重进世界、F3+T、切维度）都会重新用 native 扫描
 * {@code config/yes_steve_model/custom/} 重建内存模型注册表。若明文已删，注册表里就没有
 * natsu → Boss 回默认皮肤。之前只在 ServerStartedEvent 删一次，重载后就失效。
 *
 * <p>方案（用户选定）：hook YSM 的 reload listener 私有方法 {@code onResourceManagerReload}，
 * 进入时重新解密写出明文（HEAD），YSM 扫描加载完成后延迟一段时间删除（TAIL，给 native
 * 扫描和渲染器重建留出窗口）。这样每次重载都有明文可用，但明文只在重载窗口期存在，
 * 玩家平时看不到、拿不到。用 generation 计数避免旧的延迟删除误删新一轮注入的文件。
 *
 * <p>混淆 jar，必须 {@code remap = false}。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.oooO0Ooo0oOOOOooOoo00Oo0", remap = false)
public abstract class YsmReloadMixin {

    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "gearforge-ysm-cleanup");
        t.setDaemon(true);
        return t;
    });
    /** 每次重载自增；延迟删除时比对，只有仍是最新一代才删。 */
    private static final AtomicLong GENERATION = new AtomicLong(0);

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("HEAD"), remap = false)
    private static void gearForge$reinstallBeforeYsmReload(ResourceManager manager, CallbackInfo ci) {
        try {
            GENERATION.incrementAndGet();
            YsmModelInstaller.install();
            GearForgeMod.LOGGER.info("Re-injected temporary YSM model before resource reload scan.");
        } catch (Throwable t) {
            GearForgeMod.LOGGER.warn("Failed to re-inject YSM model on reload: {}", t.toString());
        }
    }

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("TAIL"), remap = false)
    private static void gearForge$deleteAfterYsmReload(ResourceManager manager, CallbackInfo ci) {
        final long mine = GENERATION.get();
        // 延迟删除：YSM 的 native 扫描是异步的，TAIL 时可能还在读取文件。留 3 秒窗口。
        CLEANUP_EXECUTOR.schedule(() -> {
            try {
                if (GENERATION.get() == mine) {
                    YsmModelInstaller.deletePlaintext();
                }
            } catch (Throwable ignored) {
            }
        }, 3, TimeUnit.SECONDS);
    }
}
