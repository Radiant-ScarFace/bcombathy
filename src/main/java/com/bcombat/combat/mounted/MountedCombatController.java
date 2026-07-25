package com.bcombat.combat.mounted;

import com.bcombat.combat.controller.CombatController;
import com.bcombat.combat.controller.CombatControllerManager;
import com.bcombat.combat.util.CombatConstants;
import net.minecraft.entity.LivingEntity;

/**
 * Stateless utility that turns "is this combatant currently mounted" —
 * as reported by {@link MountedCombatController} — into the actual
 * numeric modifiers applied on top of ground-combat baseline values,
 * exactly the way {@code WeaponProperties} is the sole source of
 * weapon-driven scaling. Every method here is a pure function of a
 * {@link MountedCombatController} (or a raw boolean) and the current
 * {@link CombatConstants#MOUNTED_*} configuration — no entity mutation,
 * no events, nothing stateful — so {@code CombatController} and {@code
 * DamageCalculator} can call these inline at their existing scaling
 * points without this class needing to know anything about combat
 * state, timing, or damage pipelines itself.
 * <p>
 * Every {@code *Ticks}/{@code *Modifier} method here stacks
 * multiplicatively with whatever weapon-driven modifier the caller
 * already applies — mounted combat never replaces a weapon's own
 * timing/stamina/damage scaling, it layers on top of it, mirroring how
 * {@link com.bcombat.combat.stamina.StaminaController}'s bonus
 * multipliers already stack with weapon modifiers.
 */
public final class MountedCombatModifiers {

    private MountedCombatModifiers() {
        // Stateless utility, no instances.
    }

    /**
     * @return {@code reach} scaled by {@link
     * CombatConstants#MOUNTED_REACH_MODIFIER} if {@code mounted} is
     * true, otherwise {@code reach} unchanged.
     */
    public static double applyReachModifier(boolean mounted, double reach) {
        return mounted ? reach * CombatConstants.MOUNTED_REACH_MODIFIER : reach;
    }

    /**
     * @return {@code baseTicks} scaled by {@link
     * CombatConstants#MOUNTED_WIND_UP_MODIFIER} if {@code mounted} is
     * true, otherwise {@code baseTicks} unchanged. Rounding/clamping to
     * a minimum of 1 tick is left to the caller's own {@code
     * scaledTicks} helper, exactly like every other weapon-scaled
     * timing value.
     */
    public static double windUpModifier(boolean mounted) {
        return mounted ? CombatConstants.MOUNTED_WIND_UP_MODIFIER : 1.0;
    }

    /** @return the release (ATTACKING duration) multiplier while mounted, 1.0 otherwise. */
    public static double releaseModifier(boolean mounted) {
        return mounted ? CombatConstants.MOUNTED_RELEASE_MODIFIER : 1.0;
    }

    /** @return the RECOVERY duration multiplier while mounted, 1.0 otherwise. */
    public static double recoveryModifier(boolean mounted) {
        return mounted ? CombatConstants.MOUNTED_RECOVERY_MODIFIER : 1.0;
    }

    /**
     * @return {@code amount} scaled by {@link
     * CombatConstants#MOUNTED_STAMINA_COST_MODIFIER} if {@code mounted}
     * is true, otherwise {@code amount} unchanged. Applied to every
     * discrete stamina cost (attack, block, perfect block, parry,
     * chamber) the same way {@code CombatController#consumeStaminaForAction}
     * already applies weapon-driven scaling upstream of this call.
     */
    public static double applyStaminaCostModifier(boolean mounted, double amount) {
        return mounted ? amount * CombatConstants.MOUNTED_STAMINA_COST_MODIFIER : amount;
    }

    /**
     * @return the stamina regeneration rate multiplier while mounted,
     * 1.0 otherwise — stacks multiplicatively with the equipped
     * weapon's own {@code staminaRegenRateModifier()} exactly the way
     * {@code StaminaController}'s bonus multiplier already does.
     */
    public static double staminaRegenRateModifier(boolean mounted) {
        return mounted ? CombatConstants.MOUNTED_STAMINA_REGEN_RATE_MODIFIER : 1.0;
    }

    /**
     * @return the final-damage multiplier for an attack thrown by
     * {@code attacker}, combining {@link
     * CombatConstants#MOUNTED_DAMAGE_MULTIPLIER} (if mounted at all)
     * with the additional {@link
     * CombatConstants#MOUNTED_CHARGE_DAMAGE_BONUS} once the mount's
     * horizontal speed meets {@link
     * CombatConstants#MOUNTED_CHARGE_SPEED_THRESHOLD} — a stationary
     * mount grants the base mounted bonus only, a moving/charging one
     * grants both, and an unmounted attacker gets neither (returns
     * 1.0).
     */
    public static double damageMultiplier(LivingEntity attacker) {
        CombatController controller = CombatControllerManager.getIfPresent(attacker);
        if (controller == null || !controller.isMounted()) {
            return 1.0;
        }

        double multiplier = CombatConstants.MOUNTED_DAMAGE_MULTIPLIER;
        if (controller.getMountSpeed() >= CombatConstants.MOUNTED_CHARGE_SPEED_THRESHOLD) {
            multiplier *= CombatConstants.MOUNTED_CHARGE_DAMAGE_BONUS;
        }
        return multiplier;
    }
}