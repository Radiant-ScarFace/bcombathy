package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import com.bcombat.combat.block.GuardDirection;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Fired the instant a Perfect Block is confirmed: the defender's locked
 * {@link GuardDirection} matched the incoming attack and {@code
 * CombatController#notifyIncomingAttack} landed inside {@code
 * CombatConstants#PERFECT_BLOCK_WINDOW_TICKS} of impact.
 * <p>
 * Also fired for every Parry, since a Parry is a Perfect Block with an
 * even tighter timing window — listen here for anything that should
 * react to "the block was perfectly timed" regardless of whether it also
 * upgraded to a Parry. Sound, particles, and stamina refunds are reserved
 * for future phases; this event exists now purely so those systems have
 * a stable hook to subscribe to.
 */
public record PerfectBlockEvent(PlayerEntity defender, PlayerEntity attacker,
                                GuardDirection guardDirection, AttackDirection attackDirection) {
}