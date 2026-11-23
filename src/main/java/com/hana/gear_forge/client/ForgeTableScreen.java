package com.hana.gear_forge.client;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.menu.ForgeTableMenu;
import com.hana.gear_forge.menu.ModMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.ItemCombinerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 强化台的客户端界面。
 *
 * <p>用 NeoForge 的 {@link RegisterMenuScreensEvent} 注册——原版 {@code MenuScreens.register}
 * 在 1.21.1 里已经是 private，只能通过这个 mod 总线事件登记。
 *
 * <p>直接复用原版 {@code smithing.png} 作为背景：三格布局与锻造台完全一致，
 * 唯一的区别是不画盔甲架预览（强化不改变外观）。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
public final class ForgeTableScreen {

    private ForgeTableScreen() {}

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.FORGE_TABLE.get(), Screen::new);
    }

    /** 只保留背景与错误图标的极简界面。 */
    public static class Screen extends ItemCombinerScreen<ForgeTableMenu> {

        private static final Identifier ERROR_SPRITE =
                Identifier.withDefaultNamespace("container/smithing/error");

        public Screen(ForgeTableMenu menu, Inventory playerInventory, Component title) {
            super(menu, playerInventory, title, ForgeTableMenu.GUI_TEXTURE);
            this.titleLabelX = 44;
            this.titleLabelY = 15;
        }

        @Override
        protected void extractErrorIcon(GuiGraphicsExtractor graphics, int x, int y) {
            if (this.menu.hasRecipeError()) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ERROR_SPRITE, x + 65, y + 46, 28, 21);
            }
        }
    }
}
