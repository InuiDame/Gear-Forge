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
            // 模型已成功套用 = YSM 客户端已加载进内存，此时删除磁盘明文防泄露。
            com.hana.gear_forge.entity.ysm.YsmModelInstaller.deletePlaintext();
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
            GearForgeMod.LOGGER.warn("YSM proxy render failed, falling back: {}", t.toString());
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

        // 攻击/挥手动画：同步 swing 计时与朝向，让挥击动作显示
        proxy.swingTime = entity.swingTime;
        proxy.swinging = entity.swinging;
        proxy.swingingArm = entity.swingingArm;

        // 精确拷贝行走动画相位：直接从真实体的 WalkAnimationState 复制，
        // 而不是自己 update（自己推进相位不对，腿不动/看似平移）。
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
