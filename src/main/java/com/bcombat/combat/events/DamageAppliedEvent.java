package com.bcombat.combat.events;

import com.bcombat.combat.damage.DamageResult;

/**
 * Fired the instant {@code DamageApplier} has applied a hit's final
 * damage to the target's health. This is the damage framework's
 * "the hit landed and health changed" notification — the equivalent of
 * {@link AttackHitEvent} one stage further down the pipeline, once a
 * concrete amount has actually been taken from the target rather than
 * just calculated.
 *
 * @param result the full computed breakdown for this hit; {@code result.finalDamage()} is the amount that was applied.
 */
public record DamageAppliedEvent(DamageResult result) {
}