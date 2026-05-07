package com.hana.gear_forge.Curios;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

public class LuckyDiamondItem extends Item implements ICurioItem {

    public LuckyDiamondItem() {
        super(new Properties().stacksTo(1).fireResistant());
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        Multimap<Holder<Attribute>, AttributeModifier> multimap = ArrayListMultimap.create();

        ResourceLocation modifierId = ResourceLocation.fromNamespaceAndPath("gear_forge", "lucky_diamond_luck");

        multimap.put(Attributes.LUCK,
                new AttributeModifier(
                        modifierId,
                        20.0,
                        AttributeModifier.Operation.ADD_VALUE
                ));

        return multimap;
    }
    
}