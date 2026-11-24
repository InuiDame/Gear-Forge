package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 柚鸟夏 NPC 的事件占位类。
 *
 * <p>队友的右键交互（开装备/背包菜单、Shift+右键切换状态）已迁移到
 * {@link com.hana.gear_forge.entity.ysm.NatsuCompanionEntity#mobInteract} 实体方法里。
 * 原因：1.21.1 原版 {@code Mob.interact → mobInteract} 会先处理 TamableAnimal 右键
 * （切换 sit 并返回 SUCCESS 消费交互），导致 PlayerInteractEvent.EntityInteract 事件
 * 里的处理器收不到队友右键。走实体方法这条原版路径最可靠。
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class YsmNpcEvents {

    private YsmNpcEvents() {}
}
