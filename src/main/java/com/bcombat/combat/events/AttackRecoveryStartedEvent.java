package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the moment a player enters {@code CombatState.RECOVERY} after an
 * attack. Reserved for future weapon classes that need to modify or
 * observe recovery duration without touching {@link
 * com.bcombat.combat.controller.CombatController} directly.
 */
public record AttackRecoveryStartedEvent(PlayerEntity player) {
}
