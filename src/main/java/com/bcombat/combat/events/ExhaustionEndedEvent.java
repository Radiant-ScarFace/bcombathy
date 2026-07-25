package com.bcombat.combat.events;

import net.minecraft.entity.LivingEntity;

/**
 * Fired the instant a player automatically leaves {@code
 * ExhaustionState#EXHAUSTED}, once enough stamina has regenerated (see
 * {@code CombatConstants#EXHAUSTION_RECOVERY_THRESHOLD_RATIO}). Attacks
 * and blocks may be freely initiated again from this point on, and the
 * exhaustion movement penalty is removed.
 */
public record ExhaustionEndedEvent(LivingEntity player) {
}