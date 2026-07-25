package com.bcombat.combat.events;

import com.bcombat.combat.damage.DamageResult;

/**
 * Fired the instant {@code DamageCalculator} finishes resolving a
 * confirmed hit's full damage breakdown — body multiplier, typed
 * components, and (if any) armor mitigation already computed — but
 * before that damage has been applied to the target's health. Future
 * systems that want to inspect, log, or even react to a hit's numbers
 * before they land (e.g. a damage-preview HUD) should subscribe here
 * rather than {@link DamageAppliedEvent}.
 *
 * @param result the full computed breakdown for this hit.
 */
public record DamageCalculatedEvent(DamageResult result) {
}