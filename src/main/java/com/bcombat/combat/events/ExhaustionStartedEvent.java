package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the instant a player enters {@code ExhaustionState#EXHAUSTED}
 * (stamina reached zero). From this point until {@link
 * ExhaustionEndedEvent} fires, {@code CombatController} refuses new
 * attacks and new blocks, and {@code MovementModifierManager} applies an
 * additional movement speed penalty. Basic (non-combat) movement remains
 * available throughout.
 */
public record ExhaustionStartedEvent(PlayerEntity player) {
}