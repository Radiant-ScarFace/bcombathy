package com.bcombat.combat.damage;

import com.bcombat.combat.collision.HitResult;
import net.minecraft.entity.LivingEntity;

/**
 * The single reusable data container describing the fully-resolved
 * damage outcome of one confirmed hit — the damage-framework equivalent
 * of {@code com.bcombat.combat.collision.HitResult}, which it wraps
 * rather than duplicates. Produced exclusively by {@link
 * DamageCalculator#calculate}, which performs no entity mutation and
 * fires no events; this record is the entire handoff between
 * calculation and every downstream consumer (event listeners, {@link
 * DamageApplier}), keeping "compute the numbers" and "act on the
 * numbers" strictly separate.
 * <p>
 * Every stage of the pipeline is preserved rather than collapsed into
 * just the final number, so a listener can inspect exactly how a hit's
 * damage was built up (raw weapon output, body multiplier, armor
 * mitigation, critical bonus) without recomputing anything.
 *
 * @param hitResult            the collision framework's outcome this damage was derived from.
 * @param bodyPart             the resolved six-way body region struck; see {@link BodyPartResolver}.
 * @param baseRawDamage        the weapon's untyped {@code baseDamage()}, before any multiplier.
 * @param cutRawDamage         the weapon's raw {@code cutDamage()}, before any multiplier.
 * @param pierceRawDamage      the weapon's raw {@code pierceDamage()}, before any multiplier.
 * @param bluntRawDamage       the weapon's raw {@code bluntDamage()}, before any multiplier.
 * @param bodyPartMultiplier   the multiplier applied for {@code bodyPart}; see {@link DamageConstants#multiplierFor}.
 * @param preArmorDamage       total damage after body multiplier and global scaling, before armor mitigation.
 * @param armorApplied         the combined armor protecting {@code bodyPart} at the moment of the hit, or {@code null} if none.
 * @param armorReductionAmount the absolute amount of damage armor removed ({@code preArmorDamage - postArmorDamage}).
 * @param postArmorDamage      total damage after armor mitigation, before the critical-hit bonus.
 * @param critical             true if this hit qualified as a critical hit; see {@link DamageConstants#CRITICAL_HIT_BODY_PARTS}.
 * @param criticalMultiplier   the multiplier applied for the critical bonus (1.0 if {@code critical} is false).
 * @param finalDamage          the final damage amount, clamped to {@link DamageConstants#MINIMUM_DAMAGE} — what {@link DamageApplier} applies.
 * @param staggerTriggered     true if {@code finalDamage} met {@link DamageConstants#STAGGER_DAMAGE_THRESHOLD}; an
 *                             extension-point flag only — no stagger behavior is implemented yet.
 */
public record DamageResult(
        HitResult hitResult,
        BodyPart bodyPart,
        double baseRawDamage,
        double cutRawDamage,
        double pierceRawDamage,
        double bluntRawDamage,
        double bodyPartMultiplier,
        double preArmorDamage,
        ArmorProperties armorApplied,
        double armorReductionAmount,
        double postArmorDamage,
        boolean critical,
        double criticalMultiplier,
        double finalDamage,
        boolean staggerTriggered) {

    /** @return the combatant (player or AI) who dealt this damage. */
    public LivingEntity attacker() {
        return hitResult.attacker();
    }

    /** @return the entity this damage was calculated against. */
    public LivingEntity target() {
        return hitResult.target();
    }
}