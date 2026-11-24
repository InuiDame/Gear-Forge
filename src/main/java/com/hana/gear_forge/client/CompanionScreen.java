package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.menu.CompanionMenu;
import com.hana.gear_forge.menu.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 队友装备 + 背包的客户端界面（仿原版玩家背包，双背包分区显示）。
 *
 * <p>关键：菜单类型必须在 {@link RegisterMenuScreensEvent} 注册对应 Screen，
 * 否则服务端 openMenu 成功、客户端报 "Failed to create screen for menu type:
 * gear_forge:companion"，玩家看到右键没反应。
 *
 * <p>布局分两区：上半"小夏的装备与背包"（装备 6 槽 + 队友背包 18 格），
 * 下半"我的背包"（玩家背包 27 格 + 快捷栏 9 格）。背景自绘纯面板，
 * 格子由槽位自身渲染——不贴 generic_54，避免贴图格子与槽位双重错位。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class CompanionScreen extends AbstractContainerScreen<CompanionMenu> {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPANION.get(), CompanionScreen::new);
    }

    public CompanionScreen(CompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176; // 快捷栏 y=150 + 槽位高 16 + 底边
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        // "物品栏"标签移到玩家背包区上方（队友背包底 y=70，玩家背包顶 y=90）
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 78;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        // 仿原版 GUI 面板：深色外框 + 浅灰底。槽位自身会画格子，这里只画面板。
        graphics.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, 0xFF373737);
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        // 两区之间的分隔线（y≈78）
        graphics.fill(x + 7, y + 78, x + this.imageWidth - 7, y + 79, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
