package com.bcombat.combat.damage;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * Stateless utility that applies an already-computed {@link
 * DamageResult} to its target's health via vanilla's own {@link
 * LivingEntity#damage} — deliberately the only class in this framework
 * that mutates an entity. Kept separate from {@link DamageCalculator}
 * so "compute the numbers" and "apply the numbers" can be tested and
 * reasoned about independently, and so a future system (e.g. damage
 * prediction/preview, or networked damage confirmation) can call {@link
 * DamageCalculator} without side effects.
 * <p>
 * Uses {@code World#getDamageSources().playerAttack(...)}, the same
 * vanilla damage source a normal melee attack would use, so vanilla
 * systems that already react to player-attack damage (totems, armor
 * trim toughness, absorption, death messages) continue to work
 * unmodified — this framework only changes how the amount is
 * calculated, not how the engine applies it.
 */
public final class DamageApplier {

    private DamageApplier() {
        // Stateless utility, no instances.
    }

    /**
     * Applies {@code result.finalDamage()} to {@code result.target()}.
     * No-op (returns 0) if the target is no longer alive by the time
     * this is called.
     *
     * @return the amount of damage actually requested from vanilla's
     * damage system (before vanilla's own armor/absorption/resistance
     * effects apply on top, which are intentionally untouched here).
     */
    public static float apply(DamageResult result) {
        LivingEntity target = result.target();
        PlayerEntity attacker = result.attacker();
        if (target == null || !target.isAlive()) {
            return 0f;
        }

        World world = target.getWorld();
        DamageSource source = world.getDamageSources().playerAttack(attacker);

        float amount = (float) result.finalDamage();
        target.damage(source, amount);
        return amount;
    }
}