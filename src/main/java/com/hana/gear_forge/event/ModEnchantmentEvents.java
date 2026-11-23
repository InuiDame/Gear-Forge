package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.ModEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 7 个数据驱动附魔的行为实现。
 *
 * <p>这些附魔在 {@code data/gear_forge/enchantment/*.json} 里只声明“能给谁、最高几级”，
 * 效果全部写在这里。
 *
 * <p>两个关键设计约束：
 * <ul>
 *   <li><b>不取消原版方块破坏</b>。旧实现在 {@code BlockEvent.BreakEvent} 里取消事件再手动
 *       {@code setBlockAndUpdate(AIR)}，跳过了 {@code playerWillDestroy}（潜影盒内容）、
 *       {@code mineBlock}（工具耐久）、{@code playerDestroy}（战利品表/方块经验/其它 mod 的 GLM），
 *       并且能绕过保护类 mod 的取消。现在改为在 {@link BlockDropsEvent} 里<b>追加/替换掉落</b>，
 *       原版流程完整保留。</li>
 *   <li><b>不在伤害事件里嵌套 {@code hurt()}</b>。在 {@link LivingIncomingDamageEvent} 内部再开一次
 *       伤害序列会占用无敌帧、把外层箭伤抵消掉，还会让创造模式被扣血。现在只用
 *       {@code setAmount} / {@code addReductionModifier} 在原地修改伤害。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public class ModEnchantmentEvents {

    /** 闪电箭在雷暴天气下的额外倍率。 */
    private static final float STORM_BONUS = 1.15F;

    // ===================== 箭矢类 =====================

    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide()) return;
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;
        if (event.getRayTraceResult().getType() == net.minecraft.world.phys.HitResult.Type.MISS) return;

        ItemStack weapon = resolveLauncher(arrow, shooter);
        if (weapon.isEmpty()) return;

        Level level = arrow.level();

        // 闪电箭：在命中点召唤闪电（伤害由原版闪电结算，本类不再重复加成）。
        int lightning = getEnchantLevel(weapon, ModEnchantments.LIGHTNING_ARROW, level);
        if (lightning > 0) {
            // 26.3：闪电实体常量移到 EntityTypes，create 需要 EntitySpawnReason；
            // Entity#moveTo(Vec3) 被移除，定位改用 snapTo(Vec3)。
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.EVENT);
            if (bolt != null) {
                bolt.snapTo(arrow.position());
                if (shooter instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    bolt.setCause(serverPlayer);
                }
                level.addFreshEntity(bolt);
            }
        }

        // 爆炸箭：不破坏方块、不点燃，随后本箭本身被移除（伤害在下方 IncomingDamage 里算）。
        int explosive = getEnchantLevel(weapon, ModEnchantments.EXPLOSIVE_ARROW, level);
        if (explosive > 0) {
            double x = arrow.getX();
            double y = arrow.getY();
            double z = arrow.getZ();
            float radius = Math.min(0.5F + explosive * 0.5F, 4.0F);
            level.explode(arrow, x, y, z, radius, false, Level.ExplosionInteraction.NONE);
            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.0F, Math.max(0.5F, 1.5F - explosive * 0.2F));
            arrow.discard();
        }
    }

    @SubscribeEvent
    public static void onArrowDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;
        if (event.getEntity().level().isClientSide()) return;

        ItemStack weapon = resolveLauncher(arrow, shooter);
        if (weapon.isEmpty()) return;

        Level level = event.getEntity().level();
        LivingEntity victim = event.getEntity();
        float amount = event.getAmount();

        // 霜冻箭：额外寒冰伤害 + 减速，全部就地叠加，不再嵌套 hurt()。
        int frost = getEnchantLevel(weapon, ModEnchantments.FROST_ARROW, level);
        if (frost > 0) {
            amount += 0.5F * frost;
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                    (3 + frost) * 20, Math.min(frost - 1, 4)));
        }

        // 爆炸箭：额外伤害与等级挂钩，且是“加”不是“覆盖”（旧实现会把强力弓的伤害抹成 5~13）。
        int explosive = getEnchantLevel(weapon, ModEnchantments.EXPLOSIVE_ARROW, level);
        if (explosive > 0) {
            amount += 5.0F + (explosive - 1) * 2.0F;
        }

        event.setAmount(amount);

        // 破甲箭：按等级削弱目标的护甲减免，而不是再造成一次无视护甲的伤害。
        // 用 Reduction.ARMOR 的修饰器实现，创造模式/图腾/无敌帧全部按原版规则走。
        int piercing = getEnchantLevel(weapon, ModEnchantments.ARMOR_PIERCING_ARROW, level);
        if (piercing > 0) {
            float ignoreRatio = Math.min(piercing * 0.05F, 0.5F);
            event.addReductionModifier(DamageContainer.Reduction.ARMOR,
                    (container, reduction) -> reduction * (1.0F - ignoreRatio));
        }
    }

    /**
     * 射出这支箭的弓/弩。优先使用箭上记录的发射武器，
     * 这样“射出后换手/换武器”不会改变附魔判定。
     */
    private static ItemStack resolveLauncher(AbstractArrow arrow, LivingEntity shooter) {
        ItemStack fired = arrow.getWeaponItem();
        if (fired != null && !fired.isEmpty()) return fired;
        ItemStack main = shooter.getMainHandItem();
        if (main.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem) return main;
        ItemStack off = shooter.getOffhandItem();
        if (off.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem) return off;
        return ItemStack.EMPTY;
    }

    /**
     * 读取附魔等级。附魔是数据驱动的：数据包删掉/改名某条附魔后，
     * {@code getHolderOrThrow} 会在热路径（每次箭命中、每次破坏方块、每次 tooltip）抛异常并刷屏。
     */
    private static int getEnchantLevel(ItemStack stack, net.minecraft.resources.ResourceKey<Enchantment> key,
                                       Level level) {
        if (stack.isEmpty()) return 0;
        // 26.3：RegistryAccess#registryOrThrow 改名为 lookupOrThrow；
        // Registry#getHolder(ResourceKey) 改为继承的 HolderLookup.RegistryLookup#get(ResourceKey)。
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(key)
                .map(holder -> stack.getEnchantmentLevel(holder))
                .orElse(0);
    }

    // ===================== 挖掘类（BlockDropsEvent） =====================

    /**
     * 挖掘掉落统一入口：点金、挖矿暴击、矿石猎手、丰收暴击都在这里结算。
     *
     * <p>过去这些效果分散在两个 {@link BlockDropsEvent} 监听器里，彼此都调用
     * {@code getDrops().clear()}：点金把石头掉落换成金锭，矿石猎手又换成矿石，
     * 后执行的一个会把前一个的结果覆盖掉，暴击复制到的也可能是空列表。现在合并为
     * 单一监听器、固定结算顺序，所有效果按“叠加、不互相吞”处理：
     * <ol>
     *   <li>矿石猎手先判定，把石头的基础掉落替换成随机矿石；</li>
     *   <li>点金在此基础上<b>追加</b>金锭（N 级 N 个），不清空已有掉落；</li>
     *   <li>挖矿暴击复制当前全部掉落一份；</li>
     *   <li>丰收暴击对成熟作物追加产物。</li>
     * </ol>
     * 这样一个石头可以同时掉随机矿石 + N 个金锭，暴击再复制一份，效果互不覆盖。
     */
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)) return;
        ItemStack tool = event.getTool();
        if (tool.isEmpty()) return;

        ServerLevel level = event.getLevel();
        BlockState state = event.getState();

        // 矿石猎手：把“石头”的掉落替换为随机矿石。只在原版石头（无 BlockEntity）上生效。
        int oreHunter = getEnchantLevel(tool, ModEnchantments.ORE_HUNTER, level);
        if (oreHunter > 0 && state.is(Blocks.STONE) && event.getBlockEntity() == null
                && player.getRandom().nextFloat() < oreHunter * 0.06F) {
            event.getDrops().clear();
            event.getDrops().add(newItemEntity(level, event, getRandomOre(level)));
        }

        // 点金：石头/圆石/深板岩类掉落按等级追加金锭（N 级 N 个）。
        // 用 append 而不是 clear+replace，才能和矿石猎手/挖矿暴击叠加。
        if (isStone(state) && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            int midasLevel = MidasTouchHandler.currentLevel(serverPlayer);
            if (midasLevel > 0) {
                var pos = event.getPos();
                event.getDrops().add(new ItemEntity(level,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                        new ItemStack(Items.GOLD_INGOT, midasLevel)));
            }
        }

        // 挖矿暴击：额外复制一份“当前”全部掉落（已含矿石猎手/点金的结果）。
        int miningCrit = getEnchantLevel(tool, ModEnchantments.MINING_CRIT, level);
        if (miningCrit > 0 && player.getRandom().nextFloat() < miningCrit * 0.08F) {
            List<ItemStack> copies = new ArrayList<>();
            for (ItemEntity drop : event.getDrops()) {
                copies.add(drop.getItem().copy());
            }
            spawnAt(level, event, copies);
        }

        // 丰收暴击：只对“已成熟”的作物追加产物，未成熟时绝不触发（否则可以站桩刷作物）。
        int harvestBounty = getEnchantLevel(tool, ModEnchantments.HARVEST_BOUNTY, level);
        if (harvestBounty > 0 && isMatureCrop(state)
                && player.getRandom().nextFloat() < harvestBounty * 0.12F) {
            ItemStack bonus = getCropProduce(state.getBlock());
            if (!bonus.isEmpty()) {
                bonus.setCount(1 + player.getRandom().nextInt(harvestBounty));
                event.getDrops().add(newItemEntity(level, event, bonus));
            }
        }
    }

    /** 点金适用的石头类方块。 */
    private static boolean isStone(BlockState state) {
        return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
    }

    private static void spawnAt(ServerLevel level, BlockDropsEvent event, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                event.getDrops().add(newItemEntity(level, event, stack));
            }
        }
    }

    private static ItemEntity newItemEntity(ServerLevel level, BlockDropsEvent event, ItemStack stack) {
        var pos = event.getPos();
        return new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    /** 作物是否已经成熟。未成熟时丰收附魔必须完全不生效。 */
    private static boolean isMatureCrop(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) return crop.isMaxAge(state);
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        return false;
    }

    private static ItemStack getRandomOre(ServerLevel level) {
        record WeightedOre(ItemStack stack, int weight) {}
        List<WeightedOre> ores = List.of(
                new WeightedOre(new ItemStack(Items.COAL, 1 + level.getRandom().nextInt(2)), 30),
                new WeightedOre(new ItemStack(Items.COPPER_ORE, 1), 20),
                new WeightedOre(new ItemStack(Items.IRON_ORE, 1), 20),
                new WeightedOre(new ItemStack(Items.GOLD_ORE, 1), 10),
                new WeightedOre(new ItemStack(Items.LAPIS_LAZULI, 2 + level.getRandom().nextInt(4)), 8),
                new WeightedOre(new ItemStack(Items.REDSTONE, 2 + level.getRandom().nextInt(3)), 7),
                new WeightedOre(new ItemStack(Items.DIAMOND, 1), 3),
                new WeightedOre(new ItemStack(Items.EMERALD, 1), 2)
        );
        int totalWeight = ores.stream().mapToInt(WeightedOre::weight).sum();
        int roll = level.getRandom().nextInt(totalWeight);
        int cumulative = 0;
        for (WeightedOre ore : ores) {
            cumulative += ore.weight;
            if (roll < cumulative) return ore.stack;
        }
        return new ItemStack(Items.COAL);
    }

    private static ItemStack getCropProduce(Block block) {
        if (block == Blocks.WHEAT) return new ItemStack(Items.WHEAT);
        if (block == Blocks.CARROTS) return new ItemStack(Items.CARROT);
        if (block == Blocks.POTATOES) return new ItemStack(Items.POTATO);
        if (block == Blocks.BEETROOTS) return new ItemStack(Items.BEETROOT);
        if (block == Blocks.NETHER_WART) return new ItemStack(Items.NETHER_WART);
        if (block == Blocks.COCOA) return new ItemStack(Items.COCOA_BEANS);
        return ItemStack.EMPTY;
    }
}
