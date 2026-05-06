package com.hana.gear_forge.item;

import com.hana.gear_forge.GearForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GearForgeMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GEAR_FORGE_TAB =
            CREATIVE_TABS.register("gear_forge_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + GearForgeMod.MODID))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ModItems.FORGE_HAMMER.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.FORGE_HAMMER.get());
                        output.accept(ModItems.REROLL_STONE.get());
                        output.accept(ModItems.UPGRADE_CRYSTAL.get());
                        output.accept(ModItems.RUBY.get());
                        output.accept(ModItems.SAPPHIRE.get());
                        output.accept(ModItems.EMERALD_GEM.get());
                        output.accept(ModItems.TOPAZ.get());
                        output.accept(ModItems.JADE.get());
                        output.accept(ModItems.ONYX.get());
                        output.accept(ModItems.TRANSFER_ORB.get());
                        output.accept(ModItems.GEM_REMOVER.get());
                        output.accept(ModItems.REINCARNATION_STONE.get());
                    }).build());
}
