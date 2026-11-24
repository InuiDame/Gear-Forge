package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.menu.CompanionMenu;
import com.hana.gear_forge.menu.ModMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
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
 * 下半"我的背包"（玩家背包 27 格 + 快捷栏 9 格）。槽位格子直接取原版
 * inventory.png 的现成图案逐格 blit，保证与原版观感一致且不错位。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class CompanionScreen extends AbstractContainerScreen<CompanionMenu> {

    /** 原版玩家背包贴图：内含画好的槽位格子图案。 */
    private static final ResourceLocation INVENTORY_PNG =
            ResourceLocation.withDefaultNamespace("textures/gui/container/inventory.png");

    // 原版 inventory.png 第一个背包格：描边左上角 (7,83)，18x18（含 1px 描边）。
    // 原版槽位 Slot 坐标就是格子内底左上角 (8,84)，故 blit 到 (slot.x-1, slot.y-1)
    // 并取源 (7,83) 即可 1:1 还原原版格子外观。
    private static final int SLOT_SRC_X = 7;
    private static final int SLOT_SRC_Y = 83;
    private static final int SLOT_SIZE = 18;

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.COMPANION.get(), CompanionScreen::new);
    }

    public CompanionScreen(CompanionMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
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

        // 面板底：深色外框 + 浅灰底（与 inventory.png 的面板同色）
        graphics.fill(x - 2, y - 2, x + this.imageWidth + 2, y + this.imageHeight + 2, 0xFF373737);
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);

        // 逐格 blit 原版 inventory.png 的格子图案。
        // 源 (7,83) 对应第一个背包格的描边左上角；Slot.x/y 是内底左上角，故目标为 (slot.x-1, slot.y-1)。
        // 物品由 super.render 画在 slot.x/y（内底），与原版完全一致。
        for (Slot slot : this.menu.slots) {
            graphics.blit(INVENTORY_PNG,
                    x + slot.x - 1, y + slot.y - 1,
                    SLOT_SRC_X, SLOT_SRC_Y,
                    SLOT_SIZE, SLOT_SIZE);
        }

        // 两区之间的分隔线（y≈76）
        graphics.fill(x + 7, y + 76, x + this.imageWidth - 7, y + 77, 0xFF8B8B8B);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
