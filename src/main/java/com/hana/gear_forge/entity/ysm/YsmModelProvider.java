package com.hana.gear_forge.entity.ysm;

/** 由 YSM 渲染代理读取模型 id 的接口。 */
public interface YsmModelProvider {
    /** YSM 客户端注册表中的模型 key（带 .ysm 后缀，实测确认）。 */
    String NATSU_MODEL_ID = "natsu.ysm";

    String getModelId();
}
