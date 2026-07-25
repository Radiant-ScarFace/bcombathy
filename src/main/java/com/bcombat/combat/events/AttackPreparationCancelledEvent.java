package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired when a player leaves {@code CombatState.PREPARING_ATTACK} back to
 * {@code COMBAT_IDLE} without attacking (a cancelled wind-up). Reserved
 * for the future attack/feint system.
 */
public record AttackPreparationCancelledEvent(PlayerEntity player) {
}
