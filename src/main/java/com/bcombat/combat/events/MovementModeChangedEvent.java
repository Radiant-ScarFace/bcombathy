package com.bcombat.combat.events;

import com.bcombat.combat.movement.MovementMode;
import net.minecraft.entity.LivingEntity;

/**
 * Fired whenever a player's {@link MovementMode} changes. The animation
 * controller uses this to decide which base locomotion set (normal vs
 * combat walk/run/sprint) to target.
 */
public record MovementModeChangedEvent(LivingEntity player, MovementMode previousMode, MovementMode currentMode) {
}
