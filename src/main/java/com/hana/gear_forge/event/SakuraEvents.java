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
            // 每次命中给目标周围撒一圈樱花粒子，让"印记在叠加"有可见反馈。
            if (target.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
                        6 + marks * 2, 0.3, 0.3, 0.3, 0.02);
            }
        }
    }

    /**
     * 目标处瞬间长出一棵樱花树，并对周围实体造成范围伤害。
     *
     * <p>不再依赖树苗 + 骨粉催熟（树苗落在实体脚下的悬空位置时长不出来），改为直接放置
     * 树干与树冠，保证"瞬间长大"在任何地形都可见。同时撒大量樱花飘落粒子。
     */
    private static void blossom(ServerLevel level, LivingEntity target, Player attacker) {
        BlockPos center = target.blockPosition();

        // 从目标位置向下找第一个实心地面（最多 8 格），在它上方生成树。
        BlockPos ground = center.below();
        for (int i = 0; i < 8; i++) {
            BlockState s = level.getBlockState(ground);
            if (!s.isAir()) break;
            ground = ground.below();
        }
        BlockPos base = ground.above();

        // 树干：5 格樱花原木 + 顶部 5×5×2 的樱花树叶树冠。
        for (int i = 0; i < 5; i++) {
            level.setBlock(base.above(i), Blocks.CHERRY_LOG.defaultBlockState(), 3);
        }
        BlockPos crown = base.above(4);
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) continue; // 削掉四角，树冠更圆
                    BlockPos leaf = crown.offset(dx, dy, dz);
                    if (level.getBlockState(leaf).isAir()) {
                        level.setBlock(leaf, Blocks.CHERRY_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }

        // 樱花飘落粒子：树冠处一大团 + 目标处一小团。
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                crown.getX() + 0.5, crown.getY() + 1.0, crown.getZ() + 0.5,
                90, 2.5, 1.5, 2.5, 0.05);
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.CHERRY_LEAVES,
                target.getX(), target.getY() + 1.0, target.getZ(),
                40, 1.0, 1.0, 1.0, 0.08);

        // 范围伤害：对目标周围（含目标）除攻击者外的实体造成魔法伤害。
        DamageSource dmg = attacker.damageSources().magic();
        AABB area = new AABB(center).inflate(BLOSSOM_RADIUS);
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
     * 樱花锄右键对可施肥方块自动施肥，消耗 1 点耐久。
     * 26.3 起所有物品都是 Item、没有 HoeItem 子类，施肥逻辑放在事件里。
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
        var source = net.minecraft.world.level.block.BonemealSource.INTERACTION;
        if (!bonemealable.isValidBonemealTarget(level, pos, state, source)) return;

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                && bonemealable.isBonemealSuccess(level, level.getRandom(), pos, state, source)) {
            bonemealable.performBonemeal(serverLevel, level.getRandom(), pos, state, source);
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

        // 原木正常掉落（保留原版掉落，玩家能砍倒树），额外掉樱花原木与圆石。
        // 只有樱花镐挖原木才触发，樱花斧等其它工具走原版规则。
        Block.popResource(level, pos, new ItemStack(Items.CHERRY_LOG));
        Block.popResource(level, pos, new ItemStack(Items.COBBLESTONE));
        if (level.getRandom().nextFloat() < ORE_CHANCE) {
            List<net.minecraft.world.item.Item> ores = List.of(
                    Items.IRON_ORE, Items.GOLD_ORE, Items.DIAMOND_ORE,
                    Items.EMERALD_ORE, Items.REDSTONE_ORE);
            Block.popResource(level, pos,
                    new ItemStack(ores.get(level.getRandom().nextInt(ores.size()))));
        }
    }
}
