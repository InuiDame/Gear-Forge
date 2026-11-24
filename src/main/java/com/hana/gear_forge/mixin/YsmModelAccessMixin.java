package com.hana.gear_forge.mixin;

import com.hana.gear_forge.entity.ysm.YsmNpc;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * 版权隔离：mod 内置的加密模型（柚鸟夏）不允许真实玩家通过 YSM GUI/命令给自己套用。
 *
 * <p>拦截 YSM model attachment 的 setModel(ServerPlayer, modelId)。当目标是普通玩家
 * （非我们的 {@link YsmNpc} 假玩家 NPC）且 modelId 命中内部模型 key 时直接取消。
 *
 * <p><b>关键</b>：YSM 是重度同名混淆 jar，方法名不参与官方映射，必须 {@code remap = false}，
 * 否则 Mixin 会用官方名重命名注入点、静默匹配失败（本 mixin 之前失效就是缺这个）。
 *
 * <p>真 Mob 实体方案（NatsuBossEntity / NatsuCompanionEntity）用的是纯客户端
 * {@code RemotePlayer} 代理 + attachment 的 setModel(String,String) 本地字段，
 * 不走这个 ServerPlayer 入口，因此不受影响。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.O0o0OoOOooOo0O0OOoo0Oo00", remap = false)
public abstract class YsmModelAccessMixin {

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void gearForge$blockInternalModelForRealPlayers(
            ServerPlayer player, String modelId, CallbackInfo ci) {
        if (player instanceof YsmNpc) return;   // 旧假玩家 NPC（若仍被使用）：放行
        if (YsmModelGuard.isInternalModel(modelId)) {
            ci.cancel();
        }
    }
}

/** 内部模型 id 判定（供多个 mixin 复用）。 */
final class YsmModelGuard {
    private YsmModelGuard() {}

    static boolean isInternalModel(String modelId) {
        if (modelId == null) return false;
        String lower = modelId.toLowerCase(Locale.ROOT);
        return lower.contains("natsu")
                || modelId.contains("柚鸟夏")
                || modelId.contains("柚鳥夏");
    }
}
