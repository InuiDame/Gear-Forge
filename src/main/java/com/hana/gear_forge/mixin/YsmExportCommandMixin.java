package com.hana.gear_forge.mixin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 版权保护：禁用 YSM 的 {@code /ysm export} 子命令。
 *
 * <p>该命令能把已加载的模型（包括我们内置的柚鸟夏）重新导出成 .ysm 文件，
 * 玩家可用它拿到模型源文件。这里把 export 命令的构建方法整个替换成一个
 * 只返回错误提示的空命令，彻底移除导出逻辑。
 *
 * <p>目标方法无参，返回 {@code LiteralArgumentBuilder<CommandSourceStack>}。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.oOOooOo00o00O0O0OOo0oOOo", remap = false)
public abstract class YsmExportCommandMixin {

    @Inject(
            method = "oOo0OO0O0o000OO0O000oo0o()Lcom/mojang/brigadier/builder/LiteralArgumentBuilder;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private static void gearForge$disableExport(CallbackInfoReturnable<LiteralArgumentBuilder<CommandSourceStack>> cir) {
        LiteralArgumentBuilder<CommandSourceStack> disabled = Commands.literal("export")
                .executes(ctx -> {
                    ctx.getSource().sendFailure(Component.literal(
                            "§c此命令已被 Gear-Forge 禁用（内置模型受版权保护）。"));
                    return 0;
                });
        cir.setReturnValue(disabled);
    }
}
