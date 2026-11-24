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
        // 不用 setInvulnerable(true)：原版 isInvulnerable() 恒真会让伤害系统直接忽略一切攻击，
        // 导致 Boss 打不动。Boss 的无敌帧由 NatsuBossNpc.hurt() 的 phaseTransition 自行控制。

        // 加入 PlayerList：建立 PlayerInfo（客户端才能生成 RemotePlayer），并触发 YSM 同步跟踪
        server.getPlayerList().placeNewPlayer(
                new DummyConnection(), this,
                CommonListenerCookie.createInitial(this.getGameProfile(), false));

        // 设置 YSM 模型
        applyYsmModel();

        // 诊断对照：暂不隐藏 tab，验证 PlayerInfoRemove 是否干扰 YSM 模型在客户端的渲染/同步。
        // hideFromTab();
    }

    /**
     * 设置 YSM 模型。反编译 YSM 2.6.5 确认两条路径都要走：
     * <ol>
     *   <li>{@code setModel(String modelId, String texture)} 写 attachment 的持久化字段
     *       （field #53=modelId, #47=texture），客户端实体数据同步时据此选模型渲染。</li>
     *   <li>{@code setModel(ServerPlayer, modelId)} 是 YSM 换模型命令的入口，内部走模型管理器
     *       setModel(player, sendToClient, modelId) → 发包给追踪本实体的真实客户端。</li>
     * </ol>
     * 只调其中一个都不完整：前者不发包、后者不写 attachment 字段。
     */
    private void applyYsmModel() {
        try {
            O0o0OoOOooOo0O0OOoo0Oo00 cap =
                    this.getData(O0o0OoOOooOo0O0OOoo0Oo00.oOo0OO0O0o000OO0O000oo0o);
            // 1) 写 attachment 字段：modelId + 纹理（"-" 表示用模型默认纹理）
            cap.oOo0OO0O0o000OO0O000oo0o(modelId, "-");
            // 2) 触发 YSM 管理器发包同步
            cap.oOo0OO0O0o000OO0O000oo0o(this, modelId);
            com.hana.gear_forge.GearForgeMod.LOGGER.info(
                    "YSM model applied to NPC '{}': modelId='{}' attachment='{}'",
                    this.getName().getString(), modelId, cap.oOoo00O0o0oO0o0oO00OO0O0());
        } catch (Throwable t) {
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
        // 诊断对照：tab 隐藏暂关
        // if (--tabHideCooldown <= 0) {
        //     tabHideCooldown = 100;
        //     hideFromTab();
        // }
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

    /**
     * 服务端直接驱动 NPC 朝目标移动。
     *
     * <p>假玩家是 ServerPlayer，正常玩家的位置由客户端上报驱动：{@code ServerGamePacketListenerImpl.tick()}
     * 在 doTick() 之后会用 {@code absMoveTo(firstGoodX, ...)} 把实体拉回客户端确认的位置，
     * 所以单纯 setDeltaMovement() 会被抹掉、看起来"不动"。这里直接 absMoveTo 到新位置，
     * 位置立即生效且随实体数据同步发给周围客户端。
     */
    public void moveToward(Vec3 target, double speed) {
        Vec3 dir = target.subtract(this.position());
        dir = new Vec3(dir.x, 0.0, dir.z);   // 只在水平面移动，Y 交给重力/地面
        double len = dir.length();
        if (len < 0.1) return;
        // 限制单 tick 位移，避免瞬移
        double step = Math.min(speed, len);
        Vec3 delta = dir.scale(step / len);
        double nx = this.getX() + delta.x;
        double nz = this.getZ() + delta.z;
        float yaw = (float) (Math.atan2(-dir.x, dir.z) * 180.0 / Math.PI);
        this.setYRot(yaw);
        this.absMoveTo(nx, this.getY(), nz, yaw, this.getXRot());
    }
}
