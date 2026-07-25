package com.bcombat.combat.events;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

/**
 * Fired by {@code com.bcombat.combat.mounted.MountedCombatController#tick()}
 * exactly once per actual mounted-state transition — never once per
 * tick, and never twice for the same transition; see that class's docs
 * for the full client/server correctness rationale.
 *
 * @param combatant the player or AI-controlled combatant whose mounted state changed.
 * @param mounted   the new mounted state.
 * @param mount     the entity now ridden, or {@code null} if {@code mounted} is false.
 */
public record MountedStateChangedEvent(LivingEntity combatant, boolean mounted, Entity mount) {
}