package com.bcombat.combat.events;

import com.bcombat.combat.collision.HitResult;

/**
 * Fired when an attack's collision check found a valid target, but the
 * defender's currently locked guard or wind-up qualified as a Perfect
 * Block, Parry, or Chamber (resolved via the same {@code
 * CombatController#notifyIncomingAttack} the defensive framework already
 * exposes — this event is the collision system's caller of that
 * extension point, not a new defensive mechanic). Normal hit
 * confirmation ({@link AttackHitEvent}) is not fired for this swing.
 *
 * @param result the full resolved outcome; {@code result.blocked()} is
 *               always true and {@code result.defenseResult()} is never
 *               {@code DefenseResult.NONE} here.
 */
public record AttackBlockedEvent(HitResult result) {
}