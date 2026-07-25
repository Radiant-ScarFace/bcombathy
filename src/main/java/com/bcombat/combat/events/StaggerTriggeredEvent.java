package com.bcombat.combat.events;

import com.bcombat.combat.damage.DamageResult;

/**
 * Fired whenever a hit's final damage met {@code
 * DamageConstants#STAGGER_DAMAGE_THRESHOLD}, as computed by {@code
 * DamageCalculator}. This is an extension point only: no stagger
 * behavior (movement interruption, animation reaction, wind-up
 * cancellation, etc.) is implemented anywhere in this phase — a future
 * stagger system is expected to subscribe to this event and decide what
 * "staggered" actually does, exactly as the design brief requires.
 *
 * @param result the full computed breakdown for the hit that triggered this; {@code result.staggerTriggered()} is always true here.
 */
public record StaggerTriggeredEvent(DamageResult result) {
}