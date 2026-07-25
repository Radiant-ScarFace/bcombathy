package com.bcombat.combat.events;

import com.bcombat.combat.couch.CouchState;
import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.couch.CouchLanceController} the
 * instant a rider voluntarily backs out of couching (via {@code
 * CouchLanceController#cancel()}) while {@link CouchState#PREPARING} or
 * {@link CouchState#ACTIVE} — distinct from {@link CouchInterruptedEvent},
 * which covers an involuntary loss of eligibility instead.
 *
 * @param rider          the mounted combatant who cancelled couching.
 * @param previousState  the {@link CouchState} couching was cancelled from.
 */
public record CouchCancelledEvent(LivingEntity rider, CouchState previousState) {
}