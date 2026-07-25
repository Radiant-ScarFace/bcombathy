package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;

/**
 * Fired the instant a swing's collision check finds a candidate target,
 * before it's known whether that target's defense will intercept it.
 * Distinct from {@link AttackHitEvent}/{@link AttackBlockedEvent}, which
 * fire only after that resolution — this event exists for future systems
 * (VFX, sound cues, AI reactions) that care about "something was struck"
 * independent of the eventual hit/blocked outcome.
 *
 * @param attacker   the combatant (player or AI) who threw the attack.
 * @param target     the entity the collision check found.
 * @param direction  the attack direction committed for this swing.
 * @param weaponItem the item that performed the attack, or {@code null} if unarmed.
 */
public record CollisionDetectedEvent(LivingEntity attacker, LivingEntity target, AttackDirection direction, Item weaponItem) {
}