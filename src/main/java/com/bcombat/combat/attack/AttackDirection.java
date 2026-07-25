package com.bcombat.combat.attack;

/**
 * The four Bannerlord-style strike directions a player can commit to
 * during {@code CombatState.PREPARING_ATTACK}, plus {@link #NONE} for
 * "no direction chosen yet" (deadzone not exceeded).
 * <p>
 * This lives in the common package rather than client-only input code
 * because it is part of a player's combat state, not just an input
 * detail — a future networking phase needs to sync it, and future
 * weapon/damage phases need to read it, same as {@code CombatState}.
 */
public enum AttackDirection {
    NONE,
    LEFT_SLASH,
    RIGHT_SLASH,
    OVERHEAD,
    THRUST
}
