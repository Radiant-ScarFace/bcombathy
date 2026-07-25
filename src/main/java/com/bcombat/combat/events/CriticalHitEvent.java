package com.bcombat.combat.events;

import com.bcombat.combat.damage.DamageResult;

/**
 * Fired whenever a hit's body part qualified as a critical hit (see
 * {@code DamageConstants#CRITICAL_HIT_BODY_PARTS}), before damage is
 * applied. Explicitly out of scope for this phase to react to this with
 * any presentation (VFX, sound, a UI callout) — this event exists purely
 * as the extension point a future system will use for that.
 *
 * @param result the full computed breakdown for this hit; {@code result.critical()} is always true here.
 */
public record CriticalHitEvent(DamageResult result) {
}