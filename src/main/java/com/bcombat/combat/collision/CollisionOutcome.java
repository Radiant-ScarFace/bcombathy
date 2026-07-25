package com.bcombat.combat.collision;

import net.minecraft.entity.LivingEntity;

/**
 * The result of a {@link CollisionController} resolving its window for
 * one attack: either a target was found ({@link #target()} non-null), or
 * the window closed without finding one (a miss). Kept as its own small
 * carrier rather than reusing {@link HitResult} here, since at this
 * point block interception hasn't been checked yet — only
 * {@code CombatController} has enough context (the target's own
 * defensive state) to turn this into a final {@link HitResult}.
 *
 * @param target          the entity found this attack, or {@code null} for a miss.
 * @param ticksIntoAttack how many ticks into the {@code ATTACKING} state this resolved on.
 */
public record CollisionOutcome(LivingEntity target, int ticksIntoAttack) {

    public boolean hasTarget() {
        return target != null;
    }
}