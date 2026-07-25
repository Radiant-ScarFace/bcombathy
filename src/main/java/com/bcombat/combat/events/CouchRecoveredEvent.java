package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.couch.CouchLanceController} the
 * instant a rider's post-impact/interrupt/cancel {@code
 * CouchState#RECOVERY} completes and couching returns to {@code
 * CouchState#INACTIVE} — the rider is once again free to begin
 * preparing a new couched charge the moment eligibility conditions are
 * next met.
 *
 * @param rider the mounted combatant who has finished recovering.
 */
public record CouchRecoveredEvent(LivingEntity rider) {
}