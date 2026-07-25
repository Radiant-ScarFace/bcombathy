package com.bcombat.combat.events;

import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired when a player enters {@code CombatState.PREPARING_ATTACK}.
 * Reserved for the future weapon/attack system — nothing in this phase
 * triggers this transition, but the event exists now so the attack
 * system can be built as a pure listener with no changes to this
 * framework.
 */
public record AttackPreparationStartedEvent(PlayerEntity player) {
}
