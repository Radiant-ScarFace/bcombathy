package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the moment a player begins leaving Combat Mode (state transitions
 * into EXITING_COMBAT).
 */
public record CombatExitEvent(LivingEntity player) {
}
