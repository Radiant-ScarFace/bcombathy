package com.bcombat.combat.damage;

import com.bcombat.combat.collision.HitResult;
import com.bcombat.combat.weapon.WeaponProperties;

import java.util.Objects;

/**
 * Stateless utility that turns one confirmed collision outcome ({@link
 * HitResult}) into a fully-resolved {@link DamageResult}. This is the
 * generic damage system's core: every input comes from the hit
 * detection framework ({@link HitResult}) or configuration ({@link
 * DamageConstants}), and every weapon-specific number comes from {@link
 * WeaponProperties} — nothing here hardcodes a damage value for any
 * specific weapon, armor piece, or body part.
 * <p>
 * Deliberately performs no entity mutation and fires no events — see
 * {@link DamageResult}'s class docs for why that separation matters.
 * {@code com.bcombat.combat.damage.DamageService} is what turns a
 * computed result into an actual health change and event notifications.
 * <p>
 * Calculation pipeline, in order:
 * <ol>
 *     <li>Resolve the six-way {@link BodyPart} via {@link BodyPartResolver}.</li>
 *     <li>Read the four raw weapon damage components from {@link WeaponProperties}.</li>
 *     <li>Apply the body part's multiplier and the global damage scale to get pre-armor damage.</li>
 *     <li>Resolve the target's current armor for that body part via {@link ArmorResolver}.</li>
 *     <li>Mitigate each typed component independently against the matching armor resistance.</li>
 *     <li>Apply the critical-hit bonus, if the body part qualifies.</li>
 *     <li>Clamp to the configured minimum damage floor.</li>
 * </ol>
 */
public final class DamageCalculator {

    private DamageCalculator() {
        // Stateless utility, no instances.
    }

    /**
     * Computes the full damage breakdown for {@code hitResult}.
     *
     * @param hitResult a confirmed, unblocked hit (see {@link HitResult#hit()}).
     * @return the resolved {@link DamageResult}.
     * @throws IllegalArgumentException if {@code hitResult} is not a confirmed hit.
     */
    public static DamageResult calculate(HitResult hitResult) {
        Objects.requireNonNull(hitResult, "hitResult must not be null");
        if (!hitResult.hit() || hitResult.target() == null) {
            throw new IllegalArgumentException("DamageCalculator requires a confirmed hit with a non-null target");
        }

        BodyPart bodyPart = BodyPartResolver.resolve(hitResult);
        WeaponProperties weapon = hitResult.weaponProperties();

        double base = weapon.baseDamage();
        double cut = weapon.cutDamage();
        double pierce = weapon.pierceDamage();
        double blunt = weapon.bluntDamage();

        double bodyMultiplier = DamageConstants.multiplierFor(bodyPart);
        double scale = DamageConstants.DEFAULT_DAMAGE_SCALE * DamageConstants.DEFAULT_DIFFICULTY_DAMAGE_MODIFIER;

        double scaledBase = base * bodyMultiplier * scale;
        double scaledCut = cut * bodyMultiplier * scale;
        double scaledPierce = pierce * bodyMultiplier * scale;
        double scaledBlunt = blunt * bodyMultiplier * scale;
        double preArmorDamage = scaledBase + scaledCut + scaledPierce + scaledBlunt;

        ArmorProperties armor = ArmorResolver.resolveForBodyPart(hitResult.target(), bodyPart);

        // The untyped base component has no single matching damage type
        // to mitigate it against, so it is reduced by the average of the
        // three typed resistances - a deliberate, documented choice
        // rather than letting it bypass armor entirely.
        double postArmorBase = mitigate(scaledBase, averageResistance(armor));
        double postArmorCut = mitigate(scaledCut, armor.cutResistance());
        double postArmorPierce = mitigate(scaledPierce, armor.pierceResistance());
        double postArmorBlunt = mitigate(scaledBlunt, armor.bluntResistance());
        double postArmorDamage = postArmorBase + postArmorCut + postArmorPierce + postArmorBlunt;

        double armorReductionAmount = preArmorDamage - postArmorDamage;

        boolean critical = DamageConstants.CRITICAL_HIT_BODY_PARTS.contains(bodyPart);
        double criticalMultiplier = critical ? DamageConstants.CRITICAL_HIT_MULTIPLIER : 1.0;
        double afterCritical = postArmorDamage * criticalMultiplier;

        double finalDamage = Math.max(DamageConstants.MINIMUM_DAMAGE, afterCritical);

        boolean staggerTriggered = finalDamage >= DamageConstants.STAGGER_DAMAGE_THRESHOLD;

        return new DamageResult(
                hitResult,
                bodyPart,
                base,
                cut,
                pierce,
                blunt,
                bodyMultiplier,
                preArmorDamage,
                armor.isNone() ? null : armor,
                armorReductionAmount,
                postArmorDamage,
                critical,
                criticalMultiplier,
                finalDamage,
                staggerTriggered);
    }

    /**
     * Applies the diminishing-returns armor mitigation curve documented
     * on {@link DamageConstants#ARMOR_EFFECTIVENESS_CONSTANT} to one
     * already body/scale-adjusted damage component.
     */
    private static double mitigate(double scaledDamage, double resistance) {
        if (scaledDamage <= 0.0) {
            return 0.0;
        }
        double reductionRatio = resistance / (resistance + DamageConstants.ARMOR_EFFECTIVENESS_CONSTANT);
        double cappedRatio = Math.min(reductionRatio, 1.0 - DamageConstants.ARMOR_MIN_DAMAGE_RATIO);
        return scaledDamage * (1.0 - cappedRatio);
    }

    private static double averageResistance(ArmorProperties armor) {
        return (armor.cutResistance() + armor.pierceResistance() + armor.bluntResistance()) / 3.0;
    }
}