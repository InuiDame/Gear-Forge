package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.AffixConfig.AffixDefinition;
import com.hana.gear_forge.config.GemConfig;
import com.hana.gear_forge.config.GemConfig.GemDefinition;
import com.hana.gear_forge.config.GemConfig.GemEffect;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = GearForgeMod.MODID)
public class GearEventHandlers {

    private static String fmt(double value) {
        return new DecimalFormat("#.##").format(value);
    }

    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;

        var source = event.getSource();
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity livingAttacker)) return;

        // Handle ranged weapon (bow/crossbow) projectile damage
        if (source.getDirectEntity() instanceof AbstractArrow arrow) {
            handleRangedProjectileDamage(event, livingAttacker, arrow);
            return;
        }

        float damage = event.getNewDamage();
        ItemStack weapon = resolveActiveWeapon(livingAttacker);
        if (weapon.isEmpty()) return;

        List<ModDataComponents.AffixEntry> affixes = weapon.get(ModDataComponents.AFFIXES.get());
        List<ModDataComponents.GemsEntry> gems = weapon.get(ModDataComponents.GEMS.get());
        List<ModDataComponents.StoredBonus> stored = weapon.get(ModDataComponents.STORED_BONUSES.get());

        // 注意：强化等级的攻击力、转生保留的 attack_power / upgrade_attack_damage 已经通过
        // onItemAttributeModifiers 挂成 ATTACK_DAMAGE 属性，原版伤害里已经包含，这里不能再加一次。

        if (affixes != null) {
            double critChance = GearHelper.getAffixTotalValue(affixes, "crit_chance");
            double critDamage = GearHelper.getAffixTotalValue(affixes, "crit_damage");
            if (critChance > 0 && target.getRandom().nextDouble() < critChance) {
                damage *= (1.0 + critDamage);
            }

            double fireDamage = GearHelper.getAffixTotalValue(affixes, "fire_damage");
            damage += fireDamage;

            double frostDamage = GearHelper.getAffixTotalValue(affixes, "frost_damage");
            damage += frostDamage;

            double enchantBoost = GearHelper.getAffixTotalValue(affixes, "enchantment_boost");
            damage += enchantBoost;

            double lightningDamage = GearHelper.getAffixTotalValue(affixes, "lightning_damage");
            damage += lightningDamage;

            double armorPen = GearHelper.getAffixTotalValue(affixes, "armor_penetration");
            if (armorPen > 0 && target instanceof LivingEntity livingTarget) {
                double currentArmor = livingTarget.getAttributeValue(Attributes.ARMOR);
                double reducedArmor = Math.max(0, currentArmor - armorPen);
                double armorReduction = (currentArmor - reducedArmor) / Math.max(currentArmor, 1.0);
                damage *= (1.0f + (float) armorReduction * 0.5f);
            }
        }

        if (gems != null) {
            double gemFire = 0;
            double gemFrost = 0;
            for (ModDataComponents.GemsEntry gem : gems) {
                // attack_damage / all_stats 已经挂成 ATTACK_DAMAGE / ARMOR 属性，这里不再重复加伤。
                gemFire += GearHelper.getGemStatBonus(gem.id(), "fire_damage", gem.tier());
                gemFrost += GearHelper.getGemStatBonus(gem.id(), "frost_damage", gem.tier());
                double gemEnchantBoost = GearHelper.getGemStatBonus(gem.id(), "enchant_boost", gem.tier());
                damage += (float) gemEnchantBoost;
            }
            damage += gemFire;
            if (gemFire > 0) {
                target.setRemainingFireTicks((int) (gemFire * 30));
            }
            damage += gemFrost;
            if (gemFrost > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, slownessAmplifier(gemFrost, 2)));
            }
        }

        // Stored bonuses from reincarnation。
        // attack_damage / all_stats / defense 已走属性，这里只处理“附加伤害与状态”类。
        if (stored != null) {
            damage += (float) GearHelper.getStoredBonusValue(stored, "fire_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "frost_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "enchantment_boost");
            damage += (float) GearHelper.getStoredBonusValue(stored, "lightning_damage");

            double storedCritChance = GearHelper.getStoredBonusValue(stored, "crit_chance");
            double storedCritDamage = GearHelper.getStoredBonusValue(stored, "crit_damage");
            if (storedCritChance > 0 && target.getRandom().nextDouble() < storedCritChance) {
                damage *= (1.0 + storedCritDamage);
            }

            double storedFire = GearHelper.getStoredBonusValue(stored, "fire_damage");
            if (storedFire > 0) {
                target.setRemainingFireTicks((int) (storedFire * 40));
            }
            double storedFrost = GearHelper.getStoredBonusValue(stored, "frost_damage");
            if (storedFrost > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, slownessAmplifier(storedFrost, 3)));
            }
            double storedLightning = GearHelper.getStoredBonusValue(stored, "lightning_damage");
            if (storedLightning > 0 && target.getRandom().nextDouble() < 0.25) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                if (target.level().isThundering()) {
                    damage *= 1.15f;
                }
            }
        }

        if (affixes != null) {
            // 雷暴加成必须在 setNewDamage 之前算，否则改的只是局部变量
            double lightningValue = GearHelper.getAffixTotalValue(affixes, "lightning_damage");
            if (lightningValue > 0 && target.getRandom().nextDouble() < 0.25) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
                if (target.level().isThundering()) {
                    damage *= 1.15f;
                }
            }
        }

        event.setNewDamage(damage);

        if (affixes != null) {
            double lifeSteal = GearHelper.getAffixTotalValue(affixes, "life_steal");
            if (lifeSteal > 0 && livingAttacker instanceof Player player) {
                player.heal(damage * (float) lifeSteal);
            }

            double fireDuration = GearHelper.getAffixTotalValue(affixes, "fire_damage");
            if (fireDuration > 0) {
                target.setRemainingFireTicks((int) (fireDuration * 40));
            }

            double frostValue = GearHelper.getAffixTotalValue(affixes, "frost_damage");
            if (frostValue > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, slownessAmplifier(frostValue, 3)));
            }
        }

        if (stored != null) {
            double storedLifeSteal = GearHelper.getStoredBonusValue(stored, "life_steal");
            if (storedLifeSteal > 0 && livingAttacker instanceof Player player) {
                player.heal(damage * (float) storedLifeSteal);
            }
        }
    }

    private static ItemStack resolveActiveWeapon(LivingEntity entity) {
        ItemStack mainHand = entity.getMainHandItem();
        ItemStack offHand = entity.getOffhandItem();

        if (!mainHand.isEmpty() && !Boolean.TRUE.equals(mainHand.get(ModDataComponents.OFFHAND_MODE.get()))) {
            return mainHand;
        }
        if (!offHand.isEmpty() && Boolean.TRUE.equals(offHand.get(ModDataComponents.OFFHAND_MODE.get()))) {
            return offHand;
        }
        return ItemStack.EMPTY;
    }

    private static int slownessAmplifier(double value, int max) {
        return Math.max(0, Math.min((int) value - 1, max));
    }

    /** 射出这支箭的弓/弩。优先用箭记录的发射武器，射出后切换手持物品不会影响结果。 */
    public static ItemStack resolveLauncher(AbstractArrow arrow, LivingEntity shooter) {
        ItemStack fired = arrow.getWeaponItem();
        if (fired != null && (fired.is(Items.BOW) || fired.is(Items.CROSSBOW))) {
            return fired;
        }
        ItemStack main = shooter.getMainHandItem();
        if (main.is(Items.BOW) || main.is(Items.CROSSBOW)) return main;
        ItemStack off = shooter.getOffhandItem();
        if (off.is(Items.BOW) || off.is(Items.CROSSBOW)) return off;
        return ItemStack.EMPTY;
    }

    private static void handleRangedProjectileDamage(LivingDamageEvent.Pre event, LivingEntity attacker, AbstractArrow arrow) {
        ItemStack bow = resolveLauncher(arrow, attacker);
        if (bow.isEmpty()) return;

        float damage = event.getNewDamage();

        List<ModDataComponents.GemsEntry> gems = bow.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            damage += (float) GearHelper.getTotalGemEffectValue(gems, "projectile_damage");
        }

        List<ModDataComponents.AffixEntry> affixes = bow.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            damage += (float) GearHelper.getAffixTotalValueDirect(affixes, "projectile_damage");
            damage += (float) GearHelper.getAffixTotalValueDirect(affixes, "enchantment_boost");
        }

        List<ModDataComponents.StoredBonus> stored = bow.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            damage += (float) GearHelper.getStoredBonusValue(stored, "projectile_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "enchantment_boost");
        }

        // 弓蓄力加速只影响“开弓速度”（见 LivingEntityMixin），不再同时放大箭伤。
        // 旧实现在这里对弓宝石/弓词条/转生各乘一次、四件护甲累加后再乘一次，可叠加到十倍以上。
        event.setNewDamage(damage);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        DamageSource source = event.getSource();
        // 虚空、/kill 这类伤害不能被装备的固定减伤抵消，否则高强化装备会让玩家在虚空里死不掉。
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;

        float amount = event.getAmount();

        // 固定减伤只对“护甲能挡”的伤害生效。摔落/溺水/饥饿/魔法/凋零/冻结这些 #bypasses_armor
        // 伤害本来就不看护甲，装备的固定减伤也不应该替玩家免掉它们（GF-013）。
        boolean armorApplies = !source.is(DamageTypeTags.BYPASSES_ARMOR);

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.BODY) {
                ItemStack armor = entity.getItemBySlot(slot);
                if (armor.isEmpty()) continue;

                List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
                List<ModDataComponents.GemsEntry> gems = armor.get(ModDataComponents.GEMS.get());

                // 强化 / 转生 / 宝石的 defense、all_stats 已经通过 onItemAttributeModifiers 变成
                // ARMOR / ARMOR_TOUGHNESS 属性，这里不再重复做固定减伤。

                if (armorApplies && gems != null && GearHelper.hasUniqueGemEffect("nether_star", gems)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 0, false, false));
                }

                if (affixes != null) {
                    double damageReduction = GearHelper.getAffixTotalValueDirect(affixes, "damage_reduction");
                    if (damageReduction > 0) {
                        amount *= (1.0f - (float) Math.min(damageReduction, 0.9));
                    }

                    double dodgeChance = GearHelper.getAffixTotalValueDirect(affixes, "dodge_chance");
                    // 闪避只对有攻击者的伤害生效；摔落/溺水这类“无源伤害”不能靠闪避免疫。
                    if (armorApplies && source.getEntity() != null
                            && dodgeChance > 0 && entity.getRandom().nextDouble() < dodgeChance) {
                        amount = 0;
                    }
                }
            }
        }

        // Thorns: reflect damage to attacker.
        // 反弹出去的伤害本身就是荆棘伤害，不能再次触发反弹，否则两个都带荆棘词条的实体会无限互相反弹。
        Entity attacker = source.getEntity();
        if (armorApplies && attacker instanceof LivingEntity livingAttacker
                && attacker != entity && !source.is(DamageTypes.THORNS)) {
            double totalThorns = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.BODY) {
                    ItemStack armor = entity.getItemBySlot(slot);
                    if (armor.isEmpty()) continue;
                    List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
                    if (affixes != null) {
                        totalThorns += GearHelper.getAffixTotalValueDirect(affixes, "thorns");
                    }
                    // 转生保留的荆棘此前完全没被查询过，导致 [转生]荆棘 只显示不生效。
                    List<ModDataComponents.StoredBonus> stored = armor.get(ModDataComponents.STORED_BONUSES.get());
                    if (stored != null) {
                        totalThorns += GearHelper.getStoredBonusValue(stored, "thorns");
                    }
                }
            }
            if (totalThorns > 0 && amount > 0) {
                float reflectDamage = amount * (float) Math.min(totalThorns, 2.0);
                livingAttacker.hurt(entity.damageSources().thorns(entity), reflectDamage);
            }
        }

        // Shield handling: shield effects apply when held in off-hand
        ItemStack offhandShield = entity.getOffhandItem();
        if (!offhandShield.isEmpty() && offhandShield.is(Items.SHIELD)) {
            List<ModDataComponents.GemsEntry> shieldGems = offhandShield.get(ModDataComponents.GEMS.get());
            if (shieldGems != null) {
                double shieldRecovery = GearHelper.getTotalGemEffectValue(shieldGems, "shield_block_recovery");
                if (shieldRecovery > 0 && entity.isBlocking()) {
                    amount -= (float) shieldRecovery;
                }
            }

            List<ModDataComponents.AffixEntry> shieldAffixes = offhandShield.get(ModDataComponents.AFFIXES.get());
            if (shieldAffixes != null) {
                double shieldBlockReduction = GearHelper.getAffixTotalValue(shieldAffixes, "shield_block_reduction");
                if (shieldBlockReduction > 0 && entity.isBlocking()) {
                    amount -= (float) shieldBlockReduction;
                }
            }

            List<ModDataComponents.StoredBonus> shieldStored = offhandShield.get(ModDataComponents.STORED_BONUSES.get());
            if (shieldStored != null) {
                double storedShieldReduction = GearHelper.getStoredBonusValue(shieldStored, "shield_block_reduction");
                if (storedShieldReduction > 0 && entity.isBlocking()) {
                    amount -= (float) storedShieldReduction;
                }
                double storedShieldRecovery = GearHelper.getStoredBonusValue(shieldStored, "shield_block_recovery");
                if (storedShieldRecovery > 0 && entity.isBlocking()) {
                    amount -= (float) storedShieldRecovery;
                }
            }
        }

        event.setAmount(Math.max(0, amount));
    }

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        int upgradeLevel = stack.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        List<ModDataComponents.StoredBonus> storedBonuses = stack.get(ModDataComponents.STORED_BONUSES.get());

        boolean hasGearData = (affixes != null && !affixes.isEmpty())
                || upgradeLevel > 0
                || (gems != null && !gems.isEmpty())
                || (storedBonuses != null && !storedBonuses.isEmpty());
        if (!hasGearData) return;

        // 护甲只在自己的护甲槽生效（拿在手里不生效）；武器/工具在主手生效，开启副手模式后在副手生效。
        EquipmentSlotGroup slot;
        var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
        if (equippable != null) {
            slot = EquipmentSlotGroup.bySlot(equippable.slot());
        } else if (Boolean.TRUE.equals(stack.get(ModDataComponents.OFFHAND_MODE.get()))) {
            slot = EquipmentSlotGroup.OFFHAND;
        } else {
            slot = EquipmentSlotGroup.MAINHAND;
        }
        ModifierSink sink = new ModifierSink(event, slot);

        // ===== 属性路径（ADR-0004：同一项数值只能走一条路径） =====
        // 下面这些数值一律<b>只</b>以 AttributeModifier 形式生效，伤害/受击事件里不再重复结算。
        // 旧实现里 all_stats 同时加伤 + 加护甲 + 加固定减伤，1 点全属性实际等于 4 份收益。

        // 词条属性
        if (affixes != null && !affixes.isEmpty()) {
            sink.add(Attributes.ATTACK_DAMAGE, "affix_attack_power",
                    GearHelper.getAffixTotalValue(affixes, "attack_power"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_SPEED, "affix_attack_speed",
                    GearHelper.getAffixTotalValue(affixes, "attack_speed"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "affix_defense_power",
                    GearHelper.getAffixTotalValue(affixes, "defense_power"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.MOVEMENT_SPEED, "affix_movement_speed",
                    GearHelper.getAffixTotalValue(affixes, "movement_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            // 生命上限：配置值就是“点生命”，不再乘 2（乘 2 会让提示值只有实际的一半，GF-017）。
            sink.add(Attributes.MAX_HEALTH, "affix_max_health",
                    GearHelper.getAffixTotalValue(affixes, "max_health"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_KNOCKBACK, "affix_knockback",
                    GearHelper.getAffixTotalValue(affixes, "knockback_boost"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.LUCK, "affix_luck",
                    GearHelper.getAffixTotalValue(affixes, "luck"), AttributeModifier.Operation.ADD_VALUE);
        }

        // 升级属性
        if (upgradeLevel > 0) {
            sink.add(Attributes.ATTACK_DAMAGE, "upgrade_attack_damage",
                    GearHelper.getUpgradedStatBonus("attack_damage", upgradeLevel), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "upgrade_defense",
                    GearHelper.getUpgradedStatBonus("defense", upgradeLevel), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR_TOUGHNESS, "upgrade_toughness",
                    GearHelper.getUpgradedStatBonus("toughness", upgradeLevel), AttributeModifier.Operation.ADD_VALUE);
        }

        // 宝石属性：同一个修饰符 ID 只能加一次，所以多颗宝石的数值先累加。
        // 宝石的 attack_damage / defense 也走属性，和词条同一套结算方式。
        double totalAllStats = 0;
        if (gems != null && !gems.isEmpty()) {
            double gemMoveSpeed = 0;
            double gemAttackDamage = 0;
            double gemDefense = 0;
            for (ModDataComponents.GemsEntry gem : gems) {
                totalAllStats += GearHelper.getGemStatBonus(gem.id(), "all_stats", gem.tier());
                gemMoveSpeed += GearHelper.getGemStatBonus(gem.id(), "movement_speed", gem.tier());
                gemAttackDamage += GearHelper.getGemStatBonus(gem.id(), "attack_damage", gem.tier());
                gemDefense += GearHelper.getGemStatBonus(gem.id(), "defense", gem.tier());
            }
            sink.add(Attributes.MOVEMENT_SPEED, "gem_movement_speed", gemMoveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            sink.add(Attributes.ATTACK_DAMAGE, "gem_attack_damage", gemAttackDamage, AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "gem_defense", gemDefense, AttributeModifier.Operation.ADD_VALUE);
        }

        // 转生保留奖励：同样只走属性路径。
        if (storedBonuses != null && !storedBonuses.isEmpty()) {
            totalAllStats += GearHelper.getStoredBonusValue(storedBonuses, "all_stats");
            sink.add(Attributes.ATTACK_DAMAGE, "stored_attack_power",
                    GearHelper.getStoredBonusValue(storedBonuses, "attack_power"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_SPEED, "stored_attack_speed",
                    GearHelper.getStoredBonusValue(storedBonuses, "attack_speed"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "stored_defense_power",
                    GearHelper.getStoredBonusValue(storedBonuses, "defense_power"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.MOVEMENT_SPEED, "stored_movement_speed",
                    GearHelper.getStoredBonusValue(storedBonuses, "movement_speed"), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
            sink.add(Attributes.ATTACK_DAMAGE, "stored_upgrade_attack",
                    GearHelper.getStoredBonusValue(storedBonuses, "upgrade_attack_damage"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "stored_upgrade_defense",
                    GearHelper.getStoredBonusValue(storedBonuses, "upgrade_defense"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR_TOUGHNESS, "stored_upgrade_toughness",
                    GearHelper.getStoredBonusValue(storedBonuses, "upgrade_toughness"), AttributeModifier.Operation.ADD_VALUE);
            // 转生保留的词条：此前只写进 STORED_BONUSES 却从未被任何结算路径查询，
            // 导致“提示里显示 [转生]最大生命/幸运/击退/减伤/荆棘/闪避/护甲穿透，实际一点效果都没有”。
            sink.add(Attributes.MAX_HEALTH, "stored_max_health",
                    GearHelper.getStoredBonusValue(storedBonuses, "max_health"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.LUCK, "stored_luck",
                    GearHelper.getStoredBonusValue(storedBonuses, "luck"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_KNOCKBACK, "stored_knockback",
                    GearHelper.getStoredBonusValue(storedBonuses, "knockback_boost"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ARMOR, "stored_gem_defense",
                    GearHelper.getStoredBonusValue(storedBonuses, "gem_defense"), AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_DAMAGE, "stored_gem_attack",
                    GearHelper.getStoredBonusValue(storedBonuses, "gem_attack_damage"), AttributeModifier.Operation.ADD_VALUE);
            // gem_movement_speed 已由 getStoredBonusValue("movement_speed") 的 gem_ 前缀回退包含，
            // 旧实现又用 "gem_movement_speed" 再加一次，等于宝石移速转生后翻倍。
        }

        // all_stats：1 点全属性 = 1 护甲 + 1 幸运 + 1 攻击力 + 1 生命。
        // 只在这里挂属性，伤害与受击事件里不再重复加。
        if (totalAllStats > 0) {
            sink.add(Attributes.ARMOR, "all_stats_armor", totalAllStats, AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.LUCK, "all_stats_luck", totalAllStats, AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.ATTACK_DAMAGE, "all_stats_attack", totalAllStats, AttributeModifier.Operation.ADD_VALUE);
            sink.add(Attributes.MAX_HEALTH, "all_stats_health", totalAllStats, AttributeModifier.Operation.ADD_VALUE);
        }
    }

    /**
     * 往物品上挂属性修饰符。修饰符 ID 带上槽位后缀（例如 gear_forge:upgrade_defense/head），
     * 原版按 ID 合并修饰符，四件盔甲用同一个 ID 时只有一件生效，脱下一件还会把其它件的也移除。
     */
    private record ModifierSink(ItemAttributeModifierEvent event, EquipmentSlotGroup slot) {
        void add(Holder<Attribute> attribute, String name, double value, AttributeModifier.Operation operation) {
            if (value <= 0) return;
            Identifier id = Identifier.fromNamespaceAndPath(GearForgeMod.MODID,
                    name + "/" + slot.getSerializedName());
            event.addModifier(attribute, new AttributeModifier(id, value, operation), slot);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        // 玩家死亡的掉落是整个背包，不能参与掠夺复制
        if (entity instanceof Player) return;

        Entity killer = event.getSource().getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        ItemStack weapon = resolveActiveWeapon(livingKiller);
        if (weapon.isEmpty()) return;

        double lootingBonus = getLootingBonus(weapon);
        if (lootingBonus <= 0) return;

        // 上限 3 次额外产出：掠夺可以叠加（词条 + 宝石 + 转生保留），
        // 不封顶时单次击杀可能生成上百个 ItemEntity。
        double capped = Math.min(lootingBonus, 3.0);
        int extraRolls = (int) capped;
        double fraction = capped - extraRolls;
        if (entity.getRandom().nextDouble() < fraction) {
            extraRolls++;
        }
        if (extraRolls <= 0) return;

        // 只复制“普通战利品”：排除有耐久的装备，以及容器类/唯一的贵重物品。
        // 旧实现只判断 isDamageableItem()，导致潜影盒（连同 CONTAINER 组件里的整箱内容）、
        // 收纳袋、附魔书、不死图腾、下界之星等无耐久物品被整件复制。
        List<ItemEntity> originalDrops = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            if (isCopyableLoot(drop.getItem())) {
                originalDrops.add(drop);
            }
        }
        if (originalDrops.isEmpty()) return;

        for (int i = 0; i < extraRolls; i++) {
            for (ItemEntity drop : originalDrops) {
                // 复制时剥掉容器内容与自定义名称，避免“复制出一个装满东西的潜影盒”。
                ItemStack copy = drop.getItem().copyWithCount(1);
                copy.remove(DataComponents.CONTAINER);
                copy.remove(DataComponents.BUNDLE_CONTENTS);
                copy.remove(DataComponents.CUSTOM_NAME);
                event.getDrops().add(new ItemEntity(entity.level(),
                        drop.getX(), drop.getY(), drop.getZ(), copy));
            }
        }
    }

    /** 掠夺复制只针对“普通的、可堆叠的战利品”，装备与容器类一律不参与。 */
    private static boolean isCopyableLoot(ItemStack stack) {
        if (stack.isEmpty() || stack.isDamageableItem()) return false;
        if (stack.has(DataComponents.CONTAINER) || stack.has(DataComponents.BUNDLE_CONTENTS)) return false;
        if (stack.is(Items.SHULKER_BOX) || stack.is(Items.TOTEM_OF_UNDYING)
                || stack.is(Items.NETHER_STAR) || stack.is(Items.ELYTRA)
                || stack.is(Items.ENCHANTED_BOOK) || stack.is(Items.WRITTEN_BOOK)
                || stack.is(Items.WRITABLE_BOOK)) return false;
        // 药水/箭等带自定义效果的也不复制，避免刷出成组的强力消耗品。
        return !stack.has(DataComponents.POTION_CONTENTS) && !stack.has(DataComponents.CUSTOM_NAME);
    }

    private static double getLootingBonus(ItemStack weapon) {
        double lootingBonus = 0;

        List<ModDataComponents.AffixEntry> affixes = weapon.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            lootingBonus += GearHelper.getAffixTotalValueDirect(affixes, "looting_bonus");
        }

        List<ModDataComponents.GemsEntry> gems = weapon.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            for (ModDataComponents.GemsEntry gem : gems) {
                lootingBonus += GearHelper.getGemStatBonus(gem.id(), "looting", gem.tier());
            }
        }

        List<ModDataComponents.StoredBonus> stored = weapon.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            lootingBonus += GearHelper.getStoredBonusValue(stored, "looting_bonus");
            lootingBonus += GearHelper.getStoredBonusValue(stored, "looting");
        }
        return lootingBonus;
    }

    /** xp_bonus 词条：按百分比提高击杀掉落的经验。 */
    @SubscribeEvent
    public static void onLivingExperienceDrop(LivingExperienceDropEvent event) {
        Player player = event.getAttackingPlayer();
        if (player == null || event.getDroppedExperience() <= 0) return;

        ItemStack weapon = resolveActiveWeapon(player);
        if (weapon.isEmpty()) return;

        double xpBonus = 0;
        List<ModDataComponents.AffixEntry> affixes = weapon.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            xpBonus += GearHelper.getAffixTotalValueDirect(affixes, "xp_bonus");
        }
        List<ModDataComponents.StoredBonus> stored = weapon.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            xpBonus += GearHelper.getStoredBonusValue(stored, "xp_bonus");
        }
        if (xpBonus <= 0) return;

        double bonus = event.getDroppedExperience() * xpBonus;
        int extra = (int) bonus;
        if (player.getRandom().nextDouble() < bonus - extra) {
            extra++;
        }
        event.setDroppedExperience(event.getDroppedExperience() + extra);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        if (player.tickCount % 20 != 0) return;

        applyHealthRegen(player);

        applyDurabilityBoostRepair(player.getMainHandItem());
        applyDurabilityBoostRepair(player.getOffhandItem());
        // 26.3：Inventory#armor 字段被移除，装备槽统一通过 getItemBySlot 访问。
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            applyDurabilityBoostRepair(player.getItemBySlot(slot));
        }
    }

    private static void applyDurabilityBoostRepair(ItemStack stack) {
        if (!stack.isDamageableItem() || !stack.isDamaged()) return;

        // 注意：翡翠的 durability_boost 不再在这里做“每秒修复”。
        // 它在 GearHelper.durabilityDamageFactor 里只负责“减少耐久损耗”这一条路径，
        // 否则同一数值既减损耗又自动修复，等于双重收益（且 T2 起护甲永不掉耐久）。

        int repair = 0;

        List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            repair += (int) GearHelper.getAffixTotalValueDirect(affixes, "auto_repair");
        }

        List<ModDataComponents.StoredBonus> stored = stack.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            repair += (int) GearHelper.getStoredBonusValue(stored, "auto_repair");
        }

        if (repair > 0) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
        }
    }

    private static void applyHealthRegen(Player player) {
        double totalRegen = 0;
        // 26.3：Inventory#armor 字段被移除，改为遍历四个盔甲槽（与原 armor 列表等价）。
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;
            ItemStack armor = player.getItemBySlot(slot);
            List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
            if (affixes != null) {
                totalRegen += GearHelper.getAffixTotalValueDirect(affixes, "health_regen");
            }

            List<ModDataComponents.StoredBonus> armorStored = armor.get(ModDataComponents.STORED_BONUSES.get());
            if (armorStored != null) {
                totalRegen += GearHelper.getStoredBonusValue(armorStored, "health_regen");
            }
        }

        List<ModDataComponents.AffixEntry> mainHandAffixes = player.getMainHandItem().get(ModDataComponents.AFFIXES.get());
        if (mainHandAffixes != null) {
            totalRegen += GearHelper.getAffixTotalValueDirect(mainHandAffixes, "health_regen");
        }
        List<ModDataComponents.AffixEntry> offHandAffixes = player.getOffhandItem().get(ModDataComponents.AFFIXES.get());
        if (offHandAffixes != null) {
            totalRegen += GearHelper.getAffixTotalValueDirect(offHandAffixes, "health_regen");
        }

        List<ModDataComponents.StoredBonus> mainHandStored = player.getMainHandItem().get(ModDataComponents.STORED_BONUSES.get());
        if (mainHandStored != null) {
            totalRegen += GearHelper.getStoredBonusValue(mainHandStored, "health_regen");
        }
        List<ModDataComponents.StoredBonus> offHandStored = player.getOffhandItem().get(ModDataComponents.STORED_BONUSES.get());
        if (offHandStored != null) {
            totalRegen += GearHelper.getStoredBonusValue(offHandStored, "health_regen");
        }

        if (totalRegen > 0 && player.getHealth() < player.getMaxHealth()) {
            player.heal((float) totalRegen);
        }
    }

    @SubscribeEvent
    public static void onArmorHurt(ArmorHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        event.getArmorMap().forEach((slot, entry) -> {
            // 所有耐久减免来源（翡翠 durability_boost、词条 durability_reduction、转生保留）
            // 统一收敛到 GearHelper.durabilityDamageFactor，并在那里钳制下限，
            // 避免“只剩一处没钳制就变成负数耐久（= 修复）”。
            entry.newDamage = Math.max(0.0F, entry.newDamage * GearHelper.durabilityDamageFactor(entry.armorItemStack));
        });
    }

    @EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
    public static class ClientEvents {

        /** 哪些樱花物品在语言文件里有专属 tooltip 描述键。 */
        private static boolean isSakuraTooltipKey(String itemId) {
            return switch (itemId) {
                case "gear_forge:sakura_sword", "gear_forge:sakura_pickaxe", "gear_forge:sakura_axe",
                     "gear_forge:sakura_hoe", "gear_forge:sakura_drink", "gear_forge:sakura_cookie" -> true;
                default -> false;
            };
        }

        /** 转生继承详情是否展开：按住任一 Ctrl 键才显示，避免长列表刷屏。 */
        private static boolean isControlHeld() {
            return com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                    com.mojang.blaze3d.platform.InputConstants.KEY_LCONTROL)
                    || com.mojang.blaze3d.platform.InputConstants.isKeyDown(
                    com.mojang.blaze3d.platform.InputConstants.KEY_RCONTROL);
        }

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Component> tooltip = event.getToolTip();

            // Check if this item is a raw gem (show effects)
            Identifier itemRl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String itemIdStr = itemRl.toString();

            // 樱花木系列：固定描述（装备没有 gear 数据时也会走这里，确保 tooltip 有文字）。
            if ("gear_forge".equals(itemRl.getNamespace()) && itemIdStr.startsWith("gear_forge:sakura_")) {
                String key = "tooltip." + itemIdStr.replace(':', '.');
                MutableComponent desc = Component.translatable(key);
                if (isSakuraTooltipKey(itemIdStr)) {
                    tooltip.add(Component.empty());
                    tooltip.add(desc.withStyle(ChatFormatting.LIGHT_PURPLE));
                }
                if (itemIdStr.equals("gear_forge:sakura_helmet")
                        || itemIdStr.equals("gear_forge:sakura_chestplate")
                        || itemIdStr.equals("gear_forge:sakura_leggings")
                        || itemIdStr.equals("gear_forge:sakura_boots")) {
                    tooltip.add(Component.translatable("tooltip.gear_forge.sakura_set")
                            .withStyle(ChatFormatting.AQUA));
                }
                // 继续向下追加 gear 数据的 tooltip，不吞掉强化/宝石信息。
            }

            // 耀金套装：四件套效果提示。
            if ("gear_forge".equals(itemRl.getNamespace())
                    && (itemIdStr.equals("gear_forge:yaokin_helmet")
                    || itemIdStr.equals("gear_forge:yaokin_chestplate")
                    || itemIdStr.equals("gear_forge:yaokin_leggings")
                    || itemIdStr.equals("gear_forge:yaokin_boots"))) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("tooltip.gear_forge.yaokin_set")
                        .withStyle(ChatFormatting.GOLD));
            }

            GemConfig gemConfig = ModConfigs.getGemConfig();
            boolean isRawGem = false;
            for (GemDefinition gem : gemConfig.getGems()) {
                if (itemIdStr.equals(gem.getItemId()) && gem.getEffects() != null) {
                    isRawGem = true;
                    tooltip.add(Component.empty());

                    ChatFormatting gemColor = switch (gem.getRarity() == null ? "" : gem.getRarity()) {
                        case "rare" -> ChatFormatting.BLUE;
                        case "epic" -> ChatFormatting.LIGHT_PURPLE;
                        default -> ChatFormatting.GREEN;
                    };

                    // 显示当前强化等级
                    int tier = stack.getOrDefault(ModDataComponents.GEM_TIER.get(), 1);
                    if (tier > 1) {
                        tooltip.add(Component.literal("T" + tier).withStyle(gemColor));
                    }

                    // 根据实际等级生成动态效果描述
                    for (GemEffect effect : gem.getEffects()) {
                        String type = effect.getType();
                        // 特殊效果：纯文字，无数值（走语言文件，避免英文语言下显示中文）
                        if ("unique".equals(type) || "enchant_compat".equals(type)
                                || "sakura_blessing".equals(type) || "sakura_mark".equals(type)) {
                            tooltip.add(Component.literal(" ").append(
                                    Component.translatable("tooltip.gear_forge.gem_effect." + type))
                                    .withStyle(gemColor));
                        } else {
                            double actualValue = GearHelper.getGemStatBonus(gem.getId(), type, tier);
                            MutableComponent display = formatGemEffectText(type, actualValue);
                            tooltip.add(Component.literal(" ").append(display).withStyle(gemColor));
                        }
                    }
                    break;
                }
            }
            if (isRawGem) return;

            List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
            Integer upgradeLevel = stack.get(ModDataComponents.UPGRADE_LEVEL.get());
            List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
            List<ModDataComponents.StoredBonus> storedBonuses = stack.get(ModDataComponents.STORED_BONUSES.get());

            boolean hasGearData = (affixes != null && !affixes.isEmpty())
                    || (upgradeLevel != null && upgradeLevel > 0)
                    || (gems != null && !gems.isEmpty())
                    || (storedBonuses != null && !storedBonuses.isEmpty());

            if (!hasGearData) return;

            // Show offhand mode toggle status
            Boolean offhandMode = stack.get(ModDataComponents.OFFHAND_MODE.get());
            if (Boolean.TRUE.equals(offhandMode)) {
                tooltip.add(Component.translatable("tooltip.gear_forge.offhand_mode")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }

            tooltip.add(Component.empty());

            // Reincarnation count
            Integer reincCount = stack.get(ModDataComponents.REINCARNATION_COUNT.get());
            if (reincCount != null && reincCount > 0) {
                tooltip.add(Component.translatable("tooltip.gear_forge.reincarnation_count", reincCount)
                        .withStyle(ChatFormatting.DARK_RED));
            }

            if (upgradeLevel != null && upgradeLevel > 0) {
                ChatFormatting color = upgradeLevel >= 15 ? ChatFormatting.GOLD
                        : upgradeLevel >= 10 ? ChatFormatting.LIGHT_PURPLE
                        : upgradeLevel >= 5 ? ChatFormatting.AQUA
                        : ChatFormatting.GREEN;
                tooltip.add(Component.translatable("tooltip.gear_forge.upgrade_level", upgradeLevel)
                        .withStyle(color));
            }

            if (affixes != null && !affixes.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.gear_forge.affixes_header")
                        .withStyle(ChatFormatting.GRAY));

                // 合并同名（同显示名）的词条数值，累加原始值，输出时按第一个词条的类型格式化
                Map<String, Double> mergedAffixes = new LinkedHashMap<>();
                Map<String, String> firstIdByName = new LinkedHashMap<>();
                for (ModDataComponents.AffixEntry entry : affixes) {
                    String nameKey = getAffixNameKey(entry.id());
                    mergedAffixes.merge(nameKey, entry.value(), Double::sum);
                    firstIdByName.putIfAbsent(nameKey, entry.id());
                }

                for (var entry : mergedAffixes.entrySet()) {
                    String affixId = firstIdByName.get(entry.getKey());
                    Component affixName = Component.translatable(getAffixNameKey(affixId));
                    String formattedValue = formatAffixValue(affixId, entry.getValue());
                    tooltip.add(Component.literal(" ").append(affixName)
                            .append(Component.literal(": " + formattedValue))
                            .withStyle(ChatFormatting.DARK_AQUA));
                }
            }

            if (gems != null && !gems.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.gear_forge.gems_header")
                        .withStyle(ChatFormatting.GRAY));
                for (ModDataComponents.GemsEntry entry : gems) {
                    Component gemName = Component.translatable(getGemNameKey(entry.id()));
                    if (entry.tier() > 1) {
                        tooltip.add(Component.literal(" ").append(Component.literal("\u2666 "))
                                .append(gemName).append(Component.literal(" (T" + entry.tier() + ")"))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else {
                        tooltip.add(Component.literal(" \u2666 ").append(gemName)
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }

            if (storedBonuses != null && !storedBonuses.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.gear_forge.stored_bonuses_header")
                        .withStyle(ChatFormatting.GRAY));
                if (!isControlHeld()) {
                    tooltip.add(Component.translatable("tooltip.gear_forge.stored_bonuses_hint")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    return;
                }
                // 用原始 id 作合并 key，显示名按语言文件翻译（避免英文语言下显示中文）
                Map<String, Double> mergedStored = new LinkedHashMap<>();
                for (ModDataComponents.StoredBonus sb : storedBonuses) {
                    mergedStored.merge(sb.id(), sb.value(), Double::sum);
                }
                for (var entry : mergedStored.entrySet()) {
                    String rawId = entry.getKey();
                    double totalValue = entry.getValue();
                    Component displayName = getStoredBonusName(rawId);
                    String formattedValue;
                    if (isStoredBonusPercentage(rawId)) {
                        formattedValue = fmt(totalValue * 100) + "%";
                    } else {
                        formattedValue = fmt(totalValue);
                    }
                    tooltip.add(Component.literal(" ").append(displayName)
                            .append(Component.literal(": " + formattedValue))
                            .withStyle(ChatFormatting.GOLD));
                }
            }

        }

        /** 词条 id → 语言文件键；未在池中登记的 id 回退为 affix.gear_forge.<id>。 */
        private static String getAffixNameKey(String id) {
            var config = ModConfigs.getAffixConfig();
            boolean known = config.getAffixPool().stream().anyMatch(a -> a.getId().equals(id));
            return known ? "affix.gear_forge." + id : id;
        }

        /** 宝石 id → 语言文件键。 */
        private static String getGemNameKey(String id) {
            var config = ModConfigs.getGemConfig();
            for (GemConfig.GemDefinition gem : config.getGems()) {
                if (gem.getId().equals(id)) return "gem.gear_forge." + id;
            }
            return "gem.gear_forge." + id;
        }

        private static String formatAffixValue(String id, double value) {
            var config = ModConfigs.getAffixConfig();
            var def = config.getAffixPool().stream()
                    .filter(a -> a.getId().equals(id))
                    .findFirst().orElse(null);
            if (def != null && "percentage".equals(def.getType())) {
                return fmt(value * 100) + "%";
            }
            return fmt(value);
        }

        private static MutableComponent formatGemEffectText(String type, double value) {
            String formattedValue = fmt(value);
            return switch (type) {
                case "attack_damage"        -> Component.translatable("tooltip.gear_forge.gem.attack_damage", formattedValue);
                case "fire_damage"          -> Component.translatable("tooltip.gear_forge.gem.fire_damage", formattedValue);
                case "frost_damage"         -> Component.translatable("tooltip.gear_forge.gem.frost_damage", formattedValue);
                case "defense"              -> Component.translatable("tooltip.gear_forge.gem.defense", formattedValue);
                case "toughness"            -> Component.translatable("tooltip.gear_forge.gem.toughness", formattedValue);
                case "movement_speed"       -> Component.translatable("tooltip.gear_forge.gem.movement_speed", fmt(value * 100));
                case "looting"              -> Component.translatable("tooltip.gear_forge.gem.looting", (int) value);
                case "all_stats"            -> Component.translatable("tooltip.gear_forge.gem.all_stats", formattedValue);
                case "bow_charge_speed"     -> Component.translatable("tooltip.gear_forge.gem.bow_charge_speed", fmt(value * 100));
                case "projectile_damage"    -> Component.translatable("tooltip.gear_forge.gem.projectile_damage", formattedValue);
                case "durability_boost"     -> Component.translatable("tooltip.gear_forge.gem.durability_boost", fmt(value * 100));
                case "shield_block_recovery"-> Component.translatable("tooltip.gear_forge.gem.shield_block_recovery", formattedValue);
                default                     -> Component.translatable("tooltip.gear_forge.gem." + type, formattedValue);
            };
        }

        /**
         * 转生继承项显示名。全部走语言文件，避免英文语言下出现中文。
         * 用 Component 作 translation 参数，让"[转生]"/"宝石"/"强化"前缀随语言切换。
         */
        private static Component getStoredBonusName(String id) {
            var affixCfg = ModConfigs.getAffixConfig();
            boolean knownAffix = affixCfg.getAffixPool().stream().anyMatch(a -> a.getId().equals(id));
            if (knownAffix) {
                return Component.translatable("tooltip.gear_forge.reincarnated",
                        Component.translatable("affix.gear_forge." + id));
            }

            if (id.startsWith("gem_")) {
                String base = id.substring(4);
                Component inner = knownStat(base)
                        ? Component.translatable("stat.gear_forge." + base)
                        : Component.translatable("gem.gear_forge." + base);
                return Component.translatable("tooltip.gear_forge.reincarnated.gem", inner);
            }

            if (id.startsWith("upgrade_")) {
                String base = id.substring(8);
                Component inner = knownStat(base)
                        ? Component.translatable("stat.gear_forge." + base)
                        : Component.translatable("affix.gear_forge." + base);
                return Component.translatable("tooltip.gear_forge.reincarnated.upgrade", inner);
            }

            return Component.translatable("tooltip.gear_forge.reincarnated", id);
        }

        /** 是否为已知的原版属性名。 */
        private static boolean knownStat(String base) {
            return switch (base) {
                case "attack_damage", "defense", "toughness", "knockback_resistance",
                     "movement_speed", "attack_speed", "armor", "armor_toughness",
                     "max_health", "luck" -> true;
                default -> false;
            };
        }

        private static boolean isStoredBonusPercentage(String id) {
            String lookup = id;
            if (lookup.startsWith("gem_")) lookup = lookup.substring(4);
            if (lookup.startsWith("upgrade_")) lookup = lookup.substring(8);
            var def = ModConfigs.getAffixDefinition(lookup);
            return def != null && "percentage".equals(def.getType());
        }
    }
}
