package com.bcombat.combat.events;

import com.bcombat.combat.collision.HitResult;

/**
 * Fired the instant an attack's collision check finds a target and the
 * defender's block/parry/chamber did not intercept it — i.e. a
 * confirmed, unblocked hit. Carries no damage of its own; a future
 * damage phase is the expected consumer of {@link #result()}.
 *
 * @param result the full resolved outcome; {@code result.hit()} is always true here.
 */
public record AttackHitEvent(HitResult result) {
}