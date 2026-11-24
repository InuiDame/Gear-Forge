package com.hana.gear_forge.mixin;

import com.hana.gear_forge.entity.ysm.YsmNpc;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 版权隔离兜底：拦截 YSM 模型管理器底层的 setModel(ServerPlayer, boolean, modelId)。
 *
 * <p>玩家通过 /ysm set 命令或模型选择 GUI 最终都会走到这里给真实玩家发包套模型。
 * 单拦 attachment 层（{@link YsmModelAccessMixin}）可能被底层重载绕过，这里再拦一道。
 * 我们的假玩家 NPC（{@link YsmNpc}）放行；真 Mob 实体的客户端代理不走 ServerPlayer 入口。
 *
 * <p>混淆 jar，必须 {@code remap = false}。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.OO0Oo0O00OOOo0oOo0oooooO", remap = false)
public abstract class YsmModelSyncGuardMixin {

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/server/level/ServerPlayer;ZLjava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void gearForge$blockNatsuAtManager(
            ServerPlayer player, boolean sendToClient, String modelId, CallbackInfo ci) {
        if (player instanceof YsmNpc) return;
        if (YsmModelGuard.isInternalModel(modelId)) {
            ci.cancel();
        }
    }
}
