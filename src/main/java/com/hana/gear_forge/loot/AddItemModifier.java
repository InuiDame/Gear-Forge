package com.hana.gear_forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * 在指定战利品表里按概率追加一件物品。
 *
 * <p>概率与战利品表 ID 故意放在本类自己的字段里、在 {@link #doApply} 中判断，
 * 而不是用 GLM 的 {@code conditions} 数组。原因：NeoForge 在 1.21.1（{@code condition[]}）
 * 与 26.3（{@code Optional<Holder<condition>>} + {@code priority}）之间改了条件 codec，
 * 旧版 JSON 的 {@code "conditions": [...]} 在高版本会被静默忽略、变成“无条件触发”，
 * 结果就是杀怪/破坏方块都掉饰品。把判断收进代码后两版行为完全一致，也不再依赖
 * 数据包条件格式。
 */
public class AddItemModifier extends LootModifier {
    private final Item item;
    private final float chance;
    private final ResourceLocation lootTableId;

    public AddItemModifier(Item item, float chance, ResourceLocation lootTableId) {
        super(new LootItemCondition[0]);
        this.item = item;
        this.chance = chance;
        this.lootTableId = lootTableId;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        // 只在目标战利品表、且概率命中时追加。两个条件缺一不可。
        if (!lootTableId.equals(context.getQueriedLootTableId())) return generatedLoot;
        if (chance <= 0.0F || context.getRandom().nextFloat() >= chance) return generatedLoot;
        generatedLoot.add(new ItemStack(item));
        return generatedLoot;
    }

    /** 仅三个自有字段，不接 codecStart，避免依赖版本相关的条件 codec。 */
    public static final MapCodec<AddItemModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(m -> m.item),
            Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance),
            Codec.STRING.fieldOf("loot_table_id")
                    .xmap(ResourceLocation::parse, ResourceLocation::toString)
                    .forGetter(m -> m.lootTableId)
    ).apply(inst, AddItemModifier::new));

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
