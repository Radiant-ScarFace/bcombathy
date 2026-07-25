package com.bcombat.combat.stamina;

import com.bcombat.combat.util.CombatConstants;

/**
 * The dedicated per-player controller for the stamina/combat-fatigue
 * system, inspired primarily by Mount &amp; Blade II: Bannerlord. Owned by
 * {@code CombatController} exactly the same way it owns {@link
 * com.bcombat.combat.block.BlockController} and {@link
 * com.bcombat.combat.movement.MovementModifierManager} — this class has
 * no knowledge of {@code CombatStateManager}, events, or Minecraft
 * entities, only of the stamina value itself, which keeps it trivially
 * testable and reusable (e.g. for a future AI-controlled combatant).
 * <p>
 * Responsibilities:
 * <ul>
 *     <li>Own current/maximum stamina and the {@link ExhaustionState}.</li>
 *     <li>Apply consumption ({@link #consume}), always clamped to zero.</li>
 *     <li>Regenerate over time ({@link #tick}), honoring an explicit
 *     suspension flag (attacking/blocking, decided upstream by {@code
 *     CombatController}) and a post-consumption delay before regen
 *     resumes.</li>
 *     <li>Transition into/out of {@link ExhaustionState#EXHAUSTED}
 *     automatically. Entering requires stamina to hit exactly zero;
 *     leaving requires it to climb back above {@link
 *     CombatConstants#EXHAUSTION_RECOVERY_THRESHOLD_RATIO} of maximum,
 *     so exhaustion cannot be shrugged off the instant a single tick of
 *     regen ticks over.</li>
 * </ul>
 * <p>
 * All base timing/rate values are read from {@link CombatConstants} so
 * every numeric knob stays centrally configurable, per the framework's
 * existing convention. Weapon-driven scaling is applied by the caller
 * (see {@code WeaponProperties#staminaModifier()}, {@code
 * #staminaRegenDelayModifier()}, {@code #staminaRegenRateModifier()}) —
 * this class only ever receives already-scaled multipliers, exactly the
 * way {@link com.bcombat.combat.controller.CombatController} already
 * combines weapon modifiers with base timing for attack/recovery
 * durations.
 * <p>
 * Two additional multipliers, {@link #getBonusRegenRateMultiplier()} and
 * {@link #getBonusRegenDelayMultiplier()}, are reserved extension points
 * for future perks, skills, buffs, or equipment that are not weapon-tied
 * (e.g. an "Endurance" skill or a stamina potion) — see {@link
 * #setBonusRegenRateMultiplier} / {@link #setBonusRegenDelayMultiplier}.
 * They default to neutral (1.0) and stack multiplicatively with the
 * weapon modifier passed into {@link #tick}.
 */
public final class StaminaController {

    private double maxStamina = CombatConstants.DEFAULT_MAX_STAMINA;
    private double currentStamina = maxStamina;
    private ExhaustionState exhaustionState = ExhaustionState.NORMAL;

    /**
     * Ticks elapsed since the last time stamina was consumed. Regen is
     * gated on this reaching the effective (weapon/perk-scaled) regen
     * delay, independent of the explicit {@code regenSuspended} flag
     * {@link #tick} also honors.
     */
    private int ticksSinceLastConsumption = CombatConstants.STAMINA_REGEN_DELAY_TICKS;

    private double bonusRegenRateMultiplier = 1.0;
    private double bonusRegenDelayMultiplier = 1.0;

    /**
     * Resets stamina to full and clears exhaustion. Not called
     * automatically on any combat-state transition — unlike guard
     * direction, stamina is a persistent character resource that should
     * survive entering/leaving Combat Mode, so only an explicit future
     * caller (e.g. respawn handling) should invoke this.
     */
    public void reset() {
        currentStamina = maxStamina;
        ticksSinceLastConsumption = CombatConstants.STAMINA_REGEN_DELAY_TICKS;
        exhaustionState = ExhaustionState.NORMAL;
    }

    /**
     * Deducts {@code amount} stamina, clamped so it never drops below
     * zero, and resets the post-consumption regen delay. Safe to call
     * with an amount that would exceed the remaining stamina — the
     * action that spent it is never retroactively refused here; gating
     * whether an action is *allowed* to begin while {@link
     * ExhaustionState#EXHAUSTED} is the caller's responsibility (see
     * {@code CombatController#requestPrepareAttack}/{@code
     * #requestEnterBlock}).
     *
     * @return the resulting current stamina, for convenience.
     */
    public double consume(double amount) {
        if (amount <= 0.0) {
            return currentStamina;
        }
        currentStamina = Math.max(0.0, currentStamina - amount);
        ticksSinceLastConsumption = 0;
        updateExhaustionState();
        return currentStamina;
    }

    /**
     * Advances regeneration by one tick. Must be called every tick this
     * controller is active, mirroring every other per-tick controller in
     * the framework (e.g. {@link com.bcombat.combat.block.BlockController#tick()}).
     *
     * @param regenSuspended        true while an upstream state (attacking,
     *                               holding a block) explicitly forbids
     *                               regeneration this tick regardless of
     *                               the post-consumption delay.
     * @param weaponRegenDelayModifier multiplier applied to {@link
     *                               CombatConstants#STAMINA_REGEN_DELAY_TICKS},
     *                               from the equipped weapon.
     * @param weaponRegenRateModifier  multiplier applied to {@link
     *                               CombatConstants#STAMINA_REGEN_RATE_PER_TICK},
     *                               from the equipped weapon.
     */
    public void tick(boolean regenSuspended, double weaponRegenDelayModifier, double weaponRegenRateModifier) {
        if (ticksSinceLastConsumption < Integer.MAX_VALUE) {
            ticksSinceLastConsumption++;
        }

        if (!regenSuspended && currentStamina < maxStamina) {
            int effectiveDelay = Math.max(0, (int) Math.round(
                    CombatConstants.STAMINA_REGEN_DELAY_TICKS * weaponRegenDelayModifier * bonusRegenDelayMultiplier));

            if (ticksSinceLastConsumption >= effectiveDelay) {
                double rate = CombatConstants.STAMINA_REGEN_RATE_PER_TICK
                        * weaponRegenRateModifier
                        * bonusRegenRateMultiplier;
                currentStamina = Math.min(maxStamina, currentStamina + rate);
            }
        }

        updateExhaustionState();
    }

    private void updateExhaustionState() {
        if (exhaustionState == ExhaustionState.NORMAL && currentStamina <= 0.0) {
            exhaustionState = ExhaustionState.EXHAUSTED;
        } else if (exhaustionState == ExhaustionState.EXHAUSTED
                && currentStamina >= maxStamina * CombatConstants.EXHAUSTION_RECOVERY_THRESHOLD_RATIO) {
            exhaustionState = ExhaustionState.NORMAL;
        }
    }

    public double getCurrentStamina() {
        return currentStamina;
    }

    public double getMaxStamina() {
        return maxStamina;
    }

    /**
     * Sets a new maximum stamina, e.g. from a future perk, skill, buff,
     * or piece of equipment. Current stamina is clamped down if it would
     * otherwise exceed the new maximum, but is never increased to fill a
     * raised maximum automatically — exactly like Minecraft's own max
     * health handles a max-health increase.
     */
    public void setMaxStamina(double maxStamina) {
        this.maxStamina = Math.max(0.0, maxStamina);
        if (currentStamina > this.maxStamina) {
            currentStamina = this.maxStamina;
        }
        updateExhaustionState();
    }

    /** @return current stamina as a fraction (0-1) of maximum. Extension point for a future HUD. */
    public double getStaminaRatio() {
        return maxStamina <= 0.0 ? 0.0 : currentStamina / maxStamina;
    }

    public ExhaustionState getExhaustionState() {
        return exhaustionState;
    }

    public boolean isExhausted() {
        return exhaustionState.isExhausted();
    }

    /**
     * Directly overwrites current/maximum stamina and exhaustion state,
     * bypassing {@link #consume}/{@link #tick}'s own bookkeeping (the
     * post-consumption regen delay is left untouched). Reserved for
     * exactly one caller: a network-driven {@code CombatController} on a
     * non-authoritative (client) instance applying a {@code
     * StaminaSyncSnapshot} received from the server, which is already
     * the true value and must never be second-guessed locally.
     */
    public void applyAuthoritative(double currentStamina, double maxStamina, ExhaustionState exhaustionState) {
        this.maxStamina = Math.max(0.0, maxStamina);
        this.currentStamina = Math.max(0.0, Math.min(currentStamina, this.maxStamina));
        this.exhaustionState = exhaustionState;
    }

    public double getBonusRegenRateMultiplier() {
        return bonusRegenRateMultiplier;
    }

    /** @see #getBonusRegenRateMultiplier() */
    public void setBonusRegenRateMultiplier(double bonusRegenRateMultiplier) {
        this.bonusRegenRateMultiplier = bonusRegenRateMultiplier;
    }

    public double getBonusRegenDelayMultiplier() {
        return bonusRegenDelayMultiplier;
    }

    /** @see #getBonusRegenDelayMultiplier() */
    public void setBonusRegenDelayMultiplier(double bonusRegenDelayMultiplier) {
        this.bonusRegenDelayMultiplier = bonusRegenDelayMultiplier;
    }
}