package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the moment a player begins entering Combat Mode (state transitions
 * NORMAL -> ENTERING_COMBAT). Animation and future stance systems should
 * listen to this instead of polling movement mode every tick.
 */
public record CombatEnterEvent(LivingEntity player) {
}
