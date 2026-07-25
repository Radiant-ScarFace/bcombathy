package com.bcombat.combat.events;

import com.bcombat.combat.collision.HitResult;

/**
 * Fired once per attack whose collision window (see {@code
 * CollisionController}) closed without finding any valid target — the
 * attacker swung and connected with nothing.
 *
 * @param result the full resolved outcome; {@code result.target()} is always null here.
 */
public record AttackMissEvent(HitResult result) {
}