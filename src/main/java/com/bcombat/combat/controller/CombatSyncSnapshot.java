package com.bcombat.combat.controller;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import com.bcombat.combat.movement.MovementMode;
import com.bcombat.combat.state.CombatState;

import java.util.UUID;

/**
 * A compact, wire-friendly snapshot of everything a non-authoritative
 * {@link CombatController} (client-side prediction of the local player,
 * or a purely network-driven mirror of a remote player) needs to bring
 * itself in line with the server's authoritative copy.
 * <p>
 * Captured via {@link CombatController#captureSnapshot()} on the
 * server's authoritative controller the instant {@code CombatState},
 * attack direction, or guard direction actually changes, and applied via
 * {@link CombatController#applySnapshot(CombatSyncSnapshot)} on every
 * client. Deliberately excludes anything that doesn't need its own
 * packet: stamina/exhaustion has its own lower-frequency, throttled
 * {@link StaminaSyncSnapshot}, and the equipped weapon/armor need no
 * packet at all since vanilla already synchronizes held items and worn
 * armor to every client - see {@code com.bcombat.network.ServerCombatNetworking}'s
 * class docs for the full rationale.
 *
 * @param playerId               the player this snapshot describes.
 * @param state                  the authoritative {@link CombatState}.
 * @param attackDirection        the committed (or in-progress) attack direction.
 * @param guardDirection         the locked guard direction.
 * @param movementMode           the current {@link MovementMode}.
 * @param transitionTicksRemaining the authoritative countdown remaining
 *                               on the current state's timer, so a
 *                               receiving client's own timer resumes
 *                               from the correct point rather than
 *                               restarting the full duration.
 */
public record CombatSyncSnapshot(
        UUID playerId,
        CombatState state,
        AttackDirection attackDirection,
        GuardDirection guardDirection,
        MovementMode movementMode,
        int transitionTicksRemaining
) {
}