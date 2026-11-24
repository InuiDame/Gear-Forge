package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.menu.CompanionMenu;
import com.hana.gear_forge.menu.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 队友装备 + 背包的客户端界面。
 *
 * <p>关键：菜单类型必须在 {@link RegisterMenuScreensEvent} 注册对应的 Screen，
 * 否则服务端 openMenu 成功、客户端却报 "Failed to create screen for menu type:
 * gear_forge:companion"，玩家看到右键没反应。之前只实现了 CompanionMenu 忘了注册 Screen。
 *
 * <p>背景直接复用原版通用容器贴图 generic_54（9列×6行），槽位布局与 CompanionMenu 一致：
 * 上排队友装备/背包，下排玩家背包与快捷栏。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class CompanionScreen extends AbstractContainerScreen<CompanionMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPANION.get(), CompanionScreen::new);
    }

    public CompanionScreen(CompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166; // 覆盖到快捷栏(y=120)即可
        this.inventoryLabelY = 109;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // generic_54.png 画前 166 像素高（够覆盖所有槽位）
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
