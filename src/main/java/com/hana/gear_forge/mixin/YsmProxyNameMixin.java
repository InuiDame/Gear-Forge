package com.hana.gear_forge.mixin;

import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 隐藏 YSM 渲染代理玩家的名字标签（否则头顶会显示 gearforge_ysm_proxy）。
 * 拦截 YSM 玩家渲染器的 renderName(Player, Component, ..., int, float)，
 * 当玩家名是代理名时取消。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.Oo00o00OOooooO0OooooOooO", remap = false)
public abstract class YsmProxyNameMixin {

    @Inject(method = "oOo0OO0O0o000OO0O000oo0o(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IF)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void gearForge$hideProxyName(Player player, Component name, PoseStack pose,
                                         MultiBufferSource buffer, int light,
                                         float partialTick, CallbackInfo ci) {
        if ("gearforge_ysm_proxy".equals(player.getName().getString())) {
            ci.cancel();
        }
    }
}
