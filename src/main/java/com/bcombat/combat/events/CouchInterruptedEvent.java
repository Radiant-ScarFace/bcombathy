package com.bcombat.combat.events;

import com.bcombat.combat.couch.CouchState;
import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.couch.CouchLanceController} the
 * instant a rider's couch eligibility is lost involuntarily — dismounted,
 * mount speed dropped below {@code CombatConstants#COUCH_MIN_HORSE_SPEED},
 * or terrain safety failed — while {@link CouchState#PREPARING} or
 * {@link CouchState#ACTIVE}. Distinct from {@link CouchCancelledEvent},
 * which covers a voluntary cancellation instead.
 *
 * @param rider         the mounted combatant whose couch was interrupted.
 * @param previousState the {@link CouchState} couching was interrupted from.
 */
public record CouchInterruptedEvent(LivingEntity rider, CouchState previousState) {
}