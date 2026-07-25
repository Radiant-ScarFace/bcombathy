package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the moment a player begins entering a block (state transitions
 * {@code COMBAT_IDLE -> ENTER_BLOCK}). Animation and future stance/parry
 * systems should listen to this instead of polling combat state every tick.
 */
public record BlockStartedEvent(LivingEntity player) {
}