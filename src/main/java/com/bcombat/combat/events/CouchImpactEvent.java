package com.bcombat.combat.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.couch.CouchLanceController} the
 * instant a braced ({@link com.bcombat.combat.couch.CouchState#ACTIVE})
 * couched lance is released into the normal attack pipeline via {@code
 * CombatController#beginCouchAttackRelease} — the {@code ACTIVE ->
 * IMPACT} transition. This marks the strike being thrown, not
 * necessarily a confirmed hit; whether it actually connects is resolved
 * by the same {@code AttackHitEvent}/{@code AttackMissEvent}/{@code
 * AttackBlockedEvent} trio every other attack resolves through, with
 * {@code com.bcombat.combat.damage.DamageCalculator} applying the couch
 * damage bonus (see {@code CouchLanceModifiers#damageMultiplier}) only
 * if a hit is confirmed while this rider is in {@code IMPACT}.
 *
 * @param rider           the mounted combatant unleashing the couched strike.
 * @param mount           the entity currently ridden.
 * @param mountSpeedRatio the mount's horizontal speed at the moment of
 *                        release, expressed as a multiple of {@code
 *                        CombatConstants#COUCH_MIN_HORSE_SPEED} — the
 *                        same ratio {@code CouchLanceModifiers} uses to
 *                        scale the damage/recovery bonuses this strike
 *                        will receive.
 */
public record CouchImpactEvent(LivingEntity rider, Entity mount, double mountSpeedRatio) {
}