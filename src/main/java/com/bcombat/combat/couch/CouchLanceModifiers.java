package com.bcombat.combat.couch;

import com.bcombat.combat.util.CombatConstants;

/**
 * Pure, stateless gameplay-number companion to {@link CouchLanceController},
 * mirroring the split {@code MountedCombatModifiers} already has with
 * {@code MountedCombatController}: the controller only ever drives the
 * {@link CouchState} state machine and reports state, while every actual
 * number a couched strike produces — its damage bonus, its recovery
 * duration, its impact force — is computed here from the speed ratio the
 * controller freezes at release. Keeping these formulas out of the
 * controller is what lets {@code DamageCalculator} and friends read them
 * without depending on (or duplicating) the state machine itself.
 * <p>
 * Every method takes a {@code speedRatio}: the mount's horizontal speed
 * at the moment in question, expressed as a multiple of {@link
 * CombatConstants#COUCH_MIN_HORSE_SPEED} — the same value {@link
 * CouchLanceController#getImpactSpeedRatio()} freezes for {@link
 * CouchState#IMPACT} and that this class's callers should otherwise
 * source from wherever they read a rider's current mount speed.
 */
public final class CouchLanceModifiers {

    private CouchLanceModifiers() {
    }

    /**
     * @param speedRatio the strike's speed ratio (0.0 if not mounted).
     * @return the damage multiplier a couched strike thrown at {@code
     * speedRatio} should apply on top of the weapon's base damage:
     * {@link CombatConstants#COUCH_DAMAGE_MULTIPLIER} scaled up by up to
     * {@link CombatConstants#COUCH_MAX_SPEED_BONUS} as speed climbs from
     * {@code 1.0} (minimum charge speed) toward {@link
     * CombatConstants#COUCH_MOMENTUM_SPEED_RATIO}, with the flat {@link
     * CombatConstants#COUCH_MOMENTUM_MULTIPLIER} bonus stacked on top
     * once that momentum threshold is met, and the whole result capped
     * at {@link CombatConstants#COUCH_MAX_DAMAGE_CAP}.
     */
    public static double damageMultiplier(double speedRatio) {
        double clamped = Math.max(0.0, speedRatio);
        double speedProgress = clamp01(
                (clamped - 1.0) / Math.max(1.0E-4, CombatConstants.COUCH_MOMENTUM_SPEED_RATIO - 1.0));
        double multiplier = CombatConstants.COUCH_DAMAGE_MULTIPLIER
                * (1.0 + speedProgress * CombatConstants.COUCH_MAX_SPEED_BONUS);
        if (clamped >= CombatConstants.COUCH_MOMENTUM_SPEED_RATIO) {
            multiplier *= CombatConstants.COUCH_MOMENTUM_MULTIPLIER;
        }
        return Math.min(multiplier, CombatConstants.COUCH_MAX_DAMAGE_CAP);
    }

    /**
     * @param speedRatio the strike's speed ratio (0.0 if not mounted).
     * @return the knockback/impact force a couched strike thrown at
     * {@code speedRatio} should apply, scaling {@link
     * CombatConstants#COUCH_IMPACT_FORCE} linearly with speed so a
     * barely-qualifying charge and a full-tilt charge feel distinct.
     */
    public static double impactForce(double speedRatio) {
        double clamped = Math.max(0.0, speedRatio);
        return CombatConstants.COUCH_IMPACT_FORCE * Math.max(1.0, clamped);
    }

    /**
     * @param speedRatio the speed ratio {@code RECOVERY} began at — the
     *                    frozen impact speed if reached from {@link
     *                    CouchState#IMPACT}, or the mount's speed at the
     *                    moment {@code INTERRUPTED}/{@code CANCELLED}
     *                    fell through otherwise (see {@link
     *                    CouchLanceController}'s {@code recoverySpeedRatio}).
     * @return the number of ticks {@code CouchState#RECOVERY} should
     * last: {@link CombatConstants#COUCH_RECOVERY_TICKS} at rest,
     * shrinking toward the floor of {@link
     * CombatConstants#COUCH_MIN_RECOVERY_TICKS} as {@code speedRatio}
     * climbs toward {@link CombatConstants#COUCH_MOMENTUM_SPEED_RATIO} —
     * a charge thrown at full momentum carries the rider through
     * recovery faster than one that barely qualified, or one interrupted
     * while dismounted (speedRatio {@code 0.0}, the longest recovery).
     */
    public static int recoveryTicks(double speedRatio) {
        double clamped = Math.max(0.0, speedRatio);
        double progress = clamp01(clamped / Math.max(1.0E-4, CombatConstants.COUCH_MOMENTUM_SPEED_RATIO));
        double ticks = CombatConstants.COUCH_RECOVERY_TICKS
                - progress * (CombatConstants.COUCH_RECOVERY_TICKS - CombatConstants.COUCH_MIN_RECOVERY_TICKS);
        return (int) Math.round(Math.max(CombatConstants.COUCH_MIN_RECOVERY_TICKS, ticks));
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
