package com.hana.gear_forge.entity.ysm;

import com.elfmcys.yesstevemodel.O0o0OoOOooOo0O0OOoo0Oo00;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/**
 * 由 GearForge 管理的假玩家 NPC。
 *
 * <p>利用 vanilla 原生管线：ServerPlayer → PlayerList → ADD_PLAYER/ADD_ENTITY →
 * 客户端 RemotePlayer → PlayerRenderer → RenderPlayerEvent.Pre → YSM 自动套模型。
 * 本类只接触 YSM 的 model_id attachment（混淆类），不碰其内部渲染/mixin。
 *
 * <p>假玩家留在 PlayerList 中以保证 YSM 的模型/动画同步（YSM 只同步被跟踪的 ServerPlayer），
 * 但通过 PlayerInfoRemove 包从 tab 列表隐藏。
 */
public class YsmNpc extends ServerPlayer {

    /** YSM 模型 id：就是 config/yes_steve_model/custom/ 下的 .ysm 文件名。 */
    public static final String NATSU_MODEL_ID = "natsu.ysm";

    private final MinecraftServer server;
    private final String modelId;
    private int tabHideCooldown = 0;

    public YsmNpc(MinecraftServer server, ServerLevel level, String name, Vec3 pos, String modelId) {
        super(server, level, new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        this.server = server;
        this.modelId = modelId;
        this.setPos(pos);
        this.setInvulnerable(true);

        // 加入 PlayerList：建立 PlayerInfo（客户端才能生成 RemotePlayer），并触发 YSM 同步跟踪
        server.getPlayerList().placeNewPlayer(
                new DummyConnection(), this,
                CommonListenerCookie.createInitial(this.getGameProfile(), false));

        // 设置 YSM 模型
        applyYsmModel();

        // 从 tab 隐藏（必须在 placeNewPlayer 之后）
        hideFromTab();
    }

    /** 设置 YSM 模型：model_id attachment 的 setModelAndTexture + setMandatory。 */
    private void applyYsmModel() {
        try {
            O0o0OoOOooOo0O0OOoo0Oo00 cap =
                    this.getData(O0o0OoOOooOo0O0OOoo0Oo00.oOo0OO0O0o000OO0O000oo0o);
            // 纹理传 "-" 让 YSM 用模型默认纹理；setMandatory(true) 让无连接的假玩家也同步
            cap.oOo0OO0O0o000OO0O000oo0o(modelId, "-");
            cap.oOoo00O0o0oO0o0oO00OO0O0(true);
        } catch (Throwable ignored) {
            // YSM 未安装或版本不匹配时静默降级（NPC 会显示为默认 Steve 皮肤）
        }
    }

    /** 从 tab 列表隐藏。需要周期性重发，对抗 PlayerList 的 600 tick 广播。 */
    public void hideFromTab() {
        server.getPlayerList().broadcastAll(
                new ClientboundPlayerInfoRemovePacket(List.of(this.getUUID())));
    }

    @Override
    public void tick() {
        super.tick();
        if (--tabHideCooldown <= 0) {
            tabHideCooldown = 100;   // 每 5 秒重发一次，压制 600 tick 的重新广播
            hideFromTab();
        }
    }

    public String getModelId() {
        return modelId;
    }
}
