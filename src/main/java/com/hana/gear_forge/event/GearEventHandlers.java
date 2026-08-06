package com.hana.gear_forge.event;

import com.hana.gear_forge.GearForgeMod;
import com.hana.gear_forge.component.ModDataComponents;
import com.hana.gear_forge.config.AffixConfig.AffixDefinition;
import com.hana.gear_forge.config.GemConfig;
import com.hana.gear_forge.config.GemConfig.GemDefinition;
import com.hana.gear_forge.config.GemConfig.GemEffect;
import com.hana.gear_forge.config.ModConfigs;
import com.hana.gear_forge.loot.AddItemModifier;
import com.hana.gear_forge.util.GearHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ArmorItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.ArmorHurtEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

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
        if (source.getDirectEntity() instanceof AbstractArrow) {
            handleRangedProjectileDamage(event, livingAttacker);
            return;
        }

        float damage = event.getNewDamage();
        ItemStack weapon = resolveActiveWeapon(livingAttacker);
        if (weapon.isEmpty()) return;

        List<ModDataComponents.AffixEntry> affixes = weapon.get(ModDataComponents.AFFIXES.get());
        List<ModDataComponents.GemsEntry> gems = weapon.get(ModDataComponents.GEMS.get());
        int upgradeLevel = weapon.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);
        List<ModDataComponents.StoredBonus> stored = weapon.get(ModDataComponents.STORED_BONUSES.get());

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

        double upgradeDamage = GearHelper.getUpgradedStatBonus("attack_damage", upgradeLevel);
        damage += upgradeDamage;

        if (gems != null) {
            double gemAttackBonus = 0;
            for (ModDataComponents.GemsEntry gem : gems) {
                gemAttackBonus += GearHelper.getGemStatBonus(gem.id(), "attack_damage", gem.tier());
                double allStats = GearHelper.getGemStatBonus(gem.id(), "all_stats", gem.tier());
                gemAttackBonus += allStats;
                double gemFire = GearHelper.getGemStatBonus(gem.id(), "fire_damage", gem.tier());
                damage += gemFire;
                if (gemFire > 0) {
                    target.setRemainingFireTicks((int) (gemFire * 30));
                }
                double gemFrost = GearHelper.getGemStatBonus(gem.id(), "frost_damage", gem.tier());
                damage += gemFrost;
                if (gemFrost > 0) {
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, Math.min((int) gemFrost - 1, 2)));
                }
                double gemEnchantBoost = GearHelper.getGemStatBonus(gem.id(), "enchant_boost", gem.tier());
                if (gemEnchantBoost > 0) {
                    damage += (float) gemEnchantBoost;
                }
            }
            damage += gemAttackBonus;
        }

        // Stored bonuses from reincarnation
        if (stored != null) {
            damage += (float) GearHelper.getStoredBonusValue(stored, "attack_power");
            damage += (float) GearHelper.getStoredBonusValue(stored, "fire_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "frost_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "enchantment_boost");
            damage += (float) GearHelper.getStoredBonusValue(stored, "upgrade_attack_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "attack_damage");
            damage += (float) GearHelper.getStoredBonusValue(stored, "all_stats");
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
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, Math.min((int) storedFrost - 1, 3)));
            }
            double storedLightning = GearHelper.getStoredBonusValue(stored, "lightning_damage");
            if (storedLightning > 0 && target.getRandom().nextDouble() < 0.25) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
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
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, Math.min((int) frostValue - 1, 3)));
            }

            double lightningValue = GearHelper.getAffixTotalValue(affixes, "lightning_damage");
            if (lightningValue > 0 && target.getRandom().nextDouble() < 0.25) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                if (target.level().isThundering()) {
                    damage *= 1.15f;
                }
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

    private static void handleRangedProjectileDamage(LivingDamageEvent.Pre event, LivingEntity attacker) {
        ItemStack bow = attacker.getMainHandItem();
        if (!bow.is(Items.BOW) && !bow.is(Items.CROSSBOW)) {
            bow = attacker.getOffhandItem();
        }
        if (!bow.is(Items.BOW) && !bow.is(Items.CROSSBOW)) return;

        float damage = event.getNewDamage();

        List<ModDataComponents.GemsEntry> gems = bow.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            double projDmg = GearHelper.getTotalGemEffectValue(gems, "projectile_damage");
            double chargeSpeed = GearHelper.getTotalGemEffectValue(gems, "bow_charge_speed");
            damage += (float) projDmg;
            damage *= (1.0f + (float) chargeSpeed);
        }

        List<ModDataComponents.AffixEntry> affixes = bow.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            double affixProjDmg = GearHelper.getAffixTotalValueDirect(affixes, "projectile_damage");
            damage += (float) affixProjDmg;
            double affixChargeSpeed = GearHelper.getAffixTotalValueDirect(affixes, "bow_charge_speed");
            damage *= (1.0f + (float) affixChargeSpeed);
            double affixEnchantBoost = GearHelper.getAffixTotalValueDirect(affixes, "enchantment_boost");
            damage += (float) affixEnchantBoost;
        }

        List<ModDataComponents.StoredBonus> stored = bow.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            damage += (float) GearHelper.getStoredBonusValue(stored, "projectile_damage");
            damage *= (1.0f + (float) GearHelper.getStoredBonusValue(stored, "bow_charge_speed"));
            damage += (float) GearHelper.getStoredBonusValue(stored, "enchantment_boost");
        }

        double totalArmorBowCharge = 0;
        for (ItemStack armor : attacker.getArmorSlots()) {
            if (armor.isEmpty()) continue;

            List<ModDataComponents.AffixEntry> armorAffixes = armor.get(ModDataComponents.AFFIXES.get());
            if (armorAffixes != null) {
                totalArmorBowCharge += GearHelper.getAffixTotalValueDirect(armorAffixes, "bow_charge_speed");
            }

            List<ModDataComponents.GemsEntry> armorGems = armor.get(ModDataComponents.GEMS.get());
            if (armorGems != null) {
                totalArmorBowCharge += GearHelper.getTotalGemEffectValue(armorGems, "bow_charge_speed");
            }

            List<ModDataComponents.StoredBonus> armorStored = armor.get(ModDataComponents.STORED_BONUSES.get());
            if (armorStored != null) {
                totalArmorBowCharge += GearHelper.getStoredBonusValue(armorStored, "bow_charge_speed");
            }
        }
        if (totalArmorBowCharge > 0) {
            damage *= (1.0f + (float) totalArmorBowCharge);
        }

        event.setNewDamage(damage);
    }

    @SubscribeEvent
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        float amount = event.getAmount();

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.BODY) {
                ItemStack armor = entity.getItemBySlot(slot);
                if (armor.isEmpty()) continue;

                List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
                List<ModDataComponents.GemsEntry> gems = armor.get(ModDataComponents.GEMS.get());
                int upgradeLevel = armor.getOrDefault(ModDataComponents.UPGRADE_LEVEL.get(), 0);

                double upgradeDefense = GearHelper.getUpgradedStatBonus("defense", upgradeLevel);
                amount -= upgradeDefense;
                double upgradeToughness = GearHelper.getUpgradedStatBonus("toughness", upgradeLevel);
                amount -= upgradeToughness;

                if (gems != null) {
                    double gemDefense = 0;
                    for (ModDataComponents.GemsEntry gem : gems) {
                        gemDefense += GearHelper.getGemStatBonus(gem.id(), "defense", gem.tier());
                        double allStats = GearHelper.getGemStatBonus(gem.id(), "all_stats", gem.tier());
                        gemDefense += allStats;
                    }
                    amount -= gemDefense;
                }

                if (gems != null && GearHelper.hasUniqueGemEffect("nether_star", gems)) {
                    entity.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0, false, false));
                }

                List<ModDataComponents.StoredBonus> stored = armor.get(ModDataComponents.STORED_BONUSES.get());
                if (stored != null) {
                    amount -= (float) GearHelper.getStoredBonusValue(stored, "upgrade_defense");
                    amount -= (float) GearHelper.getStoredBonusValue(stored, "upgrade_toughness");
                    amount -= (float) GearHelper.getStoredBonusValue(stored, "defense_power");
                    amount -= (float) GearHelper.getStoredBonusValue(stored, "defense");
                    amount -= (float) GearHelper.getStoredBonusValue(stored, "all_stats");
                }

                if (affixes != null) {
                    double damageReduction = GearHelper.getAffixTotalValueDirect(affixes, "damage_reduction");
                    if (damageReduction > 0) {
                        amount *= (1.0f - (float) damageReduction);
                    }

                    double dodgeChance = GearHelper.getAffixTotalValueDirect(affixes, "dodge_chance");
                    if (dodgeChance > 0 && entity.getRandom().nextDouble() < dodgeChance) {
                        amount = 0;
                    }
                }
            }
        }

        // Thorns: reflect damage to attacker
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof LivingEntity livingAttacker) {
            double totalThorns = 0;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot == EquipmentSlot.BODY) {
                    ItemStack armor = entity.getItemBySlot(slot);
                    if (armor.isEmpty()) continue;
                    List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
                    if (affixes != null) {
                        totalThorns += GearHelper.getAffixTotalValueDirect(affixes, "thorns");
                    }
                }
            }
            if (totalThorns > 0 && amount > 0) {
                float reflectDamage = amount * (float) totalThorns;
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
        boolean offhand = Boolean.TRUE.equals(stack.get(ModDataComponents.OFFHAND_MODE.get()));
        boolean isArmor = stack.getItem() instanceof ArmorItem;

        // 武器类（非护甲）：攻击属性挂载到主手或副手；防御/移速等其他属性挂载到 ANY
        // 护甲类：所有属性统一挂载到 ANY（穿戴效果）
        EquipmentSlotGroup weaponSlot = offhand ? EquipmentSlotGroup.OFFHAND : EquipmentSlotGroup.MAINHAND;
        EquipmentSlotGroup attackSlot = isArmor ? EquipmentSlotGroup.ANY : weaponSlot;   // 攻击、攻速
        EquipmentSlotGroup otherSlot = isArmor ? EquipmentSlotGroup.ANY : weaponSlot;                          // 防御、移速等

        // 词条属性
        if (affixes != null && !affixes.isEmpty()) {
            double attackPower = GearHelper.getAffixTotalValue(affixes, "attack_power");
            if (attackPower > 0) {
                event.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_attack_power"),
                                attackPower, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }

            double attackSpeed = GearHelper.getAffixTotalValue(affixes, "attack_speed");
            if (attackSpeed > 0) {
                event.addModifier(Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_attack_speed"),
                                attackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }

            double defensePower = GearHelper.getAffixTotalValue(affixes, "defense_power");
            if (defensePower > 0) {
                event.addModifier(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_defense_power"),
                                defensePower, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }

            double moveSpeed = GearHelper.getAffixTotalValue(affixes, "movement_speed");
            if (moveSpeed > 0) {
                event.addModifier(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_movement_speed"),
                                moveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        otherSlot);
            }

            double maxHealth = GearHelper.getAffixTotalValue(affixes, "max_health");
            if (maxHealth > 0) {
                event.addModifier(Attributes.MAX_HEALTH,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_max_health"),
                                maxHealth * 2.0, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }

            double knockback = GearHelper.getAffixTotalValue(affixes, "knockback_boost");
            if (knockback > 0) {
                event.addModifier(Attributes.ATTACK_KNOCKBACK,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_knockback"),
                                knockback, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }

            double luck = GearHelper.getAffixTotalValue(affixes, "luck");
            if (luck > 0) {
                event.addModifier(Attributes.LUCK,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "affix_luck"),
                                luck, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
        }

        // 升级属性
        if (upgradeLevel > 0) {
            double upgradeAttack = GearHelper.getUpgradedStatBonus("attack_damage", upgradeLevel);
            if (upgradeAttack > 0) {
                event.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "upgrade_attack_damage"),
                                upgradeAttack, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);   // 盔甲上的攻击升级也作为穿戴效果
            }
            double upgradeDefense = GearHelper.getUpgradedStatBonus("defense", upgradeLevel);
            if (upgradeDefense > 0) {
                event.addModifier(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "upgrade_defense"),
                                upgradeDefense, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
            double upgradeToughness = GearHelper.getUpgradedStatBonus("toughness", upgradeLevel);
            if (upgradeToughness > 0) {
                event.addModifier(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "upgrade_toughness"),
                                upgradeToughness, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
        }

        // 宝石属性（已有逻辑不动，但这里也加上统一槽位）
        double totalAllStats = 0;
        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null && !gems.isEmpty()) {
            for (ModDataComponents.GemsEntry gem : gems) {
                totalAllStats += GearHelper.getGemStatBonus(gem.id(), "all_stats", gem.tier());
                double gemMoveSpeed = GearHelper.getGemStatBonus(gem.id(), "movement_speed", gem.tier());
                if (gemMoveSpeed > 0) {
                    event.addModifier(Attributes.MOVEMENT_SPEED,
                            new AttributeModifier(
                                    ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "gem_movement_speed"),
                                    gemMoveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                            otherSlot);
                }
            }
        }

        // 转生保留奖励
        List<ModDataComponents.StoredBonus> storedBonuses = stack.get(ModDataComponents.STORED_BONUSES.get());
        if (storedBonuses != null && !storedBonuses.isEmpty()) {
            totalAllStats += GearHelper.getStoredBonusValue(storedBonuses, "all_stats");
            double storedAttackPower = GearHelper.getStoredBonusValue(storedBonuses, "attack_power");
            if (storedAttackPower > 0) {
                event.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_attack_power"),
                                storedAttackPower, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }
            double storedAttackSpeed = GearHelper.getStoredBonusValue(storedBonuses, "attack_speed");
            if (storedAttackSpeed > 0) {
                event.addModifier(Attributes.ATTACK_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_attack_speed"),
                                storedAttackSpeed, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }
            double storedDefensePower = GearHelper.getStoredBonusValue(storedBonuses, "defense_power");
            if (storedDefensePower > 0) {
                event.addModifier(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_defense_power"),
                                storedDefensePower, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
            double storedMoveSpeed = GearHelper.getStoredBonusValue(storedBonuses, "movement_speed");
            if (storedMoveSpeed > 0) {
                event.addModifier(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_movement_speed"),
                                storedMoveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        otherSlot);
            }
            double storedUpgradeAttack = GearHelper.getStoredBonusValue(storedBonuses, "upgrade_attack_damage");
            if (storedUpgradeAttack > 0) {
                event.addModifier(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_upgrade_attack"),
                                storedUpgradeAttack, AttributeModifier.Operation.ADD_VALUE),
                        attackSlot);
            }
            double storedUpgradeDefense = GearHelper.getStoredBonusValue(storedBonuses, "upgrade_defense");
            if (storedUpgradeDefense > 0) {
                event.addModifier(Attributes.ARMOR,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_upgrade_defense"),
                                storedUpgradeDefense, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
            double storedUpgradeToughness = GearHelper.getStoredBonusValue(storedBonuses, "upgrade_toughness");
            if (storedUpgradeToughness > 0) {
                event.addModifier(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_upgrade_toughness"),
                                storedUpgradeToughness, AttributeModifier.Operation.ADD_VALUE),
                        otherSlot);
            }
            double storedGemMoveSpeed = GearHelper.getStoredBonusValue(storedBonuses, "gem_movement_speed");
            if (storedGemMoveSpeed > 0) {
                event.addModifier(Attributes.MOVEMENT_SPEED,
                        new AttributeModifier(
                                ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "stored_gem_movement"),
                                storedGemMoveSpeed, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
                        otherSlot);
            }
        }
        // --- 应用 all_stats 的多重加成 ---
        if (totalAllStats > 0) {
            // 1. 增加护甲值 (Armor)
            event.addModifier(Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "all_stats_armor"),
                            totalAllStats, AttributeModifier.Operation.ADD_VALUE),
                    otherSlot);

            // 2. 增加幸运值 (Luck)
            event.addModifier(Attributes.LUCK,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "all_stats_luck"),
                            totalAllStats, AttributeModifier.Operation.ADD_VALUE),
                    otherSlot);

            // 3. 增加生命值 (Max Health)
            // 注意：生命值加成通常只在护甲上生效比较平衡，如果你希望武器也加血，保持 otherSlot 即可
            event.addModifier(Attributes.MAX_HEALTH,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(GearForgeMod.MODID, "all_stats_health"),
                            totalAllStats * 2, // 比如 1点全属性 = 2点生命值(1颗心)
                            AttributeModifier.Operation.ADD_VALUE),
                    otherSlot);
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;

        var source = event.getSource();
        Entity killer = source.getEntity();
        if (!(killer instanceof LivingEntity livingKiller)) return;

        ItemStack weapon = resolveActiveWeapon(livingKiller);
        if (weapon.isEmpty()) return;

        List<ModDataComponents.AffixEntry> affixes = weapon.get(ModDataComponents.AFFIXES.get());
        List<ModDataComponents.GemsEntry> gems = weapon.get(ModDataComponents.GEMS.get());
        List<ModDataComponents.StoredBonus> stored = weapon.get(ModDataComponents.STORED_BONUSES.get());

        double lootingBonus = 0;

        if (affixes != null) {
            double affixLooting = GearHelper.getAffixTotalValueDirect(affixes, "looting_bonus");
            lootingBonus += affixLooting;
        }

        if (gems != null) {
            for (ModDataComponents.GemsEntry gem : gems) {
                lootingBonus += GearHelper.getGemStatBonus(gem.id(), "looting", gem.tier());
            }
        }

        if (stored != null) {
            lootingBonus += GearHelper.getStoredBonusValue(stored, "looting_bonus");
            lootingBonus += GearHelper.getStoredBonusValue(stored, "looting");
        }

        if (lootingBonus > 0 && event.getDrops() != null) {
            int extraRolls = (int) lootingBonus;
            double fraction = lootingBonus - extraRolls;
            if (entity.getRandom().nextDouble() < fraction) {
                extraRolls++;
            }
            List<ItemEntity> originalDrops = new ArrayList<>(event.getDrops());
            for (int i = 0; i < extraRolls && !originalDrops.isEmpty(); i++) {
                for (ItemEntity drop : originalDrops) {
                    ItemStack dropStack = drop.getItem().copy();
                    dropStack.setCount(1);
                    event.getDrops().add(new ItemEntity(entity.level(),
                            drop.getX(), drop.getY(), drop.getZ(), dropStack));
                }
            }
        }

        if (affixes != null) {
            double xpBonus = GearHelper.getAffixTotalValueDirect(affixes, "xp_bonus");
            if (xpBonus > 0) {
                int extraXp = (int) (event.getDrops().size() * xpBonus);
                if (extraXp > 0) {
                    entity.level().addFreshEntity(new ExperienceOrb(entity.level(),
                            entity.getX(), entity.getY(), entity.getZ(), extraXp));
                }
            }
        }

        if (stored != null) {
            double storedXp = GearHelper.getStoredBonusValue(stored, "xp_bonus");
            if (storedXp > 0) {
                int extraXp = (int) (event.getDrops().size() * storedXp);
                if (extraXp > 0) {
                    entity.level().addFreshEntity(new ExperienceOrb(entity.level(),
                            entity.getX(), entity.getY(), entity.getZ(), extraXp));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        if (player.tickCount % 20 != 0) return;

        applyHealthRegen(player);

        applyDurabilityBoostRepair(player.getMainHandItem());
        applyDurabilityBoostRepair(player.getOffhandItem());
        for (ItemStack armor : player.getInventory().armor) {
            applyDurabilityBoostRepair(armor);
        }
    }

    private static void applyDurabilityBoostRepair(ItemStack stack) {
        if (!stack.isDamageableItem() || !stack.isDamaged()) return;

        List<ModDataComponents.GemsEntry> gems = stack.get(ModDataComponents.GEMS.get());
        if (gems != null) {
            double boost = GearHelper.getTotalGemEffectValue(gems, "durability_boost");
            if (boost > 0) {
                int repair = Math.max(1, (int) (stack.getMaxDamage() * boost * 0.002));
                int newDamage = Math.max(0, stack.getDamageValue() - repair);
                stack.setDamageValue(newDamage);
            }
        }

        List<ModDataComponents.AffixEntry> affixes = stack.get(ModDataComponents.AFFIXES.get());
        if (affixes != null) {
            double autoRepair = GearHelper.getAffixTotalValueDirect(affixes, "auto_repair");
            if (autoRepair > 0) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - (int) autoRepair));
            }
        }

        List<ModDataComponents.StoredBonus> stored = stack.get(ModDataComponents.STORED_BONUSES.get());
        if (stored != null) {
            double storedAutoRepair = GearHelper.getStoredBonusValue(stored, "auto_repair");
            if (storedAutoRepair > 0) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - (int) storedAutoRepair));
            }
            double storedDurabilityBoost = GearHelper.getStoredBonusValue(stored, "durability_boost");
            if (storedDurabilityBoost > 0) {
                int repair = Math.max(1, (int) (stack.getMaxDamage() * storedDurabilityBoost * 0.002));
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
            }
        }
    }

    private static void applyHealthRegen(Player player) {
        double totalRegen = 0;
        for (ItemStack armor : player.getInventory().armor) {
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
            ItemStack armor = entry.armorItemStack;

            List<ModDataComponents.GemsEntry> gems = armor.get(ModDataComponents.GEMS.get());
            if (gems != null) {
                double durabilityBoost = GearHelper.getTotalGemEffectValue(gems, "durability_boost");
                if (durabilityBoost > 0) {
                    entry.newDamage = (float) (entry.newDamage * (1.0 - durabilityBoost * 0.5));
                }
            }

            List<ModDataComponents.AffixEntry> affixes = armor.get(ModDataComponents.AFFIXES.get());
            if (affixes != null) {
                double durabilityReduction = GearHelper.getAffixTotalValueDirect(affixes, "durability_reduction");
                if (durabilityReduction > 0) {
                    entry.newDamage = (float) (entry.newDamage * (1.0 - durabilityReduction));
                }
            }

            List<ModDataComponents.StoredBonus> stored = armor.get(ModDataComponents.STORED_BONUSES.get());
            if (stored != null) {
                double storedDurabilityReduction = GearHelper.getStoredBonusValue(stored, "durability_reduction");
                if (storedDurabilityReduction > 0) {
                    entry.newDamage = (float) (entry.newDamage * (1.0 - storedDurabilityReduction));
                }
                double storedDurabilityBoost = GearHelper.getStoredBonusValue(stored, "durability_boost");
                if (storedDurabilityBoost > 0) {
                    entry.newDamage = (float) (entry.newDamage * (1.0 - storedDurabilityBoost * 0.5));
                }
            }
        });
    }

    @EventBusSubscriber(modid = GearForgeMod.MODID, value = Dist.CLIENT)
    public static class ClientEvents {

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            List<Component> tooltip = event.getToolTip();

            // Check if this item is a raw gem (show effects)
            ResourceLocation itemRl = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String itemIdStr = itemRl.toString();
            GemConfig gemConfig = ModConfigs.getGemConfig();
            boolean isRawGem = false;
            for (GemDefinition gem : gemConfig.getGems()) {
                if (gem.getItemId().equals(itemIdStr)) {
                    isRawGem = true;
                    tooltip.add(Component.empty());

                    ChatFormatting gemColor = switch (gem.getRarity()) {
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
                        // 特殊效果：纯文字，无数值
                        if ("unique".equals(type) || "enchant_compat".equals(type)) {
                            tooltip.add(Component.literal(" " + effect.getDescription()).withStyle(gemColor));
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

                // 合并同名（同显示名）的词条数值
                Map<String, Double> mergedAffixes = new LinkedHashMap<>();
                for (ModDataComponents.AffixEntry entry : affixes) {
                    String displayName = getAffixDisplayName(entry.id());
                    String formattedValue = formatAffixValue(entry.id(), entry.value());
                    // 由于 formattedValue 可能带百分号，我们需要还原原始数值进行累加
                    double val = entry.value();
                    if (formattedValue.endsWith("%")) {
                        // 百分比格式的数值已经被乘以100，累加时用原始值
                        val = entry.value();
                    }
                    mergedAffixes.merge(displayName, val, Double::sum);
                }

                // 输出合并后的行（保留百分比格式需要根据id判断，简单起见统一用数值和名称，格式由formatAffixValue对单个条目处理）
                // 这里直接显示总和，格式需要重新生成，将每个显示名对应的总和重新格式化。
                for (var entry : mergedAffixes.entrySet()) {
                    String displayName = entry.getKey();
                    double totalValue = entry.getValue();
                    // 查找第一个匹配的id以获取格式类型（假设同名的id类型相同，简单取第一个）
                    String firstId = null;
                    for (ModDataComponents.AffixEntry affix : affixes) {
                        if (getAffixDisplayName(affix.id()).equals(displayName)) {
                            firstId = affix.id();
                            break;
                        }
                    }
                    String formattedValue = formatAffixValue(firstId != null ? firstId : displayName, totalValue);
                    tooltip.add(Component.literal(" ").append(Component.literal(displayName + ": " + formattedValue))
                            .withStyle(ChatFormatting.DARK_AQUA));
                }
            }

            if (gems != null && !gems.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.gear_forge.gems_header")
                        .withStyle(ChatFormatting.GRAY));
                for (ModDataComponents.GemsEntry entry : gems) {
                    String gemName = getGemDisplayName(entry.id());
                    if (entry.tier() > 1) {
                        tooltip.add(Component.literal(" ").append(Component.literal("\u2666 " + gemName + " (T" + entry.tier() + ")"))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    } else {
                        tooltip.add(Component.literal(" ").append(Component.literal("\u2666 " + gemName))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }

            if (storedBonuses != null && !storedBonuses.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.gear_forge.stored_bonuses_header")
                        .withStyle(ChatFormatting.GRAY));
                Map<String, Double> mergedStored = new LinkedHashMap<>();
                for (ModDataComponents.StoredBonus sb : storedBonuses) {
                    String displayName = getStoredBonusDisplayName(sb.id());
                    mergedStored.merge(displayName, sb.value(), Double::sum);
                }
                for (var entry : mergedStored.entrySet()) {
                    String displayName = entry.getKey();
                    double totalValue = entry.getValue();
                    String rawId = storedBonuses.stream()
                            .filter(sb -> getStoredBonusDisplayName(sb.id()).equals(displayName))
                            .findFirst().map(ModDataComponents.StoredBonus::id).orElse(displayName);
                    String formattedValue;
                    if (isStoredBonusPercentage(rawId)) {
                        formattedValue = fmt(totalValue * 100) + "%";
                    } else {
                        formattedValue = fmt(totalValue);
                    }
                    tooltip.add(Component.literal(" ").append(Component.literal(displayName + ": " + formattedValue))
                            .withStyle(ChatFormatting.GOLD));
                }
            }

        }

        private static String getAffixDisplayName(String id) {
            var config = ModConfigs.getAffixConfig();
            return config.getAffixPool().stream()
                    .filter(a -> a.getId().equals(id))
                    .findFirst()
                    .map(AffixDefinition::getName)
                    .orElse(id);
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

        private static String getGemDisplayName(String id) {
            var config = ModConfigs.getGemConfig();
            for (GemConfig.GemDefinition gem : config.getGems()) {
                if (gem.getId().equals(id)) return gem.getName();
            }
            return id;
        }

        private static String getStoredBonusDisplayName(String id) {
            String name = getAffixDisplayName(id);
            if (!name.equals(id)) return "[转生]" + name;

            if (id.startsWith("gem_")) {
                String base = id.substring(4);
                String baseName = getAffixDisplayName(base);
                if (!baseName.equals(base)) return "[转生]宝石:" + baseName;
                return "[转生]宝石:" + base;
            }

            if (id.startsWith("upgrade_")) {
                String base = id.substring(8);
                String baseName = getAffixDisplayName(base);
                if (!baseName.equals(base)) return "[转生]强化:" + baseName;
                String statName = STAT_NAME_MAP.get(base);
                if (statName != null) return "[转生]强化:" + statName;
                return "[转生]强化:" + base;
            }

            return "[转生]" + id;
        }

        private static final Map<String, String> STAT_NAME_MAP = Map.ofEntries(
                Map.entry("attack_damage", "攻击力"),
                Map.entry("defense", "防御力"),
                Map.entry("toughness", "韧性"),
                Map.entry("knockback_resistance", "击退抗性"),
                Map.entry("movement_speed", "移动速度"),
                Map.entry("attack_speed", "攻击速度"),
                Map.entry("armor", "护甲值"),
                Map.entry("armor_toughness", "护甲韧性"),
                Map.entry("max_health", "最大生命"),
                Map.entry("luck", "幸运值")
        );

        private static boolean isStoredBonusPercentage(String id) {
            String lookup = id;
            if (lookup.startsWith("gem_")) lookup = lookup.substring(4);
            if (lookup.startsWith("upgrade_")) lookup = lookup.substring(8);
            var def = ModConfigs.getAffixDefinition(lookup);
            return def != null && "percentage".equals(def.getType());
        }
    }
}
