package com.hana.gear_forge.menu;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.entity.ysm.NatsuCompanionEntity;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;

/** 菜单类型注册。 */
public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, GearForgeMod.MODID);

    /**
     * 强化台菜单。
     *
     * <p>必须显式注册一个 {@code MenuType}：原版 {@code MenuType.SMITHING} 构造出来的
     * 永远是 {@code SmithingMenu}，客户端拿到菜单类型后会按类型实例化对应菜单类，
     * 直接复用原版类型会让服务端的 {@link ForgeTableMenu} 在客户端变成 {@code SmithingMenu}。
     */
    public static final DeferredHolder<MenuType<?>, MenuType<ForgeTableMenu>> FORGE_TABLE =
            MENU_TYPES.register("forge_table",
                    () -> new MenuType<>(ForgeTableMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    /** 队友装备 + 背包菜单：用 3 参 IContainerFactory 从 extraData 读实体 ID。 */
    public static final DeferredHolder<MenuType<?>, MenuType<CompanionMenu>> COMPANION =
            MENU_TYPES.register("companion",
                    () -> new MenuType<>(
                            (net.neoforged.neoforge.network.IContainerFactory<CompanionMenu>) CompanionMenu::new,
                            net.minecraft.world.flag.FeatureFlags.VANILLA_SET));

    private ModMenus() {}
}
