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
 * 队友装备 + 背包的客户端界面（仿原版玩家背包布局）。
 *
 * <p>关键：菜单类型必须在 {@link RegisterMenuScreensEvent} 注册对应的 Screen，
 * 否则服务端 openMenu 成功、客户端却报 "Failed to create screen for menu type:
 * gear_forge:companion"，玩家看到右键没反应。
 *
 * <p>槽位在 CompanionMenu 里按 x=8+col*18 的标准网格排布（与原版玩家背包一致：
 * 第一行队友装备、接着队友背包 9x2、玩家背包 9x3、最后快捷栏 9）。
 * 背景用原版通用容器贴图 generic_54（9列×6行统一网格），槽位与贴图格子一一对齐、不错位。
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
        // 槽位到 y=126+16=142，留边取整 158；贴图按相同尺寸取顶部区域，避免拉伸导致错位
        this.imageHeight = 158;
        // 玩家背包标签放在玩家背包 27 格(第 3 行 y=72)上方
        this.inventoryLabelY = 59;
        this.titleLabelY = 6;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // generic_54.png 是 9 列统一网格，槽位 x=8+col*18、y=18+row*18。
        // 源和目标都用 imageWidth×imageHeight（1:1 像素），不乱缩放就不会错位。
        graphics.blit(BACKGROUND, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
