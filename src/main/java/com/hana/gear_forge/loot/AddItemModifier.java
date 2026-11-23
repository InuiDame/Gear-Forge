package com.hana.gear_forge.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import java.util.Optional;

/**
 * 在指定战利品表里按概率追加一件物品。
 *
 * <p>概率与战利品表 ID 故意放在本类自己的字段里、在 {@link #doApply} 中判断，
 * 而不是用 GLM 的条件系统。原因：NeoForge 26.3 把条件的 codec 从 1.21.1 的
 * {@code condition[]} 改成了 {@code Optional<Holder<condition>> + priority}，
 * 旧 JSON 的 {@code "conditions": [...]} 会被静默忽略 → 条件为空 → 无条件触发，
 * 表现就是杀怪/破坏方块都掉幸运钻石/魔性铁砧。判断收进代码后不再依赖数据包条件格式。
 */
public class AddItemModifier extends LootModifier {
    private final Item item;
    private final float chance;
    private final Identifier lootTableId;

    public AddItemModifier(Item item, float chance, Identifier lootTableId) {
        super(Optional.empty(), 0);
        this.item = item;
        this.chance = chance;
        this.lootTableId = lootTableId;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
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
                    .xmap(Identifier::parse, Identifier::toString)
                    .forGetter(m -> m.lootTableId)
    ).apply(inst, AddItemModifier::new));

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
