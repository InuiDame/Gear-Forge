package com.hana.gear_forge.client;

import com.elfmcys.yesstevemodel.OoooO0OO0000O00oo0Oo00OO;
import com.elfmcys.yesstevemodel.oooO0Ooo0oOOOOooOoo00Oo0;
import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.YsmModelProvider;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.UUID;

/**
 * 用 YSM 玩家模型渲染柚鸟夏实体（Boss / 队友）。
 *
 * <p>原理：真 Mob 实体负责全部游戏逻辑（AI/寻路/碰撞/受伤），本渲染器创建一个纯客户端的
 * {@link RemotePlayer} 代理，每帧把实体的位置/朝向/行走状态同步给代理，设置 YSM 模型 id
 * 后调用 YSM 的玩家渲染单例绘制。全程使用 YSM 的 public API，无需 mixin、不碰 native。
 */
public class YsmEntityRenderer<T extends LivingEntity & YsmModelProvider>
        extends LivingEntityRenderer<T, net.minecraft.client.model.PlayerModel<T>> {

    private RemotePlayer proxy;
    private boolean modelSet = false;
    private String modelId;
    // 动画时间轴：proxy.tickCount 跟随真实实体 tick（20/s），重算 offset 保证实体切换时单调
    private int lastEntityId = -1;
    private int tickOffset = 0;
    // 攻击动画：记录上一帧挥手状态，检测上升沿时强制干净重启 proxy 的 swing。
    // YSM 的 attack 是状态驱动动画；长生命周期 proxy 直接拷贝 swinging=true 不会
    // 重新触发同名动画（只第一次有效），需要先清 false 再走一次 swing(hand)。
    private boolean prevSwinging = false;
    private int prevSwingTime = 0;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public YsmEntityRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, (net.minecraft.client.model.PlayerModel) new net.minecraft.client.model.PlayerModel<>(
                ctx.bakeLayer(net.minecraft.client.model.geom.ModelLayers.PLAYER), false), 0.5F);
    }

    private void ensureProxy() {
        if (proxy != null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), "gearforge_ysm_proxy");
            proxy = new RemotePlayer(mc.level, profile);
            // 隐藏代理的玩家名/皮肤层干扰：让所有 model part 都显示（默认即是）
            GearForgeMod.LOGGER.info("Created YSM render proxy player");
        } catch (Throwable t) {
            GearForgeMod.LOGGER.warn("Failed to create YSM render proxy: {}", t.toString());
        }
    }

    private void applyModel() {
        if (modelSet || proxy == null || modelId == null) return;
        try {
            OoooO0OO0000O00oo0Oo00OO data =
                    OoooO0OO0000O00oo0Oo00OO.oOoo00O0o0oO0o0oO00OO0O0(proxy).orElse(null);
            if (data == null) return;
            // 两个参数：modelId + skin（skin 无效时回退模型默认贴图）
            data.oOo0OO0O0o000OO0O000oo0o(modelId, "default");
            modelSet = true;
            GearForgeMod.LOGGER.info("YSM render proxy model set to '{}'", modelId);
            // 注意：不要在这里删明文。模型加载由 YSM 的 reload listener 统一完成，
            // 明文删除由 ServerStartedEvent 在加载完成后统一执行（见 YsmModelInstaller）。
        } catch (Throwable t) {
            GearForgeMod.LOGGER.warn("Failed to set YSM proxy model: {}", t.toString());
        }
    }

    @Override
    public void render(T entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        ensureProxy();
        if (proxy == null) {
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        if (!modelIdEquals(entity.getModelId())) {
            modelId = entity.getModelId();
            modelSet = false;
        }
        applyModel();

        syncProxy(entity, partialTick);
        try {
            var ysmRenderer = oooO0Ooo0oOOOOooOoo00Oo0.oOo0OO0O0o000OO0O000oo0o();
            ysmRenderer.oOo0OO0O0o000OO0O000oo0o(proxy, entityYaw, partialTick,
                    poseStack, buffer, packedLight);
        } catch (Throwable t) {
            // 打印完整堆栈：YSM 渲染异常会被兜底成默认 Steve，必须能看到根因。
            GearForgeMod.LOGGER.warn("YSM proxy render failed for {}, falling back to default skin",
                    entity.getType().getDescriptionId(), t);
            super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
        }
    }

    private boolean modelIdEquals(String id) {
        return id != null && id.equals(modelId);
    }

    private void syncProxy(T entity, float partialTick) {
        proxy.setPos(entity.getX(), entity.getY(), entity.getZ());
        proxy.xo = entity.xo;
        proxy.yo = entity.yo;
        proxy.zo = entity.zo;

        proxy.setYRot(entity.getYRot());
        proxy.yRotO = entity.yRotO;
        proxy.setXRot(entity.getXRot());
        proxy.xRotO = entity.xRotO;
        proxy.yHeadRot = entity.getYHeadRot();
        proxy.yHeadRotO = entity.yHeadRotO;
        proxy.yBodyRot = entity.yBodyRot;
        proxy.yBodyRotO = entity.yBodyRotO;

        proxy.setPose(entity.getPose());
        proxy.setShiftKeyDown(entity.isShiftKeyDown());
        proxy.setOnGround(entity.onGround());

        // 攻击/挥手/受伤动画：YSM 动画谓词读这些字段选 swing/use/attacked 动画。
        // swing 关键：必须检测到 swinging 的"上升沿"并让 swingTime 从起点重新计数，
        // 否则长生命周期 proxy 上同名攻击动画只在第一次播放。
        boolean swinging = entity.swinging;
        int swingTime = entity.swingTime;
        net.minecraft.world.InteractionHand swingArm = entity.swingingArm;
        boolean risingEdge = swinging && !prevSwinging;
        // swingTime 回退到 0/1 也算新一轮（1.21.1 每 6 tick 一次攻击，swinging 可能一直为 true）
        boolean restarted = swinging && prevSwinging && swingTime <= 1 && prevSwingTime > 1;
        if (risingEdge || restarted) {
            proxy.swinging = false;
            proxy.swingTime = 0;
            // 走原版 swing() 触发干净的动画状态转换（swingTime=-1 → 0 起步）
            if (swingArm != null) {
                proxy.swing(swingArm);
            } else {
                proxy.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }
        proxy.swingTime = swingTime;
        proxy.swinging = swinging;
        proxy.swingingArm = swingArm;
        prevSwinging = swinging;
        prevSwingTime = swingTime;
        proxy.hurtTime = entity.hurtTime;
        proxy.hurtDuration = entity.hurtDuration;
        proxy.setSprinting(entity.isSprinting());

        // 【装备同步】YSM 的装备/手持层读渲染目标(proxy)的 getItemBySlot()，
        // 不是真实体。必须把真实体的装备复制到 proxy，否则模型上看不到盔甲和武器。
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            proxy.setItemSlot(slot, entity.getItemBySlot(slot).copy());
        }

        // 【关键】YSM 动画时间轴 = Entity.tickCount + partialTick，帧间累加成 accumulator，
        // 关键帧按 accumulator/20 采样，即设计速率是 20 tick/秒（原版 TPS）。
        // 之前用 max(proxy.tickCount+1, entity.tickCount) 每"渲染帧"+1，60FPS 下
        // 每秒 +60 → 动画快 3 倍、受伤/挥剑动画被压缩变短。
        // 正确做法：时间轴严格跟随真实实体的 tick（每秒 +20）。同一实体内 tickCount 单调
        // 自增；实体切换/重新召唤时新实体 tickCount 回退到 0，用 offset 把它平移到
        // 上次时间之后，保证 accumulator 永不检测到倒退（不重置），同时速率正确。
        int tick = entity.tickCount;
        if (entity.getId() != lastEntityId) {
            tickOffset = proxy.tickCount - tick + 1;
            lastEntityId = entity.getId();
        }
        proxy.tickCount = tickOffset + tick;

        // 行走动画相位：walk 谓词读 abs(speed)>0.05 决定是否播放行走动画。
        copyWalkAnimation(entity.walkAnimation, proxy.walkAnimation);
    }

    private void copyWalkAnimation(net.minecraft.world.entity.WalkAnimationState from,
                                   net.minecraft.world.entity.WalkAnimationState to) {
        try {
            com.hana.gear_forge.mixin.WalkAnimationAccessor src =
                    (com.hana.gear_forge.mixin.WalkAnimationAccessor) (Object) from;
            com.hana.gear_forge.mixin.WalkAnimationAccessor dst =
                    (com.hana.gear_forge.mixin.WalkAnimationAccessor) (Object) to;
            dst.gearForge$setSpeedOld(src.gearForge$getSpeedOld());
            dst.gearForge$setSpeed(src.gearForge$getSpeed());
            dst.gearForge$setPosition(src.gearForge$getPosition());
        } catch (Throwable ignored) {
            // mixin 未生效时退回 setSpeed，至少有速度
            to.setSpeed(from.speed());
        }
    }

    @Override
    public net.minecraft.resources.ResourceLocation getTextureLocation(T entity) {
        // fallback 皮肤：YSM 正常渲染时会用模型自身贴图，这里只在 YSM 失败时用到
        return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();
    }

    @Override
    protected boolean shouldShowName(T entity) {
        return false; // 不显示代理玩家名，名字由实体自身渲染
    }
}
