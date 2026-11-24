package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.content.ModMobEffects;
import com.hana.gear_forge.item.ModItems;
import com.hana.gear_forge.util.SakuraHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * 樱花木体系运行时效果：
 * <ul>
 *   <li>樱花剑 / 镶樱花石的武器近战命中叠加印记，5 层触发樱花绽放（树苗瞬间长大 + 范围伤害）；</li>
 *   <li>樱花镐砍树力=斧头、把原木替换为樱花原木并掉圆石（低概率矿物）；樱花斧砍树速度翻倍；</li>
 *   <li>四件套给樱花之护佑 V；护甲上的樱花石给护佑 I，两者取最大值不叠加；护佑按等级减免魔法伤害。</li>
 * </ul>
 */
@EventBusSubscriber(modid = GearForgeMod.MODID)
public final class SakuraEvents {

    private static final double BLOSSOM_RADIUS = 3.5D;
    private static final float BLOSSOM_DAMAGE = 8.0F;
    private static final float ORE_CHANCE = 0.08F;
    private static final int BLESSING_REFRESH_TICKS = 60;

    private SakuraEvents() {}

    // ===================== 印记与绽放 =====================

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) return;

        DamageSource source = event.getSource();
        Entity attacker = source.getEntity();
        // 只有玩家主手近战攻击才打印记；远程/爆发伤害不叠层。
        if (!(attacker instanceof Player player)) return;
        if (source.getDirectEntity() != player) return;
        if (event.getNewDamage() <= 0) return;

        ItemStack weapon = player.getMainHandItem();
        if (!SakuraHelper.grantsMark(weapon)) return;

        LivingEntity target = event.getEntity();
        int marks = SakuraHelper.getMarks(target) + 1;
        if (marks >= SakuraHelper.MARK_TRIGGER) {
            SakuraHelper.setMarks(target, 0);
            if (target.level() instanceof ServerLevel serverLevel) {
                blossom(serverLevel, target, player);
            }
        } else {
            SakuraHelper.setMarks(target, marks);
        }
    }

    /** 目标处种一棵樱花树并瞬间长大，同时对周围实体造成范围伤害。 */
    private static void blossom(ServerLevel level, LivingEntity target, Player attacker) {
        BlockPos pos = target.blockPosition();
        BlockPos ground = pos;
        if (!level.getBlockState(ground).isAir() && !level.getBlockState(ground).canBeReplaced()) {
            ground = pos.above();
        }
        BlockState at = level.getBlockState(ground);
        if (at.isAir() || at.canBeReplaced()) {
            level.setBlock(ground, Blocks.CHERRY_SAPLING.defaultBlockState(), 3);
            if (Blocks.CHERRY_SAPLING instanceof BonemealableBlock bonemealable) {
                // SaplingBlock 的 performBonemeal 内部直接调用 TreeGrower，一次即长成整棵树。
                bonemealable.performBonemeal(level, level.getRandom(), ground, level.getBlockState(ground), net.minecraft.world.level.block.BonemealSource.INTERACTION);
            }
        }

        DamageSource dmg = attacker.damageSources().magic();
        AABB area = new AABB(pos).inflate(BLOSSOM_RADIUS);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area)) {
            if (victim == attacker) continue;
            victim.hurt(dmg, BLOSSOM_DAMAGE);
        }
    }

    // ===================== 魔法伤害减免 =====================

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Player player)) return;

        DamageSource source = event.getSource();
        boolean magic = source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
        if (!magic) return;

        int blessing = SakuraHelper.blessingLevel(player);
        if (blessing <= 0) return;

        float reduction = Math.min(blessing * 0.10F, 0.5F);
        event.setAmount(Math.max(0, event.getAmount() * (1.0F - reduction)));
    }

    // ===================== 四件套效果 =====================

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        if (SakuraHelper.hasFullSakuraArmor(player)) {
            player.addEffect(new MobEffectInstance(
                    ModMobEffects.SAKURA_BLESSING, BLESSING_REFRESH_TICKS, 4, true, true, true));
        } else if (player.getEffect(ModMobEffects.SAKURA_BLESSING) != null) {
            // 只移除套装来源的效果；镶樱花石护甲提供的护佑在 onIncomingDamage 里直接读取装备，
            // 不依赖该状态效果，因此这里移除不会影响宝石的 10%。
            player.removeEffect(ModMobEffects.SAKURA_BLESSING);
        }
    }

    // ===================== 砍树与挖掘 =====================

    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        ItemStack held = event.getEntity().getMainHandItem();
        BlockState state = event.getState();
        if (!state.is(net.minecraft.tags.BlockTags.LOGS)) return;

        if (held.is(ModItems.SAKURA_AXE.get())) {
            event.setNewSpeed(event.getOriginalSpeed() * 2.0F);
        } else if (held.is(ModItems.SAKURA_PICKAXE.get())) {
            // 樱花镐砍树力=斧头：直接用下界合金斧对该原木的破坏速度。
            float axeSpeed = new ItemStack(Items.NETHERITE_AXE).getDestroySpeed(state);
            event.setNewSpeed(Math.max(event.getOriginalSpeed(), axeSpeed));
        }
    }

    /**
     * 樱花锄右键对可施肥方块（作物、树苗、草等）自动施肥，消耗 1 点耐久。
     * 26.3 起所有物品都是 Item、没有 HoeItem 子类，因此施肥逻辑放在事件里。
     */
    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        if (!held.is(ModItems.SAKURA_HOE.get())) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock bonemealable)) return;
        if (!bonemealable.isValidBonemealTarget(level, pos, state, net.minecraft.world.level.block.BonemealSource.INTERACTION)) return;

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                && bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state, net.minecraft.world.level.block.BonemealSource.INTERACTION)) {
            bonemealable.performBonemeal(serverLevel, level.getRandom(), pos, state, net.minecraft.world.level.block.BonemealSource.INTERACTION);
            held.hurtAndBreak(1, player,
                    event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                            ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                            : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        Entity breaker = event.getBreaker();
        if (!(breaker instanceof Player player)) return;
        if (!player.getMainHandItem().is(ModItems.SAKURA_PICKAXE.get())) return;

        BlockState state = event.getState();
        if (!state.is(net.minecraft.tags.BlockTags.LOGS)) return;

        ServerLevel level = event.getLevel();
        BlockPos pos = event.getPos();

        // 去掉原木掉落，改为圆石（低概率矿物）。
        event.getDrops().clear();
        Block.popResource(level, pos, new ItemStack(Items.COBBLESTONE));
        if (level.getRandom().nextFloat() < ORE_CHANCE) {
            List<net.minecraft.world.item.Item> ores = List.of(
                    Items.IRON_ORE, Items.GOLD_ORE, Items.DIAMOND_ORE,
                    Items.EMERALD_ORE, Items.REDSTONE_ORE);
            Block.popResource(level, pos,
                    new ItemStack(ores.get(level.getRandom().nextInt(ores.size()))));
        }

        // 把挖掉的原木位置替换成樱花原木，保留轴向。
        // BlockDropsEvent 触发时原方块已被移除，此处直接放置即可。
        BlockState cherry = Blocks.CHERRY_LOG.defaultBlockState()
                .setValue(BlockStateProperties.AXIS, state.getValue(BlockStateProperties.AXIS));
        level.setBlock(pos, cherry, 3);
    }
}
