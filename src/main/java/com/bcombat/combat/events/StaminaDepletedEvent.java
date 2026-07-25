package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the instant a player's stamina is consumed down to exactly zero.
 * Always fired alongside {@link ExhaustionStartedEvent} on the same
 * transition — this event exists separately so a future system that
 * only cares about "stamina hit zero" (e.g. a sound cue) doesn't need to
 * subscribe to the broader exhaustion-state event.
 */
public record StaminaDepletedEvent(LivingEntity player) {
}