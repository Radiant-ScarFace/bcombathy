package com.bcombat.combat.movement;

/**
 * The two movement behaviors a player can be in. Combat systems built
 * later (stamina drain while sprinting in combat, weapon-weight movement
 * penalties, mount combat, etc.) should branch on this rather than on
 * {@code CombatState} directly, since a state like {@code RECOVERY} is
 * still movement-mode {@code COMBAT} even though it isn't "idle".
 */
public enum MovementMode {
    NORMAL,
    COMBAT
}
