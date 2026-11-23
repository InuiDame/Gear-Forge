package com.hana.gear_forge.command;

import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.event.MobGearEventHandler;
import com.hana.gear_forge.util.GearHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class GearForgeCommands {

    /** 单次命令最多生成的实体数。没有上限时 /gear_forge test spawnmob zombie 100000 会在同一 tick 卡死服务器。 */
    private static final int MAX_SPAWN_COUNT = 64;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gear_forge")
                .requires(source -> ((LevelBasedPermissionSet) source.permissions()).level().id() >= 2)
                .then(Commands.literal("test")
                        .then(Commands.literal("spawnmob")
                                // 用 string() 而不是 word()：word() 的 readUnquotedString 不允许冒号，
                                // 导致 /gear_forge test spawnmob gear_forge:xxx 无法输入，
                                // 只有省略命名空间的 minecraft 实体能写。
                                .then(Commands.argument("entityType", StringArgumentType.string())
                                        .executes(ctx -> execute(ctx, 1, 1, "", ""))
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, MAX_SPAWN_COUNT))
                                                .executes(ctx -> execute(ctx,
                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                        1, "", ""))
                                                .then(Commands.argument("gearCount", IntegerArgumentType.integer(1, 6))
                                                        .executes(ctx -> execute(ctx,
                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                IntegerArgumentType.getInteger(ctx, "gearCount"),
                                                                "", ""))
                                                        .then(Commands.argument("upgradeLevels", StringArgumentType.string())
                                                                .executes(ctx -> execute(ctx,
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        IntegerArgumentType.getInteger(ctx, "gearCount"),
                                                                        StringArgumentType.getString(ctx, "upgradeLevels"),
                                                                        ""))
                                                                .then(Commands.argument("affixCounts", StringArgumentType.string())
                                                                        .executes(ctx -> execute(ctx,
                                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                                IntegerArgumentType.getInteger(ctx, "gearCount"),
                                                                                StringArgumentType.getString(ctx, "upgradeLevels"),
                                                                                StringArgumentType.getString(ctx, "affixCounts")))))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> ctx,
                               int count, int gearCount,
                               String upgradeLevelsStr, String affixCountsStr)
            throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        RandomSource random = level.getRandom();

        String entityTypeStr = StringArgumentType.getString(ctx, "entityType");
        Identifier entityTypeId = Identifier.tryParse(entityTypeStr);
        if (entityTypeId == null) {
            source.sendFailure(Component.literal("无效的实体类型: " + entityTypeStr));
            return 0;
        }
        Optional<EntityType<?>> entityTypeOpt = BuiltInRegistries.ENTITY_TYPE.getOptional(entityTypeId);
        if (entityTypeOpt.isEmpty()) {
            source.sendFailure(Component.literal("未知实体类型: " + entityTypeStr));
            return 0;
        }
        EntityType<?> entityType = entityTypeOpt.get();

        // 解析强化等级和词条数量，格式不合法直接报错
        Map<Integer, Integer> upgradeMap;
        Map<Integer, Integer> affixMap;
        try {
            upgradeMap = parseSlotValueMap(upgradeLevelsStr, gearCount, "强化等级");
            affixMap = parseSlotValueMap(affixCountsStr, gearCount, "词条数量");
        } catch (IllegalArgumentException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }

        // 如果指定了参数但解析为空（例如空字符串），视为合法（全部默认0）
        for (int i = 0; i < count; i++) {
            var entity = entityType.create(level, EntitySpawnReason.COMMAND);
            if (entity == null) continue;

            if (entity instanceof LivingEntity livingEntity) {
                List<EquipmentSlot> slots = getEquipmentSlots(gearCount);
                for (int slotIndex = 0; slotIndex < gearCount; slotIndex++) {
                    EquipmentSlot slot = slots.get(slotIndex);
                    ItemStack gear = getDefaultGearForSlot(slot);
                    if (gear.isEmpty()) continue;

                    int upgradeLvl = upgradeMap.getOrDefault(slotIndex + 1, 0);
                    int affixCount = affixMap.getOrDefault(slotIndex + 1, 0);

                    if (upgradeLvl > 0) {
                        gear.set(ModDataComponents.UPGRADE_LEVEL.get(), upgradeLvl);
                    }
                    if (affixCount > 0) {
                        List<ModDataComponents.AffixEntry> affixes = GearHelper.rollAffixes(gear, random);
                        if (affixes.size() > affixCount) {
                            affixes = new ArrayList<>(affixes.subList(0, affixCount));
                        }
                        gear.set(ModDataComponents.AFFIXES.get(), affixes);
                    }

                    livingEntity.setItemSlot(slot, gear);
                }

                // 测试命令自己指定了装备，标记为已处理，避免加入世界时被随机怪物装备逻辑覆盖
                livingEntity.getPersistentData().putBoolean(MobGearEventHandler.GEARED_TAG, true);
                livingEntity.setPos(source.getPosition());
                level.addFreshEntity(livingEntity);
            }
        }

        source.sendSuccess(() -> Component.literal(
                "已生成 " + count + " 个 " + entityTypeStr + "，装备数: " + gearCount +
                        "，强化参数: " + upgradeLevelsStr + "，词条参数: " + affixCountsStr), true);
        return count;
    }

    /**
     * 解析索引:值的配对，例如 "1:3,2:7" 或单个数字 "5" 应用于所有槽位
     * @param input 输入字符串
     * @param maxSlots 最大槽位数
     * @param label 用于错误提示（“强化等级” / “词条数量”）
     * @return 槽位索引(1-based) -> 值
     * @throws IllegalArgumentException 如果格式不正确
     */
    private static Map<Integer, Integer> parseSlotValueMap(String input, int maxSlots, String label) {
        Map<Integer, Integer> map = new HashMap<>();
        if (input == null || input.isBlank()) return map;

        // 尝试解析为单个整数，应用于所有槽位
        try {
            int value = Integer.parseInt(input.trim());
            for (int i = 1; i <= maxSlots; i++) {
                map.put(i, value);
            }
            return map;
        } catch (NumberFormatException ignored) {
        }

        // 按逗号分割
        String[] pairs = input.split(",");
        for (String pair : pairs) {
            pair = pair.trim();
            if (pair.isEmpty()) continue;
            String[] parts = pair.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("格式错误：'" + pair + "' 应为 '索引:值'。正确示例：1:3,2:5");
            }
            try {
                int index = Integer.parseInt(parts[0].trim());
                int value = Integer.parseInt(parts[1].trim());
                if (index < 1 || index > maxSlots) {
                    throw new IllegalArgumentException("索引 " + index + " 超出范围 (1-" + maxSlots + ")");
                }
                map.put(index, value);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("'" + pair + "' 中包含非数字字符");
            }
        }
        if (map.isEmpty()) {
            throw new IllegalArgumentException(label + "参数不能解析出有效值。格式：'值' 或 '索引:值,...'，例如 '5' 或 '1:3,2:7'");
        }
        return map;
    }

    private static List<EquipmentSlot> getEquipmentSlots(int count) {
        List<EquipmentSlot> all = List.of(
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND,
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        );
        return all.subList(0, Math.min(count, all.size()));
    }

    private static ItemStack getDefaultGearForSlot(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> new ItemStack(Items.DIAMOND_SWORD);
            case OFFHAND -> new ItemStack(Items.SHIELD);
            case HEAD -> new ItemStack(Items.DIAMOND_HELMET);
            case CHEST -> new ItemStack(Items.DIAMOND_CHESTPLATE);
            case LEGS -> new ItemStack(Items.DIAMOND_LEGGINGS);
            case FEET -> new ItemStack(Items.DIAMOND_BOOTS);
            default -> ItemStack.EMPTY;
        };
    }
}