package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired whenever a player's current stamina value actually changes,
 * whether from consumption (attacking, blocking, sprinting in combat,
 * a defensive maneuver) or from regeneration. This is the general-purpose
 * stamina event, the same role {@link CombatStateChangedEvent} plays for
 * combat state — {@link StaminaDepletedEvent}, {@link StaminaRegeneratedEvent},
 * {@link ExhaustionStartedEvent}, and {@link ExhaustionEndedEvent} are all
 * convenience events fired alongside this one for specific thresholds.
 */
public record StaminaChangedEvent(PlayerEntity player, double previousStamina, double currentStamina, double maxStamina) {
}