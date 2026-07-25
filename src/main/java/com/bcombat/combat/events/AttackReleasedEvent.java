package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.LivingEntity;

/**
 * Fired when a player leaves {@code CombatState.PREPARING_ATTACK} into
 * {@code CombatState.ATTACKING} (i.e. Left Click was released and the
 * wind-up committed). Carries the final {@link AttackDirection} the
 * attack will be executed with.
 * <p>
 * This phase only creates the animation-facing transition; damage and
 * hit detection are explicitly out of scope and are reserved for a
 * future phase to listen for this event and act on it.
 */
public record AttackReleasedEvent(LivingEntity player, AttackDirection direction) {
}
