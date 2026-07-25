package com.bcombat.combat.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.couch.CouchLanceController} the
 * instant a rider's couch eligibility (mounted, couch-capable weapon,
 * minimum charge speed, safe terrain) first becomes true — the {@code
 * CouchState.INACTIVE -> CouchState.PREPARING} transition. Whether the
 * lance actually becomes ready to strike is resolved a few ticks later
 * once {@code CombatConstants#COUCH_PREPARE_TICKS} elapses uninterrupted;
 * this event only marks the attempt beginning.
 *
 * @param rider the mounted combatant beginning to couch their weapon.
 * @param mount the entity currently ridden.
 */
public record CouchStartedEvent(LivingEntity rider, Entity mount) {
}