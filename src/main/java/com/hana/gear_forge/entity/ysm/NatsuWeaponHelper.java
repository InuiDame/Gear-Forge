package com.hana.gear_forge.entity.ysm;

import com.hana.gear_forge.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 柚鸟夏主手武器工厂。
 *
 * <p>玩家装了 TaCZ（永恒枪械工坊）且带有蔚蓝档案枪包时，直接取枪包里的
 * 真·超越沉思（GunId: {@code bluearchive:natsu}），这样模型、动画和音效
 * 都是 TaCZ 的原版表现。没装 TaCZ/枪包时降级到 mod 自带的简易 SMG 物品。
 *
 * <p>TaCZ 是 compileOnly 可选依赖，所有对它的引用都隔离在此类并通过
 * 反射调用，避免运行时缺类导致 NoClassDefFoundError。
 */
public final class NatsuWeaponHelper {

    private static final ResourceLocation GUN_ID =
            ResourceLocation.fromNamespaceAndPath("bluearchive", "natsu");

    private NatsuWeaponHelper() {}

    /** 返回超越沉思 ItemStack：优先 TaCZ 真枪，降级为自带枪。 */
    public static ItemStack createGun() {
        ItemStack tacGun = tryCreateTaczGun();
        return tacGun != null ? tacGun : new ItemStack(ModItems.NATSU_GUN.get());
    }

    /** 通过反射调用 TaCZ 的 GunItemBuilder，避免硬链接可选依赖。 */
    private static ItemStack tryCreateTaczGun() {
        try {
            Class<?> builderClass = Class.forName("com.tacz.guns.api.item.builder.GunItemBuilder");
            Object builder = builderClass.getMethod("create").invoke(null);
            // setId(ResourceLocation)
            builderClass.getMethod("setId", ResourceLocation.class).invoke(builder, GUN_ID);
            // setAmmoCount(int)
            builderClass.getMethod("setAmmoCount", int.class).invoke(builder, 30);
            // build() -> ItemStack
            Object result = builderClass.getMethod("build").invoke(builder);
            return result instanceof ItemStack stack && !stack.isEmpty() ? stack : null;
        } catch (Throwable t) {
            // 没装 TaCZ、没装枪包、或 API 变化：静默降级
            return null;
        }
    }
}
