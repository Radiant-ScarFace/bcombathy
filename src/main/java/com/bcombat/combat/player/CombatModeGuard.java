package com.bcombat.combat.player;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Centralizes the conditions under which Combat Mode must be forcibly
 * exited regardless of player input, so this logic lives in exactly one
 * place rather than being duplicated wherever the controller ticks.
 * <p>
 * Per the design spec: swimming automatically returns the combatant to
 * normal movement, and flying disables combat mode outright. Flying is
 * a {@link PlayerEntity}-only concept (creative/spectator abilities) —
 * AI-controlled combatants (plain {@link LivingEntity}s) can never fly,
 * so the check simply never applies to them rather than requiring a
 * separate code path.
 */
public final class CombatModeGuard {

    private CombatModeGuard() {
        // Utility class, no instances.
    }

    /**
     * @return true if the combatant's current state requires Combat
     * Mode to be forcibly exited (swimming, or - for a player - flying).
     */
    public static boolean shouldForceExitCombat(LivingEntity entity) {
        if (entity.isSwimming()) {
            return true;
        }
        return entity instanceof PlayerEntity player && player.getAbilities().flying;
    }
}