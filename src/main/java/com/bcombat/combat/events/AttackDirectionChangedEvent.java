package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.LivingEntity;

/**
 * Fired whenever the player's committed {@link AttackDirection} changes
 * while in {@code CombatState.PREPARING_ATTACK}, e.g. the player mouse
 * looks left, then changes their mind and looks up instead. The
 * animation controller uses this to swap which wind-up pose is shown;
 * a future feint system can also listen here to detect direction
 * fake-outs.
 */
public record AttackDirectionChangedEvent(LivingEntity player, AttackDirection previousDirection,
                                           AttackDirection currentDirection) {
}
