package com.bcombat.combat.stamina;

/**
 * A player's binary exhaustion status, as tracked by {@link StaminaController}.
 * Kept as its own type (rather than a raw {@code boolean}) so a future
 * phase can introduce intermediate tiers (e.g. {@code WINDED}) without
 * changing every call site that currently only needs "exhausted or not" —
 * {@link #isExhausted()} is the stable boolean view every existing caller
 * should keep using.
 */
public enum ExhaustionState {

    /** Stamina is available; attacks and blocks may be freely initiated. */
    NORMAL,

    /**
     * Stamina has been fully depleted. New attacks and new blocks are
     * refused by {@code CombatController} while in this state; movement
     * speed is further reduced by {@code MovementModifierManager}. Left
     * automatically once enough stamina has regenerated — see
     * {@code CombatConstants#EXHAUSTION_RECOVERY_THRESHOLD_RATIO}.
     */
    EXHAUSTED;

    public boolean isExhausted() {
        return this == EXHAUSTED;
    }
}