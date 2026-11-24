package com.hana.gear_forge.mixin;

import com.hana.gear_forge.entity.ysm.YsmNpc;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 版权隔离：mod 内置的加密模型（柚鸟夏）只允许 GearForge 的 Boss/队友 NPC 使用，
 * 真实玩家不能通过 YSM 的 GUI 或 /ysm 命令给自己套用这个模型。
 *
 * <p>拦截 YSM model attachment 的 setModel(ServerPlayer, modelId)。当目标是普通玩家
 * （非我们的 {@link YsmNpc}）且 modelId 命中内部模型 key 时直接取消。
 * 模型本身仍以加密资源打包、运行时只解密写入 YSM custom 目录供 native 加载，
 * 但玩家无法在 YSM 面板/命令里选用它。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.O0o0OoOOooOo0O0OOoo0Oo00")
public abstract class YsmModelAccessMixin {

    @Inject(method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true)
    private void gearForge$blockInternalModelForRealPlayers(
            ServerPlayer player, String modelId, CallbackInfo ci) {
        if (player instanceof YsmNpc) return;   // 我们的 Boss/队友：放行
        // 内部模型 key（无扩展名与带扩展名两种形式都拦截）
        String id = modelId == null ? "" : modelId;
        if (YsmNpc.NATSU_MODEL_ID.equals(id)
                || (YsmNpc.NATSU_MODEL_ID + ".ysm").equals(id)
                || "natsu".equals(id)
                || "natsu.ysm".equals(id)
                || id.contains("natsu")) {
            ci.cancel();
        }
    }
}
