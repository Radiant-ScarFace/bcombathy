package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the instant a player's stamina regenerates back up to its
 * maximum, having previously been below it. Not fired for every
 * incremental tick of regeneration — see {@link StaminaChangedEvent} for
 * that — only for the specific "fully recovered" milestone, the same
 * convenience-event relationship {@link StaminaDepletedEvent} has to
 * {@link StaminaChangedEvent}.
 */
public record StaminaRegeneratedEvent(PlayerEntity player) {
}