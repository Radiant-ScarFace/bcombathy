package com.bcombat.combat.events;

import com.bcombat.combat.attack.AttackDirection;
import net.minecraft.entity.LivingEntity;

/**
 * Fired the instant a chamber attempt resolves successfully (state
 * transitions {@code CHAMBER_PREPARE -> CHAMBER_SUCCESS}): direction
 * matched and timing fell within {@code CombatConstants#CHAMBER_WINDOW_TICKS}.
 * No counter damage is applied yet — this is purely a detection/animation
 * event with an extension point for a future counter-attack phase to
 * hook into.
 */
public record ChamberSucceededEvent(LivingEntity defender, LivingEntity attacker, AttackDirection direction) {
}