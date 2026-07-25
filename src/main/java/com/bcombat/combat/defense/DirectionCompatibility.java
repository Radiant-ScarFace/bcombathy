package com.bcombat.combat.defense;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;

/**
 * Maps between {@link AttackDirection} and {@link GuardDirection}, the
 * same four-way (plus NONE) split used consistently across this
 * framework's attack and block systems. Kept as a small standalone
 * utility rather than added to either enum, since neither {@code attack}
 * nor {@code block} should depend on the other's package.
 */
public final class DirectionCompatibility {

    private DirectionCompatibility() {
        // Utility class, no instances.
    }

    /**
     * @return the guard direction that correctly defends against
     * {@code attackDirection}, or {@link GuardDirection#NONE} if
     * {@code attackDirection} is {@link AttackDirection#NONE}.
     */
    public static GuardDirection matchingGuard(AttackDirection attackDirection) {
        return switch (attackDirection) {
            case LEFT_SLASH -> GuardDirection.LEFT_GUARD;
            case RIGHT_SLASH -> GuardDirection.RIGHT_GUARD;
            case OVERHEAD -> GuardDirection.UP_GUARD;
            case THRUST -> GuardDirection.THRUST_GUARD;
            case NONE -> GuardDirection.NONE;
        };
    }

    /**
     * @return the attack direction that {@code guardDirection} defends
     * against, the inverse of {@link #matchingGuard}. Used by debug/test
     * tooling to derive a plausible incoming attack from a currently-held
     * guard.
     */
    public static AttackDirection matchingAttack(GuardDirection guardDirection) {
        return switch (guardDirection) {
            case LEFT_GUARD -> AttackDirection.LEFT_SLASH;
            case RIGHT_GUARD -> AttackDirection.RIGHT_SLASH;
            case UP_GUARD -> AttackDirection.OVERHEAD;
            case THRUST_GUARD -> AttackDirection.THRUST;
            case NONE -> AttackDirection.NONE;
        };
    }
}