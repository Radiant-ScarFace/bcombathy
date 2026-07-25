package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the moment a player begins leaving Combat Mode (state transitions
 * into EXITING_COMBAT).
 */
public record CombatExitEvent(PlayerEntity player) {
}
