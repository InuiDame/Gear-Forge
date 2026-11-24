package com.hana.gear_forge.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;

/**
 * 版权隔离：把 mod 内置的柚鸟夏模型从 YSM 玩家模型选择 GUI 里隐藏。
 *
 * <p>YSM 的玩家模型选择页（"YSM Player Model GUI"）构造函数会建一个隐藏集合
 * {@code oOOO00ooO0oOOoOOo0OoOOOo}，渲染模型条目时过滤掉集合里的 id。原本它只从
 * 配置 {@code ClientNotDisplayModels} 填充；这里在构造末尾强行把内部模型 id 加进去，
 * 这样玩家在切换页面看不到、也点不到我们的 natsu 模型（不影响我们 Boss 用 attachment 套用）。
 *
 * <p>混淆 jar 方法同名，必须带 {@code remap = false}（与 YsmExportCommandMixin 一致）。
 */
@Mixin(targets = "com.elfmcys.yesstevemodel.O0o0Oo0Oo0Ooo0oO000o0OOO", remap = false)
public abstract class YsmModelGuiHideMixin {

    /** 访问 GUI 的隐藏模型 id 集合字段（@Accessor 方法必须是抽象的）。 */
    @org.spongepowered.asm.mixin.gen.Accessor("oOOO00ooO0oOOoOOo0OoOOOo")
    abstract HashSet<String> gearForge$getHiddenModels();

    @Inject(method = "<init>()V", at = @At("TAIL"), remap = false)
    private void gearForge$hideInternalModel(CallbackInfo ci) {
        try {
            HashSet<String> hidden = this.gearForge$getHiddenModels();
            if (hidden != null) {
                // 模型注册表 key 与常见别名一律隐藏
                hidden.add("natsu.ysm");
                hidden.add("natsu");
                hidden.add("柚鸟夏");
                hidden.add("柚鸟夏：常服");
                hidden.add("BA_柚鸟夏：常服");
                hidden.add("柚鳥夏");
            }
        } catch (Throwable ignored) {
            // 字段混淆名若变化，静默跳过（有 YsmModelAccessMixin 兜底）
        }
    }
}
