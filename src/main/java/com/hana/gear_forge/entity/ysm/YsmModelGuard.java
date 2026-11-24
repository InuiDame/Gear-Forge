package com.hana.gear_forge.entity.ysm;

import java.util.Locale;

/**
 * 内部模型 id 判定工具（供版权隔离 mixin 复用）。
 *
 * <p>注意：本类必须在普通包下，不能放进 {@code com.hana.gear_forge.mixin.*}——
 * 该包被 gear_forge.mixins.json 声明为 mixin 包，从外部直接引用其中的类会抛
 * {@code IllegalClassLoadError: ... is in a defined mixin package ... cannot be referenced directly}。
 */
public final class YsmModelGuard {
    private YsmModelGuard() {}

    public static boolean isInternalModel(String modelId) {
        if (modelId == null) return false;
        String lower = modelId.toLowerCase(Locale.ROOT);
        return lower.contains("natsu")
                || modelId.contains("柚鸟夏")
                || modelId.contains("柚鳥夏");
    }
}
