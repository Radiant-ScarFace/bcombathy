package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the moment a player begins leaving a block (state transitions
 * into {@code EXIT_BLOCK}).
 */
public record BlockEndedEvent(PlayerEntity player) {
}