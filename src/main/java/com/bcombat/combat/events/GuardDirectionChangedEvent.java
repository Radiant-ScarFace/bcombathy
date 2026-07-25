package com.bcombat.combat.events;

import com.bcombat.combat.block.GuardDirection;
import net.minecraft.entity.LivingEntity;

/**
 * Fired whenever the player's locked {@link GuardDirection} changes while
 * in {@code CombatState.ENTER_BLOCK} or {@code CombatState.BLOCK_IDLE},
 * e.g. the player was holding a left guard and moved the mouse far enough
 * to switch to an upper guard. The animation controller uses this to swap
 * which guard pose is shown; a future chamber-block or parry system can
 * also listen here to detect a deliberate guard switch.
 */
public record GuardDirectionChangedEvent(LivingEntity player, GuardDirection previousDirection,
                                         GuardDirection currentDirection) {
}