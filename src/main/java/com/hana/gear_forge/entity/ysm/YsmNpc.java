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

    /**
     * YSM 模型 id：必须是去掉扩展名的模型 key。
     * 磁盘文件名是 natsu.ysm（见 YsmModelInstaller.MODEL_FILE），但 YSM 加载器会把
     * .ysm/.zip/.7z 后缀去掉作为注册表 key，故 attachment 与同步包里要用 "natsu"。
     */
    public static final String NATSU_MODEL_ID = "natsu";

    private final MinecraftServer server;
    private final String modelId;
    private int tabHideCooldown = 0;
    /** 生成后重发模型设置的剩余次数：追踪关系在加入世界后下一 tick 才建立，需延迟重发。 */
    private int modelRetry = 3;

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

    /**
     * 设置 YSM 模型。
     *
     * <p>必须调用带 ServerPlayer 参数的重载 {@code setModel(ServerPlayer, modelId)}：
     * 它内部会调用 YSM 的模型管理器 setModel(player, boolean, modelId)，后者不仅写入
     * modelId，还会通过 {@code PacketDistributor.sendToPlayersTrackingEntityAndSelf}
     * 把模型数据包发给所有追踪本假玩家实体的真实客户端——这才是让模型真正显示的关键。
     * 之前调用的 setModelAndTexture(String,String) / setMandatory(boolean) 只改 attachment
     * 内部字段、不触发同步，所以角色一直是默认 Steve 皮肤。
     */
    private void applyYsmModel() {
        try {
            // 探测 YSM 注册表中的模型 key：加载器会把 .ysm 后缀去掉，故可能是 "natsu" 或 "natsu.ysm"。
            // 用实际存在的那个，避免猜错导致模型套不上。
            String resolved = modelId;
            try {
                boolean plain = com.elfmcys.yesstevemodel.O0Ooo000O0ooO00Oooo00oOO
                        .oOo0OO0O0o000OO0O000oo0o(modelId).isPresent();
                boolean withExt = com.elfmcys.yesstevemodel.O0Ooo000O0ooO00Oooo00oOO
                        .oOo0OO0O0o000OO0O000oo0o(modelId + ".ysm").isPresent();
                com.hana.gear_forge.GearForgeMod.LOGGER.info(
                        "YSM model lookup '{}'={}, '{}.ysm'={}", modelId, plain, modelId, withExt);
                if (!plain && withExt) resolved = modelId + ".ysm";
            } catch (Throwable probe) {
                com.hana.gear_forge.GearForgeMod.LOGGER.warn("YSM model lookup failed: {}", probe.toString());
            }

            O0o0OoOOooOo0O0OOoo0Oo00 cap =
                    this.getData(O0o0OoOOooOo0O0OOoo0Oo00.oOo0OO0O0o000OO0O000oo0o);
            cap.oOo0OO0O0o000OO0O000oo0o(this, resolved);
            String actual = cap.oOoo00O0o0oO0o0oO00OO0O0();
            com.hana.gear_forge.GearForgeMod.LOGGER.info(
                    "YSM model applied to NPC: requested='{}' attachment='{}'", resolved, actual);
        } catch (Throwable t) {
            // YSM 未安装/版本不匹配/模型加载失败时降级为默认皮肤，但写日志便于排查
            com.hana.gear_forge.GearForgeMod.LOGGER.warn(
                    "Failed to apply YSM model '{}' to NPC: {}", modelId, t.toString());
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
        // 前几 tick 重发模型：实体刚加入世界时尚无追踪者，模型包可能发不出去。
        // 在第 5/20/60 tick 各重发一次，确保附近的真实客户端收到并套上模型。
        if (modelRetry > 0) {
            int elapsed = this.tickCount;
            if ((modelRetry == 3 && elapsed >= 5)
                    || (modelRetry == 2 && elapsed >= 20)
                    || (modelRetry == 1 && elapsed >= 60)) {
                modelRetry--;
                applyYsmModel();
            }
        }
    }

    public String getModelId() {
        return modelId;
    }
}
