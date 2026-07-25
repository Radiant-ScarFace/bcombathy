package com.bcombat.combat.ai;

/**
 * The tactical positioning decision {@link AICombatController} is
 * currently acting on, exposed read-only purely for observability/
 * debugging (e.g. a future debug HUD or {@code /bcombat ai} command
 * output) — nothing in the combat framework itself branches on this;
 * it is a reflection of {@link AICombatController}'s last positioning
 * decision, not an input to it.
 */
public enum AITacticalIntent {

    /** No target, or target out of engagement range — holding position. */
    IDLE,

    /** Target is farther than the ideal fighting distance — closing in. */
    APPROACHING,

    /** Within the ideal fighting distance band — holding ground and facing the target. */
    HOLDING,

    /** Too close and/or too fatigued to safely stay this close — backing off. */
    RETREATING,

    /** Mid-swing, mid-defensive-reaction, or otherwise committed — positioning is frozen. */
    COMMITTED
}