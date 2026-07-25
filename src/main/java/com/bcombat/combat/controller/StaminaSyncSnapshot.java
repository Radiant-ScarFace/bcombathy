package com.bcombat.combat.controller;

import java.util.UUID;

/**
 * A compact, wire-friendly snapshot of a player's stamina/exhaustion
 * state, captured via {@link CombatController#captureStaminaSnapshot()}
 * on the server's authoritative controller and applied via {@link
 * CombatController#applyStaminaSnapshot(StaminaSyncSnapshot)} on every
 * client. Kept as its own packet, separate from {@link
 * CombatSyncSnapshot}, since stamina changes far more often (every
 * regeneration tick) than the state machine does, so it is broadcast on
 * its own lower-frequency, throttled schedule - see {@code
 * com.bcombat.network.ServerCombatNetworking}'s class docs.
 *
 * @param playerId       the player this snapshot describes.
 * @param currentStamina the authoritative current stamina value.
 * @param maxStamina     the authoritative maximum stamina value.
 * @param exhausted      whether the player is currently {@code ExhaustionState#EXHAUSTED}.
 */
public record StaminaSyncSnapshot(
        UUID playerId,
        double currentStamina,
        double maxStamina,
        boolean exhausted
) {
}