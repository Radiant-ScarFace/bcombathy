package com.bcombat.combat.defense;

/**
 * Outcome of {@code CombatController#notifyIncomingAttack}, so a future
 * hit-detection/AI/networking system can react appropriately (e.g. skip
 * damage application on {@link #PARRY}) without needing to poll combat
 * state afterward.
 */
public enum DefenseResult {

    /** Nothing about this notification qualified for any defensive mechanic. */
    NONE,

    /** The defender's guard was correctly directed and timed; {@code CombatState.PERFECT_BLOCK}. */
    PERFECT_BLOCK,

    /** A Perfect Block landed within the tighter Parry window; {@code CombatState.PARRY}. */
    PARRY,

    /**
     * The defender's attack direction matched the incoming attack and a
     * chamber attempt began ({@code CombatState.CHAMBER_PREPARE}). This
     * is an acknowledgement, not a guarantee of success — subscribe to
     * {@code ChamberSucceededEvent} for the resolved outcome, since
     * timing is only confirmed a few ticks later.
     */
    CHAMBER_STARTED
}