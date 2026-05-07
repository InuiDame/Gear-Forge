package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.datagen.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class ModEnchantmentEvents {

    private static ItemStack getWeapon(LivingEntity shooter) {
        ItemStack main = shooter.getMainHandItem();
        ItemStack off = shooter.getOffhandItem();

        if (main.getItem() instanceof net.minecraft.world.item.BowItem || main.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
            return main;
        }
        if (off.getItem() instanceof net.minecraft.world.item.BowItem || off.getItem() instanceof net.minecraft.world.item.CrossbowItem) {
            return off;
        }
        return ItemStack.EMPTY;
    }

    @SubscribeEvent
    public static void onArrowHit(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow) || arrow.level().isClientSide) return;
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) return;

        // 获取弓（由于箭已经射出，通常需要通过 EnchantmentHelper 获取射击时的附魔）
        // 1.21 推荐通过射出箭时保存附魔信息，或者简单地获取主手物品
        // 获取发射武器（弓或弩）
        ItemStack weapon = getWeapon(shooter);
        if (weapon.isEmpty()) return;

        var registry = arrow.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        // --- 闪电箭逻辑 ---
        int lightLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.LIGHTNING_ARROW), weapon);
        if (lightLvl > 0 && event.getRayTraceResult().getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(arrow.level());
            if (bolt != null) {
                bolt.moveTo(arrow.position());
                // 伤害通过下面 onDamage 增强，或者在此手动处理
                arrow.level().addFreshEntity(bolt);
            }
        }

        // --- 爆炸箭逻辑 (优化版) ---
        int expLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.EXPLOSIVE_ARROW), weapon);
        if (expLvl > 0) {
            Level level = arrow.level();
            double x = arrow.getX();
            double y = arrow.getY();
            double z = arrow.getZ();
            float radius = (float) expLvl;

            // 1. 触发实际爆炸（不破坏方块，不产生火焰）
            level.explode(arrow, x, y, z, radius, false, Level.ExplosionInteraction.NONE);

            // 2. 注入自定义粒子效果 (只在服务端执行，发送包给客户端)
            if (level instanceof ServerLevel serverLevel) {
                // 核心冲击波：等级越高，闪光范围越大
                serverLevel.sendParticles(ParticleTypes.FLASH, x, y + 0.5, z, 1 + expLvl, 0.2, 0.2, 0.2, 0.0);

                // 环绕烟雾：模拟不同威力的扩散
                int particleCount = 10 + (expLvl * 15); // 1级25个，5级85个
                serverLevel.sendParticles(ParticleTypes.EXPLOSION, x, y, z,
                        particleCount,
                        radius * 0.5, radius * 0.5, radius * 0.5, 0.1);

                // 可选：添加一些火花粒子增加视觉冲击力
                serverLevel.sendParticles(ParticleTypes.LAVA, x, y, z,
                        5 * expLvl, 0.1, 0.1, 0.1, 0.5);
            }

            // 3. 增强音效：根据等级改变音调，低等级清脆，高等级沉闷有力
            float pitch = 1.5f - (expLvl * 0.2f); // 1级1.3(清脆), 5级0.5(重低音)
            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(),
                    SoundSource.PLAYERS, 1.0f, pitch);

            arrow.discard();
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getDirectEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity shooter) {
            ItemStack weapon = getWeapon(shooter);
            if (weapon.isEmpty()) return;

            var registry = arrow.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
            LivingEntity victim = event.getEntity();

            // 1. 闪电伤害加成 (每级10%)
            int lightLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.LIGHTNING_ARROW), weapon);
            if (lightLvl > 0 && event.getSource().is(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT)) {
                event.setAmount(event.getAmount() * (1.0f + lightLvl * 0.1f));
            }

            // 2. 霜冻箭 (持续掉血+减速)
            int frostLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.FROST_ARROW), weapon);
            if (frostLvl > 0) {
                // 减速时间：每级+1秒 (初始应该是3s + lvl)
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, (3 + frostLvl) * 20, 1));
                // 掉血：由于无法直接通过附魔做持续伤害，这里给予一个中毒或自定义伤害逻辑
                // 每秒掉血 (0.5 + (lvl-1)*0.5)
                victim.hurt(victim.damageSources().magic(), (float)(0.5 * frostLvl));
            }

            // 3. 爆炸箭额外伤害 (1级5, 每级+2)
            int expLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.EXPLOSIVE_ARROW), weapon);
            if (expLvl > 0) {
                event.setAmount(5.0f + (expLvl - 1) * 2.0f);
            }

            // 4. 破甲箭 (每级穿透5%)
            int pierceLvl = EnchantmentHelper.getItemEnchantmentLevel(registry.getHolderOrThrow(ModEnchantments.ARMOR_PIERCING_ARROW), weapon);
            if (pierceLvl > 0) {
                float armorValue = victim.getArmorValue();
                if (armorValue > 0) {
                    // 1.21 很难直接修改伤害公式，通常计算一段额外的“无视护甲伤害”注入
                    float ignoreRatio = pierceLvl * 0.05f;
                    float extra = event.getAmount() * ignoreRatio;
                    victim.hurt(victim.damageSources().genericKill(), extra); // 使用 bypasses armor 的来源
                }
            }
        }
    }

    // --- Tooltip 描述展示 ---
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        var registry = event.getEntity().level().registryAccess().registry(Registries.ENCHANTMENT);
        if (registry.isEmpty()) return;

        addDesc(stack, registry.get(), ModEnchantments.LIGHTNING_ARROW, "enchantment.gear_forge.lightning_arrow.desc", event.getToolTip());
        addDesc(stack, registry.get(), ModEnchantments.FROST_ARROW, "enchantment.gear_forge.frost_arrow.desc", event.getToolTip());
        addDesc(stack, registry.get(), ModEnchantments.EXPLOSIVE_ARROW, "enchantment.gear_forge.explosive_arrow.desc", event.getToolTip());
        addDesc(stack, registry.get(), ModEnchantments.ARMOR_PIERCING_ARROW, "enchantment.gear_forge.armor_piercing_arrow.desc", event.getToolTip());
    }

    private static void addDesc(ItemStack stack, net.minecraft.core.Registry<Enchantment> reg, ResourceKey<Enchantment> key, String langKey, List<Component> tooltips) {
        if (EnchantmentHelper.getItemEnchantmentLevel(reg.getHolderOrThrow(key), stack) > 0) {
            tooltips.add(Component.translatable(langKey).withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }
}