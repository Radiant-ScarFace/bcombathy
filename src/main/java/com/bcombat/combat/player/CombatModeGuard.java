package com.bcombat.combat.player;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Centralizes the conditions under which Combat Mode must be forcibly
 * exited regardless of player input, so this logic lives in exactly one
 * place rather than being duplicated wherever the controller ticks.
 * <p>
 * Per the design spec: swimming automatically returns the player to
 * normal movement, and flying disables combat mode outright.
 */
public final class CombatModeGuard {

    private CombatModeGuard() {
        // Utility class, no instances.
    }

    /**
     * @return true if the player's current state requires Combat Mode to
     * be forcibly exited (swimming or flying).
     */
    public static boolean shouldForceExitCombat(PlayerEntity player) {
        return player.isSwimming() || player.getAbilities().flying;
    }
}
