package com.bcombat.combat.block;

/**
 * The four Bannerlord-style guard directions a player can hold during
 * {@code CombatState.ENTER_BLOCK} / {@code CombatState.BLOCK_IDLE}, plus
 * {@link #NONE} for "no direction locked yet" (deadzone not exceeded, or
 * block was just entered).
 * <p>
 * Lives in a common package rather than client-only input code for the
 * same reason {@code AttackDirection} does: it is part of a player's
 * combat state, not just an input detail. A future networking phase needs
 * to sync it, and future hit-detection/parry phases need to read it.
 */
public enum GuardDirection {
    NONE,
    LEFT_GUARD,
    RIGHT_GUARD,
    UP_GUARD,
    THRUST_GUARD
}