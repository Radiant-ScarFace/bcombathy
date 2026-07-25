package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the moment a chamber attempt begins (state transitions {@code
 * PREPARING_ATTACK -> CHAMBER_PREPARE}): the defender's committed
 * attack direction matched an incoming attack's direction. Whether the
 * attempt ultimately succeeds is resolved a few ticks later — listen for
 * {@link ChamberSucceededEvent} to react to a confirmed chamber.
 */
public record ChamberStartedEvent(PlayerEntity defender, PlayerEntity attacker, AttackDirection direction) {
}