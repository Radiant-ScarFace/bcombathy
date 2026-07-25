package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the moment a player begins leaving a block (state transitions
 * into {@code EXIT_BLOCK}).
 */
public record BlockEndedEvent(LivingEntity player) {
}